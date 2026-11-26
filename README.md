# Sonora

Sonora is a native Android music experience with two companion apps:

- **Sonora Remote** (`:remote`): a phone-first remote control for playback on a TV.
- **Sonora TV** (`:tv`): a full-screen, landscape music player designed for Android TV and Google TV, navigable with a TV remote.

## Technology

- Kotlin and Jetpack Compose
- Compose for TV for large-screen navigation
- Android Media3 for TV playback
- Gradle multi-project build

## Project status

This repository is being initialized. The first milestone establishes both native app targets and their initial interfaces. Device discovery, pairing, and live remote-to-TV commands will be added as the next milestone; the initial controls are not yet connected to another device.

## Build

Use Android Studio with JDK 17 and Android SDK 37, or use the **Android build** GitHub Actions workflow. The workflow builds debug APKs for both `remote` and `tv` and publishes them as downloadable artifacts.

## Modules

```text
Sonora/
├── remote/    # Android phone remote app
├── tv/        # Android TV / Google TV player app
└── .github/workflows/android.yml
```
