package lila.game

import chess.*
import chess.format.Uci
import chess.format.pgn.SanStr

import lila.db.ByteArray

sealed trait PgnStorage

private object PgnStorage:

  case object OldBin:

    def encode(sans: Vector[SanStr]) =
      ByteArray:
        format.pgn.Binary.writeMoves(sans).get

    def decode(bytes: ByteArray, plies: Ply): Vector[SanStr] =
      format.pgn.Binary.readMoves(bytes.value.toList, plies.value).get.toVector

  case object Huffman:

    import org.lichess.compression.game.{ Board as JavaBoard, Encoder }

    def encode(sans: Vector[SanStr]) =
      ByteArray:
        Encoder.encode(SanStr.raw(sans.toArray))

    def decode(bytes: ByteArray, plies: Ply, id: String): Decoded =
      val decoded =
        try Encoder.decode(bytes.value, plies.value)
        catch
          case e: java.nio.BufferUnderflowException =>
            println(s"Can't decode game $id PGN")
            throw e
      Decoded(
        sans = SanStr.from(decoded.pgnMoves.toVector),
        board = chessBoard(decoded.board),
        positionHashes = PositionHash(decoded.positionHashes),
        unmovedRooks = UnmovedRooks(decoded.board.castlingRights),
        lastMove = Option(decoded.lastUci).flatMap(Uci.apply),
        castles = Castles(decoded.board.castlingRights),
        halfMoveClock = HalfMoveClock(decoded.halfMoveClock)
      )

    private def chessBoard(b: JavaBoard): Board =
      Board(
        occupied = Bitboard(b.occupied),
        white = Bitboard(b.white),
        black = Bitboard(b.black),
        pawns = Bitboard(b.pawns),
        knights = Bitboard(b.knights),
        bishops = Bitboard(b.bishops),
        rooks = Bitboard(b.rooks),
        queens = Bitboard(b.queens),
        kings = Bitboard(b.kings)
      )

  case class Decoded(
      sans: Vector[SanStr],
      board: Board,
      positionHashes: PositionHash, // irrelevant after game ends
      unmovedRooks: UnmovedRooks,   // irrelevant after game ends
      lastMove: Option[Uci],
      castles: Castles,            // irrelevant after game ends
      halfMoveClock: HalfMoveClock // irrelevant after game ends
  )
