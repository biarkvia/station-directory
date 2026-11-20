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
