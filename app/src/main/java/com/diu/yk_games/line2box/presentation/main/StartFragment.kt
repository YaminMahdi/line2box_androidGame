package com.diu.yk_games.line2box.presentation.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat.recreate
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.BuildConfig
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.ActivityStartBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutInfoBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutUpdateuiBinding
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.presentation.bot.GameActivity3
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.presentation.offline.GameActivity1
import com.diu.yk_games.line2box.presentation.online.MultiplayerActivity
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
    private val isFirstRun: Boolean by lazy { pref.read("firstRun", true) }

    lateinit var safeContext: Context

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivityStartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListener()
        setupObserver()
    }
    private fun setupUI() {
        safeContext = requireActivity()
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
                toast("MultiPlayer Database Cleared")
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
            val mediaPlayer = MediaPlayer.create(safeContext, R.raw.btn_click_ef)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
        }
        navigateSafe(Routes.ScoreBoard)
//        scrBrdVisible = true
//        val fm = supportFragmentManager
//        val ft = fm.beginTransaction()
//        ft.replace(R.id.disFragment, DisplayFragment())
//        ft.commit()
//        binding.linearLayoutStart1.gone()
//        binding.linearLayoutStart2.gone()
//        binding.motionLayout.gone()
//        binding.globalScoreFrag.show()
    }

    fun ideaBtn() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(safeContext, R.raw.btn_click_ef)
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
        val builder = AlertDialog.Builder(safeContext)
        val dialogBinding = DialogLayoutInfoBinding.inflate(LayoutInflater.from(safeContext))
        builder.setView(dialogBinding.root)
        builder.setCancelable(false)

        dialogBinding.textMessage.text = msg[0]
        dialogBinding.playGif.loadDrawable(gifs[0])
        dialogBinding.buttonPre.invisible()
        val alertDialog = builder.create()
        dialogBinding.buttonPre.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(safeContext, R.raw.btn_click_ef)
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
                val mediaPlayer = MediaPlayer.create(safeContext, R.raw.btn_click_ef)
                mediaPlayer?.start()
                mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
            }
            if (i <= 4) i++
            if (!isFirstRun && i == 4) i++
            if (i == 1) dialogBinding.buttonPre.show()
            if (i >= 5) alertDialog.dismiss() else {
                dialogBinding.textMessage.text = msg[i]
                dialogBinding.playGif.loadDrawable(gifs[i])
            }
        }
        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        try { alertDialog.show() }
        catch (e: Exception) { e.printStackTrace() }
    }

    @SuppressLint("SetTextI18n")
    fun startBtn() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(safeContext, R.raw.btn_click_ef)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
        }
        if (binding.mode1.alpha < .5) {
            if (viewModel.onlineStatus == "pass") {
                startActivity(Intent(safeContext, MultiplayerActivity::class.java).putExtra("playerId", viewModel.playerId))
                //finish()
            } else if (viewModel.onlineStatus == "needReload") {
                //updateUI()
                val builder = AlertDialog.Builder(safeContext)
                val dialogBinding = DialogLayoutUpdateuiBinding.inflate(LayoutInflater.from(safeContext))

                builder.setView(dialogBinding.root)
                builder.setCancelable(false)
                dialogBinding.googlePlayWarning.gone()
                dialogBinding.UpdateInfo.text = "You must have INTERNET connection to play in ONLINE mode"
                val alertDialog = builder.create()
                dialogBinding.buttonUpdate.setBounceClickListener {
                    isNotMuted {
                        val mediaPlayer = MediaPlayer.create(safeContext, R.raw.btn_click_ef)
                        mediaPlayer?.start()
                        mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
                    }
                    recreate(requireActivity())
                    alertDialog.dismiss()
                }
                alertDialog.window?.setBackgroundDrawable(0.toDrawable())
                try { alertDialog.show() }
                catch (ex: Exception) { ex.printStackTrace() }
            }
        } else if (binding.mode3.alpha < .5){
            startActivity(Intent(safeContext, GameActivity1::class.java))
            //finish()
        } else{
            startActivity(Intent(safeContext, GameActivity3::class.java))
            //finish()
        }
    }

    companion object {
        private const val TAG = "StartFragment"
    }

}
