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
import com.diu.yk_games.line2box.model.GameRoom
import com.diu.yk_games.line2box.model.MsgStore
import com.diu.yk_games.line2box.model.PlayerColor
import com.diu.yk_games.line2box.presentation.base.BaseFragment
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.util.*
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SuppressLint("DiscouragedApi")
class GameOnlineFragment : BaseFragment<FragmentGameDualBinding>(FragmentGameDualBinding::inflate) {
    private val drawerLayout: DrawerLayout by lazy {
        parentActivity.findViewById(R.id.drawer_layout)
    }

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
        val route = setupUI()
        gameUtils.setupListener(onLineClick = {
            if (!route.watchOnly) performClick(it)
        })
        binding.root.post {
            setupObserver()
        }
    }

    private fun setupObserver() {
        fun onClick(line: GameRoom.Line) {
            cat("viewIdFromServer: $line")
            val viewId = resources
                .getIdentifier(line.id, "id", parentActivity.packageName)
            performClick(
                view = parentActivity.findViewById(viewId),
                color = line.color,
                serverTurn = true
            )
        }

        var lastServerEvent: Set<GameRoom.Line> = setOf()

        viewModel.lineIdsFromServer.collectWithLifecycle(minActiveState = Lifecycle.State.CREATED) { newServerEvent ->
            if (newServerEvent.isEmpty()) {
                delay(2.seconds)
                if (viewModel.lineIdsFromServer.value.isEmpty()) {
                    viewModel.fetchServerLineClick()
                    viewModel.fetchFriendlyChat()
                }
                return@collectWithLifecycle
            }

            val delta = newServerEvent.size - lastServerEvent.size

            if (delta > 0 && lastServerEvent.isNotEmpty()) {
                var found = 0
                for (entry in newServerEvent) {
                    if (entry !in lastServerEvent) {
                        onClick(entry)
                        if (++found == delta) break
                    }
                }
                if (found != delta) newServerEvent.forEach(::onClick) // mismatch fallback
            } else {
                newServerEvent.forEach(::onClick)
            }
            newServerEvent.lastOrNull()?.let { last ->
                val oldTurn = gameUtils.plyrTurn
                val isMe = viewModel.matchRouteInfo.isPlyr1 == last.color.isRed
                gameUtils.plyrTurn = isMe == gameUtils.lastHadExtraTurn
                if (oldTurn != gameUtils.plyrTurn)
                    gameUtils.changePlayerTurnUi(!last.color.isRed)
            }

            lastServerEvent = newServerEvent
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI(): Routes.GameOnline {
        val arg = findNavController().getBackStackEntry<Routes.GameOnline>()
            .toRoute<Routes.GameOnline>()
        arg.log()
        lifecycleScope.launch {
            isFirstRun = IO { pref.read("firstRun", true) }
            if (isFirstRun) gameUtils.infoShow()
            try {
                viewModel.matchRouteInfo = arg
                gameUtils.plyrTurn = arg.isPlyr1
                gameUtils.nm1 = arg.nm1
                gameUtils.nm2 = arg.nm2

                binding.nm1Id.text = "(${arg.nm1})"
                binding.nm2Id.text = "(${arg.nm2})"
                while (isActive && isAdded) {
                    delay(2.minutes)
                    if (isAdded) viewModel.pingCurrentMatch()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                toast("Couldn't find the game")
                popBackSafe()
            }
        }
        return arg
    }

    @SuppressLint("SetTextI18n")
    fun performClick(
        view: View,
        color: PlayerColor = viewModel.matchRouteInfo.currentPlayerColor,
        serverTurn: Boolean = false
    ) {
        Log.d(TAG, "After performClick (plyrTurn): ${gameUtils.plyrTurn}")
        //Toast.makeText(parentActivity, "clicked", Toast.LENGTH_SHORT).show()
        val idNm = resources.getResourceEntryName(view.id) ?: return
        val aroundIds = gameUtils.getAroundIdNames(idNm)
        val bg = view.background as GradientDrawable
        val lineColor = gameUtils.getColorGrad(bg)
        if (lineColor == gameUtils.whiteX && (gameUtils.plyrTurn || serverTurn)) {
            gameUtils.playLineClickSound()
//            gameUtils.clickCount++
//            val isRedTurn = color == LineColor.Red
//            val isRedTurn = gameUtils.clickCount % 2 == 1
            bg.setColor(if (color.isRed) gameUtils.redX else gameUtils.blueX)

            viewModel.sendClick2Server(GameRoom.Line(idNm, color))

            val extraTurn = gameUtils.handleBoxPair(
                aroundIds = aroundIds,
                isRedTurn = color.isRed
            )

            val isMe = viewModel.matchRouteInfo.isPlyr1 == color.isRed

            val oldTurn = gameUtils.plyrTurn
            gameUtils.plyrTurn = isMe == extraTurn
            Log.d(
                TAG,
                "performClick: plyrTurn: ${gameUtils.plyrTurn}, extraTurn: $extraTurn, isMe: $isMe"
            )

            if (oldTurn != gameUtils.plyrTurn) {
                gameUtils.changePlayerTurnUi(
                    isRedTurn = color.isRed
                )
            }
            if (gameUtils.totalScore == 36) {
                finishGame()
            }
        } else {
            Log.d(TAG, "performClick: not your turn")
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
                playerId = viewModel.matchRouteInfo.currentPlayerId,
                nmData = viewModel.matchRouteInfo.currentPlayerName,
                lvlData = viewModel.matchRouteInfo.currentPlayerLevel.toString(),
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
                if (viewModel.matchRouteInfo.isPlyr1) {
                    handleWin()
                    plr1Cup = "+$winCoin"
                } else {
                    handleLoss()
                    plr2Cup = "-$lostCoin"
                }
            }

            gameUtils.scoreRed < gameUtils.scoreBlue -> {
                if (!viewModel.matchRouteInfo.isPlyr1) {
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
            if (viewModel.matchRouteInfo.isPlyr1 && viewModel.matchRouteInfo.gameKey.isNotEmpty()) {
                viewModel.multiPlayerRef
                    .child(viewModel.matchRouteInfo.gameKey)
                    .updateChildren(
                        mapOf(
                            "player1/seenAt" to -2L,
                            "player2/seenAt" to -2L
                        )
                    )
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
        if (matchWinMulti > 1) {
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
        val firestore = viewModel.firestore
        if (viewModel.matchRouteInfo.gameKey.isEmpty()) return
        val plr2CupRef = viewModel.multiPlayerRef
            .child(viewModel.matchRouteInfo.gameKey)
            .child("plr2Cup") //hehe
        if (!viewModel.matchRouteInfo.isPlyr1) {
            plr2CupRef.setValue(plr2Cup)
            return
        }
        val ds = DataStore(
            time = System.currentTimeMillis(),
            redData = "${
                viewModel.matchRouteInfo.nm1.trim().split("\n", " ").firstOrNull()
            }: ${gameUtils.scoreRed}",
            blueData = "${
                viewModel.matchRouteInfo.nm2.trim().split("\n", " ").firstOrNull()
            }: ${gameUtils.scoreBlue}",
            starData = "globe",
            plr1Id = viewModel.matchRouteInfo.plr1Id,
            plr2Id = viewModel.matchRouteInfo.plr2Id,
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
                ds.plr2Cup = snapshot.getValueOrNull<String>() ?: return
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