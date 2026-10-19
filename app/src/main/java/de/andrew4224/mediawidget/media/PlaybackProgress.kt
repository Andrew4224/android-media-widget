package de.andrew4224.mediawidget.media

/** Playback position as a fraction of [duration], extrapolated while playing. */
internal fun playbackProgress(
    position: Long,
    duration: Long,
    elapsedSinceUpdate: Long,
    speed: Float,
    isPlaying: Boolean,
): Float? {
    if (duration <= 0 || position < 0) return null
    val current = if (isPlaying) position + (elapsedSinceUpdate * speed).toLong() else position
    return (current.toFloat() / duration).coerceIn(0f, 1f)
}
