package lila
package game

import scalalib.extensions.*
import scala.language.postfixOps

import chess.Color.{ Black, White }
import chess.format.{ Fen, Uci }
import chess.opening.{ Opening, OpeningDb }
import chess.variant.Variant
import chess.format.pgn.SanStr
import chess.{ Board, ByColor, Castles, Centis, CheckCount, Clock, Color, Game as ChessGame, Mode, Status }

import lila.common.Sequence
import lila.db.ByteArray

case class Game(
    id: String,
    players: ByColor[Player],
    chess: ChessGame,
    status: Status,
    daysPerTurn: Option[Int],
    binaryMoveTimes: Option[ByteArray] = None,
    clockHistory: Option[ClockHistory] = Option(ClockHistory()),
    mode: Mode = Mode.default,
    createdAt: Instant,
    movedAt: Instant,
    metadata: Metadata
):
  export chess.{ clock, player as turnColor, ply, sans, board, startedAtPly }
  export chess.board.{ history, variant }

  def player: Player = players(turnColor)

  def playerByUserId(userId: String): Option[Player] =
    players.find(_.userId contains userId)
  def opponentByUserId(userId: String): Option[Player] =
    playerByUserId(userId).map(opponent)

  def opponent(p: Player): Player = opponent(p.color)

  def opponent(c: Color): Player = players(!c)

  def turnOf(p: Player): Boolean = p == player
  def turnOf(c: Color): Boolean  = c == turnColor

  def playedTurns = ply - startedAtPly

  def flagged = if status == Status.Outoftime then Some(turnColor) else None

  export metadata.*

  def isTournament = tournamentId.isDefined
  def isSimul      = simulId.isDefined
  def isMandatory  = isTournament || isSimul
  def nonMandatory = !isMandatory

  def hasChat = !isTournament && !isSimul && nonAi

  def everyOther[A](l: List[A]): List[A] = l match
    case a :: b :: tail => a :: everyOther(tail)
    case _              => l

  def moveTimes(color: Color): Option[List[Centis]] = {
    for
      clk <- clock
      inc = clk.incrementOf(color)
      history <- clockHistory
      clocks = history(color)
    yield Centis(0) :: {
      val pairs = clocks.iterator.zip(clocks.iterator.drop(1))

      // We need to determine if this color's last clock had inc applied.
      // if finished and history.size == playedTurns then game was ended
      // by a players move, such as with mate or autodraw. In this case,
      // the last move of the game, and the only one without inc, is the
      // last entry of the clock history for !turnColor.
      //
      // On the other hand, if history.size is more than playedTurns,
      // then the game ended during a players turn by async event, and
      // the last recorded time is in the history for turnColor.
      val noLastInc = finished && (history.size <= playedTurns.value) == (color != turnColor)

      pairs.map { case (first, second) =>
        {
          val d = first - second
          if pairs.hasNext || !noLastInc then d + inc else d
        }.nonNeg
      }.toList
    }
  }.orElse(binaryMoveTimes.map { binary =>
    // TODO: make movetime.read return List after writes are disabled.
    val base = BinaryFormat.moveTime.read(binary, playedTurns)
    val mts  = if color == startColor then base else base.drop(1)
    everyOther(mts.toList)
  })

  def moveTimes: Option[Vector[Centis]] =
    for
      a <- moveTimes(startColor)
      b <- moveTimes(!startColor)
    yield Sequence.interleave(a, b)

  def bothClockStates: Option[Vector[Centis]] =
    clockHistory.map(_.bothClockStates(startColor))

  def sansOf(color: Color): Vector[SanStr] =
    val pivot = if color == startColor then 0 else 1
    sans.zipWithIndex.collect:
      case (e, i) if (i % 2) == pivot => e

  def lastMoveKeys: Option[String] = history.lastMove.map {
    case Uci.Drop(target, _) => s"$target$target"
    case m: Uci.Move         => m.keys
  }

  def correspondenceClock: Option[CorrespondenceClock] =
    daysPerTurn.map: days =>
      val increment   = days * 24 * 60 * 60
      val secondsLeft = (movedAt.toSeconds + increment - nowSeconds).toInt.max(0)
      CorrespondenceClock(
        increment = increment,
        whiteTime = turnColor.fold(secondsLeft, increment).toFloat,
        blackTime = turnColor.fold(increment, secondsLeft).toFloat
      )

  def started = status >= Status.Started

  def notStarted = !started

  def aborted = status == Status.Aborted

  def playedThenAborted = aborted && bothPlayersHaveMoved

  def playable = status < Status.Aborted && !imported

  def playableEvenImported = status < Status.Aborted

  def alarmable = hasCorrespondenceClock && playable && nonAi

  def continuable = status != Status.Mate && status != Status.Stalemate

  def aiLevel: Option[Int] = players.find(_.isAi).flatMap(_.aiLevel)

  def hasAi: Boolean = players.exists(_.isAi)
  def nonAi          = !hasAi

  def aiPov: Option[Pov] = players.find(_.isAi).map(_.color).map(pov)

  def boosted = rated && finished && bothPlayersHaveMoved && playedTurns < 10

  def rated  = mode.rated
  def casual = !rated

  def finished = status >= Status.Mate

  def finishedOrAborted = finished || aborted

  def accountable = playedTurns >= 2 || isTournament

  def imported = source contains Source.Import

  def fromPool   = source contains Source.Pool
  def fromLobby  = source contains Source.Lobby
  def fromFriend = source contains Source.Friend

  def winner = players.find(_.wins)

  def loser = winner.map(opponent)

  def winnerColor: Option[Color] = winner.map(_.color)

  def winnerUserId: Option[String] = winner.flatMap(_.userId)

  def loserUserId: Option[String] = loser.flatMap(_.userId)

  def wonBy(c: Color): Option[Boolean] = winnerColor.map(_ == c)

  def lostBy(c: Color): Option[Boolean] = winnerColor.map(_ != c)

  def drawn = finished && winner.isEmpty

  def hasClock = clock.isDefined

  def hasCorrespondenceClock = daysPerTurn.isDefined

  def isUnlimited = !hasClock && !hasCorrespondenceClock

  def estimateClockTotalTime = clock.map(_.estimateTotalSeconds)

  def estimateTotalTime =
    estimateClockTotalTime.orElse(correspondenceClock.map(_.estimateTotalTime)).getOrElse(1200)

  def onePlayerHasMoved    = playedTurns > 0
  def bothPlayersHaveMoved = playedTurns > 1

  def startColor = startedAtPly.turn

  def ratingVariant =
    if isTournament && board.variant.fromPosition then _root_.chess.variant.Standard
    else variant

  def fromPosition = variant.fromPosition || source.contains(Source.Position)

  lazy val opening: Option[Opening.AtPly] =
    (!fromPosition && Variant.list.openingSensibleVariants(variant)).so(OpeningDb.search(sans))

  def synthetic = id == Game.syntheticId

  def pov(c: Color)        = Pov(this, c)
  def whitePov             = pov(White)
  def blackPov             = pov(Black)
  def playerPov(p: Player) = pov(p.color)
  def loserPov             = loser.map(playerPov)

  def speed = _root_.chess.Speed(clock.map(_.config))

  def perfKey  = PerfPicker.key(this)
  def perfType = lila.rating.PerfType(perfKey)

