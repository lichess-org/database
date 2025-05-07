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

import lila.analyse.Analysis
import lila.game.{ Game, PgnDump }
import lila.db.dsl.*
import java.time.LocalDateTime

object GWC:

  def main(args: Array[String]): Unit =
    val path = args(0)

    val variant = chess.variant.Standard

    val from      = LocalDateTime.parse("2024-07-19T22:00:00.00")
    val to        = LocalDateTime.parse("2024-07-20T22:00:00.00")
    val toInstant = to.toInstant(java.time.ZoneOffset.UTC)

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

      def bsonRead(variant: chess.variant.Variant)(docs: Seq[BSONDocument]) = Future {
        docs
          .map(lila.game.BSONHandlers.gameWithInitialFenBSONHandler.read)
          .filter(_.game.variant == variant)
      }

      type Analysed = (Game.WithInitialFen, Option[Analysis])
      def withAnalysis(gs: Seq[Game.WithInitialFen]): Future[Seq[Analysed]] =
        Future.successful(gs.map(_ -> None))

      type WithUsers = (Analysed, Users)
      def withUsers(as: Seq[Analysed]): Future[Seq[WithUsers]] =
        db.users(as.map(_._1.game)).map { users =>
          as.zip(users)
        }

      def toPgn(ws: Seq[WithUsers]): Future[ByteString] =
        Future {
          val str = ws
            .map { case ((g, analysis), users) =>
              val pgn = PgnDump(g.game, users, g.fen)
              lila.analyse.Annotator(pgn, analysis)
            }
            .map(_.toString)
            .mkString("\n\n")
            .replace("] } { [", "] [")
          ByteString(s"$str\n\n")
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
        .map(_.filter(_.game.movedAt.isBefore(toInstant)))
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
