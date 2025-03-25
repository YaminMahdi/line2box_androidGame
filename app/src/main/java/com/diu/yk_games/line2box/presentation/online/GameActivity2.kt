package com.diu.yk_games.line2box.presentation.online

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.GravityCompat
import androidx.customview.widget.ViewDragHelper
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.ActivityGame2Binding
import com.diu.yk_games.line2box.databinding.ContentGame2Binding
import com.diu.yk_games.line2box.databinding.DialogLayoutAlertBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutGameOverBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutInfoBinding
import com.diu.yk_games.line2box.model.DataStore
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.model.MsgStore
import com.diu.yk_games.line2box.model.toMessage
import com.diu.yk_games.line2box.pref
import com.diu.yk_games.line2box.prefEditor
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.presentation.ViewPagerAdapter
import com.diu.yk_games.line2box.util.applyState
import com.diu.yk_games.line2box.util.changeVisibility
import com.diu.yk_games.line2box.util.closeKeyboard
import com.diu.yk_games.line2box.util.collectWithLifecycle
import com.diu.yk_games.line2box.util.getSystemBars
import com.diu.yk_games.line2box.util.gone
import com.diu.yk_games.line2box.util.hideSystemBars
import com.diu.yk_games.line2box.util.invisible
import com.diu.yk_games.line2box.util.isMuted
import com.diu.yk_games.line2box.util.isNotMuted
import com.diu.yk_games.line2box.util.loadDrawable
import com.diu.yk_games.line2box.util.onBackPressedIgnoreCallback
import com.diu.yk_games.line2box.util.performOnClick
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.diu.yk_games.line2box.util.setNavStatusPadding
import com.diu.yk_games.line2box.util.show
import com.diu.yk_games.line2box.util.toast
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Objects
import kotlin.random.Random


@SuppressLint("DiscouragedApi")
class GameActivity2 : AppCompatActivity() {
    private lateinit var bindingRoot: ActivityGame2Binding
    private lateinit var binding: ContentGame2Binding
    private val viewModel: MainViewModel by viewModels()

    private var lvl1: Int = 0
    private var lvl2: Int = 0
    private lateinit var gameKey: String
    private lateinit var playerId: String
    private lateinit var plr1Id: String
    private lateinit var plr2Id: String
    private var redX = 0
    private var redY = 0
    private var blueX = 0
    private var blueY = 0
    lateinit var matchRef: DatabaseReference
    lateinit var chatRef: DatabaseReference

