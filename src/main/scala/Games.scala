package lichess

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import reactivemongo.api.*
import reactivemongo.api.bson.*

import akka.actor.ActorSystem
import akka.stream.*
import akka.stream.scaladsl.*
import akka.util.ByteString
import java.nio.file.Paths
import reactivemongo.akkastream.cursorProducer

import chess.variant.{ Horde, Variant }
import lila.analyse.Analysis
import lila.analyse.Analysis.analysisBSONHandler
import lila.game.{ Game, PgnDump }
import lila.db.dsl.*
import java.time.LocalDate

object Games:

  def main(args: Array[String]): Unit =
    val fromStr = args.lift(0).getOrElse("2015-01")

    val path =
      args.lift(1).getOrElse("out/lichess_db_%.pgn").replace("%", fromStr)

    val variant = Variant
      .apply(Variant.LilaKey(args.lift(2).getOrElse("standard")))
      .getOrElse(throw new RuntimeException("Invalid variant."))

    val fromWithoutAdjustments = LocalDate.parse(s"$fromStr-01").atStartOfDay
    val to                     = fromWithoutAdjustments.plusMonths(1)

    val hordeStartDate = java.time.LocalDateTime.of(2015, 4, 11, 10, 0)
    val from =
      if variant == Horde && hordeStartDate.isAfter(fromWithoutAdjustments)
      then hordeStartDate
      else fromWithoutAdjustments

    if !from.isBefore(to) then
      System.out.println("Too early for Horde games. Exiting.");
      System.exit(0);

    println(s"Export $from to $path")

    given system: ActorSystem = ActorSystem()
    given ActorMaterializer = ActorMaterializer(
      ActorMaterializerSettings(system)
        .withInputBuffer(
          initialSize = 32,
          maxSize = 32
        )
    )

    val process = lichess.DB.get.flatMap { (db, close) =>

      val query = BSONDocument(
        "ca" -> BSONDocument("$gte" -> from, "$lt" -> to),
        "ra" -> true,
        "v"  -> BSONDocument("$exists" -> variant.exotic)
      )

      val gameSource = db.gameColl
        .find(query)
        .sort(BSONDocument("ca" -> 1))
        .cursor[BSONDocument](readPreference = ReadPreference.primary)
        .documentSource(
          maxDocs = Int.MaxValue,
          err = Cursor.ContOnError((_, e) => println(e.getMessage))
        )

      val tickSource =
        Source.tick(Reporter.freq, Reporter.freq, None)

      // def checkLegality(g: Game): Future[(Game, Boolean)] = Future {
      //   g -> chess.Replay
      //     .boards(g.sans, None, g.variant)
      //     .fold(
      //       err => {
      //         println(s"Replay error ${g.id} ${err.toString.take(60)}")
      //         false
      //       },
      //       boards => {
      //         if boards.size == g.sans.size + 1 then true
      //         else {
      //           println(
      //             s"Replay error ${g.id} boards.size=${boards.size}, moves.size=${g.sans.size}"
      //           )
      //           false
      //         }
      //       }
      //     )
      // }

      def bsonRead(variant: Variant)(docs: Seq[BSONDocument]) = Future {
        docs
          .map(lila.game.BSONHandlers.gameWithInitialFenBSONHandler.read)
          .filter(_.game.variant == variant)
      }

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
          .collect[List](Int.MaxValue, Cursor.ContOnError[List[Analysis]]())
          .map { as =>
            gs.map { g =>
              g -> as.find(_.id == g.game.id)
            }
          }

      type WithUsers = (Analysed, Users)
      def withUsers(as: Seq[Analysed]): Future[Seq[WithUsers]] =
        db.users(as.map(_._1.game)).map { users =>
          as.zip(users)
        }

      def toPgn(ws: Seq[WithUsers]): Future[ByteString] =
        Future {
          ByteString {
            ws.map { case ((g, analysis), users) =>
              val pgn = PgnDump(g.game, users, g.fen)
              lila.analyse
                .Annotator(
                  pgn,
                  analysis,
                  g.game.winnerColor,
                  g.game.status,
                  g.game.clock
                )
                .render
                .value
                .replace("] } { [", "] [") + "\n\n"
            }.mkString
          }
        }

      def pgnSink: Sink[ByteString, Future[IOResult]] =
        Flow[ByteString].toMat(FileIO.toPath(Paths.get(path)))(Keep.right)

      gameSource
        .buffer(10000, OverflowStrategy.backpressure)
        .grouped(64)
        .mapAsyncUnordered(12)(bsonRead(variant))
        .map(g => Some(g))
        .merge(tickSource, eagerComplete = true)
        .via(Reporter.graph)
        // .mapAsyncUnordered(16)(checkLegality)
        // .filter(_._2).map(_._1)
        .mapAsyncUnordered(16)(withAnalysis)
        .mapAsyncUnordered(16)(withUsers)
        .mapAsyncUnordered(12)(toPgn)
        .runWith(pgnSink)
        .andThen { case _ => close() }

    }

    scala.concurrent.Await.result(process, Duration.Inf)
    println("done")
    system.terminate()
