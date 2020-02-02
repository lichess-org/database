package lila
package analyse

import chess.format.pgn.{ Glyphs, Move, Pgn, Tag, Turn }
import chess.{ Clock, Color, Status }

object Annotator {

  def apply(
      p: Pgn,
      analysis: Option[Analysis],
      winner: Option[Color],
      status: Status,
      clock: Option[Clock]
  ): Pgn =
    addEvals(
      addGlyphs(p, analysis.fold(List.empty[Advice])(_.advices)),
      analysis.fold(List.empty[Info])(_.infos)
    )

  private def addGlyphs(p: Pgn, advices: List[Advice]): Pgn =
    advices.foldLeft(p) {
      case (pgn, advice) =>
        pgn.updateTurn(
          advice.turn,
          turn =>
            turn
              .update(advice.color, move => move.copy(glyphs = Glyphs.fromList(advice.judgment.glyph :: Nil)))
        )
    }

  private def addEvals(p: Pgn, infos: List[Info]): Pgn =
    infos.foldLeft(p) {
      case (pgn, info) =>
        pgn.updateTurn(
          info.turn,
          turn =>
            turn.update(
              info.color,
              move => {
                val comment = info.cp
                  .map(_.pawns.toString)
                  .orElse(info.mate.map(m => s"#${m.value}"))
                move.copy(comments = comment.map(c => s"[%eval $c]").toList ::: move.comments)
              }
            )
        )
    }

  private def makeVariation(turn: Turn, advice: Advice): List[Turn] =
    Turn.fromMoves(advice.info.variation take 20 map { san =>
      Move(san)
    }, turn plyOf advice.color)
}
