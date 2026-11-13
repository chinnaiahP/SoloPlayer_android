# SOLO Player Android Architecture

## Current package responsibilities

```text
com.soloplayer.android
  MainActivity                         Activity lifecycle, navigation, and legacy screen coordination
  AudioPlaybackService                 Android foreground audio session and notification
  SoloCastOptionsProvider              Google Cast options integration

com.soloplayer.android.model
  MediaItem                            Shared media entity used by library, stream, and player layers

com.soloplayer.android.data.library
  MediaLibraryRepository               Android MediaStore queries and media metadata mapping

com.soloplayer.android.data.repository
  StreamBookmarkRepository             Bookmark persistence, validation, and legacy JSON migration

com.soloplayer.android.streaming
  M3uPlaylistParser                    Pure parsing of extended M3U metadata and stream entries

com.soloplayer.android.ui.player
  AudioArtworkEffectView               Reusable playback-state artwork animation
```

## Data and UI flow

- `MainActivity` owns Android navigation, permission prompts, view lifecycle, and screen composition.
- Media scans run on the activity's worker executor. `MediaLibraryRepository` returns domain `MediaItem` values; the activity applies app-specific privacy filtering and updates the visible library on the main thread.
- Stream URL fetching and timeout handling stay with the streaming screen coordinator. The fetched text is handed to `M3uPlaylistParser`, which has no Activity or view dependencies and returns parsed channel metadata. The activity maps those results into library entities.
- `StreamBookmarkRepository` owns bookmark serialization and persistence. It migrates the existing `data.streamBookmarks` records into its dedicated preference key on first load, while playlist/history persistence remains backward compatible.
- `AudioArtworkEffectView` receives theme colors and playback state from the player screen. It does not own or control the media session.
- `AudioPlaybackService` owns the Android foreground notification/media session. `SoloCastOptionsProvider` supplies Cast SDK configuration.

## Dependency rules

1. Domain models must not depend on Android views, Activities, or storage APIs.
2. Repositories own data access and return domain values; they do not render screens or show dialogs.
3. Parsers and other streaming transformations must remain independent of the Activity and Android UI.
4. Reusable views receive the data and state they need; they do not fetch media or mutate navigation.
5. Expensive scans and network reads execute off the main thread. UI updates are posted back to the main thread.

## Refactoring boundary

The project currently uses a programmatic, single-Activity UI. The extracted model, repository, parser, and player view establish real data and component boundaries without changing screen behavior or the saved-data format. Existing screen composition and playback orchestration are still coordinated in `MainActivity`; future screen extraction should move one feature at a time behind screen contracts, with the Activity retaining navigation and lifecycle ownership.
