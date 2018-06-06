package lila.rating

import chess.Centis

import chess.Speed

sealed abstract class PerfType(
    val id: Int,
    val key: String,
    val name: String,
    val title: String,
    val iconChar: Char
) {

  def iconString = iconChar.toString
}

object PerfType {

  case object UltraBullet extends PerfType(
    0,
    key = "ultraBullet",
    name = Speed.UltraBullet.name,
    title = Speed.UltraBullet.title,
    iconChar = '{'
  )

  case object Bullet extends PerfType(
    1,
    key = "bullet",
    name = Speed.Bullet.name,
    title = Speed.Bullet.title,
    iconChar = 'T'
  )

  case object Blitz extends PerfType(
    2,
    key = "blitz",
    name = Speed.Blitz.name,
    title = Speed.Blitz.title,
    iconChar = ')'
  )

  case object Rapid extends PerfType(
    6,
    key = "rapid",
    name = Speed.Rapid.name,
    title = Speed.Rapid.title,
    iconChar = '#'
  )

  case object Classical extends PerfType(
    3,
    key = "classical",
    name = Speed.Classical.name,
    title = Speed.Classical.title,
    iconChar = '+'
  )

  case object Correspondence extends PerfType(
    4,
    key = "correspondence",
    name = "Correspondence",
    title = "Correspondence (days per turn)",
    iconChar = ';'
  )

  case object Standard extends PerfType(
    5,
    key = "standard",
    name = chess.variant.Standard.name,
    title = "Standard rules of chess",
    iconChar = '8'
  )

  case object Chess960 extends PerfType(
    11,
    key = "chess960",
    name = chess.variant.Chess960.name,
    title = "Chess960 variant",
    iconChar = '''
  )

  case object KingOfTheHill extends PerfType(
    12,
    key = "kingOfTheHill",
    name = chess.variant.KingOfTheHill.name,
    title = "King of the Hill variant",
    iconChar = '('
  )

  case object Antichess extends PerfType(
    13,
    key = "antichess",
    name = chess.variant.Antichess.name,
    title = "Antichess variant",
    iconChar = '@'
  )

  case object Atomic extends PerfType(
    14,
    key = "atomic",
    name = chess.variant.Atomic.name,
    title = "Atomic variant",
    iconChar = '>'
  )

  case object ThreeCheck extends PerfType(
    15,
    key = "threeCheck",
    name = chess.variant.ThreeCheck.name,
    title = "Three-check variant",
    iconChar = '.'
  )

  case object Horde extends PerfType(
    16,
    key = "horde",
    name = chess.variant.Horde.name,
    title = "Horde variant",
    iconChar = '_'
  )

  case object RacingKings extends PerfType(
    17,
    key = "racingKings",
    name = chess.variant.RacingKings.name,
    title = "Racing kings variant",
    iconChar = ''
  )

  case object Crazyhouse extends PerfType(
    18,
    key = "crazyhouse",
    name = chess.variant.Crazyhouse.name,
    title = "Crazyhouse variant",
    iconChar = ''
  )

  case object Puzzle extends PerfType(
    20,
    key = "puzzle",
    name = "Training",
    title = "Training puzzles",
    iconChar = '-'
  )

  val all: List[PerfType] = List(UltraBullet, Bullet, Blitz, Rapid, Classical, Correspondence, Standard, Crazyhouse, Chess960, KingOfTheHill, ThreeCheck, Antichess, Atomic, Horde, RacingKings, Puzzle)
  val byKey = all map { p => (p.key, p) } toMap
  val byId = all map { p => (p.id, p) } toMap

  val default = Standard

  def apply(key: String): Option[PerfType] = byKey get key
  def orDefault(key: String): PerfType = apply(key) getOrElse default

  def apply(id: Int): Option[PerfType] = byId get id

  def name(key: String): Option[String] = apply(key) map (_.name)

  def id2key(id: Int): Option[String] = byId get id map (_.key)

  val nonPuzzle: List[PerfType] = List(UltraBullet, Bullet, Blitz, Rapid, Classical, Correspondence, Crazyhouse, Chess960, KingOfTheHill, ThreeCheck, Antichess, Atomic, Horde, RacingKings)
  val nonGame: List[PerfType] = List(Puzzle)
  val leaderboardable: List[PerfType] = List(Bullet, Blitz, Rapid, Classical, UltraBullet, Crazyhouse, Chess960, KingOfTheHill, ThreeCheck, Antichess, Atomic, Horde, RacingKings)
  val variants: List[PerfType] = List(Crazyhouse, Chess960, KingOfTheHill, ThreeCheck, Antichess, Atomic, Horde, RacingKings)
  val standard: List[PerfType] = List(Bullet, Blitz, Rapid, Classical, Correspondence)

  def isGame(pt: PerfType) = !nonGame.contains(pt)
}
