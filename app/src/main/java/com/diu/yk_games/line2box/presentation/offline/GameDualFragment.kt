package com.diu.yk_games.line2box.presentation.offline

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.toRoute
import com.diu.yk_games.line2box.databinding.DialogLayoutAlertBinding
import com.diu.yk_games.line2box.databinding.FragmentGameDualBinding
import com.diu.yk_games.line2box.model.DataStore
import com.diu.yk_games.line2box.presentation.base.BaseFragment
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.util.*
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.time.Duration.Companion.milliseconds

class GameDualFragment : BaseFragment<FragmentGameDualBinding>(FragmentGameDualBinding::inflate) {
    private lateinit var scoreRedView: TextView
    private lateinit var scoreBlueView: TextView

    private var isFirstRun = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        _gameUtils = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (_gameUtils == null)
            _gameUtils = GameUtils(fragment = this, binding = binding, isDual = true)
        else _gameUtils?.updateContext(
            context = parentActivity,
            binding = binding
        )
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
        }
        scoreRedView = binding.scoreRed
        scoreBlueView = binding.scoreBlue
        lifecycleScope.launch {
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
                gameUtils.changePlayerTurnUi(isRedTurn)

            if (gameUtils.totalScore == 36) {
                lifecycleScope.launch {
                    var winOffline = IO { pref.read("winOffline", 0) }
                    pref.save("winOffline", ++winOffline)
                    delay(800.milliseconds)
                    viewModel.player.playWinSound()
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
    suspend fun onGameOver(winMsg: String, winOffline: Int) {
        val builder = AlertDialog.Builder(parentActivity)

        val binding = DialogLayoutAlertBinding.inflate(LayoutInflater.from(parentActivity))
        val view = binding.root // The root view of the inflated layout

        builder.setView(view)

        if (saveToFirebase())
            toast("Score Saved to Online Score Board")

        binding.textMessage.text = winMsg
        binding.buttonNo.text = "Exit"
        binding.buttonYes.text = "Retry!"

        val alertDialog = builder.create()

        binding.buttonYes.setBounceClickListener {
            viewModel.player.playButtonClickSound()
            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
            if (winOffline > 2 && viewModel.isConnected) {
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
            viewModel.player.playButtonClickSound()
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

    suspend fun saveToFirebase(): Boolean {
        if (!viewModel.isConnected) return false

        val redScore = gameUtils.scoreRed
        val blueScore = gameUtils.scoreBlue
        val redData = "${gameUtils.nm1}: $redScore"
        val blueData = "${gameUtils.nm2}: $blueScore"

        val ds = DataStore(
            time = System.currentTimeMillis(),
            redData = redData,
            blueData = blueData,
            starData = "friendly",
            plr1Id = "",
            plr2Id = "",
            plr1Cup = "",
            plr2Cup = ""
        )

        return IO {
            runCatching {
                val db = viewModel.firestore

                // 1. Save user's score to ScoreBoard
                db.collection("ScoreBoard")
                    .document(viewModel.uuidV7)
                    .set(ds)
                    .await()

                // 2. Check and update LastBestPlayer atomically via Transaction
                val highestLocalData = when {
                    redScore >= blueScore -> redData
                    else -> blueData
                }
                val maxScore = maxOf(redScore, blueScore)

                val docRef = db.collection("LastBestPlayer").document("LastBestPlayer")

                db.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    val currentInfo = snapshot.getString("info") ?: ""
                    val currentBestScore =
                        currentInfo.substringAfterLast(": ", "0").toIntOrNull() ?: 0

                    if (maxScore > currentBestScore) {
                        transaction.update(docRef, "info", highestLocalData)
                    }
                }.await()

                true
            }.getOrDefault(false)
        }
    }

    companion object {
        private var _gameUtils: GameUtils? = null
        private val gameUtils: GameUtils
            get() = _gameUtils!!
    }
}