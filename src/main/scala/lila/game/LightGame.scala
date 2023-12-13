package lila
package game

import chess.{ Color, Status }

case class LightGame(
    id: String,
    whitePlayer: LightPlayer,
    blackPlayer: LightPlayer,
    status: Status,
    win: Option[Color]
):
  def playable                                            = status < Status.Aborted
  def player(color: Color): LightPlayer                   = color.fold(whitePlayer, blackPlayer)
  def players                                             = List(whitePlayer, blackPlayer)
  def playerByUserId(userId: String): Option[LightPlayer] = players.find(_.userId contains userId)
  def finished                                            = status >= Status.Mate

object LightGame:

  import Game.BSONFields as F

  def projection =
    lila.db.dsl.$doc(
      F.whitePlayer -> true,
      F.blackPlayer -> true,
      F.playerUids  -> true,
      F.winnerColor -> true,
      F.status      -> true
    )

case class LightPlayer(
    color: Color,
    aiLevel: Option[Int],
    userId: Option[String] = None,
    rating: Option[Int] = None,
    ratingDiff: Option[Int] = None,
    provisional: Boolean = false,
    berserk: Boolean = false
)

object LightPlayer:

  import reactivemongo.api.bson.*
  import lila.db.dsl.*

  private[game] type Builder = Color => Option[String] => LightPlayer

  private def safeRange[A](range: Range)(a: A): Option[A] = range.contains(a) option a
  private val ratingRange                                 = safeRange[Int](0 to 4000)
  private val ratingDiffRange                             = safeRange[Int](-1000 to 1000)

  given lightPlayerReader: BSONDocumentReader[Builder] with
    import scala.util.{ Success, Try }
    def readDocument(doc: Bdoc): Try[Builder] = Success(builderRead(doc))

  def builderRead(doc: Bdoc): Builder = color =>
    userId =>
      import Player.BSONFields.*
      LightPlayer(
        color = color,
        aiLevel = doc int aiLevel,
        userId = userId,
        rating = doc.getAsOpt[Int](rating) flatMap ratingRange,
        ratingDiff = doc.getAsOpt[Int](ratingDiff) flatMap ratingDiffRange,
        provisional = ~doc.getAsOpt[Boolean](provisional),
        berserk = doc booleanLike berserk getOrElse false
      )
