package lila
package analyse

import chess.Color
import chess.format.Uci

import lila.tree.Eval

case class Info(
    ply: Int,
    eval: Eval,
    // variation is first in UCI, then converted to PGN before storage
    variation: List[String] = Nil
) {

  def cp = eval.cp
  def mate = eval.mate
  def best = eval.best

  def turn = 1 + (ply - 1) / 2

  def color = Color(ply % 2 == 1)

  def hasVariation = variation.nonEmpty
  def dropVariation = copy(variation = Nil, eval = eval.dropBest)

  def invert = copy(eval = eval.invert)

  def cpComment: Option[String] = cp map (_.showPawns)
  def mateComment: Option[String] = mate map { m => s"Mate in ${math.abs(m.value)}" }
  def evalComment: Option[String] = cpComment orElse mateComment

  def isEmpty = cp.isEmpty && mate.isEmpty

  def forceCentipawns: Option[Int] = mate match {
    case None => cp.map(_.centipawns)
    case Some(m) if m.negative => Some(Int.MinValue - m.value)
    case Some(m) => Some(Int.MaxValue - m.value)
  }
}

object Info {

  import Eval.{ Cp, Mate }

  val LineMaxPlies = 14

  private val separator = ","
  private val listSeparator = ";"

  def start(ply: Int) = Info(ply, Eval.initial, Nil)

  private def strCp(s: String) = parseIntOption(s) map Cp.apply
  private def strMate(s: String) = parseIntOption(s) map Mate.apply

  private def decode(ply: Int, str: String): Option[Info] = str.split(separator) match {
    case Array() => Some(Info(ply, Eval.empty))
    case Array(cp) => Some(Info(ply, Eval(strCp(cp), None, None)))
    case Array(cp, ma) => Some(Info(ply, Eval(strCp(cp), strMate(ma), None)))
    case Array(cp, ma, va) => Some(Info(ply, Eval(strCp(cp), strMate(ma), None), va.split(' ').toList))
    case Array(cp, ma, va, be) => Some(Info(ply, Eval(strCp(cp), strMate(ma), Uci.Move piotr be), va.split(' ').toList))
    case _ => None
  }

  def decodeList(str: String, fromPly: Int): Option[List[Info]] = {
    str.split(listSeparator).toList.zipWithIndex map {
      case (infoStr, index) => decode(index + 1 + fromPly, infoStr)
    }
  }.sequence

  def apply(cp: Option[Cp], mate: Option[Mate], variation: List[String]): Int => Info =
    ply => Info(ply, Eval(cp, mate, None), variation)
}
