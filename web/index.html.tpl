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
        <div id="selector">
          <a for="standard_games" class="on">Standard Chess</a>
          <a for="variant_games">Variants</a>
        </div>
        <section id="standard_games" class="panel">
          <!-- table-standard -->
        </section>
        <section id="variant_games" class="panel" style="display:none">
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

        <br /><br />
        <section>
        <h3 id="sample">Sample</h3>
        <pre>
[Event "Rated Bullet tournament https://lichess.org/tournament/yc1WW2Ox"]
[Site "https://lichess.org/PpwPOZMq"]
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

        <br /><br />
        <section>
        <h3 id="notes">Notes</h3>
          <p>
            About 6% of the games include Stockfish analysis evaluations: <code>[%eval 2.35]</code>.<br />
            In files with <strong>✔ Clock</strong>, real-time games include clock states: <code>[%clk 0:01:00]</code>.<br />
            The <code>WhiteElo</code> and <code>BlackElo</code> tags contain Glicko2 ratings.<br />
            The <code>Round</code> and <code>Date</code> tags are omitted (See <code>UTCDate</code> &amp; <code>UTCTime</code> instead).<br />
            Variant games  have a <code>Variant</code> tag, e.g. <code>[Variant "Antichess"]</code>.
          </p>
        </section>

        <br /><br />
        <section>
          <h3 id="extract" >Extract bz2 files</h3>
          <p>
            Linux: <code>pbzip2 -d filename.pgn.bz2</code><br />
            Windows: use <a href="http://www.7-zip.org/download.html">7zip</a><br />
            OSX: natively supported.
          </p>
        </section>

        <br /><br />
        <section>
          <h3 id="open-pgn-files" >Open PGN files</h3>
          <p>
            Traditional PGN databases, like SCID or ChessBase, fail to open large PGN files.
            Until they fix it, you can <a href="https://github.com/cyanfish/pgnsplit">split the PGN files</a>,
            or use programmatic APIs such as <a href="https://github.com/niklasf/python-chess">python-chess</a>
            or <a href="https://github.com/mcostalba/scoutfish">Scoutfish</a>.
          </p>
        </section>

        <br /><br />
        <section>
          <h3 id="related-projects" >Related projects</h3>
          <ul>
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
              <a href="https://medium.com/@yyyeliko/this-bot-is-actually-you-63d89631b2e5">A Bot plays its next move by what the majority of all the players chose at that specific position.</a>
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
          </ul>
          <p>
            Did you use this database? Please share your results! contact@lichess.org
          </p>
        </section>

        <footer>
          All games played on <a href="https://lichess.org">lichess.org</a> are in the public domain.<br />
          These collections of games are in the public domain, with no rights reserved.<br />
          Use them in any way you like, for data mining, research, commercial purpose, publication, anything.<br />
          You can download, modify and redistribute them at will, without asking for permission.<br />
          <a href="https://twitter.com/lichess">@lichess</a> | contact@lichess.org | <a href="https://github.com/ornicar/lichess-db">Source code</a>
        </footer>

      </div>
    </div>
    <script>
      function doAll(els, f) {
        Array.prototype.forEach.call(els, f);
      }
      function select(selected) {
        doAll(selectors, function(s) {
          s.className = s.getAttribute('for') === selected ? 'on' : '';
        });
        doAll(document.querySelectorAll('.panel'), function(panel) {
          panel.style.display = panel.id === selected ? '' : 'none';
        });
      }
      var selectors = document.querySelectorAll('#selector a');
      doAll(selectors, function(el) {
        el.addEventListener('click', function() {
          select(el.getAttribute('for'));
        });
      });
      if (location.hash.indexOf('#variant') === 0) select('variant_games');
    </script>
  </body>
</html>
