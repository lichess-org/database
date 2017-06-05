#!/bin/bash

month=${1}
dir=${2}
file="$dir/lichess_db_standard_rated_$month.pgn"

echo "Export $month to $file"

sbt "run $month $file"

echo "Compress to $file.bz2"

bzip2 $file

echo "Done!"
