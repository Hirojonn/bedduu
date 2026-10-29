# Bedduu

Bedduu is an Android app for discovering and reading manga from available online sources. It includes browsing, source selection, a manga library, details and chapter lists, and a reading view.

Manga titles, images, and chapters are provided by their respective websites. Their availability and rights remain with those sources and their owners.

## Requirements

- Android Studio
- JDK 17 or newer
- Android SDK Platform 34

The app supports Android 7.0 (API 24) and newer.

## Build

Open the project in Android Studio and let Gradle sync, or build a debug APK from the project root:

```powershell
.\gradlew.bat assembleDebug
```

On macOS or Linux:

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

## Tests

Run the unit tests from the project root:

```powershell
.\gradlew.bat testDebugUnitTest
```

On macOS or Linux:

```bash
./gradlew testDebugUnitTest
```

## Technology

- Kotlin
- Jetpack Compose and Material 3
- Android Gradle Plugin
- Gradle Wrapper
