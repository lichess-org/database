package lila.game

import chess.format.pgn.{ ParsedPgn, Parser, Pgn, Tag, TagType }
import chess.format.{ FEN, Forsyth }
import chess.format.{ pgn => chessPgn }
import chess.{ Black, Centis, Color, White }
import lichess.Users

object PgnDump {

  def apply(game: Game, users: Users, initialFen: Option[FEN]): Pgn = {
    val ts           = tags(game, users, initialFen)
    val fenSituation = ts find (_.name == Tag.FEN) flatMap { case Tag(_, fen) => Forsyth <<< FEN(fen) }
    val moves2: PgnMoves =
      if (fenSituation.fold(false)(_.situation.color.black)) ".." +: game.pgnMoves
      else game.pgnMoves
    val turns = makeTurns(
      moves2,
      fenSituation.map(_.fullMoveNumber) getOrElse 1,
      game.bothClockStates getOrElse Vector.empty,
      game.startColor
    )
    Pgn(chessPgn.Tags(ts), turns)
  }

  def result(game: Game) =
    if (game.finished) game.winnerColor.fold("1/2-1/2")(_.fold("1-0", "0-1"))
    else "*"

  private def gameUrl(id: String) = s"https://lichess.org/$id"

  private def elo(p: Player) = p.rating.fold("?")(_.toString)

  private def player(g: Game, color: Color, users: Users) = {
    val player = g.player(color)
    player.aiLevel.fold(users(color).name)("lichess AI level " + _)
  }

  private def eventOf(game: Game) = {
    val perf = game.perfType.fold("Standard")(_.name)
    game.tournamentId.map { id =>
      s"${game.mode} $perf tournament https://lichess.org/tournament/$id"
    } orElse game.simulId.map { id =>
      s"$perf simul https://lichess.org/simul/$id"
    } getOrElse {
      s"${game.mode} $perf game"
    }
  }

  private def ratingDiffTag(p: Player, tag: Tag.type => TagType) =
    p.ratingDiff.map { rd =>
      Tag(tag(Tag), s"${if (rd >= 0) "+" else ""}$rd")
    }

  private val emptyRound = Tag(_.Round, "-")

  def tags(game: Game, users: Users, initialFen: Option[FEN]): List[Tag] = {
    val date = Tag.UTCDate.format.print(game.createdAt)
    List(
      Tag(_.Event, eventOf(game)),
      Tag(_.Site, gameUrl(game.id)),
      Tag(_.Date, date),
      emptyRound,
      Tag(_.White, player(game, White, users)),
      Tag(_.Black, player(game, Black, users)),
      Tag(_.Result, result(game)),
      Tag(_.UTCDate, Tag.UTCDate.format.print(game.createdAt)),
      Tag(_.UTCTime, Tag.UTCTime.format.print(game.createdAt)),
      Tag(_.WhiteElo, elo(game.whitePlayer)),
      Tag(_.BlackElo, elo(game.blackPlayer))
    ) ::: List(
      ratingDiffTag(game.whitePlayer, _.WhiteRatingDiff),
      ratingDiffTag(game.blackPlayer, _.BlackRatingDiff),
      users.white.title.map { t =>
        Tag(_.WhiteTitle, t)
      },
      users.black.title.map { t =>
        Tag(_.BlackTitle, t)
      },
      if (game.variant.standard) Some(Tag(_.ECO, game.opening.fold("?")(_.opening.eco))) else None,
      if (game.variant.standard) Some(Tag(_.Opening, game.opening.fold("?")(_.opening.name))) else None,
      Some(
        Tag(
          _.TimeControl,
          game.clock.fold("-") { c =>
            s"${c.limit.roundSeconds}+${c.increment.roundSeconds}"
          }
        )
      ),
      Some(
        Tag(
          _.Termination, {
            import chess.Status._
            game.status match {
              case Created | Started                             => "Unterminated"
              case Aborted | NoStart                             => "Abandoned"
              case Timeout | Outoftime                           => "Time forfeit"
              case Resign | Draw | Stalemate | Mate | VariantEnd => "Normal"
              case Cheat                                         => "Rules infraction"
              case UnknownFinish                                 => "Unknown"
            }
          }
        )
      ),
      if (!game.variant.standardInitialPosition)
        Some(Tag(_.FEN, initialFen.getOrElse(game.variant.initialFen)))
      else None,
      if (!game.variant.standardInitialPosition) Some(Tag("SetUp", "1")) else None,
      if (game.variant.exotic) Some(Tag(_.Variant, game.variant.name)) else None
    ).flatten
  }

  private def makeTurns(
      moves: Vector[String],
      from: Int,
      clocks: Vector[Centis],
      startColor: Color
  ): List[chessPgn.Turn] =
    (moves grouped 2).zipWithIndex.toList map { case (moves, index) =>
      val clockOffset = startColor.fold(0, 1)
      chessPgn.Turn(
        number = index + from,
        white = moves.headOption.filter(".." != _).map { san =>
          chessPgn.Move(
            san = san,
            secondsLeft = clocks lift (index * 2 - clockOffset) map (_.roundSeconds)
          )
        },
        black = moves lift 1 map { san =>
          chessPgn.Move(
            san = san,
            secondsLeft = clocks lift (index * 2 + 1 - clockOffset) map (_.roundSeconds)
          )
        }
      )
    } filterNot (_.isEmpty)
}
