package com.diu.yk_games.line2box.presentation.offline

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.SharedPreferences
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.ActivityGame1Binding
import com.diu.yk_games.line2box.model.DataStore
import com.diu.yk_games.line2box.util.hideSystemBars
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.droidsonroids.gif.GifImageView
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Objects
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


@Suppress("DEPRECATION")
class GameActivity1 : AppCompatActivity() {

    private lateinit var binding : ActivityGame1Binding
    private lateinit var scoreRedView: TextView
    private lateinit var scoreBlueView: TextView
    private lateinit var redTxt: TextView
    private lateinit var blueTxt: TextView
    private lateinit var nm1Txt: TextView
    private lateinit var nm2Txt: TextView

    //MediaPlayer lineClick, boxPlus, winSoundEf, btnClick;
    lateinit var sharedPref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private var isFirstRun = false
    @SuppressLint("SetTextI18n")
    fun onStopFragment() {
        findViewById<View>(R.id.relativeLayout).visibility = View.VISIBLE
        findViewById<View>(R.id.txtLayout).visibility = View.VISIBLE
        findViewById<View>(R.id.nmLayout).visibility = View.VISIBLE
        nm1Txt.text = "($nm1)"
        nm2Txt.text = "($nm2)"
        if (nm1 == "Red") nm1Txt.visibility = View.GONE
        if (nm2 == "Blue") nm2Txt.visibility = View.GONE
//        val handler = Handler()
//        handler.postDelayed({ if (isFirstRun) infoShow() }, 200)
        lifecycleScope.launch {
            delay(200)
            if (isFirstRun) infoShow()
        }
    }

    private val isMuted: Boolean
        get() = sharedPref.getBoolean("muted", false)

    private fun ifMuted() {
        if (isMuted) {
            findViewById<View>(R.id.volBtn).setBackgroundResource(R.drawable.btn_gry_bg)
            (findViewById<View>(R.id.volBtn) as ImageButton).setImageResource(R.drawable.icon_vol_mute)
        } else {
            findViewById<View>(R.id.volBtn).setBackgroundResource(R.drawable.btn_ylw_bg)
            (findViewById<View>(R.id.volBtn) as ImageButton).setImageResource(R.drawable.icon_vol_unmute)
        }
    }

