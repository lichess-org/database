import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


import reactivemongo.bson._

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer, IOResult}
import akka.stream.scaladsl._
import akka.util.ByteString
import reactivemongo.akkastream.{State, cursorProducer}
import java.nio.file.Paths

import org.joda.time.DateTime

import chess.format.pgn.Pgn
import db.BSONDateTimeHandler
import lila.game.BSONHandlers._
import lila.game.{Game, PgnDump, Source => S}

object Main extends App {

  override def main(args: Array[String]) {

    val fromStr = args.lift(0).getOrElse("2015-01")
    val from = new DateTime(fromStr).withDayOfMonth(1).withTimeAtStartOfDay()
    val to = from plusMonths 1

    val path = args.lift(1).getOrElse("lichess_db_%.pgn").replace("%", fromStr)

    println(s"Export $from to $path")

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    db.getColl foreach {
      case (coll, close) =>

        val sources = List(S.Lobby, S.Friend, S.Tournament, S.Pool)

        val query = BSONDocument(
          "ca" -> BSONDocument("$gte" -> from, "$lt" -> to),
          "ra" -> true,
          "so" -> BSONDocument("$in" -> sources.map(_.id)),
          "v" -> BSONDocument("$exists" -> false))

        def printSink = Sink.foreach[String](s => println(s))

        def toPgn(g: Game) = g.toString

        def pgnSink: Sink[Pgn, Future[IOResult]] =
          Flow[Pgn]
            .map(pgn => ByteString(s"$pgn\n\n"))
            .toMat(FileIO.toPath(Paths.get(path)))(Keep.right)

        coll
          .find(query)
          .cursor[Game]
          .documentSource(maxDocs = 100000) // Int.MaxValue)
          .map(g => PgnDump(g, None))
          .runWith(pgnSink) andThen {
            case state =>
              close()
              system.terminate()
          }
    }
  }
}
