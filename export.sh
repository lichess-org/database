#!/bin/bash

month=${1}
dir=${2}

export_variant() {
    echo "---------------------------"
    mkdir -p $dir/$1
    ./export-single-variant.sh $month $dir/$1 $1
}

variants="standard chess960 antichess atomic crazyhouse horde kingOfTheHill racingKings threeCheck"

for variant in $variants; do
  export_variant $variant
done

cd -

echo "Generating website"

cd web
nodejs index.js $dir
