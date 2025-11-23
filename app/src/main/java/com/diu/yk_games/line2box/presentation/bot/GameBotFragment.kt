package com.diu.yk_games.line2box.presentation.bot

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogLayoutAlertBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutInfoBinding
import com.diu.yk_games.line2box.databinding.FragmentGameDualBinding
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.presentation.main.StartActivity
import com.diu.yk_games.line2box.util.applyState
import com.diu.yk_games.line2box.util.cat
import com.diu.yk_games.line2box.util.invisible
import com.diu.yk_games.line2box.util.isMuted
import com.diu.yk_games.line2box.util.isNotMuted
import com.diu.yk_games.line2box.util.loadDrawable
import com.diu.yk_games.line2box.util.log
import com.diu.yk_games.line2box.util.onBackPressed
import com.diu.yk_games.line2box.util.performOnClickF
import com.diu.yk_games.line2box.util.pref
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.diu.yk_games.line2box.util.show
import com.diu.yk_games.line2box.util.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Objects
import java.util.Random

class GameBotFragment : Fragment() {
    private lateinit var binding: FragmentGameDualBinding
    private val viewModel: MainViewModel by activityViewModels()
    lateinit var parentActivity: FragmentActivity

    private val lineIDs by lazy { viewModel.lineIDs.toMutableList() }
    private var random = Random()

