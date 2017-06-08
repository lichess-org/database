package lila.game

import chess.Speed
import lila.rating.PerfType

object PerfPicker {

  def key(speed: Speed, variant: chess.variant.Variant, daysPerTurn: Option[Int]): String =
    if (variant.standard) {
      if (daysPerTurn.isDefined || speed == Speed.Correspondence) PerfType.Correspondence.key
      else speed.key
    }
    else variant.key

  def key(game: Game): String = key(game.speed, game.ratingVariant, game.daysPerTurn)
}
