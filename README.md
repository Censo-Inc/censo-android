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

`adb shell am start -W -a android.intent.action.VIEW -d "vault://guardian/[INTERMEDIARY_KEY_HERE]/[DEVICE_KEY_HERE]/[PARTICIPANT_ID_HERE]" co.censo.vault.[VARIANT_SUFFIX]`

## UI Tests

Start adb server before running UI Tests in a separate terminal

`java -jar app/adbserver-desktop.jar`

Start an emulator

Then run the tests

`./gradlew connectedCheck`
