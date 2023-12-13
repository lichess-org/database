package lichess

import akka.actor.ActorSystem
import akka.stream.*
import akka.stream.scaladsl.*
import java.nio.file.Paths
import com.typesafe.config.ConfigFactory
import lila.db.dsl.*
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.*
import reactivemongo.api.bson.*
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global
import chess.format.Uci
import cats.data.NonEmptyList
import cats.syntax.all.*
import scala.util.{ Success, Try }
import scala.concurrent.Future
import play.api.libs.json.*
import akka.util.ByteString
import akka.NotUsed
import chess.variant.Variant

object Evals:

  enum Score:
    case Cp(c: Int)
    case Mate(m: Int)
  case class Pv(score: Score, moves: NonEmptyList[Uci])
  case class Eval(pvs: NonEmptyList[Pv], knodes: Int, depth: Int)
  case class Id(variant: Variant, smallFen: String)
  case class Entry(_id: Id, evals: List[Eval]):
    def fen = chess.format.SmallFen(_id.smallFen).garbageInefficientReadBackIntoFen

  /*
    val base = fen.value.split(' ').take(4).mkString("").filter { c =>
      c != '/' && c != '-' && c != 'w'
    }
    if variant == chess.variant.ThreeCheck
    then fen.value.split(' ').lift(6).foldLeft(base)(_ + _)
    else base
   */
  // def toEpd(small: String): Fen.Epd =

  given BSONReader[Id] = new:
    def readTry(bs: BSONValue) = bs match
      case BSONString(value) =>
        value split ':' match
          case Array(fen) => Success(Id(chess.variant.Standard, fen))
          case Array(variantId, fen) =>
            import chess.variant.Variant
            Success(
              Id(
                Variant.Id.from(variantId.toIntOption) flatMap {
                  Variant(_)
                } getOrElse sys.error(s"Invalid evalcache variant $variantId"),
                fen
              )
            )
          case _ => handlerBadValue(s"Invalid evalcache id $value")
      case _ => handlerBadValue(s"Invalid evalcache id $bs")
  given BSONReader[NonEmptyList[Pv]] = new:
    private def scoreRead(str: String): Option[Score] =
      if str startsWith "#" then str.drop(1).toIntOption.map(Score.Mate.apply)
      else str.toIntOption.map(Score.Cp.apply)
    private def movesRead(str: String): Option[NonEmptyList[Uci]] =
      Uci readListChars str flatMap (_.toNel)
    private val scoreSeparator = ':'
    private val pvSeparator    = '/'
    def readTry(bs: BSONValue) = bs match
      case BSONString(value) =>
        Try {
          value.split(pvSeparator).toList.map { pvStr =>
            pvStr.split(scoreSeparator) match
              case Array(score, moves) =>
                Pv(
                  scoreRead(score) getOrElse sys.error(s"Invalid score $score"),
                  movesRead(moves) getOrElse sys.error(s"Invalid moves $moves")
                )
              case x => sys error s"Invalid PV $pvStr: ${x.toList} (in $value)"
          }
        }.map {
          _.toNel getOrElse sys.error(s"Empty PVs $value")
        }
      case b => lila.db.BSON.handlerBadType[NonEmptyList[Pv]](b)
  given BSONDocumentReader[Eval]  = Macros.reader
  given BSONDocumentReader[Entry] = Macros.reader

  def main(args: Array[String]): Unit =

    val path = args.headOption.getOrElse("out/lichess_db_eval.json")

    println(s"Exporting to $path")

    val config   = ConfigFactory.load()
    val dbName   = "lichess"
    val collName = "eval_cache"

    val uri    = config.getString("db.eval.uri")
    val driver = new AsyncDriver(Some(config.getConfig("mongo-async-driver")))

    given system: ActorSystem = ActorSystem()
    given ActorMaterializer = ActorMaterializer(
      ActorMaterializerSettings(system)
        .withInputBuffer(
          initialSize = 32,
          maxSize = 32
        )
    )

    def toJson: Flow[Entry, JsObject, NotUsed] = Flow[Entry].map: entry =>
      Json.obj(
        "fen" -> entry.fen.value,
        "evals" -> entry.evals.map: eval =>
          Json.obj(
            "pvs" -> eval.pvs.toList.map: pv =>
              val base = pv.score match
                case Score.Cp(c)   => Json.obj("cp" -> c)
                case Score.Mate(m) => Json.obj("mate" -> m)
              base + ("line" -> JsString(pv.moves.map(_.uci).toList.mkString(" ")))
            ,
            "knodes" -> eval.knodes,
            "depth"  -> eval.depth
          )
      )

    def ndjsonSink: Sink[JsObject, Future[IOResult]] =
      Flow[JsObject]
        .map { obj =>
          ByteString(s"${Json.stringify(obj)}\n")
        }
        .toMat(FileIO.toPath(Paths.get(path)))(Keep.right)

    val process = MongoConnection
      .fromString(uri)
      .flatMap { parsedUri =>
        driver.connect(parsedUri, Some("lichess-eval"))
      }
      .flatMap(_.database(dbName))
      .flatMap {
        _.collection(collName)
          .find($doc("_id" -> $doc("$not" -> BSONRegex(":", "")))) // no variant
          // .cursor[Entry]()
          .cursor[Entry](readPreference = ReadPreference.secondary)
          .documentSource(
            // maxDocs = 1000,
            maxDocs = Int.MaxValue,
            err = Cursor.ContOnError((_, e) => println(e.getMessage))
          )
          .buffer(1000, OverflowStrategy.backpressure)
          .via(toJson)
          .runWith(ndjsonSink)
      }

    scala.concurrent.Await.result(process, Duration.Inf)
    println("done")
    system.terminate()
