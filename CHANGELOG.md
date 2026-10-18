# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/Andrew4224/android-media-widget/compare/v0.1...HEAD
[0.1]: https://github.com/Andrew4224/android-media-widget/releases/tag/v0.1
