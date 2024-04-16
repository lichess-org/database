package lichess

import akka.actor.ActorSystem
import akka.stream.*
import akka.stream.scaladsl.*
import akka.util.ByteString
import chess.format.{ EpdFen, Fen }
import com.typesafe.config.ConfigFactory
import java.nio.file.Paths
import lila.db.dsl.*
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.*
import reactivemongo.api.bson.*
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Puzzles:

  case class PuzzleLine(
      id: String,
      fen: Fen.Epd,
      moves: List[String],
      rating: Int,
      ratingDev: Int,
      popularity: Int,
      plays: Int,
      themes: List[String],
      openings: List[String],
      gameUrl: String
  )

  def main(args: Array[String]): Unit =

    val path = args.headOption.getOrElse("out/lichess_db_puzzle.csv")

    println(s"Exporting to $path")

    val config   = ConfigFactory.load()
    val dbName   = "puzzler"
    val collName = "puzzle2_puzzle"

    val uri    = config.getString("db.puzzle.uri")
    val driver = new AsyncDriver(Some(config.getConfig("mongo-async-driver")))

    given system: ActorSystem = ActorSystem()
    given ActorMaterializer = ActorMaterializer(
      ActorMaterializerSettings(system)
        .withInputBuffer(
          initialSize = 32,
          maxSize = 32
        )
    )

    val hiddenThemes = Set("checkFirst")

    def parseDoc(doc: Bdoc): Option[PuzzleLine] = for
      id         <- doc.string("_id")
      fen        <- EpdFen.from(doc.string("fen"))
      moves      <- doc.string("line")
      glicko     <- doc.child("glicko")
      rating     <- glicko.double("r")
      rd         <- glicko.double("d")
      popularity <- doc.double("vote")
      plays      <- doc.int("plays")
      themes     <- doc.getAsOpt[List[String]]("themes")
      gameId     <- doc.string("gameId")
      openings = doc.getAsOpt[List[String]]("opening")
    yield PuzzleLine(
      id = id,
      fen = fen,
      moves = moves.split(' ').toList,
      rating = rating.toInt,
      ratingDev = rd.toInt,
      popularity = math.round(popularity * 100).toInt,
      plays = plays,
      themes = themes.filterNot(hiddenThemes.contains),
      gameUrl =
        val asWhite = fen.colorOrWhite.white
        val hash    = Fen.readPly(fen).fold("")(p => s"#$p")
        s"https://lichess.org/${gameId}${if asWhite then "" else "/black"}$hash"
      ,
      openings = openings.getOrElse(Nil)
    )

    def toCsvLine(puzzle: PuzzleLine): String =
      List(
        puzzle.id,
        puzzle.fen.value,
        puzzle.moves.mkString(" "),
        puzzle.rating,
        puzzle.ratingDev,
        puzzle.popularity,
        puzzle.plays,
        puzzle.themes.sorted.mkString(" "),
        puzzle.gameUrl,
        puzzle.openings.mkString(" ")
      ).mkString(",")

    def csvSink: Sink[String, Future[IOResult]] =
      Flow[String]
        .map { line =>
          ByteString(s"$line\n")
        }
        .toMat(FileIO.toPath(Paths.get(path)))(Keep.right)

    val process = MongoConnection
      .fromString(uri)
      .flatMap { parsedUri =>
        driver.connect(parsedUri, Some("lichess-puzzle"))
      }
      .flatMap(_.database(dbName))
      .flatMap {
        _.collection(collName)
          .find(BSONDocument("issue" -> BSONDocument("$exists" -> false)))
          .sort(BSONDocument("_id" -> 1))
          .cursor[Bdoc]()
          // .cursor[Bdoc](readPreference = ReadPreference.secondary)
          .documentSource(
            // maxDocs = 10,
            maxDocs = Int.MaxValue,
            err = Cursor.ContOnError((_, e) => println(e.getMessage))
          )
          .buffer(1000, OverflowStrategy.backpressure)
          .mapConcat(d => parseDoc(d).toList)
          .map(toCsvLine)
          .prepend(
            Source(
              List("PuzzleId,FEN,Moves,Rating,RatingDeviation,Popularity,NbPlays,Themes,GameUrl,OpeningTags")
            )
          )
          .runWith(csvSink)
      }

    scala.concurrent.Await.result(process, Duration.Inf)
    println("done")
    system.terminate()
