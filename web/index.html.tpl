<!DOCTYPE html>
<html lang="en-US">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="chrome=1">
    <link href="https://fonts.googleapis.com/css?family=Noto+Sans:400,700|Roboto:300" rel="stylesheet">
    <style><!-- style --></style>
    <title>lichess.org open database</title>
  </head>

  <body>
    <div id="container">
      <div class="inner">
        <header>
          <img src="https://lichess1.org/assets/logo/lichess-pad4.svg" alt="Lichess logo" />
          <h1>
            <a href="https://lichess.org">
              lichess.org
            </a>
            <strong>open database</strong>
          </h1>
        </header>
        <p class="license">
          Lichess games and puzzles are released under the
          <a href="https://tldrlegal.com/license/creative-commons-cc0-1.0-universal">Creative Commons CC0 license</a>.<br />
          Use them for research, commercial purpose, publication, anything you like.<br />
          You can download, modify and redistribute them, without asking for permission.<br />
        </p>
        <section id="puzzles" class="panel">
          <nav>
            <a href="#standard_games">Standard Chess</a>
            <a href="#variant_games">Variants</a>
            <a href="#puzzles" class="on">Puzzles</a>
          </nav>
          <p>
            <strong><!-- nbPuzzles --></strong> original chess puzzles, rated and tagged.
            <a href="https://lichess.org/training/themes">See them in action on Lichess</a>.
          <p>
            <a href="lichess_db_puzzle.csv.bz2">Download lichess_db_puzzle.csv.bz2</a>
          </p>
          <h3 id="puzzle_format">Format</h3>
          <p>Puzzles are formatted as standard CSV. The fields are as follows:</p>
          <pre>PuzzleId,FEN,Moves,Rating,RatingDeviation,Popularity,NbPlays,Themes,GameUrl</pre>
          <h3 id="puzzle_sample">Sample</h3>
          <pre>0000D,5rk1/1p3ppp/pq3b2/8/8/1P1Q1N2/P4PPP/3R2K1 w - - 2 27,d3d6 f8d8 d6d8 f6d8,1426,500,2,0,advantage endgame short,https://lichess.org/F8M8OS71#53
