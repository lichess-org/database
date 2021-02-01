#!/bin/bash -e

dir=${1}
file="lichess_db_puzzle.csv"
bz2file="$file.bz2"

echo "Export puzzles to $dir/$file"

sbt "runMain lichess.Puzzle $dir/$file"

cd "$dir"

echo "Counting puzzles in $file"

puzzles=$(grep --count -F '' "$file")
echo "$puzzles" > puzzle-count.txt

echo "Compressing $puzzles puzzles to $bz2file"

rm -f $bz2file
pbzip2 -p4 $file

echo "Check summing $bz2file"
touch sha256sums.txt
grep -v -F "$bz2file" sha256sums.txt > sha256sums.txt.new || touch sha256sums.txt.new
sha256sum "$bz2file" | tee --append sha256sums.txt.new
mv sha256sums.txt.new sha256sums.txt

echo "Done!"
