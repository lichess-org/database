package lila
package game

import chess.variant.{ Crazyhouse, Variant }
import chess.{
  ByColor,
  CheckCount,
  Clock,
  Color,
  Game as ChessGame,
  HalfMoveClock,
  History as ChessHistory,
  Mode,
  Ply,
  Status,
  UnmovedRooks
}
import chess.bitboard.Board as BBoard
import lila.db.BSON
import lila.db.dsl.{ *, given }
import reactivemongo.api.bson.*
import scala.util.Try

object BSONHandlers {
  import lila.db.ByteArray.byteArrayHandler

  given StatusBSONHandler: BSONHandler[Status] = tryHandler[Status](
    { case BSONInteger(v) =>
      Status(v)
        .fold[Try[Status]](scala.util.Failure(new Exception(s"No such status: $v")))(scala.util.Success.apply)
    },
    x => BSONInteger(x.id)
  )

  private[game] given unmovedRooksHandler: BSONHandler[UnmovedRooks] = tryHandler[UnmovedRooks](
    { case bin: BSONBinary => byteArrayHandler.readTry(bin) map BinaryFormat.unmovedRooks.read },
    x => byteArrayHandler.writeTry(BinaryFormat.unmovedRooks write x).get
  )

  private[game] given crazyhouseDataHandler: BSON[Crazyhouse.Data] with
    import Crazyhouse.*
    def reads(r: BSON.Reader) =
      val (white, black) = r.str("p").view.flatMap(chess.Piece.fromChar).to(List).partition(_ is chess.White)
      Crazyhouse.Data(
        pockets = ByColor(white, black).map(pieces => Pocket(pieces.map(_.role))),
        promoted = chess.bitboard.Bitboard(r.str("t").view.flatMap(chess.Square.fromChar(_)))
      )

  private given lightGameReader: lila.db.BSONReadOnly[LightGame] with

    import Game.BSONFields as F

    private val emptyPlayerBuilder = LightPlayer.builderRead($empty)

    def reads(r: BSON.Reader): LightGame =
      val winC                 = r boolO F.winnerColor map { Color.fromWhite(_) }
      val uids                 = ~r.getO[List[String]](F.playerUids)
      val (whiteUid, blackUid) = (uids.headOption.filter(_.nonEmpty), uids.lift(1))
      def makePlayer(field: String, color: Color, uid: Option[String]): LightPlayer =
        val builder =
          r.getO[LightPlayer.Builder](field)(using LightPlayer.lightPlayerReader) | emptyPlayerBuilder
        builder(color)(uid)
      LightGame(
        id = r.str(F.id),
        whitePlayer = makePlayer(F.whitePlayer, Color.White, whiteUid),
        blackPlayer = makePlayer(F.blackPlayer, Color.Black, blackUid),
        status = r.get[Status](F.status),
        win = winC
      )

  given gameBSONHandler: BSON[Game] = new:

    import Game.BSONFields as F

    def reads(r: BSON.Reader): Game =

      val playerIds = r str F.playerIds
      val light     = lightGameReader.reads(r)

      val gameVariant  = Variant.idOrDefault(r.getO[Variant.Id](F.variant))
      val startedAtPly = Ply(r intD F.startedAtTurn)
      val ply          = r.get[Ply](F.turns)
      val turnColor    = ply.turn
      val createdAt    = r date F.createdAt
      val playedPlies  = ply - startedAtPly

      val whitePlayer = Player.from(light, Color.white, playerIds, r.getD[Bdoc](F.whitePlayer))
      val blackPlayer = Player.from(light, Color.black, playerIds, r.getD[Bdoc](F.blackPlayer))

