# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1] - 2026-10-18

### Added

- 4x1 home screen widget (Jetpack Glance) showing cover, title and artist of the current media session.
- Previous, play/pause and next buttons that control the active player via `MediaController`.
- Session selection: Spotify first, then any playing session, then the most recently active one.
- Responsive layout: cover, text and buttons when wide; buttons only when narrow.
- Tapping cover or text opens the player app.
- Special states for missing notification access and for "nothing playing".
- Settings screen showing the notification access status and the detected session.
- GitHub Actions workflows for CI builds and signed tag releases.

[Unreleased]: https://github.com/OWNER/REPO/compare/v0.1...HEAD
[0.1]: https://github.com/OWNER/REPO/releases/tag/v0.1