    //MediaPlayer lineClick, boxPlus, winSoundEf, btnClick;
    private var isFirstRun = false
    private var recursion = false
    private var clickEnabled = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGameDualBinding.inflate(inflater, container, false)
        parentActivity = requireActivity()
        return binding.root
    }

    @SuppressLint("DiscouragedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupListener()
    }

    @SuppressLint("DiscouragedApi")
    private fun setupUI() {
        clickCount = 0
        scoreRed = 0
        scoreBlue = 0
        bestScore = 9999
        val ind = random.nextInt(84)
        val randLineId = resources.getIdentifier(lineIDs[ind], "id", parentActivity.packageName)
        lifecycleScope.launch {
            binding.volBtn.applyState(isMuted())
            isFirstRun = pref.read("firstRun", true)
            if (isFirstRun) {
                delay(200)
                infoShow {
                    lifecycleScope.launch {
                        delay(600)
                        performClick(binding.root.findViewById(randLineId), true)
                    }
                }
            } else {
                delay(600)
                performClick(binding.root.findViewById(randLineId), true)
            }
        }
    }

    fun setupListener() {
        viewModel.getLineViewGroups(binding).forEach { viewGroup ->
            repeat(viewGroup.childCount) { i ->
                viewGroup.getChildAt(i)?.setOnClickListener {
                    performClick(it, false)
                }
            }
        }
        binding.volBtn.performOnClickF()
        binding.ideaBtn.setBounceClickListener(::ideaBtn)
        binding.homeBtn.setBounceClickListener(::backBtn)
    }
    
    val redX by lazy { resources.getColor(R.color.redX, parentActivity.theme) }
    val redY by lazy { resources.getColor(R.color.redY, parentActivity.theme) }
    val blueX by lazy { resources.getColor(R.color.blueX, parentActivity.theme) }
    val blueY by lazy { resources.getColor(R.color.blueY, parentActivity.theme) }
    val whiteX by lazy { resources.getColor(R.color.whiteX, parentActivity.theme) }
    val whiteT by lazy { resources.getColor(R.color.whiteT, parentActivity.theme) }
    val whiteY by lazy { resources.getColor(R.color.whiteY, parentActivity.theme) }
    val white by lazy { resources.getColor(R.color.white, parentActivity.theme) }

    @SuppressLint("SetTextI18n", "DiscouragedApi")
    fun performClick(view: View, isBot: Boolean) {
        val idNm = resources.getResourceEntryName(view.id)
        idNm.log()
        val aroundIds = getIdNm(idNm)
        val bg = view.background.mutate() as GradientDrawable
        val color = getColorGrad(bg)

        if (color == whiteX && (clickEnabled || isBot) && lineIDs.isNotEmpty()) {
            cat("clickEnabled")
            playLineClickSound()
            lineIDs.remove(idNm)
            clickCount++
            bg.setColor(if (isBot) redX else blueX)

            val extraTurn =
                (shouldCheckTop(idNm) && handleBox(aroundIds, true, isBot)) ||
                (shouldCheckBottom(idNm) && handleBox(aroundIds, false, isBot))

            if (extraTurn) {
                clickCount--
            } else handleTurnUI(isBot)
            cat("idNm $idNm, extraTurn $extraTurn, isBot $isBot, recursion $recursion, lineIDs.size ${lineIDs.size}")
            if (((extraTurn && isBot) || (!extraTurn && !isBot)) && lineIDs.isNotEmpty())
                handleAI(idNm)
            else
                clickEnabled = true

            if (scoreRed + scoreBlue == 36 && !isGameOver) {
                lifecycleScope.launch {
                    delay(950)
                    finishGame()
                }
            }
        }
    }

    private fun playLineClickSound() {
        isNotMuted {
            val mp = MediaPlayer.create(parentActivity, R.raw.line_click_ef)
            mp.start()
            mp.setOnCompletionListener(MediaPlayer::release)
        }
    }

    private fun shouldCheckTop(idNm: String): Boolean {
        return (idNm[1].digitToInt() > 1 && idNm[4] == 'T') ||
                (idNm[3].digitToInt() > 1 && idNm[4] == 'L')
    }

    private fun shouldCheckBottom(idNm: String): Boolean {
        return (idNm[1].digitToInt() < 7 && idNm[4] == 'T') ||
                (idNm[3].digitToInt() < 7 && idNm[4] == 'L')
    }

    private fun handleBox(aroundIds: List<String?>, isTop: Boolean, isBot: Boolean): Boolean {
        cat("handleBox isTop $isTop")
        val u = binding.root.findViewById<View>(idFromName(aroundIds[if(isTop) 0 else 3]
            .also { it.log("handleBox") }))
        val l = binding.root.findViewById<View>(idFromName(aroundIds[if(isTop) 1 else 4]
            .also { it.log("handleBox") }))
        val r = binding.root.findViewById<View>(idFromName(aroundIds[if(isTop) 2 else 5]
            .also { it.log("handleBox") }))

        if (!isAllColored(u, l, r)) return false

        val txt = binding.root.findViewById<TextView>(idFromName(aroundIds[if(isTop) 6 else 7]))

        val mid1 = binding.root.findViewById<View>(idFromName(aroundIds[8]))
        val mid2 = binding.root.findViewById<View>(idFromName(aroundIds[9]))
        val up1 = binding.root.findViewById<View>(idFromName(aroundIds[if(isTop) 10 else 12])) //down1 ifTop
        val up2 = binding.root.findViewById<View>(idFromName(aroundIds[if(isTop) 11 else 13]))

        colorCapturedBox(
            innerText = txt,
            lineUp = u,
            lineLeft = l,
            lineRight = r,
            circleMidLeft = mid1,
            circleMidRight = mid2,
            circleUpLeft = up1,
            circleUpRight = up2,
            isBot = isBot
        )
        return true
    }

    private fun isAllColored(vararg views: View): Boolean {
        return views.all {
            val bg = it.background.mutate() as GradientDrawable
            val c = getColorGrad(bg)
            c == redX || c == blueX
        }.also {
            cat("isAllColored $it")
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun idFromName(name: String?): Int {
        return resources.getIdentifier(name, "id", parentActivity.packageName)
    }

    private fun colorCapturedBox(
        innerText: TextView,
        lineUp: View, lineLeft: View, lineRight: View,
        circleMidLeft: View, circleMidRight: View,
        circleUpLeft: View, circleUpRight: View, isBot: Boolean
    ) {
        playBoxSound()

        if (isBot) {
            scoreRed++
            binding.scoreRed.text = scoreRed.toString()
            innerText.text = nm1.first().toString()
        } else {
            scoreBlue++
            binding.scoreBlue.text = scoreBlue.toString()
            innerText.text = nm2.first().toString()
            if (one) {
                one = false
                toast("Bonus TURN for you")
            }
        }

        innerText.typeface = ResourcesCompat.getFont(parentActivity, R.font.bertram)

        val color = if (isBot) redX else blueX
        val stroke = if (isBot) redY else blueY

        listOf(lineUp, lineLeft, lineRight).forEach {
            val bg = it.background.mutate() as GradientDrawable
            bg.setColor(color)
        }
        listOf(circleMidLeft, circleMidRight, circleUpLeft, circleUpRight).forEach {
            val bg = it.background.mutate() as GradientDrawable
            bg.setColor(color)
            bg.setStroke(14, stroke)
        }
    }

    private fun playBoxSound() {
        isNotMuted {
            val mp = MediaPlayer.create(parentActivity, R.raw.box_ef)
            mp.start()
            mp.setOnCompletionListener(MediaPlayer::release)
        }
    }

    private fun handleTurnUI(isBot: Boolean) {
        if (isBot) {
            clickEnabled = true
            binding.red.textSize = 30f
            binding.red.setTextColor(whiteT)
            binding.blue.textSize = 35f
            binding.blue.setTextColor(white)
        } else {
            binding.blue.textSize = 30f
            binding.blue.setTextColor(whiteT)
            binding.red.textSize = 35f
            binding.red.setTextColor(white)
        }
    }

    private fun handleAI(idNm: String) {
        clickEnabled = false
        val halfTopFoundExtraTurn = checkAIHalf(idNm, true)
        val halfBottomFoundExtraTurn = checkAIHalf(idNm, false)

        if (halfTopFoundExtraTurn && halfBottomFoundExtraTurn) recursion = true
        if (halfTopFoundExtraTurn || halfBottomFoundExtraTurn) return

        if(!recursion)
            handleAIRandom()
        else
            recursion = false
    }

    private fun checkAIHalf(idNm: String, isTop: Boolean): Boolean {
        val ids = getIdNm(idNm)
        val idx = if (isTop) 0 else 3

        var countColored = 0
        var blankIndex = -1

        if ((isTop && shouldCheckTop(idNm)) || (!isTop && shouldCheckBottom(idNm))){
            for (i in idx..idx + 2) {
                val v = binding.root.findViewById<View>(idFromName(ids[i]))
                val bg = v.background.mutate() as GradientDrawable
                if (getColorGrad(bg) != whiteX) countColored++
                else blankIndex = i
            }

            if (countColored == 2) {
                val lineId = idFromName(ids[blankIndex])
                lifecycleScope.launch {
                    delay(if (isTop) 500 else 650)
                    performClick(binding.root.findViewById(lineId), true)
                }
                return true
            }
        }
        return false
    }

    private fun handleAIRandom() {
        val ids = lineIDs.toMutableList()
        var name = lineIDs[random.nextInt(ids.size)]

        while (true) {
            val countUp = countColored(name, true)
            val countDn = countColored(name, false)

            if ((countUp > 1 || countDn > 1) && !(countUp == 3 || countDn == 3)) {
                if (ids.size == 1) break
                ids.remove(name)
                name = ids[random.nextInt(ids.size)]
            } else break
        }

        val lineId = idFromName(name)
        lifecycleScope.launch {
            delay(800)
            performClick(binding.root.findViewById(lineId), true)
        }
    }

    private fun countColored(randLineIdNm: String, isTop: Boolean): Int {
        val aroundIds = getIdNm(randLineIdNm)
        var c = 0
        if ((isTop && shouldCheckTop(randLineIdNm)) || (!isTop && shouldCheckBottom(randLineIdNm))){
            val start = if (isTop) 0 else 3
            for (i in start..start + 2) {
                val v = binding.root.findViewById<View>(idFromName(aroundIds[i]))
                val bg = v.background.mutate() as GradientDrawable
                if (getColorGrad(bg) != whiteX) c++
            }
        }
        return c
    }

    private fun finishGame() {
        isGameOver = true
        playWinSound()
        binding.red.textSize = 30f
        binding.red.setTextColor(white)
        binding.blue.textSize = 30f
        binding.blue.setTextColor(white)

        if (scoreRed > scoreBlue) onGameOver("AI won the match.")
        else if (scoreBlue > scoreRed) {
            var winAI = pref.read("winAI", 0)
            pref.save("winAI", ++winAI)
            onGameOver("You won the match.")
        } else onGameOver("Match Draw.")
    }

    private fun playWinSound() {
        isNotMuted {
            val mp = MediaPlayer.create(parentActivity, R.raw.win_ef)
            mp.start()
            mp.setOnCompletionListener(MediaPlayer::release)
        }
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
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
            startActivity(Intent(parentActivity, GameBotFragment::class.java))
            onBackPressed()
        }
        dialogBinding.buttonNo.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
            startActivity(Intent(parentActivity, StartActivity::class.java))
            onBackPressed()
            flag = true
        }
        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        runCatching { alertDialog?.show() }
    }

    @Suppress("unused")
    private fun ideaBtn(view: View) {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        infoShow()
    }

    private fun backBtn(view: View) {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        onBackPressed(view)
    }

    private fun infoShow(finish: (() -> Unit)? = null) {
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
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            if (i != 0) i--
            if (i == 0) dialogBinding.buttonPre.invisible()
            dialogBinding.textMessage.text = msg[i]
            dialogBinding.playGif.loadDrawable(gifs[i])
        }
        dialogBinding.buttonNext.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            i++
            if (!isFirstRun && i == 4) i++
            if (i == 1) dialogBinding.buttonPre.show()
            if (i == 5) {
                runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
                finish?.invoke()
            } else {
                dialogBinding.textMessage.text = msg[i]
                dialogBinding.playGif.loadDrawable(gifs[i])
            }
        }
        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        runCatching { alertDialog?.show() }
    }
    fun getIdNm(idNm: String): List<String?> {
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
        val aClass: Class<out GradientDrawable> = bg.javaClass
        try {
            @SuppressLint("DiscouragedPrivateApi") val mFillPaint =
                aClass.getDeclaredField("mFillPaint")
            mFillPaint.isAccessible = true
            val strokePaint = mFillPaint[bg] as Paint
            color = Objects.requireNonNull(strokePaint).color
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return color
    }

    companion object {
        var clickCount = 0
        var scoreRed = 0
        var scoreBlue = 0
        var bestScore = 9999
        lateinit var top: String
        lateinit var left: String
        var nm1 = "AI"
        var nm2 = "Blue"
        var one = true
        var flag = true
        var isGameOver = true
    }
}