# HiFiDisplay

HiFiDisplay is a landscape Android display and controller for the active system media session. It does not play media itself: it shows metadata exposed through Android's `MediaSession` APIs and sends supported commands through `MediaController.TransportControls`.

## Requirements

- Android 8.0 (API 26) or newer
- Notification access enabled for HiFiDisplay
- A media app that publishes an active Android media session

## Build and test

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## v0.2 media core

The media layer keeps session arbitration, playback capabilities, position estimation, and Android lifecycle handling separate from the Compose UI. It can automatically follow the playing session or persist a pinned source application for a future session picker.

## v0.3 session safety

Transport commands are validated against the latest session actions inside the repository, not only in the UI. Controller reads and callback registration tolerate sessions that disappear mid-operation, while playback speed and seek positions are sanitized before updating the display or sending commands.
