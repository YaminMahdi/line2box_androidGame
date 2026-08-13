package com.diu.yk_games.line2box.presentation.online

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.toRoute
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogLayoutGameOverBinding
import com.diu.yk_games.line2box.databinding.FragmentGameDualBinding
import com.diu.yk_games.line2box.model.DataStore
import com.diu.yk_games.line2box.model.MsgStore
import com.diu.yk_games.line2box.presentation.base.BaseFragment
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.util.*
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.Firebase
import com.google.firebase.database.*
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("DiscouragedApi")
class GameOnlineFragment : BaseFragment<FragmentGameDualBinding>(FragmentGameDualBinding::inflate) {
    private val drawerLayout: DrawerLayout by lazy {
        parentActivity.findViewById(R.id.drawer_layout)
    }

    private var redX = 0
    private var redY = 0
    private var blueX = 0
    private var blueY = 0
    lateinit var matchRef: DatabaseReference

    override fun onAttach(context: Context) {
        super.onAttach(context)
        _gameUtils = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (_gameUtils == null)
            _gameUtils = GameUtils(this, binding)
        else _gameUtils?.updateContext(
            context = parentActivity,
            binding = binding
        )
        setupUI()
        gameUtils.setupListener(onLineClick = ::performClick)
        viewModel.viewIdFromServer.collectWithLifecycle(minActiveState = Lifecycle.State.CREATED) { viewId ->
            cat("viewIdFromServer: $viewId")
            gameUtils.plyrTurn = true
            val viewId = resources
                .getIdentifier(viewId, "id", parentActivity.packageName)
            performClick(parentActivity.findViewById(viewId))
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        redX = ContextCompat.getColor(parentActivity, R.color.redX)
        redY = ContextCompat.getColor(parentActivity, R.color.redY)
        blueX = ContextCompat.getColor(parentActivity, R.color.blueX)
        blueY = ContextCompat.getColor(parentActivity, R.color.blueY)

        lifecycleScope.launch {
            delay(200.milliseconds)
            isFirstRun = IO { pref.read("firstRun", true) }
            if (isFirstRun) gameUtils.infoShow()
        }
        try {
            val arg = findNavController().getBackStackEntry<Routes.GameOnline>()
                .toRoute<Routes.GameOnline>()
            arg.log()
            viewModel.matchInfo = arg
            viewModel.matchKey = arg.gameKey
            gameUtils.plyrTurn = arg.isPlyr1
            gameUtils.nm1 = arg.nm1
            gameUtils.nm2 = arg.nm2

            binding.nm1Id.text = "(${arg.nm1})"
            binding.nm2Id.text = "(${arg.nm2})"
            matchRef = viewModel.multiPlayerRef.child(arg.gameKey).child("matchInfo")
//            viewModel.fetchServerLineClick(gameKey = arg.gameKey, isPlyr1 = arg.isPlyr1)
        } catch (e: Exception) {
            e.printStackTrace()
            toast("Couldn't find the game")
            popBackSafe()
        }
    }

    @SuppressLint("SetTextI18n")
    fun performClick(view: View) {
        Log.d(TAG, "After performClick (plyrTurn): ${gameUtils.plyrTurn}")
        //Toast.makeText(parentActivity, "clicked", Toast.LENGTH_SHORT).show()
        val idNm = resources.getResourceEntryName(view.id)
        val aroundIds = gameUtils.getAroundIdNames(idNm)
        val bg = view.background as GradientDrawable
        val color = gameUtils.getColorGrad(bg)
        if (color == gameUtils.whiteX && gameUtils.plyrTurn) {
            gameUtils.playLineClickSound()
            gameUtils.clickCount++
            val isRedTurn = gameUtils.clickCount % 2 == 1
            bg.setColor(if (isRedTurn) gameUtils.redX else gameUtils.blueX)

            if (viewModel.matchInfo.isPlyr1 && isRedTurn) {
                matchRef.child("plyr1").push()
                    .setValue(view.resources.getResourceEntryName(view.id))
            } else if (!viewModel.matchInfo.isPlyr1 && !isRedTurn) {
                matchRef.child("plyr2").push()
                    .setValue(view.resources.getResourceEntryName(view.id))
            }

            val extraTurn = gameUtils.handleBoxPair(
                aroundIds = aroundIds,
                isRedTurn = isRedTurn
            )

            if (extraTurn) {
                if (isRedTurn) {
                    if (!viewModel.matchInfo.isPlyr1) gameUtils.plyrTurn = false
                } else {
                    if (viewModel.matchInfo.isPlyr1) gameUtils.plyrTurn = false
                }
                gameUtils.clickCount--
            } else {
                gameUtils.changePlayerTurn(
                    isRedTurn = isRedTurn,
                    isPlyr1 = viewModel.matchInfo.isPlyr1
                )
            }
            if (gameUtils.totalScore == 36) {
                finishGame()
            }
        }
    }

    private fun finishGame() {
        val winCoin = Random.nextInt(80) + 45
        val lostCoin = Random.nextInt(35) + 15
        gameUtils.playWinSound()
        binding.red.textSize = 30f
        binding.red.setTextColor(resources.getColor(R.color.white, parentActivity.theme))
        binding.blue.textSize = 30f
        binding.blue.setTextColor(resources.getColor(R.color.white, parentActivity.theme))
        var winTxt = ""
        var wCoin = ""
        var plr1Cup = ""
        var plr2Cup = ""

        fun handleWin() {
            viewModel.doOnMatchEnd(winCoin)

            winTxt = "You won the match."
            wCoin = "+$winCoin"

            val ms = MsgStore(
                playerId = viewModel.matchInfo.currentPlayerId,
                nmData = viewModel.matchInfo.currentPlayerName,
                lvlData = viewModel.matchInfo.currentPlayerLevel.toString(),
                time = System.currentTimeMillis(),
                msgData = "Won the match.",
                type = MsgStore.MessageType.EnterText.name
            )

            viewModel.friendlyChatRef?.push()?.setValue(ms)
        }

        fun handleLoss() {
            viewModel.doOnMatchEnd(lostCoin, false)
            winTxt = "You lost the match."
            wCoin = "-$lostCoin"
        }

        fun handleDraw() {
            viewModel.doOnMatchEnd(50)
            winTxt = "Match Draw."
            wCoin = "+$winCoin"
        }

        when {
            gameUtils.scoreRed > gameUtils.scoreBlue -> {
                if (viewModel.matchInfo.isPlyr1) {
                    handleWin()
                    plr1Cup = "+$winCoin"
                } else {
                    handleLoss()
                    plr2Cup = "-$lostCoin"
                }
            }

            gameUtils.scoreRed < gameUtils.scoreBlue -> {
                if (!viewModel.matchInfo.isPlyr1) {
                    handleWin()
                    plr2Cup = "+$winCoin"
                } else {
                    handleLoss()
                    plr1Cup = "-$lostCoin"
                }
            }

            else -> handleDraw()
        }
        saveToFirebase(plr1Cup, plr2Cup)
        lifecycleScope.launch {
            delay(1200.milliseconds)
            onGameOver(
                winMsg = winTxt,
                winCoin = wCoin,
                matchWinMulti = viewModel.gameProfile.matchWinMulti
            )
        }
    }

    @SuppressLint("SetTextI18n")
    fun onGameOver(winMsg: String, winCoin: String, matchWinMulti: Int) {
        val coin = winCoin.toInt()
        val isWin = coin > -1

        val dialogBinding = DialogLayoutGameOverBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(parentActivity)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialogBinding.apply {
            textMessage.text = winMsg
            buttonNo.text = "Exit"
            buttonYes.text = "Chat"
        }

        dialogBinding.buttonYes.setBounceClickListener {
            gameUtils.playButtonClickSound()
            dismissDialog(alertDialog)
            openDrawerOrReview(matchWinMulti, openDrawer = true)
        }

        dialogBinding.buttonNo.setBounceClickListener {
            gameUtils.playButtonClickSound()
            dismissDialog(alertDialog)
            if (viewModel.matchInfo.isPlyr1 && viewModel.matchKey.isNotEmpty()) {
                viewModel.multiPlayerRef.child(viewModel.matchKey).removeValue()
            }
            openDrawerOrReview(matchWinMulti, openDrawer = false)
        }

        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        showDialogWithCoinAnimation(alertDialog, dialogBinding, coin, isWin)
    }

    private fun dismissDialog(dialog: AlertDialog) {
        runCatching { if (dialog.isShowing) dialog.dismiss() }
    }

    private fun openDrawerOrReview(matchWinMulti: Int, openDrawer: Boolean) {
        if (matchWinMulti > 2) {
            showReviewFlow(openDrawer)
        } else {
            if (openDrawer) drawerLayout.openDrawer(GravityCompat.START)
            else onBackPressed()
        }
    }

    private fun showReviewFlow(openDrawer: Boolean) {
        val manager = ReviewManagerFactory.create(parentActivity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result ?: return@addOnCompleteListener
                manager.launchReviewFlow(parentActivity, reviewInfo).addOnCompleteListener {
                    if (openDrawer) drawerLayout.openDrawer(GravityCompat.START)
                    else popBackSafe()
                }
            } else {
                if (openDrawer) drawerLayout.openDrawer(GravityCompat.START)
                else popBackSafe()
            }
        }
    }

