<!DOCTYPE html>
<html lang="en-US">
  <head>
    <meta charset='utf-8'>
    <meta http-equiv="X-UA-Compatible" content="chrome=1">
    <link href='https://fonts.googleapis.com/css?family=Chivo:900' rel='stylesheet' type='text/css'>
    <style><!-- style --></style>
    <title>lichess.org game database</title>
  </head>

  <body>
    <div id="container">
      <div class="inner">

        <header>
          <h2>lichess.org game database</h2>
        </header>
        <hr>
        <section id="main_content">
          <p>All standard rated games played on <a href="https://lichess.org">lichess.org</a>, in PGN format.</p>

          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Size</th>
                <th>Games</th>
                <th class="center">Eval</th>
                <th class="center">Clock</th>
                <th>Download</th>
              </tr>
            </thead>
            <tbody>
              <!-- files -->
              <!-- total -->
            </tbody>
          </table>

        </section>

        <br /><br />
        <section>
          <p>
            Some games include stockfish 8 analysis evaluations, in the format <code>[%eval +2.35]</code>.<br />
            In files with <strong>✔ Clock</strong>, real-time games include clock states, as <code>[%clk 0:01:00]</code>.<br />
            The number of games per file is estimated, with 98% accuracy.
          </p>
        </section>

        <br /><br />
        <section>
          <h3>Extract bz2 files</h3>
          <ul>
            <li>Linux: <code>bzip -k filename.pgn.bz2</code></li>
            <li>Windows: use <a href="http://www.7-zip.org/download.html">7zip</a></li>
            <li>OSX: just click it</li>
          </ul>
        </section>

        <br /><br />
        <section>
          <h3>Plain text download list</h3>
          <pre><!-- list --></pre>
        </section>

        <footer>
          All games played on <a href="https://lichess.org">lichess.org</a> are in the public domain.<br />
          Are you building something cool with this database? Please let us know!<br />
          <a href="https://twitter.com/lichessorg">@lichessorg</a> | contact@lichess.org 
        </footer>

      </div>
    </div>


  </body>
</html>
