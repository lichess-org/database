#!/bin/bash

dir=${1}

export() {
    echo "---------------------------"
    ./export.sh "$1-$2" $dir
}

echo "Export all to $dir"

for year in 2013; do
  for month in 08 09 10 11 12; do
    export $year $month
  done
done

for year in 2014 2015 2016; do
  for month in 01 02 03 04 05 06 07 08 09 10 11 12; do
    export $year $month
  done
done

for year in 2017; do
  for month in 01 02 03 04 05; do
    export $year $month
  done
done