    private fun ifMuted() {
        lifecycleScope.launch {
            binding.volBtn.applyState(isMuted())
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.hideSystemBars()
        bindingRoot = ActivityGame2Binding.inflate(layoutInflater)
        binding = bindingRoot.appBarGame2
        setContentView(bindingRoot.root)
        setNavStatusPadding(binding.linearLayout)

        redX = ContextCompat.getColor(applicationContext, R.color.redX)
        redY = ContextCompat.getColor(applicationContext, R.color.redY)
        blueX = ContextCompat.getColor(applicationContext, R.color.blueX)
        blueY = ContextCompat.getColor(applicationContext, R.color.blueY)

        ifMuted()
        isFirstRun = pref.getBoolean("firstRun", true)
        //        DrawerLayout drawer = binding.drawerLayout;
//        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        lifecycleScope.launch {
            delay(200)
            if (isFirstRun) infoShow()
        }
        PACKAGE_NAME = applicationContext.packageName
        intent.extras.let {
            gameKey = it?.getString("gameKey").orEmpty()
            plr1Id = it?.getString("plr1Id").orEmpty()
            plr2Id = it?.getString("plr2Id").orEmpty()
            playerId = if(plyr1) plr1Id else plr2Id
        }
        intent.extras?.let{
            it.getString("nm1")?.let { nm1 = it }
            it.getString("nm2")?.let { nm2 = it }
            lvl1 = it.getInt("lvl1")
            lvl2 = it.getInt("lvl2")
            plyr1 = it.getBoolean("plyr1")
            viewModel.initGameProfile()
            viewModel.fetchServerLineClick(gameKey = gameKey, isPlyr1 = plyr1)
        }
        plyrTurn = plyr1
        binding.nm1Id.text = "($nm1)"
        binding.nm2Id.text = "($nm2)"
        clickCount = 0
        scoreRed = 0
        scoreBlue = 0
        bestScore = 9999
        one = true
        viewModel.isNewMsgBoltVisible.collectWithLifecycle {
            binding.newMsgBoltu.changeVisibility(it)
        }
        binding.emojiPlay.gone()
        bindingRoot.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        bindingRoot.root.addDrawerListener(object : DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {
                Log.d("TAG", "onDrawerStateChanged: $newState")
                if (newState == ViewDragHelper.STATE_SETTLING) {
                    closeKeyboard()
                    isNotMuted {
                        val mediaPlayer = MediaPlayer.create(this@GameActivity2, R.raw.slide)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                    }
                }
            }
        })
        bindingRoot.chatPager.isUserInputEnabled = false
        bindingRoot.chatPager.adapter = ViewPagerAdapter(
            listOf(
                ChatFragmentGlobal(),
                ChatFragmentFriendly()
            ), this
        )
        bindingRoot.bubbleTabBar.addBubbleListener { id  ->
            if (id == R.id.globalChat)
                bindingRoot.chatPager.currentItem = 0
            else
                bindingRoot.chatPager.currentItem = 1
        }
        bindingRoot.bubbleTabBar.setSelected(1, true)
        bindingRoot.chatPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> bindingRoot.bubbleTabBar.setSelected(0, true)
                    1 -> bindingRoot.bubbleTabBar.setSelected(1, true)
                }
            }
        })
        val activityRootView = window.decorView
        activityRootView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            //r will be populated with the coordinates of your view that area still visible.
            activityRootView.getWindowVisibleDisplayFrame(r)
            val maxHight = activityRootView.height
            val heightDiff = maxHight - r.height()
            Log.d("TAG", "onGlobalLayout: "+"heidiff: "+heightDiff+" "+r.height()+" "+maxHight)
            val layout1 = findViewById<LinearLayout>(R.id.chatFragmentLinerLayout)
            val layout2 = findViewById<LinearLayout>(R.id.navCloseButtonLayout)
            if (heightDiff > 0.25 * maxHight) {
                // if more than 25% of the screen, its probably a keyboard......do something here
                Log.d("TAG", "onGlobalLayout: here")
                layout1.setPadding(0, 0, 0, heightDiff)
                layout2.setPadding(0, 0, 0, heightDiff)
            } else {
                val systemBars = getSystemBars()
                layout1.setPadding(0, 0, 0, systemBars.bottom)
                layout2.setPadding(0, 0, 0, systemBars.bottom)
            }
        }

        matchRef = viewModel.multiPlayerRef.child(gameKey).child("matchInfo")
        chatRef = viewModel.multiPlayerRef.child(gameKey).child("friendlyChat")
        viewModel.viewIdFromServer.collectWithLifecycle(minActiveState = Lifecycle.State.CREATED) {viewId->
            plyrTurn = true
            lineClick(findViewById(resources.getIdentifier(viewId, "id", packageName)))
        }
