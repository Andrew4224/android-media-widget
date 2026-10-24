@file:Suppress("unused")

package de.andrew4224.mediawidget.widget

import androidx.compose.runtime.Composable
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import de.andrew4224.mediawidget.R
import de.andrew4224.mediawidget.media.MediaSessions.SPOTIFY_PACKAGE
import de.andrew4224.mediawidget.media.MediaState

private val previewTrack: MediaState.Active
    @Composable get() = MediaState.Active(
        packageName = SPOTIFY_PACKAGE,
        title = stringResource(R.string.preview_title),
        artist = stringResource(R.string.preview_artist),
        cover = null,
        isPlaying = true,
        progress = 0.4f,
    )

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 100)
@Composable
private fun WidePreview() = WidgetTheme { MediaWidget.WidgetContent(previewTrack) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 72)
@Composable
private fun CompactPreview() = WidgetTheme { MediaWidget.WidgetContent(previewTrack) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 48)
@Composable
private fun FlatPreview() = WidgetTheme { MediaWidget.WidgetContent(previewTrack) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 100)
@Composable
private fun WidePausedWithoutArtistPreview() =
    WidgetTheme { MediaWidget.WidgetContent(previewTrack.copy(artist = null, isPlaying = false)) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 160, heightDp = 160)
@Composable
private fun NarrowTallPreview() = WidgetTheme { MediaWidget.WidgetContent(previewTrack) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 160, heightDp = 96)
@Composable
private fun NarrowTallMinimumPreview() = WidgetTheme { MediaWidget.WidgetContent(previewTrack) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 160, heightDp = 160)
@Composable
private fun NarrowTallPausedWithoutArtistPreview() =
    WidgetTheme { MediaWidget.WidgetContent(previewTrack.copy(artist = null, isPlaying = false)) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 160, heightDp = 80)
@Composable
private fun NarrowPreview() =
    WidgetTheme { MediaWidget.WidgetContent(previewTrack.copy(isPlaying = false)) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 64, heightDp = 64)
@Composable
private fun SingleCellPreview() = WidgetTheme { MediaWidget.WidgetContent(previewTrack) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 64, heightDp = 160)
@Composable
private fun SingleColumnTallPreview() = WidgetTheme { MediaWidget.WidgetContent(previewTrack) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 64)
@Composable
private fun NoAccessPreview() = WidgetTheme { MediaWidget.WidgetContent(MediaState.NoAccess) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 56)
@Composable
private fun NoSessionPreview() = WidgetTheme { MediaWidget.WidgetContent(MediaState.NoSession) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 160, heightDp = 80)
@Composable
private fun NoAccessNarrowPreview() = WidgetTheme { MediaWidget.WidgetContent(MediaState.NoAccess) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 160, heightDp = 80)
@Composable
private fun NoSessionNarrowPreview() =
    WidgetTheme { MediaWidget.WidgetContent(MediaState.NoSession) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 64, heightDp = 64)
@Composable
private fun NoAccessSingleCellPreview() =
    WidgetTheme { MediaWidget.WidgetContent(MediaState.NoAccess) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 64, heightDp = 64)
@Composable
private fun NoSessionSingleCellPreview() =
    WidgetTheme { MediaWidget.WidgetContent(MediaState.NoSession) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 100)
@Composable
private fun IgnoredPlayerConnectedPreview() = WidgetTheme {
    MediaWidget.WidgetContent(
        previewTrack.copy(
            isPlaying = false,
            isRemembered = true,
            isIgnoredPlayerConnected = true,
            progress = null
        ),
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 100)
@Composable
private fun SwitchPlayerPreview() =
    WidgetTheme { MediaWidget.WidgetContent(previewTrack.copy(canSwitchPlayer = true)) }
