# Station Directory

Browse stations and inspect their devices, connection status and last contact time.
The app loads each station's devices when you open it. Once loaded, both the
directory and the device list are available offline.

[Download the signed APK](https://github.com/biarkvia/station-directory/releases/download/v0.1.1/station-directory.apk) · [Release and checksum](https://github.com/biarkvia/station-directory/releases/tag/v0.1.1)

## Build and run

Use JDK 17 and the Android SDK with Platform 36, Build Tools 35.0.0 and Platform Tools.
Set `JAVA_HOME` to the JDK and `ANDROID_HOME` to the SDK. Gradle also accepts an SDK
location set with `sdk.dir` in `local.properties`. The Gradle Wrapper downloads Gradle on first use.

Clone the repository, connect an Android device or start an emulator (API 24+),
and run the following from PowerShell. The debug build needs no account or API key.

```powershell
git clone https://github.com/biarkvia/station-directory.git
cd station-directory
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell am start -n dev.biarko.stations/.MainActivity
```

On macOS or Linux, use `./gradlew` and `"$ANDROID_HOME/platform-tools/adb"`.
The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Try it

1. Open a station and select a device status. The count shows how many devices match. Select **All** to see the whole list.
2. With that station open, turn on airplane mode and pull to refresh. The saved devices stay visible alongside **Showing saved data**.
3. Go back and open a station you have never viewed. Its details show **No saved device list for this station**. Turn airplane mode off and tap **Retry** to load them. The mock API occasionally returns errors or delays a response.

## Architecture

The app uses one Android module. The domain models have no Android dependencies.
Ktor reads the API with the OkHttp engine. Unknown device types and statuses keep
their original string codes. A new status also appears as a filter on the detail screen.
The application provides a single repository and passes dependencies through constructors.

Refreshing writes a successful response to Room in one transaction. Each screen's
ViewModel observes the stored list as a Flow. A failed refresh leaves the last
successful list visible. Room records whether each station's devices have ever
been fetched. This lets the screen distinguish a known empty station from one
whose devices still need to be downloaded.
Navigation 3 passes the station ID to the detail screen.

[Repository](app/src/main/java/dev/biarko/stations/data/StationRepository.kt) · [Room transactions](app/src/main/java/dev/biarko/stations/data/local/StationDao.kt) · [Screen state](app/src/main/java/dev/biarko/stations/ui/CachedListViewModel.kt) · [Further improvements](IMPROVEMENTS.md)

## Tests

Run lint and the domain and HTTP tests on the JVM. The Room and Compose tests use
a connected emulator or device. The test suites provide their own API responses.

```powershell
.\gradlew.bat :app:lintDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest
```

Generate JaCoCo reports for both suites with the same device connected.

```powershell
.\gradlew.bat -Pcoverage=true :app:createDebugUnitTestCoverageReport :app:createDebugAndroidTestCoverageReport
```

Open `app/build/reports/coverage/test/debug/index.html` for JVM coverage and
`app/build/reports/coverage/androidTest/debug/connected/index.html` for device coverage.

## Releases

GitHub Actions runs lint, JVM tests and the Android instrumentation suite for pushes to
`main`, pull requests and version tags. After the checks pass for a tag such as
`v0.1.1`, the workflow publishes the signed APK and its SHA-256 checksum on the
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
