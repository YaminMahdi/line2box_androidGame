package com.diu.yk_games.line2box.presentation.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat.recreate
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.BuildConfig
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.ActivityStartBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutUpdateuiBinding
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.util.*
import kotlinx.coroutines.launch

class StartFragment : Fragment() {
    private lateinit var binding: ActivityStartBinding
    private val viewModel by activityViewModels<MainViewModel>()
    private lateinit var gameUtils: GameUtils
    lateinit var parentActivity: FragmentActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivityStartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        viewModel.lastMotionState = binding.motionLayout.currentState
    }

    override fun onResume() {
        super.onResume()
        if (binding.motionLayout.currentState == viewModel.lastMotionState)
            return
        viewModel.lastMotionState?.let {
            binding.motionLayout.jumpToState(it)
            viewModel.lastMotionState = null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListener()
        setupObserver()
    }

    private fun setupUI() {
        parentActivity = requireActivity()
        gameUtils = GameUtils(parentActivity)
        ifMuted()
    }

    private fun setupListener() {
        binding.startBtnId.setBounceClickListener {
            startBtn()
        }
        binding.volBtn.performOnClickF()
        binding.ideaBtn.setBounceClickListener {
            ideaBtn()
        }
        binding.scrBrdBtn.setBounceClickListener {
            navigateToScoreBoard()
        }
        binding.logo.setBounceClickListener {
            if (BuildConfig.DEBUG) {
                viewModel.clearMultiPlayerDB()
                toast("MultiPlayer database cleared")
            }
        }
    }

    private fun setupObserver() {

    }

    private fun ifMuted() {
        lifecycleScope.launch {
            binding.volBtn.applyState(isMuted())
        }
    }

    private fun navigateToScoreBoard() {
        gameUtils.playButtonClickSound()
        navigateSafe(Routes.ScoreBoard)
    }

    fun ideaBtn() {
        gameUtils.playButtonClickSound()
        gameUtils.infoShow()
    }

    @SuppressLint("SetTextI18n")
    fun startBtn() {
        gameUtils.playButtonClickSound()

        when (binding.motionLayout.currentState) {
            R.id.next -> {
                if (viewModel.onlineStatus == "pass") {
                    navigateSafe(Routes.MultiPlayer)
                } else if (viewModel.onlineStatus == "needReload") {
                    //updateUI()
                    val builder = AlertDialog.Builder(parentActivity)
                    val dialogBinding =
                        DialogLayoutUpdateuiBinding.inflate(layoutInflater)

                    builder.setView(dialogBinding.root)
                    builder.setCancelable(false)
                    dialogBinding.googlePlayWarning.gone()
                    dialogBinding.updateInfo.text =
                        "You must have INTERNET connection to play in ONLINE mode"
                    val alertDialog = builder.create()
                    dialogBinding.buttonUpdate.setBounceClickListener {
                        gameUtils.playButtonClickSound()
                        recreate(requireActivity())
                        runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
                    }
                    alertDialog.window?.setBackgroundDrawable(0.toDrawable())
                    runCatching { alertDialog.show() }
                }
            }

            R.id.previous -> navigateSafe(Routes.ChangeName)
            R.id.start -> navigateSafe(Routes.GameBot)
        }
    }
}
