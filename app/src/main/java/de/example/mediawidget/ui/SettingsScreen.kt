package de.example.mediawidget.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.example.mediawidget.R

@Composable
fun SettingsScreen(state: SettingsUiState, onOpenNotificationSettings: () -> Unit) {
    AppTheme {
        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)

                Section(stringResource(R.string.settings_access_title)) {
                    Text(
                        stringResource(
                            if (state.hasNotificationAccess) R.string.settings_access_granted else R.string.settings_access_missing,
                        ),
                    )
                    Text(stringResource(R.string.settings_access_reason), style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onOpenNotificationSettings) {
                        Text(stringResource(R.string.settings_open_access))
                    }
                }

                Section(stringResource(R.string.settings_session_title)) {
                    if (state.sessionPackage == null) {
                        Text(stringResource(R.string.settings_session_none))
                    } else {
                        Text(stringResource(R.string.settings_session_package, state.sessionPackage))
                        Text(
                            stringResource(
                                R.string.settings_session_track,
                                state.sessionTitle ?: stringResource(R.string.widget_unknown_title),
                            ),
                        )
                    }
                }

                Section(stringResource(R.string.settings_howto_title)) {
                    Text(stringResource(R.string.settings_howto_text))
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}
