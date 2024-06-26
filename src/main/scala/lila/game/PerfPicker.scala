package lila.game

import chess.Speed
import lila.rating.PerfType

object PerfPicker:

  def key(speed: Speed, variant: chess.variant.Variant, daysPerTurn: Option[Int]): String =
    if variant.standard then
      if daysPerTurn.isDefined || speed == Speed.Correspondence then PerfType.Correspondence.key
      else speed.key.value
    else variant.key.value

  def key(game: Game): String = key(game.speed, game.ratingVariant, game.daysPerTurn)
