package lila.game

import chess.Color

case class PlayerUser(id: String, rating: Int, ratingDiff: Option[Int])

case class Player(
    id: String,
    color: Color,
    aiLevel: Option[Int],
    isWinner: Option[Boolean] = None,
    userId: Option[String] = None,
    rating: Option[Int] = None,
    ratingDiff: Option[Int] = None,
    provisional: Boolean = false,
    berserk: Boolean = false,
    name: Option[String] = None
) {

  def playerUser = userId flatMap { uid =>
    rating map { PlayerUser(uid, _, ratingDiff) }
  }

  def isAi = aiLevel.isDefined

  def isHuman = !isAi

  def hasUser = userId.isDefined

  def wins = isWinner getOrElse false

  def goBerserk = copy(berserk = true)

  def before(other: Player) = ((rating, id), (other.rating, other.id)) match {
    case ((Some(a), _), (Some(b), _)) if a != b => a > b
    case ((Some(_), _), (None, _))              => true
    case ((None, _), (Some(_), _))              => false
    case ((_, a), (_, b))                       => a < b
  }

  def ratingAfter = rating map (_ + ratingDiff.getOrElse(0))
}

object Player {

  object BSONFields {

    val aiLevel           = "ai"
    val isOfferingDraw    = "od"
    val isOfferingRematch = "or"
    val lastDrawOffer     = "ld"
    val proposeTakebackAt = "ta"
    val rating            = "e"
    val ratingDiff        = "d"
    val provisional       = "p"
    val berserk           = "be"
    val name              = "na"
  }

  import reactivemongo.api.bson.*
  import lila.db.BSON

  type Id      = String
  type UserId  = Option[String]
  type Win     = Option[Boolean]
  type Builder = Color => Id => UserId => Win => Player

  private def safeRange(range: Range, name: String)(userId: Option[String])(v: Int): Option[Int] =
    if (range contains v) Some(v)
    else {
      println(s"Player $userId $name=$v (range: ${range.min}-${range.max})")
      None
    }

  private val ratingRange     = safeRange(0 to 4000, "rating") _
  private val ratingDiffRange = safeRange(-1000 to 1000, "ratingDiff") _

  def from(light: LightGame, color: Color, ids: String, doc: Bdoc): Player =
    import BSONFields.*
    val p = light.player(color)
    Player(
      id = GamePlayerId(color.fold(ids take 4, ids drop 4)),
      color = p.color,
      aiLevel = p.aiLevel,
      isWinner = light.win.map(_ == color),
      isOfferingDraw = doc booleanLike isOfferingDraw getOrElse false,
      proposeTakebackAt = Ply(doc int proposeTakebackAt getOrElse 0),
      userId = p.userId,
      rating = p.rating,
      ratingDiff = p.ratingDiff,
      provisional = p.provisional,
      blurs = doc.getAsOpt[Blurs](blursBits) getOrElse Blurs.zeroBlurs.zero,
      berserk = p.berserk,
      name = doc string name
    )
}
