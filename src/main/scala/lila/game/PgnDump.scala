package lila
package game

import chess.format.pgn.{ InitialComments, Pgn, PgnTree, SanStr, Tag, TagType, Tags }
import chess.format.Fen
import chess.format.pgn as chessPgn
import chess.{ Centis, Color, Ply }
import lichess.Users

// #TODO add draw offers comments
object PgnDump:

  def apply(game: Game, users: Users, initialFen: Option[Fen.Full]): Pgn =
    val ts           = tags(game, users, initialFen)
    val fenSituation = ts.fen.flatMap(Fen.readWithMoveNumber)
    val initialPly   = fenSituation.fold(Ply.initial)(_.ply)
    val tree = makeTree(
      game.sans,
      ~game.bothClockStates,
      game.startColor
    )
    Pgn(ts, InitialComments.empty, tree, initialPly.next)

  def result(game: Game) =
    if game.finished then game.winnerColor.fold("1/2-1/2")(_.fold("1-0", "0-1"))
    else "*"

  private def gameUrl(id: String) = s"https://lichess.org/$id"

  private def elo(p: Player) = p.rating.fold("?")(_.toString)

  private def player(g: Game, color: Color, users: Users) =
    val player = g.players(color)
    player.aiLevel.fold(users(color).name)("lichess AI level " + _)

  private def modeName(g: Game) = if g.rated.yes then "Rated" else "Casual"

  private def eventOf(game: Game) =
    val perf = game.perfType.fold("Standard")(_.name)
    game.tournamentId
      .map { id =>
        s"${modeName(game)} $perf tournament https://lichess.org/tournament/$id"
      }
      .orElse(game.simulId.map { id =>
        s"$perf simul https://lichess.org/simul/$id"
      })
      .orElse(game.swissId.map { id =>
        s"$perf swiss https://lichess.org/swiss/$id"
      })
      .getOrElse {
        s"${modeName(game)} $perf game"
      }

  private def ratingDiffTag(p: Player, tag: Tag.type => TagType) =
    p.ratingDiff.map { rd =>
      Tag(tag(Tag), s"${if rd >= 0 then "+" else ""}$rd")
    }

  private val emptyRound = Tag(_.Round, "-")

  def tags(game: Game, users: Users, initialFen: Option[Fen.Full]): Tags = Tags:
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
      Tag(_.WhiteElo, elo(game.players.white)),
      Tag(_.BlackElo, elo(game.players.black))
    ) ::: List(
      ratingDiffTag(game.players.white, _.WhiteRatingDiff),
      ratingDiffTag(game.players.black, _.BlackRatingDiff),
      users.white.title.map { t =>
        Tag(_.WhiteTitle, t)
      },
      users.black.title.map { t =>
        Tag(_.BlackTitle, t)
      },
      if game.variant.standard then Some(Tag(_.ECO, game.opening.fold("?")(_.opening.eco))) else None,
      if game.variant.standard then Some(Tag(_.Opening, game.opening.fold("?")(_.opening.name))) else None,
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
            game.status match
              case Created | Started                             => "Unterminated"
              case Aborted | NoStart                             => "Abandoned"
              case Timeout | Outoftime                           => "Time forfeit"
              case Resign | Draw | Stalemate | Mate | VariantEnd => "Normal"
              case Cheat                                         => "Rules infraction"
              case UnknownFinish                                 => "Unknown"
          }
        )
      ),
      if !game.variant.standardInitialPosition then
        Some(Tag(_.FEN, initialFen.getOrElse(game.variant.initialFen)))
      else None,
      if !game.variant.standardInitialPosition then Some(Tag("SetUp", "1")) else None,
      if game.variant.exotic then Some(Tag(_.Variant, game.variant.name)) else None
    ).flatten

def makeTree(
    moves: Seq[SanStr],
    clocks: Vector[Centis],
    startColor: Color
): Option[PgnTree] =
  val clockOffset = startColor.fold(0, 1)
  def f(san: SanStr, index: Int) = chessPgn.Move(
    san = san,
    timeLeft = clocks.lift(index - clockOffset).map(_.roundSeconds)
  )
  chess.Tree.buildWithIndex(moves, f)
