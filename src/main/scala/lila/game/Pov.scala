package lila.game

import chess.Color

case class Pov(game: Game, color: Color) {

  def player = game player color

  def playerId = player.id

  def gameId = game.id

  def opponent = game player !color

  def isFirstPlayer = game.firstPlayer.color == color

  def unary_! = Pov(game, !color)

  def ref = PovRef(game.id, color)

  def withGame(g: Game) = copy(game = g)
  def withColor(c: Color) = copy(color = c)

  lazy val isMyTurn = game.started && game.playable && game.turnColor == color

  def hasMoved = game playerHasMoved color

  def win = game wonBy color

  def loss = game lostBy color

  def forecastable = game.forecastable && game.turnColor != color

  override def toString = ref.toString
}

object Pov {

  def apply(game: Game): List[Pov] = game.players.map { apply(game, _) }

  def first(game: Game) = apply(game, if (!game.variant.racingKings) game.firstPlayer else game.whitePlayer)
  def second(game: Game) = apply(game, if (!game.variant.racingKings) game.secondPlayer else game.blackPlayer)
  def white(game: Game) = apply(game, game.whitePlayer)
  def black(game: Game) = apply(game, game.blackPlayer)
  def player(game: Game) = apply(game, game.player)

  def apply(game: Game, player: Player) = new Pov(game, player.color)

  def apply(game: Game, playerId: String): Option[Pov] =
    game player playerId map { apply(game, _) }

  def ofUserId(game: Game, userId: String): Option[Pov] =
    game playerByUserId userId map { apply(game, _) }

  def opponentOfUserId(game: Game, userId: String): Option[Player] =
    ofUserId(game, userId) map (_.opponent)
}

case class PovRef(gameId: String, color: Color) {

  def unary_! = PovRef(gameId, !color)

  override def toString = s"$gameId/${color.name}"
}

case class PlayerRef(gameId: String, playerId: String)

object PlayerRef {

  def apply(fullId: String): PlayerRef = PlayerRef(Game takeGameId fullId, Game takePlayerId fullId)
}
