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

if [[ -z type ]]; then
  printf "Missing app type. Make sure to pass app type as: --type argument. The two options are censo or approver.\n"
  exit 1
fi

environment=$variant

appType=$type



./gradlew --stop
echo Running "${appType}:"lint"${environment}"
./gradlew "${appType}:"lint"${environment}"
echo Running "${appType}:"test"${environment}"UnitTest
./gradlew "${appType}:"test"${environment}"UnitTest

export FIREBASE_TOKEN="$token"
echo "$FIREBASE_TOKEN"

echo Running "${appType}:"assemble"${environment}" "${appType}:"appDistributionUpload"${environment}"
./gradlew "${appType}:"assemble"${environment}" "${appType}:"appDistributionUpload"${environment}"