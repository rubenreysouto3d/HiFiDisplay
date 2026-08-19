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

During active playback, the compact source badge is the single entry point for source, design, palette, and diagnostics. Transport controls share its top rail and fade away after six seconds, while the source remains available. Tap the artwork, metadata, or progress area to reveal controls; using a transport control or seeking restarts the interval. The custom seek surface expands only while controls are visible and is enabled only when the active session advertises seek support. Artwork, bounded metadata, times, and a thin live progress line remain visible without layout jumps. Track and artwork changes crossfade, touch feedback is restrained, missing artwork uses a palette-aware record treatment, and a minute periodic offset helps protect always-on OLED panels. Configuration remains permanently visible on permission, error, and no-session screens.

## v0.7 display engine

Display structure is now driven by independent design tokens instead of being hard-coded into a single screen. `Modern Reference` keeps the balanced leading-artwork composition, while `Studio Ledger` introduces trailing deck artwork, indexed typography, console controls, a ticked timeline, and a dedicated transient transport rail. Both designs use the same media state and capability rules, and either can be combined with `Hi-Fi Green` or `Warm Amber` without changing layout.

The source/display panel presents both designs as visual previews, shows palette swatches explicitly as color-only choices, updates immediately, persists the selection, and keeps its close action fixed while content scrolls. Layout modes cover compact, standard, and wide landscape displays; 16:9 and 20:9 emulator passes verify that metadata, source, controls, progress, and artwork stay within their zones. The custom seek control also publishes adjustable progress semantics for accessibility.

## v0.8 tactile finish and cutout safety

The playback surface now respects Android display-cutout insets while remaining immersive, keeping artwork and metadata clear of landscape camera holes. Interactive surfaces use restrained custom press states and system-respecting haptic confirmation for reveal, transport, selection, and completed seek actions rather than generic Material ripples.

Artwork motion is independent from design and palette and persists across launches. `Focus` introduces the next cover with subtle depth, `Dissolve` uses a scale-free crossfade, and `Direct` disables the transition. Transitions are keyed to meaningful track/source changes, so repeated bitmap instances from a media-session callback no longer restart the animation or create a periodic flash.

## v0.8.1 interaction foundation

Artwork and metadata taps now toggle between the transient control HUD and ambient mode instead of silently restarting the same timeout. A short in-artwork `CONTROLS` / `AMBIENT` confirmation makes the result explicit, while transport and seek interactions keep controls available without changing modes. The shared interaction reducer covers reveal, toggle, keep-alive, timeout, and overlay behavior independently from any skin.

The source entry is now a quieter compact plate with a 48 dp semantic touch target, lower contrast, and less width. Control entrances combine restrained scale and fade. Artwork motion is deliberately more perceptible, adds a directional `Deck` changeover, and includes an A/B preview inside `SOURCE / DISPLAY`; tapping any effect replays its actual timing before it is used on a track change.

## v0.9 playback presence and signature displays

Ambient mode now removes the complete transient HUD, including the source entry. A tap on the unobstructed display toggles that HUD without moving the permanent composition; the artwork is deliberately reserved for a separate full-screen album-focus view. This removes competing gestures and gives the cover a useful, predictable action.

Track transitions and continuous ambient effects are separate preferences. `Breathe`, `Float`, and `Glow` animate only while the active session reports playback; `Still` disables continuous motion. Their phase is derived from the session's estimated playback position, so the motion remains stable across Compose recompositions and media callbacks. They are explicitly presented as visual-only ambient motion—not audio-reactive or beat-synchronized—because Android `MediaSession` does not expose a beat or amplitude stream.

The display engine now includes five designs. Every design preserves album artwork as a square, including fallback art and the album-focus view. `Monolith Glass` uses a large cinematic square artwork plane that dissolves into editorial metadata, a luminous baseline, and a transient glass HUD instead of nested cards. `Precision Deck` replaces the undersized circular treatment with a substantially larger square cover in a restrained machined frame. Its permanent horizontal timebase occupies the transport area while the HUD is hidden, so ambient mode no longer leaves a dead lower band. Each design also owns a deliberate no-cover treatment. All remain independent from the selected color palette and preserve the same session capability rules as the existing designs.

The visual refinement pass adds `Crystal Atrium`, a clear-glass display built as one suspended optical sheet with a floating cover frame, quiet refractions, editorial metadata, a prismatic progress line, and a single transient glass control capsule. Monolith's HUD is now one thinner integrated band, while Precision reserves accent color primarily for state, timebase progress, and the main transport action.

The motion pass gives every design its own restrained track-change language instead of applying one generic animation everywhere: Modern rises softly, Studio slides like a deck readout, Monolith arrives cinematically, Precision responds quickly, and Crystal resolves through a short optical fade. Continuous `Breathe`, `Float`, and `Glow` effects combine subtle cover movement, edge light, and sheen, run only during reported playback, and return to a completely still state when paused. Transport buttons add a brief confirmation ring alongside press-scale and haptic feedback, so touch acknowledgment remains visible without leaving permanent controls on screen.

The palette system separates color family from light level. Reference Green, Valve Amber, Arctic Cyan, Cobalt, Velvet, and Ruby can each be combined with `Dark`, `Light`, or `OLED`. OLED uses an opaque `#000000` canvas and suppresses nonessential ambient fields and grids so unused pixels remain off; controls and display-specific structure appear only where they carry information. Existing Arctic Silver, Champagne Frost, and OLED Absolute preferences migrate to their equivalent family and mode.

The display panel is organized into `SOURCE`, `APPEARANCE`, and `MOTION`. Appearance places design, light level, and color family in one comparison surface; Motion separates track changeovers from visual-only ambient presence. Press states use clearly visible scale, contrast, and differentiated light/strong haptic confirmation.

Monolith Glass keeps its artwork frame fixed while moving only an overscanned image layer, preventing animated edges from escaping the full-screen fade. Crystal Atrium uses a more architectural optical sheet, restrained diagonal refractions, a double glass edge, a luminous separator, and a rectangular integrated control rail. Metadata reserves space for the transient HUD to prevent long-title overlap.
