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
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.ActivityGame3Binding
import com.diu.yk_games.line2box.databinding.DialogLayoutAlertBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutInfoBinding
import com.diu.yk_games.line2box.presentation.main.StartActivity
import com.diu.yk_games.line2box.util.applyState
import com.diu.yk_games.line2box.util.hideSystemBars
import com.diu.yk_games.line2box.util.invisible
import com.diu.yk_games.line2box.util.isMuted
import com.diu.yk_games.line2box.util.isNotMuted
import com.diu.yk_games.line2box.util.loadDrawable
import com.diu.yk_games.line2box.util.performOnClick
import com.diu.yk_games.line2box.util.pref
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.diu.yk_games.line2box.util.show
import com.diu.yk_games.line2box.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Objects
import java.util.Random

class BotActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGame3Binding

    private val lineIDs =
        mutableListOf(
            "r1c1T", "r1c1L", "r1c2T", "r1c2L", "r1c3T", "r1c3L", "r1c4T", "r1c4L", "r1c5T", "r1c5L", "r1c6T", "r1c6L", "r1c7L", "r2c1T", "r2c1L", "r2c2T", "r2c2L", "r2c3T", "r2c3L",
            "r2c4T", "r2c4L", "r2c5T", "r2c5L", "r2c6T", "r2c6L", "r2c7L", "r3c1T", "r3c1L", "r3c2T", "r3c2L", "r3c3T", "r3c3L", "r3c4T", "r3c4L", "r3c5T", "r3c5L", "r3c6T", "r3c6L", 
            "r3c7L", "r4c1T", "r4c1L", "r4c2T", "r4c2L", "r4c3T", "r4c3L", "r4c4T", "r4c4L", "r4c5T", "r4c5L", "r4c6T", "r4c6L", "r4c7L", "r5c1T", "r5c1L", "r5c2T", "r5c2L", "r5c3T", 
            "r5c3L", "r5c4T", "r5c4L", "r5c5T", "r5c5L", "r5c6T", "r5c6L", "r5c7L", "r7c1T", "r6c1T", "r6c1L", "r7c6T", "r6c2T", "r6c2L", "r7c2T", "r6c3T", "r6c3L", "r7c3T", "r6c4T", 
            "r6c4L", "r7c4T", "r6c5T", "r6c5L", "r7c5T", "r6c6T", "r6c6L", "r6c7L"
        )
    private var random = Random()

    //MediaPlayer lineClick, boxPlus, winSoundEf, btnClick;
    private var isFirstRun = false
    private var recursion = false
    private var clickEnabled = false
    private var tmpLineId = 0

    private fun ifMuted() {
        lifecycleScope.launch {
            binding.volBtn.applyState(isMuted())
        }
    }

    @SuppressLint("DiscouragedApi", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGame3Binding.inflate(layoutInflater)
        setContentView(binding.root)
        window.hideSystemBars()
        clickCount = 0
        scoreRed = 0
        scoreBlue = 0
        bestScore = 9999

        ifMuted()
        isFirstRun = pref.read("firstRun", true)

        binding.volBtn.performOnClick()
        binding.ideaBtn.setBounceClickListener {
            ideaBtn()
        }
        binding.homeBtn.setBounceClickListener {
            backBtn()
        }
        onBackPressedDispatcher.addCallback(this) {
            val builder = AlertDialog.Builder(this@BotActivity)
            val dialogBinding =
                DialogLayoutAlertBinding.inflate(LayoutInflater.from(this@BotActivity))
            builder.setView(dialogBinding.root)
            dialogBinding.textMessage.text = "Do you really want to QUIT the match?"
            dialogBinding.buttonYes.text = "YES"
            dialogBinding.buttonNo.text = "NO"
            val alertDialog = builder.create()
            dialogBinding.buttonYes.setBounceClickListener {
                isNotMuted {
                    val mediaPlayer = MediaPlayer.create(this@BotActivity, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
                runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
                scoreRed = 0
                scoreBlue = 0
                clickCount = 0
                flag = true
//                onBackPressedDispatcher.onBackPressed()
                //startActivity(Intent(this@GameActivity3, StartActivity::class.java))
                finish()
            }
            dialogBinding.buttonNo.setBounceClickListener {
                isNotMuted {
                    val mediaPlayer = MediaPlayer.create(this@BotActivity, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
                runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
            }
            alertDialog.window?.setBackgroundDrawable(0.toDrawable())
            runCatching { alertDialog.show() }
            
        }

        val index = StringBuilder()
        for (i in 1..6) {
            index.setLength(0)
            index.append(fst)
            index.append('T')
            index.deleteCharAt(1)
            index.insert(1, i)
            top = index.toString()
            index.deleteCharAt(4)
            index.append('L')
            left = index.toString()
            index.deleteCharAt(4)
            index.insert(3, 'r')
            circle = index.toString()
            for (j in 1..6) {
                var idTop = resources.getIdentifier(top, "id", packageName)
                var idLeft = resources.getIdentifier(left, "id", packageName)
                var idCircle = resources.getIdentifier(circle, "id", packageName)
                var lineTop = findViewById<View>(idTop)
                var lineLeft = findViewById<View>(idLeft)
                var lineCircle = findViewById<View>(idCircle)
                var bgTop = lineTop.background as GradientDrawable
                var bgLeft = lineLeft.background as GradientDrawable
                var bgCircle = lineCircle.background as GradientDrawable
                bgTop.setColor(ContextCompat.getColor(applicationContext, R.color.whiteX))
                bgLeft.setColor(ContextCompat.getColor(applicationContext, R.color.whiteX))
                bgCircle.setColor(ContextCompat.getColor(applicationContext, R.color.white))
                bgCircle.setStroke(14, ContextCompat.getColor(applicationContext, R.color.whiteY))
                if (i == 6) {
                    index.setLength(0)
                    index.append(top)
                    index.deleteCharAt(1)
                    index.insert(1, i + 1)
                    idTop = resources.getIdentifier(index.toString(), "id", packageName)
                    index.setLength(0)
                    index.append(circle)
                    index.deleteCharAt(1)
                    index.insert(1, i + 1)
                    idCircle =
                        resources.getIdentifier(index.toString(), "id", packageName)
                    lineTop = findViewById(idTop)
                    lineCircle = findViewById(idCircle)
                    bgTop = lineTop.background as GradientDrawable
                    bgCircle = lineCircle.background as GradientDrawable
                    bgTop.setColor(ContextCompat.getColor(applicationContext, R.color.whiteX))
                    bgCircle.setColor(ContextCompat.getColor(applicationContext, R.color.white))
                    bgCircle.setStroke(14, ContextCompat.getColor(applicationContext, R.color.whiteY))
                    //Log.d("TAG", "onDestroy: " + top + " " + circle)
                }
                index.setLength(0)
                index.append(top)
                index.deleteCharAt(3)
                index.insert(3, j + 1)
                top = index.toString()
                index.setLength(0)
                index.append(left)
                index.deleteCharAt(3)
                index.insert(3, j + 1)
                left = index.toString()
                index.setLength(0)
                index.append(circle)
                index.deleteCharAt(4)
                index.insert(4, j + 1)
                circle = index.toString()
                if (j == 6) {
                    index.setLength(0)
                    index.append(left)
                    index.deleteCharAt(3)
                    index.insert(3, j + 1)
                    left = index.toString()
                    idLeft = resources.getIdentifier(left, "id", packageName)
                    index.setLength(0)
                    index.append(circle)
                    index.deleteCharAt(4)
                    index.insert(4, j + 1)
                    circle = index.toString()
                    idCircle = resources.getIdentifier(circle, "id", packageName)
                    lineLeft = findViewById(idLeft)
                    lineCircle = findViewById(idCircle)
                    bgLeft = lineLeft.background as GradientDrawable
                    bgCircle = lineCircle.background as GradientDrawable
                    bgLeft.setColor(ContextCompat.getColor(applicationContext, R.color.whiteX))
                    bgCircle.setColor(ContextCompat.getColor(applicationContext, R.color.white))
                    bgCircle.setStroke(
                        14,
                        ContextCompat.getColor(applicationContext, R.color.whiteY)
                    )
                    if (i == 6) {
                        index.setLength(0)
                        index.append(circle)
                        index.deleteCharAt(1)
                        index.insert(1, i + 1)
                        circle = index.toString()
                        idCircle = resources.getIdentifier(circle, "id", packageName)
                        lineCircle = findViewById(idCircle)
                        bgCircle = lineCircle.background as GradientDrawable
                        bgCircle.setColor(ContextCompat.getColor(applicationContext, R.color.white))
                        bgCircle.setStroke(
                            14,
                            ContextCompat.getColor(applicationContext, R.color.whiteY)
                        )
                    }
                }
            }
        }
        val ind = random.nextInt(84)
        val randLineId = resources.getIdentifier(lineIDs[ind], "id", packageName)
        lifecycleScope.launch {
            if (isFirstRun) {
                delay(200)
                infoShow {
                    lifecycleScope.launch {
                        delay(600)
                        clickEnabled = true
                        performClick(findViewById(randLineId))
                    }
                }
            } else {
                delay(600)
                clickEnabled = true
                performClick(findViewById(randLineId))
            }
        }
    }

    fun lineClick(view: View) {
        lifecycleScope.launch{
            performClick(view)
        }
    }

    @SuppressLint("SetTextI18n", "DiscouragedApi")
    suspend fun performClick(view: View) {
        idNm = resources.getResourceEntryName(view.id)
        val bg = view.background as GradientDrawable
        val color = getColorGrad(bg)
        var change = false
        val red = resources.getColor(R.color.redX, theme)
        val blue = resources.getColor(R.color.blueX, theme)
        val white = resources.getColor(R.color.whiteX, theme)
        if (color == white && clickEnabled && lineIDs.isNotEmpty()) {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(this, R.raw.line_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            lineIDs.remove(idNm)
            clickCount++
            if (clickCount % 2 == 1) {
                bg.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
            } else {
                bg.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
            }

            if (Character.getNumericValue(idNm[1]) > 1 && idNm[4] == 'T' || Character.getNumericValue(
                    idNm[3]
                ) > 1 && idNm[4] == 'L'
            ) {
                val idTopU = resources.getIdentifier(getIdNm(idNm)[0], "id", packageName)
                val idTopL = resources.getIdentifier(getIdNm(idNm)[1], "id", packageName)
                val idTopR = resources.getIdentifier(getIdNm(idNm)[2], "id", packageName)
                val lineU = findViewById<View>(idTopU)
                val lineL = findViewById<View>(idTopL)
                val lineR = findViewById<View>(idTopR)
                val bgTopU = lineU.background as GradientDrawable
                val bgTopL = lineL.background as GradientDrawable
                val bgTopR = lineR.background as GradientDrawable
                if ((getColorGrad(bgTopU) == red || getColorGrad(bgTopU) == blue) &&
                    (getColorGrad(bgTopL) == red || getColorGrad(bgTopL) == blue) &&
                    (getColorGrad(bgTopR) == red || getColorGrad(bgTopR) == blue)
                ) {
                    val txtId = resources.getIdentifier(getIdNm(idNm)[6], "id", packageName)
                    val idMidC1 = resources.getIdentifier(getIdNm(idNm)[8], "id", packageName)
                    val idMidC2 = resources.getIdentifier(getIdNm(idNm)[9], "id", packageName)
                    val idUpC1 = resources.getIdentifier(getIdNm(idNm)[10], "id", packageName)
                    val idUpC2 = resources.getIdentifier(getIdNm(idNm)[11], "id", packageName)
                    val crMid1 = findViewById<View>(idMidC1)
                    val crMid2 = findViewById<View>(idMidC2)
                    val crUp1 = findViewById<View>(idUpC1)
                    val crUp2 = findViewById<View>(idUpC2)
                    val bgMidC1 = crMid1.background as GradientDrawable
                    val bgMidC2 = crMid2.background as GradientDrawable
                    val bgUpC1 = crUp1.background as GradientDrawable
                    val bgUpC2 = crUp2.background as GradientDrawable
                    val txt = findViewById<TextView>(txtId)
                    if (clickCount % 2 == 1) {
                        isNotMuted {
                            val mediaPlayer = MediaPlayer.create(this, R.raw.box_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        scoreRed++
                        binding.scoreRed.text = "" + scoreRed
                        txt.text = "" + nm1[0]
                        txt.typeface = ResourcesCompat.getFont(applicationContext, R.font.bertram)

                        bgTopU.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgTopL.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgTopR.setColor(ContextCompat.getColor(applicationContext, R.color.redX))

                        bgMidC1.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgMidC1.setStroke(14, ContextCompat.getColor(applicationContext, R.color.redY))
                        bgMidC2.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgMidC2.setStroke(14, ContextCompat.getColor(applicationContext, R.color.redY))

                        bgUpC1.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgUpC1.setStroke(14, ContextCompat.getColor(applicationContext, R.color.redY))
                        bgUpC2.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgUpC2.setStroke(14, ContextCompat.getColor(applicationContext, R.color.redY))
                    } else {
                        isNotMuted {
                            val mediaPlayer = MediaPlayer.create(this, R.raw.box_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        scoreBlue++
                        binding.scoreBlue.text = scoreBlue.toString()
                        txt.text = "" + nm2[0]
                        txt.typeface = ResourcesCompat.getFont(applicationContext, R.font.bertram)
                        bgTopU.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgTopL.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgTopR.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))

                        bgMidC1.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgMidC1.setStroke(14, ContextCompat.getColor(applicationContext, R.color.blueY))
                        bgMidC2.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgMidC2.setStroke(14, ContextCompat.getColor(applicationContext, R.color.blueY))

                        bgUpC1.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgUpC1.setStroke(14, ContextCompat.getColor(applicationContext, R.color.blueY))
                        bgUpC2.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgUpC2.setStroke(14, ContextCompat.getColor(applicationContext, R.color.blueY))
                        if (one) {
                            one = false
                            toast("Bonus TURN for you")
                        }
                    }
                    change = true
                }
            }
            if ((Character.getNumericValue(idNm[1]) < 7 && idNm[4] == 'T' || Character.getNumericValue(idNm[3]) < 7) && idNm[4] == 'L'
            ) {
                val idDownU = resources.getIdentifier(getIdNm(idNm)[3], "id", packageName)
                val idDownL = resources.getIdentifier(getIdNm(idNm)[4], "id", packageName)
                val idDownR = resources.getIdentifier(getIdNm(idNm)[5], "id", packageName)
                val lineDownU = findViewById<View>(idDownU)
                val lineDownL = findViewById<View>(idDownL)
                val lineDownR = findViewById<View>(idDownR)
                val bgDownU = lineDownU.background as GradientDrawable
                val bgDownL = lineDownL.background as GradientDrawable
                val bgDownR = lineDownR.background as GradientDrawable
                if ((getColorGrad(bgDownU) == red || getColorGrad(bgDownU) == blue) &&
                    (getColorGrad(bgDownL) == red || getColorGrad(bgDownL) == blue) &&
                    (getColorGrad(bgDownR) == red || getColorGrad(bgDownR) == blue)
                ) {
                    val txtId = resources.getIdentifier(getIdNm(idNm)[7], "id", packageName)
                    val idMidC1 = resources.getIdentifier(getIdNm(idNm)[8], "id", packageName)
                    val idMidC2 = resources.getIdentifier(getIdNm(idNm)[9], "id", packageName)
                    val idDownC1 = resources.getIdentifier(getIdNm(idNm)[12], "id", packageName)
                    val idDownC2 = resources.getIdentifier(getIdNm(idNm)[13], "id", packageName)
                    val crMid1 = findViewById<View>(idMidC1)
                    val crMid2 = findViewById<View>(idMidC2)
                    val crDown1 = findViewById<View>(idDownC1)
                    val crDown2 = findViewById<View>(idDownC2)

                    val bgMidC1 = crMid1.background as GradientDrawable
                    val bgMidC2 = crMid2.background as GradientDrawable
                    val bgDownC1 = crDown1.background as GradientDrawable
                    val bgDownC2 = crDown2.background as GradientDrawable
                    val txt = findViewById<TextView>(txtId)
                    if (clickCount % 2 == 1) {
                        isNotMuted {
                            val mediaPlayer = MediaPlayer.create(this, R.raw.box_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        scoreRed++
                        binding.scoreRed.text = "" + scoreRed
                        txt.text = "" + nm1[0]
                        txt.typeface = ResourcesCompat.getFont(applicationContext, R.font.bertram)
                        bgDownU.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgDownL.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgDownR.setColor(ContextCompat.getColor(applicationContext, R.color.redX))

                        bgMidC1.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgMidC1.setStroke(14, ContextCompat.getColor(applicationContext, R.color.redY))
                        bgMidC2.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgMidC2.setStroke(14, ContextCompat.getColor(applicationContext, R.color.redY))

                        bgDownC1.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgDownC1.setStroke(14, ContextCompat.getColor(applicationContext, R.color.redY))
                        bgDownC2.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgDownC2.setStroke(14, ContextCompat.getColor(applicationContext, R.color.redY))
                    } else {
                        isNotMuted {
                            val mediaPlayer = MediaPlayer.create(this, R.raw.box_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        scoreBlue++
                        binding.scoreBlue.text = scoreBlue.toString()
                        txt.text = "" + nm2[0]
                        txt.typeface = ResourcesCompat.getFont(applicationContext, R.font.bertram)
                        bgDownU.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgDownL.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgDownR.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))

                        bgMidC1.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgMidC1.setStroke(14, ContextCompat.getColor(applicationContext, R.color.blueY))
                        bgMidC2.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgMidC2.setStroke(14, ContextCompat.getColor(applicationContext, R.color.blueY))

                        bgDownC1.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgDownC1.setStroke(14, ContextCompat.getColor(applicationContext, R.color.blueY))
                        bgDownC2.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgDownC2.setStroke(14, ContextCompat.getColor(applicationContext, R.color.blueY))
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
                    binding.red.setTextColor(resources.getColor(R.color.whiteT, theme))
                    binding.blue.textSize = 35f
                    binding.blue.setTextColor(resources.getColor(R.color.white, theme))
                } else {
                    binding.blue.textSize = 30f
                    binding.blue.setTextColor(resources.getColor(R.color.whiteT, theme))
                    binding.red.textSize = 35f
                    binding.red.setTextColor(resources.getColor(R.color.white, theme))
                }
            }
            if (clickCount % 2 == 1) {
                clickEnabled = true
            } else if (lineIDs.isNotEmpty()) {
                clickEnabled = false
                var countColored = 0
                var blankIndex = -69
                var extraTurn = false
                if (Character.getNumericValue(idNm[1]) > 1 && idNm[4] == 'T' || Character.getNumericValue(
                        idNm[3]
                    ) > 1 && idNm[4] == 'L'
                ) {
                    Log.d("TAG", "lineClick: AI in top half")
                    val idTopU = resources.getIdentifier(getIdNm(idNm)[0], "id", packageName)
                    val idTopL = resources.getIdentifier(getIdNm(idNm)[1], "id", packageName)
                    val idTopR = resources.getIdentifier(getIdNm(idNm)[2], "id", packageName)
                    val lineU = findViewById<View>(idTopU)
                    val lineL = findViewById<View>(idTopL)
                    val lineR = findViewById<View>(idTopR)
                    val bgTopU = lineU.background as GradientDrawable
                    val bgTopL = lineL.background as GradientDrawable
                    val bgTopR = lineR.background as GradientDrawable
                    if (getColorGrad(bgTopU) != white) countColored++ else blankIndex = 0
                    if (getColorGrad(bgTopL) != white) countColored++ else blankIndex = 1
                    if (getColorGrad(bgTopR) != white) countColored++ else blankIndex = 2
                    if (countColored == 2) {
                        Log.d("TAG", "lineClick: AI in top half countColored")
                        val lineId = resources.getIdentifier(getIdNm(idNm)[blankIndex], "id", packageName)
                        lifecycleScope.launch(Dispatchers.Main){
                            delay(500)
                            clickEnabled = true
                            recursion = true
                            performClick(findViewById(lineId))
                            tmpLineId = lineId
                            recursion = false
                        }
                        extraTurn = true
                    }
                }
                countColored = 0
                blankIndex = -69
                if (Character.getNumericValue(idNm[1]) < 7 && idNm[4] == 'T' || Character.getNumericValue(
                        idNm[3]
                    ) < 7 && idNm[4] == 'L'
                ) {
                    Log.d("TAG", "lineClick: AI in dwn half")
                    val idDownU = resources.getIdentifier(getIdNm(idNm)[3], "id", packageName)
                    val idDownL = resources.getIdentifier(getIdNm(idNm)[4], "id", packageName)
                    val idDownR = resources.getIdentifier(getIdNm(idNm)[5], "id", packageName)
                    val lineDownU = findViewById<View>(idDownU)
                    val lineDownL = findViewById<View>(idDownL)
                    val lineDownR = findViewById<View>(idDownR)
                    val bgDownU = lineDownU.background as GradientDrawable
                    val bgDownL = lineDownL.background as GradientDrawable
                    val bgDownR = lineDownR.background as GradientDrawable
                    if (getColorGrad(bgDownU) != white) countColored++ else blankIndex = 3
                    if (getColorGrad(bgDownL) != white) countColored++ else blankIndex = 4
                    if (getColorGrad(bgDownR) != white) countColored++ else blankIndex = 5
                    if (countColored == 2) {
                        val lineId = resources.getIdentifier(getIdNm(idNm)[blankIndex], "id", packageName)
                        Log.d("TAG", "lineClick: AI in dwn half countColored")
                        delay(650)
                        if (tmpLineId != lineId) {
                            clickEnabled = true
                            recursion = true
                            performClick(findViewById(lineId))
                        }
                        recursion = false
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
                        if (Character.getNumericValue(randLineIdNm!![1]) > 1 && randLineIdNm[4] == 'T' ||
                            Character.getNumericValue(randLineIdNm[3]) > 1 && randLineIdNm[4] == 'L'
                        ) {
                            val idTopU = resources.getIdentifier(getIdNm(randLineIdNm)[0], "id", packageName)
                            val idTopL = resources.getIdentifier(getIdNm(randLineIdNm)[1], "id", packageName)
                            val idTopR = resources.getIdentifier(getIdNm(randLineIdNm)[2], "id", packageName)
                            val lineU = findViewById<View>(idTopU)
                            val lineL = findViewById<View>(idTopL)
                            val lineR = findViewById<View>(idTopR)
                            val bgTopU = lineU.background as GradientDrawable
                            val bgTopL = lineL.background as GradientDrawable
                            val bgTopR = lineR.background as GradientDrawable
                            if (getColorGrad(bgTopU) != white) countColoredUp++
                            if (getColorGrad(bgTopL) != white) countColoredUp++
                            if (getColorGrad(bgTopR) != white) countColoredUp++
                        }
                        if ((Character.getNumericValue(randLineIdNm[1]) < 7 && randLineIdNm[4] == 'T' || Character.getNumericValue(randLineIdNm[3]) < 7) && randLineIdNm[4] == 'L'
                        ) {
                            val idDownU = resources.getIdentifier(getIdNm(randLineIdNm)[3], "id", packageName)
                            val idDownL = resources.getIdentifier(getIdNm(randLineIdNm)[4], "id", packageName)
                            val idDownR = resources.getIdentifier(getIdNm(randLineIdNm)[5], "id", packageName)
                            val lineDownU = findViewById<View>(idDownU)
                            val lineDownL = findViewById<View>(idDownL)
                            val lineDownR = findViewById<View>(idDownR)
                            val bgDownU = lineDownU.background as GradientDrawable
                            val bgDownL = lineDownL.background as GradientDrawable
                            val bgDownR = lineDownR.background as GradientDrawable
                            if (getColorGrad(bgDownU) != white) countColoredDn++
                            if (getColorGrad(bgDownL) != white) countColoredDn++
                            if (getColorGrad(bgDownR) != white) countColoredDn++
                        }
                        if ((countColoredUp > 1 || countColoredDn > 1) && !(countColoredUp == 3 || countColoredDn == 3)) {
                            Log.d("TAG", "lineClick: AI in random countColored u d. clk cnt: $clickCount")
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

                    Log.d("TAG", "AiLineClick: $randLineIdNm Up- $countColoredUp Dn- $countColoredDn")
                    val lineId = resources.getIdentifier(randLineIdNm, "id", packageName)
                    delay(800)
                    clickEnabled = true
                    performClick(findViewById(lineId))
                }
            }
            if (scoreRed + scoreBlue == 36){
                delay(950)
                isNotMuted {
                    val mediaPlayer = MediaPlayer.create(this@BotActivity, R.raw.win_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
                binding.red.textSize = 30f
                binding.red.setTextColor(resources.getColor(R.color.white, theme))
                binding.blue.textSize = 30f
                binding.blue.setTextColor(resources.getColor(R.color.white, theme))
                if (scoreRed > scoreBlue) onGameOver("AI won the match.") else if (scoreRed < scoreBlue) onGameOver(
                    "You won the match."
                ) else onGameOver("Match Draw.")
            }
        }
    }


    @SuppressLint("SetTextI18n")
    fun onGameOver(winMsg: String) {
        val builder = AlertDialog.Builder(this@BotActivity)
        val dialogBinding = DialogLayoutAlertBinding.inflate(LayoutInflater.from(this@BotActivity))
        builder.setView(dialogBinding.root)
        //builder.setCancelable(false)
        dialogBinding.textMessage.text = "" + winMsg
        dialogBinding.buttonNo.text = "Exit"
        dialogBinding.buttonYes.text = "Retry!"
        val alertDialog = builder.create()
        dialogBinding.buttonYes.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
            startActivity(Intent(this@BotActivity, BotActivity::class.java))
            finish()
        }
        dialogBinding.buttonNo.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
            startActivity(Intent(this, StartActivity::class.java))
            finish()
            flag = true
        }
        if (alertDialog.window != null) {
            alertDialog.window!!.setBackgroundDrawable(0.toDrawable())
        }
        try { alertDialog?.show() }
        catch (npe: Exception) { npe.printStackTrace() }
    }

    fun ideaBtn() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        infoShow()
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
        val builder = AlertDialog.Builder(this@BotActivity)
        val dialogBinding = DialogLayoutInfoBinding.inflate(LayoutInflater.from(this@BotActivity))
        builder.setView(dialogBinding.root)
        builder.setCancelable(false)

        dialogBinding.textMessage.text = msg[0]
        dialogBinding.playGif.loadDrawable(gifs[0])
        dialogBinding.buttonPre.invisible()
        val alertDialog = builder.create()
        dialogBinding.buttonPre.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
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
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
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

        try { alertDialog?.show() }
        catch (npe: Exception) { npe.printStackTrace() }
    }

    private fun backBtn() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        onBackPressedDispatcher.onBackPressed()
    }

    companion object {
        var clickCount = 0
        var scoreRed = 0
        var scoreBlue = 0
        var bestScore = 9999
        lateinit var idNm: String
        var fst = "r1c1"
        lateinit var top: String
        lateinit var left: String
        lateinit var circle: String
        var nm1 = "AI"
        var nm2 = "Blue"
        var one = true
        var flag = true
        fun getIdNm(idNm: String): List<String?> {
            val idS = arrayOfNulls<String>(14)
            val id = StringBuilder()
            if (idNm[4] == 'T') {
                if (Character.getNumericValue(idNm[1]) > 1) {
                    //top up//
                    id.append(idNm)
                    id.deleteCharAt(1)
                    id.insert(1, Character.getNumericValue(idNm[1]) - 1)
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
                    id.insert(3, Character.getNumericValue(idNm[3]) + 1)
                    idS[2] = id.toString()
                    //circle up right
                    id.insert(3, 'r')
                    id.deleteCharAt(5)
                    idS[11] = id.toString()
                    //circle up left
                    id.deleteCharAt(4)
                    id.insert(4, Character.getNumericValue(idNm[3]))
                    idS[10] = id.toString()
                }
                if (Character.getNumericValue(idNm[1]) < 7) {
                    //down down//
                    id.setLength(0)
                    id.append(idNm)
                    id.deleteCharAt(1)
                    id.insert(1, Character.getNumericValue(idNm[1]) + 1)
                    idS[3] = id.toString()
                    //left down
                    id.setLength(0)
                    id.append(idNm)
                    id.deleteCharAt(4)
                    id.append('L')
                    idS[4] = id.toString()
                    //right down
                    id.deleteCharAt(3)
                    id.insert(3, Character.getNumericValue(idNm[3]) + 1)
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
                    id.insert(1, Character.getNumericValue(idNm[1]) + 1)
                    id.insert(3, 'r')
                    idS[13] = id.toString()
                    //circle Down right
                    id.deleteCharAt(4)
                    id.insert(4, Character.getNumericValue(idNm[3]) + 1)
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
                id.append(Character.getNumericValue(idNm[3]) + 1)
                idS[9] = id.toString()
            } else if (idNm[4] == 'L') {
                if (Character.getNumericValue(idNm[3]) > 1) {
                    //top up//
                    id.append(idNm)
                    id.deleteCharAt(3)
                    id.insert(3, Character.getNumericValue(idNm[3]) - 1)
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
                    id.insert(1, Character.getNumericValue(idNm[1]) + 1)
                    idS[1] = id.toString()
                    //circle up left
                    id.delete(4, 6)
                    id.insert(3, 'r')
                    idS[10] = id.toString()
                    //circle up right
                    id.deleteCharAt(1)
                    id.insert(1, Character.getNumericValue(idNm[1]))
                    idS[11] = id.toString()
                }
                if (Character.getNumericValue(idNm[3]) < 7) {
                    //down down//
                    id.setLength(0)
                    id.append(idNm)
                    id.deleteCharAt(3)
                    id.insert(3, Character.getNumericValue(idNm[3]) + 1)
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
                    id.insert(1, Character.getNumericValue(idNm[1]) + 1)
                    id.deleteCharAt(5)
                    idS[4] = id.toString()
                    //circle Down right
                    id.setLength(0)
                    id.append(idNm)
                    id.deleteCharAt(4)
                    id.deleteCharAt(3)
                    id.insert(3, Character.getNumericValue(idNm[3]) + 1)
                    id.insert(3, 'r')
                    idS[13] = id.toString()
                    //circle Down left
                    id.deleteCharAt(1)
                    id.insert(1, Character.getNumericValue(idNm[1]) + 1)
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
                id.insert(1, Character.getNumericValue(idNm[1]) + 1)
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
    }
}