# Video Surface Resume: Reproduction and Expected Behavior

## Reproduction steps

1. Install the release APK and open any local video from the library.
2. Confirm both picture and sound are playing.
3. Open an Android system file picker from the app (for example, choose an external subtitle, or use the Files tab to select another media item).
4. Return to SOLO Player without closing the current player screen.
5. Repeat in portrait and landscape. Also try cancelling the picker and returning without selecting anything.
6. While returning, check the video image, playback position, and Play/Pause state.

## Expected resume behavior

- Returning from a picker keeps the player on the same screen and orientation.
- The active video surface is attached to the current player view after Activity resume/window focus.
- Video and audio continue at the same playback position and retain the same playing or paused state.
- Background media-library scans must not rebuild the Player view or detach the video surface.
- Selecting another video opens that video and renders it in the existing player surface; cancelling a picker leaves the current video untouched.
- Audio-only playback remains unaffected by video-surface recovery.

## Implemented lifecycle handling

- Video surface recovery is scheduled after Activity resume, window focus restoration, and file-picker results.
- A library scan that finishes while the Player screen is active updates library data without rebuilding or detaching the Player view.
- Media picker results do not issue a second Player render after the selected item has already been opened.
