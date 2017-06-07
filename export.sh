#!/bin/bash -e

month=${1}
dir=${2}
file="lichess_db_standard_rated_$month.pgn"

echo "Export $month to $file"

sbt "run $month $dir/$file"

cd "$dir"

echo "Check summing $file.bz2"
touch sha256sums.txt
grep -v -F "$file" sha256sums.txt > sha256sums.txt.new || touch sha256sums.txt.new
sha256sum "$file" | tee --append sha256sums.txt.new
mv sha256sums.txt.new sha256sums.txt

touch counts.txt
grep -v -F "$file" counts.txt > counts.txt.new || touch counts.txt.new
games=$(grep --count -F '[Site ' "$file")
echo "$file.bz2 $games" >> counts.txt.new
mv counts.txt.new counts.txt

echo "Compressing $games games to $file.bz2"

bzip2 $file

cd -
echo "Done!"
