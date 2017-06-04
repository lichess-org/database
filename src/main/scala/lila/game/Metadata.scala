package lila.game

import lila.db.ByteArray
import org.joda.time.DateTime

private[game] case class Metadata(
    source: Option[Source],
    pgnImport: Option[PgnImport],
    tournamentId: Option[String],
    simulId: Option[String],
    tvAt: Option[DateTime],
    analysed: Boolean
) {

  def pgnDate = pgnImport flatMap (_.date)

  def pgnUser = pgnImport flatMap (_.user)

  def isEmpty = this == Metadata.empty
}

private[game] object Metadata {

  val empty = Metadata(None, None, None, None, None, false)
}

case class PgnImport(
  user: Option[String],
  date: Option[String],
  pgn: String,
  // hashed PGN for DB unicity
  h: Option[ByteArray]
)

object PgnImport {

  import reactivemongo.bson.Macros
  import ByteArray.ByteArrayBSONHandler
  implicit val pgnImportBSONHandler = Macros.handler[PgnImport]
}
