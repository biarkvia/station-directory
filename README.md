# Station Directory

Native Android app built with Kotlin and Jetpack Compose.

## Build and run

Use JDK 17 and the Android SDK with Platform 36, Build Tools 35.0.0 and Platform Tools.
Set `JAVA_HOME` to the JDK and `ANDROID_HOME` to the SDK. Gradle also accepts an SDK
location set with `sdk.dir` in `local.properties`. The Gradle Wrapper downloads Gradle on first use.

From the repository root on Windows, with an emulator running or a device connected
(Android 7.0 / API 24 or newer), run:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell am start -n dev.biarko.stations/.MainActivity
```

On macOS or Linux, use `./gradlew` and `"$ANDROID_HOME/platform-tools/adb"`.
The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Tests

Run the domain and HTTP tests on the JVM, then run the Room and Compose tests
with one Android emulator or device connected:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest
```

To generate JaCoCo reports for both suites:

```powershell
.\gradlew.bat -Pcoverage=true :app:createDebugUnitTestCoverageReport :app:createDebugAndroidTestCoverageReport
```

Open `app/build/reports/coverage/test/debug/index.html` for JVM coverage and
`app/build/reports/coverage/androidTest/debug/connected/index.html` for device coverage.

## Releases

GitHub Actions runs lint, JVM tests and the Android instrumentation suite for pushes to
`main`, pull requests and version tags. After the checks pass for a tag such as
`v0.1.0`, the workflow publishes the signed APK and its SHA-256 checksum on the
repository's Releases page. The matching R8 mapping is stored with the workflow run.

For local builds, `./gradlew :app:assembleRelease` produces an unsigned optimized
APK in `app/build/outputs/apk/release/`. CI signs it with the `stations` alias.
The repository secrets `KEYSTORE_BASE64` and `KEYSTORE_PASSWORD` hold the encoded
keystore and its password.

## Crash reporting

Release builds include Firebase Crashlytics when `app/google-services.json` is
present. CI reads the same configuration from the `GOOGLE_SERVICES_JSON` secret.
The Crashlytics Gradle plugin uploads the R8 mapping during assembly. A fresh
checkout builds and runs without a Firebase account.

To verify reporting, build `./gradlew -PcrashProbe=true :app:assembleRelease`
with a Firebase configuration, sign and install the APK, then run:

```sh
adb shell am start -W -n dev.biarko.stations/.MainActivity
adb shell am start -n dev.biarko.stations/.CrashProbeActivity
adb shell am start -W -n dev.biarko.stations/.MainActivity
```

Restart after the crash to send the report. Look for
`Station Directory release mapping check` in the Crashlytics dashboard under the
`-probe` version. Only the probe build contains the test activity.
