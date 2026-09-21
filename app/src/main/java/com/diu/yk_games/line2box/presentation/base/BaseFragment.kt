package com.diu.yk_games.line2box.presentation.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.viewbinding.ViewBinding
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.google.android.material.transition.MaterialElevationScale

abstract class BaseFragment<VB : ViewBinding>(
    private val factory: (inflater: LayoutInflater, parent: ViewGroup?, attachToParent: Boolean) -> VB
) : Fragment() {
    protected lateinit var binding: VB
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

    open fun onCreateView(view: View) = Unit

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = factory(inflater, container, false)
        .apply { binding = this }
        .run(ViewBinding::getRoot)
        .apply(::onCreateView)
}