0009B,r2qr1k1/b1p2ppp/pp4n1/P1P1p3/4P1n1/B2P2Pb/3NBP1P/RN1QR1K1 b - - 1 16,b6c5 e2g4 h3g4 d1g4,1500,500,2,0,advantage middlegame short,https://lichess.org/4MWQCxQ6/black#32
000tp,4r3/5pk1/1p3np1/3p3p/2qQ4/P4N1P/1P3RP1/7K w - - 6 34,d4b6 f6e4 h1g1 e4f2,1687,500,-5,0,crushing endgame short trappedPiece,https://lichess.org/GeXqsW90#67
001cv,6k1/5p1p/3p2pP/3Pr1b1/1pp5/7R/PP6/1K1R4 w - - 0 34,d1d4 e5e1 d4d1 e1d1 b1c2 d1c1,1043,500,-15,0,endgame long mate mateIn3 queensideAttack,https://lichess.org/Wn5Xtz5X#67
001u3,2r3k1/p1q2pp1/Q3p2p/b1Np4/2nP1P2/4P1P1/5K1P/2B1N3 b - - 3 33,c7b6 a6c8 g8h7 c8b7,1268,500,2,0,advantage hangingPiece kingsideAttack middlegame short,https://lichess.org/BBn6ipaK/black#66
00206,r3kb1r/pppqpn1p/5p2/3p1bpQ/2PP4/4P1B1/PP3PPP/RN2KB1R w KQkq - 1 11,b1c3 f5g4 h5g4 d7g4,1236,500,-5,0,advantage opening short trappedPiece,https://lichess.org/MbJRo6PT#21
002Cw,r7/2p2r1k/p2p1q1p/Pp1P4/1P2P3/2PQ4/6R1/R5K1 b - - 2 28,f7g7 e4e5 f6g6 g2g6,1315,500,-5,0,crushing discoveredAttack endgame short,https://lichess.org/lxiSa85s/black#56
002E4,8/8/kpq5/p4pQp/P7/7P/3r2P1/4R2K b - - 10 48,c6a4 g5d2,1406,500,-5,0,crushing endgame hangingPiece oneMove,https://lichess.org/JwMca3Nw/black#96
002rj,2k5/p1pp1p2/1p3Bb1/7p/1nP3PP/1P2rP2/PK6/3R1B1R w - - 7 22,f1g2 e3e2 d1d2 e2d2,960,500,-5,0,advantage fork middlegame short,https://lichess.org/JbCsF5hm#43
003YT,r1bqk1nr/1pp2ppp/p1pb4/4p3/3PP3/5N2/PPP2PPP/RNBQ1RK1 b kq - 0 6,d8f6 d4e5 d6e5 c1g5 f6d6 f3e5 d6d1 f1d1,1387,500,-5,0,advantage fork opening veryLong,https://lichess.org/TAffogpQ/black#12</pre>
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
              You can find a list of themes, their names and descriptions, in <a href="https://github.com/ornicar/lila/blob/master/translation/source/puzzleTheme.xml">this file</a>.
            </p>
            <p>
              Generating these chess puzzles took more than 25 years of CPU time.<br />
              We went through 150,000,000 analysed games from the Lichess database,
              and re-analyzed interesting positions with Stockfish 12 NNUE at 40 meganodes.
              The resulting puzzles were then automatically tagged.
              Finally, player input defined their quality and rating.
            </p>
        </section>
        <section id="variant_games" class="panel">
          <nav>
            <a href="#standard_games">Standard Chess</a>
            <a href="#variant_games" class="on">Variants</a>
            <a href="#puzzles">Puzzles</a>
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
        <div id="standard_games" class="panel">
          <section>
            <nav>
              <a href="#standard_games" class="on">Standard Chess</a>
              <a href="#variant_games">Variants</a>
              <a href="#puzzles">Puzzles</a>
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
              In files with <strong>✔ Clock</strong>, real-time games include clock states: <code>[%clk 0:01:00]</code>.
            </p>
            <p>
              The <code>WhiteElo</code> and <code>BlackElo</code> tags contain Glicko2 ratings.
            </p>
            <p>
              Variant games  have a <code>Variant</code> tag, e.g., <code>[Variant "Antichess"]</code>.
            </p>
          </section>

          <section>
            <h3 id="issues">Known issues</h3>
            <ul>
              <li>
                December 2020, January 2021: Many variant games have been
                <a href="https://github.com/niklasf/fishnet/issues/147">mistakenly analyzed using standard NNUE</a>,
                leading to incorrect evaluations.
              </li>
              <li>
                Up to December 2020:
                Some exports are missing the redundant (but stricly speaking mandatory)
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

          <section>
            <h3 id="extract" >Extract bz2 files</h3>
            <p>
              Unix: <code>pbzip2 -d filename.pgn.bz2</code> (faster than <code>bunzip2</code>)<br />
              Windows: use <a href="http://www.7-zip.org/download.html">7zip</a>
            </p>
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
        </div>

        <section>
          <h3 id="related-projects" >Related projects</h3>
          <ul>
            <li>
              <a href="https://maiachess.com/">MAIA CHESS - A human-like neural network chess engine</a>
            </li>
            <li>
              <a href="https://github.com/Antiochian/chess-blunders/blob/main/README.md">Does the low time alarm make people play worse?</a>
            </li>
            <li>
              <a href="https://database.nikonoel.fr/">Elite database (2400+)</a>
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
              <a href="https://www.tryit.in/blindfoldpuzzles/">Blindfold puzzles (using the puzzle DB)</a>
            </li>
            <li>
              <a href="https://chesscup.org/">Puzzle battle (using the puzzle DB)</a>
            </li>
          </ul>
          <p>
            Did you use this database? Please share your results! contact@lichess.org
          </p>
        </section>

        <footer>
          <a href="https://twitter.com/lichess">@lichess</a> | contact@lichess.org | <a href="https://github.com/ornicar/lichess-db">Source code</a>
        </footer>
      </div>
    </div>
  </body>
</html>
