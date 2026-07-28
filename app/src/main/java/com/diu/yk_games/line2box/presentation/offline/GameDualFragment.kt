package com.diu.yk_games.line2box.presentation.offline

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.toRoute
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogLayoutAlertBinding
import com.diu.yk_games.line2box.databinding.FragmentGameDualBinding
import com.diu.yk_games.line2box.model.DataStore
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.util.*
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class GameDualFragment : Fragment() {
    private lateinit var binding: FragmentGameDualBinding
    private val viewModel by activityViewModels<MainViewModel>()
    private lateinit var scoreRedView: TextView
    private lateinit var scoreBlueView: TextView
    private lateinit var redTxt: TextView
    private lateinit var blueTxt: TextView

    private lateinit var parentActivity: FragmentActivity

    private var isFirstRun = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        _gameUtils = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGameDualBinding.inflate(inflater, container, false)
        parentActivity = requireActivity()
        _gameUtils?.updateContext(
            context = parentActivity,
            binding = binding
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (_gameUtils == null)
            _gameUtils = GameUtils(parentActivity, binding)

        setupUI()
        gameUtils.setupListener(onLineClick = ::performClick)
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        runCatching {
            val arg =
                findNavController().getBackStackEntry<Routes.GameDual>().toRoute<Routes.GameDual>()
            gameUtils.nm1 = arg.nm1
            gameUtils.nm2 = arg.nm2
            binding.nm1Id.text = "(${arg.nm1})"
            binding.nm2Id.text = "(${arg.nm2})"
        }
        scoreRedView = binding.scoreRed
        scoreBlueView = binding.scoreBlue
        redTxt = binding.red
        blueTxt = binding.blue
        lifecycleScope.launch {
            binding.volBtn.applyState(isMuted())
            isFirstRun = IO { pref.read("firstRun", true) }
        }
    }

    // Hide the status bar.
    //WindowCompat.setDecorFitsSystemWindows(getWindow(), false)
    //getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().SYSTEM_UI_FLAG_FULLSCREEN)
    //getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    //getActionBar().hide()
    @SuppressLint("SetTextI18n", "DiscouragedApi")
    fun performClick(view: View) {
        //Toast.makeText(parentActivity, "clicked", Toast.LENGTH_SHORT).show()
        val idNm = resources.getResourceEntryName(view.id)
        val aroundIds = gameUtils.getAroundIdNames(idNm)
        val bg = view.background.mutate() as GradientDrawable
        val color = gameUtils.getColorGrad(bg)

        if (color == gameUtils.whiteX) {
            gameUtils.playLineClickSound()
            gameUtils.clickCount++
            val isRedTurn = gameUtils.clickCount % 2 == 1
            bg.setColor(if (isRedTurn) gameUtils.redX else gameUtils.blueX)

            val extraTurn = gameUtils.handleBoxPair(
                aroundIds = aroundIds,
                isRedTurn = isRedTurn
            )

            if (extraTurn)
                gameUtils.clickCount--
            else
                gameUtils.changePlayerTurn(isRedTurn)

            if (gameUtils.totalScore == 36) {
                var winOffline = pref.read("winOffline", 0)
                pref.save("winOffline", ++winOffline)
                lifecycleScope.launch {
                    delay(800.milliseconds)
                    isNotMuted {
                        val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.win_ef)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                    }
                    redTxt.textSize = 30f
                    redTxt.setTextColor(ContextCompat.getColor(parentActivity, R.color.white))
                    blueTxt.textSize = 30f
                    blueTxt.setTextColor(ContextCompat.getColor(parentActivity, R.color.white))
                    if (gameUtils.scoreRed > gameUtils.scoreBlue)
                        onGameOver("Player RED won the match.", winOffline)
                    else if (gameUtils.scoreRed < gameUtils.scoreBlue)
                        onGameOver("Player BLUE won the match.", winOffline)
                    else
                        onGameOver("Match Draw.", winOffline)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun onGameOver(winMsg: String, winOffline: Int) {
        val builder = AlertDialog.Builder(parentActivity)

        val binding = DialogLayoutAlertBinding.inflate(LayoutInflater.from(parentActivity))
        val view = binding.root // The root view of the inflated layout

        builder.setView(view)

        if (saveToFirebase()) toast("Score Saved to Online Score Board")

        binding.textMessage.text = winMsg
        binding.buttonNo.text = "Exit"
        binding.buttonYes.text = "Retry!"

        val alertDialog = builder.create()

        binding.buttonYes.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
            if (winOffline > 5) {
                val manager = ReviewManagerFactory.create(parentActivity)
                val request = manager.requestReviewFlow()
                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val reviewInfo = task.result
                        val flow = manager.launchReviewFlow(parentActivity, reviewInfo!!)
                        flow.addOnCompleteListener {
                            recreateGame()
                        }
                    } else recreateGame()
                }
            } else recreateGame()
        }

        binding.buttonNo.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
            toast("Score Saved to Online Score Board")
            popBackSafe()
        }

        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        runCatching { alertDialog.show() }
    }

    fun recreateGame() {
        navigateSafe(Routes.GameDual(gameUtils.nm1, gameUtils.nm2)) {
            popUpTo(Routes.GameDual::class) {
                inclusive = true
            }
        }
    }

    fun saveToFirebase(): Boolean {
        val success = AtomicBoolean(false)
        val starData = "friendly"
        val redData = "${gameUtils.nm1}: ${gameUtils.scoreRed}"
        val blueData = "${gameUtils.nm2}: ${gameUtils.scoreBlue}"
        val ds = DataStore(
            time = System.currentTimeMillis(),
            redData = redData,
            blueData = blueData,
            starData = starData,
            plr1Id = "offline",
            plr2Id = "",
            plr1Cup = "",
            plr2Cup = ""
        )

        //single
        val db = Firebase.firestore
        //Source source = Source.CACHE;
        db.collection("LastBestPlayer").document("LastBestPlayer")
            .get().addOnSuccessListener {
                val map = it.data ?: return@addOnSuccessListener
                Log.d("TAG", "Cached document data: $map")
                val bestScore = map["info"]
                    .toString()
                    .substringAfterLast(": ")
                    .toIntOrNull() ?: 0
                val data = when {
                    bestScore <= gameUtils.scoreRed -> ds.redData
                    bestScore <= gameUtils.scoreBlue -> ds.blueData
                    else -> null
                }
                data?.let { info ->
                    db.collection("LastBestPlayer").document("LastBestPlayer")
                        .update("info", info)
                }
            }
        //multiple
        @OptIn(ExperimentalUuidApi::class)
        val key = Uuid.generateV7().toString()
        db.collection("ScoreBoard").document(key).set(ds)
            .addOnCompleteListener { success.set(true) }
        return success.get()
    }

    companion object {
        private var _gameUtils: GameUtils? = null
        private val gameUtils: GameUtils
            get() = _gameUtils!!
    }
}