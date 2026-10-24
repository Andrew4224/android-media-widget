package de.andrew4224.mediawidget.media

/** What the selection needs from a media session; [handle] is the framework object behind it. */
internal data class Session<T>(val packageName: String, val isPlaying: Boolean, val handle: T)

/** The user's choices that steer which session is shown. */
internal data class SelectionSettings(
    val ignored: Set<String> = emptySet(),
    val favorite: String? = null,
    val selected: String? = null,
)

internal data class Selection<T>(
    val session: Session<T>?,
    val canSwitch: Boolean,
    val isIgnoredPlayerConnected: Boolean,
    /** False once the player picked in the widget has no allowed session anymore. */
    val keepSelected: Boolean,
)

/**
 * Picks the session to show: the player picked in the widget first, then the favorite, then
 * any playing session, then the most recently active one.
 */
internal fun <T> selectSession(
    sessions: List<Session<T>>,
    settings: SelectionSettings
): Session<T>? =
    sessions.firstOrNull { it.packageName == settings.selected }
        ?: sessions.firstOrNull { it.packageName == settings.favorite }
        ?: sessions.firstOrNull { it.isPlaying }
        // NOTE: The framework orders active sessions by priority and recent activity,
        // so the first entry is treated as the "last active" session.
        ?: sessions.firstOrNull()

internal fun <T> chooseSession(
    sessions: List<Session<T>>,
    settings: SelectionSettings
): Selection<T> {
    val allowed = sessions.filter { it.packageName !in settings.ignored }
    return Selection(
        session = selectSession(allowed, settings),
        canSwitch = allowed.distinctBy { it.packageName }.size > 1,
        isIgnoredPlayerConnected = allowed.size < sessions.size,
        keepSelected = settings.selected == null || allowed.any { it.packageName == settings.selected },
    )
}

/** Without a session to show: the remembered track, unless its player has been turned off since. */
internal fun fallbackState(
    remembered: MediaState.Active?,
    selection: Selection<*>,
    ignored: Set<String>
): MediaState =
    remembered
        ?.takeIf { it.packageName !in ignored }
        ?.copy(isIgnoredPlayerConnected = selection.isIgnoredPlayerConnected)
        ?: MediaState.NoSession

/** The player after [current] in a stable order, or null if there is nothing to switch to. */
internal fun nextPlayer(packageNames: List<String>, current: String?): String? {
    val players = packageNames.distinct().sorted()
    if (players.size < 2) return null
    return players[(players.indexOf(current) + 1) % players.size]
}
