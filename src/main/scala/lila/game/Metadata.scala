package lila.game

import chess.{ Color, Ply }

private[game] case class Metadata(
    source: Option[Source],
    tournamentId: Option[String],
    swissId: Option[String],
    simulId: Option[String],
    analysed: Boolean,
    drawOffers: GameDrawOffers
)

case class GameDrawOffers(white: Set[Ply], black: Set[Ply]):
  def isEmpty = white.isEmpty && black.isEmpty

  // lichess allows to offer draw on either turn,
  // normalize to pretend it was done on the opponent turn.
  private def normalize(color: Color): Set[Ply] = color
    .fold(white, black)
    .map: ply =>
      if ply.turn == color then ply + 1 else ply

  def normalizedPlies: Set[Ply] = normalize(Color.white) ++ normalize(Color.black)