      val decoded = r.bytesO(F.huffmanPgn) match
        case Some(huffPgn) => PgnStorage.Huffman.decode(huffPgn, playedPlies, light.id)
        case None =>
          val clm  = r.get[CastleLastMove](F.castleLastMove)
          val sans = PgnStorage.OldBin.decode(r bytesD F.oldPgn, playedPlies)
          val halfMoveClock =
            HalfMoveClock from sans.reverse
              .indexWhere(san => san.value.contains("x") || san.value.headOption.exists(_.isLower))
              .some
              .filter(HalfMoveClock.initial <= _)
          PgnStorage.Decoded(
            sans = sans,
            board = BBoard.fromMap(BinaryFormat.piece.read(r bytes F.binaryPieces, gameVariant)),
            positionHashes =
              r.getO[Array[Byte]](F.positionHashes).map(chess.PositionHash.apply) | chess.PositionHash.empty,
            unmovedRooks = r.getO[UnmovedRooks](F.unmovedRooks) | UnmovedRooks.default,
            lastMove = clm.lastMove,
            castles = clm.castles,
            halfMoveClock = halfMoveClock orElse
              r.getO[chess.format.Fen.Epd](F.initialFen).flatMap { fen =>
                chess.format.Fen.readHalfMoveClockAndFullMoveNumber(fen)._1
              } getOrElse playedPlies.into(HalfMoveClock)
          )

      val chessGame = ChessGame(
        situation = chess.Situation(
          chess.Board(
            board = decoded.board,
            history = ChessHistory(
              lastMove = decoded.lastMove,
              castles = decoded.castles,
              halfMoveClock = decoded.halfMoveClock,
              positionHashes = decoded.positionHashes,
              unmovedRooks = decoded.unmovedRooks,
              checkCount = if gameVariant.threeCheck then
                val counts = r.intsD(F.checkCount)
                CheckCount(~counts.headOption, ~counts.lastOption)
              else Game.emptyCheckCount
            ),
            variant = gameVariant,
            crazyData = gameVariant.crazyhouse option r.get[Crazyhouse.Data](F.crazyData)
          ),
          color = turnColor
        ),
        sans = decoded.sans,
        clock = r.getO[Color => Clock](F.clock)(using
          clockBSONReader(createdAt, whitePlayer.berserk, blackPlayer.berserk)
        ) map (_(turnColor)),
        ply = ply,
        startedAtPly = startedAtPly
      )

      val whiteClockHistory = r bytesO F.whiteClockHistory
      val blackClockHistory = r bytesO F.blackClockHistory

      Game(
        id = light.id,
        players = ByColor(whitePlayer, blackPlayer),
        chess = chessGame,
        clockHistory = for
          clk <- chessGame.clock
          bw  <- whiteClockHistory
          bb  <- blackClockHistory
          history <- BinaryFormat.clockHistory
            .read(clk.limit, bw, bb, (light.status == Status.Outoftime).option(turnColor))
        yield history,
        status = light.status,
        daysPerTurn = r.getO[Int](F.daysPerTurn),
        binaryMoveTimes = r bytesO F.moveTimes,
        mode = Mode(r boolD F.rated),
        createdAt = createdAt,
        movedAt = r.dateD(F.movedAt, createdAt),
        metadata = Metadata(
          source = r intO F.source flatMap Source.apply,
          tournamentId = r.strO(F.tournamentId),
          swissId = r.strO(F.swissId),
          simulId = r.strO(F.simulId),
          analysed = r boolD F.analysed
        )
      )

  import chess.format.Fen
  given gameWithInitialFenBSONHandler: BSON[Game.WithInitialFen] = new:
    def reads(r: BSON.Reader): Game.WithInitialFen =
      Game.WithInitialFen(
        gameBSONHandler.reads(r),
        Fen.Epd.from(r strO Game.BSONFields.initialFen)
      )

  private[game] def clockBSONReader(since: Instant, whiteBerserk: Boolean, blackBerserk: Boolean) =
    new BSONReader[Color => Clock]:
      def readTry(bson: BSONValue): Try[Color => Clock] =
        bson match
          case bin: BSONBinary =>
            byteArrayHandler readTry bin map { cl =>
              BinaryFormat.clock(since).read(cl, whiteBerserk, blackBerserk)
            }
          case b => lila.db.BSON.handlerBadType(b)
}
