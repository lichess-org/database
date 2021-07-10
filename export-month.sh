#!/bin/bash -e

month=${1}
dir=${2}
file="lichess_db_univ_$month.pgn"
bz2file="$file.bz2"

if test -f "$dir/$bz2file"; then
  echo "$dir/$bz2file already exists, skipping"
  exit 0
fi

echo "Export univ games of $month to $file"

sbt "runMain lichess.Main $month $dir/$file"

cd "$dir"

echo "Counting games in $bz2file"

touch counts.txt
grep -v -F "$file" counts.txt > counts.txt.new || touch counts.txt.new
games=$(grep --count -F '[Site ' "$file")
echo "$bz2file $games" >> counts.txt.new
mv counts.txt.new counts.txt

echo "Compressing $games games to $bz2file"

rm -f $bz2file
pbzip2 -p12 $file

echo "Check summing $bz2file"
touch sha256sums.txt
grep -v -F "$bz2file" sha256sums.txt > sha256sums.txt.new || touch sha256sums.txt.new
sha256sum "$bz2file" | tee --append sha256sums.txt.new
mv sha256sums.txt.new sha256sums.txt

echo "Done!"
