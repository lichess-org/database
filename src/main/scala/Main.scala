import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}

import reactivemongo.bson._

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import reactivemongo.akkastream.{State, cursorProducer}

import org.joda.time.DateTime

import db.BSONDateTimeHandler

object Main extends App {

  override def main(args: Array[String]) {
    val from = new DateTime(args.lift(0).getOrElse("2016-01")).withDayOfMonth(1).withTimeAtStartOfDay()
    val to = from plusMonths 1
    println(s"Export $from -> $to")

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    db.getColl foreach {
      case (coll, close) =>

        val query = BSONDocument("ca" -> BSONDocument("$gte" -> from, "$lt" -> to))

        def printSink = Sink.foreach[String](s => println(s))

        def toPgn(doc: BSONDocument) = db.debug(doc)

        coll
          .find(query)
          .cursor[BSONDocument]
          .documentSource(maxDocs = Int.MaxValue)
          .map(toPgn)
          .runWith(printSink) andThen {
            case state =>
              close()
              system.terminate()
          }
    }
  }
}
