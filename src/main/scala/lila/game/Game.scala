package lila.game

import scala.concurrent.duration._

import chess.Color.{ White, Black }
import chess.format.{ Uci, FEN }
import chess.opening.{ FullOpening, FullOpeningDB }
import chess.variant.{ Variant, Crazyhouse }
import chess.{ History => ChessHistory, CheckCount, Castles, Board, MoveOrDrop, Pos, Game => ChessGame, Clock, Status, Color, Mode, PositionHash, UnmovedRooks, Centis }
import org.joda.time.DateTime

import lila.common.Sequence
import lila.db.ByteArray

case class Game(
    id: String,
    whitePlayer: Player,
    blackPlayer: Player,
    binaryPgn: ByteArray,
    status: Status,
    turns: Int, // = ply
    startedAtTurn: Int,
    clock: Option[Clock] = None,
    daysPerTurn: Option[Int],
    checkCount: CheckCount = CheckCount(0, 0),
    binaryMoveTimes: Option[ByteArray] = None,
    clockHistory: Option[ClockHistory] = Option(ClockHistory()),
    mode: Mode = Mode.default,
    variant: Variant = Variant.default,
    crazyData: Option[Crazyhouse.Data] = None,
    createdAt: DateTime = DateTime.now,
    movedAt: DateTime = DateTime.now,
    metadata: Metadata
) {

  val players = List(whitePlayer, blackPlayer)

  def player(color: Color): Player = color match {
    case White => whitePlayer
    case Black => blackPlayer
  }

  def player(playerId: String): Option[Player] =
    players find (_.id == playerId)

  def player(c: Color.type => Color): Player = player(c(Color))

  def isPlayerFullId(player: Player, fullId: String): Boolean =
    (fullId.size == Game.fullIdSize) && player.id == (fullId drop 8)

  def player: Player = player(turnColor)

  def playerByUserId(userId: String): Option[Player] = players.find(_.userId contains userId)

  def opponent(p: Player): Player = opponent(p.color)

  def opponent(c: Color): Player = player(!c)

  lazy val firstColor = Color(whitePlayer before blackPlayer)
  def firstPlayer = player(firstColor)
  def secondPlayer = player(!firstColor)

  def turnColor = Color((turns & 1) == 0)

  def turnOf(p: Player): Boolean = p == player
  def turnOf(c: Color): Boolean = c == turnColor

  def playedTurns = turns - startedAtTurn

  def flagged = if (status == Status.Outoftime) Some(turnColor) else None

  def tournamentId = metadata.tournamentId
  def simulId = metadata.simulId

  def isTournament = tournamentId.isDefined
  def isSimul = simulId.isDefined
  def isMandatory = isTournament || isSimul
  def nonMandatory = !isMandatory

  def hasChat = !isTournament && !isSimul && nonAi

  def everyOther[A](l: List[A]): List[A] = l match {
    case a :: b :: tail => a :: everyOther(tail)
    case _ => l
  }

  def moveTimes(color: Color): Option[List[Centis]] = {
    for {
      clk <- clock
      inc = clk.incrementOf(color)
      history <- clockHistory
      clocks = history(color)
    } yield Centis(0) :: {
      val pairs = clocks.iterator zip clocks.iterator.drop(1)

      // We need to determine if this color's last clock had inc applied.
      // if finished and history.size == playedTurns then game was ended
      // by a players move, such as with mate or autodraw. In this case,
      // the last move of the game, and the only one without inc, is the
      // last entry of the clock history for !turnColor.
      //
      // On the other hand, if history.size is more than playedTurns,
      // then the game ended during a players turn by async event, and
      // the last recorded time is in the history for turnColor.
      val noLastInc = finished && (history.size <= playedTurns) == (color != turnColor)

      pairs map {
        case (first, second) => {
          val d = first - second
          if (pairs.hasNext || !noLastInc) d + inc else d
        } nonNeg
      } toList
    }
  } orElse binaryMoveTimes.map { binary =>
    // TODO: make movetime.read return List after writes are disabled.
    val base = BinaryFormat.moveTime.read(binary, playedTurns)
    val mts = if (color == startColor) base else base.drop(1)
    everyOther(mts.toList)
  }

  def moveTimes: Option[Vector[Centis]] = for {
    a <- moveTimes(startColor)
    b <- moveTimes(!startColor)
  } yield Sequence.interleave(a, b)

  def bothClockStates: Option[Vector[Centis]] = clockHistory.map(_ bothClockStates startColor)

  lazy val pgnMoves: PgnMoves = BinaryFormat.pgn read binaryPgn

  def openingPgnMoves(nb: Int): PgnMoves = BinaryFormat.pgn.read(binaryPgn, nb)

  def pgnMoves(color: Color): PgnMoves = {
    val pivot = if (color == startColor) 0 else 1
    pgnMoves.zipWithIndex.collect {
      case (e, i) if (i % 2) == pivot => e
    }
  }

  def correspondenceClock: Option[CorrespondenceClock] = daysPerTurn map { days =>
    val increment = days * 24 * 60 * 60
    val secondsLeft = (movedAt.getMillis / 1000 + increment - System.currentTimeMillis() / 1000).toInt max 0
    CorrespondenceClock(
      increment = increment,
      whiteTime = turnColor.fold(secondsLeft, increment),
      blackTime = turnColor.fold(increment, secondsLeft)
    )
  }

  def speed = chess.Speed(clock.map(_.config))

  def started = status >= Status.Started

  def notStarted = !started

  def aborted = status == Status.Aborted

  def playedThenAborted = aborted && bothPlayersHaveMoved

  def playable = status < Status.Aborted && !imported

  def playableEvenImported = status < Status.Aborted

  def playableBy(p: Player): Boolean = playable && turnOf(p)

  def playableBy(c: Color): Boolean = playableBy(player(c))

  def playableByAi: Boolean = playable && player.isAi

  def mobilePushable = isCorrespondence && playable && nonAi

  def alarmable = hasCorrespondenceClock && playable && nonAi

  def continuable = status != Status.Mate && status != Status.Stalemate

  def aiLevel: Option[Int] = players find (_.isAi) flatMap (_.aiLevel)

  def hasAi: Boolean = players.exists(_.isAi)
  def nonAi = !hasAi

  def aiPov: Option[Pov] = players.find(_.isAi).map(_.color) map pov

  def mapPlayers(f: Player => Player) = copy(
    whitePlayer = f(whitePlayer),
    blackPlayer = f(blackPlayer)
  )

  def boosted = rated && finished && bothPlayersHaveMoved && playedTurns < 10

  def abortable = status == Status.Started && playedTurns < 2 && nonMandatory

  def resignable = playable && !abortable
  def drawable = playable && !abortable

  def rated = mode.rated
  def casual = !rated

  def finished = status >= Status.Mate

  def finishedOrAborted = finished || aborted

  def accountable = playedTurns >= 2 || isTournament

  def replayable = finished || (aborted && bothPlayersHaveMoved)

  def analysable =
    replayable && playedTurns > 4 &&
      Game.analysableVariants(variant) &&
      !Game.isOldHorde(this)

  def ratingVariant =
    if (isTournament && variant == chess.variant.FromPosition) chess.variant.Standard
    else variant

  def fromPosition = variant == chess.variant.FromPosition || source.contains(Source.Position)

  def imported = source contains Source.Import

  def fromPool = source contains Source.Pool
  def fromLobby = source contains Source.Lobby

  def winner = players find (_.wins)

  def loser = winner map opponent

  def winnerColor: Option[Color] = winner map (_.color)

  def winnerUserId: Option[String] = winner flatMap (_.userId)

  def loserUserId: Option[String] = loser flatMap (_.userId)

  def wonBy(c: Color): Option[Boolean] = winnerColor map (_ == c)

  def lostBy(c: Color): Option[Boolean] = winnerColor map (_ != c)

  def drawn = finished && winner.isEmpty

  def isCorrespondence = speed == chess.Speed.Correspondence

  def isSwitchable = nonAi && (isCorrespondence || isSimul)

  def hasClock = clock.isDefined

  def hasCorrespondenceClock = daysPerTurn.isDefined

  def isUnlimited = !hasClock && !hasCorrespondenceClock

  def estimateClockTotalTime = clock.map(_.estimateTotalSeconds)

  def estimateTotalTime = estimateClockTotalTime orElse
    correspondenceClock.map(_.estimateTotalTime) getOrElse 1200

  def onePlayerHasMoved = playedTurns > 0
  def bothPlayersHaveMoved = playedTurns > 1

  def startColor = Color(startedAtTurn % 2 == 0)

  def playerMoves(color: Color): Int =
    if (color == startColor) (playedTurns + 1) / 2
    else playedTurns / 2

  def playerHasMoved(color: Color) = playerMoves(color) > 0

  def isBeingPlayed = !finishedOrAborted

  def olderThan(seconds: Int) = movedAt isBefore DateTime.now.minusSeconds(seconds)

  def unplayed = !bothPlayersHaveMoved && (createdAt isBefore Game.unplayedDate)

  def forecastable = started && playable && isCorrespondence && !hasAi

  def userIds = playerMaps(_.userId)

  def userRatings = playerMaps(_.rating)

  def averageUsersRating = userRatings match {
    case a :: b :: Nil => Some((a + b) / 2)
    case a :: Nil => Some((a + 1500) / 2)
    case _ => None
  }

  def source = metadata.source

  def resetTurns = copy(turns = 0, startedAtTurn = 0)

  def synthetic = id == Game.syntheticId

  private def playerMaps[A](f: Player => Option[A]): List[A] = players flatMap { f(_) }

  def pov(c: Color) = Pov(this, c)
  def whitePov = pov(White)
  def blackPov = pov(Black)
}

