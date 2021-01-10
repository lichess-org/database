#!/bin/bash -e

month=${1}
dir=${2}
variant=${3}
file="lichess_db_${variant}_rated_$month.pgn"
bz2file="$file.bz2"

echo "Export $variant games of $month to $file"

sbt "runMain lichess.Main $month $dir/$file $variant"

cd "$dir"

echo "Counting games in $bz2file"

touch counts.txt
grep -v -F "$file" counts.txt > counts.txt.new || touch counts.txt.new
games=$(grep --count -F '[Site ' "$file")
echo "$bz2file $games" >> counts.txt.new
mv counts.txt.new counts.txt

echo "Compressing $games games to $bz2file"

rm -f $bz2file
pbzip2 -p2 $file

echo "Check summing $bz2file"
touch sha256sums.txt
grep -v -F "$bz2file" sha256sums.txt > sha256sums.txt.new || touch sha256sums.txt.new
sha256sum "$bz2file" | tee --append sha256sums.txt.new
mv sha256sums.txt.new sha256sums.txt

echo "Creating torrent for $bz2file"
mktorrent --web-seed "https://database.lichess.org/$variant/$bz2file" --piece-length 20 "$bz2file"

echo "Done!"
