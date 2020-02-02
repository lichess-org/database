package lichess

import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import reactivemongo.api._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._

import org.joda.time._

final class DB(
    val gameColl: BSONCollection,
    val analysisColl: BSONCollection,
    val userColl: BSONCollection
) {

  private val userProj = BSONDocument("username" -> true, "title" -> true)
  implicit private val lightUserBSONReader = new BSONDocumentReader[LightUser] {
    def read(doc: BSONDocument) = LightUser(
      id = doc.getAs[String]("_id").get,
      name = doc.getAs[String]("username").get,
      title = doc.getAs[String]("title")
    )
  }

  def users(gs: Seq[lila.game.Game]): Future[Seq[Users]] =
    userColl
      .find(
        BSONDocument(
          "_id" -> BSONDocument("$in" -> gs.flatMap(_.userIds).distinct)
        ),
        userProj
      )
      .cursor[LightUser](readPreference = ReadPreference.secondary)
      .collect[List](Int.MaxValue, Cursor.ContOnError[List[LightUser]]())
      .map { users =>
        def of(p: lila.game.Player) = p.userId.fold(LightUser("?", "?")) { uid =>
          users.find(_.id == uid) getOrElse LightUser(uid, uid)
        }
        gs.map { g =>
          Users(of(g.whitePlayer), of(g.blackPlayer))
        }
      }
}

object DB {

  private val config = ConfigFactory.load()

  val dbName   = "lichess"
  val collName = "game5"

  val uri       = config.getString("db.uri")
  val driver    = new AsyncDriver(Some(config.getConfig("mongo-async-driver ")))
  val parsedUri = MongoConnection.fromString(uri)
  val conn      = parsedUri.flatMap(driver.connect)

  def get: Future[(DB, () => Unit)] =
    conn.flatMap(_.database(dbName)).map { db =>
      (
        new DB(
          gameColl = db collection "game5",
          analysisColl = db collection "analysis2",
          userColl = db collection "user4"
        ),
        (() => {
          driver.close()
        })
      )
    }

  implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
    def read(time: BSONDateTime) = new DateTime(time.value, DateTimeZone.UTC)
    def write(jdtime: DateTime)  = BSONDateTime(jdtime.getMillis)
  }

  def debug(v: BSONValue): String = v match {
    case d: BSONDocument => debugDoc(d)
    case d: BSONArray    => debugArr(d)
    case BSONString(x)   => x
    case BSONInteger(x)  => x.toString
    case BSONDouble(x)   => x.toString
    case BSONBoolean(x)  => x.toString
    case v               => v.toString
  }
  def debugArr(doc: BSONArray): String =
    doc.values.toList.map(debug).mkString("[", ", ", "]")
  def debugDoc(doc: BSONDocument): String =
    (doc.elements.toList map {
      case BSONElement(k, v) => s"$k: ${debug(v)}"
    }).mkString("{", ", ", "}")
}
