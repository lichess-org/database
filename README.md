# lichess DB

This code exports lichess game database in a standard PGN format.

Files are available on [https://database.lichess.org](https://database.lichess.org).

Use them to do great things. Please share the results!

## Usage

```
# export Jan 2016 Standard games to lichess_db_2016-01.pgn
sbt "runMain lichess.buildGameDb 2016-01"

# export Jan 2016 Standard games to custom_file.pgn
sbt "runMain lichess.buildGameDb 2016-01 custom_file.pgn"

# export Jan 2016 Atomic games to custom_file.pgn
sbt "runMain lichess.buildGameDb 2016-01 custom_file.pgn atomic"
```
