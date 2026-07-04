package lila
package analyse

import chess.format.pgn.{ Glyphs, Pgn, Comment }
import chess.Ply
import lila.game.{ Game, GameDrawOffers }

object Annotator:

  def apply(
      p: Pgn,
      game: Game,
      analysis: Option[Analysis]
  ): Pgn =
    addDrawOffers(
      addEvals(
        addGlyphs(p, analysis.fold(List.empty[Advice])(_.advices)),
        analysis.fold(List.empty[Info])(_.infos)
      ),
      game.metadata.drawOffers
    )

  private def addDrawOffers(pgn: Pgn, drawOffers: GameDrawOffers): Pgn =
    if drawOffers.isEmpty then pgn
    else
      drawOffers.normalizedPlies.foldLeft(pgn): (pgn, ply) =>
        pgn
          .updatePly(
            ply,
            move => move.copy(comments = Comment(s"${!ply.turn} offers draw") :: move.comments)
          )
          .getOrElse(pgn)

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
