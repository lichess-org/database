#!/bin/bash

month=${1}
file="out/lichess_db_$month.pgn"

echo "Export $month to $file"

sbt "run $month $file"

echo "Compress to $file.bz2"

bzip2 $file

echo "Done!"
