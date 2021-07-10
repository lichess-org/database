#!/bin/bash

dir=${1}

export_month() {
    echo "---------------------------"
    ./export-month.sh "$1-$2" $dir
}

echo "Export all to $dir"

for year in 2013 2014 2015 2016 2017 2018 2019 2020 2021; do
  for month in 01 02 03 04 05 06 07 08 09 10 11 12; do
    export_month $year $month
  done
done

for year in 2021; do
  for month in 01 02 03 04 05 06; do
    export_month $year $month
  done
done
