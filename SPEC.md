# Media-Widget (4x1) – Spezifikation

## Ziel

Eine kleine Android-App mit einem Homescreen-Widget im Format **4x1**, das die aktuell aktive Medienwiedergabe anzeigt und steuert. Primär für Spotify gedacht, soll aber mit jedem Player funktionieren, der eine `MediaSession` bereitstellt.

Hintergrund: Das offizielle Spotify-Widget lässt sich auf dem Zielgerät nicht kleiner als 4x2 machen. Dieses Widget soll genau eine Zeile hoch sein.

## Technischer Rahmen

- Sprache: Kotlin
- Widget: Jetpack Glance (aktuelle stabile Version)
- UI der App: Jetpack Compose in einem Fragment, Zustand im ViewModel (nur ein einfacher Einstellungs-Screen)
- Sprache im Code: durchgehend Englisch (siehe „Hinweise für die Umsetzung“)
- Build: Gradle Kotlin DSL mit Version Catalog (`libs.versions.toml`)
- `minSdk` 26, `targetSdk` und `compileSdk` aktuellste stabile Version
- Testgerät: Fairphone 6 (Standard-Launcher Quickstep)
- Paketname: `de.example.mediawidget` (Platzhalter, leicht änderbar halten)
- Keine Netzwerkzugriffe, keine Analytics, keine unnötigen Abhängigkeiten
- Kein Spotify-SDK; ausschließlich Android-Framework-APIs für Mediensteuerung

## Architektur

### Zugriff auf Mediensessions

- `NotificationListenerService` als Komponente registrieren (wird nur benötigt, damit `MediaSessionManager.getActiveSessions(ComponentName)` erlaubt ist; Benachrichtigungen selbst werden nicht ausgewertet).
- Über `MediaSessionManager.addOnActiveSessionsChangedListener` auf neue/beendete Sessions reagieren.
- Für die gewählte Session einen `MediaController.Callback` registrieren (`onMetadataChanged`, `onPlaybackStateChanged`, `onSessionDestroyed`) und bei Änderungen das Widget aktualisieren.

### Auswahl der Session

1. Session von `com.spotify.music`, falls vorhanden.
2. Sonst die erste Session mit `PlaybackState.STATE_PLAYING`.
3. Sonst die zuletzt aktive Session.
4. Sonst: Leerzustand.

### Widget-Zustand

- Zustand (Titel, Interpret, Cover, Wiedergabestatus, Paketname des Players) über Glance-State (`PreferencesGlanceStateDefinition`) oder ein kleines Repository halten.
- Aktualisierung über `GlanceAppWidget.updateAll()` bzw. gezielt pro `GlanceId`.
- Keine Fortschrittsanzeige / Live-Position (vermeidet ständige Widget-Updates).

### Aktionen

- Zurück, Play/Pause, Weiter als Glance-`ActionCallback`.
- Im Callback den passenden `MediaController` über `MediaSessionManager.getActiveSessions` holen und `transportControls` aufrufen.
- Tippen auf Cover oder Text öffnet die Player-App über `MediaController.sessionActivity`; Fallback: Launch-Intent des Pakets.

### Cover

- Bitmap aus `METADATA_KEY_ALBUM_ART` bzw. `METADATA_KEY_ART`.
- Vor der Übergabe ans Widget stark verkleinern (z. B. max. 128×128 px), um das Größenlimit von `RemoteViews` nicht zu überschreiten.
- Fallback: neutrales Platzhalter-Icon.

## Widget-Layout

Standard (4x1, eine Zeile):

```
[Cover] Titel / Interpret (je 1 Zeile, ellipsiert)   [⏮] [⏯] [⏭]
```

- `SizeMode.Responsive` mit mindestens zwei Größen:
  - breit: Cover + Text + drei Buttons
  - schmal: nur drei Buttons (ggf. Cover)
- `AppWidgetProviderInfo`:
  - `targetCellWidth="4"`, `targetCellHeight="1"`
  - `minHeight` / `minResizeHeight` so klein wählen, dass das Widget sicher in **eine** Zeile passt (ca. 40–50 dp)
  - horizontal und vertikal resizable
- Farben über `GlanceTheme` (dynamische Farben, Hell/Dunkel).
- Ausreichend große Touch-Ziele für die Buttons, Content-Descriptions setzen.

### Sonderzustände

- **Kein Benachrichtigungszugriff:** Widget zeigt „Zugriff erteilen“; Tippen öffnet `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`.
- **Keine aktive Session:** dezenter Hinweis „Nichts wird abgespielt“; Tippen öffnet Spotify, falls installiert.

## App-Screen

Aufbau nach dem Muster **Activity → Fragment → ViewModel**, UI in Compose:

