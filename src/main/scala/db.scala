package lichess

import com.typesafe.config.ConfigFactory
import reactivemongo.api.*
import reactivemongo.api.bson.*
import reactivemongo.api.bson.collection.BSONCollection
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

final class DB(
    val gameColl: BSONCollection,
    val analysisColl: BSONCollection,
    val userColl: BSONCollection
) {

  private val userProj = BSONDocument("username" -> true, "title" -> true)
  given lightUserBSONReader: BSONDocumentReader[LightUser] = new:

    def readDocument(doc: BSONDocument) =
      Success(
        LightUser(
          id = doc.string("_id").get,
          name = doc.string("username").get,
          title = doc.string("title")
        )
      )

  def users(gs: Seq[lila.game.Game]): Future[Seq[Users]] =
    userColl
      .find(
        BSONDocument(
          "_id" -> BSONDocument("$in" -> gs.flatMap(_.players.mapList(_.userId).flatten).distinct)
        ),
        Some(userProj)
      )
      .cursor[LightUser](readPreference = ReadPreference.secondary)
      .collect[List](Int.MaxValue, Cursor.ContOnError[List[LightUser]]())
      .map { users =>
        def of(p: lila.game.Player) = p.userId.fold(LightUser("?", "?")) { uid =>
          users.find(_.id == uid) getOrElse LightUser(uid, uid)
        }
        gs.map { g =>
          Users(of(g.players.white), of(g.players.black))
        }
      }
}

object DB {

  private val config = ConfigFactory.load()

  val dbName   = "lichess"
  val collName = "game5"

  val uri    = config.getString("db.game.uri")
  val driver = new AsyncDriver(Some(config.getConfig("mongo-async-driver")))
  val conn =
    MongoConnection.fromString(uri) flatMap { parsedUri =>
      driver.connect(parsedUri, Some("lichess-db"))
    }

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
}
