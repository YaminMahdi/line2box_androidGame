package com.diu.yk_games.line2box.presentation.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.media.MediaPlayer
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
import com.diu.yk_games.line2box.databinding.DialogLayoutInfoBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutUpdateuiBinding
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.util.applyState
import com.diu.yk_games.line2box.util.gone
import com.diu.yk_games.line2box.util.invisible
import com.diu.yk_games.line2box.util.isMuted
import com.diu.yk_games.line2box.util.isNotMuted
import com.diu.yk_games.line2box.util.loadDrawable
import com.diu.yk_games.line2box.util.navigateSafe
import com.diu.yk_games.line2box.util.performOnClickF
import com.diu.yk_games.line2box.util.pref
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.diu.yk_games.line2box.util.show
import com.diu.yk_games.line2box.util.toast
import kotlinx.coroutines.launch

class StartFragment : Fragment() {
    private lateinit var binding: ActivityStartBinding
    private val viewModel: MainViewModel by activityViewModels()
    private val isFirstRun by lazy { pref.read("firstRun", true) }

    lateinit var parentActivity: FragmentActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivityStartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        when (viewModel.motionProgress){
            -1 -> binding.motionLayout.jumpToState(R.id.previous)
            1 -> binding.motionLayout.jumpToState(R.id.next)
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
            if(BuildConfig.DEBUG){
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
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
        }
        navigateSafe(Routes.ScoreBoard)
    }

    fun ideaBtn() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
        }
        infoShow()
    }

    private fun infoShow() {
        var i = 0
        val gifs = intArrayOf(
            R.drawable.g0,
            R.drawable.g1,
            R.drawable.g2,
            R.drawable.g3,
            R.drawable.g4
        )
        val msg = arrayOf(
            "If the color of RED is popped, it's the TURN of the first player.",
            "Click on a LINE to connect two DOT.",
            "The player who makes a BOX gets a point.",
            "Take a bonus TURN after making a BOX.",
            "Click on this button anytime to see the rules again."
        )
        val builder = AlertDialog.Builder(parentActivity)
        val dialogBinding = DialogLayoutInfoBinding.inflate(LayoutInflater.from(parentActivity))
        builder.setView(dialogBinding.root)
        builder.setCancelable(false)

        dialogBinding.textMessage.text = msg[0]
        dialogBinding.playGif.loadDrawable(gifs[0])
        dialogBinding.buttonPre.invisible()
        val alertDialog = builder.create()
        dialogBinding.buttonPre.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                mediaPlayer?.start()
                mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
            }
            if (i != 0) i--
            if (i == 0) dialogBinding.buttonPre.invisible()
            dialogBinding.textMessage.text = msg[i]
            dialogBinding.playGif.loadDrawable(gifs[i])
        }
        dialogBinding.buttonNext.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                mediaPlayer?.start()
                mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
            }
            if (i <= 4) i++
            if (!isFirstRun && i == 4) i++
            if (i == 1) dialogBinding.buttonPre.show()
            if (i >= 5) runCatching { if (alertDialog.isShowing) alertDialog.dismiss() } else {
                dialogBinding.textMessage.text = msg[i]
                dialogBinding.playGif.loadDrawable(gifs[i])
            }
        }
        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        runCatching { alertDialog.show() }
    }

    @SuppressLint("SetTextI18n")
    fun startBtn() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
        }
        if (binding.mode1.alpha < .5) {
            if (viewModel.onlineStatus == "pass") {
                navigateSafe(Routes.MultiPlayer)
                viewModel.motionProgress = 1
            } else if (viewModel.onlineStatus == "needReload") {
                //updateUI()
                val builder = AlertDialog.Builder(parentActivity)
                val dialogBinding = DialogLayoutUpdateuiBinding.inflate(LayoutInflater.from(parentActivity))

                builder.setView(dialogBinding.root)
                builder.setCancelable(false)
                dialogBinding.googlePlayWarning.gone()
                dialogBinding.UpdateInfo.text = "You must have INTERNET connection to play in ONLINE mode"
                val alertDialog = builder.create()
                dialogBinding.buttonUpdate.setBounceClickListener {
                    isNotMuted {
                        val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                        mediaPlayer?.start()
                        mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
                    }
                    recreate(requireActivity())
                    runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
                }
                alertDialog.window?.setBackgroundDrawable(0.toDrawable())
                runCatching { alertDialog.show() }
            }
        } else if (binding.mode3.alpha < .5){
            navigateSafe(Routes.ChangeName)
            viewModel.motionProgress = -1
        } else{
            navigateSafe(Routes.GameBot)
            viewModel.motionProgress = 0
        }
    }

    companion object {
        private const val TAG = "StartFragment"
    }

}
