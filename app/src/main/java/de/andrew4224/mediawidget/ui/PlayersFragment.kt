package de.andrew4224.mediawidget.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class PlayersFragment : Fragment() {

    private val viewModel: SettingsViewModel by activityViewModels { SettingsViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                PlayersScreen(
                    players = state.players,
                    onBack = { parentFragmentManager.popBackStack() },
                    onPlayerEnabledChange = viewModel::setPlayerEnabled,
                    onFavoriteChange = viewModel::setFavorite,
                    onForget = viewModel::forget,
                )
            }
        }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
