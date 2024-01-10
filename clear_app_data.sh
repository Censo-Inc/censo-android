#! /bin/bash

#Sets "exit on error" behavior for the shell script
set -e

#Loop to go through all extra arguments in the command
while [ $# -gt 0 ]; do
    if [[ $1 == "--"* ]]; then
        v="${1/--/}"
        declare "$v"="$2"
        shift
    fi
    shift
done

#Checks that the variant flag was passed in
if [[ -z $variant ]]; then
    printf "Missing variant. make sure to pass variant as: --variant argument.\n"
    printf "Common variants are: debug | staging\n"
    exit 1
fi

environment=$variant

#Run adb commands to wipe app data based on package name
adb shell pm clear co.censo.censo."${environment}"
adb shell pm clear co.censo.approver."${environment}"
#Message the CLI interface that the app data was cleared and for the selected environment
echo Vault and Approver ${environment} app data cleared