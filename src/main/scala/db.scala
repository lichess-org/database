import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.MongoConnection
import reactivemongo.bson._

import org.joda.time._

object db {

  private val config = ConfigFactory.load()
  private val dbUri = config.getString("db.uri")

  val dbName = "lichess"
  val collName = "game5"

  val driver = new reactivemongo.api.MongoDriver
  val conn = driver connection MongoConnection.parseURI(dbUri).get

  def getColls: Future[(BSONCollection, BSONCollection, () => Unit)] =
    conn.database(dbName).map { db =>
      (
        db collection "game5",
        db collection "analysis2",
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
