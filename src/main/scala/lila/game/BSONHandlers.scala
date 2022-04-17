package lila.game

import chess.variant.{ Crazyhouse, Variant }
import chess.{
  Black,
  CheckCount,
  Clock,
  Color,
  Game as ChessGame,
  History as ChessHistory,
  Mode,
  Status,
  UnmovedRooks,
  White
}
import lila.db.BSON
import lila.db.dsl.*
import org.joda.time.DateTime
import reactivemongo.api.bson.*
import scala.util.Try

object BSONHandlers:

  private[game] given BSONHandler[CastleLastMove] = new BSONHandler[CastleLastMove] {
    def readTry(bson: BSONValue) =
      bson match
        case bin: BSONBinary => lila.db.ByteArray.bsonHandler readTry bin map BinaryFormat.castleLastMove.read
        case b               => lila.db.BSON.handlerBadType(b)
    def writeTry(clmt: CastleLastMove) =
      lila.db.ByteArray.bsonHandler writeTry {
        BinaryFormat.castleLastMove write clmt
      }
  }

  given BSONHandler[Status] = tryHandler[Status](
    { case BSONInteger(v) =>
      Status(v)
        .fold[Try[Status]](scala.util.Failure(new Exception(s"No such status: $v")))(scala.util.Success.apply)
    },
    x => BSONInteger(x.id)
  )

  given BSONHandler[UnmovedRooks] = tryHandler[UnmovedRooks](
    { case bin: BSONBinary => ByteArrayBSONHandler.readTry(bin) map BinaryFormat.unmovedRooks.read },
    x => ByteArrayBSONHandler.writeTry(BinaryFormat.unmovedRooks write x).get
  )

  private[game] given BSONHandler[Crazyhouse.Data] =
    new BSON[Crazyhouse.Data] {

      import Crazyhouse._

      def reads(r: BSON.Reader) = Crazyhouse.Data(
        pockets = {
          val (white, black) = {
            r.str("p")
              .flatMap(chess.Piece.fromChar)
              .toList
          }.partition(_ is chess.White)
          Pockets(
            white = Pocket(white.map(_.role)),
            black = Pocket(black.map(_.role))
          )
        },
        promoted = r.str("t").flatMap(chess.Pos.piotr).toSet
      )
    }

  given BSON[Game] with

    import Game.{ BSONFields => F }
    import Player.playerBSONHandler

    private val emptyPlayerBuilder = playerBSONHandler.read(BSONDocument())

    def reads(r: BSON.Reader): Game = {

      val gameVariant   = Variant(r intD F.variant) | chess.variant.Standard
      val startedAtTurn = r intD F.startedAtTurn
      val plies         = r int F.turns
      val turnColor     = Color.fromPly(plies)
      val playedPlies   = plies - startedAtTurn

      val decoded = r.bytesO(F.huffmanPgn).map {
        PgnStorage.Huffman.decode(_, plies)
      } getOrElse {
        val clm      = r.get[CastleLastMove](F.castleLastMove)
        val pgnMoves = PgnStorage.OldBin.decode(r bytesD F.oldPgn, playedPlies)
        PgnStorage.Decoded(
          pgnMoves = pgnMoves,
          pieces = BinaryFormat.piece.read(r bytes F.binaryPieces, gameVariant),
          positionHashes = r
            .getO[chess.PositionHash](F.positionHashes) getOrElse Array.empty,
          unmovedRooks = r
            .getO[UnmovedRooks](F.unmovedRooks) getOrElse UnmovedRooks.default,
          lastMove = clm.lastMove,
          castles = clm.castles,
          halfMoveClock =
            pgnMoves.reverse.indexWhere(san => san.contains("x") || san.headOption.exists(_.isLower))
        )
      }

      val winC = r boolO F.winnerColor map Color.fromWhite
      val uids = r.getO[List[String]](F.playerUids) getOrElse Nil
      val (whiteUid, blackUid) =
        (uids.headOption.filter(_.nonEmpty), uids.lift(1).filter(_.nonEmpty))
      def makePlayer(
          field: String,
          color: Color,
          id: Player.Id,
          uid: Player.UserId
      ): Player = {
        val builder = r.getO[Player.Builder](field)(playerBSONHandler) | emptyPlayerBuilder
        val win     = winC map (_ == color)
        builder(color)(id)(uid)(win)
      }
      val (whiteId, blackId) = r str F.playerIds splitAt 4
      val wPlayer            = makePlayer(F.whitePlayer, White, whiteId, whiteUid)
      val bPlayer            = makePlayer(F.blackPlayer, Black, blackId, blackUid)

      val createdAt = r date F.createdAt
      val status    = r.get[Status](F.status)

      val chessGame = ChessGame(
        situation = chess.Situation(
          chess.Board(
            pieces = decoded.pieces,
            history = ChessHistory(
              lastMove = decoded.lastMove,
              castles = decoded.castles,
              positionHashes = decoded.positionHashes,
              unmovedRooks = decoded.unmovedRooks,
              checkCount = if (gameVariant.threeCheck) {
                val counts = r.intsD(F.checkCount)
                CheckCount(
                  counts.headOption getOrElse 0,
                  counts.lastOption getOrElse 0
                )
              } else Game.emptyCheckCount
            ),
            variant = gameVariant,
            crazyData =
              if (gameVariant.crazyhouse)
                Some(r.get[Crazyhouse.Data](F.crazyData))
              else None
          ),
          color = turnColor
        ),
        pgnMoves = decoded.pgnMoves,
        clock = r.getO[Color => Clock](F.clock) {
          clockBSONReader(createdAt, wPlayer.berserk, bPlayer.berserk)
        } map (_(turnColor)),
        turns = plies,
        startedAtTurn = r intD F.startedAtTurn
      )

      Game(
        id = r str F.id,
        whitePlayer = wPlayer,
        blackPlayer = bPlayer,
        chess = chessGame,
        status = status,
        daysPerTurn = r intO F.daysPerTurn,
        binaryMoveTimes = r bytesO F.moveTimes,
        clockHistory = for {
          clk <- chessGame.clock
          bw  <- r bytesO F.whiteClockHistory
          bb  <- r bytesO F.blackClockHistory
          history <- BinaryFormat.clockHistory.read(
            clk.limit,
            bw,
            bb,
            if (status == Status.Outoftime) Some(turnColor) else None
          )
        } yield history,
        mode = Mode(r boolD F.rated),
        createdAt = createdAt,
        movedAt = r.dateD(F.movedAt, createdAt),
        metadata = Metadata(
          source = r intO F.source flatMap Source.apply,
          tournamentId = r strO F.tournamentId,
          simulId = r strO F.simulId,
          analysed = r boolD F.analysed
        )
      )
    }

  import chess.format.FEN
  given BSON[Game.WithInitialFen] with
    def reads(r: BSON.Reader): Game.WithInitialFen =
      Game.WithInitialFen(
        gameBSONHandler.reads(r),
        (r strO Game.BSONFields.initialFen).map(FEN.apply)
      )

  private def clockHistory(
      color: Color,
      clockHistory: Option[ClockHistory],
      clock: Option[Clock],
      flagged: Option[Color]
  ) =
    for {
      clk     <- clock
      history <- clockHistory
      times = history(color)
    } yield BinaryFormat.clockHistory.writeSide(
      clk.limit,
      times,
      flagged contains color
    )

  private[game] def clockBSONReader(since: DateTime, whiteBerserk: Boolean, blackBerserk: Boolean) =
    new BSONReader[Color => Clock] {
      def readTry(bson: BSONValue): Try[Color => Clock] =
        bson match {
          case bin: BSONBinary =>
            ByteArrayBSONHandler readTry bin map { cl =>
              BinaryFormat.clock(since).read(cl, whiteBerserk, blackBerserk)
            }
          case b => lila.db.BSON.handlerBadType(b)
        }
    }
