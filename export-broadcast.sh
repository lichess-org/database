#!/bin/bash -e

month=${1}
dir=${2}/broadcast
file="lichess_db_broadcast_$month.pgn"
compressed_file="$file.zst"

# if EXPORT_BROADCAST_TOKEN is not set, exit
if [ -z "$BROADCAST_TOKEN" ]; then
  echo "BROADCAST_TOKEN is not set. Exiting."
  exit 1
fi

echo "Export broadcasts of $month to $file"

cd "$dir"

url="http://lichess.org/api/broadcast/round/_${month//-/_}.pgn"

echo "curl $url"

curl -s $url -H "Authorization: Bearer $BROADCAST_TOKEN" >$file

echo "Counting games in $file"

touch counts.txt
grep -v -F "$file" counts.txt >counts.txt.new || touch counts.txt.new
games=$(grep --count -F '[GameURL ' "$file")
echo "$compressed_file $games" >>counts.txt.new
mv counts.txt.new counts.txt

echo "Compressing $games games to $compressed_file"

rm -f $compressed_file
nice -n19 pzstd -p10 -19 --verbose $file

rm $file

echo "Check summing $compressed_file"
touch sha256sums.txt
grep -v -F "$compressed_file" sha256sums.txt >sha256sums.txt.new || touch sha256sums.txt.new
sha256sum "$compressed_file" | tee --append sha256sums.txt.new
mv sha256sums.txt.new sha256sums.txt

echo "Done!"
