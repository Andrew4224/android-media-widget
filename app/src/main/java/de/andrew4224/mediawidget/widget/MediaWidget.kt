package de.andrew4224.mediawidget.widget

import android.content.Context
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import de.andrew4224.mediawidget.R
import de.andrew4224.mediawidget.media.MediaListenerService
import de.andrew4224.mediawidget.media.MediaRepository
import de.andrew4224.mediawidget.media.MediaState
import de.andrew4224.mediawidget.ui.MainActivity

class MediaWidget : GlanceAppWidget() {

    // NOTE: Exact instead of Responsive, because the cover is sized to the real widget height
    // and the buttons stretch over the remaining area.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Re-read on every (re)start so the widget is correct even after process death.
        MediaListenerService.ensureConnected(context)
        MediaRepository.refresh(context)
        provideContent {
            val state by MediaRepository.state.collectAsState()
            WidgetTheme {
                WidgetContent(state)
            }
        }
    }

    companion object {
        /** Below this width the cover is hidden. */
        private val MIN_WIDTH_FOR_INFO = 200.dp

        /** Below this width messages are shown as an icon. */
        private val MIN_WIDTH_FOR_MESSAGE_TEXT = 100.dp

        /** Narrow widgets below this height show only the three buttons. */
        private val MIN_HEIGHT_FOR_NARROW_INFO = 96.dp

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
                    .background(primaryContainer),
            ) {
                val progress = (state as? MediaState.Active)?.progress
                if (progress != null) {
                    Box(
                        GlanceModifier.width(LocalSize.current.width * progress).fillMaxHeight()
                            .background(ProgressOverlay)
                    ) {}
                }
                Box(
                    modifier = GlanceModifier.fillMaxSize().padding(PADDING),
                    contentAlignment = Alignment.Center
                ) {
                    Content(state)
                }
            }
        }

        @Composable
        private fun Content(state: MediaState) {
            when (state) {
                MediaState.NoAccess -> Message(
                    text = stringResource(R.string.widget_grant_access),
                    icon = R.drawable.ic_lock,
                    onClick = actionStartActivity<MainActivity>(),
                )

                MediaState.NoSession -> Message(
                    text = stringResource(R.string.widget_nothing_playing),
                    icon = R.drawable.ic_help,
                    onClick = openPlayer(packageName = null),
                )

                is MediaState.Active -> Player(state)
            }
        }

        @Composable
        private fun Message(text: String, icon: Int, onClick: Action) {
            Box(
                modifier = GlanceModifier.fillMaxSize().clickable(onClick),
                contentAlignment = Alignment.Center,
            ) {
                if (LocalSize.current.width < MIN_WIDTH_FOR_MESSAGE_TEXT) {
                    Image(
                        provider = ImageProvider(icon),
                        contentDescription = text,
                        colorFilter = ColorFilter.tint(onPrimaryContainer),
                        contentScale = ContentScale.Fit,
                        modifier = GlanceModifier.fillMaxSize().padding(12.dp),
                    )
                } else {
                    Text(
                        text = text,
                        maxLines = 2,
                        style = TextStyle(
                            color = onPrimaryContainer,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        ),
                    )
                }
            }
        }

        @Composable
        private fun Player(state: MediaState.Active) {
            val size = LocalSize.current
            val open = openPlayer(state.packageName)
            if (size.width < MIN_WIDTH_FOR_INFO) {
                if (size.height < MIN_HEIGHT_FOR_NARROW_INFO) {
                    Buttons(state, GlanceModifier.fillMaxSize())
                } else {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        TrackInfo(state, open, singleLine = false)
                        Spacer(GlanceModifier.height(4.dp))
                        Buttons(state, GlanceModifier.fillMaxWidth().defaultWeight())
                    }
                }
                return
            }
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
            Box(
                modifier = GlanceModifier.size(size).cornerRadius(12.dp).clickable(open),
                contentAlignment = Alignment.BottomEnd,
            ) {
                if (state.cover != null) {
                    Image(
                        provider = ImageProvider(state.cover),
                        contentDescription = description,
                        contentScale = ContentScale.Crop,
                        modifier = GlanceModifier.fillMaxSize(),
                    )
                } else {
                    Image(
                        provider = ImageProvider(R.drawable.ic_music_note),
                        contentDescription = description,
                        colorFilter = ColorFilter.tint(onPrimaryContainer),
                        modifier = GlanceModifier.fillMaxSize().background(DarkenOverlay)
                            .padding(size / 4),
                    )
                }
                if (state.isIgnoredPlayerConnected) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_open_in_new),
                        contentDescription = stringResource(R.string.cd_other_player_connected),
                        colorFilter = ColorFilter.tint(onPrimaryContainer),
                        modifier = GlanceModifier
                            .size(size / 3)
                            .cornerRadius(8.dp)
                            .background(primaryContainer)
                            .padding(size / 18),
                    )
                }
            }
        }

        @Composable
        private fun TrackInfo(state: MediaState.Active, open: Action, singleLine: Boolean) {
            val title = state.title ?: stringResource(R.string.widget_unknown_title)
            val artist = state.artist?.takeIf { it.isNotBlank() }
            val titleStyle = TextStyle(color = onPrimaryContainer, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight().clickable(open)) {
                    if (singleLine) {
                        Text(
                            text = if (artist == null) title else stringResource(
                                R.string.widget_title_artist,
                                title,
                                artist
                            ),
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
                if (state.canSwitchPlayer) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_swap_horiz),
                        contentDescription = stringResource(R.string.cd_switch_player),
                        colorFilter = ColorFilter.tint(onPrimaryContainer),
                        modifier = GlanceModifier
                            .size(28.dp)
                            .cornerRadius(14.dp)
                            .clickable(playback(PlaybackAction.SWITCH_PLAYER))
                            .padding(4.dp),
                    )
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
                ControlButton(
                    R.drawable.ic_skip_previous,
                    R.string.cd_previous,
                    playback(PlaybackAction.PREVIOUS)
                )
                when {
                    state.isIgnoredPlayerConnected -> ControlButton(
                        R.drawable.ic_open_in_new,
                        R.string.cd_open_and_play,
                        openPlayer(state.packageName, play = true),
                    )

                    state.isPlaying -> ControlButton(
                        R.drawable.ic_pause,
                        R.string.cd_pause,
                        playback(PlaybackAction.PLAY_PAUSE)
                    )

                    else -> ControlButton(
                        R.drawable.ic_play,
                        R.string.cd_play,
                        playback(PlaybackAction.PLAY_PAUSE)
                    )
                }
                ControlButton(
                    R.drawable.ic_skip_next,
                    R.string.cd_next,
                    playback(PlaybackAction.NEXT)
                )
            }
        }

        @Composable
        private fun RowScope.ControlButton(icon: Int, description: Int, onClick: Action) {
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .cornerRadius(12.dp)
                    .clickable(onClick),
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

        private fun playback(command: String): Action =
            actionRunCallback<PlaybackAction>(actionParametersOf(PlaybackAction.COMMAND to command))

        private fun openPlayer(packageName: String?, play: Boolean = false): Action =
            if (packageName == null) {
                actionStartActivity<LaunchPlayerActivity>()
            } else {
                actionStartActivity<LaunchPlayerActivity>(
                    actionParametersOf(
                        LaunchPlayerActivity.PACKAGE to packageName,
                        LaunchPlayerActivity.PLAY to play
                    ),
                )
            }
    }
}
