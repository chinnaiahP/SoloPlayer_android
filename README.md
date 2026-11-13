# 🎬 SOLO Player

A privacy-focused Android media player for playing local videos, music, and live network streams with a modern dark-purple UI.

## ✨ Features

- **Video Playback:** LibVLC-powered playback, multiple aspect ratios, PiP mode, subtitles, A-B repeat, and playback resume.
- **Audio Playback:** Background playback, audio and bass boost, equalizer, audio delay, and media controls.
- **Network Streaming:** M3U/M3U8 IPTV playlists, channel search, bookmarks, and configurable network cache.
- **Google Cast:** Chromecast support for local and network media.
- **Private Vault:** Biometric/PIN protection and AES-GCM encrypted media storage.
- **Media Library:** Automatic scanning, Movies, Music, Albums, Artists, Folders, search, history, and custom playlists.
- **User Interface:** Dark theme, neon-purple accents, mini player, sleep timer, and orientation controls.

## 🛠️ Tech Stack

| Component | Technology |
|---|---|
| Language | Java |
| UI | Android Views (Programmatic UI) |
| Media Engine | LibVLC 3.4.9 |
| Casting | Google Cast SDK |
| Authentication | AndroidX Biometric |
| Encryption | AES-GCM |
| Build System | Gradle 8.1.1 / AGP 7.4.1 |
| Min SDK | Android 7.0 (API 24) |
| Target SDK | Android 13 (API 33) |

## 📁 Project Structure

```text
SoloPlayer_android/
├── app/
│   └── src/main/
│       ├── java/com/soloplayer/android/
│       │   ├── MainActivity.java
│       │   ├── AudioPlaybackService.java
│       │   ├── SoloCastOptionsProvider.java
│       │   ├── model/
│       │   ├── data/
│       │   ├── streaming/
│       │   └── ui/player/
│       └── res/
├── docs/
│   └── ARCHITECTURE.md
├── gradle/
├── build.gradle
└── app/build.gradle
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11 or later
- Android SDK API 33

### Clone Repository

```bash
git clone <repository-url>
cd SoloPlayer_android
```

Open the project in Android Studio, sync Gradle, and run the application on an Android device or emulator.

### Build APK

```bash
./gradlew assembleRelease
```

Generated APKs are available in:

```text
app/build/outputs/apk/release/
```

## 📱 Supported ABIs

- arm64-v8a
- armeabi-v7a
- x86
- x86_64
- Universal APK

## 🔐 Permissions

The application uses permissions for:
- Internet and network access
- Media and storage access
- Wi-Fi multicast for Cast discovery
- Background media playback
- Wake lock management

## 🏗️ Architecture

The application follows a single-Activity, programmatic-UI architecture.

- **MainActivity:** Navigation and UI management.
- **Models:** Shared media data structures.
- **Repositories:** Media library and bookmark management.
- **Parsers:** M3U/M3U8 playlist processing.
- **Custom Views:** Audio artwork effects and playback UI.

Refer to `docs/ARCHITECTURE.md` for detailed architecture documentation.

## 📄 License

This project is proprietary. All rights reserved.

---

**Built with ❤️ for the best solo media experience on Android.**
