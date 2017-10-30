package lichess

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import reactivemongo.api.ReadPreference
import reactivemongo.bson._

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import java.nio.file.Paths
import reactivemongo.akkastream.{State, cursorProducer}

import org.joda.time.DateTime

import chess.format.pgn.Pgn
import chess.variant.{ Standard, Horde, Variant }
import lichess.DB.BSONDateTimeHandler
import lila.analyse.Analysis
import lila.analyse.Analysis.analysisBSONHandler
import lila.game.BSONHandlers._
import lila.game.BSONHandlers._
import lila.game.{Game, PgnDump, Source => S}

object Main extends App {

  override def main(args: Array[String]) {

    val fromStr = args.lift(0).getOrElse("2015-01")

    val path = args.lift(1).getOrElse("out/lichess_db_%.pgn").replace("%", fromStr)

    val variant = Variant.apply(args.lift(2).getOrElse("standard")).getOrElse(throw new RuntimeException("Invalid variant."))

    val fromWithoutAdjustments = new DateTime(fromStr).withDayOfMonth(1).withTimeAtStartOfDay()
    val to = fromWithoutAdjustments plusMonths 1

    val hordeStartDate = new DateTime(2015, 4, 11, 10, 0)
    val from = if (variant == Horde && hordeStartDate.compareTo(fromWithoutAdjustments) > 0) hordeStartDate else fromWithoutAdjustments

    if (from.compareTo(to) > 0) {
      System.out.println("Too early for Horde games. Exiting.");
      System.exit(0);
    }

    println(s"Export $from to $path")

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer(
      ActorMaterializerSettings(system)
        .withInputBuffer(
          initialSize = 32,
          maxSize = 32))

    DB.get foreach {
      case (db, close) =>

        val sources = List(S.Lobby, S.Friend, S.Tournament, S.Pool)

        val variantBson = if (variant == Standard) BSONDocument("$exists" -> false) else BSONInteger(variant.id)
        val query = BSONDocument(
          "ca" -> BSONDocument("$gte" -> from, "$lt" -> to),
          "ra" -> true,
          "v" -> variantBson)

        val gameSource = db.gameColl
          .find(query)
          .sort(BSONDocument("ca" -> 1))
          .cursor[Game]()
          .documentSource(maxDocs = Int.MaxValue)

        val tickSource =
          Source.tick(Reporter.freq, Reporter.freq, None)

        def checkLegality(g: Game): Future[(Game, Boolean)] = Future {
          g -> chess.Replay.boards(g.pgnMoves, None, g.variant).fold(
            err => {
              println(s"Replay error ${g.id} ${err.toString.take(60)}")
              false
            },
            boards => {
              if (boards.size == g.pgnMoves.size + 1) true
              else {
                println(s"Replay error ${g.id} boards.size=${boards.size}, moves.size=${g.pgnMoves.size}")
                false
              }
            })
        }

        type Analysed = (Game, Option[Analysis])

        def withAnalysis(g: Game): Future[Analysed] =
          if (g.metadata.analysed)
            db.analysisColl.find(BSONDocument("_id" -> g.id)).one[Analysis] map { g -> _ }
          else Future.successful(g -> None)

        type WithUsers = (Analysed, Users)
        def withUsers(a: Analysed): Future[WithUsers] =
          db.users(a._1).map { a -> _ }

        def toPgn(w: WithUsers): Future[Pgn] = Future(w match {
          case ((game, analysis), users) =>
            val pgn = PgnDump(game, users, None)
            lila.analyse.Annotator(pgn, analysis, game.winnerColor, game.status, game.clock)
        })

        def pgnSink: Sink[Pgn, Future[IOResult]] =
          Flow[Pgn]
            .map { pgn =>
              // merge analysis & eval comments
              // 1. e4 { [%eval 0.17] } { [%clk 0:00:30] }
              // 1. e4 { [%eval 0.17] [%clk 0:00:30] }
              val str = pgn.toString.replace("] } { [", "] [")
              ByteString(s"$str\n\n")
            }
            .toMat(FileIO.toPath(Paths.get(path)))(Keep.right)

        gameSource
          .buffer(10000, OverflowStrategy.backpressure)
          .map(g => Some(g))
          .merge(tickSource, eagerComplete = true)
          .via(Reporter)
          // .mapAsyncUnordered(16)(checkLegality)
          // .filter(_._2).map(_._1)
          .mapAsyncUnordered(16)(withAnalysis)
          .mapAsyncUnordered(16)(withUsers)
          .mapAsyncUnordered(16)(toPgn)
          .runWith(pgnSink) andThen {
            case state =>
              close()
              system.terminate()
          }
    }
  }
}