    private fun showDialogWithCoinAnimation(
        alertDialog: AlertDialog,
        dialogBinding: DialogLayoutGameOverBinding,
        coin: Int,
        isWin: Boolean
    ) {
        alertDialog.show()
        val targetCoin = if (isWin) kotlin.math.abs(coin) else -kotlin.math.abs(coin)
        val prefix = if (isWin) "+" else ""
        ValueAnimator.ofInt(0, targetCoin).apply {
            duration = 1000L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val currentValue = animator.animatedValue as Int
                dialogBinding.coinWin.text = "$prefix$currentValue"
            }
            start()
        }
    }

    private fun saveToFirebase(plr1Cup: String, plr2Cup: String) {
        val firestore = Firebase.firestore
        if (viewModel.matchKey.isEmpty()) return
        val plr2CupRef = viewModel.multiPlayerRef
            .child(viewModel.matchKey)
            .child("plr2Cup") //hehe
        if (!viewModel.matchInfo.isPlyr1) {
            plr2CupRef.setValue(plr2Cup)
            return
        }
        val ds = DataStore(
            time = System.currentTimeMillis(),
            redData = "${
                viewModel.matchInfo.nm1.trim().split("\n", " ").firstOrNull()
            }: ${gameUtils.scoreRed}",
            blueData = "${
                viewModel.matchInfo.nm2.trim().split("\n", " ").firstOrNull()
            }: ${gameUtils.scoreBlue}",
            starData = "globe",
            plr1Id = viewModel.matchInfo.plr1Id,
            plr2Id = viewModel.matchInfo.plr2Id,
            plr1Cup = plr1Cup,
            plr2Cup = "0"
        )
        firestore.collection("LastBestPlayer").document("LastBestPlayer").get()
            .addOnSuccessListener { doc ->
                val map = doc.data ?: return@addOnSuccessListener
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
                data?.let {
                    firestore.collection("LastBestPlayer")
                        .document("LastBestPlayer")
                        .update("info", it)
                }
            }
        plr2CupRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ds.plr2Cup = snapshot.getValue<String>() ?: return
                firestore.collection("ScoreBoard").document(viewModel.uuidV7).set(ds)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    companion object {
        private const val TAG = "GameOnlineFragment"
        private var isFirstRun = false
        private var _gameUtils: GameUtils? = null
        private val gameUtils: GameUtils
            get() = _gameUtils!!
    }
}