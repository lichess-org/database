#!/bin/bash

for year in 2014 2015 2016 2017; do
  for month in 01 02 03 04 05 06 07 08 09 10 11 12; do
    date="$year-$month"
    echo "---------------------------"
    ./export.sh $date
  done
done