object Game {

  type ID = String

  case class WithInitialFen(game: Game, fen: Option[FEN])

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

  val unanalysableVariants: Set[Variant] = Variant.all.toSet -- analysableVariants

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

  val hordeWhitePawnsSince = new DateTime(2015, 4, 11, 10, 0)

  def isOldHorde(game: Game) =
    game.variant == chess.variant.Horde &&
      game.createdAt.isBefore(Game.hordeWhitePawnsSince)

  def allowRated(variant: Variant, clock: Clock.Config) =
    variant.standard || clock.estimateTotalTime >= Centis(3000)

  val gameIdSize = 8
  val playerIdSize = 4
  val fullIdSize = 12
  val tokenSize = 4

  val unplayedHours = 24
  def unplayedDate = DateTime.now minusHours unplayedHours

  val abandonedDays = 21
  def abandonedDate = DateTime.now minusDays abandonedDays

  val aiAbandonedHours = 6
  def aiAbandonedDate = DateTime.now minusHours aiAbandonedHours

  def takeGameId(fullId: String) = fullId take gameIdSize
  def takePlayerId(fullId: String) = fullId drop gameIdSize

  object BSONFields {

    val id = "_id"
    val whitePlayer = "p0"
    val blackPlayer = "p1"
    val playerIds = "is"
    val playerUids = "us"
    val playingUids = "pl"
    val binaryPgn = "pg"
    val status = "s"
    val turns = "t"
    val startedAtTurn = "st"
    val clock = "c"
    val checkCount = "cc"
    val daysPerTurn = "cd"
    val moveTimes = "mt"
    val whiteClockHistory = "cw"
    val blackClockHistory = "cb"
    val rated = "ra"
    val analysed = "an"
    val variant = "v"
    val crazyData = "chd"
    val createdAt = "ca"
    val movedAt = "ua" // ua = updatedAt (bc)
    val source = "so"
    val tournamentId = "tid"
    val simulId = "sid"
    val winnerColor = "w"
    val winnerId = "wid"
    val initialFen = "if"
  }
}

case class CastleLastMoveTime(
    castles: Castles,
    lastMove: Option[(Pos, Pos)],
    check: Option[Pos]
) {

  def lastMoveString = lastMove map { case (a, b) => s"$a$b" }
}

object CastleLastMoveTime {

  def init = CastleLastMoveTime(Castles.all, None, None)

  import reactivemongo.bson._
  import lila.db.ByteArray.ByteArrayBSONHandler

  private[game] implicit val castleLastMoveTimeBSONHandler = new BSONHandler[BSONBinary, CastleLastMoveTime] {
    def read(bin: BSONBinary) = BinaryFormat.castleLastMoveTime read {
      ByteArrayBSONHandler read bin
    }
    def write(clmt: CastleLastMoveTime) = ByteArrayBSONHandler write {
      BinaryFormat.castleLastMoveTime write clmt
    }
  }
}

case class ClockHistory(
    white: Vector[Centis] = Vector.empty,
    black: Vector[Centis] = Vector.empty
) {

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
}
