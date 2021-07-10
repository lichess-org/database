package lila.game

import chess.format.pgn.Move
import chess.format.pgn.Pgn
import chess.format.pgn.Tag
import chess.format.pgn.Tags
import chess.format.pgn.Turn
import lila.analyse.Analysis

/*
 * bits of scalachess copied over here
 * so that clock readings are in centiseconds
 */
object UnivPgn {

  def render(pgn: Pgn, analysis: Option[Analysis]): String = {
    import pgn._
    val initStr =
      if (initial.comments.nonEmpty) initial.comments.mkString("{ ", " } { ", " }\n")
      else ""
    val turnStr   = turns.map(renderTurn) mkString " "
    val resultStr = tags(_.Result) getOrElse ""
    val endStr =
      if (turnStr.nonEmpty) s" $resultStr"
      else resultStr
    val berserkTag = makeBerserkTag(pgn).fold("")(t => t + "\n")
    val anaReqTag  = analysis.map(makeAnalysisRequesterTag).fold("")(t => t + "\n")
    s"$tags\n$berserkTag$anaReqTag\n$initStr$turnStr$endStr"
  }.trim

  private def makeBerserkTag(pgn: Pgn): Option[String] =
    if (pgn.tags(_.Event).exists(_ contains "/tournament/")) {
      val initialCentis = pgn.tags.clockConfig.fold(0)(_.limit.centis)
      val w             = pgn.turns.headOption.exists(_.white.exists(_.secondsLeft.exists(_ < initialCentis)))
      val b             = pgn.turns.headOption.exists(_.black.exists(_.secondsLeft.exists(_ < initialCentis)))
      val colors = List("white" -> w, "black" -> b).collect { case (c, true) =>
        c
      } mkString " "
      if (colors.nonEmpty) Some(s"""[Berserk "$colors"]""") else None
    } else None

  private def makeAnalysisRequesterTag(analysis: Analysis) =
    s"""[AnalysisRequester "${analysis.uid getOrElse "?"}"]"""

  private def renderTurn(turn: Turn): String = {
    import turn._
    val text = (white, black) match {
      case (Some(w), Some(b)) if w.isLong => s" ${renderMove(w)} $number... ${renderMove(b)}"
      case (Some(w), Some(b))             => s" ${renderMove(w)} ${renderMove(b)}"
      case (Some(w), None)                => s" ${renderMove(w)}"
      case (None, Some(b))                => s".. ${renderMove(b)}"
      case _                              => ""
    }
    s"$number.$text"
  }

  private def renderMove(move: Move) = {
    import move._
    val glyphStr = glyphs.toList
      .map({
        case glyph if glyph.id <= 6 => glyph.symbol
        case glyph                  => s" $$${glyph.id}"
      })
      .mkString
    val clockString: Option[String] =
      secondsLeft.map(centis => s"[%clkc $centis]")
    val commentsOrTime =
      if (comments.nonEmpty || secondsLeft.isDefined || opening.isDefined || result.isDefined)
        List(clockString, opening, result).flatten
          .:::(comments map noDoubleLineBreak)
          .map { text =>
            s" { $text }"
          }
          .mkString
      else ""
    val variationString =
      if (variations.isEmpty) ""
      else variations.map(_.mkString(" (", " ", ")")).mkString(" ")
    s"$san$glyphStr$commentsOrTime$variationString"
  }

  private val noDoubleLineBreakRegex = "(\r?\n){2,}".r

  private def noDoubleLineBreak(txt: String) =
    noDoubleLineBreakRegex.replaceAllIn(txt, "\n")
}