    @SuppressLint("DiscouragedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.hideSystemBars()

        binding = ActivityGame1Binding.inflate(layoutInflater)
        setContentView(binding.root)
        PACKAGE_NAME = applicationContext.packageName
        scoreRedView = binding.scoreRed
        scoreBlueView = binding.scoreBlue
        redTxt = binding.red
        blueTxt = binding.blue
        nm1Txt = binding.nm1Id
        nm2Txt = binding.nm2Id
        clickCount = 0
        scoreRed = 0
        scoreBlue = 0
        bestScore = 9999
        one = true
        sharedPref = getSharedPreferences(
            getString(R.string.preference_file_key), MODE_PRIVATE
        )
        editor = sharedPref.edit()
        ifMuted()
        isFirstRun = sharedPref.getBoolean("firstRun", true)
        if (flag) {
            val fm = supportFragmentManager
            val ft = fm.beginTransaction()
            ft.replace(R.id.nmFragment, NameInfoFragment())
            ft.commit()
            binding.relativeLayout.visibility = View.INVISIBLE
            binding.txtLayout.visibility = View.GONE
            binding.nmLayout.visibility = View.INVISIBLE
            flag = false
        } else onStopFragment()
        val index = StringBuilder()
        for(i in 1..6) {
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
            for(j in 1..6) {
                var idTop = this.resources.getIdentifier(top, "id", this.packageName)
                var idLeft = this.resources.getIdentifier(left, "id", this.packageName)
                var idCircle = this.resources.getIdentifier(circle, "id", this.packageName)
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
                    idTop = this.resources.getIdentifier(index.toString(), "id", this.packageName)
                    index.setLength(0)
                    index.append(circle)
                    index.deleteCharAt(1)
                    index.insert(1, i + 1)
                    idCircle =
                        this.resources.getIdentifier(index.toString(), "id", this.packageName)
                    lineTop = findViewById(idTop)
                    lineCircle = findViewById(idCircle)
                    bgTop = lineTop.background as GradientDrawable
                    bgCircle = lineCircle.background as GradientDrawable
                    bgTop.setColor(ContextCompat.getColor(applicationContext, R.color.whiteX))
                    bgCircle.setColor(ContextCompat.getColor(applicationContext, R.color.white))
                    bgCircle.setStroke(14, ContextCompat.getColor(applicationContext, R.color.whiteY))
                    ////Log.d("TAG", "onDestroy: " + top + " " + circle);
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
                    idLeft = this.resources.getIdentifier(left, "id", this.packageName)
                    index.setLength(0)
                    index.append(circle)
                    index.deleteCharAt(4)
                    index.insert(4, j + 1)
                    circle = index.toString()
                    idCircle = this.resources.getIdentifier(circle, "id", this.packageName)
                    lineLeft = findViewById(idLeft)
                    lineCircle = findViewById(idCircle)
                    bgLeft = lineLeft.background as GradientDrawable
                    bgCircle = lineCircle.background as GradientDrawable
                    bgLeft.setColor(ContextCompat.getColor(applicationContext, R.color.whiteX))
                    bgCircle.setColor(ContextCompat.getColor(applicationContext, R.color.white))
                    bgCircle.setStroke(14, ContextCompat.getColor(applicationContext, R.color.whiteY))
                    if (i == 6) {
                        index.setLength(0)
                        index.append(circle)
                        index.deleteCharAt(1)
                        index.insert(1, i + 1)
                        circle = index.toString()
                        idCircle = this.resources.getIdentifier(circle, "id", this.packageName)
                        lineCircle = findViewById(idCircle)
                        bgCircle = lineCircle.background as GradientDrawable
                        bgCircle.setColor(ContextCompat.getColor(applicationContext, R.color.white))
                        bgCircle.setStroke(14, ContextCompat.getColor(applicationContext, R.color.whiteY))
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                val builder = AlertDialog.Builder(this@GameActivity1)
                val view = LayoutInflater.from(this@GameActivity1).inflate(
                    R.layout.dialog_layout_alert, findViewById(R.id.layoutDialog)
                )
                builder.setView(view)
                (view.findViewById<View>(R.id.textMessage) as TextView).text =
                    "Do you really want to QUIT the match?"
                (view.findViewById<View>(R.id.buttonYes) as Button).text = "YES"
                (view.findViewById<View>(R.id.buttonNo) as Button).text = "NO"
                val alertDialog = builder.create()
                view.findViewById<View>(R.id.buttonYes).setBounceClickListener {
                    if (!isMuted) {
                        val mediaPlayer = MediaPlayer.create(this@GameActivity1, R.raw.btn_click_ef)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                    }
                    alertDialog.dismiss()
                    flag = true
//            onBackPressedDispatcher.onBackPressed()
//            startActivity(Intent(this@GameActivity1, StartActivity::class.java))
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
                view.findViewById<View>(R.id.buttonNo).setBounceClickListener {
                    if (!isMuted) {
                        val mediaPlayer = MediaPlayer.create(this@GameActivity1, R.raw.btn_click_ef)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                    }
                    alertDialog.dismiss()
                }
                alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
                try { alertDialog.show() } catch (npe: NullPointerException) { npe.printStackTrace() }
            }
        })

        binding.volBtn.setBounceClickListener { volButton(it) }
        binding.ideaBtn.setBounceClickListener { ideaBtn() }
        binding.backBtn.setBounceClickListener {
            if (!isMuted) {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            onBackPressedDispatcher.onBackPressed()
        }

    }

    // Hide the status bar.
    //WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    //getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().SYSTEM_UI_FLAG_FULLSCREEN);
    //getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    //getActionBar().hide();
    @SuppressLint("SetTextI18n", "DiscouragedApi")
    fun lineClick(view: View) {
        //Toast.makeText(this, "clicked", Toast.LENGTH_SHORT).show();
        idNm = resources.getResourceEntryName(view.id)
        val bg = view.background as GradientDrawable
        val color = getColorGrad(bg)
        var change = false
        val red = resources.getColor(R.color.redX, theme)
        val blue = resources.getColor(R.color.blueX, theme)
        //        if(temp != null)
//        {
//            if(getColorGrad(temp)==ContextCompat.getColor(getApplicationContext(), R.color.redZ))
//                temp.setColor(red);
//            else
//                temp.setColor(blue);
//        }
//        temp=bg;
        if (color == resources.getColor(R.color.whiteX, theme)) {
            if (!isMuted) {
                val mediaPlayer = MediaPlayer.create(this, R.raw.line_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            clickCount++
            if (clickCount % 2 == 1) {
                bg.setColor(red)
            } else {
                bg.setColor(blue)
            }
            if (Character.getNumericValue(idNm[1]) > 1 && idNm[4] == 'T' || Character.getNumericValue(
                    idNm[3]
                ) > 1 && idNm[4] == 'L'
            ) {
                val idTopU = this.resources.getIdentifier(getIdNm(idNm)[0], "id", this.packageName)
                val idTopL = this.resources.getIdentifier(getIdNm(idNm)[1], "id", this.packageName)
                val idTopR = this.resources.getIdentifier(getIdNm(idNm)[2], "id", this.packageName)
                val lineU = findViewById<View>(idTopU)
                val lineL = findViewById<View>(idTopL)
                val lineR = findViewById<View>(idTopR)
                val bgTopU = lineU.background as GradientDrawable
                val bgTopL = lineL.background as GradientDrawable
                val bgTopR = lineR.background as GradientDrawable
                if ((getColorGrad(bgTopU) == red || getColorGrad(bgTopU) == blue) && (getColorGrad(
                        bgTopL
                    ) == red || getColorGrad(bgTopL) == blue) && (getColorGrad(bgTopR) == red || getColorGrad(
                        bgTopR
                    ) == blue)
                ) {
                    val txtId =
                        this.resources.getIdentifier(getIdNm(idNm)[6], "id", this.packageName)
                    val idMidC1 =
                        this.resources.getIdentifier(getIdNm(idNm)[8], "id", this.packageName)
                    val idMidC2 =
                        this.resources.getIdentifier(getIdNm(idNm)[9], "id", this.packageName)
                    val idUpC1 =
                        this.resources.getIdentifier(getIdNm(idNm)[10], "id", this.packageName)
                    val idUpC2 =
                        this.resources.getIdentifier(getIdNm(idNm)[11], "id", this.packageName)
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
                        if (!isMuted) {
                            val mediaPlayer = MediaPlayer.create(this, R.raw.box_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        scoreRed++
                        scoreRedView.text = "" + scoreRed
                        txt.text = "" + nm1[0]
                        txt.typeface = ResourcesCompat.getFont(applicationContext, R.font.bertram)
                        bgTopU.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgTopL.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgTopR.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgMidC1.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgMidC1.setStroke(
                            14,
                            ContextCompat.getColor(applicationContext, R.color.redY)
                        )
                        bgMidC2.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgMidC2.setStroke(
                            14,
                            ContextCompat.getColor(applicationContext, R.color.redY)
                        )
                        bgUpC1.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgUpC1.setStroke(
                            14,
                            ContextCompat.getColor(applicationContext, R.color.redY)
                        )
                        bgUpC2.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgUpC2.setStroke(
                            14,
                            ContextCompat.getColor(applicationContext, R.color.redY)
                        )
                        if (one) {
                            one = false
                            Toast.makeText(
                                this,
                                "Bonus TURN for " + redTxt.text,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        if (!isMuted) {
                            val mediaPlayer = MediaPlayer.create(this, R.raw.box_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        scoreBlue++
                        scoreBlueView.text = "" + scoreBlue
                        txt.text = "" + nm2[0]
                        txt.typeface = ResourcesCompat.getFont(applicationContext, R.font.bertram)
                        bgTopU.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgTopL.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgTopR.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgMidC1.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgMidC1.setStroke(
                            14,
                            ContextCompat.getColor(applicationContext, R.color.blueY)
                        )
                        bgMidC2.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgMidC2.setStroke(
                            14,
                            ContextCompat.getColor(applicationContext, R.color.blueY)
                        )
                        bgUpC1.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgUpC1.setStroke(
                            14,
                            ContextCompat.getColor(applicationContext, R.color.blueY)
                        )
                        bgUpC2.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgUpC2.setStroke(
                            14,
                            ContextCompat.getColor(applicationContext, R.color.blueY)
                        )
                        if (one) {
                            one = false
                            Toast.makeText(
                                this,
                                "Bonus TURN for " + blueTxt.text,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    change = true
                }
            }
            if (Character.getNumericValue(idNm[1]) < 7 && idNm[4] == 'T' || Character.getNumericValue(
                    idNm[3]
                ) < 7 && idNm[4] == 'L'
            ) {
                val idDownU = this.resources.getIdentifier(getIdNm(idNm)[3], "id", this.packageName)
                val idDownL = this.resources.getIdentifier(getIdNm(idNm)[4], "id", this.packageName)
                val idDownR = this.resources.getIdentifier(getIdNm(idNm)[5], "id", this.packageName)
                val lineDownU = findViewById<View>(idDownU)
                val lineDownL = findViewById<View>(idDownL)
                val lineDownR = findViewById<View>(idDownR)
                val bgDownU = lineDownU.background as GradientDrawable
                val bgDownL = lineDownL.background as GradientDrawable
                val bgDownR = lineDownR.background as GradientDrawable
                if ((getColorGrad(bgDownU) == red || getColorGrad(bgDownU) == blue) && (getColorGrad(
                        bgDownL
                    ) == red || getColorGrad(bgDownL) == blue) && (getColorGrad(bgDownR) == red || getColorGrad(
                        bgDownR
                    ) == blue)
                ) {
                    val txtId =
                        this.resources.getIdentifier(getIdNm(idNm)[7], "id", this.packageName)
                    val idMidC1 =
                        this.resources.getIdentifier(getIdNm(idNm)[8], "id", this.packageName)
                    val idMidC2 =
                        this.resources.getIdentifier(getIdNm(idNm)[9], "id", this.packageName)
                    val idDownC1 =
                        this.resources.getIdentifier(getIdNm(idNm)[12], "id", this.packageName)
                    val idDownC2 =
                        this.resources.getIdentifier(getIdNm(idNm)[13], "id", this.packageName)
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
                        if (!isMuted) {
                            val mediaPlayer = MediaPlayer.create(this, R.raw.box_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        scoreRed++
                        scoreRedView.text = "" + scoreRed
                        txt.text = "" + nm1[0]
                        txt.typeface = ResourcesCompat.getFont(applicationContext, R.font.bertram)
                        bgDownU.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgDownL.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgDownR.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgMidC1.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgMidC1.setStroke(
                            14,
                            ContextCompat.getColor(applicationContext, R.color.redY)
                        )
                        bgMidC2.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgMidC2.setStroke(
                            14,
                            ContextCompat.getColor(applicationContext, R.color.redY)
                        )
                        bgDownC1.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgDownC1.setStroke(
                            14,
                            ContextCompat.getColor(applicationContext, R.color.redY)
                        )
                        bgDownC2.setColor(ContextCompat.getColor(applicationContext, R.color.redX))
                        bgDownC2.setStroke(
                            14,
                            ContextCompat.getColor(applicationContext, R.color.redY)
                        )
                        if (one) {
                            one = false
                            Toast.makeText(
                                this,
                                "Bonus TURN for " + redTxt.text,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        if (!isMuted) {
                            val mediaPlayer = MediaPlayer.create(this, R.raw.box_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        scoreBlue++
                        scoreBlueView.text = "" + scoreBlue
                        txt.text = "" + nm2[0]
                        txt.typeface = ResourcesCompat.getFont(applicationContext, R.font.bertram)
                        bgDownU.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgDownL.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgDownR.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgMidC1.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgMidC1.setStroke(
                            14,
                            ContextCompat.getColor(applicationContext, R.color.blueY)
                        )
                        bgMidC2.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgMidC2.setStroke(
                            14,
                            ContextCompat.getColor(applicationContext, R.color.blueY)
                        )
                        bgDownC1.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgDownC1.setStroke(
                            14,
                            ContextCompat.getColor(applicationContext, R.color.blueY)
                        )
                        bgDownC2.setColor(ContextCompat.getColor(applicationContext, R.color.blueX))
                        bgDownC2.setStroke(
                            14,
                            ContextCompat.getColor(applicationContext, R.color.blueY)
                        )
                        if (one) {
                            one = false
                            Toast.makeText(
                                this,
                                "Bonus TURN for " + blueTxt.text,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    change = true
                }
            }
            if (change) clickCount-- else {
                if (clickCount % 2 == 1) {
                    redTxt.textSize = 30f
                    redTxt.setTextColor(resources.getColor(R.color.whiteT, theme))
                    blueTxt.textSize = 35f
                    blueTxt.setTextColor(resources.getColor(R.color.white, theme))
                } else {
                    blueTxt.textSize = 30f
                    blueTxt.setTextColor(resources.getColor(R.color.whiteT, theme))
                    redTxt.textSize = 35f
                    redTxt.setTextColor(resources.getColor(R.color.white, theme))
                }
            }
            if (scoreRed + scoreBlue == 36) {
                var winOffline = sharedPref.getInt("winOffline", 0)
                editor.putInt("winOffline", ++winOffline).apply()
                val handler = Handler()
                handler.postDelayed({
                    if (!isMuted) {
                        val mediaPlayer = MediaPlayer.create(this, R.raw.win_ef)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                    }
                    redTxt.textSize = 30f
                    redTxt.setTextColor(resources.getColor(R.color.white, theme))
                    blueTxt.textSize = 30f
                    blueTxt.setTextColor(resources.getColor(R.color.white, theme))
                    if (scoreRed > scoreBlue) 
                        onGameOver("Player RED won the match.",winOffline)
                    else if (scoreRed < scoreBlue) 
                        onGameOver("Player BLUE won the match.", winOffline)
                    else 
                        onGameOver("Match Draw.", winOffline)
                }, 800)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun onGameOver(winMsg: String, winOffline: Int) {
        val builder = AlertDialog.Builder(this@GameActivity1)
        val view = LayoutInflater.from(this@GameActivity1).inflate(
            R.layout.dialog_layout_alert, findViewById(R.id.layoutDialog)
        )
        builder.setView(view)
        //builder.setCancelable(false);
        if (saveToFirebase()) Toast.makeText(
            this,
            "Score Saved to Online Score Board",
            Toast.LENGTH_SHORT
        ).show()
        (view.findViewById<View>(R.id.textMessage) as TextView).text = "" + winMsg
        (view.findViewById<View>(R.id.buttonNo) as Button).text = "Exit"
        (view.findViewById<View>(R.id.buttonYes) as Button).text = "Retry!"
        val alertDialog = builder.create()
        view.findViewById<View>(R.id.buttonYes).setBounceClickListener {
            if (!isMuted) {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            if (winOffline > 5) {
                val manager = ReviewManagerFactory.create(this)
                val request = manager.requestReviewFlow()
                request.addOnCompleteListener { task->
                    if (task.isSuccessful) {
                        // We can get the ReviewInfo object
                        val reviewInfo = task.result
                        val flow = manager.launchReviewFlow(this, reviewInfo!!)
                        flow.addOnCompleteListener {
                            // The flow has finished. The API does not indicate whether the user
                            // reviewed or not, or even whether the review dialog was shown. Thus, no
                            // matter the result, we continue our app flow.
                            recreate()
                        }
                    } else {
                        recreate()
                        // There was some problem, log or handle the error code.
//                        @ReviewErrorCode int reviewErrorCode = ((ReviewException) task.getException()).getErrorCode();
                    }
                }
            } else {
                recreate()
            }
            alertDialog.dismiss()
        }
        view.findViewById<View>(R.id.buttonNo).setBounceClickListener {
            if (!isMuted) {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            alertDialog.dismiss()
            finish()
            flag = true
            Toast.makeText(this, "Score Saved to Online Score Board", Toast.LENGTH_SHORT).show()
        }
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
        try {
            alertDialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun volButton(view: View) {
        if (!isMuted) {
            binding.volBtn.setBackgroundResource(R.drawable.btn_gry_bg)
            binding.volBtn.setImageResource(R.drawable.icon_vol_mute)
            editor.putBoolean("muted", true).apply()
        } else {
            run {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            binding.volBtn.setBackgroundResource(R.drawable.btn_ylw_bg)
            binding.volBtn.setImageResource(R.drawable.icon_vol_unmute)
            editor.putBoolean("muted", false).apply()
        }
    }

    private fun ideaBtn() {
        if (!isMuted) {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        infoShow()
    }

    private fun infoShow() {
        if (isFirstRun) editor.putBoolean("firstRun", false).apply()
        val i = AtomicInteger()
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
        val builder = AlertDialog.Builder(this@GameActivity1)
        val view = LayoutInflater.from(this@GameActivity1).inflate(
            R.layout.dialog_layout_info, findViewById(R.id.layoutInfo)
        )
        builder.setView(view)
        builder.setCancelable(false)
        (view.findViewById<View>(R.id.textMessage) as TextView).text = msg[0]
        (view.findViewById<View>(R.id.playGif) as GifImageView).setImageResource(gifs[0])
        view.findViewById<View>(R.id.buttonPre).visibility = View.INVISIBLE
        val alertDialog = builder.create()
        view.findViewById<View>(R.id.buttonPre).setBounceClickListener {
            if (!isMuted) {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            if (i.get() != 0) i.getAndDecrement()
            if (i.get() == 0) view.findViewById<View>(R.id.buttonPre).visibility = View.INVISIBLE
            (view.findViewById<View>(R.id.textMessage) as TextView).text = msg[i.get()]
            (view.findViewById<View>(R.id.playGif) as GifImageView).setImageResource(gifs[i.get()])
        }
        view.findViewById<View>(R.id.buttonNext).setBounceClickListener {
            if (!isMuted) {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            i.getAndIncrement()
            if (!isFirstRun && i.get() == 4) i.getAndIncrement()
            if (i.get() == 1) view.findViewById<View>(R.id.buttonPre).visibility = View.VISIBLE
            if (i.get() == 5) alertDialog.dismiss() else {
                (view.findViewById<View>(R.id.textMessage) as TextView).text = msg[i.get()]
                (view.findViewById<View>(R.id.playGif) as GifImageView).setImageResource(
                    gifs[i.get()]
                )
            }
        }
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
        try {
            alertDialog.show()
        } catch (npe: NullPointerException) {
            npe.printStackTrace()
        }
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
        var nm1 = "Red"
        var nm2 = "Blue"
        lateinit var PACKAGE_NAME: String
        var one = true
        var flag = true
        fun getIdNm(idNm: String?): Array<String?> {
            val idS = arrayOfNulls<String>(14)
            val id = StringBuilder()
            if (idNm!![4] == 'T') {
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
            return idS
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

        fun saveToFirebase(): Boolean {
            val success = AtomicBoolean(false)
            val dtf = DateTimeFormatter.ofPattern("dd MMM, hh:mm a")
            val now = LocalDateTime.now()
            val timeData = dtf.format(now)
            val starData = "friendly"
            val redData = "$nm1: $scoreRed"
            val blueData = "$nm2: $scoreBlue"
            val ds = DataStore(
                System.currentTimeMillis(),
                timeData,
                redData,
                blueData,
                starData,
                "offline",
                "",
                "",
                ""
            )

//        FirebaseDatabase database = FirebaseDatabase.getInstance();
//        DatabaseReference myRef = database.getReference("ScoreBoard");
//        //single
//        DatabaseReference finalMyRef = myRef;
//        myRef.child("Last Best Player").addValueEventListener(new ValueEventListener() {
//            @SuppressLint("SetTextI18n")
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
//            {
//                if(dataSnapshot.exists())
//                {
//                    String bestScoreData = dataSnapshot.getValue(String.class);
//                    assert bestScoreData != null;
//                    String[] arrOfStr =bestScoreData.split(" ");
//                    bestScore= Integer.parseInt(arrOfStr[arrOfStr.length-1]);
//                    if(bestScore < scoreRed)
//                        finalMyRef.child("Last Best Player").setValue(redData);
//                    else if(bestScore < scoreBlue)
//                        finalMyRef.child("Last Best Player").setValue(blueData);
//                    scoreRed=0;
//                    scoreBlue=0;
//                }
//
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Log.w("saveToFirebase", "Failed to read value.", error.toException());
//            }
//        });
//
//        //multiple
//        myRef=myRef.child("allScore");
//        String key = myRef.push().getKey();
//        assert key != null;
//        myRef.child(key).setValue(ds);

            //single
            val db = FirebaseFirestore.getInstance()
            //Source source = Source.CACHE;
            db.collection("LastBestPlayer").document("LastBestPlayer")
                .get().addOnCompleteListener { task: Task<DocumentSnapshot> ->
                    if (task.isSuccessful) {
                        val document = task.result
                        //Log.d("TAG", "Cached document data: " + document.getData());
                        val bestScoreData =
                            Objects.requireNonNull(Objects.requireNonNull(document.data)["info"])
                                .toString()
                        val arrOfStr =
                            bestScoreData.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        bestScore = arrOfStr[arrOfStr.size - 1].toInt()
                        if (bestScore <= scoreRed) db.collection("LastBestPlayer")
                            .document("LastBestPlayer")
                            .update("info", redData) else if (bestScore <= scoreBlue) db.collection(
                            "LastBestPlayer"
                        ).document("LastBestPlayer").update("info", blueData)
                    }
                }
            //multiple
            val myRef =
                Firebase.database.getReference("ScoreBoard").child("allScore") //he he
            val key = myRef.push().key!!
            db.collection("ScoreBoard").document(key).set(ds)
                .addOnCompleteListener { success.set(true) }
            return success.get()
        }
    }
}