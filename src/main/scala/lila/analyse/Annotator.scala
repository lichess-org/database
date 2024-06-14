package lila
package analyse

import chess.format.pgn.{ Glyphs, Pgn }
import chess.{ Clock, Color, Ply, Status }

object Annotator:

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
    advices.foldLeft(p) { (pgn, advice) =>
      pgn
        .modifyInMainline(
          Ply(advice.ply),
          node => node.copy(value = node.value.copy(glyphs = Glyphs.fromList(advice.judgment.glyph :: Nil)))
        )
        .getOrElse(pgn)
    }

  private def addEvals(p: Pgn, infos: List[Info]): Pgn =
    infos.foldLeft(p) { (pgn, info) =>
      pgn
        .updatePly(
          Ply(info.ply),
          move => move.copy(comments = info.pgnComment.toList ::: move.comments)
        )
        .getOrElse(pgn)
    }
