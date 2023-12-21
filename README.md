# Censo - Android

This project contains all of the source code for the Censo Android apps - both the primary Censo app as
well as the Censo Approver app.

## No License

This project is not licensed, and no permissions are granted to use, copy, modify, or distribute its
contents. While the source code is publicly available, you are not authorized to do anything beyond
viewing and forking the repository.

## Creating Firebase App Tester Build

Run the local script `upload_build.sh`

Need to pass three arguments: 

    --token which is the firebase token needed to upload builds to Firebase App Tester.

    --variant the variant you want to create:
        - Debug
        - AIntegration
        - BIntegration
        - CIntegration
        - DIntegration
        - Staging
        - Release

    --type the app you want to build (these are case sensitive): 
        - censo 
        - approver 

## Testing Deep Link

Onboarding case: 

`adb shell am start -W -a android.intent.action.VIEW -d "censo-integration://invite/[INVITATION_ID]" co.censo.approver.[VARIANT_SUFFIX]`

Access case:

`adb shell am start -W -a android.intent.action.VIEW -d "censo-integration://access/[PARTICIPANT_ID]" co.censo.approver.[VARIANT_SUFFIX]`

## UI Tests

Start adb server before running UI Tests in a separate terminal

`java -jar app/adbserver-desktop.jar`

Start an emulator

Then run the tests

`./gradlew connectedCheck`

## How to wipe all app data on the device

Run 

`adb shell pm clear co.censo.censo.debug`

or

`adb shell pm clear co.censo.approver.staging`

depending on the build variant and app type you are running. 

Adb is a tool that is located in `~/Library/Android/sdk/platform-tools`
