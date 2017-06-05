package lila.game

import chess.format.Forsyth
import chess.format.pgn.{Pgn, Tag, Parser, ParsedPgn}
import chess.format.{pgn => chessPgn}
import chess.{Centis, Color, White, Black}
import lichess.Users
import org.joda.time.format.DateTimeFormat

object PgnDump {

  def apply(game: Game, users: Users, initialFen: Option[String]): Pgn = {
    val ts = tags(game, users, initialFen)
    val fenSituation = ts find (_.name == Tag.FEN) flatMap { case Tag(_, fen) => Forsyth <<< fen }
    val moves2 =
      if (fenSituation.fold(false)(_.situation.color.black)) ".." :: game.pgnMoves
      else game.pgnMoves
    val turns = makeTurns(
      moves2,
      fenSituation.map(_.fullMoveNumber) getOrElse 1,
      game.bothClockStates getOrElse Vector.empty,
      game.startColor)
    Pgn(ts, turns)
  }

  def result(game: Game) =
    if (game.finished) game.winnerColor.fold("1/2-1/2")(_.fold("1-0", "0-1"))
    else "*"

  private def gameUrl(id: String) = s"https://lichess.org/$id"

  private val dateFormat = DateTimeFormat forPattern "yyyy.MM.dd";

  private def rating(p: Player) = p.rating.fold("?")(_.toString)

  private def player(g: Game, color: Color, users: Users) = {
    val player = g.player(color)
    player.aiLevel.fold(users(color).name)("lichess AI level " + _)
  }

  private val customStartPosition: Set[chess.variant.Variant] =
    Set(chess.variant.Chess960, chess.variant.FromPosition, chess.variant.Horde, chess.variant.RacingKings)

  def tags(game: Game, users: Users, initialFen: Option[String]): List[Tag] = List(
    Tag(_.Site, gameUrl(game.id)),
    Tag(_.Date, dateFormat.print(game.createdAt)),
    Tag(_.White, player(game, White, users)),
    Tag(_.Black, player(game, Black, users)),
    Tag(_.WhiteElo, rating(game.whitePlayer)),
    Tag(_.BlackElo, rating(game.blackPlayer))) ::: List(
      users.white.title.map { t => Tag(_.WhiteTitle, t) },
      users.black.title.map { t => Tag(_.BlackTitle, t) }).flatten ::: List(
        Tag(_.Result, result(game)),
        Tag(_.TimeControl, game.clock.fold("-") { c => s"${c.limit.roundSeconds}+${c.increment.roundSeconds}" }),
        Tag(_.Termination, {
          import chess.Status._
          game.status match {
            case Created | Started => "Unterminated"
            case Aborted | NoStart => "Abandoned"
            case Timeout | Outoftime => "Time forfeit"
            case Resign | Draw | Stalemate | Mate | VariantEnd => "Normal"
            case Cheat => "Rules infraction"
            case UnknownFinish => "Unknown"
          }
        })) ::: {
          if (customStartPosition(game.variant)) List(Tag(_.FEN, initialFen getOrElse "?"), Tag("SetUp", "1"))
          else Nil
        } ::: {
          if (game.variant.exotic) List(Tag(_.Variant, game.variant.name.capitalize))
          else Nil
        }

  private def makeTurns(moves: List[String], from: Int, clocks: Vector[Centis], startColor: Color): List[chessPgn.Turn] =
    (moves grouped 2).zipWithIndex.toList map {
      case (moves, index) =>
        val clockOffset = startColor.fold(0, 1)
        chessPgn.Turn(
          number = index + from,
          white = moves.headOption filter (".." !=) map { san =>
          chessPgn.Move(
            san = san,
            secondsLeft = clocks lift (index * 2 - clockOffset) map (_.roundSeconds))
        },
          black = moves lift 1 map { san =>
          chessPgn.Move(
            san = san,
            secondsLeft = clocks lift (index * 2 + 1 - clockOffset) map (_.roundSeconds))
        })
    } filterNot (_.isEmpty)
}
