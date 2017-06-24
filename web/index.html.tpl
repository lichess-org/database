<!DOCTYPE html>
<html lang="en-US">
  <head>
    <meta charset='utf-8'>
    <meta http-equiv="X-UA-Compatible" content="chrome=1">
    <link href='//fonts.googleapis.com/css?family=Noto+Sans:400,700|Roboto:300' rel='stylesheet'>
    <style><!-- style --></style>
    <title>lichess.org game database</title>
  </head>

  <body>
    <div id="container">
      <div class="inner">

        <header>
          <h1><a href="https://lichess.org">lichess.org</a> game database</h1>
        </header>
        <hr>
        <section id="main_content">
          <p><strong><!-- nbGames --></strong> standard rated games, played on lichess.org, in PGN format.</p>

          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Size</th>
                <th>Games</th>
                <th class="center">Clock</th>
                <th>Download</th>
              </tr>
            </thead>
            <tbody>
              <!-- files -->
              <!-- total -->
            </tbody>
          </table>

          <br /><br />
          <p>
            Here's a plain text <a href="list.txt">download list</a>,
            and the <a href="sha256sums.txt">SHA256 checksums.</a>
          </p>

        </section>

        <br /><br />
        <section>
        <h3>Sample</h3>
        <pre>
[Event "Rated Bullet tournament https://lichess.org/tournament/yc1WW2Ox"]
[Site "https://lichess.org/PpwPOZMq"]
[White "Abbot"]
[Black "Costello"]
[Result "0-1"]
[UTCDate "2017.04.01"]
[UTCTime "11:32:01"]
[WhiteElo "1175"]
[BlackElo "1691"]
[WhiteRatingDiff "-4"]
[BlackRatingDiff "+1"]
[WhiteTitle "FM"]
[ECO "B30"]
[Opening "Sicilian Defense: Old Sicilian"]
[TimeControl "30+0"]
[Termination "Time forfeit"]

1. e4 { [%eval 0.17] [%clk 0:00:30] } 1... c5 { [%eval 0.19] [%clk 0:00:30] } 2. Nf3 { [%eval 0.25] [%clk 0:00:29] } 2... Nc6 { [%eval 0.33] [%clk 0:00:30] } 3. Bc4 { [%eval -0.13] [%clk 0:00:28] } 3... e6 { [%eval -0.04] [%clk 0:00:30] } 4. c3 { [%eval -0.4] [%clk 0:00:27] } 4... b5? { [%eval 1.18] [%clk 0:00:30] } 5. Bb3?! { [%eval 0.21] [%clk 0:00:26] } 5... c4 { [%eval 0.32] [%clk 0:00:29] } 6. Bc2 { [%eval 0.2] [%clk 0:00:25] } 6... a5 { [%eval 0.6] [%clk 0:00:29] } 7. d4 { [%eval 0.29] [%clk 0:00:23] } 7... cxd3 { [%eval 0.6] [%clk 0:00:27] } 8. Qxd3 { [%eval 0.12] [%clk 0:00:22] } 8... Nf6 { [%eval 0.52] [%clk 0:00:26] } 9. e5 { [%eval 0.39] [%clk 0:00:21] } 9... Nd5 { [%eval 0.45] [%clk 0:00:25] } 10. Bg5?! { [%eval -0.44] [%clk 0:00:18] } 10... Qc7 { [%eval -0.12] [%clk 0:00:23] } 11. Nbd2?? { [%eval -3.15] [%clk 0:00:14] } 11... h6 { [%eval -2.99] [%clk 0:00:23] } 12. Bh4 { [%eval -3.0] [%clk 0:00:11] } 12... Ba6? { [%eval -0.12] [%clk 0:00:23] } 13. b3?? { [%eval -4.14] [%clk 0:00:02] } 13... Nf4? { [%eval -2.73] [%clk 0:00:21] } 0-1</pre>
        </section>

        <br /><br />
        <section>
        <h3>Notes</h3>
          <p>
            About 26 million games include Stockfish analysis evaluations: <code>[%eval 2.35]</code>.<br />
            In files with <strong>✔ Clock</strong>, real-time games include clock states: <code>[%clk 0:01:00]</code>.<br />
            The <code>WhiteElo</code> and <code>BlackElo</code> tags contain Glicko2 ratings.<br />
            The <code>Round</code> and <code>Date</code> tags are omitted.<br />
          </p>
        </section>

        <br /><br />
        <section>
          <h3>Extract bz2 files</h3>
          <p>
            Linux: <code>bunzip2 filename.pgn.bz2</code><br />
            Windows: use <a href="http://www.7-zip.org/download.html">7zip</a><br />
            OSX: natively supported.
          </p>
        </section>

        <br /><br />
        <section>
          <h3>Open PGN files</h3>
          <p>
            Current PGN database software, like SCID or ChessBase, are very limited.
            They fail to open large PGN collections! Please send a bug report to info@chessbase.com.<br />
            In the meantime, you can <a href="https://github.com/cyanfish/pgnsplit">split the PGN files</a>,
            or use programmatic APIs such as <a href="https://github.com/niklasf/python-chess">python-chess</a>
            or <a href="https://github.com/mcostalba/scoutfish">Scoutfish</a>.
          </p>
        </section>

        <footer>
          All games played on <a href="https://lichess.org">lichess.org</a> are in the public domain.<br />
          Are you building something cool with this database? Please let us know!<br />
          <a href="https://twitter.com/lichessorg">@lichessorg</a> | contact@lichess.org | <a href="https://github.com/ornicar/lichess-db">Source code</a>
        </footer>

      </div>
    </div>


  </body>
</html>
