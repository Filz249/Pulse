# Pulse 🎵

> Moderan, offline, ad-free glazbeni player za Android inspiriran Spotifyjem, namijenjen reprodukciji lokalnih audio datoteka i naprednom uređivanju audio tagova.

---

## 🚀 Ključne funkcionalnosti

1. **Audio Player & Media Management**:
   - Reprodukcija formata: **MP3, FLAC, WAV, AAC, M4A, OGG**
   - Standardne kontrole: Play / Pause, Skip Next / Previous, Seekbar, Shuffle, Repeat (One / All / Off)
   - Background playback putem **Jetpack Media3 (`MediaSessionService`)**
   - Lock screen kontrole, notification player widget i podrška za Bluetooth / slušalice

2. **Organizacija i Sortiranje**:
   - Pregled pjesama, albuma, izvođača i playlisti
   - Sortiranje po nazivu, izvođaču, datumu dodavanja i trajanju
   - Brza pretraga glazbe u stvarnom vremenu

3. **Metadata / Tag Editor**:
   - Izmjena ID3 i audio tagova direktno u datoteci preko C++ **TagLib** JNI biblioteke
   - Uređivanje: Naslov, Izvođač, Album, Godina, Žanr, Broj pjesme (Track number)
   - Uređivanje i dodavanje teksta pjesme (Lyrics)
   - Scoped Storage kompatibilnost (Android 11+ `MediaStore.createWriteRequest`)

4. **UX / UI**:
   - Moderan minimalistički dizajn u **Jetpack Compose + Material 3**
   - Mini player na dnu s mogućnošću proširenja u Full Screen Player
   - Spotify-inspired tamni način rada (Dark Mode)
   - 100% offline rad, bez oglasa, bez analitike i bez praćenja

---

## 🛠️ Tehnološki stek i arhitektura

- **Jezik**: Kotlin 2.0+ (Coroutines & StateFlow)
- **UI**: Jetpack Compose + Material 3
- **Reprodukcija**: AndroidX Media3 (ExoPlayer + MediaSession)
- **Tag Editing**: AndroidTagLib (JNI wrapper oko TagLib-a)
- **Baza**: Room Database (SQLite)
- **Dependency Injection**: Hilt (Dagger)
- **Učitavanje slika**: Coil (Compose)
- **Arhitektura**: Clean Architecture + MVVM

---

## 📂 Struktura projekta

```
app/src/main/java/com/pulse/music/
├── PulseApplication.kt           # @HiltAndroidApp
├── data/
│   ├── local/
│   │   ├── db/                   # Room entiteti, DAO i AppDatabase
│   │   ├── mediastore/           # MediaStoreDataSource za skeniranje uređaja
│   │   └── tageditor/            # TagLibDataSource za čitanje/pisanje tagova
│   └── repository/               # Implementacije repozitorija
├── domain/
│   ├── model/                    # Modeli (Song, SongMetadata, Album, Artist, Playlist)
│   ├── repository/               # Sučelja repozitorija
│   └── usecase/                  # UseCases (GetSongs, RefreshSongs, UpdateSongTags)
├── player/
│   ├── AudioPlayerService.kt     # MediaSessionService
│   └── PlayerConnection.kt       # MediaController singleton bridge
├── di/                           # Hilt moduli (Database, Repository)
└── ui/
    ├── MainActivity.kt
    ├── components/               # MiniPlayer, SongListItem
    ├── navigation/               # NavHost i rute
    ├── screens/
    │   ├── home/                 # HomeScreen & HomeViewModel
    │   ├── player/               # PlayerScreen & PlayerViewModel
    │   └── tageditor/            # TagEditorScreen & TagEditorViewModel
    └── theme/                    # Material 3 tema i boje
```

---

## 💻 Pokretanje u Android Studiju

1. Otvorite Android Studio (Ladybug ili noviji).
2. Odaberite **Open** i navigirajte do mape projekta `pulse`.
3. Pustite Gradle Sync da preuzme potrebne zavisnosti.
4. Pokrenite aplikaciju na emulatoru ili fizičkom Android uređaju (Android 8.0 / API 26+).
