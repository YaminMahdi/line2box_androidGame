package com.diu.yk_games.line2box.presentation.offline

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat.recreate
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.toRoute
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogLayoutAlertBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutInfoBinding
import com.diu.yk_games.line2box.databinding.FragmentGameDualBinding
import com.diu.yk_games.line2box.model.DataStore
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.presentation.navigation.Routes
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
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Objects
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class GameDualFragment : Fragment() {
    private lateinit var binding: FragmentGameDualBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var scoreRedView: TextView
    private lateinit var scoreBlueView: TextView
    private lateinit var redTxt: TextView
    private lateinit var blueTxt: TextView

    var nm1 = "Red"
    var nm2 = "Blue"

    lateinit var parentActivity: FragmentActivity

    private var isFirstRun = false

    private fun ifMuted() {
        lifecycleScope.launch {
            binding.volBtn.applyState(isMuted())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGameDualBinding.inflate(layoutInflater)
        parentActivity = requireActivity()
        return binding.root
    }

    @SuppressLint("DiscouragedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        runCatching {
            val arg = findNavController().getBackStackEntry<Routes.GameDual>().toRoute<Routes.GameDual>()
            nm1 = arg.nm1
            nm2 = arg.nm2
        }
        scoreRedView = binding.scoreRed
        scoreBlueView = binding.scoreBlue
        redTxt = binding.red
        blueTxt = binding.blue
        binding.nm1Id.text = nm1
        binding.nm2Id.text = nm2
        clickCount = 0
        scoreRed = 0
        scoreBlue = 0
        bestScore = 9999
        one = true
        ifMuted()
        isFirstRun = pref.read("firstRun", true)
        setupListener()
        /*        val index = StringBuilder()
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
                        var idTop = resources.getIdentifier(top, "id", parentActivity.packageName)
                        var idLeft = resources.getIdentifier(left, "id", parentActivity.packageName)
                        var idCircle = resources.getIdentifier(circle, "id", parentActivity.packageName)
                        var lineTop = binding.root.findViewById<View>(idTop)
                        var lineLeft = binding.root.findViewById<View>(idLeft)
                        var lineCircle = binding.root.findViewById<View>(idCircle)
                        var bgTop = lineTop.background as GradientDrawable
                        var bgLeft = lineLeft.background as GradientDrawable
                        var bgCircle = lineCircle.background as GradientDrawable
                        bgTop.setColor(ContextCompat.getColor(parentActivity, R.color.whiteX))
                        bgLeft.setColor(ContextCompat.getColor(parentActivity, R.color.whiteX))
                        bgCircle.setColor(ContextCompat.getColor(parentActivity, R.color.white))
                        bgCircle.setStroke(14, ContextCompat.getColor(parentActivity, R.color.whiteY))
                        if (i == 6) {
                            index.setLength(0)
                            index.append(top)
                            index.deleteCharAt(1)
                            index.insert(1, i + 1)
                            idTop = resources.getIdentifier(index.toString(), "id", parentActivity.packageName)
                            index.setLength(0)
                            index.append(circle)
                            index.deleteCharAt(1)
                            index.insert(1, i + 1)
                            idCircle =
                                resources.getIdentifier(index.toString(), "id", parentActivity.packageName)
                            lineTop = binding.root.findViewById(idTop)
                            lineCircle = binding.root.findViewById(idCircle)
                            bgTop = lineTop.background as GradientDrawable
                            bgCircle = lineCircle.background as GradientDrawable
                            bgTop.setColor(ContextCompat.getColor(parentActivity, R.color.whiteX))
                            bgCircle.setColor(ContextCompat.getColor(parentActivity, R.color.white))
                            bgCircle.setStroke(14, ContextCompat.getColor(parentActivity, R.color.whiteY))
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
                            idLeft = resources.getIdentifier(left, "id", parentActivity.packageName)
                            index.setLength(0)
                            index.append(circle)
                            index.deleteCharAt(4)
                            index.insert(4, j + 1)
                            circle = index.toString()
                            idCircle = resources.getIdentifier(circle, "id", parentActivity.packageName)
                            lineLeft = binding.root.findViewById(idLeft)
                            lineCircle = binding.root.findViewById(idCircle)
                            bgLeft = lineLeft.background as GradientDrawable
                            bgCircle = lineCircle.background as GradientDrawable
                            bgLeft.setColor(ContextCompat.getColor(parentActivity, R.color.whiteX))
                            bgCircle.setColor(ContextCompat.getColor(parentActivity, R.color.white))
                            bgCircle.setStroke(14, ContextCompat.getColor(parentActivity, R.color.whiteY))
                            if (i == 6) {
                                index.setLength(0)
                                index.append(circle)
                                index.deleteCharAt(1)
                                index.insert(1, i + 1)
                                circle = index.toString()
                                idCircle = resources.getIdentifier(circle, "id", parentActivity.packageName)
                                lineCircle = binding.root.findViewById(idCircle)
                                bgCircle = lineCircle.background as GradientDrawable
                                bgCircle.setColor(ContextCompat.getColor(parentActivity, R.color.white))
                                bgCircle.setStroke(14, ContextCompat.getColor(parentActivity, R.color.whiteY))
                            }
                        }
                    }
                }*/
    }

    fun setupListener() {
        lifecycleScope.launch {
//            viewModel.lineIDs.forEach {
//                val lineId =
//                    resources.getIdentifier(it, "id", parentActivity.packageName)
//                binding.root.findViewById<View>(lineId).setOnClickListener(::lineClick)
//            }
            val lineViews = listOf(
                binding.rh1,
                binding.rh2,
                binding.rh3,
                binding.rh4,
                binding.rh5,
                binding.rh6,
                binding.rh7,
                binding.rv1,
                binding.rv2,
                binding.rv3,
                binding.rv4,
                binding.rv5,
                binding.rv6
            )
            lineViews.forEach { viewGroup ->
                repeat(viewGroup.childCount) { i ->
                    viewGroup.getChildAt(i)?.setOnClickListener(::lineClick)
                }
            }
        }
        binding.volBtn.performOnClickF()
        binding.ideaBtn.setBounceClickListener { ideaBtn() }
        binding.backBtn.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            onBackPressed()
        }
    }

    // Hide the status bar.
    //WindowCompat.setDecorFitsSystemWindows(getWindow(), false)
    //getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().SYSTEM_UI_FLAG_FULLSCREEN)
    //getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    //getActionBar().hide()
    @SuppressLint("SetTextI18n", "DiscouragedApi")
    fun lineClick(view: View) {
        //Toast.makeText(parentActivity, "clicked", Toast.LENGTH_SHORT).show()
        idNm = resources.getResourceEntryName(view.id)
        val bg = view.background as GradientDrawable
        val color = getColorGrad(bg)
        var change = false
        val red = resources.getColor(R.color.redX, parentActivity.theme)
        val blue = resources.getColor(R.color.blueX, parentActivity.theme)
        //        if(temp != null)
//        {
//            if(getColorGrad(temp)==ContextCompat.getColor(getApplicationContext(), R.color.redZ))
//                temp.setColor(red)
//            else
//                temp.setColor(blue)
//        }
//        temp=bg;
        if (color == resources.getColor(R.color.whiteX, parentActivity.theme)) {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.line_click_ef)
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
                val idTopU =
                    resources.getIdentifier(getIdNm(idNm)[0], "id", parentActivity.packageName)
                val idTopL =
                    resources.getIdentifier(getIdNm(idNm)[1], "id", parentActivity.packageName)
                val idTopR =
                    resources.getIdentifier(getIdNm(idNm)[2], "id", parentActivity.packageName)
                val lineU = binding.root.findViewById<View>(idTopU)
                val lineL = binding.root.findViewById<View>(idTopL)
                val lineR = binding.root.findViewById<View>(idTopR)
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
                        resources.getIdentifier(getIdNm(idNm)[6], "id", parentActivity.packageName)
                    val idMidC1 =
                        resources.getIdentifier(getIdNm(idNm)[8], "id", parentActivity.packageName)
                    val idMidC2 =
                        resources.getIdentifier(getIdNm(idNm)[9], "id", parentActivity.packageName)
                    val idUpC1 =
                        resources.getIdentifier(getIdNm(idNm)[10], "id", parentActivity.packageName)
                    val idUpC2 =
                        resources.getIdentifier(getIdNm(idNm)[11], "id", parentActivity.packageName)
                    val crMid1 = binding.root.findViewById<View>(idMidC1)
                    val crMid2 = binding.root.findViewById<View>(idMidC2)
                    val crUp1 = binding.root.findViewById<View>(idUpC1)
                    val crUp2 = binding.root.findViewById<View>(idUpC2)
                    val bgMidC1 = crMid1.background as GradientDrawable
                    val bgMidC2 = crMid2.background as GradientDrawable
                    val bgUpC1 = crUp1.background as GradientDrawable
                    val bgUpC2 = crUp2.background as GradientDrawable
                    val txt = binding.root.findViewById<TextView>(txtId)
                    if (clickCount % 2 == 1) {
                        isNotMuted {
                            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.box_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        scoreRed++
                        scoreRedView.text = "" + scoreRed
                        txt.text = "" + nm1[0]
                        txt.typeface = ResourcesCompat.getFont(parentActivity, R.font.bertram)
                        bgTopU.setColor(ContextCompat.getColor(parentActivity, R.color.redX))
                        bgTopL.setColor(ContextCompat.getColor(parentActivity, R.color.redX))
                        bgTopR.setColor(ContextCompat.getColor(parentActivity, R.color.redX))
                        bgMidC1.setColor(ContextCompat.getColor(parentActivity, R.color.redX))
                        bgMidC1.setStroke(
                            14,
                            ContextCompat.getColor(parentActivity, R.color.redY)
                        )
                        bgMidC2.setColor(ContextCompat.getColor(parentActivity, R.color.redX))
                        bgMidC2.setStroke(
                            14,
                            ContextCompat.getColor(parentActivity, R.color.redY)
                        )
                        bgUpC1.setColor(ContextCompat.getColor(parentActivity, R.color.redX))
                        bgUpC1.setStroke(
                            14,
                            ContextCompat.getColor(parentActivity, R.color.redY)
                        )
                        bgUpC2.setColor(ContextCompat.getColor(parentActivity, R.color.redX))
                        bgUpC2.setStroke(
                            14,
                            ContextCompat.getColor(parentActivity, R.color.redY)
                        )
                        if (one) {
                            one = false
                            toast("Bonus TURN for ${redTxt.text}")
                        }
                    } else {
                        isNotMuted {
                            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.box_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        scoreBlue++
                        scoreBlueView.text = "" + scoreBlue
                        txt.text = "" + nm2[0]
                        txt.typeface = ResourcesCompat.getFont(parentActivity, R.font.bertram)
                        bgTopU.setColor(ContextCompat.getColor(parentActivity, R.color.blueX))
                        bgTopL.setColor(ContextCompat.getColor(parentActivity, R.color.blueX))
                        bgTopR.setColor(ContextCompat.getColor(parentActivity, R.color.blueX))
                        bgMidC1.setColor(ContextCompat.getColor(parentActivity, R.color.blueX))
                        bgMidC1.setStroke(
                            14,
                            ContextCompat.getColor(parentActivity, R.color.blueY)
                        )
                        bgMidC2.setColor(ContextCompat.getColor(parentActivity, R.color.blueX))
                        bgMidC2.setStroke(
                            14,
                            ContextCompat.getColor(parentActivity, R.color.blueY)
                        )
                        bgUpC1.setColor(ContextCompat.getColor(parentActivity, R.color.blueX))
                        bgUpC1.setStroke(
                            14,
                            ContextCompat.getColor(parentActivity, R.color.blueY)
                        )
                        bgUpC2.setColor(ContextCompat.getColor(parentActivity, R.color.blueX))
                        bgUpC2.setStroke(
                            14,
                            ContextCompat.getColor(parentActivity, R.color.blueY)
                        )
                        if (one) {
                            one = false
                            toast("Bonus TURN for ${blueTxt.text}")
                        }
                    }
                    change = true
                }
            }
            if (Character.getNumericValue(idNm[1]) < 7 && idNm[4] == 'T' || Character.getNumericValue(
                    idNm[3]
                ) < 7 && idNm[4] == 'L'
            ) {
                val idDownU =
                    resources.getIdentifier(getIdNm(idNm)[3], "id", parentActivity.packageName)
                val idDownL =
                    resources.getIdentifier(getIdNm(idNm)[4], "id", parentActivity.packageName)
                val idDownR =
                    resources.getIdentifier(getIdNm(idNm)[5], "id", parentActivity.packageName)
                val lineDownU = binding.root.findViewById<View>(idDownU)
                val lineDownL = binding.root.findViewById<View>(idDownL)
                val lineDownR = binding.root.findViewById<View>(idDownR)
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
                        resources.getIdentifier(getIdNm(idNm)[7], "id", parentActivity.packageName)
                    val idMidC1 =
                        resources.getIdentifier(getIdNm(idNm)[8], "id", parentActivity.packageName)
                    val idMidC2 =
                        resources.getIdentifier(getIdNm(idNm)[9], "id", parentActivity.packageName)
                    val idDownC1 =
                        resources.getIdentifier(getIdNm(idNm)[12], "id", parentActivity.packageName)
                    val idDownC2 =
                        resources.getIdentifier(getIdNm(idNm)[13], "id", parentActivity.packageName)
                    val crMid1 = binding.root.findViewById<View>(idMidC1)
                    val crMid2 = binding.root.findViewById<View>(idMidC2)
                    val crDown1 = binding.root.findViewById<View>(idDownC1)
                    val crDown2 = binding.root.findViewById<View>(idDownC2)
                    val bgMidC1 = crMid1.background as GradientDrawable
                    val bgMidC2 = crMid2.background as GradientDrawable
                    val bgDownC1 = crDown1.background as GradientDrawable
                    val bgDownC2 = crDown2.background as GradientDrawable
                    val txt = binding.root.findViewById<TextView>(txtId)
                    if (clickCount % 2 == 1) {
                        isNotMuted {
                            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.box_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        scoreRed++
                        scoreRedView.text = "" + scoreRed
                        txt.text = "" + nm1[0]
                        txt.typeface = ResourcesCompat.getFont(parentActivity, R.font.bertram)
                        bgDownU.setColor(ContextCompat.getColor(parentActivity, R.color.redX))
                        bgDownL.setColor(ContextCompat.getColor(parentActivity, R.color.redX))
                        bgDownR.setColor(ContextCompat.getColor(parentActivity, R.color.redX))
                        bgMidC1.setColor(ContextCompat.getColor(parentActivity, R.color.redX))
                        bgMidC1.setStroke(
                            14,
                            ContextCompat.getColor(parentActivity, R.color.redY)
                        )
                        bgMidC2.setColor(ContextCompat.getColor(parentActivity, R.color.redX))
                        bgMidC2.setStroke(
                            14,
                            ContextCompat.getColor(parentActivity, R.color.redY)
                        )
                        bgDownC1.setColor(ContextCompat.getColor(parentActivity, R.color.redX))
                        bgDownC1.setStroke(
                            14,
                            ContextCompat.getColor(parentActivity, R.color.redY)
                        )
                        bgDownC2.setColor(ContextCompat.getColor(parentActivity, R.color.redX))
                        bgDownC2.setStroke(
                            14,
                            ContextCompat.getColor(parentActivity, R.color.redY)
                        )
                        if (one) {
                            one = false
                            toast("Bonus TURN for ${redTxt.text}")
                        }
                    } else {
                        isNotMuted {
                            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.box_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        scoreBlue++
                        scoreBlueView.text = "" + scoreBlue
                        txt.text = "" + nm2[0]
                        txt.typeface = ResourcesCompat.getFont(parentActivity, R.font.bertram)
                        bgDownU.setColor(ContextCompat.getColor(parentActivity, R.color.blueX))
                        bgDownL.setColor(ContextCompat.getColor(parentActivity, R.color.blueX))
                        bgDownR.setColor(ContextCompat.getColor(parentActivity, R.color.blueX))
                        bgMidC1.setColor(ContextCompat.getColor(parentActivity, R.color.blueX))
                        bgMidC1.setStroke(
                            14,
                            ContextCompat.getColor(parentActivity, R.color.blueY)
                        )
                        bgMidC2.setColor(ContextCompat.getColor(parentActivity, R.color.blueX))
                        bgMidC2.setStroke(
                            14,
                            ContextCompat.getColor(parentActivity, R.color.blueY)
                        )
                        bgDownC1.setColor(ContextCompat.getColor(parentActivity, R.color.blueX))
                        bgDownC1.setStroke(
                            14,
                            ContextCompat.getColor(parentActivity, R.color.blueY)
                        )
                        bgDownC2.setColor(ContextCompat.getColor(parentActivity, R.color.blueX))
                        bgDownC2.setStroke(
                            14,
                            ContextCompat.getColor(parentActivity, R.color.blueY)
                        )
                        if (one) {
                            one = false
                            toast("Bonus TURN for ${blueTxt.text}")
                        }
                    }
                    change = true
                }
            }
            if (change) clickCount-- else {
                if (clickCount % 2 == 1) {
                    redTxt.textSize = 30f
                    redTxt.setTextColor(resources.getColor(R.color.whiteT, parentActivity.theme))
                    blueTxt.textSize = 35f
                    blueTxt.setTextColor(resources.getColor(R.color.white, parentActivity.theme))
                } else {
                    blueTxt.textSize = 30f
                    blueTxt.setTextColor(resources.getColor(R.color.whiteT, parentActivity.theme))
                    redTxt.textSize = 35f
                    redTxt.setTextColor(resources.getColor(R.color.white, parentActivity.theme))
                }
            }
            if (scoreRed + scoreBlue == 36) {
                var winOffline = pref.read("winOffline", 0)
                pref.save("winOffline", ++winOffline)
                lifecycleScope.launch {
                    delay(800)
                    isNotMuted {
                        val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.win_ef)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                    }
                    redTxt.textSize = 30f
                    redTxt.setTextColor(ContextCompat.getColor(parentActivity, R.color.white))
                    blueTxt.textSize = 30f
                    blueTxt.setTextColor(ContextCompat.getColor(parentActivity, R.color.white))
                    if (scoreRed > scoreBlue)
                        onGameOver("Player RED won the match.", winOffline)
                    else if (scoreRed < scoreBlue)
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
            if (winOffline > 5) {
                val manager = ReviewManagerFactory.create(parentActivity)
                val request = manager.requestReviewFlow()
                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val reviewInfo = task.result
                        val flow = manager.launchReviewFlow(parentActivity, reviewInfo!!)
                        flow.addOnCompleteListener {
                            recreate(parentActivity)
                        }
                    } else recreate(parentActivity)
                }
            } else recreate(parentActivity)
            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
        }

        binding.buttonNo.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
            parentActivity.finish()
            toast("Score Saved to Online Score Board")
        }

        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        runCatching { alertDialog.show() }
    }

    private fun ideaBtn() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        infoShow()
    }

    private fun infoShow() {
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
        val binding = DialogLayoutInfoBinding.inflate(layoutInflater)
        builder.setView(binding.root)
        builder.setCancelable(false)
        binding.textMessage.text = msg[0]
        binding.playGif.loadDrawable(gifs[0])
        binding.buttonPre.invisible()
        val alertDialog = builder.create()
        binding.buttonPre.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            if (i != 0) i--
            if (i == 0) binding.buttonPre.invisible()
            binding.textMessage.text = msg[i]
            binding.playGif.loadDrawable(gifs[i])
        }
        binding.buttonNext.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            if (i <= 4) i++
            if (!isFirstRun && i == 4) i++
            if (i == 1) binding.buttonPre.show()
            if (i >= 5) runCatching { if (alertDialog.isShowing) alertDialog.dismiss() } else {
                binding.textMessage.text = msg[i]
                binding.playGif.loadDrawable(gifs[i])
            }
        }
        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        try {
            alertDialog.show()
        } catch (npe: NullPointerException) {
            npe.printStackTrace()
        }
    }

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
        val starData = "friendly"
        val redData = "$nm1: $scoreRed"
        val blueData = "$nm2: $scoreBlue"
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
                    bestScore <= scoreRed -> ds.redData
                    bestScore <= scoreBlue -> ds.blueData
                    else -> null
                }
                data?.let { info ->
                    db.collection("LastBestPlayer").document("LastBestPlayer")
                        .update("info", info)
                }
            }
        //multiple
        val key = UUID.randomUUID().toString()
        db.collection("ScoreBoard").document(key).set(ds)
            .addOnCompleteListener { success.set(true) }
        return success.get()
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
        var one = true
    }
}