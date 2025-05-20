<!DOCTYPE html>
<html lang="en-US">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="chrome=1">
    <link href="https://fonts.googleapis.com/css?family=Noto+Sans:400,700|Roboto:300" rel="stylesheet">
    <!-- <link href="/web/style.css" rel="stylesheet"></link> -->
    <style><!-- style --></style>
    <title>lichess.org open database</title>
  </head>

  <body>
    <div id="container">
      <div class="big-header">
        <header>
          <img src="https://lichess1.org/assets/logo/lichess-white.svg" alt="Lichess logo" />
          <h1>
            <a href="https://lichess.org">
              lichess.org
            </a>
            <strong>open database</strong>
          </h1>
        </header>
        <p class="license">
          Database exports are released under the
          <a href="https://tldrlegal.com/license/creative-commons-cc0-1.0-universal">Creative Commons CC0 license</a>.<br />
          Use them for research, commercial purpose, publication, anything you like.<br />
          You can download, modify and redistribute them, without asking for permission.<br />
        </p>
      </div>
      <div class="content">
        <section id="evals" class="panel">
          <nav>
            <a href="#standard_games">Chess games</a>
            <a href="#variant_games">Variants</a>
            <a href="#broadcasts">Broadcasts</a>
            <a href="#puzzles">Puzzles</a>
            <a href="#evals" class="on">Evaluations</a>
          </nav>
          <p>
            <strong><!-- nbEvals --></strong> chess positions evaluated with Stockfish.
            <br>
            <a href="https://lichess.org/analysis">Produced by, and for, the Lichess analysis board</a>,
            running various flavours of Stockfish within user browsers.
          <p>
            <a class="primary" href="lichess_db_eval.jsonl.zst">Download lichess_db_eval.jsonl.zst</a>
          </p>
          <p>
            This file was last updated on <!-- dateUpdated -->.
          </p>
          <h3 id="eval_format">Format</h3>
          <p>Evaluations are formatted as JSON; one position per line.</p>
          <p>The schema of a position looks like this:</p>
          <pre>
{
  "fen":          // the position FEN only contains pieces, active color, castling rights, and en passant square.
  "evals": [      // a list of evaluations, ordered by number of PVs.
      "knodes":   // number of kilo-nodes searched by the engine
      "depth":    // depth reached by the engine
      "pvs": [    // list of principal variations
        "cp":     // centipawn evaluation. Omitted if mate is certain.
        "mate":   // mate evaluation. Omitted if mate is not certain.
        "line":   // principal variation, in UCI format.
}</pre>
          <p>Each position can have multiple evaluations, each with a different number of <a href="https://www.chessprogramming.org/Principal_Variation">PVs</a>.
          <h3 id="eval_sample">Sample</h3>
          <pre>
{
  "fen": "2bq1rk1/pr3ppn/1p2p3/7P/2pP1B1P/2P5/PPQ2PB1/R3R1K1 w - -",
  "evals": [
    {
      "pvs": [
        {
          "cp": 311,
          "line": "g2e4 f7f5 e4b7 c8b7 f2f3 b7f3 e1e6 d8h4 c2h2 h4g4"
        }
      ],
      "knodes": 206765,
      "depth": 36
    },
    {
      "pvs": [
        {
          "cp": 292,
          "line": "g2e4 f7f5 e4b7 c8b7 f2f3 b7f3 e1e6 d8h4 c2h2 h4g4"
        },
        {
          "cp": 277,
          "line": "f4g3 f7f5 e1e5 d8f6 a1e1 b7f7 g2c6 f8d8 d4d5 e6d5"
        }
      ],
      "knodes": 92958,
      "depth": 34
    },
    {
      "pvs": [
        {
          "cp": 190,
          "line": "h5h6 d8h4 h6g7 f8d8 f4g3 h4g4 c2e4 g4e4 g2e4 g8g7"
        },
        {
          "cp": 186,
          "line": "g2e4 f7f5 e4b7 c8b7 f2f3 b7f3 e1e6 d8h4 c2h2 h4g4"
        },
        {
          "cp": 176,
          "line": "f4g3 f7f5 e1e5 f5f4 g2e4 h7f6 e4b7 c8b7 g3f4 f6g4"
        }
      ],
      "knodes": 162122,
      "depth": 31
    }
  ]
}
</pre>
          <h3 id="eval_notes">Notes</h3>
          <p>Evaluations have various depths and node count. If you only want one PV, we recommend selecting the evaluation with the highest depth, and use its first PV.</p>
          </p>
        </section>
        <section id="puzzles" class="panel">
          <nav>
            <a href="#standard_games">Chess games</a>
            <a href="#variant_games">Variants</a>
            <a href="#broadcasts">Broadcasts</a>
            <a href="#puzzles" class="on">Puzzles</a>
            <a href="#evals">Evaluations</a>
          </nav>
          <p>
            <strong><!-- nbPuzzles --></strong> chess puzzles, rated, and tagged.
            <a href="https://lichess.org/training/themes">See them in action on Lichess</a>.
          <p>
            <a class="primary" href="lichess_db_puzzle.csv.zst">Download lichess_db_puzzle.csv.zst</a>
          </p>
          <p>
            This file was last updated on <!-- dateUpdated -->.
          </p>          
          <h3 id="puzzle_format">Format</h3>
          <p>Puzzles are formatted as standard CSV. The fields are as follows:</p>
          <pre>PuzzleId,FEN,Moves,Rating,RatingDeviation,Popularity,NbPlays,Themes,GameUrl,OpeningTags</pre>
          <h3 id="puzzle_sample">Sample</h3>
          <pre>00sHx,q3k1nr/1pp1nQpp/3p4/1P2p3/4P3/B1PP1b2/B5PP/5K2 b k - 0 17,e8d7 a2e6 d7d8 f7f8,1760,80,83,72,mate mateIn2 middlegame short,https://lichess.org/yyznGmXs/black#34,Italian_Game Italian_Game_Classical_Variation
00sJ9,r3r1k1/p4ppp/2p2n2/1p6/3P1qb1/2NQR3/PPB2PP1/R1B3K1 w - - 5 18,e3g3 e8e1 g1h2 e1c1 a1c1 f4h6 h2g1 h6c1,2671,105,87,325,advantage attraction fork middlegame sacrifice veryLong,https://lichess.org/gyFeQsOE#35,French_Defense French_Defense_Exchange_Variation
00sJb,Q1b2r1k/p2np2p/5bp1/q7/5P2/4B3/PPP3PP/2KR1B1R w - - 1 17,d1d7 a5e1 d7d1 e1e3 c1b1 e3b6,2235,76,97,64,advantage fork long,https://lichess.org/kiuvTFoE#33,Sicilian_Defense Sicilian_Defense_Dragon_Variation
00sO1,1k1r4/pp3pp1/2p1p3/4b3/P3n1P1/8/KPP2PN1/3rBR1R b - - 2 31,b8c7 e1a5 b7b6 f1d1,998,85,94,293,advantage discoveredAttack master middlegame short,https://lichess.org/vsfFkG0s/black#62,</pre>
          <h3 id="puzzle_notes">Notes</h3>
            <p>
              Moves are in UCI format. Use a chess library to convert them to SAN, for display.
            </p>
            <p>
              All player moves of the solution are "only moves".
              I.e. playing any other move would considerably worsen the player position.
              An exception is made for mates in one: there can be several. Any move that checkmates should win the puzzle.
            </p>
            <p>
              FEN is the position before the opponent makes their move.<br>
              The position to present to the player is after applying the first move to that FEN.<br>
              The second move is the beginning of the solution.
            </p>
            <p>
              Popularity is a number between 100 (best) and -100 (worst).<br>
              It is calculated as <code>100 * (upvotes - downvotes)/(upvotes + downvotes)</code>, although votes are weigthed by various factors such as whether the puzzle was solved successfully or the solver's puzzle rating in comparison to the puzzle's.
            </p>
            <p>
              You can find a list of themes, their names and descriptions, in <a href="https://github.com/ornicar/lila/blob/master/translation/source/puzzleTheme.xml">this file</a>.
            </p>
            <p>
              The <code>OpeningTags</code> field is only set for puzzles starting before move 20.
              Here's the <a href="https://github.com/lichess-org/chess-openings">list of possible openings</a>.
            </p>
            <p>
              <a href="https://github.com/ornicar/lichess-puzzler/tree/master/generator">Generating these chess puzzles</a>
              took more than 100 years of CPU time.<br />
              We went through 600,000,000 analysed games from the Lichess database,
              and re-analyzed interesting positions with Stockfish NNUE at 40 meganodes.
              The resulting puzzles were then <a href="https://github.com/ornicar/lichess-puzzler/tree/master/tagger">automatically tagged</a>.
              To determine the rating, each attempt to solve is considered as a Glicko2 rated game between the player and the puzzle.
              Finally, player votes refine the tags and define popularity.
            </p>
        </section>
        <section id="variant_games" class="panel">
          <nav>
            <a href="#standard_games">Chess games</a>
            <a href="#variant_games" class="on">Variants</a>
            <a href="#broadcasts">Broadcasts</a>
            <a href="#puzzles">Puzzles</a>
            <a href="#evals">Evaluations</a>
          </nav>
          <h3>Antichess</h3>
          <!-- table-antichess -->
          <h3>Atomic</h3>
          <!-- table-atomic -->
          <h3>Chess960</h3>
          <!-- table-chess960 -->
          <h3>Crazyhouse</h3>
          <!-- table-crazyhouse -->
          <h3>Horde</h3>
          <!-- table-horde -->
          <h3>King of the Hill</h3>
          <!-- table-kingOfTheHill -->
          <h3>Racing Kings</h3>
          <!-- table-racingKings -->
          <h3>Three-check</h3>
          <!-- table-threeCheck -->
        </section>
        <div id="broadcasts" class="panel">
          <section>
            <nav>
              <a href="#standard_games">Chess games</a>
              <a href="#variant_games">Variants</a>
              <a href="#broadcasts" class="on">Broadcasts</a>
              <a href="#puzzles">Puzzles</a>
              <a href="#evals">Evaluations</a>
            </nav>
            <!-- table-broadcasts -->
          </section>

          <section>
            <h3 id="sample">Sample</h3>
            <pre>[Event "New Zealand Chess Congress 2025"]
[Round "01"]
[White "Metge, J. Nigel"]
[Black "Nagy, Gabor"]
[Result "1/2-1/2"]
[WhiteElo "1885"]
[WhiteFideId "4300181"]
[BlackElo "2469"]
[BlackTitle "GM"]
[BlackFideId "737119"]
[TimeControl "5400+30"]
[Board "1"]
[Variant "Standard"]
[ECO "E47"]
[Opening "Nimzo-Indian Defense: Normal Variation"]
[StudyName "Round 1"]
[ChapterName "Metge, J. Nigel - Nagy, Gabor"]
[UTCDate "2025.01.02"]
[UTCTime "01:29:03"]
[BroadcastName "New Zealand Chess Congress 2025 | Championship"]
[BroadcastURL "https://lichess.org/broadcast/new-zealand-chess-congress-2025--championship/round-1/spI5tcia"]
[GameURL "https://lichess.org/broadcast/new-zealand-chess-congress-2025--championship/round-1/spI5tcia/oHmutTGn"]

1. d4 { [%eval 0.17] [%clk 1:30:50] } 1... Nf6 { [%eval 0.19] [%clk 1:30:39] } 2. c4 { [%eval 0.18] [%clk 1:31:12] } 2... e6 { [%eval 0.11] [%clk 1:30:21] } 3. Nc3 { [%eval 0.17] [%clk 1:31:34] } 3... Bb4 { [%eval 0.09] [%clk 1:28:27] } 4. e3 { [%eval 0.11] [%clk 1:31:38] } 4... O-O { [%eval 0.11] [%clk 1:27:10] } 5. Bd3 { [%eval 0.03] [%clk 1:31:37] } 5... Re8 { [%eval 0.28] [%clk 1:25:53] } 6. Nf3 { [%eval 0.18] [%clk 1:31:03] } 6... d5 { [%eval 0.23] [%clk 1:25:50] } 7. O-O { [%eval 0.2] [%clk 1:27:01] } 7... Nbd7 { [%eval 0.4] [%clk 1:24:53] } 8. Ne5 { [%eval 0.17] [%clk 1:12:36] } 8... dxc4 { [%eval 0.29] [%clk 1:20:40] } 9. Nxc4 { [%eval 0.34] [%clk 1:08:45] } 9... a6 { [%eval 0.23] [%clk 1:15:52] } 10. Qc2 { [%eval 0.16] [%clk 0:52:48] } 10... c5 { [%eval 0.7] [%clk 1:03:58] } 11. Rd1?! { [%eval 0.05] } { Inaccuracy. a3 was best. } { [%clk 0:45:36] } 11... Qc7 { [%eval 0.22] [%clk 0:59:08] } 12. a3 { [%eval 0.0] [%clk 0:27:22] } 12... Bxc3 { [%eval 0.01] [%clk 0:56:30] } 13. bxc3 { [%eval -0.29] [%clk 0:24:44] } 13... b5 { [%eval -0.23] [%clk 0:55:45] } 14. Ne5?! { [%eval -1.05] } { Inaccuracy. Nd2 was best. } { [%clk 0:24:46] } 14... Nxe5 { [%eval -1.08] [%clk 0:55:09] } 15. dxe5 { [%eval -0.99] [%clk 0:25:06] } 15... Qxe5 { [%eval -1.15] [%clk 0:55:20] } 16. f4 { [%eval -1.06] [%clk 0:24:56] } 16... Qc7 { [%eval -1.04] [%clk 0:52:02] } 17. c4 { [%eval -1.06] [%clk 0:24:54] } 17... e5 { [%eval -0.59] [%clk 0:46:02] } 18. cxb5? { [%eval -1.76] } { Mistake. Bb2 was best. } { [%clk 0:21:18] } 18... exf4 { [%eval -1.74] [%clk 0:45:48] } 19. exf4?! { [%eval -2.55] } { Inaccuracy. Rb1 was best. } { [%clk 0:20:49] } 19... Qb6 { [%eval -2.6] [%clk 0:44:49] } 20. Bc4 { [%eval -2.47] [%clk 0:15:07] } 20... axb5 { [%eval -2.32] [%clk 0:44:34] } 21. Qb3?! { [%eval -3.64] } { Inaccuracy. Rb1 was best. } { [%clk 0:15:05] } 21... Qa7 { [%eval -4.06] [%clk 0:44:15] } 22. Qxb5 { [%eval -3.87] [%clk 0:13:39] } 22... Rb8 { [%eval -3.92] [%clk 0:43:20] } 23. Qc6 { [%eval -3.61] [%clk 0:11:51] } 23... Bg4 { [%eval -3.81] [%clk 0:40:29] } 24. Rf1 { [%eval -3.55] [%clk 0:10:56] } 24... Rbc8?? { [%eval -1.35] } { Blunder. Re4 was best. } { [%clk 0:36:46] } 25. Qa6 { [%eval -1.21] [%clk 0:09:58] } 25... Qd7? { [%eval 0.0] } { Mistake. Qe7 was best. } { [%clk 0:36:59] } 26. Bb2 { [%eval 0.0] [%clk 0:09:25] } 26... Ra8 { [%eval 0.0] [%clk 0:35:18] } 27. Qb5 { [%eval 0.03] [%clk 0:09:21] } 27... Qd2 { [%eval 0.13] [%clk 0:33:14] } 28. Qb7 { [%eval -0.18] [%clk 0:01:54] } 28... Nd7 { [%eval 0.28] [%clk 0:29:14] } 29. Rf2 { [%eval 0.34] [%clk 0:01:10] } 29... Qe3 { [%eval 0.36] [%clk 0:28:34] } 30. Qd5?! { [%eval -0.19] } { Inaccuracy. f5 was best. } { [%clk 0:00:58] } 30... Be6 { [%eval -0.24] [%clk 0:27:26] } 31. Qd3 { [%eval -0.22] [%clk 0:00:48] } 31... Bxc4 { [%eval -0.2] [%clk 0:26:04] } 32. Qxc4 { [%eval -0.17] [%clk 0:01:15] } 32... Nb6 { [%eval -0.26] [%clk 0:21:47] } 33. Qc3 { [%eval -0.25] [%clk 0:01:30] } 33... Qxc3 { [%eval -0.19] [%clk 0:22:07] } 34. Bxc3 { [%eval -0.24] [%clk 0:01:59] } 34... Re3 { [%eval -0.01] [%clk 0:22:25] } 35. Rc2 { [%eval -0.03] [%clk 0:02:03] } 35... f6 { [%eval -0.01] [%clk 0:22:07] } 36. Kf2 { [%eval -0.14] [%clk 0:01:42] } 36... Rd3 { [%eval -0.03] [%clk 0:22:06] } 37. Ke2 { [%eval -0.03] [%clk 0:01:18] } 37... c4 { [%eval -0.06] [%clk 0:22:22] } 38. Bb4 { [%eval -0.03] [%clk 0:00:48] } 38... Re8+ { [%eval -0.09] [%clk 0:17:43] } 39. Kf2 { [%eval -0.06] [%clk 0:01:11] } 39... Re4 { [%eval -0.08] [%clk 0:15:48] } 40. g3 { [%eval -0.05] [%clk 0:31:15] } 40... Nd5 { [%eval 0.0] [%clk 0:46:06] } 41. Rac1 { [%eval -0.06] [%clk 0:21:23] } 41... Ne3 { [%eval 0.0] [%clk 0:45:01] } 42. Re2 { [%eval 0.0] [%clk 0:11:27] } 42... f5 { [%eval 0.0] [%clk 0:43:22] } 43. Bc5 { [%eval 0.0] [%clk 0:07:39] } 43... Ng4+ { [%eval 0.0] [%clk 0:39:39] } 44. Ke1 { [%eval 0.0] [%clk 0:07:54] } 44... Rxe2+ { [%eval 0.0] [%clk 0:36:31] } 45. Kxe2 { [%eval 0.0] [%clk 0:08:21] } 45... Nxh2 { [%eval 0.03] [%clk 0:36:51] } 46. Rxc4 { [%eval 0.0] [%clk 0:08:30] } 46... Rxg3 { [%eval 0.0] [%clk 0:37:16] } 47. a4 { [%eval 0.0] [%clk 0:08:01] } 47... h5 { [%eval 0.0] [%clk 0:36:55] } 48. a5 { [%eval 0.0] [%clk 0:08:14] } 48... Rg2+ { [%eval 0.0] [%clk 0:37:19] } 49. Kd3 { [%eval 0.0] [%clk 0:08:01] } 49... Ra2 { [%eval 0.0] [%clk 0:37:41] } 50. Rc2 { [%eval 0.0] [%clk 0:06:57] } 50... Rxa5 { [%eval 0.0] [%clk 0:37:59] } 51. Rxh2 { [%eval 0.0] [%clk 0:07:20] } 51... Rxc5 { [%eval 0.0] [%clk 0:38:21] } 52. Rxh5 { [%eval -0.01] [%clk 0:07:46] } 52... g6 { [%eval -0.05] [%clk 0:38:44] } 53. Rg5 { [%eval -0.07] [%clk 0:08:09] } 53... Kg7 { [%eval -0.01] [%clk 0:38:58] } 54. Kd4 { [%eval 0.0] [%clk 0:08:32] } 54... Rc1 { [%eval 0.0] [%clk 0:39:11] } 55. Ke3 { [%eval 0.0] [%clk 0:08:31] } 55... Kh6 { [%eval 0.0] [%clk 0:37:59] } 56. Kf3 { [%eval 0.0] [%clk 0:06:35] } 56... Rf1+ { [%eval 0.0] [%clk 0:38:19] } 57. Ke3 { [%eval 0.0] [%clk 0:06:48] } 57... Ra1 { [%eval 0.0] [%clk 0:38:12] } 58. Kf3 { [%eval -0.01] [%clk 0:06:50] } 58... Kg7 { [%eval -0.03] [%clk 0:38:35] } 59. Ke3 { [%eval 0.0] [%clk 0:06:59] } 59... Kf6 { [%eval 0.0] [%clk 0:38:43] } 60. Kf3 { [%eval 0.0] [%clk 0:07:21] } 60... Ra3+ { [%eval -0.02] [%clk 0:38:29] } 61. Kf2 { [%eval -0.05] [%clk 0:07:31] } 61... Ke6 { [%eval 0.0] [%clk 0:37:20] } 62. Rxg6+ { [%eval 0.0] [%clk 0:07:05] } 62... Kd5 { [%eval 0.0] [%clk 0:37:44] } 63. Rb6 { [%eval 0.0] [%clk 0:07:00] } 63... Rc3 { [%eval 0.0] [%clk 0:37:42] } 64. Rb5+ { [%eval 0.0] [%clk 0:06:30] } 64... Kd4 { [%eval 0.0] [%clk 0:37:54] } 65. Rxf5 { [%eval 0.0] [%clk 0:06:32] } 65... Ke4 { [%eval 0.0] [%clk 0:38:19] } 66. Re5+ { [%eval 0.0] [%clk 0:05:49] } 66... Kxf4 { [%clk 0:38:41] } 67. Re2 { [%clk 0:06:11] } 67... Rf3+ { [%clk 0:39:07] } 68. Kg2 { [%clk 0:06:36] } 68... Re3 { [%clk 0:39:32] } 69. Rxe3 { [%clk 0:07:02] } 69... Kxe3 { [%clk 0:40:01] } 1/2-1/2</pre>
          </section>

          <section>
            <h3 id="notes">Notes</h3>
            <p>
              Almost all the games include Stockfish analysis evaluations:
              <code>[%eval 2.35]</code> (235 centipawn advantage),
              <code>[%eval #-4]</code> (getting mated in 4),
              always from White's point of view.
            </p>
            <p>
              Games between chess engines are marked with [WhiteTitle "BOT"] or [BlackTitle "BOT"], respectively.
            </p>
          </section>

          <section>
            <h3 id="zst">Decompress .zst</h3>
            <p>
              Unix: <code>pzstd -d filename.pgn.zst</code> (faster than <code>unzstd</code>)<br />
              Windows: use <a href="https://peazip.github.io/">PeaZip</a>
            </p>
            <p>
              Expect uncompressed files to be about 7.1 times larger.
            </p>
            <p>
              ZStandard archives are partially decompressable, so you can start downloading and then cancel at any point. You will be able to decompress the partial download if you only want a smaller set of game data.
            </p>
            <p>
              You can also decompress the data on-the-fly without having to create large temporary files. This example shows how you can pipe the contents to a Python script for analyzing using <code>zstdcat</code>.
            </p>
            <pre>$ zstdcat lichess_db.pgn.zst | python script.py</pre>
          </section>
        </div>
        <div id="standard_games" class="panel">
          <section>
            <nav>
              <a href="#standard_games" class="on">Chess games</a>
              <a href="#variant_games">Variants</a>
              <a href="#broadcasts">Broadcasts</a>
              <a href="#puzzles">Puzzles</a>
              <a href="#evals">Evaluations</a>
            </nav>
            <!-- table-standard -->
          </section>

          <section>
            <h3 id="sample">Sample</h3>
            <pre>[Event "Rated Bullet tournament https://lichess.org/tournament/yc1WW2Ox"]
[Site "https://lichess.org/PpwPOZMq"]
[Date "2017.04.01"]
[Round "-"]
[White "Abbot"]
[Black "Costello"]
[Result "0-1"]
[UTCDate "2017.04.01"]
[UTCTime "11:32:01"]
[WhiteElo "2100"]
[BlackElo "2000"]
[WhiteRatingDiff "-4"]
[BlackRatingDiff "+1"]
[WhiteTitle "FM"]
[ECO "B30"]
[Opening "Sicilian Defense: Old Sicilian"]
[TimeControl "300+0"]
[Termination "Time forfeit"]

1. e4 { [%eval 0.17] [%clk 0:00:30] } 1... c5 { [%eval 0.19] [%clk 0:00:30] }
2. Nf3 { [%eval 0.25] [%clk 0:00:29] } 2... Nc6 { [%eval 0.33] [%clk 0:00:30] }
3. Bc4 { [%eval -0.13] [%clk 0:00:28] } 3... e6 { [%eval -0.04] [%clk 0:00:30] }
4. c3 { [%eval -0.4] [%clk 0:00:27] } 4... b5? { [%eval 1.18] [%clk 0:00:30] }
5. Bb3?! { [%eval 0.21] [%clk 0:00:26] } 5... c4 { [%eval 0.32] [%clk 0:00:29] }
6. Bc2 { [%eval 0.2] [%clk 0:00:25] } 6... a5 { [%eval 0.6] [%clk 0:00:29] }
7. d4 { [%eval 0.29] [%clk 0:00:23] } 7... cxd3 { [%eval 0.6] [%clk 0:00:27] }
8. Qxd3 { [%eval 0.12] [%clk 0:00:22] } 8... Nf6 { [%eval 0.52] [%clk 0:00:26] }
9. e5 { [%eval 0.39] [%clk 0:00:21] } 9... Nd5 { [%eval 0.45] [%clk 0:00:25] }
10. Bg5?! { [%eval -0.44] [%clk 0:00:18] } 10... Qc7 { [%eval -0.12] [%clk 0:00:23] }
11. Nbd2?? { [%eval -3.15] [%clk 0:00:14] } 11... h6 { [%eval -2.99] [%clk 0:00:23] }
12. Bh4 { [%eval -3.0] [%clk 0:00:11] } 12... Ba6? { [%eval -0.12] [%clk 0:00:23] }
13. b3?? { [%eval -4.14] [%clk 0:00:02] } 13... Nf4? { [%eval -2.73] [%clk 0:00:21] } 0-1</pre>
          </section>

          <section>
            <h3 id="notes">Notes</h3>
            <p>
              About 6% of the games include Stockfish analysis evaluations:
              <code>[%eval 2.35]</code> (235 centipawn advantage),
              <code>[%eval #-4]</code> (getting mated in 4),
              always from White's point of view.
            </p>
            <p>
              The <code>WhiteElo</code> and <code>BlackElo</code> tags contain Glicko2 ratings.
            </p>
            <p>
              Games contain clock information down to the second as PGN <code>%clk</code> comments since April 2017. If you need centisecond precision, there is a separate export of <a href="https://database.lichess.org/db-univ/">standard games across all time controls from 2013 to 2021</a> using <code>%clkc</code> comments.
            </p>
            <p>
              Players using the <a href="https://lichess.org/api#tag/Bot">Bot API</a>
              are marked with <code>[WhiteTitle "BOT"]</code> or
              <code>[BlackTitle "BOT"]</code>, respectively.
            </p>
            <p>
              Variant games have a <code>Variant</code> tag, e.g., <code>[Variant "Antichess"]</code>.
            </p>
          </section>

          <section>
            <h3 id="zst">Decompress .zst</h3>
            <p>
              Unix: <code>pzstd -d filename.pgn.zst</code> (faster than <code>unzstd</code>)<br />
              Windows: use <a href="https://peazip.github.io/">PeaZip</a>
            </p>
            <p>
              Expect uncompressed files to be about 7.1 times larger.
            </p>
            <p>
              ZStandard archives are partially decompressable, so you can start downloading and then cancel at any point. You will be able to decompress the partial download if you only want a smaller set of game data.
            </p>
            <p>
              You can also decompress the data on-the-fly without having to create large temporary files. This example shows how you can pipe the contents to a Python script for analyzing using <code>zstdcat</code>.
            </p>
            <pre>$ zstdcat lichess_db.pgn.zst | python script.py</pre>
          </section>

          <section>
            <h3 id="open-pgn-files" >Open PGN files</h3>
            <p>
              Traditional PGN databases, like SCID or ChessBase, fail to open large PGN files.
              Until they fix it, you can <a href="https://github.com/cyanfish/pgnsplit">split the PGN files</a>,
              or use programmatic APIs such as <a href="https://github.com/niklasf/python-chess">python-chess</a>
              or <a href="https://github.com/mcostalba/scoutfish">Scoutfish</a>.
            </p>
          </section>

          <section>
            <h3 id="issues">Known issues</h3>
            <ul>
              <li>
                November 2023: Some <a href="https://github.com/lichess-org/database/issues/54">Chess960 rematches were recorded with invalid castling rights in their starting FEN</a>.
              </li>
              <li>
                December 2022: Some <a href="https://github.com/lichess-org/lila/commit/3a742bcbb9504ca471e3c4baf6fe7ab25fc46441">Antichess games were recorded with bullet ratings</a>.
              </li>
              <li>
                12th March 2021: Some games have incorrect results due to a database outage in the aftermath of a <a href="https://lichess.org/forum/lichess-feedback/fire-in-a-lichess-datacenter">datacenter fire</a>.
              </li>
              <li>
                9th February 2021: Some games were
                <a href="https://lichess.org/forum/lichess-feedback/bug-with-game-completion-today-between-724-and-804">resigned even after the game ended</a>.
                In variants, additional moves could be played after the end
                of the game.
              </li>
              <li>
                December 2020, January 2021: Many variant games have been
                <a href="https://github.com/niklasf/fishnet/issues/147">mistakenly analyzed using standard NNUE</a>,
                leading to incorrect evaluations.
              </li>
              <li>
                Up to December 2020:
                Some exports are missing the redundant (but strictly speaking mandatory)
                <code>Round</code> tag (always <code>-</code>),
                <code>Date</code> tag (see <code>UTCDate</code> &amp; <code>UTCTime</code> instead),
                and <a href="https://github.com/ornicar/lichess-db/issues/31">black move numbers after comments</a>.
                This may be fixed by a future re-export.
              </li>
              <li>
                July 2020 (especially 31st), August 2020 (up to 16th):
                Many games, especially variant games, may have
                <a href="https://github.com/ornicar/lila/issues/7086">incorrect evaluations</a>
                in the opening (up to 15 plies).
              </li>
              <li>
                December 2016 (up to and especially 9th):
                Many games may have <a href="https://github.com/ornicar/lichess-db/issues/10">incorrect evaluations</a>.
              </li>
              <li>
                Before 2016: In some cases,
                <a href="https://github.com/ornicar/lichess-db/issues/9#issuecomment-373883385">mate may not be forced in the number of moves given by the evaluations</a>.
              </li>
              <li>
                June 2020, all before March 2016: Some players were able to <a href="https://github.com/ornicar/lila/issues/7031">play themselves in rated games</a>.
              </li>
              <li>
                Up to August 2016: <a href="https://github.com/ornicar/lichess-db/issues/23">7 games with illegal castling moves</a> were recorded.
              </li>
            </ul>
          </section>
        </div>

        <section>
          <h3 id="related-projects" >Related projects</h3>
          <ul>
            <li>
              <a href="https://maiachess.com/">MAIA CHESS - A human-like neural network chess engine</a>
            </li>
            <li>
              <a href="https://freopen.org/">World Chess Champion number</a>
            </li>
            <li>
              <a href="https://database.nikonoel.fr/">Elite database (2400+)</a>
            </li>
            <li>
              <a href="https://github.com/Antiochian/chess-blunders/blob/main/README.md">Does the low time alarm make people play worse?</a>
            </li>
            <li>
              <a href="https://www.chessroots.com">ChessRoots visual opening explorer</a>
            <li>
              <a href="https://github.com/Ramon-Deniz/ChessData#heatmap-of-chess-moves">Heatmap of Chess Moves</a>
            </li>
            <li>
              <a href="https://github.com/QueensGambit/CrazyAra">Deep Learning Engine for Crazyhouse </a>
            </li>
            <li>
              <a href="https://github.com/mark-dev/chessfactory-hall-of-fame">Top lichess games by various criteria</a>
            </li>
            <li>
              <a href="https://reddit.com/r/chess/comments/c4nzje/how_lichess_ratings_compare_analysis_of_35/">Comparison of Bullet, Blitz, Rapid and Classical ratings</a>
            </li>
            <li>
              <a href="https://medium.com/@yyyeliko/this-bot-is-actually-you-63d89631b2e5">A Bot that plays its next move by what the majority of all the players chose at that specific position</a>
            </li>
            <li>
              <a href="https://thenewstack.io/artificial-stupidity-one-google-engineers-algorithms-for-bad-chess-playing/">Machine learning over 500 million games</a>
            </li>
            <li>
              <a href="https://www.youtube.com/watch?v=7eevSgJqV7o">Finding the Most Common Chess Blunders</a>
            </li>
            <li>
              <a href="https://www.youtube.com/watch?v=VcghDhMlgBw">20 most popular chess openings over time</a>
            </li>
            <li>
              <a href="http://tom7.org/chess/survival.pdf">Survival in chessland [PDF]</a> - <a href="https://youtu.be/DpXy041BIlA?t=859">YouTube video</a>
            </li>
            <li>
              <a href="https://github.com/welyab/chess960-win-by-position-setup">Chess960 Win by Position Setup</a>
            </li>
            <li>
              <a href="https://www.reddit.com/r/chess/comments/kzb9f5/selected_opening_usage_in_17_billion_lichess/">Selected Opening Usage by Game Rating</a>
            </li>
            <li>
              <a href="https://www.kaggle.com/ironicninja/visualizing-chess-game-length-and-piece-movement">Chess Visualization Project</a>
            </li>
            <li>
              <a href="https://github.com/githubvishwas/blindfoldpuzzles">Blindfold puzzles (using the puzzle DB)</a>
            </li>
            <li>
              <a href="https://chesscup.org/">Puzzle battle (using the puzzle DB)</a>
            </li>
            <li>
              <a href="https://web.chessdigits.com/articles/when-should-you-resign">When should you resign?</a>
            </li>
            <li>
              <a href="https://github.com/Paul566/chessOpeningStats">Popularity and win rate of chess openings</a>
            </li>
            <li>
              <a href="https://github.com/marcusbuffett/chess-tactics-cli">Chess Tactics CLI (using the puzzle DB)</a>
            </li>
            <li>
              <a href="https://cshancock.netlify.app/post/2021-06-23-lichess-puzzles-by-eco/">Lichess puzzles, by ECO</a>
            </li>
            <li>
              <a href="https://github.com/brianch/offline-chess-puzzles">Offline puzzles</a>
            </li>
            <li>
              <a href="https://www.reddit.com/r/chess/comments/pn2f45/whats_the_difficulty_of_the_tactics_that_people/">What's the difficulty of the tactics that people actually spot during their games?</a>
            </li>
            <li>
              <a href="https://chessbook.com/">Repertoire builder</a>
            </li>
            <li>
              <a href="https://tusharmurali.github.io/chess-memory/">Puzzle memory trainer</a>
            </li>
            <li>
              <a href="https://github.com/chesspecker/chesspecker">Chesspecker - puzzle repetition training</a>
            </li>
            <li>
              <a href="https://lichess.org/@/piazzai/blog/do-variants-help-you-play-better-chess-statistical-evidence/0tAPXnqH">Do Variants Help You Play Better Chess?</a>
            </li>
            <li>
              <a href="https://github.com/ornicar/lichess-db/blob/master/web/chess-social-networks-paper.pdf">Online Chess Social Networks</a>
            </li>
            <li>
              <a href="https://emiruz.com/post/2022-04-15-lichess1/">Fast thinking on lichess.org</a>
            </li>
            <li>
              <a href="https://lichess.org/@/jmviz/blog/when-should-you-berserk/rQdcB4QB">When should you berserk?</a>
            </li>
            <li>
              <a href="https://mcognetta.github.io/posts/lichess-combined-puzzle-game-db/">Combined puzzle and game database</a>
            </li>
            <li>
              <a href="https://www.youtube.com/watch?v=iDnW0WiCqNc">The rarest move in chess</a>
            </li>
            <li>
              <a href="https://chessort.com/">Chessort, a puzzle game where you sort moves based on the chess engine's evaluation.</a>
            </li>
            <li>
              <a href="https://www.youtube.com/watch?v=oUERPFqruYo/">500,000 games of intermediate chess players analyzed (in French)</a>
            </li>
          </ul>
          <p>
            Did you use this database? Please share your results! contact@lichess.org
          </p>
        </section>
      </div>
      <footer>
        <a href="https://twitter.com/lichess">@lichess</a> | contact@lichess.org | <a href="https://github.com/lichess-org/database">Source code</a>
      </footer>
    </div>
  </body>
</html>
