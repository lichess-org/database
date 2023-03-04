#!/bin/bash -e

dir=${1}
file="lichess_db_puzzle.csv"
compressed_file="$file.zst"

echo "Export puzzles to $dir/$file"

nice -n19 sbt "runMain lichess.Puzzle $dir/$file"

cd "$dir"

echo "Counting puzzles in $file"

puzzles=$(grep --count -F '' "$file")
echo "$puzzles" > puzzle-count.txt

echo "Compressing $puzzles puzzles to $compressed_file"

rm -f $compressed_file
nice -n19 pzstd -p10 -19 --verbose $file

echo "Check summing $compressed_file"
touch sha256sums.txt
grep -v -F "$compressed_file" sha256sums.txt > sha256sums.txt.new || touch sha256sums.txt.new
sha256sum "$compressed_file" | tee --append sha256sums.txt.new
mv sha256sums.txt.new sha256sums.txt

echo "Done!"
