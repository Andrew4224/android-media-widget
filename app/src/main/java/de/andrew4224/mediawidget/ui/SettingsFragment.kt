package de.andrew4224.mediawidget.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                SettingsScreen(
                    state = state,
                    onOpenNotificationSettings = {
                        startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onProgressIntervalChange = viewModel::setProgressInterval,
                    onOpenAppInfo = {
                        startActivity(
                            Intent(
                                ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", requireContext().packageName, null)
                            ),
                        )
                    },
                )
            }
        }

    override fun onResume() {
        super.onResume()
        // The user may just have come back from the system settings.
        viewModel.refresh()
    }
}
