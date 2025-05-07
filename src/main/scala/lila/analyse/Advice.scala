package lila.analyse

import chess.format.pgn.Glyph
import lila.tree.Eval.*

sealed trait Advice:
  def judgment: Advice.Judgment
  def info: Info
  def prev: Info

  def ply   = info.ply
  def turn  = info.turn
  def color = info.color
  def cp    = info.cp
  def mate  = info.mate

  def makeComment(withEval: Boolean, withBestMove: Boolean): String =
    evalComment.filter(_ => withEval).fold("") { c =>
      s"($c) "
    } +
      (this match
        case MateAdvice(seq, _, _, _) => seq.desc
        case CpAdvice(judgment, _, _) => judgment.toString) + "." + {
        info.variation.headOption.filter(_ => withBestMove).fold("") { move =>
          s" Best move was $move."
        }
      }

  def evalComment: Option[String] =
    Some {
      List(prev.evalComment, info.evalComment).flatten.mkString(" → ")
    }.filter(_.nonEmpty)

object Advice:

  sealed abstract class Judgment(val glyph: Glyph, val name: String):
    override def toString = name
    def isBlunder         = this == Judgment.Blunder
  object Judgment:
    object Inaccuracy extends Judgment(Glyph.MoveAssessment.dubious, "Inaccuracy")
    object Mistake    extends Judgment(Glyph.MoveAssessment.mistake, "Mistake")
    object Blunder    extends Judgment(Glyph.MoveAssessment.blunder, "Blunder")
    val all = List(Inaccuracy, Mistake, Blunder)

  def apply(prev: Info, info: Info): Option[Advice] = CpAdvice(prev, info).orElse(MateAdvice(prev, info))

private[analyse] case class CpAdvice(judgment: Advice.Judgment, info: Info, prev: Info) extends Advice

private[analyse] object CpAdvice:

  private val cpJudgments =
    List(300 -> Advice.Judgment.Blunder, 100 -> Advice.Judgment.Mistake, 50 -> Advice.Judgment.Inaccuracy)

  def apply(prev: Info, info: Info): Option[CpAdvice] =
    for
      cp     <- prev.cp.map(_.ceiled.centipawns)
      infoCp <- info.cp.map(_.ceiled.centipawns)
      delta =
        val d = infoCp - cp
        info.color.fold(-d, d)
      judgment <- cpJudgments.find((d, _) => d <= delta).map(_._2)
    yield CpAdvice(judgment, info, prev)

sealed abstract private[analyse] class MateSequence(val desc: String)
private[analyse] case object MateDelayed extends MateSequence(desc = "Not the best checkmate sequence")
private[analyse] case object MateLost    extends MateSequence(desc = "Lost forced checkmate sequence")
private[analyse] case object MateCreated extends MateSequence(desc = "Checkmate is now unavoidable")

private[analyse] object MateSequence:
  def apply(prev: Option[Mate], next: Option[Mate]): Option[MateSequence] =
    Some(prev, next).collect {
      case (None, Some(n)) if n.negative                              => MateCreated
      case (Some(p), None) if p.positive                              => MateLost
      case (Some(p), Some(n)) if p.positive && n.negative             => MateLost
      case (Some(p), Some(n)) if p.positive && n >= p && p <= Mate(5) => MateDelayed
    }
private[analyse] case class MateAdvice(
    sequence: MateSequence,
    judgment: Advice.Judgment,
    info: Info,
    prev: Info
) extends Advice
private[analyse] object MateAdvice:

  def apply(prev: Info, info: Info): Option[MateAdvice] =
    def invertCp(cp: Cp)       = cp.invertIf(info.color.black)
    def invertMate(mate: Mate) = mate.invertIf(info.color.black)
    def prevCp                 = prev.cp.map(invertCp).fold(0)(_.centipawns)
    def nextCp                 = info.cp.map(invertCp).fold(0)(_.centipawns)
    MateSequence(prev.mate.map(invertMate), info.mate.map(invertMate)).map { sequence =>
      import Advice.Judgment.*
      val judgment = sequence match
        case MateCreated if prevCp < -999 => Inaccuracy
        case MateCreated if prevCp < -700 => Mistake
        case MateCreated                  => Blunder
        case MateLost if nextCp > 999     => Inaccuracy
        case MateLost if nextCp > 700     => Mistake
        case MateLost                     => Blunder
        case MateDelayed                  => Inaccuracy
      MateAdvice(sequence, judgment, info, prev)
    }
