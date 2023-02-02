#!/bin/bash -e

month=${1}
dir=${2}
variant=${3}
file="lichess_db_${variant}_rated_$month.pgn"
compressed_file="$file.zst"

echo "Export $variant games of $month to $file"

nice -n19 sbt "runMain lichess.Main $month $dir/$file $variant"

cd "$dir"

echo "Counting games in $compressed_file"

touch counts.txt
grep -v -F "$file" counts.txt > counts.txt.new || touch counts.txt.new
games=$(grep --count -F '[Site ' "$file")
echo "$compressed_file $games" >> counts.txt.new
mv counts.txt.new counts.txt

echo "Compressing $games games to $compressed_file"

rm -f $compressed_file
pzstd -p2 -19 --verbose $file

echo "Check summing $compressed_file"
touch sha256sums.txt
grep -v -F "$compressed_file" sha256sums.txt > sha256sums.txt.new || touch sha256sums.txt.new
sha256sum "$compressed_file" | tee --append sha256sums.txt.new
mv sha256sums.txt.new sha256sums.txt

echo "Creating torrent for $compressed_file"
mktorrent --web-seed "https://database.lichess.org/$variant/$compressed_file" --piece-length 20 --announce "udp://tracker.torrent.eu.org:451" "$compressed_file"

echo "Done!"
