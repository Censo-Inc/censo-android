## Creating Firebase App Tester Build

Run the local script `upload_build.sh`

Need to pass two arguments: 

    --token which is the firebase token needed to upload builds to Firebase App Tester.

    --variant the variant you want to create:
        - Debug
        - AIntegration
        - BIntegration
        - CIntegration
        - DIntegration
        - Staging
        - Release

## Testing Deep Link

`adb shell am start -W -a android.intent.action.VIEW -d "vault://guardian/[INVITATION_ID]" co.censo.guardian.[VARIANT_SUFFIX]`

## UI Tests

Start adb server before running UI Tests in a separate terminal

`java -jar app/adbserver-desktop.jar`

Start an emulator

Then run the tests

`./gradlew connectedCheck`

## How to wipe all app data on the device

Run 

`adb shell pm clear co.censo.vault.debug`

or

`adb shell pm clear co.censo.vault.staging`

depending on the build variant you are running. 

Adb is a tool that is located in `~/Library/Android/sdk/platform-tools`