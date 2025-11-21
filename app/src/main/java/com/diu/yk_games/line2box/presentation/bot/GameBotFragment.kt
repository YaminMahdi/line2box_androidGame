package com.diu.yk_games.line2box.presentation.bot

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
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
import com.diu.yk_games.line2box.util.invisible
import com.diu.yk_games.line2box.util.isMuted
import com.diu.yk_games.line2box.util.isNotMuted
import com.diu.yk_games.line2box.util.loadDrawable
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
    private var tmpLineId = 0

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
                        clickEnabled = true
                        performClick(binding.root.findViewById(randLineId))
                    }
                }
            } else {
                delay(600)
                clickEnabled = true
                performClick(binding.root.findViewById(randLineId))
            }
        }
    }

    fun setupListener() {
        viewModel.getLineViewGroups(binding).forEach { viewGroup ->
            repeat(viewGroup.childCount) { i ->
                viewGroup.getChildAt(i)?.setOnClickListener(::performClick)
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
    fun performClick(view: View) {
        val idNm = resources.getResourceEntryName(view.id)
        val aroundIds = getIdNm(idNm)
        val bg = view.background.mutate() as GradientDrawable
        val color = getColorGrad(bg)
        var change = false
        if (color == whiteX && clickEnabled && lineIDs.isNotEmpty()) {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.line_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            lineIDs.remove(idNm)
            clickCount++
            bg.setColor(if (clickCount % 2 == 1) redX else blueX)

            if (idNm[1].digitToInt() > 1 && idNm[4] == 'T' || idNm[3].digitToInt() > 1 && idNm[4] == 'L') {
                val idTopU =
                    resources.getIdentifier(aroundIds[0], "id", parentActivity.packageName)
                val idTopL =
                    resources.getIdentifier(aroundIds[1], "id", parentActivity.packageName)
                val idTopR =
                    resources.getIdentifier(aroundIds[2], "id", parentActivity.packageName)
                val lineU = binding.root.findViewById<View>(idTopU)
                val lineL = binding.root.findViewById<View>(idTopL)
                val lineR = binding.root.findViewById<View>(idTopR)
                val bgTopU = lineU.background.mutate() as GradientDrawable
                val bgTopL = lineL.background.mutate() as GradientDrawable
                val bgTopR = lineR.background.mutate() as GradientDrawable
                if ((getColorGrad(bgTopU) == redX || getColorGrad(bgTopU) == blueX) &&
                    (getColorGrad(bgTopL) == redX || getColorGrad(bgTopL) == blueX) &&
                    (getColorGrad(bgTopR) == redX || getColorGrad(bgTopR) == blueX)
                ) {
                    val txtId =
                        resources.getIdentifier(aroundIds[6], "id", parentActivity.packageName)
                    val idMidC1 =
                        resources.getIdentifier(aroundIds[8], "id", parentActivity.packageName)
                    val idMidC2 =
                        resources.getIdentifier(aroundIds[9], "id", parentActivity.packageName)
                    val idUpC1 =
                        resources.getIdentifier(aroundIds[10], "id", parentActivity.packageName)
                    val idUpC2 =
                        resources.getIdentifier(aroundIds[11], "id", parentActivity.packageName)
                    val crMid1 = binding.root.findViewById<View>(idMidC1)
                    val crMid2 = binding.root.findViewById<View>(idMidC2)
                    val crUp1 = binding.root.findViewById<View>(idUpC1)
                    val crUp2 = binding.root.findViewById<View>(idUpC2)
                    val bgMidC1 = crMid1.background.mutate() as GradientDrawable
                    val bgMidC2 = crMid2.background.mutate() as GradientDrawable
                    val bgUpC1 = crUp1.background.mutate() as GradientDrawable
                    val bgUpC2 = crUp2.background.mutate() as GradientDrawable
                    val txt = binding.root.findViewById<TextView>(txtId)
                    if (clickCount % 2 == 1) {
                        isNotMuted {
                            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.box_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        scoreRed++
                        binding.scoreRed.text = scoreRed.toString()
                        txt.text = nm1.first().toString()
                        txt.typeface = ResourcesCompat.getFont(parentActivity, R.font.bertram)

                        bgTopU.setColor(redX)
                        bgTopL.setColor(redX)
                        bgTopR.setColor(redX)

                        bgMidC1.setColor(redX)
                        bgMidC1.setStroke(14, redY)
                        bgMidC2.setColor(redX)
                        bgMidC2.setStroke(14, redY)

                        bgUpC1.setColor(redX)
                        bgUpC1.setStroke(14, redY)
                        bgUpC2.setColor(redX)
                        bgUpC2.setStroke(14, redY)
                    } else {
                        isNotMuted {
                            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.box_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        scoreBlue++
                        binding.scoreBlue.text = scoreBlue.toString()
                        txt.text = nm2.first().toString()
                        txt.typeface = ResourcesCompat.getFont(parentActivity, R.font.bertram)
                        bgTopU.setColor(blueX)
                        bgTopL.setColor(blueX)
                        bgTopR.setColor(blueX)

                        bgMidC1.setColor(blueX)
                        bgMidC1.setStroke(14, blueY)
                        bgMidC2.setColor(blueX)
                        bgMidC2.setStroke(14, blueY)

                        bgUpC1.setColor(blueX)
                        bgUpC1.setStroke(14, blueY)
                        bgUpC2.setColor(blueX)
                        bgUpC2.setStroke(14, blueY)
                        if (one) {
                            one = false
                            toast("Bonus TURN for you")
                        }
                    }
                    change = true
                }
            }
            if ((idNm[1].digitToInt() < 7 && idNm[4] == 'T' || idNm[3].digitToInt() < 7) && idNm[4] == 'L') {
                val idDownU =
                    resources.getIdentifier(aroundIds[3], "id", parentActivity.packageName)
                val idDownL =
                    resources.getIdentifier(aroundIds[4], "id", parentActivity.packageName)
                val idDownR =
                    resources.getIdentifier(aroundIds[5], "id", parentActivity.packageName)
                val lineDownU = binding.root.findViewById<View>(idDownU)
                val lineDownL = binding.root.findViewById<View>(idDownL)
                val lineDownR = binding.root.findViewById<View>(idDownR)
                val bgDownU = lineDownU.background.mutate() as GradientDrawable
                val bgDownL = lineDownL.background.mutate() as GradientDrawable
                val bgDownR = lineDownR.background.mutate() as GradientDrawable
                if ((getColorGrad(bgDownU) == redX || getColorGrad(bgDownU) == blueX) &&
                    (getColorGrad(bgDownL) == redX || getColorGrad(bgDownL) == blueX) &&
                    (getColorGrad(bgDownR) == redX || getColorGrad(bgDownR) == blueX)
                ) {
                    val txtId =
                        resources.getIdentifier(aroundIds[7], "id", parentActivity.packageName)
                    val idMidC1 =
                        resources.getIdentifier(aroundIds[8], "id", parentActivity.packageName)
                    val idMidC2 =
                        resources.getIdentifier(aroundIds[9], "id", parentActivity.packageName)
                    val idDownC1 =
                        resources.getIdentifier(aroundIds[12], "id", parentActivity.packageName)
                    val idDownC2 =
                        resources.getIdentifier(aroundIds[13], "id", parentActivity.packageName)
                    val crMid1 = binding.root.findViewById<View>(idMidC1)
                    val crMid2 = binding.root.findViewById<View>(idMidC2)
                    val crDown1 = binding.root.findViewById<View>(idDownC1)
                    val crDown2 = binding.root.findViewById<View>(idDownC2)

                    val bgMidC1 = crMid1.background.mutate() as GradientDrawable
                    val bgMidC2 = crMid2.background.mutate() as GradientDrawable
                    val bgDownC1 = crDown1.background.mutate() as GradientDrawable
                    val bgDownC2 = crDown2.background.mutate() as GradientDrawable
                    val txt = binding.root.findViewById<TextView>(txtId)
                    if (clickCount % 2 == 1) {
                        isNotMuted {
                            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.box_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        scoreRed++
                        binding.scoreRed.text = "" + scoreRed
                        txt.text = nm1.first().toString()
                        txt.typeface = ResourcesCompat.getFont(parentActivity, R.font.bertram)
                        bgDownU.setColor(redX)
                        bgDownL.setColor(redX)
                        bgDownR.setColor(redX)

                        bgMidC1.setColor(redX)
                        bgMidC1.setStroke(14, redY)
                        bgMidC2.setColor(redX)
                        bgMidC2.setStroke(14, redY)

                        bgDownC1.setColor(redX)
                        bgDownC1.setStroke(14, redY)
                        bgDownC2.setColor(redX)
                        bgDownC2.setStroke(14, redY)
                    } else {
                        isNotMuted {
                            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.box_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        scoreBlue++
                        binding.scoreBlue.text = scoreBlue.toString()
                        txt.text = "" + nm2[0]
                        txt.typeface = ResourcesCompat.getFont(parentActivity, R.font.bertram)
                        bgDownU.setColor(blueX)
                        bgDownL.setColor(blueX)
                        bgDownR.setColor(blueX)

                        bgMidC1.setColor(blueX)
                        bgMidC1.setStroke(14, blueY)
                        bgMidC2.setColor(blueX)
                        bgMidC2.setStroke(14, blueY)

                        bgDownC1.setColor(blueX)
                        bgDownC1.setStroke(14, blueY)
                        bgDownC2.setColor(blueX)
                        bgDownC2.setStroke(14, blueY)
                        if (one) {
                            one = false
                            toast("Bonus TURN for you")
                        }
                    }
                    change = true
                }
            }
            if (change) {
                clickCount--
                recursion = false
            } else {
                if (clickCount % 2 == 1) {
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
            if (clickCount % 2 == 1) {
                clickEnabled = true
            } else if (lineIDs.isNotEmpty()) {
                clickEnabled = false
                var countColored = 0
                var blankIndex = -69
                var extraTurn = false
                if (idNm[1].digitToInt() > 1 && idNm[4] == 'T' || idNm[3].digitToInt() > 1 && idNm[4] == 'L') {
                    Log.d("TAG", "lineClick: AI in top half")
                    val idTopU =
                        resources.getIdentifier(aroundIds[0], "id", parentActivity.packageName)
                    val idTopL =
                        resources.getIdentifier(aroundIds[1], "id", parentActivity.packageName)
                    val idTopR =
                        resources.getIdentifier(aroundIds[2], "id", parentActivity.packageName)
                    val lineU = binding.root.findViewById<View>(idTopU)
                    val lineL = binding.root.findViewById<View>(idTopL)
                    val lineR = binding.root.findViewById<View>(idTopR)
                    val bgTopU = lineU.background.mutate() as GradientDrawable
                    val bgTopL = lineL.background.mutate() as GradientDrawable
                    val bgTopR = lineR.background.mutate() as GradientDrawable
                    if (getColorGrad(bgTopU) != whiteX) countColored++ else blankIndex = 0
                    if (getColorGrad(bgTopL) != whiteX) countColored++ else blankIndex = 1
                    if (getColorGrad(bgTopR) != whiteX) countColored++ else blankIndex = 2
                    if (countColored == 2) {
                        Log.d("TAG", "lineClick: AI in top half countColored")
                        val lineId = resources.getIdentifier(
                            aroundIds[blankIndex],
                            "id",
                            parentActivity.packageName
                        )
                        lifecycleScope.launch {
                            delay(500)
                            clickEnabled = true
                            recursion = true
                            performClick(binding.root.findViewById(lineId))
                            tmpLineId = lineId
                            recursion = false
                        }
                        extraTurn = true
                    }
                }
                countColored = 0
                blankIndex = -69
                if (idNm[1].digitToInt() < 7 && idNm[4] == 'T' || idNm[3].digitToInt() < 7 && idNm[4] == 'L') {
                    Log.d("TAG", "lineClick: AI in dwn half")
                    val idDownU =
                        resources.getIdentifier(aroundIds[3], "id", parentActivity.packageName)
                    val idDownL =
                        resources.getIdentifier(aroundIds[4], "id", parentActivity.packageName)
                    val idDownR =
                        resources.getIdentifier(aroundIds[5], "id", parentActivity.packageName)
                    val lineDownU = binding.root.findViewById<View>(idDownU)
                    val lineDownL = binding.root.findViewById<View>(idDownL)
                    val lineDownR = binding.root.findViewById<View>(idDownR)
                    val bgDownU = lineDownU.background.mutate() as GradientDrawable
                    val bgDownL = lineDownL.background.mutate() as GradientDrawable
                    val bgDownR = lineDownR.background.mutate() as GradientDrawable
                    if (getColorGrad(bgDownU) != whiteX) countColored++ else blankIndex = 3
                    if (getColorGrad(bgDownL) != whiteX) countColored++ else blankIndex = 4
                    if (getColorGrad(bgDownR) != whiteX) countColored++ else blankIndex = 5
                    if (countColored == 2) {
                        val lineId = resources.getIdentifier(aroundIds[blankIndex], "id", parentActivity.packageName)
                        Log.d("TAG", "lineClick: AI in dwn half countColored")
                        lifecycleScope.launch {
                            delay(650)
                            if (tmpLineId != lineId) {
                                clickEnabled = true
                                recursion = true
                                performClick(binding.root.findViewById(lineId))
                                recursion = false
                            }
                        }
                        extraTurn = true
                    }
                }
                Log.d("TAG", "lineClick: lineIDs.size: ${lineIDs.size}")
                if (!extraTurn && !recursion) {
                    Log.d("TAG", "lineClick: AI in random")
                    //recursion=false;
                    var ind: Int
                    var countColoredUp = 0
                    var countColoredDn = 0
                    var randLineIdNm: String?
                    val lineIdTemp = ArrayList(lineIDs)
                    ind = random.nextInt(lineIDs.size)
                    randLineIdNm = lineIDs[ind]
                    while (true) {
                        Log.d("TAG", "lineClick: AI in random loop")
                        if (randLineIdNm!![1].digitToInt() > 1 && randLineIdNm[4] == 'T' || randLineIdNm[3].digitToInt() > 1 && randLineIdNm[4] == 'L') {
                            val idTopU = resources.getIdentifier(
                                getIdNm(randLineIdNm)[0],
                                "id",
                                parentActivity.packageName
                            )
                            val idTopL = resources.getIdentifier(
                                getIdNm(randLineIdNm)[1],
                                "id",
                                parentActivity.packageName
                            )
                            val idTopR = resources.getIdentifier(
                                getIdNm(randLineIdNm)[2],
                                "id",
                                parentActivity.packageName
                            )
                            val lineU = binding.root.findViewById<View>(idTopU)
                            val lineL = binding.root.findViewById<View>(idTopL)
                            val lineR = binding.root.findViewById<View>(idTopR)
                            val bgTopU = lineU.background.mutate() as GradientDrawable
                            val bgTopL = lineL.background.mutate() as GradientDrawable
                            val bgTopR = lineR.background.mutate() as GradientDrawable
                            if (getColorGrad(bgTopU) != whiteX) countColoredUp++
                            if (getColorGrad(bgTopL) != whiteX) countColoredUp++
                            if (getColorGrad(bgTopR) != whiteX) countColoredUp++
                        }
                        if ((randLineIdNm[1].digitToInt() < 7 && randLineIdNm[4] == 'T' || randLineIdNm[3].digitToInt() < 7) && randLineIdNm[4] == 'L') {
                            val idDownU = resources.getIdentifier(
                                getIdNm(randLineIdNm)[3],
                                "id",
                                parentActivity.packageName
                            )
                            val idDownL = resources.getIdentifier(
                                getIdNm(randLineIdNm)[4],
                                "id",
                                parentActivity.packageName
                            )
                            val idDownR = resources.getIdentifier(
                                getIdNm(randLineIdNm)[5],
                                "id",
                                parentActivity.packageName
                            )
                            val lineDownU = binding.root.findViewById<View>(idDownU)
                            val lineDownL = binding.root.findViewById<View>(idDownL)
                            val lineDownR = binding.root.findViewById<View>(idDownR)
                            val bgDownU = lineDownU.background.mutate() as GradientDrawable
                            val bgDownL = lineDownL.background.mutate() as GradientDrawable
                            val bgDownR = lineDownR.background.mutate() as GradientDrawable
                            if (getColorGrad(bgDownU) != whiteX) countColoredDn++
                            if (getColorGrad(bgDownL) != whiteX) countColoredDn++
                            if (getColorGrad(bgDownR) != whiteX) countColoredDn++
                        }
                        if ((countColoredUp > 1 || countColoredDn > 1) && !(countColoredUp == 3 || countColoredDn == 3)) {
                            Log.d(TAG, "lineClick: AI in random countColored. clk cnt: $clickCount")
                            if (lineIdTemp.size == 1) {
                                randLineIdNm = lineIdTemp[0]
                                break
                            }
                            lineIdTemp.removeAt(ind)
                            ind = random.nextInt(lineIdTemp.size)
                            randLineIdNm = lineIdTemp[ind]
                            countColoredUp = 0
                            countColoredDn = 0
                        } else break
                    }

                    Log.d(TAG, "AiLineClick: $randLineIdNm Up- $countColoredUp Dn- $countColoredDn")
                    val lineId =
                        resources.getIdentifier(randLineIdNm, "id", parentActivity.packageName)
                    lifecycleScope.launch {
                        delay(800)
                        clickEnabled = true
                        performClick(binding.root.findViewById(lineId))
                    }
                }
            }
            lifecycleScope.launch {
                if (scoreRed + scoreBlue == 36) {
                    delay(950)
                    isNotMuted {
                        val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.win_ef)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                    }
                    binding.red.textSize = 30f
                    binding.red.setTextColor(white)
                    binding.blue.textSize = 30f
                    binding.blue.setTextColor(white)
                    if (scoreRed > scoreBlue) onGameOver("AI won the match.")
                    else if (scoreRed < scoreBlue) {
                        var winAI = pref.read("winAI", 0)
                        pref.save("winAI", ++winAI)
                        onGameOver("You won the match.")
                    }else onGameOver("Match Draw.")
                }
            }
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
        private const val TAG = "GameBotFragment"
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
    }
}