- Eine `MainActivity` (`FragmentActivity`/`AppCompatActivity`), die nur das Fragment hostet.
- Ein `SettingsFragment`, das seine UI über `ComposeView` mit `setContent { … }` rendert (inkl. `ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed`).
- Ein `SettingsViewModel` (`androidx.lifecycle.ViewModel`), das den UI-State als `StateFlow` bereitstellt; das Composable sammelt ihn mit `collectAsStateWithLifecycle()`.
- Logik (Berechtigungsstatus, erkannte Session) gehört ins ViewModel, nicht ins Fragment oder Composable.

Inhalt des Screens:

- Status des Benachrichtigungszugriffs + Button zum Öffnen der Einstellungen
- aktuell erkannte Session (Paketname, Titel) zur Kontrolle
- kurzer Hinweis, wie man das Widget hinzufügt

## Nicht im Umfang

- Spotify-SDK / Login
- Playlists, Suche, Warteschlange
- Fortschrittsbalken / Seek
- Konfigurations-Activity für das Widget

## Abnahmekriterien

- `./gradlew assembleDebug` läuft ohne Fehler durch.
- Widget lässt sich auf dem Homescreen als 4x1 platzieren.
- Titel, Interpret und Cover aktualisieren sich bei Liedwechsel in Spotify innerhalb weniger Sekunden.
- Zurück, Play/Pause und Weiter funktionieren zuverlässig, auch nachdem die App länger nicht geöffnet war.
- Play/Pause-Icon spiegelt den tatsächlichen Wiedergabestatus.
- Tippen auf Cover/Text öffnet den Player.
- Ohne Berechtigung bzw. ohne Wiedergabe wird der passende Sonderzustand angezeigt.
- `./gradlew assembleRelease` baut auch ohne lokale Signierdaten; das minifizierte Release-APK funktioniert vollständig.
- Beide Workflows sind lauffähig; ein Tag `v0.1` erzeugt ein GitHub Release mit APK.
- Im Repository liegen keine Geheimnisse, Keystores oder `local.properties`.

## Veröffentlichung auf GitHub (Version 0.1)

Das Repository soll so vorbereitet sein, dass es direkt öffentlich auf GitHub gestellt und als **v0.1** veröffentlicht werden kann.

### Versionierung

- `versionName = "0.1"`, `versionCode = 1` in `app/build.gradle.kts`.
- Git-Tag für das Release: `v0.1`.

### Dateien im Repository

- `README.md` (Englisch): Kurzbeschreibung, Screenshot-Platzhalter, Funktionsumfang, benötigte Berechtigung (Benachrichtigungszugriff) und warum, Installation (APK aus GitHub Releases), Build-Anleitung, bekannte Einschränkungen (z. B. Launcher, die Widgets nicht auf 1 Zeile verkleinern).
- `CHANGELOG.md` mit einem Eintrag für `0.1` (Format: Keep a Changelog).
- `LICENSE`: **nicht selbst wählen.** Im README einen Abschnitt „License“ mit Platzhalter `TBD` anlegen; die Lizenz legt der Maintainer fest.
- `.gitignore` für Android/Gradle/Android Studio (u. a. `local.properties`, `build/`, `.gradle/`, `*.jks`, `*.keystore`).
- Gradle Wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`) einchecken.

### Release-Build und Signierung

- `release`-BuildType mit `isMinifyEnabled = true` und passenden ProGuard/R8-Regeln; die App muss im Release-Build voll funktionieren.
- Signierung ausschließlich über Umgebungsvariablen bzw. GitHub-Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Keine Keystores, Passwörter oder anderen Geheimnisse im Repository.
- Fehlen die Signierdaten lokal, muss `assembleRelease` trotzdem bauen können (unsigniert), statt abzubrechen.

### GitHub Actions

- `.github/workflows/build.yml`: bei Push und Pull Request auf `main` → `./gradlew assembleDebug lint`.
- `.github/workflows/release.yml`: bei Push eines Tags `v*` → signiertes Release-APK bauen und als Asset an ein GitHub Release mit dem Tag-Namen anhängen; Release-Text aus dem passenden `CHANGELOG.md`-Abschnitt.
- Actions auf feste Versionen pinnen.

## Hinweise für die Umsetzung

- Code klein und lesbar halten; ViewModel und Fragment wie oben beschrieben, aber keine weiteren Architektur-Frameworks (kein Hilt nötig).
- **Der gesamte Code ist auf Englisch:** Bezeichner, Kommentare, Log-Meldungen, Ressourcen-Namen und die Standard-Strings in `res/values/strings.xml`. Die deutschen UI-Texte in dieser Spezifikation sind nur Beispiele und werden sinngemäß auf Englisch übernommen.
- Bei Unklarheiten die einfachste funktionierende Lösung wählen und im Code mit `// NOTE:` kennzeichnen.