object Game:

  type ID = String

  case class WithInitialFen(game: Game, fen: Option[Fen.Full])

  val syntheticId = "synthetic"

  val maxPlayingRealtime = 100 // plus 200 correspondence games

  val analysableVariants: Set[Variant] = Set(
    chess.variant.Standard,
    chess.variant.Crazyhouse,
    chess.variant.Chess960,
    chess.variant.KingOfTheHill,
    chess.variant.ThreeCheck,
    chess.variant.Antichess,
    chess.variant.FromPosition,
    chess.variant.Horde,
    chess.variant.Atomic,
    chess.variant.RacingKings
  )

  val variantsWhereWhiteIsBetter: Set[Variant] = Set(
    chess.variant.ThreeCheck,
    chess.variant.Atomic,
    chess.variant.Horde,
    chess.variant.RacingKings,
    chess.variant.Antichess
  )

  val visualisableVariants: Set[Variant] = Set(
    chess.variant.Standard,
    chess.variant.Chess960
  )

  val hordeWhitePawnsSince = instantOf(2015, 4, 11, 10, 0)

  def isOldHorde(game: Game) =
    game.variant == chess.variant.Horde &&
      game.createdAt.isBefore(Game.hordeWhitePawnsSince)

  def allowRated(variant: Variant, clock: Clock.Config) =
    variant.standard || clock.estimateTotalTime >= Centis(3000)

  val gameIdSize   = 8
  val playerIdSize = 4
  val fullIdSize   = 12
  val tokenSize    = 4

  def takeGameId(fullId: String)   = fullId.take(gameIdSize)
  def takePlayerId(fullId: String) = fullId.drop(gameIdSize)

  private[game] val emptyCheckCount = CheckCount(0, 0)

  object BSONFields:

    val id                = "_id"
    val whitePlayer       = "p0"
    val blackPlayer       = "p1"
    val playerIds         = "is"
    val playerUids        = "us"
    val playingUids       = "pl"
    val binaryPieces      = "ps"
    val oldPgn            = "pg"
    val huffmanPgn        = "hp"
    val status            = "s"
    val turns             = "t"
    val startedAtTurn     = "st"
    val clock             = "c"
    val positionHashes    = "ph"
    val checkCount        = "cc"
    val castleLastMove    = "cl"
    val unmovedRooks      = "ur"
    val daysPerTurn       = "cd"
    val moveTimes         = "mt"
    val whiteClockHistory = "cw"
    val blackClockHistory = "cb"
    val rated             = "ra"
    val analysed          = "an"
    val variant           = "v"
    val crazyData         = "chd"
    val next              = "ne"
    val bookmarks         = "bm"
    val createdAt         = "ca"
    val movedAt           = "ua" // ua = updatedAt (bc)
    val source            = "so"
    val pgnImport         = "pgni"
    val tournamentId      = "tid"
    val simulId           = "sid"
    val swissId           = "iid"
    val tvAt              = "tv"
    val winnerColor       = "w"
    val winnerId          = "wid"
    val initialFen        = "if"
    val checkAt           = "ck"

