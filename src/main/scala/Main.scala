package lichess

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import reactivemongo.api._
import reactivemongo.api.bson._

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import java.nio.file.Paths
import reactivemongo.akkastream.{ cursorProducer, State }

import org.joda.time.DateTime

import chess.format.pgn.Pgn
import chess.variant.{ Horde, Standard, Variant }
import lila.analyse.Analysis
import lila.analyse.Analysis.analysisBSONHandler
import lila.game.BSONHandlers
import lila.game.{ Game, PgnDump, UnivPgn, Source => S }
import lila.db.dsl._

object Main extends App {

  val fromStr = args.lift(0).getOrElse("2015-01")

  val path =
    args.lift(1).getOrElse("out/lichess_db_%.pgn").replace("%", fromStr)

  val from =
    new DateTime(fromStr).withDayOfMonth(1).withTimeAtStartOfDay()
  val to = from plusMonths 1

  val hordeStartDate = new DateTime(2015, 4, 11, 10, 0)

  println(s"Export $from to $path")

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withInputBuffer(
        initialSize = 32,
        maxSize = 32
      )
  )

  lichess.DB.get foreach { case (db, close) =>
    val query = BSONDocument(
      "ca" -> BSONDocument("$gte" -> from, "$lt" -> to)
      // "_id" -> BSONDocument("$in" -> List("XvPAZYmX", "TBpSyJWy", "R3kAcqz2", "2rSVaqhQ"))
      // "ra" -> BSONDocument("$ne" -> true)
    )

    val docSource = db.gameColl
      .find(query)
      .sort(BSONDocument("ca" -> 1))
      // .cursor[Game.WithInitialFen]()
      .cursor[Bdoc](readPreference = ReadPreference.secondaryPreferred)
      .documentSource(
        maxDocs = Int.MaxValue,
        err = Cursor.ContOnError((_, e) => {
          println("Cursor error")
          println(e)
          println(e.getMessage)
        })
      )

    val tickSource =
      Source.tick(Reporter.freq, Reporter.freq, None)

    def toGame(doc: BSONDocument): Option[Game.WithInitialFen] =
      BSONHandlers.gameWithInitialFenBSONHandler.readDocument(doc).toOption

    def filterGame(g: Game.WithInitialFen) =
      g.game.variant != Horde || g.game.createdAt.isAfter(hordeStartDate)

    type Analysed = (Game.WithInitialFen, Option[Analysis])
    def withAnalysis(gs: Seq[Game.WithInitialFen]): Future[Seq[Analysed]] =
      db.analysisColl
        .find(
          BSONDocument(
            "_id" -> BSONDocument(
              "$in" -> gs.filter(_.game.metadata.analysed).map(_.game.id)
            )
          )
        )
        .cursor[Analysis](readPreference = ReadPreference.secondaryPreferred)
        .collect[List](Int.MaxValue, Cursor.ContOnError[List[Analysis]]()) map { as =>
        gs.map { g =>
          g -> as.find(_.id == g.game.id)
        }
      }

    type WithUsers = (Analysed, Users)
    def withUsers(as: Seq[Analysed]): Future[Seq[WithUsers]] =
      db.users(as.map(_._1.game)).map { users =>
        as zip users
      }

//     type WithBerserks = (WithUsers, Berserks)
//     def withUsers(as: Seq[WithUsers]): Future[Seq[WithBerserks]] =
//       db.users(as.map(_._1.game)).map { users =>
//         as zip users
//       }

    def toPgn(ws: Seq[WithUsers]): Future[Seq[String]] = Future {
      ws map { case ((g, analysis), users) =>
        val pgn = PgnDump(g.game, users, g.fen)
        val annotated = lila.analyse.Annotator(
          pgn,
          analysis,
          g.game.winnerColor,
          g.game.status,
          g.game.clock
        )
        UnivPgn.render(annotated, analysis)
      }
    }

    def pgnSink: Sink[Seq[String], Future[IOResult]] =
      Flow[Seq[String]]
        .map { pgns =>
          // merge analysis & eval comments
          // 1. e4 { [%eval 0.17] } { [%clk 0:00:30] }
          // 1. e4 { [%eval 0.17] [%clk 0:00:30] }
          val str = pgns.mkString("\n\n").replace("] } { [", "] [")
          ByteString(s"$str\n\n")
        }
        .toMat(FileIO.toPath(Paths.get(path)))(Keep.right)

    docSource
      .map(doc => toGame(doc) filter filterGame)
      .buffer(20000, OverflowStrategy.backpressure)
      .merge(tickSource, eagerComplete = true)
      .via(Reporter)
      .grouped(300)
      .mapAsyncUnordered(32)(withAnalysis)
      .mapAsyncUnordered(32)(withUsers)
      // .mapAsyncUnordered(16)(withBerserks)
      .mapAsyncUnordered(32)(toPgn)
      .runWith(pgnSink) andThen { case state =>
      close()
      system.terminate()
    }
  }
}