//        performServerLineClick(plyr1)

        binding.ideaBtn.setBounceClickListener {
            ideaBtn()
        }
        binding.backBtn.setBounceClickListener {
            backBtn()
        }
        binding.volBtn.performOnClick()
        binding.openNavBtn.setBounceClickListener {
            bindingRoot.drawerLayout.openDrawer(GravityCompat.START)
            viewModel.setNewMsgBoltVisible(false)
        }
        bindingRoot.closeNavBtn.setBounceClickListener {
            closeKeyboard()
            bindingRoot.drawerLayout.closeDrawer(GravityCompat.START)
            viewModel.setNewMsgBoltVisible(false)
        }
        val dialogBinding = DialogLayoutAlertBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root).create()
        onBackPressedDispatcher.addCallback(this){
            if(bindingRoot.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                bindingRoot.drawerLayout.closeDrawer(GravityCompat.START)
                return@addCallback
            }
            dialogBinding.textMessage.text =
                "Do you really want to QUIT the match?"
            dialogBinding.buttonYes.text = "YES"
            dialogBinding.buttonNo.text = "NO"
            dialogBinding.buttonYes.setBounceClickListener {
                isNotMuted {
                    val mediaPlayer = MediaPlayer.create(this@GameActivity2, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }

                if(viewModel.localPlayerCount != 2)
                    viewModel.multiPlayerRef.child(gameKey).removeValue()
                else {
                    val ms = GameProfile().toMessage(
                        playerId = playerId,
                        msg = "Left the match.",
                        type = MsgStore.Type.ExitText
                    )
                    chatRef.push().setValue(ms)
                    viewModel.multiPlayerRef.child(gameKey).child("playerCount")
                        .setValue("-1")
                }
                alertDialog.dismiss()
                onBackPressedIgnoreCallback()
            }
            dialogBinding.buttonNo.setBounceClickListener {
                isNotMuted {
                    val mediaPlayer = MediaPlayer.create(this@GameActivity2, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
                alertDialog.dismiss()
            }
            alertDialog.window?.setBackgroundDrawable(0.toDrawable())
            try {
                alertDialog.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                    bgCircle.setStroke(
                        14,
                        ContextCompat.getColor(applicationContext, R.color.whiteY)
                    )
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
                        idCircle = this.resources.getIdentifier(circle, "id", this.packageName)
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
    }

    @SuppressLint("SetTextI18n")
    fun lineClick(view: View) {
        Log.d(TAG, "After lineClick (plyrTurn): $plyrTurn")
        //Toast.makeText(this, "clicked", Toast.LENGTH_SHORT).show()
        idNm = resources.getResourceEntryName(view.id)
        val bg = view.background as GradientDrawable
        val color = getColorGrad(bg)
        var change = false
        val red = resources.getColor(R.color.redX, theme)
        val blue = resources.getColor(R.color.blueX, theme)
        if (color == resources.getColor(R.color.whiteX, theme) && plyrTurn) {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(this, R.raw.line_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            clickCount++
            if (plyr1 && clickCount % 2 == 1) {
                matchRef.child("plyr1").push()
                    .setValue(view.resources.getResourceEntryName(view.id))
            } else if (!plyr1 && clickCount % 2 == 0) {
                matchRef.child("plyr2").push()
                    .setValue(view.resources.getResourceEntryName(view.id))
            }
            if (clickCount % 2 == 1) {
                bg.setColor(redX)
            } else {
                bg.setColor(blueX)
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
                if ((getColorGrad(bgTopU) == red || getColorGrad(bgTopU) == blue) &&
                    (getColorGrad(bgTopL) == red || getColorGrad(bgTopL) == blue) &&
                    (getColorGrad(bgTopR) == red || getColorGrad(bgTopR) == blue)
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
                        isNotMuted {
                            val mediaPlayer = MediaPlayer.create(this, R.raw.box_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        scoreRed++
                        binding.scoreRed.text = "" + scoreRed
                        txt.text = "" + nm1[0]
                        txt.typeface = ResourcesCompat.getFont(applicationContext, R.font.bertram)
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
                        if (one) {
                            one = false
                            Toast.makeText(this, "Bonus TURN for $nm1", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        isNotMuted {
                            val mediaPlayer = MediaPlayer.create(this, R.raw.box_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        scoreBlue++
                        binding.scoreBlue.text = "" + scoreBlue
                        txt.text = "" + nm2[0]
                        txt.typeface = ResourcesCompat.getFont(applicationContext, R.font.bertram)
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
                            toast("Bonus TURN for $nm2")
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
                        isNotMuted {
                            val mediaPlayer = MediaPlayer.create(this, R.raw.box_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        scoreRed++
                        binding.scoreRed.text = "" + scoreRed
                        txt.text = "" + nm1[0]
                        txt.typeface = ResourcesCompat.getFont(applicationContext, R.font.bertram)
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
                        if (one) {
                            one = false
                            Toast.makeText(this, "Bonus TURN for " + binding.red.text, Toast.LENGTH_SHORT).show()
                        }
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
                            toast("Bonus TURN for " + binding.blue.text)
                        }
                    }
                    change = true
                }
            }
            if (change) {
                if (clickCount % 2 == 1) {
                    if (!plyr1) plyrTurn = false
                } else {
                    if (plyr1) plyrTurn = false
                }
                clickCount--
            } else {
                if (clickCount % 2 == 1) {
                    binding.red.textSize = 30f
                    binding.red.setTextColor(resources.getColor(R.color.whiteT, theme))
                    binding.blue.textSize = 35f
                    binding.blue.setTextColor(resources.getColor(R.color.white, theme))
                    if (plyr1) plyrTurn = false
                } else {
                    binding.blue.textSize = 30f
                    binding.blue.setTextColor(resources.getColor(R.color.whiteT, theme))
                    binding.red.textSize = 35f
                    binding.red.setTextColor(resources.getColor(R.color.white, theme))
                    if (!plyr1) plyrTurn = false
                }
            }
            if (scoreRed + scoreBlue == 36) {
                val db = Firebase.firestore
                val doc = db.collection("gamerProfile").document(playerId)
                doc.update("matchPlayed", FieldValue.increment(1))
                val updatePro = GameProfile()
                val winCoin = Random.nextInt(80) + 45
                val lostCoin = Random.nextInt(35) + 15
                updatePro.setMatchPlayed()
                isNotMuted {
                    val mediaPlayer = MediaPlayer.create(this, R.raw.win_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
                binding.red.textSize = 30f
                binding.red.setTextColor(resources.getColor(R.color.white, theme))
                binding.blue.textSize = 30f
                binding.blue.setTextColor(resources.getColor(R.color.white, theme))
                var winTxt = ""
                var wCoin = ""
                var plr1Cup = ""
                var plr2Cup = ""

                fun handleWin() {
                    doc.update("matchWinMulti", FieldValue.increment(1))
                    updatePro.setMatchWinMulti()
                    updatePro.coin += winCoin
                    updatePro.apply()
                    doc.update("coin", updatePro.coin)

                    winTxt = "You won the match."
                    wCoin = "+$winCoin"

                    val ms = MsgStore(
                        playerId = if (plyr1) plr1Id else plr2Id,
                        nmData = if (plyr1) nm1 else nm2,
                        lvlData = (if (plyr1) lvl1 else lvl2).toString(),
                        time = System.currentTimeMillis(),
                        msgData = "Won the match.",
                        type = MsgStore.Type.EnterText.name
                    )

                    chatRef.push().setValue(ms)
                }

                fun handleLoss() {
                    updatePro.coin -= lostCoin
                    updatePro.apply()
                    doc.update("coin", updatePro.coin)

                    winTxt = "You lost the match."
                    wCoin = "-$lostCoin"
                }

                fun handleDraw() {
                    updatePro.coin = 50
                    updatePro.apply()
                    doc.update("coin", updatePro.coin)

                    winTxt = "Match Draw."
                    wCoin = "+$winCoin"
                }

                when {
                    scoreRed > scoreBlue -> {
                        if (plyr1) {
                            handleWin()
                            plr1Cup = "+$winCoin"
                        } else {
                            handleLoss()
                            plr2Cup = "-$lostCoin"
                        }
                    }
                    scoreRed < scoreBlue -> {
                        if (!plyr1) {
                            handleWin()
                            plr2Cup = "+$winCoin"
                        } else {
                            handleLoss()
                            plr1Cup = "-$lostCoin"
                        }
                    }
                    else -> handleDraw()
                }
                // Update level after match
                doc.update("lvl", updatePro.lvlByCal)
                saveToFirebase(plr1Cup, plr2Cup)
                lifecycleScope.launch {
                    delay(1200)
                    onGameOver(winTxt, wCoin, updatePro)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun onGameOver(winMsg: String, winCoin: String, updatePro: GameProfile) {
        val coin = winCoin.toInt()
        val win = coin > -1

        val dialogBinding = DialogLayoutGameOverBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(this@GameActivity2)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()
        dialogBinding.textMessage.text = winMsg
        dialogBinding.buttonNo.text = "Exit"
        dialogBinding.buttonYes.text = "Chat"
        dialogBinding.buttonYes.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            if (updatePro.matchWinMulti > 2) {
                val manager = ReviewManagerFactory.create(this)
                val request = manager.requestReviewFlow()
                request.addOnCompleteListener { task: Task<ReviewInfo?> ->
                    if (task.isSuccessful) {
                        // We can get the ReviewInfo object
                        val reviewInfo = task.result ?: return@addOnCompleteListener
                        val flow = manager.launchReviewFlow(this, reviewInfo)
                        flow.addOnCompleteListener {
                            bindingRoot.drawerLayout.openDrawer(GravityCompat.START)
                        }
                    } else {
                        bindingRoot.drawerLayout.openDrawer(GravityCompat.START)
                    }
                }
            } else {
                bindingRoot.drawerLayout.openDrawer(GravityCompat.START)
            }
            //recreate()
            alertDialog.dismiss()
        }
        dialogBinding.buttonNo.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            alertDialog.dismiss()
            if(plyr1)
                viewModel.multiPlayerRef.child(gameKey).removeValue()
            if (updatePro.matchWinMulti > 2) {
                val manager = ReviewManagerFactory.create(this)
                val request = manager.requestReviewFlow()
                request.addOnCompleteListener { task->
                    if (task.isSuccessful) {
                        // We can get the ReviewInfo object
                        val reviewInfo = task.result
                        val flow = manager.launchReviewFlow(this, reviewInfo!!)
                        flow.addOnCompleteListener {
//                            startActivity(Intent(this, MultiplayerActivity::class.java).putExtra("playerId", playerId))
                            finish()
                        }
                    } else {
//                        startActivity(Intent(this, MultiplayerActivity::class.java).putExtra("playerId", playerId))
                        finish()
                    }
                }
            } else {
//                startActivity(Intent(this, MultiplayerActivity::class.java).putExtra("playerId", playerId))
                finish()
            }
        }
        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        try {
            alertDialog.show()
            lifecycleScope.launch {
                if(win) {
                    for (i in 0 .. coin step 4) {
                        delay(100)
                        dialogBinding.coinWin.text = "+$i"
                    }
                    dialogBinding.coinWin.text = "+$coin"
                }
                else{
                    for (i in 0 downTo  coin step 4) {
                        delay(100)
                        dialogBinding.coinWin.text = "$i"
                    }
                    dialogBinding.coinWin.text = "$coin"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveToFirebase(plr1Cup: String, plr2Cup: String) {
        val firestore = Firebase.firestore
        val plr2CupRef = viewModel.multiPlayerRef.child(gameKey).child("plr2Cup") //he he
        if (!plyr1) {
            plr2CupRef.setValue(plr2Cup)
            return
        }
        val ds = DataStore(
            time = System.currentTimeMillis(),
            redData = "${nm1.split("\n", " ").firstOrNull()}: $scoreRed",
            blueData = "${nm2.split("\n", " ").firstOrNull()}: $scoreBlue",
            starData = "globe",
            plr1Id = plr1Id,
            plr2Id = plr2Id,
            plr1Cup = plr1Cup,
            plr2Cup = "0"
        )
        firestore.collection("LastBestPlayer").document("LastBestPlayer").get()
            .addOnSuccessListener {
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
                data?.let {
                    firestore.collection("LastBestPlayer").document("LastBestPlayer")
                        .update("info", it)
                }
            }
        val key = viewModel.scoreBoardKey
        plr2CupRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ds.plr2Cup = snapshot.getValue<String>() ?: return
                firestore.collection("ScoreBoard").document(key).set(ds)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun ideaBtn() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        infoShow()
    }

    private fun infoShow() {
        if (isFirstRun) prefEditor.putBoolean("firstRun", false).apply()
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
        val builder = AlertDialog.Builder(this@GameActivity2)
        val binding = DialogLayoutInfoBinding.inflate(layoutInflater)
        builder.setView(binding.root)
        builder.setCancelable(false)
        binding.textMessage.text = msg[0]
        binding.playGif.loadDrawable(gifs[0])
        binding.buttonPre.invisible()
        val alertDialog = builder.create()
        binding.buttonPre.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            if (i != 0) i--
            if (i == 0)binding.buttonPre.invisible()
            binding.textMessage.text = msg[i]
            binding.playGif.loadDrawable(gifs[i])
        }
        binding.buttonNext.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            if (i <= 4) i++
            if (!isFirstRun && i == 4) i++
            if (i == 1) binding.buttonPre.show()
            if (i >= 5) alertDialog.dismiss() else {
                binding.textMessage.text = msg[i]
                binding.playGif.loadDrawable(gifs[i])
            }
        }
        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        try {
            alertDialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        private const val TAG = "TAG: GameActivity2"
        var clickCount = 0
        var scoreRed = 0
        var scoreBlue = 0
        var bestScore = 9999
        lateinit var idNm: String
        var fst = "r1c1"
        var top: String? = null
        var left: String? = null
        var circle: String? = null
        var nm1: String = "Red"
        var nm2: String = "Blue"
        var PACKAGE_NAME: String? = null
        var one = true
        var isFirstRun = false
        var plyr1 = false
        var plyrTurn = false

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
    }
}