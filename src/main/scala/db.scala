package lichess

import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{ MongoConnection, MongoConnectionOptions }
import reactivemongo.bson._

import org.joda.time._

final class DB(
    val gameColl: BSONCollection,
    val analysisColl: BSONCollection,
    val userColl: BSONCollection
) {

  private val userProj = BSONDocument("username" -> true, "title" -> true)
  private implicit val lightUserBSONReader = new BSONDocumentReader[LightUser] {
    def read(doc: BSONDocument) = LightUser(
      id = doc.getAs[String]("_id").get,
      name = doc.getAs[String]("username").get,
      title = doc.getAs[String]("title")
    )
  }

  def users(g: lila.game.Game): Future[Users] =
    userColl.find(BSONDocument("_id" -> BSONDocument("$in" -> g.userIds)), userProj)
      .cursor[LightUser].collect[List]().map { users =>
        def of(p: lila.game.Player) = p.userId.fold(LightUser("?", "?")) { uid =>
          users.find(_.id == uid) getOrElse LightUser(uid, uid)
        }
        Users(of(g.whitePlayer), of(g.blackPlayer))
      }
}

object DB {

  private val config = ConfigFactory.load()
  private val dbUri = config.getString("db.uri")

  val dbName = "lichess"
  val collName = "game5"

  val fiveMinutesInMillis = 5 * 60 * 1000

  val driver = new reactivemongo.api.MongoDriver
  val conOpts = MongoConnectionOptions(
    connectTimeoutMS = fiveMinutesInMillis,
    maxIdleTimeMS = fiveMinutesInMillis,
    monitorRefreshMS = fiveMinutesInMillis
  )
  val conUri = MongoConnection.parseURI(dbUri).get
  val conn = driver.connection(List(dbUri), conOpts, Nil, None)

  def get: Future[(DB, () => Unit)] =
    conn.database(dbName).map { db =>
      (new DB(
        gameColl = db collection "game5",
        analysisColl = db collection "analysis2",
        userColl = db collection "user4"
      ),
        (() => {
          conn.close()
          driver.close()
        }))
    }

  implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
    def read(time: BSONDateTime) = new DateTime(time.value, DateTimeZone.UTC)
    def write(jdtime: DateTime) = BSONDateTime(jdtime.getMillis)
  }

  def debug(v: BSONValue): String = v match {
    case d: BSONDocument => debugDoc(d)
    case d: BSONArray => debugArr(d)
    case BSONString(x) => x
    case BSONInteger(x) => x.toString
    case BSONDouble(x) => x.toString
    case BSONBoolean(x) => x.toString
    case v => v.toString
  }
  def debugArr(doc: BSONArray): String = doc.values.toList.map(debug).mkString("[", ", ", "]")
  def debugDoc(doc: BSONDocument): String = (doc.elements.toList map {
    case BSONElement(k, v) => s"$k: ${debug(v)}"
  }).mkString("{", ", ", "}")
}
