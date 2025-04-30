#!/bin/bash -e

dir=$1

# iterate on all months of the year
for year in $(seq -w 2024 2024); do
  for month in $(seq -w 1 12); do
    # call export-broadcast.sh with the month and dir
    ./export-broadcast.sh "$year-$month" $dir
  done
done
