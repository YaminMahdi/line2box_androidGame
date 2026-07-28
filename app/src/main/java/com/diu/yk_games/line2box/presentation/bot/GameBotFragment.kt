package com.diu.yk_games.line2box.presentation.bot

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.diu.yk_games.line2box.databinding.DialogLayoutAlertBinding
import com.diu.yk_games.line2box.databinding.FragmentGameDualBinding
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.util.*
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

class GameBotFragment : Fragment() {
    private lateinit var binding: FragmentGameDualBinding
    private val viewModel by activityViewModels<MainViewModel>()
    lateinit var parentActivity: FragmentActivity

    private val lineIDs by lazy { GameUtils.lineIDs.toMutableList() }

    private val scope =
        CoroutineScope(Dispatchers.Main.limitedParallelism(1) + SupervisorJob())

    private var isFirstRun = false
    private var recursion = false

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

    @SuppressLint("DiscouragedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (_gameUtils == null)
            _gameUtils = GameUtils(context = parentActivity, binding = binding, isBot = true)
        setupUI()
        gameUtils.setupListener(onLineClick = ::performClick)
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        binding.nm1Id.text = "(${gameUtils.nm1})"
        binding.nm2Id.text = "(${gameUtils.nm2})"

        val randLineId = gameUtils.idFromName(lineIDs.random())
        scope.launch {
            binding.volBtn.applyState(isMuted())
            isFirstRun = IO { pref.read("firstRun", true) }
            if (isFirstRun) {
                delay(200.milliseconds)
                gameUtils.infoShow(onEnd = {
                    scope.launch {
                        delay(600.milliseconds)
                        performClick(binding.root.findViewById(randLineId), true)
                    }
                })
            } else {
                delay(600.milliseconds)
                performClick(binding.root.findViewById(randLineId), true)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun performClick(view: View, isBot: Boolean = false) {
        val idNm = resources.getResourceEntryName(view.id)
        cat("performClick $idNm")
        val aroundIds = gameUtils.getAroundIdNames(idNm)
        cat("aroundIds $aroundIds")
        val bg = view.background.mutate() as GradientDrawable
        val color = gameUtils.getColorGrad(bg)

        if (color == gameUtils.whiteX && (gameUtils.plyrTurn || isBot) && lineIDs.isNotEmpty()) {
            cat("clickEnabled ${gameUtils.plyrTurn}")
            gameUtils.playLineClickSound()
            lineIDs.remove(idNm)
            gameUtils.clickCount++
            bg.setColor(if (isBot) gameUtils.redX else gameUtils.blueX)

            val extraTurn = gameUtils.handleBoxPair(
                aroundIds = aroundIds,
                isRedTurn = isBot
            )

            if (extraTurn)
                gameUtils.clickCount--
            else
                gameUtils.changePlayerTurn(isBot)
            cat("idNm $idNm, extraTurn $extraTurn, isBot $isBot, recursion $recursion, lineIDs.size ${lineIDs.size}")
            if (((extraTurn && isBot) || (!extraTurn && !isBot)) && lineIDs.isNotEmpty())
                handleAI(idNm, aroundIds)
            else
                gameUtils.plyrTurn = true

            cat("isGameOver ${gameUtils.isGameOver}, totalScore ${gameUtils.totalScore}")
            if (gameUtils.totalScore == 36 && !gameUtils.isGameOver) {
                scope.launch {
                    delay(950.milliseconds)
                    finishGame()
                }
            }
        }
    }

    private fun handleAI(idNm: String, aroundIds: GameUtils.AroundIds) {
        gameUtils.plyrTurn = false
        val halfTopFoundExtraTurn = checkAIHalf(
            box = aroundIds.boxTop,
            shouldCheck = gameUtils.shouldCheckTop(idNm)
        )
        val halfBottomFoundExtraTurn = checkAIHalf(
            box = aroundIds.boxBottom,
            shouldCheck = gameUtils.shouldCheckBottom(idNm),
            delayMillis = 650
        )
        cat("halfTopFoundExtraTurn $halfTopFoundExtraTurn, halfBottomFoundExtraTurn $halfBottomFoundExtraTurn")

        recursion = halfTopFoundExtraTurn && halfBottomFoundExtraTurn
        cat("handleAI recursion $recursion")
        if (halfTopFoundExtraTurn || halfBottomFoundExtraTurn) return

        handleAIRandom()
    }

    private fun checkAIHalf(
        box: GameUtils.Box?,
        shouldCheck: Boolean,
        delayMillis: Int = 500
    ): Boolean {
        if (!shouldCheck) return false
        box ?: return false
        var countColored = 0
        var blankLineName = ""

        box.lines.forEach {
            val v = binding.root.findViewById<View>(gameUtils.idFromName(it))
            val bg = v.background.mutate() as GradientDrawable
            if (gameUtils.getColorGrad(bg) != gameUtils.whiteX) countColored++
            else blankLineName = it
        }

        if (countColored == 2) {
            val lineId = gameUtils.idFromName(blankLineName)
            scope.launch {
                delay(delayMillis.milliseconds)
                performClick(binding.root.findViewById(lineId), true)
            }
            return true
        }
        return false
    }

    private fun handleAIRandom() {
        val availableIds = lineIDs.toMutableList()
        var selectedId = availableIds.random()

        while (availableIds.size > 1) {
            val (topCount, bottomCount) = getColorCounts(selectedId)

            val canBotLose = topCount > 1 || bottomCount > 1       // 2 line captured
            val canBotCapture = topCount == 3 || bottomCount == 3   // 3 line captured

            if (canBotLose && !canBotCapture) {
                availableIds.remove(selectedId)
                selectedId = availableIds.random()
            } else break
        }

        val viewId = gameUtils.idFromName(selectedId)
        scope.launch {
            delay(800.milliseconds)
            performClick(binding.root.findViewById(viewId), true)
        }
    }

    private fun getColorCounts(lineName: String): Pair<Int, Int> {
        val aroundIds = gameUtils.getAroundIdNames(lineName)

        fun countColored(box: GameUtils.Box?, shouldCheck: Boolean): Int {
            if (box == null || !shouldCheck) return 0
            return box.lines.count { line ->
                val view = binding.root.findViewById<View>(gameUtils.idFromName(line))
                val drawable = view.background.mutate() as GradientDrawable
                gameUtils.getColorGrad(drawable) != gameUtils.whiteX
            }
        }

        val topCount = countColored(
            box = aroundIds.boxTop,
            shouldCheck = gameUtils.shouldCheckTop(lineName)
        )
        val bottomCount = countColored(
            box = aroundIds.boxBottom,
            shouldCheck = gameUtils.shouldCheckBottom(lineName)
        )

        return topCount to bottomCount
    }

    private fun finishGame() {
        gameUtils.isGameOver = true
        gameUtils.playWinSound()
        binding.red.textSize = 30f
        binding.red.setTextColor(gameUtils.white)
        binding.blue.textSize = 30f
        binding.blue.setTextColor(gameUtils.white)

        if (gameUtils.scoreRed > gameUtils.scoreBlue) onGameOver("AI won the match.")
        else if (gameUtils.scoreBlue > gameUtils.scoreRed) {
            var winAI = pref.read("winAI", 0)
            pref.save("winAI", ++winAI)
            onGameOver("You won the match.")
        } else onGameOver("Match Draw.")
    }


    @SuppressLint("SetTextI18n")
    fun onGameOver(winMsg: String) {
        val builder = AlertDialog.Builder(parentActivity)
        val dialogBinding = DialogLayoutAlertBinding.inflate(LayoutInflater.from(parentActivity))
        builder.setView(dialogBinding.root)
        //builder.setCancelable(false)
        dialogBinding.textMessage.text = "" + winMsg
        dialogBinding.buttonNo.text = "Exit"
        dialogBinding.buttonYes.text = "Retry!"
        val alertDialog = builder.create()
        dialogBinding.buttonYes.setBounceClickListener {
            gameUtils.playButtonClickSound()
            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
            navigateSafe(Routes.GameBot) {
                popUpTo(Routes.GameBot::class) {
                    inclusive = true
                }
            }
        }
        dialogBinding.buttonNo.setBounceClickListener {
            gameUtils.playButtonClickSound()
            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
            popBackSafe()
        }
        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        runCatching { alertDialog?.show() }
    }

    companion object {
        private var _gameUtils: GameUtils? = null
        private val gameUtils: GameUtils
            get() = _gameUtils!!
    }
}