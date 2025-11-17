package com.diu.yk_games.line2box.presentation.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.diu.yk_games.line2box.databinding.ActivityStartBinding
import com.diu.yk_games.line2box.pref
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.util.InAppUpdate

class StartFragment : Fragment() {
    private lateinit var binding: ActivityStartBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var scrBrdVisible = false
    private val isFirstRun: Boolean by lazy { pref.getBoolean("firstRun", true) }
    private val inAppUpdate = InAppUpdate(requireActivity())

    companion object {
        private const val TAG = "TAG: StartFragment"
        private var showHadith = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivityStartBinding.inflate(inflater, container, false)
        return binding.root
    }

}
