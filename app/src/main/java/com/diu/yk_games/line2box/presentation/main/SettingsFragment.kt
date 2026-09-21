package com.diu.yk_games.line2box.presentation.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.ui.theme.Line2BoxTheme
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SettingsFragment : BottomSheetDialogFragment() {
    private val viewModel by activityViewModels<MainViewModel>()

    override fun getTheme()= R.style.TransparentBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): ComposeView = content {
        Line2BoxTheme {
            val settings by viewModel.settingsState.collectAsStateWithLifecycle()

            SettingsScreen(
                settings = settings,
                onSettingsChange = viewModel::updateSettings
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            isHideable = true
            skipCollapsed = true
        }
    }

    companion object {
        private const val TAG = "SettingsFragment"
        fun isShowing(fragmentManager: FragmentManager): Boolean {
            val fragment =
                fragmentManager.findFragmentByTag(TAG) as? SettingsFragment
            return fragment != null && fragment.isAdded
        }

        fun show(fragmentManager: FragmentManager) {
            if (!isShowing(fragmentManager))
                SettingsFragment().show(fragmentManager, TAG)
        }
    }
}
