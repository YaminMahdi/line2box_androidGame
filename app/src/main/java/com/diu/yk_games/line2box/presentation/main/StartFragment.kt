package com.diu.yk_games.line2box.presentation.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import com.diu.yk_games.line2box.BuildConfig
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogLayoutUpdateuiBinding
import com.diu.yk_games.line2box.databinding.FragmentStartBinding
import com.diu.yk_games.line2box.model.ErrorType
import com.diu.yk_games.line2box.model.OnlineStatus
import com.diu.yk_games.line2box.presentation.base.BaseFragment
import com.diu.yk_games.line2box.presentation.component.DynamicIslandController
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.util.*

class StartFragment : BaseFragment<FragmentStartBinding>(FragmentStartBinding::inflate) {
    private lateinit var gameUtils: GameUtils

    var failedAttempt = 0

    override fun onPause() {
        super.onPause()
        viewModel.lastMotionState = binding.motionLayout.currentState
    }

    override fun onCreateView(view: View) {
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
        gameUtils = GameUtils(this)
    }

    private fun setupListener() {
        binding.startBtnId.setBounceClickListener {
            startBtn()
        }
        binding.settingBtn.setBounceClickListener {
            gameUtils.playButtonClickSound()
            SettingsFragment.show(childFragmentManager)
        }
        binding.ideaBtn.setBounceClickListener {
            ideaBtn()
        }
        binding.scrBrdBtn.setBounceClickListener {
            if (DynamicIslandController.isLoading)
                toast("Calm down, we're still loading!")
            else if (viewModel.isConnected)
                navigateToScoreBoard()
            else if (failedAttempt == 0)
                viewModel.initializePlayGameUser(
                    activity = parentActivity,
                    onSuccess = ::navigateToScoreBoard
                )
            else
                toast((viewModel.onlineStatus.error ?: ErrorType.NoInternet).description)
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
            R.id.next -> if (DynamicIslandController.isLoading)
                toast("Calm down, we're still loading!")
            else if (viewModel.isConnected)
                navigateSafe(Routes.MultiPlayer)
            else if (failedAttempt == 0)
                viewModel.initializePlayGameUser(
                    activity = parentActivity,
                    onSuccess = { navigateSafe(Routes.MultiPlayer) }
                )
            else
                showPlayServiceRequirementDialog()

            R.id.previous -> navigateSafe(Routes.ChangeName)
            R.id.start -> navigateSafe(Routes.GameBot)
        }
    }

    @SuppressLint("SetTextI18n")
    fun showPlayServiceRequirementDialog() {
        val builder = AlertDialog.Builder(parentActivity)
        val dialogBinding = DialogLayoutUpdateuiBinding
            .inflate(LayoutInflater.from(parentActivity))
        builder.setView(dialogBinding.root)
        builder.setCancelable(false)
        val alertDialog = builder.create()

        when {
            !ConnectivityObserver.isConnected -> {
                ConnectivityObserver.initialize(parentActivity)
                dialogBinding.warningMessage.text = ErrorType.NoInternet.description
                dialogBinding.updateInfo.text =
                    "Online mode requires an internet connection."
                dialogBinding.buttonUpdate.text = "Dismiss"
                dialogBinding.googlePlayWarning.gone()
            }

            viewModel.onlineStatus == OnlineStatus.Offline -> {
                dialogBinding.warningMessage.text =
                    (viewModel.onlineStatus.error ?: ErrorType.PlayServiceNeeded).description
                dialogBinding.updateInfo.text =
                    "Online mode requires Google Play Games Services to play!\n"
                        .plus("You may need to UPDATE an app.\n(Link Below)")
                dialogBinding.googlePlayWarning.show()
            }

            else -> {
                toast("Something went wrong!")
                return
            }
        }

        dialogBinding.buttonUpdate.setBounceClickListener {
            viewModel.player.playButtonClickSound()
            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
            if (viewModel.onlineStatus == OnlineStatus.Offline && ConnectivityObserver.isConnected)
                viewModel.initializePlayGameUser(parentActivity)
        }

        fun TextView.bindMarketLink(packageName: String) = setBounceClickListener {
            val url = Constants.getPlayStoreUrl(packageName)
            setTextColor(parentActivity.getColor(R.color.teal_700))
            viewModel.player.playButtonClickSound()
            parentActivity.launchPlayStoreOverlayBypass(url)
                ?: parentActivity.showOnMarket(packageName)
                ?: parentActivity.showCustomTab(url)
        }

        dialogBinding.playStoreLink.bindMarketLink(Constants.PLAY_STORE)
        dialogBinding.playSvLink.bindMarketLink(Constants.PLAY_SERVICES)
        dialogBinding.playGmLink.bindMarketLink(Constants.PLAY_GAMES)

        dialogBinding.restartLink.setBounceClickListener {
            dialogBinding.restartLink.setTextColor(parentActivity.getColor(R.color.teal_700))
            viewModel.player.playButtonClickSound()
            parentActivity.showCustomTab(Constants.RESTART_YOUTUBE_URL)
        }

        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        runCatching { alertDialog.show() }
    }
}
