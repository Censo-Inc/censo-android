#! /bin/bash

set -e

while [ $# -gt 0 ]; do
    if [[ $1 == "--"* ]]; then
        v="${1/--/}"
        declare "$v"="$2"
        shift
    fi
    shift
done

if [[ -z $token ]]; then
  printf "Missing firebase token. Make sure to pass token as: --token argument.\n"
  exit 1
fi

if [[ -z variant ]]; then
  printf "Missing variant. Make sure to pass variant as: --variant argument.\n"
  exit 1
fi

environment=$variant

./gradlew --stop
echo Running lint"${environment}"
./gradlew lint"${environment}"
echo Running test"${environment}"UnitTest
./gradlew test"${environment}"UnitTest

export FIREBASE_TOKEN="$token"
echo "$FIREBASE_TOKEN"

echo Running assemble"${environment}" appDistributionUpload"${environment}"
./gradlew assemble"${environment}" appDistributionUpload"${environment}"