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


## SONORA Bridge integration

The phone module now includes a QR/code pairing panel and an audio picker. It uses the shared SONORA TV Bridge protocol:
- Pair with a six-digit code or scan the receiver QR.
- Read title, artist, album, duration and embedded artwork from the selected audio file.
- Send the full audio file to SONORA TV.
- Stream audio in chunks through the bridge while the TV plays it.
- Send play/pause, previous and next control messages.

See the SONORA-TV repository file PROTOCOL.md for the current endpoint and message contract. Configure the HTTPS bridge URL shown by the SONORA TV receiver before pairing.
