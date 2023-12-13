package lila
package game

import chess.format.pgn.{ InitialComments, ParsedPgn, Parser, Pgn, PgnTree, SanStr, Tag, TagType, Tags }
import chess.format.Fen
import chess.format.pgn as chessPgn
import chess.{ Centis, Color, Ply }
import lichess.Users

object PgnDump {

  def apply(game: Game, users: Users, initialFen: Option[Fen.Epd]): Pgn =
    val ts           = tags(game, users, initialFen)
    val fenSituation = ts.fen.flatMap(Fen.readWithMoveNumber)
    val tree = makeTree(
      game.sans,
      fenSituation.fold(Ply.initial)(_.ply),
      ~game.bothClockStates,
      game.startColor
    )
    Pgn(ts, InitialComments.empty, tree)

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

  def tags(game: Game, users: Users, initialFen: Option[Fen.Epd]): Tags = Tags:
    val date = Tag.UTCDate.format.print(game.createdAt)
    List(
      Tag(_.Event, eventOf(game)),
      Tag(_.Site, gameUrl(game.id)),
      Tag(_.Date, date),
      emptyRound,
      Tag(_.White, player(game, chess.White, users)),
      Tag(_.Black, player(game, chess.Black, users)),
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
            import chess.Status.*
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

def makeTree(
    moves: Seq[SanStr],
    from: Ply,
    clocks: Vector[Centis],
    startColor: Color
): Option[PgnTree] =
  val clockOffset = startColor.fold(0, 1)
  def f(san: SanStr, index: Int) = chessPgn.Move(
    ply = from + index + 1,
    san = san,
    secondsLeft = clocks.lift(index - clockOffset).map(_.roundSeconds)
  )
  chess.Tree.buildWithIndex(moves, f)
