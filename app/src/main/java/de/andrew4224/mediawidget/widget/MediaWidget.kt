package de.andrew4224.mediawidget.widget

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
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
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import de.andrew4224.mediawidget.R
import de.andrew4224.mediawidget.media.MediaRepository
import de.andrew4224.mediawidget.media.MediaState

class MediaWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(NARROW, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Re-read on every (re)start so the widget is correct even after process death.
        MediaRepository.refresh(context)
        provideContent {
            val state by MediaRepository.state.collectAsState()
            GlanceTheme {
                WidgetContent(state)
            }
        }
    }

    companion object {
        private val NARROW = DpSize(140.dp, 40.dp)
        private val WIDE = DpSize(240.dp, 40.dp)
        private val BUTTON_SIZE = 44.dp
        private val COVER_SIZE = 40.dp

        @Composable
        internal fun WidgetContent(state: MediaState) {
            val wide = LocalSize.current.width >= WIDE.width
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .appWidgetBackground()
                    .cornerRadius(16.dp)
                    .background(GlanceTheme.colors.widgetBackground)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (state) {
                    MediaState.NoAccess -> Message(
                        text = LocalContext.current.getString(R.string.widget_grant_access),
                        onClick = actionStartActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)),
                    )
                    MediaState.NoSession -> Message(
                        text = LocalContext.current.getString(R.string.widget_nothing_playing),
                        onClick = openPlayer(packageName = null),
                    )
                    is MediaState.Active -> Player(state, wide)
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
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp),
                )
            }
        }

        @Composable
        private fun RowScope.Player(state: MediaState.Active, wide: Boolean) {
            if (wide) {
                val open = openPlayer(state.packageName)
                Cover(state, open)
                TrackInfo(state, open, GlanceModifier.defaultWeight())
            } else {
                Spacer(GlanceModifier.defaultWeight())
            }
            ControlButton(R.drawable.ic_skip_previous, R.string.cd_previous, PlaybackAction.PREVIOUS)
            if (state.isPlaying) {
                ControlButton(R.drawable.ic_pause, R.string.cd_pause, PlaybackAction.PLAY_PAUSE)
            } else {
                ControlButton(R.drawable.ic_play, R.string.cd_play, PlaybackAction.PLAY_PAUSE)
            }
            ControlButton(R.drawable.ic_skip_next, R.string.cd_next, PlaybackAction.NEXT)
            if (!wide) Spacer(GlanceModifier.defaultWeight())
        }

        @Composable
        private fun Cover(state: MediaState.Active, open: Action) {
            val description = LocalContext.current.getString(R.string.cd_cover)
            val modifier = GlanceModifier.size(COVER_SIZE).cornerRadius(8.dp).clickable(open)
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
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                    modifier = modifier.background(GlanceTheme.colors.surfaceVariant).padding(8.dp),
                )
            }
        }

        @Composable
        private fun TrackInfo(state: MediaState.Active, open: Action, modifier: GlanceModifier) {
            Column(
                modifier = modifier.fillMaxHeight().padding(horizontal = 8.dp).clickable(open),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.title ?: LocalContext.current.getString(R.string.widget_unknown_title),
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                if (!state.artist.isNullOrBlank()) {
                    Text(
                        text = state.artist,
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                    )
                }
            }
        }

        @Composable
        private fun ControlButton(icon: Int, description: Int, command: String) {
            Box(
                modifier = GlanceModifier
                    .size(BUTTON_SIZE)
                    .cornerRadius(BUTTON_SIZE / 2)
                    .clickable(actionRunCallback<PlaybackAction>(actionParametersOf(PlaybackAction.COMMAND to command))),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(icon),
                    contentDescription = LocalContext.current.getString(description),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
                    modifier = GlanceModifier.size(28.dp),
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

private val previewTrack = MediaState.Active(
    packageName = "com.spotify.music",
    title = "A rather long song title that needs to be ellipsized",
    artist = "Some Artist",
    cover = null,
    isPlaying = true,
)

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 56)
@Composable
private fun WidePreview() = GlanceTheme { MediaWidget.WidgetContent(previewTrack) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 160, heightDp = 56)
@Composable
private fun NarrowPreview() = GlanceTheme { MediaWidget.WidgetContent(previewTrack.copy(isPlaying = false)) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 56)
@Composable
private fun NoAccessPreview() = GlanceTheme { MediaWidget.WidgetContent(MediaState.NoAccess) }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 56)
@Composable
private fun NoSessionPreview() = GlanceTheme { MediaWidget.WidgetContent(MediaState.NoSession) }
