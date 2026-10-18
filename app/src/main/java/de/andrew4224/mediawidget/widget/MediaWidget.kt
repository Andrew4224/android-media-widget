package de.andrew4224.mediawidget.widget

import android.content.Context
import android.content.Intent
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import de.andrew4224.mediawidget.R
import de.andrew4224.mediawidget.media.MediaRepository
import de.andrew4224.mediawidget.media.MediaSessions.SPOTIFY_PACKAGE
import de.andrew4224.mediawidget.media.MediaState

class MediaWidget : GlanceAppWidget() {

    // NOTE: Exact instead of Responsive, because the cover is sized to the real widget height
    // and the buttons stretch over the remaining area.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Re-read on every (re)start so the widget is correct even after process death.
        MediaRepository.refresh(context)
        provideContent {
            val state by MediaRepository.state.collectAsState()
            WidgetTheme {
                WidgetContent(state)
            }
        }
    }

    companion object {
        /** Below this width only the three buttons are shown. */
        private val MIN_WIDTH_FOR_INFO = 200.dp

        /** Below this height title and artist share one line to leave room for the buttons. */
        private val MIN_HEIGHT_FOR_TWO_LINES = 88.dp

        /** Below this height the buttons sit next to the text instead of below it. */
        private val MIN_HEIGHT_FOR_STACKED = 64.dp
        private val PADDING = 6.dp

        @Composable
        internal fun WidgetContent(state: MediaState) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .appWidgetBackground()
                    .cornerRadius(16.dp)
                    .background(primaryContainer)
                    .padding(PADDING),
                contentAlignment = Alignment.Center,
            ) {
                when (state) {
                    MediaState.NoAccess -> Message(
                        text = stringResource(R.string.widget_grant_access),
                        onClick = actionStartActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)),
                    )
                    MediaState.NoSession -> Message(
                        text = stringResource(R.string.widget_nothing_playing),
                        onClick = openPlayer(packageName = null),
                    )
                    is MediaState.Active -> Player(state)
                }
            }
        }

        @Composable
        private fun Message(text: String, onClick: Action) {
            Box(
                modifier = GlanceModifier.fillMaxSize().clickable(onClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    maxLines = 1,
                    style = TextStyle(color = onPrimaryContainer, fontSize = 14.sp),
                )
            }
        }

        @Composable
        private fun Player(state: MediaState.Active) {
            val size = LocalSize.current
            if (size.width < MIN_WIDTH_FOR_INFO) {
                Buttons(state, GlanceModifier.fillMaxSize())
                return
            }
            val open = openPlayer(state.packageName)
            val innerHeight = (size.height - PADDING * 2).coerceAtLeast(24.dp)
            Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                // Square cover over the full inner height, flush left.
                Cover(state, open, innerHeight)
                if (size.height < MIN_HEIGHT_FOR_STACKED) {
                    // Very flat widget: one row with text and buttons side by side.
                    Box(modifier = GlanceModifier.defaultWeight().padding(horizontal = 8.dp)) {
                        TrackInfo(state, open, singleLine = false)
                    }
                    Buttons(state, GlanceModifier.width(innerHeight * 3).fillMaxHeight())
                } else {
                    Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight().padding(start = 8.dp)) {
                        TrackInfo(state, open, singleLine = size.height < MIN_HEIGHT_FOR_TWO_LINES)
                        Spacer(GlanceModifier.height(4.dp))
                        Buttons(state, GlanceModifier.fillMaxWidth().defaultWeight())
                    }
                }
            }
        }

        @Composable
        private fun Cover(state: MediaState.Active, open: Action, size: Dp) {
            val description = stringResource(R.string.cd_cover)
            val modifier = GlanceModifier.size(size).cornerRadius(12.dp).clickable(open)
            if (state.cover != null) {
                Image(
                    provider = ImageProvider(state.cover),
                    contentDescription = description,
                    contentScale = ContentScale.Crop,
                    modifier = modifier,
                )
            } else {
                Image(
                    provider = ImageProvider(R.drawable.ic_music_note),
                    contentDescription = description,
                    colorFilter = ColorFilter.tint(onPrimaryContainer),
                    modifier = modifier.background(DarkenOverlay).padding(size / 4),
                )
            }
        }

        @Composable
        private fun TrackInfo(state: MediaState.Active, open: Action, singleLine: Boolean) {
            val title = state.title ?: stringResource(R.string.widget_unknown_title)
            val artist = state.artist?.takeIf { it.isNotBlank() }
            val titleStyle = TextStyle(color = onPrimaryContainer, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Column(modifier = GlanceModifier.fillMaxWidth().clickable(open)) {
                if (singleLine) {
                    Text(
                        text = if (artist == null) title else stringResource(R.string.widget_title_artist, title, artist),
                        maxLines = 1,
                        style = titleStyle,
                    )
                } else {
                    Text(text = title, maxLines = 1, style = titleStyle)
                    if (artist != null) {
                        Text(
                            text = artist,
                            maxLines = 1,
                            style = TextStyle(color = onPrimaryContainer, fontSize = 14.sp),
                        )
                    }
                }
            }
        }

        /** The three controls share [modifier]'s area equally; icons scale with it. */
        @Composable
        private fun Buttons(state: MediaState.Active, modifier: GlanceModifier) {
            Row(
                modifier = modifier.cornerRadius(12.dp).background(DarkenOverlay),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ControlButton(R.drawable.ic_skip_previous, R.string.cd_previous, PlaybackAction.PREVIOUS)
                if (state.isPlaying) {
                    ControlButton(R.drawable.ic_pause, R.string.cd_pause, PlaybackAction.PLAY_PAUSE)
                } else {
                    ControlButton(R.drawable.ic_play, R.string.cd_play, PlaybackAction.PLAY_PAUSE)
                }
                ControlButton(R.drawable.ic_skip_next, R.string.cd_next, PlaybackAction.NEXT)
            }
        }

        @Composable
        private fun RowScope.ControlButton(icon: Int, description: Int, command: String) {
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .cornerRadius(12.dp)
                    .clickable(actionRunCallback<PlaybackAction>(actionParametersOf(PlaybackAction.COMMAND to command))),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(icon),
                    contentDescription = stringResource(description),
                    colorFilter = ColorFilter.tint(onPrimaryContainer),
                    contentScale = ContentScale.Fit,
                    modifier = GlanceModifier.fillMaxSize(),
                )
            }
        }

        private fun openPlayer(packageName: String?): Action =
            if (packageName == null) {
                actionStartActivity<LaunchPlayerActivity>()
            } else {
                actionStartActivity<LaunchPlayerActivity>(actionParametersOf(LaunchPlayerActivity.PACKAGE to packageName))
            }
    }
}

private val previewTrack: MediaState.Active
    @Composable get() = MediaState.Active(
        packageName = SPOTIFY_PACKAGE,
        title = stringResource(R.string.preview_title),
        artist = stringResource(R.string.preview_artist),
        cover = null,
        isPlaying = true,
    )

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 100)
@Composable
private fun WidePreview() = WidgetTheme { MediaWidget.WidgetContent(previewTrack) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 160, heightDp = 80)
@Composable
private fun NarrowPreview() = WidgetTheme { MediaWidget.WidgetContent(previewTrack.copy(isPlaying = false)) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 72)
@Composable
private fun CompactPreview() = WidgetTheme { MediaWidget.WidgetContent(previewTrack) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 48)
@Composable
private fun FlatPreview() = WidgetTheme { MediaWidget.WidgetContent(previewTrack) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 64)
@Composable
private fun NoAccessPreview() = WidgetTheme { MediaWidget.WidgetContent(MediaState.NoAccess) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 56)
@Composable
private fun NoSessionPreview() = WidgetTheme { MediaWidget.WidgetContent(MediaState.NoSession) }
