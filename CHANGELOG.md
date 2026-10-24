# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.3] - 2026-10-25

### Added

- Every player that shows up is remembered and listed on a separate players page in the settings.
  Players that are turned off there are ignored by the widget.
- A favorite player can be chosen on the players page. It replaces the fixed preference for
  Spotify, which stays the default favorite.
- Players can be forgotten by swiping them away on the players page. Uninstalled players and their
  covers are removed automatically. The page shows the app icon of each player.
- If several players are active, a switch button next to the title shows the next one. The picked
  player stays in the widget as long as it has a media session.
- If a turned-off player is connected, the widget shows an open symbol on the cover and the play
  button. Play then opens the shown player and starts playback, because Android would otherwise
  send it to the turned-off player.

### Changed

- "Detected session" in the settings shows the app name of the player, with the package name below.

### Fixed

- The cover no longer disappears when a player briefly sends its track info without a cover. The
  last cover of each player is kept until the player sends a new one, also for the remembered
  track.

## [0.2] - 2026-10-19

### Added

- Settings show the artist of the detected session and mark a remembered track as such.
- Settings explain how to allow restricted settings when Android blocks notification access, and
  that the access can be revoked at any time.
- Battery optimization section in the settings with a button that opens the app info.
- Screenshot, supported sizes, troubleshooting, known limitations and building sections in the
  README.
- Narrow widgets that are tall enough show title and artist above the buttons.
- The widget can be resized down to a single cell, showing only the three buttons. Missing access
  and "nothing playing" are shown there as a lock and a question mark icon.
- Playback progress as a subtle fill behind the widget, updated while the screen is on and unlocked.
  The update interval (1, 2, 3, 5 or 10 seconds, default 5) or turning it off can be chosen in the
  settings, which show the measured extra CPU load for the selected interval.

### Changed

- The widget keeps showing the last track when the player has released its media session (long
  pause, player killed, reboot) instead of "Nothing is playing". The buttons then send media keys,
  so the system restarts the last active player.
- Clearer widget messages for missing access and "nothing playing", shown on up to two lines.
  Tapping the missing access message now opens the app instead of failing silently.
- The "Adding the widget" section in the settings is hidden once a widget has been placed, and the
  "Detected session" section is hidden while notification access is missing.
- Settings have a tinted background, a music note next to the title, and buttons in the widget
  color with an arrow.
- Shorter README focused on users.
- The remembered track and its cover are excluded from Android backups, so a restored device does
  not show an old track. The settings are still backed up.

### Fixed

- The widget no longer stops updating when Android disconnects the notification listener: the app
  requests a rebind right away, on every widget update and every 30 minutes.
- The widget no longer stops reacting to play, pause and progress when no session was found at the
  moment the notification listener connected, e.g. right after an app update.

## [0.1] - 2026-10-19

First public release.

### Added

- 4x1 home screen widget (Jetpack Glance) showing cover, title and artist of the current media session.
- Previous, play/pause and next buttons that control the active player via `MediaController`.
- Session selection: Spotify first, then any playing session, then the most recently active one.
- Layout that uses the whole widget area: cover on the left at full height, title and artist on the
  right with the buttons below them. The buttons grow with the widget.
- Adapts to the widget size: title and artist share one line on flat widgets, buttons move next to
  the text on very flat widgets, and only the buttons are shown on narrow widgets.
- Colors follow the system color settings (Material You) on Android 12+, with Spotify green as
  fallback on older versions; the button area is slightly darker than the background.
- Tapping cover or text opens the player app.
- Special states for missing notification access and for "nothing playing".
- Settings screen showing the notification access status and the detected session.
- GitHub Actions workflows for CI builds and signed tag releases.

[Unreleased]: https://github.com/Andrew4224/android-media-widget/compare/v0.3...HEAD

[0.3]: https://github.com/Andrew4224/android-media-widget/compare/v0.2...v0.3
[0.2]: https://github.com/Andrew4224/android-media-widget/compare/v0.1...v0.2
[0.1]: https://github.com/Andrew4224/android-media-widget/releases/tag/v0.1
