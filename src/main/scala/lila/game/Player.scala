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
):

  def playerUser = userId.flatMap { uid =>
    rating.map { PlayerUser(uid, _, ratingDiff) }
  }

  def isAi = aiLevel.isDefined

  def isHuman = !isAi

  def hasUser = userId.isDefined

  def wins = isWinner.getOrElse(false)

  def ratingAfter = rating.map(_ + ratingDiff.getOrElse(0))

object Player:

  object BSONFields:

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

  type Id      = String
  type UserId  = Option[String]
  type Win     = Option[Boolean]
  type Builder = Color => Id => UserId => Win => Player

  def from(light: LightGame, color: Color, ids: String, doc: lila.db.dsl.Bdoc): Player =
    import BSONFields.*
    val p = light.player(color)
    Player(
      id = color.fold(ids.take(4), ids.drop(4)),
      color = p.color,
      aiLevel = p.aiLevel,
      isWinner = light.win.map(_ == color),
      userId = p.userId,
      rating = p.rating,
      ratingDiff = p.ratingDiff,
      provisional = p.provisional,
      berserk = p.berserk,
      name = doc.string(name)
    )
