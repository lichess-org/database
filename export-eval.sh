#!/bin/bash -e

dir=${1}
file="lichess_db_eval.json"
compressed_file="$file.zst"

echo "Export evals to $dir/$file"

nice -n19 sbt "runMain lichess.Evals $dir/$file"

cd "$dir"

echo "Counting evals in $file"

evals=$(tail -n +2 "$file" | wc -l)
echo "$evals" >eval-count.txt

echo "Compressing $evals evals to $compressed_file"

rm -f $compressed_file
nice -n19 pzstd -p10 -19 --verbose $file

echo "Check summing $compressed_file"
touch sha256sums.txt
grep -v -F "$compressed_file" sha256sums.txt >sha256sums.txt.new || touch sha256sums.txt.new
sha256sum "$compressed_file" | tee --append sha256sums.txt.new
mv sha256sums.txt.new sha256sums.txt

echo "Done!"
