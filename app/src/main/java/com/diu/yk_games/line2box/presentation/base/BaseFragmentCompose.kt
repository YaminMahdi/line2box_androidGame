package com.diu.yk_games.line2box.presentation.base

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.google.android.material.transition.MaterialElevationScale

abstract class BaseFragmentCompose : Fragment() {
    protected val viewModel by activityViewModels<MainViewModel>()
    protected lateinit var parentActivity: FragmentActivity
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parentActivity = requireActivity()
        exitTransition = MaterialElevationScale(false)
        returnTransition = MaterialElevationScale(false)
        enterTransition = MaterialElevationScale(true)
        reenterTransition = MaterialElevationScale(true)
    }
}