case class CastleLastMove(castles: Castles, lastMove: Option[Uci])

object CastleLastMove:

  def init = CastleLastMove(Castles.init, None)

  import reactivemongo.api.bson.*
  import lila.db.dsl.*
  import lila.db.ByteArray.byteArrayHandler

  private[game] given castleLastMoveHandler: BSONHandler[CastleLastMove] = tryHandler[CastleLastMove](
    { case bin: BSONBinary =>
      byteArrayHandler.readTry(bin).map(BinaryFormat.castleLastMove.read)
    },
    clmt => byteArrayHandler.writeTry { BinaryFormat.castleLastMove.write(clmt) }.get
  )

case class ClockHistory(
    white: Vector[Centis] = Vector.empty,
    black: Vector[Centis] = Vector.empty
):

  def update(color: Color, f: Vector[Centis] => Vector[Centis]): ClockHistory =
    color.fold(copy(white = f(white)), copy(black = f(black)))

  def record(color: Color, clock: Clock): ClockHistory =
    update(color, _ :+ clock.remainingTime(color))

  def reset(color: Color) = update(color, _ => Vector.empty)

  def apply(color: Color): Vector[Centis] = color.fold(white, black)

  def last(color: Color) = apply(color).lastOption

  def size = white.size + black.size

  // first state is of the color that moved first.
  def bothClockStates(firstMoveBy: Color): Vector[Centis] =
    Sequence.interleave(
      firstMoveBy.fold(white, black),
      firstMoveBy.fold(black, white)
    )
