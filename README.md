# Media Widget

A tiny Android app that provides a **single-row (4x1) home screen widget** to show and control the
media that is currently playing. It is made with Spotify in mind, but works with every player that
publishes an Android `MediaSession`.

Why? The official Spotify widget cannot be made smaller than 4x2 on some launchers. This one is exactly
one row high.

![Screenshot](docs/screenshot.png)
<!-- TODO: add a screenshot at docs/screenshot.png -->

## Features

- Cover art, title and artist of the current track
- Previous / play-pause / next buttons
- Tap cover or text to open the player app
- Prefers Spotify, otherwise the session that is playing, otherwise the most recently active one
- Responsive: shrinks down to just the three buttons
- Material You dynamic colors, light and dark theme
- No network access, no analytics, no Spotify SDK – only Android framework APIs

## Permission: notification access

The app asks for **notification access** (Settings → Notifications → Device & app notifications).
Android only allows apps with this access to list the active media sessions
(`MediaSessionManager.getActiveSessions`). The app **does not read, store or send any notifications**;
the notification listener exists solely to unlock media session access.

Without the permission the widget shows "Tap to grant access", which opens the right settings page.

## Installation

1. Download the latest `media-widget-vX.Y.apk` from [GitHub Releases](../../releases).
2. Install it (you may need to allow installing apps from your browser / file manager).
3. Open **Media Widget** and grant notification access.
4. Long-press your home screen → **Widgets** → **Media Widget** → drag **Media controls** onto the home screen.

## Building

Requirements: JDK 17 or newer (a full JDK including `javac`, e.g. the one bundled with Android Studio)
and the Android SDK (`ANDROID_HOME` or `local.properties` with `sdk.dir`).

```sh
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # minified; unsigned unless signing variables are set
./gradlew lint
```

### Release signing

Signing is configured only through environment variables, nothing is stored in the repository:

| Variable            | Meaning                                    |
|---------------------|--------------------------------------------|
| `KEYSTORE_FILE`     | Path to the keystore (`.jks`)              |
| `KEYSTORE_PASSWORD` | Keystore password                          |
| `KEY_ALIAS`         | Key alias                                  |
| `KEY_PASSWORD`      | Key password                               |

If `KEYSTORE_FILE` is not set, `assembleRelease` produces `app-release-unsigned.apk`.

For GitHub Actions, add these repository secrets: `KEYSTORE_BASE64` (the keystore, base64-encoded,
e.g. `base64 -w0 release.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. The release workflow
decodes the keystore to a temporary file and sets `KEYSTORE_FILE`.

### Releasing

1. Update `versionCode` / `versionName` in `app/build.gradle.kts` and add a section to `CHANGELOG.md`.
2. Push a tag, e.g. `git tag v0.1 && git push origin v0.1`.
3. The `Release` workflow builds the signed APK and creates a GitHub release using the matching
   changelog section as release notes.

### Changing the package name

`de.example.mediawidget` is a placeholder. Change `namespace` and `applicationId` in
`app/build.gradle.kts` and move the sources under `app/src/main/java/` accordingly.

## Known limitations

- Some launchers do not allow widgets to be resized to a single row, or have rows that are too small
  for the 40 dp minimum height. The widget was tested with the Fairphone 6 default launcher (Quickstep).
- No progress bar or seeking (intentionally, to avoid constant widget updates).
- If several players are active, Spotify always wins; there is no way to pick another player yet.
- Some players only expose controls while they are running in the background; if a player was killed
  by the system, the widget shows "Nothing is playing" until the player is opened again.
- Android may delay widget updates while the device is in deep sleep / battery saver.

## License

Licensed under the [Apache License 2.0](LICENSE).
