# Pulse

<p align="center">
  <strong>Your music. Your way.</strong><br>
  A modern, offline-first music player for your personal collection.
</p>

<p align="center">
  <a href="https://github.com/Filz249/Pulse/releases/download/latest/Pulse.apk">
    <img src="assets/pulse-download.svg" width="286" height="76" alt="Download Pulse for Android">
  </a>
</p>

---

## Overview

Pulse is a local music player for people who want to keep their listening experience personal. Browse the audio already stored on your device, build playlists, play music in the background, and maintain your own metadata without an account or streaming subscription.

Pulse is designed around a simple principle: your music library belongs to you.

## Features

### Playback

- Local audio playback with background support.
- System media controls for the lock screen, notifications, headphones, and Bluetooth devices.
- Play, pause, skip, seek, shuffle, and repeat controls.
- Album artwork displayed throughout the listening experience.

### Personal library

- Browse a local collection by songs, albums, artists, and playlists.
- Search across track titles, artists, albums, and genres.
- Sort by title, artist, recently added, or duration.
- A recently played section that remembers tracks on the device.

### Metadata editor

- Edit track title, artist, album, year, genre, and track number.
- Read and update embedded lyrics.
- Read and update embedded album artwork.
- Android media-write approval is requested before changing files when required by the operating system.

### Private by design

- No advertising SDKs or analytics in the app.
- No account required.
- No streaming service dependency.
- Your library and recently played history stay on your device.

## Download and installation

Use the download button above to get the latest Android APK.

1. Download the APK to your Android device.
2. Open the downloaded file.
3. Allow installation from your browser or file manager if Android asks.
4. Open Pulse and grant access to local audio when prompted.

## Build from source

### Requirements

- JDK 17
- Android SDK 35
- Android Studio with Kotlin and Compose support

### Build

```bash
git clone https://github.com/Filz249/Pulse.git
cd Pulse
./gradlew assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Technology

| Area | Implementation |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose and Material 3 |
| Playback | AndroidX Media3 and ExoPlayer |
| Background audio | MediaSessionService |
| Metadata | TagLib through JNI |
| Local database | Room and SQLite |
| Dependency injection | Hilt |
| Image loading | Coil |
| Architecture | Clean Architecture and MVVM |

## Project structure

```text
app/
  src/main/
    java/com/pulse/music/
      data/          Local data sources and repositories
      domain/        Models, repository contracts, and use cases
      player/        Media3 playback service and controller
      ui/            Compose screens, components, navigation, and theme
    res/             App icon, launch screen, and Android resources
.github/workflows/   Automated Android build and release
```

## Privacy

Pulse is intended to work with audio stored on the device. It does not need an account or online music service to play your local files. Review the source code and Android permissions in this repository for implementation details.

## License

Distributed under the MIT License. See [LICENSE](LICENSE) for details.
