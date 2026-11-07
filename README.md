# Pulse 🎵

<p align="center">
  <strong>A modern, offline, ad-free Android music player and audio tag editor inspired by Spotify.</strong>
</p>

<p align="center">
  <a href="https://github.com/Filz249/Pulse/actions/workflows/build-apk.yml">
    <img src="https://github.com/Filz249/Pulse/actions/workflows/build-apk.yml/badge.svg" alt="Build Status" />
  </a>
  <a href="https://github.com/Filz249/Pulse/releases/latest">
    <img src="https://img.shields.io/github/v/release/Filz249/Pulse?label=Latest%20Release&color=1DB954" alt="Latest Release" />
  </a>
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform" />
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License" />
</p>

<p align="center">
  <a href="https://github.com/Filz249/Pulse/releases/download/latest/Pulse.apk">
    <img src="https://img.shields.io/badge/⬇️_Download-Pulse.apk-1DB954?style=for-the-badge&logo=android&logoColor=white" height="48" alt="Download Pulse APK" />
  </a>
</p>

---

## ⚡ Overview

**Pulse** is built for music lovers who prefer locally stored audio files without streaming subscriptions, intrusive advertisements, or telemetry tracking. It provides a polished Spotify-like experience combined with powerful local ID3 / Vorbis tag editing capabilities.

---

## ✨ Key Features

### 1. 🎧 Audio Player & Media Management
- **Multi-Format Playback**: Supports **MP3, FLAC, WAV, AAC, M4A, and OGG**.
- **Full Playback Controls**: Play, Pause, Previous, Next, Interactive Seekbar, Shuffle, and Repeat modes (Off, All, One).
- **Background Playback**: Powered by Android Jetpack **Media3 (`MediaSessionService`)** and ExoPlayer.
- **System Integration**: Lock screen playback controls, notification player widget, and Bluetooth / headset media button handling (auto-pause on unplug).

### 2. 🗂️ Music Organization & Sorting
- Browse your music library by **Songs, Artists, Albums, and Playlists**.
- Real-time instant search across your entire library.
- Multi-criteria sorting:
  - By Title (A-Z)
  - By Artist
  - By Date Added
  - By Duration

### 3. 🏷️ Native Audio Metadata / Tag Editor
- Direct in-file ID3 / Vorbis tag modification powered by native C++ **TagLib** via JNI:
  - Song Title, Artist, Album, Year, Genre, Track Number
  - Synchronized and unsynchronized lyrics editor
  - Embedded Album Art cover extractor and updater
- **Scoped Storage Friendly**: Fully compliant with Android 11+ (API 30+) user write consent (`MediaStore.createWriteRequest`).

### 4. 🎨 Modern UX / UI
- 100% built with **Jetpack Compose** and **Material 3**.
- Sleek Spotify-inspired Dark Theme.
- Persistent **Mini Player** on bottom with smooth expansion into a **Full Screen Player**.
- **Privacy-First**: No ads, no analytics, no network permissions required for core features.

---

## 🏗️ Architecture & Tech Stack

| Layer | Technology | Role |
|---|---|---|
| **Language** | Kotlin 2.0+ | Coroutines & StateFlow for reactive asynchronous state |
| **UI Framework** | Jetpack Compose + Material 3 | Modern declarative UI, edge-to-edge support |
| **Audio Engine** | AndroidX Media3 (ExoPlayer + Session) | Background service, audio focus, notification widget |
| **Tag Editing** | AndroidTagLib (`io.github.amanrajaryan:TagLib`) | High-performance native C++ TagLib binding |
| **Local Database** | Room Database (SQLite) | Caching scanned metadata and user playlists |
| **Dependency Injection** | Hilt (Dagger) | Clean, testable dependency injection |
| **Image Loading** | Coil (Compose) | Fast album artwork caching and rendering |
| **Architecture Pattern** | Clean Architecture + MVVM | Strict separation of concerns (Data, Domain, Presentation) |

---

## 📦 Project Structure

```
pulse/
├── .github/workflows/
│   └── build-apk.yml               # Automated GitHub Actions APK build & release
├── app/
│   ├── build.gradle.kts            # App dependencies & Android configuration
│   └── src/main/
│       ├── AndroidManifest.xml     # Permissions & MediaSessionService declaration
│       └── java/com/pulse/music/
│           ├── PulseApplication.kt # @HiltAndroidApp
│           ├── data/
│           │   ├── local/
│           │   │   ├── db/         # Room Database, DAOs & Entities
│           │   │   ├── mediastore/ # MediaStoreDataSource (Storage scanner)
│           │   │   └── tageditor/  # TagLibDataSource (ID3 tag reader & writer)
│           │   └── repository/     # Data repository implementations
│           ├── domain/
│           │   ├── model/          # Song, SongMetadata, Album, Artist, Playlist
│           │   ├── repository/     # Repository interfaces
│           │   └── usecase/        # Clean business use cases
│           ├── player/
│           │   ├── AudioPlayerService.kt # Media3 background service
│           │   └── PlayerConnection.kt   # UI-to-Service MediaController bridge
│           ├── di/                 # Hilt dependency injection modules
│           └── ui/
│               ├── MainActivity.kt # Navigation host & MiniPlayer container
│               ├── components/     # MiniPlayer, SongListItem
│               ├── navigation/     # Screen routes
│               ├── screens/
│               │   ├── home/       # Library browsing, sorting & search
│               │   ├── player/     # Full screen playback view
│               │   └── tageditor/  # Tag editing screen
│               └── theme/          # Material 3 colors, typography & theme
├── gradle/
│   └── libs.versions.toml          # Gradle version catalog
└── README.md
```

---

## 📲 Download & Installation

1. Tap the **[Download Pulse.apk](https://github.com/Filz249/Pulse/releases/download/latest/Pulse.apk)** button above (or check the [Releases](https://github.com/Filz249/Pulse/releases) tab).
2. Open the downloaded `.apk` file on your Android device.
3. If prompted, allow your browser or file manager to *Install unknown apps*.
4. Launch **Pulse**, grant audio storage permissions, and start listening!

---

## 🛠️ Building from Source

### Prerequisites
- JDK 17
- Android SDK 35 (Android 15)
- Android Studio (Ladybug or newer recommended)

### Build Commands
```bash
# Clone the repository
git clone https://github.com/Filz249/Pulse.git
cd Pulse

# Build debug APK
./gradlew assembleDebug

# The built APK will be located at:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License & Privacy

- **100% Offline & Ad-Free**: Pulse does not contain ads, third-party trackers, or network calls.
- Distributed under the **MIT License**.
