# HiFiDisplay

HiFiDisplay is a landscape Android display and controller for the active system media session. It does not play media itself: it shows metadata exposed through Android's `MediaSession` APIs and sends supported commands through `MediaController.TransportControls`.

## Requirements

- Android 8.0 (API 26) or newer
- Notification access enabled for HiFiDisplay
- A media app that publishes an active Android media session

## Build and test

```bash
./gradlew :app:lintDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## v0.2 media core

The media layer keeps session arbitration, playback capabilities, position estimation, and Android lifecycle handling separate from the Compose UI. It can automatically follow the playing session or persist a pinned source application for a future session picker.

## v0.3 session safety

Transport commands are validated against the latest session actions inside the repository, not only in the UI. Controller reads and callback registration tolerate sessions that disappear mid-operation, while playback speed and seek positions are sanitized before updating the display or sending commands.

## v0.4 compatibility diagnostics

Long-press the source application name while a session is active to open a privacy-safe diagnostics overlay. It reports the package, playback state, advertised controls, metadata availability, and retry state without displaying or storing notification contents.

Suggested device compatibility pass:

| App | Session detected | App switching | Controls | Seek | Metadata/artwork |
| --- | --- | --- | --- | --- | --- |
| Spotify | ☐ | ☐ | ☐ | ☐ | ☐ |
| YouTube Music | ☐ | ☐ | ☐ | ☐ | ☐ |
| Poweramp | ☐ | ☐ | ☐ | ☐ | ☐ |
| Other player | ☐ | ☐ | ☐ | ☐ | ☐ |

## v0.5 sources and appearance

Use the always-visible `SOURCE / DISPLAY` button to choose automatic session following or pin a specific active media app, even when no session is playing. Display design and color palette are independent: `Modern Reference` currently defines structure, typography, and interaction, while `Hi-Fi Green` and `Warm Amber` are interchangeable palettes. Preferences persist across launches and migrate the earlier skin setting automatically. Session diagnostics is also an explicit tool in this panel.

## v0.6 ambient interaction

During active playback, controls and the `SOURCE / DISPLAY` entry fade away after six seconds to leave a clean Hi-Fi display. Tap anywhere to reveal them; using a transport control or seeking restarts the interval. Artwork, metadata, times, and a thin live progress line remain visible, and the layout keeps its dimensions while controls fade so the display never jumps. Configuration remains permanently visible on permission, error, and no-session screens.
