import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.MongoConnection
import reactivemongo.bson.BSONDocument

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import reactivemongo.akkastream.{State, cursorProducer}

object Main extends App {
  println("Hello, World!")

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  db.getColl foreach {
    case (coll, close) =>

      def printSink = Sink.foreach[BSONDocument] { d =>
        println(db.debug(d))
      }

      def processGames(query: BSONDocument, limit: Int)(implicit m: Materializer) = {

        val gameSource: Source[BSONDocument, Future[State]] =
          coll.find(query).cursor[BSONDocument].documentSource(maxDocs = limit)

        gameSource
      }

      processGames(BSONDocument(), 1000).runWith(printSink) andThen {
        case state =>
          println(state)
          close()
          system.terminate()
      }
  }
}
