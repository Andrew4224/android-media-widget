package de.andrew4224.mediawidget.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.andrew4224.mediawidget.R
import de.andrew4224.mediawidget.media.MediaSessions.SPOTIFY_PACKAGE
import kotlinx.coroutines.launch

@Composable
fun PlayersScreen(
    players: List<PlayerOption>,
    onBack: () -> Unit,
    onPlayerEnabledChange: (String, Boolean) -> Unit,
    onFavoriteChange: (String, Boolean) -> Unit,
    onForget: (String) -> Unit,
) {
    AppTheme {
        Scaffold(containerColor = colorScheme.surfaceContainer) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.cd_back),
                                tint = colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_players_title),
                            style = typography.headlineMedium,
                            color = colorScheme.primary,
                        )
                    }
                }
                item {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (players.isEmpty()) {
                            Text(stringResource(R.string.settings_players_none))
                        } else {
                            Text(stringResource(R.string.settings_players_text))
                            Text(
                                stringResource(R.string.settings_players_favorite),
                                style = typography.bodySmall
                            )
                            Text(
                                stringResource(R.string.settings_players_switch),
                                style = typography.bodySmall
                            )
                            Text(
                                stringResource(R.string.settings_players_connected),
                                style = typography.bodySmall
                            )
                            Text(
                                stringResource(R.string.settings_players_forget),
                                style = typography.bodySmall
                            )
                        }
                    }
                }
                if (players.isNotEmpty() && players.all { it.isIgnored }) {
                    item { AllPlayersOffWarning() }
                }
                items(players, key = { it.packageName }) { player ->
                    ForgettablePlayer(player, onForget) {
                        PlayerRow(player, onPlayerEnabledChange, onFavoriteChange)
                    }
                }
            }
        }
    }
}

@Composable
internal fun AllPlayersOffWarning() {
    Text(stringResource(R.string.settings_players_all_off), color = colorScheme.error)
}

@Composable
private fun ForgettablePlayer(
    player: PlayerOption,
    onForget: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = {
            onForget(player.packageName)
            // A player with an active session is known again right away and stays in the list.
            scope.launch { state.reset() }
        },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CardDefaults.shape)
                    .background(colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    stringResource(R.string.settings_players_forget_action),
                    color = colorScheme.onErrorContainer
                )
            }
        },
    ) {
        content()
    }
}

@Composable
private fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { APP_ICON_SIZE.roundToPx() }
    return remember(packageName, sizePx) { AppIcons.get(context, packageName, sizePx) }
}

private val APP_ICON_SIZE = 36.dp

@Composable
private fun PlayerRow(
    player: PlayerOption,
    onEnabledChange: (String, Boolean) -> Unit,
    onFavoriteChange: (String, Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = !player.isIgnored,
                    role = Role.Switch,
                    onValueChange = { onEnabledChange(player.packageName, it) },
                )
                .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconToggleButton(
                checked = player.isFavorite,
                onCheckedChange = { onFavoriteChange(player.packageName, it) },
                enabled = !player.isIgnored,
            ) {
                Icon(
                    painter = painterResource(if (player.isFavorite) R.drawable.ic_star else R.drawable.ic_star_outline),
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = if (player.isIgnored) colorScheme.outlineVariant else colorScheme.primary,
                )
            }
            val icon = rememberAppIcon(player.packageName)
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.size(APP_ICON_SIZE)
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_music_note),
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(APP_ICON_SIZE)
                        .padding(6.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(player.label)
                Text(
                    player.packageName,
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = !player.isIgnored, onCheckedChange = null)
        }
    }
}

@Preview(name = "Players", showBackground = true)
@Composable
private fun PlayersScreenPreview() {
    PlayersScreen(
        players = listOf(
            PlayerOption(SPOTIFY_PACKAGE, "Spotify", isIgnored = false, isFavorite = true),
            PlayerOption("com.google.android.youtube", "YouTube", isIgnored = true),
        ),
        onBack = {},
        onPlayerEnabledChange = { _, _ -> },
        onFavoriteChange = { _, _ -> },
        onForget = {},
    )
}

@Preview(name = "All players off", showBackground = true)
@Composable
private fun PlayersScreenAllOffPreview() {
    PlayersScreen(
        players = listOf(PlayerOption(SPOTIFY_PACKAGE, "Spotify", isIgnored = true)),
        onBack = {},
        onPlayerEnabledChange = { _, _ -> },
        onFavoriteChange = { _, _ -> },
        onForget = {},
    )
}
