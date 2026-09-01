package com.diu.yk_games.line2box.util

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.BuildConfig
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogLayoutInfoBinding
import com.diu.yk_games.line2box.databinding.FragmentGameDualBinding
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.presentation.component.TurnBattleBar
import com.diu.yk_games.line2box.presentation.main.SettingsFragment
import kotlinx.coroutines.launch
import java.util.Objects

class GameUtils(
    var fragment: Fragment,
    var binding: FragmentGameDualBinding? = null,
    val isBot: Boolean = false,
    val isDual: Boolean = false
) {
    private val viewModel by fragment.activityViewModels<MainViewModel>()
    private var context = fragment.requireActivity()

    fun updateContext(context: FragmentActivity, binding: FragmentGameDualBinding) {
        this.context = context
        this.binding = binding
    }

    fun getColor(@ColorRes id: Int): Int =
        ContextCompat.getColor(context, id)

    val redX = getColor(R.color.redX)
    val redY = getColor(R.color.redY)
    val blueX = getColor(R.color.blueX)
    val blueY = getColor(R.color.blueY)
    val whiteX = getColor(R.color.whiteX)
    val whiteT = getColor(R.color.whiteT)
    val whiteY = getColor(R.color.whiteY)
    val white = getColor(R.color.white)

    val strokeSize = context.resources.getDimension(com.intuit.sdp.R.dimen._3sdp).toInt()

    var clickCount = 0
    var scoreRed = 0
    var scoreBlue = 0
    val totalScore get() = scoreRed + scoreBlue

    private var isRedTurnState by mutableStateOf(true)
    var nm1 by mutableStateOf("AI")
    var nm2 by mutableStateOf("Blue")
    var isFirstRun = false
    var isGameOver = false
    var lastHadExtraTurn = false
    var plyrTurn = false
    var firstBonus = true

    init {
        context.lifecycleScope.launch {
            resetGameBoard()
            isFirstRun = IO { pref.read("firstRun", true) }
            setupTurnUi()
        }
    }

    suspend fun resetGameBoard() {
        val binding = binding ?: return
        lineIDs.mapAsync {
            val bg = binding.root
                .findViewById<View>(idFromName(it))?.background?.mutate() as? GradientDrawable
            bg?.setColor(whiteX)
        }
        circleIds.mapAsync {
            val bg = binding.root
                .findViewById<View>(idFromName(it))?.background?.mutate() as? GradientDrawable
            if (bg == null)
                cat("resetGameBoard bg is null, id $it")
            bg?.setColor(white)
            bg?.setStroke(strokeSize, whiteY)
        }
    }

    fun getLineViewGroups() = binding?.run {
        listOf(rh1, rh2, rh3, rh4, rh5, rh6, rh7, rv1, rv2, rv3, rv4, rv5, rv6)
    }.orEmpty()

    fun setupListener(onLineClick: (View) -> Unit) {
        val binding = binding ?: return
        context.apply {
            lifecycleScope.launch {
                getLineViewGroups().forEach { viewGroup ->
                    repeat(viewGroup.childCount) { i ->
                        viewGroup.getChildAt(i)?.setOnClickListener(onLineClick)
                    }
                }
            }
            binding.settingBtn.setBounceClickListener {
                playButtonClickSound()
                SettingsFragment.show(context.supportFragmentManager)
            }
            binding.homeBtn.setBounceClickListener(::backBtn)
            binding.ideaBtn.setBounceClickListener(::ideaBtn)
        }
    }

    private fun backBtn(view: View) {
        playButtonClickSound()
        context.onBackPressed(view)
    }

    @Suppress("unused")
    private fun ideaBtn(view: View) {
        playButtonClickSound()
        infoShow()
    }

    fun infoShow(onEnd: (() -> Unit) = {}) {
        if (isFirstRun) pref.save("firstRun", false)
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
        val builder = AlertDialog.Builder(context)
        val dialogBinding = DialogLayoutInfoBinding.inflate(LayoutInflater.from(context))
        builder.setView(dialogBinding.root)
        builder.setCancelable(false)
        dialogBinding.textMessage.text = msg[0]
        dialogBinding.playGif.loadDrawable(gifs[0])
        dialogBinding.buttonPre.invisible()
        val alertDialog = builder.create()
        dialogBinding.buttonPre.setBounceClickListener {
            playButtonClickSound()
            if (i != 0) i--
            if (i == 0) dialogBinding.buttonPre.invisible()
            dialogBinding.textMessage.text = msg[i]
            dialogBinding.playGif.loadDrawable(gifs[i])
        }
        dialogBinding.buttonNext.setBounceClickListener {
            playButtonClickSound()
            if (i <= 4) i++
            if (!isFirstRun && i == 4) i++
            if (i == 1) dialogBinding.buttonPre.show()
            if (i >= 5) {
                runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
                onEnd.invoke()
            } else {
                dialogBinding.textMessage.text = msg[i]
                dialogBinding.playGif.loadDrawable(gifs[i])
            }
        }
        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        runCatching { alertDialog.show() }
    }

    private inline fun <T> orNull(condition: Boolean, value: () -> T): T? =
        if (condition) value() else null

    private fun lineId(row: Int, col: Int, type: Char) = "r${row}c${col}$type"
    private fun textId(row: Int, col: Int, type: Char = 'T') = "r${row}c${col}${type}x"
    private fun circleId(row: Int, col: Int) = "r${row}cr${col}"

    fun getAroundIdNames(idNm: String): AroundIds {
        val row = idNm[1].digitToInt()
        val col = idNm[3].digitToInt()

        return if (idNm[4] == 'T') {
            AroundIds(
                line = idNm,
                circleLeft = circleId(row, col),
                circleRight = circleId(row, col + 1),
                boxTop = orNull(row > 1) {
                    Box(
                        lineTop = lineId(row - 1, col, 'T'),
                        lineLeft = lineId(row - 1, col, 'L'),
                        lineRight = lineId(row - 1, col + 1, 'L'),
                        text = textId(row - 1, col),
                        circleTopLeft = circleId(row - 1, col),
                        circleTopRight = circleId(row - 1, col + 1),
                    )
                },
                boxBottom = orNull(row < 7) {
                    Box(
                        lineTop = lineId(row + 1, col, 'T'),
                        lineLeft = lineId(row, col, 'L'),
                        lineRight = lineId(row, col + 1, 'L'),
                        text = textId(row, col),
                        circleTopLeft = circleId(row + 1, col),
                        circleTopRight = circleId(row + 1, col + 1),
                    )
                },
            )
        } else {
            AroundIds(
                line = idNm,
                circleLeft = circleId(row + 1, col),
                circleRight = circleId(row, col),
                boxTop = orNull(col > 1) {
                    Box(
                        lineTop = lineId(row, col - 1, 'L'),
                        lineLeft = lineId(row + 1, col - 1, 'T'),
                        lineRight = lineId(row, col - 1, 'T'),
                        text = textId(row, col - 1),
                        circleTopLeft = circleId(row + 1, col - 1),
                        circleTopRight = circleId(row, col - 1),
                    )
                },
                boxBottom = orNull(col < 7) {
                    Box(
                        lineTop = lineId(row, col + 1, 'L'),
                        lineLeft = lineId(row + 1, col, 'T'),
                        lineRight = lineId(row, col, 'T'),
                        text = textId(row, col),
                        circleTopLeft = circleId(row, col + 1),
                        circleTopRight = circleId(row + 1, col + 1),
                    )
                },
            )
        }
    }

    fun getAroundIdNamesOld(idNm: String): List<String?> {
        val idS = arrayOfNulls<String>(14)
        val id = StringBuilder()
        if (idNm[4] == 'T') {
            if (idNm[1].digitToInt() > 1) {
                //top up//
                id.append(idNm)
                id.deleteCharAt(1)
                id.insert(1, idNm[1].digitToInt() - 1)
                idS[0] = id.toString()
                //txt up
                id.append('x')
                idS[6] = id.toString()
                //left up
                id.deleteCharAt(4)
                id.deleteCharAt(4)
                id.append('L')
                idS[1] = id.toString()
                //right up
                id.deleteCharAt(3)
                id.insert(3, idNm[3].digitToInt() + 1)
                idS[2] = id.toString()
                //circle up right
                id.insert(3, 'r')
                id.deleteCharAt(5)
                idS[11] = id.toString()
                //circle up left
                id.deleteCharAt(4)
                id.insert(4, idNm[3].digitToInt())
                idS[10] = id.toString()
            }
            if (idNm[1].digitToInt() < 7) {
                //down down//
                id.setLength(0)
                id.append(idNm)
                id.deleteCharAt(1)
                id.insert(1, idNm[1].digitToInt() + 1)
                idS[3] = id.toString()
                //left down
                id.setLength(0)
                id.append(idNm)
                id.deleteCharAt(4)
                id.append('L')
                idS[4] = id.toString()
                //right down
                id.deleteCharAt(3)
                id.insert(3, idNm[3].digitToInt() + 1)
                idS[5] = id.toString()
                //txt down
                id.setLength(0)
                id.append(idNm)
                id.append('x')
                idS[7] = id.toString()
                //circle Down left
                id.setLength(0)
                id.append(idNm)
                id.deleteCharAt(4)
                id.deleteCharAt(1)
                id.insert(1, idNm[1].digitToInt() + 1)
                id.insert(3, 'r')
                idS[13] = id.toString()
                //circle Down right
                id.deleteCharAt(4)
                id.insert(4, idNm[3].digitToInt() + 1)
                idS[12] = id.toString()
            }
            //circle Middle left
            id.setLength(0)
            id.append(idNm)
            id.insert(3, 'r')
            id.deleteCharAt(5)
            idS[8] = id.toString()
            //circle Middle right
            id.deleteCharAt(4)
            id.append(idNm[3].digitToInt() + 1)
            idS[9] = id.toString()
        } else if (idNm[4] == 'L') {
            if (idNm[3].digitToInt() > 1) {
                //top up//
                id.append(idNm)
                id.deleteCharAt(3)
                id.insert(3, idNm[3].digitToInt() - 1)
                idS[0] = id.toString()
                //right up
                id.deleteCharAt(4)
                id.append('T')
                idS[2] = id.toString()
                //txt up
                id.append('x')
                idS[6] = id.toString()
                //left up
                id.deleteCharAt(5)
                id.deleteCharAt(1)
                id.insert(1, idNm[1].digitToInt() + 1)
                idS[1] = id.toString()
                //circle up left
                id.delete(4, 6)
                id.insert(3, 'r')
                idS[10] = id.toString()
                //circle up right
                id.deleteCharAt(1)
                id.insert(1, idNm[1].digitToInt())
                idS[11] = id.toString()
            }
            if (idNm[3].digitToInt() < 7) {
                //down down//
                id.setLength(0)
                id.append(idNm)
                id.deleteCharAt(3)
                id.insert(3, idNm[3].digitToInt() + 1)
                idS[3] = id.toString()
                //right down
                id.setLength(0)
                id.append(idNm)
                id.deleteCharAt(4)
                id.insert(4, 'T')
                idS[5] = id.toString()
                //txt down
                id.append('x')
                idS[7] = id.toString()
                //left down
                id.deleteCharAt(1)
                id.insert(1, idNm[1].digitToInt() + 1)
                id.deleteCharAt(5)
                idS[4] = id.toString()
                //circle Down right
                id.setLength(0)
                id.append(idNm)
                id.deleteCharAt(4)
                id.deleteCharAt(3)
                id.insert(3, idNm[3].digitToInt() + 1)
                id.insert(3, 'r')
                idS[13] = id.toString()
                //circle Down left
                id.deleteCharAt(1)
                id.insert(1, idNm[1].digitToInt() + 1)
                idS[12] = id.toString()
            }
            //circle Middle right
            id.setLength(0)
            id.append(idNm)
            id.insert(3, 'r')
            id.deleteCharAt(5)
            idS[9] = id.toString()
            //circle Middle left
            id.deleteCharAt(1)
            id.insert(1, idNm[1].digitToInt() + 1)
            idS[8] = id.toString()
        }
        return idS.toList()
    }

    fun getColorGrad(bg: GradientDrawable): Int {
        var color = 0
        val aClass = bg.javaClass
        try {
            @SuppressLint("DiscouragedPrivateApi")
            val mFillPaint = aClass.getDeclaredField("mFillPaint")
            mFillPaint.isAccessible = true
            val strokePaint = mFillPaint[bg] as Paint
            color = Objects.requireNonNull(strokePaint).color
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return color
    }

    fun shouldCheckTop(idNm: String): Boolean {
        val shouldCheckTop = (idNm[1].digitToInt() > 1 && idNm[4] == 'T') ||
                (idNm[3].digitToInt() > 1 && idNm[4] == 'L')
        cat("shouldCheckTop $shouldCheckTop, idNm $idNm")
        return shouldCheckTop
    }

    fun shouldCheckBottom(idNm: String): Boolean {
        val shouldCheckBottom = (idNm[1].digitToInt() < 7 && idNm[4] == 'T') ||
                (idNm[3].digitToInt() < 7 && idNm[4] == 'L')
        cat("shouldCheckBottom $shouldCheckBottom, idNm $idNm")
        return shouldCheckBottom
    }

    fun handleBoxPair(aroundIds: AroundIds, isRedTurn: Boolean): Boolean {
        val hasExtraTurn1 = handleBox(
            aroundIds = aroundIds,
            box = aroundIds.boxTop,
            isRedTurn = isRedTurn,
            shouldCheck = shouldCheckTop(aroundIds.line)
        )
        val hasExtraTurn2 = handleBox(
            aroundIds = aroundIds,
            box = aroundIds.boxBottom,
            isRedTurn = isRedTurn,
            shouldCheck = shouldCheckBottom(aroundIds.line)
        )
        lastHadExtraTurn = hasExtraTurn1 || hasExtraTurn2
        return lastHadExtraTurn
    }

    fun handleBox(
        aroundIds: AroundIds,
        box: Box?,
        isRedTurn: Boolean,
        shouldCheck: Boolean
    ): Boolean {
        val binding = binding ?: return false
        if (!shouldCheck) return false
        this.cat("handleBox4 ${aroundIds.line}, box $box")
        box ?: return false
        val u = binding.root.findViewById<View>(idFromName(box.lineTop))
        val l = binding.root.findViewById<View>(idFromName(box.lineLeft))
        val r = binding.root.findViewById<View>(idFromName(box.lineRight))

        if (!isAllColored(u, l, r)) return false

        val txt = binding.root.findViewById<TextView>(idFromName(box.text))

        val mid1 = binding.root.findViewById<View>(idFromName(aroundIds.circleLeft))
        val mid2 = binding.root.findViewById<View>(idFromName(aroundIds.circleRight))
        val up1 =
            binding.root.findViewById<View>(idFromName(box.circleTopLeft))
        val up2 = binding.root.findViewById<View>(idFromName(box.circleTopRight))

        colorCapturedBox(
            innerText = txt,
            lineUp = u,
            lineLeft = l,
            lineRight = r,
            circleMidLeft = mid1,
            circleMidRight = mid2,
            circleUpLeft = up1,
            circleUpRight = up2,
            isRedTurn = isRedTurn
        )
        return true
    }

    fun colorCapturedBox(
        innerText: TextView,
        lineUp: View, lineLeft: View, lineRight: View,
        circleMidLeft: View, circleMidRight: View,
        circleUpLeft: View, circleUpRight: View,
        isRedTurn: Boolean
    ) {
        val binding = binding ?: return
        playBoxSound()

        viewModel.increaseServerScore(isRedTurn)
        if (isRedTurn) {
            scoreRed++
            binding.scoreRed.text = scoreRed.toString()
            innerText.text = nm1.first().toString()
        } else {
            scoreBlue++
            binding.scoreBlue.text = scoreBlue.toString()
            innerText.text = nm2.first().toString()
        }
        if (firstBonus) {
            firstBonus = false
            val txt = when {
                !isBot && isRedTurn -> "Bonus TURN for $nm1"
                isBot && !isRedTurn -> "Bonus TURN for you"
                !isRedTurn -> "Bonus TURN for $nm2"
                else -> ""
            }
            if (txt.isNotEmpty())
                context.toast(txt)
        }

        innerText.typeface = ResourcesCompat.getFont(context, R.font.bertram)

        val color = if (isRedTurn) redX else blueX
        val stroke = if (isRedTurn) redY else blueY

        listOf(lineUp, lineLeft, lineRight).forEach {
            val bg = it.background.mutate() as GradientDrawable
            bg.setColor(color)
        }
        listOf(circleMidLeft, circleMidRight, circleUpLeft, circleUpRight).forEach {
            val bg = it.background.mutate() as GradientDrawable
            bg.setColor(color)
            bg.setStroke(strokeSize, stroke)
        }
    }

    fun setupTurnUi() {
        val binding = binding ?: return
        binding.turnBar.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TurnBattleBar(
                    redName = nm1,
                    blueName = nm2,
                    isRedTurn = isRedTurnState,
                    showTurnText = { forRed ->
                        !forRed || isDual || (!isBot && viewModel.matchRouteInfo.isPlyr1)
                    }
                )
            }
        }
    }

    fun changePlayerTurnUi(isRedTurn: Boolean) {
        isRedTurnState = !isRedTurn
    }

    fun isAllColored(vararg views: View): Boolean {
        return views.all {
            val bg = it.background.mutate() as GradientDrawable
            val c = getColorGrad(bg)
            cat("isAllColored - getColorGrad $c, whiteX $whiteX, blueX $blueX, redX $redX")
            c == redX || c == blueX
        }.also {
            cat("isAllColored $it")
        }
    }

    @SuppressLint("DiscouragedApi")
    fun idFromName(name: String?): Int {
        return context.resources.getIdentifier(name, "id", BuildConfig.APPLICATION_ID)
    }

    fun playBoxSound() = viewModel.player.playBoxSound()
    fun playLineClickSound() = viewModel.player.playLineClickSound()
    fun playButtonClickSound() = viewModel.player.playButtonClickSound()
    fun playWinSound() = viewModel.player.playWinSound()

    data class Box(
        val lineTop: String,
        val lineLeft: String,
        val lineRight: String,
        val text: String,
        val circleTopLeft: String,
        val circleTopRight: String
    ) {
        val lines = listOf(lineTop, lineLeft, lineRight)
    }

    data class AroundIds(
        val line: String,
        val circleLeft: String,
        val circleRight: String,
        val boxTop: Box? = null,
        val boxBottom: Box? = null
    )

    // @formatter:off
    companion object {
        val lineIDs = listOf(
            "r1c1T", "r1c1L", "r1c2T", "r1c2L", "r1c3T", "r1c3L", "r1c4T", "r1c4L", "r1c5T", "r1c5L", "r1c6T", "r1c6L", "r1c7L",
            "r2c1T", "r2c1L", "r2c2T", "r2c2L", "r2c3T", "r2c3L", "r2c4T", "r2c4L", "r2c5T", "r2c5L", "r2c6T", "r2c6L", "r2c7L",
            "r3c1T", "r3c1L", "r3c2T", "r3c2L", "r3c3T", "r3c3L", "r3c4T", "r3c4L", "r3c5T", "r3c5L", "r3c6T", "r3c6L", "r3c7L",
            "r4c1T", "r4c1L", "r4c2T", "r4c2L", "r4c3T", "r4c3L", "r4c4T", "r4c4L", "r4c5T", "r4c5L", "r4c6T", "r4c6L", "r4c7L",
            "r5c1T", "r5c1L", "r5c2T", "r5c2L", "r5c3T", "r5c3L", "r5c4T", "r5c4L", "r5c5T", "r5c5L", "r5c6T", "r5c6L", "r5c7L",
            "r6c1T", "r6c1L", "r6c2T", "r6c2L", "r6c3T", "r6c3L", "r6c4T", "r6c4L", "r6c5T", "r6c5L", "r6c6T", "r6c6L", "r6c7L",
            "r7c1T", "r7c2T", "r7c3T", "r7c4T", "r7c5T", "r7c6T"
        )

        val circleIds = listOf(
            "r1cr1", "r1cr2", "r1cr3", "r1cr4", "r1cr5", "r1cr6", "r1cr7",
            "r2cr1", "r2cr2", "r2cr3", "r2cr4", "r2cr5", "r2cr6", "r2cr7",
            "r3cr1", "r3cr2", "r3cr3", "r3cr4", "r3cr5", "r3cr6", "r3cr7",
            "r4cr1", "r4cr2", "r4cr3", "r4cr4", "r4cr5", "r4cr6", "r4cr7",
            "r5cr1", "r5cr2", "r5cr3", "r5cr4", "r5cr5", "r5cr6", "r5cr7",
            "r6cr1", "r6cr2", "r6cr3", "r6cr4", "r6cr5", "r6cr6", "r6cr7",
            "r7cr1", "r7cr2", "r7cr3", "r7cr4", "r7cr5", "r7cr6", "r7cr7"
        )

        val countryEmojis = listOf(
            "🇦🇫", "🇦🇱", "🇩🇿", "🇦🇩", "🇦🇴", "🇦🇬", "🇦🇷", "🇦🇲", "🇦🇺", "🇦🇹", "🇦🇿", "🇧🇸", "🇧🇭", "🇧🇩", "🇧🇧", "🇧🇾", "🇧🇪",
            "🇧🇿", "🇧🇯", "🇧🇹", "🇧🇴", "🇧🇦", "🇧🇼", "🇧🇷", "🇧🇳", "🇧🇬", "🇧🇫", "🇧🇮", "🇨🇻", "🇰🇭", "🇨🇲", "🇨🇦", "🇨🇫", "🇹🇩",
            "🇨🇱", "🇨🇳", "🇨🇴", "🇰🇲", "🇨🇩", "🇨🇷", "🇭🇷", "🇨🇺", "🇨🇾", "🇨🇿", "🇨🇮", "🇩🇰", "🇩🇯", "🇩🇲", "🇩🇴", "🇨🇩", "🇪🇨",
            "🇪🇬", "🇸🇻", "🏴󠁧󠁢󠁥󠁮󠁧󠁿", "🇬🇶", "🇪🇷", "🇪🇪", "🇸🇿", "🇪🇹", "🇫🇯", "🇫🇮", "🇫🇷", "🇬🇦", "🇬🇲", "🇬🇪", "🇩🇪", "🇬🇭", "🇬🇷",
            "🇬🇩", "🇬🇹", "🇬🇳", "🇬🇼", "🇬🇾", "🇭🇹", "🇭🇳", "🇭🇰", "🇭🇺", "🇮🇸", "🇮🇳", "🇮🇩", "🇮🇷", "🇮🇶", "🇮🇪", "🇮🇱", "🇮🇹",
            "🇯🇲", "🇯🇵", "🇯🇴", "🇰🇿", "🇰🇪", "🇰🇮", "🇰🇼", "🇰🇬", "🇱🇦", "🇱🇻", "🇱🇧", "🇱🇸", "🇱🇷", "🇱🇾", "🇱🇮", "🇱🇹", "🇱🇺",
            "🇲🇬", "🇲🇼", "🇲🇾", "🇲🇻", "🇲🇱", "🇲🇹", "🇲🇭", "🇲🇶", "🇲🇺", "🇲🇽", "🇫🇲", "🇲🇩", "🇲🇨", "🇲🇳", "🇲🇪", "🇲🇦", "🇲🇿",
            "🇲🇲", "🇳🇦", "🇳🇷", "🇳🇵", "🇳🇱", "🇳🇿", "🇳🇮", "🇳🇪", "🇳🇬", "🇰🇵", "🇲🇰", "🇳🇴", "🇴🇲", "🇵🇰", "🇵🇼", "🇵🇸", "🇵🇦",
            "🇵🇬", "🇵🇾", "🇵🇪", "🇵🇭", "🇵🇱", "🇵🇹", "🇶🇦", "🇷🇴", "🇷🇺", "🇷🇼", "🇰🇳", "🇱🇨", "🇻🇨", "🇼🇸", "🇸🇲", "🇸🇹", "🇸🇦",
            "🏴󠁧󠁢󠁳󠁣󠁴󠁿", "🇸🇳", "🇷🇸", "🇸🇨", "🇸🇱", "🇸🇬", "🇸🇰", "🇸🇮", "🇸🇧", "🇸🇴", "🇿🇦", "🇰🇷", "🇸🇸", "🇪🇸", "🇱🇰", "🇸🇩", "🇸🇷",
            "🇸🇪", "🇨🇭", "🇸🇾", "🇹🇼", "🇹🇯", "🇹🇿", "🇹🇭", "🇹🇱", "🇹🇬", "🇹🇴", "🇹🇹", "🇹🇳", "🇹🇷", "🇹🇲", "🇹🇻", "🇺🇬", "🇺🇦",
            "🇦🇪", "🇬🇧", "🇺🇸", "🇺🇾", "🇺🇿", "🇻🇺", "🇻🇪", "🇻🇳", "🇾🇪", "🇿🇲", "🇿🇼"
        )

        val countryNm = listOf(
            "Afghanistan", "Albania", "Algeria", "Andorra", "Angola", "Antigua and Barbuda", "Argentina", "Armenia", "Australia", "Austria", "Azerbaijan", "Bahamas", "Bahrain", "Bangladesh", "Barbados", "Belarus", "Belgium", "Belize", "Benin", "Bhutan", "Bolivia", "Bosnia and Herzegovina", "Botswana", "Brazil", "Brunei",
            "Bulgaria", "Burkina Faso", "Burundi", "Cabo Verde", "Cambodia", "Cameroon", "Canada", "Central African Republic", "Chad", "Chile", "China", "Colombia", "Comoros", "Congo", "Costa Rica", "Croatia", "Cuba", "Cyprus", "Czechia", "Côte d'Ivoire", "Denmark", "Djibouti", "Dominica", "Dominican Republic", "DR Congo",
            "Ecuador", "Egypt", "El Salvador", "England", "Equatorial Guinea", "Eritrea", "Estonia", "Eswatini (Swaziland)", "Ethiopia", "Fiji", "Finland", "France", "Gabon", "Gambia", "Georgia", "Germany", "Ghana", "Greece", "Grenada", "Guatemala", "Guinea", "Guinea-Bissau", "Guyana", "Haiti", "Honduras", "Hong Kong", "Hungary",
            "Iceland", "India", "Indonesia", "Iran", "Iraq", "Ireland", "Israel", "Italy", "Jamaica", "Japan", "Jordan", "Kazakhstan", "Kenya", "Kiribati", "Kuwait", "Kyrgyzstan", "Laos", "Latvia", "Lebanon", "Lesotho", "Liberia", "Libya", "Liechtenstein", "Lithuania", "Luxembourg", "Madagascar", "Malawi", "Malaysia", "Maldives",
            "Mali", "Malta", "Marshall Islands", "Martinique", "Mauritius", "Mexico", "Micronesia", "Moldova", "Monaco", "Mongolia", "Montenegro", "Morocco", "Mozambique", "Myanmar", "Namibia", "Nauru", "Nepal", "Netherlands", "New Zealand", "Nicaragua", "Niger", "Nigeria", "North Korea", "North Macedonia", "Norway", "Oman",
            "Pakistan", "Palau", "Palestine", "Panama", "Papua New Guinea", "Paraguay", "Peru", "Philippines", "Poland", "Portugal", "Qatar", "Romania", "Russia", "Rwanda", "Saint Kitts and Nevis", "Saint Lucia", "Saint Vincent", "Samoa", "San Marino", "São Tomé and Príncipe", "Saudi Arabia", "Scotland", "Senegal", "Serbia",
            "Seychelles", "Sierra Leone", "Singapore", "Slovakia", "Slovenia", "Solomon Islands", "Somalia", "South Africa", "South Korea", "South Sudan", "Spain", "Sri Lanka", "Sudan", "Suriname", "Sweden", "Switzerland", "Syria", "Taiwan", "Tajikistan", "Tanzania", "Thailand", "Timor-Leste", "Togo", "Tonga",
            "Trinidad and Tobago", "Tunisia", "Turkey", "Turkmenistan", "Tuvalu", "Uganda", "Ukraine", "United Arab Emirates", "United Kingdom", "United States", "Uruguay", "Uzbekistan", "Vanuatu", "Venezuela", "Vietnam", "Yemen", "Zambia", "Zimbabwe"
        )


        val countryList = listOf(
            "Afghanistan" to "🇦🇫", "Albania" to "🇦🇱", "Algeria" to "🇩🇿", "Andorra" to "🇦🇩", "Angola" to "🇦🇴", "Antigua and Barbuda" to "🇦🇬", "Argentina" to "🇦🇷", "Armenia" to "🇦🇲", "Australia" to "🇦🇺", "Austria" to "🇦🇹", "Azerbaijan" to "🇦🇿", "Bahamas" to "🇧🇸", "Bahrain" to "🇧🇭", "Bangladesh" to "🇧🇩", "Barbados" to "🇧🇧", "Belarus" to "🇧🇾",
            "Belgium" to "🇧🇪", "Belize" to "🇧🇿", "Benin" to "🇧🇯", "Bhutan" to "🇧🇹", "Bolivia" to "🇧🇴", "Bosnia and Herzegovina" to "🇧🇦", "Botswana" to "🇧🇼", "Brazil" to "🇧🇷", "Brunei" to "🇧🇳", "Bulgaria" to "🇧🇬", "Burkina Faso" to "🇧🇫", "Burundi" to "🇧🇮", "Cabo Verde" to "🇨🇻", "Cambodia" to "🇰🇭", "Cameroon" to "🇨🇲", "Canada" to "🇨🇦",
            "Central African Republic" to "🇨🇫", "Chad" to "🇹🇩", "Chile" to "🇨🇱", "China" to "🇨🇳", "Colombia" to "🇨🇴", "Comoros" to "🇰🇲", "Congo" to "🇨🇩", "Costa Rica" to "🇨🇷", "Croatia" to "🇭🇷", "Cuba" to "🇨🇺", "Cyprus" to "🇨🇾", "Czechia" to "🇨🇿", "Côte d'Ivoire" to "🇨🇮", "Denmark" to "🇩🇰", "Djibouti" to "🇩🇯", "Dominica" to "🇩🇲",
            "Dominican Republic" to "🇩🇴", "DR Congo" to "🇨🇩", "Ecuador" to "🇪🇨", "Egypt" to "🇪🇬", "El Salvador" to "🇸🇻", "England" to "🏴", "Equatorial Guinea" to "🇬🇶", "Eritrea" to "🇪🇷", "Estonia" to "🇪🇪", "Eswatini (Swaziland)" to "🇸🇿", "Ethiopia" to "🇪🇹", "Fiji" to "🇫🇯", "Finland" to "🇫🇮", "France" to "🇫🇷", "Gabon" to "🇬🇦", "Gambia" to "🇬🇲",
            "Georgia" to "🇬🇪", "Germany" to "🇩🇪", "Ghana" to "🇬🇭", "Greece" to "🇬🇷", "Grenada" to "🇬🇩", "Guatemala" to "🇬🇹", "Guinea" to "🇬🇳", "Guinea-Bissau" to "🇬🇼", "Guyana" to "🇬🇾", "Haiti" to "🇭🇹", "Honduras" to "🇭🇳", "Hong Kong" to "🇭🇰", "Hungary" to "🇭🇺", "Iceland" to "🇮🇸", "India" to "🇮🇳", "Indonesia" to "🇮🇩", "Iran" to "🇮🇷",
            "Iraq" to "🇮🇶", "Ireland" to "🇮🇪", "Italy" to "🇮🇹", "Jamaica" to "🇯🇲", "Japan" to "🇯🇵", "Jordan" to "🇯🇴", "Kazakhstan" to "🇰🇿", "Kenya" to "🇰🇪", "Kiribati" to "🇰🇮", "Kuwait" to "🇰🇼", "Kyrgyzstan" to "🇰🇬", "Laos" to "🇱🇦", "Latvia" to "🇱🇻", "Lebanon" to "🇱🇧", "Lesotho" to "🇱🇸", "Liberia" to "🇱🇷", "Libya" to "🇱🇾", "Liechtenstein" to "🇱🇮",
            "Lithuania" to "🇱🇹", "Luxembourg" to "🇱🇺", "Madagascar" to "🇲🇬", "Malawi" to "🇲🇼", "Malaysia" to "🇲🇾", "Maldives" to "🇲🇻", "Mali" to "🇲🇱", "Malta" to "🇲🇹", "Marshall Islands" to "🇲🇭", "Mexico" to "🇲🇽", "Moldova" to "🇲🇩", "Monaco" to "🇲🇨", "Mongolia" to "🇲🇳", "Montenegro" to "🇲🇪", "Morocco" to "🇲🇦", "Mozambique" to "🇲🇿",
            "Myanmar" to "🇲🇲", "Namibia" to "🇳🇦", "Nauru" to "🇳🇷", "Nepal" to "🇳🇵", "Netherlands" to "🇳🇱", "New Zealand" to "🇳🇿", "Nicaragua" to "🇳🇮", "Niger" to "🇳🇪", "Nigeria" to "🇳🇬", "North Korea" to "🇰🇵", "North Macedonia" to "🇲🇰", "Norway" to "🇳🇴", "Oman" to "🇴🇲", "Pakistan" to "🇵🇰", "Palestine" to "🇵🇸", "Panama" to "🇵🇦", "Papua New Guinea" to "🇵🇬",
            "Paraguay" to "🇵🇾", "Peru" to "🇵🇪", "Philippines" to "🇵🇭", "Poland" to "🇵🇱", "Portugal" to "🇵🇹", "Qatar" to "🇶🇦", "Romania" to "🇷🇴", "Russia" to "🇷🇺", "Rwanda" to "🇷🇼", "Saudi Arabia" to "🇸🇦", "Scotland" to "🏴", "Serbia" to "🇷🇸", "South Korea" to "🇰🇷", "Spain" to "🇪🇸", "Sri Lanka" to "🇱🇰", "Turkey" to "🇹🇷", "United Arab Emirates" to "🇦🇪",
            "United Kingdom" to "🇬🇧", "United States" to "🇺🇸", "Uruguay" to "🇺🇾", "Uzbekistan" to "🇺🇿", "Vanuatu" to "🇻🇺", "Venezuela" to "🇻🇪", "Vietnam" to "🇻🇳", "Yemen" to "🇾🇪", "Zambia" to "🇿🇲", "Zimbabwe" to "🇿🇼"
        )
    }
}