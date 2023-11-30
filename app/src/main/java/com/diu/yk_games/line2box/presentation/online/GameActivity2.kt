package com.diu.yk_games.line2box.presentation.online

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.ActivityGame2Binding
import com.diu.yk_games.line2box.databinding.ContentGame2Binding
import com.diu.yk_games.line2box.model.DataStore
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.model.MsgStore
import com.diu.yk_games.line2box.util.hideSystemBars
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.droidsonroids.gif.GifImageView
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Objects
import java.util.Random
import java.util.concurrent.atomic.AtomicInteger

@SuppressLint("DiscouragedApi")
class GameActivity2 : AppCompatActivity() {
    private lateinit var bindingRoot: ActivityGame2Binding
    private lateinit var binding: ContentGame2Binding

    private lateinit var sharedPref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private var lvl1: Int = 0
    private var lvl2: Int = 0
    private lateinit var key: String
    private lateinit var playerId: String
    private var redX = 0
    private var redY = 0
    private var blueX = 0
    private var blueY = 0
    val isMuted: Boolean
        get() = sharedPref.getBoolean("muted", false)

    private fun ifMuted() {
        if (isMuted) {
            binding.volBtn.setBackgroundResource(R.drawable.btn_gry_bg)
            binding.volBtn.setImageResource(R.drawable.icon_vol_mute)
        } else {
            binding.volBtn.setBackgroundResource(R.drawable.btn_ylw_bg)
            binding.volBtn.setImageResource(R.drawable.icon_vol_unmute)
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingRoot = ActivityGame2Binding.inflate(layoutInflater)
        binding = bindingRoot.appBarGame2
        window.hideSystemBars()
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(bindingRoot.root)
        redX = ContextCompat.getColor(applicationContext, R.color.redX)
        redY = ContextCompat.getColor(applicationContext, R.color.redY)
        blueX = ContextCompat.getColor(applicationContext, R.color.blueX)
        blueY = ContextCompat.getColor(applicationContext, R.color.blueY)

        sharedPref = getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        editor = sharedPref.edit()
        ifMuted()
        isFirstRun = sharedPref.getBoolean("firstRun", true)
        GameProfile.setPreferences(sharedPref)
        //        DrawerLayout drawer = binding.drawerLayout;
//        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        lifecycleScope.launch { 
            delay(200)
            if (isFirstRun) infoShow()
        }
        PACKAGE_NAME = applicationContext.packageName
        val bundleInfo = intent.getBundleExtra("bundleInfo")
        if(bundleInfo != null) {
            key = bundleInfo.getString("gameKey")!!
            nm1 = bundleInfo.getString("nm1")!!
            nm2 = bundleInfo.getString("nm2")!!
            lvl1 = bundleInfo.getInt("lvl1")
            lvl2 = bundleInfo.getInt("lvl2")
            plyr1 = bundleInfo.getBoolean("plyr1")
            playerId = bundleInfo.getString("playerId")!!
        }
        plyrTurn = plyr1
        binding.nm1Id.text = "($nm1)"
        binding.nm2Id.text = "($nm2)"
        clickCount = 0
        scoreRed = 0
        scoreBlue = 0
        bestScore = 9999
        one = true
        binding.newMsgBoltu.visibility = View.GONE
        binding.emojiPlay.visibility = View.GONE
        bindingRoot.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        bindingRoot.root.addDrawerListener(object : DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {
                //Log.d("TAG", "onDrawerStateChanged: "+newState);
                if (newState == 2) {
                    closeKeyboard()
                    if (!isMuted) {
                        val mediaPlayer = MediaPlayer.create(this@GameActivity2, R.raw.slide)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                    }
                }
            }
        })
        bindingRoot.bubbleTabBar.setSelected(1, true)
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.replace(R.id.chatFragment, ChatFragmentFriendly.newInstance(key, playerId))
        ft.commit()
        bindingRoot.bubbleTabBar.addBubbleListener { id: Int ->
            val fm2 = supportFragmentManager
            val ft2 = fm2.beginTransaction()
            if (id == R.id.globalChat) {
                ft2.replace(R.id.chatFragment, ChatFragmentGlobal.newInstance(playerId))
            } else {
                ft2.replace(R.id.chatFragment, ChatFragmentFriendly.newInstance(key, playerId))
            }
            ft2.commit()
        }
        val activityRootView = this.window.decorView
        activityRootView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            //r will be populated with the coordinates of your view that area still visible.
            activityRootView.getWindowVisibleDisplayFrame(r)
            val maxHight = activityRootView.rootView.height
            val heightDiff = maxHight - r.height()
            //Log.d("TAG", "onGlobalLayout: "+"heidiff: "+heightDiff+" "+r.height()+" "+maxHight);
            val layout1 = findViewById<LinearLayout>(R.id.chatFragmentLinerLayout)
            val layout2 = findViewById<LinearLayout>(R.id.navCloseButtonLayout)
            if (heightDiff > 0.25 * maxHight) {
                // if more than 25% of the screen, its probably a keyboard......do something here
                //Log.d("TAG", "onGlobalLayout: here");
                layout1.setPadding(0, 0, 0, heightDiff)
                layout2.setPadding(0, 0, 0, heightDiff)
            } else {
                layout1.setPadding(0, 0, 0, 0)
                layout2.setPadding(0, 0, 0, 0)
            }
        }
        database = FirebaseDatabase.getInstance()
        myRef = database.getReference("MultiPlayer").child(key).child("matchInfo")
        myRef.child("plyr2").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                if (dataSnapshot.exists() && plyr1) {
                    val viewIdFromServer = dataSnapshot.getValue(String::class.java)!!
                    plyrTurn = true
                    lineClick(findViewById(resources.getIdentifier(viewIdFromServer, "id", packageName)))
                    //Log.d(TAG, "onChildAdded (view): "+getResources().getResourceEntryName(viewFromServer)+" "+plyrTurn);
                }
            }
            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException())
            }
        })
        myRef.child("plyr1").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                if (dataSnapshot.exists() && !plyr1) {
                    val viewIdFromServer = Objects.requireNonNull(dataSnapshot.getValue(String::class.java))
                    plyrTurn = true
                    lineClick(findViewById(resources.getIdentifier(viewIdFromServer, "id", packageName)))
                    //Log.d(TAG, "onChildAdded (view): "+getResources().getResourceEntryName(viewFromServer)+" "+plyrTurn);
                }
            }
            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException())
            }
        })
        
        binding.ideaBtn.setBounceClickListener { 
            ideaBtn()
        }
        binding.backBtn.setBounceClickListener {
            backBtn()
        }
        binding.volBtn.setBounceClickListener {
            volButton()
        }
        binding.openNavBtn.setBounceClickListener {
            bindingRoot.drawerLayout.openDrawer(GravityCompat.START)
        }
        bindingRoot.closeNavBtn.setBounceClickListener {
            closeKeyboard()
            bindingRoot.drawerLayout.closeDrawer(GravityCompat.START)
            findViewById<View>(R.id.newMsgBoltu).visibility = View.GONE
        }
        onBackPressedDispatcher.addCallback{
            val builder = AlertDialog.Builder(this@GameActivity2)
            val view = LayoutInflater.from(this@GameActivity2).inflate(
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
                    val mediaPlayer = MediaPlayer.create(this@GameActivity2, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
                val ms = MsgStore()
                ms.playerId = playerId
                ms.nmData = nm2
                ms.lvlData = lvl2.toString()
                ms.time = System.currentTimeMillis()
                ms.msgData = "Left the match."
                val key2 = myRef.child(key).child("friendlyChat").push().key!!
                myRef.child(key).child("friendlyChat").child(key2).setValue(ms)
                alertDialog.dismiss()
                database.getReference("MultiPlayer").child(key).removeValue()
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
//            startActivity(Intent(this, MultiplayerActivity::class.java).putExtra("playerId", playerId))
//                finish()
            }
            view.findViewById<View>(R.id.buttonNo).setBounceClickListener {
                if (!isMuted) {
                    val mediaPlayer = MediaPlayer.create(this@GameActivity2, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
                alertDialog.dismiss()
            }
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
            try {
                alertDialog.show()
            } catch (npe: Exception) {
                npe.printStackTrace()
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
        //Log.d(TAG, "After lineClick (plyrTurn): "+plyrTurn);
        //Toast.makeText(this, "clicked", Toast.LENGTH_SHORT).show();
        idNm = resources.getResourceEntryName(view.id)
        val bg = view.background as GradientDrawable
        val color = getColorGrad(bg)
        var change = false
        val red = resources.getColor(R.color.redX, theme)
        val blue = resources.getColor(R.color.blueX, theme)
        if (color == resources.getColor(R.color.whiteX, theme) && plyrTurn) {
            if (!isMuted) {
                val mediaPlayer = MediaPlayer.create(this, R.raw.line_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            clickCount++
            if (plyr1 && clickCount % 2 == 1) {
                val key = myRef.child("plyr1").push().key!!
                myRef.child("plyr1").child(key)
                    .setValue(view.resources.getResourceEntryName(view.id))
            } else if (!plyr1 && clickCount % 2 == 0) {
                val key = myRef.child("plyr2").push().key!!
                myRef.child("plyr2").child(key)
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
                        if (!isMuted) {
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
                        bgMidC1.setStroke(
                            14,
                            redY
                        )
                        bgMidC2.setColor(redX)
                        bgMidC2.setStroke(
                            14,
                            redY
                        )
                        bgUpC1.setColor(redX)
                        bgUpC1.setStroke(
                            14,
                            redY
                        )
                        bgUpC2.setColor(redX)
                        bgUpC2.setStroke(
                            14,
                            redY
                        )
                        if (one) {
                            one = false
                            Toast.makeText(this, "Bonus TURN for $nm1", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        if (!isMuted) {
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
                        bgMidC1.setStroke(
                            14,
                            blueY
                        )
                        bgMidC2.setColor(blueX)
                        bgMidC2.setStroke(
                            14,
                            blueY
                        )
                        bgUpC1.setColor(blueX)
                        bgUpC1.setStroke(
                            14,
                            blueY
                        )
                        bgUpC2.setColor(blueX)
                        bgUpC2.setStroke(
                            14,
                            blueY
                        )
                        if (one) {
                            one = false
                            Toast.makeText(this, "Bonus TURN for $nm2", Toast.LENGTH_SHORT).show()
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
                        if (!isMuted) {
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
                        bgMidC1.setStroke(
                            14,
                            blueY
                        )
                        bgMidC2.setColor(blueX)
                        bgMidC2.setStroke(
                            14,
                            blueY
                        )
                        bgDownC1.setColor(blueX)
                        bgDownC1.setStroke(
                            14,
                            blueY
                        )
                        bgDownC2.setColor(blueX)
                        bgDownC2.setStroke(
                            14,
                            blueY
                        )
                        if (one) {
                            one = false
                            Toast.makeText(
                                this,
                                "Bonus TURN for " + binding.blue.text,
                                Toast.LENGTH_SHORT
                            ).show()
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
                val db = FirebaseFirestore.getInstance()
                val doc = db.collection("gamerProfile").document(playerId)
                doc.update("matchPlayed", FieldValue.increment(1))
                val updatePro = GameProfile()
                val winCoin = Random().nextInt(80) + 45
                val lostCoin = Random().nextInt(35) + 15
                updatePro.setMatchPlayed()
                if (!isMuted) {
                    val mediaPlayer = MediaPlayer.create(this, R.raw.win_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
                binding.red.textSize = 30f
                binding.red.setTextColor(resources.getColor(R.color.white, theme))
                binding.blue.textSize = 30f
                binding.blue.setTextColor(resources.getColor(R.color.white, theme))
                val winTxt: String
                val wCoin: String
                var plr1Cup = ""
                var plr2Cup = ""
                if (scoreRed > scoreBlue) {
                    if (plyr1) {
                        doc.update("matchWinMulti", FieldValue.increment(1))
                        updatePro.setMatchWinMulti()
                        updatePro.coin = updatePro.coin + winCoin
                        updatePro.apply()
                        doc.update("coin", updatePro.coin)
                        winTxt = "You won the match."
                        wCoin = "+$winCoin"
                        plr1Cup = wCoin
                        val ms = MsgStore()
                        ms.playerId = playerId
                        ms.nmData = nm2
                        ms.lvlData = lvl2.toString()
                        ms.time = System.currentTimeMillis()
                        ms.msgData = "Won the match."
                        val key2 = myRef.child(key).child("friendlyChat").push().key!!
                        myRef.child(key).child("friendlyChat").child(key2).setValue(ms)
                    } else {
                        updatePro.coin = updatePro.coin - lostCoin
                        updatePro.apply()
                        doc.update("coin", updatePro.coin)
                        winTxt = "You lost the match."
                        wCoin = "-$lostCoin"
                        plr2Cup = wCoin
                    }
                } else if (scoreRed < scoreBlue) {
                    if (!plyr1) {
                        doc.update("matchWinMulti", FieldValue.increment(1))
                        updatePro.setMatchWinMulti()
                        updatePro.coin = updatePro.coin + winCoin
                        updatePro.apply()
                        doc.update("coin", updatePro.coin)
                        winTxt = "You won the match."
                        wCoin = "+$winCoin"
                        plr2Cup = wCoin
                        val ms = MsgStore()
                        ms.playerId = playerId
                        ms.nmData = nm2
                        ms.lvlData = lvl2.toString()
                        ms.time = System.currentTimeMillis()
                        ms.msgData = "Won the match."
                        val key2 = myRef.child(key).child("friendlyChat").push().key!!
                        myRef.child(key).child("friendlyChat").child(key2).setValue(ms)
                    } else {
                        updatePro.coin = updatePro.coin - lostCoin
                        updatePro.apply()
                        doc.update("coin", updatePro.coin)
                        winTxt = "You lost the match."
                        wCoin = "-$lostCoin"
                        plr1Cup = wCoin
                    }
                } else {
                    updatePro.coin = 50
                    updatePro.apply()
                    doc.update("coin", updatePro.coin)
                    winTxt = "Match Draw."
                    wCoin = "+$winCoin"
                }
                doc.update("lvl", updatePro.lvlByCal)
                lifecycleScope.launch { 
                    delay(1200)
                    onGameOver(winTxt, wCoin, updatePro)
                }
                saveToFirebase(plr1Cup, plr2Cup)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun onGameOver(winMsg: String, winCoin: String, updatePro: GameProfile) {
        val coin = winCoin.toInt()
        val win = coin > -1

        val builder = AlertDialog.Builder(this@GameActivity2)
        val view = LayoutInflater.from(this@GameActivity2).inflate(
            R.layout.dialog_layout_game_over, findViewById(R.id.layoutDialogGameOver)
        )
        builder.setView(view)
        builder.setCancelable(false)
        (view.findViewById<View>(R.id.textMessage) as TextView).text = winMsg.toString()
        (view.findViewById<View>(R.id.buttonNo) as Button).text = "Exit"
        (view.findViewById<View>(R.id.buttonYes) as Button).text = "Chat"
        val alertDialog = builder.create()
        view.findViewById<View>(R.id.buttonYes).setBounceClickListener { 
            if (!isMuted) {
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
                        val reviewInfo = task.result
                        val flow = manager.launchReviewFlow(this, reviewInfo!!)
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
            //recreate();
            alertDialog.dismiss()
        }
        view.findViewById<View>(R.id.buttonNo).setBounceClickListener { 
            if (!isMuted) {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            alertDialog.dismiss()
            database.getReference("MultiPlayer").child(key).removeValue()
            if (updatePro.matchWinMulti > 2) {
                val manager = ReviewManagerFactory.create(this)
                val request = manager.requestReviewFlow()
                request.addOnCompleteListener { task: Task<ReviewInfo?> ->
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
        if (alertDialog.window != null) {
            alertDialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }
        try {
            alertDialog.show()
            lifecycleScope.launch {
                if(win) {
                    for (i in 0 .. coin step 4) {
                        delay(100)
                        (view.findViewById<View>(R.id.coinWin) as TextView).text = "+$i"
                    }
                    (view.findViewById<View>(R.id.coinWin) as TextView).text = "+$coin"
                }
                else{
                    for (i in 0 downTo  coin step 4) {
                        delay(100)
                        (view.findViewById<View>(R.id.coinWin) as TextView).text = "$i"
                    }
                    (view.findViewById<View>(R.id.coinWin) as TextView).text = "$coin"
                }
            }
        } catch (npe: Exception) {
            npe.printStackTrace()
        }
    }

    private fun saveToFirebase(plr1Cup: String, plr2Cup: String) {
        val dtf = DateTimeFormatter.ofPattern("dd MMM, hh:mm a")
        val now = LocalDateTime.now()
        val timeData = dtf.format(now)
        val starData = "globe"
        val redData = "${nm1.split("\n")[0]}: $scoreRed"
        val blueData = "${nm2.split("\n")[0]}: $scoreBlue"
        val ds = DataStore(
            System.currentTimeMillis(),
            timeData,
            redData,
            blueData,
            starData,
            playerId,
            "",
            plr1Cup,
            ""
        )
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
                    val arrOfStr = bestScoreData.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    bestScore = arrOfStr[arrOfStr.size - 1].toInt()
                    if (bestScore <= scoreRed) db.collection("LastBestPlayer")
                        .document("LastBestPlayer").update(
                        "info",
                        redData
                    ) else if (bestScore <= scoreBlue) db.collection("LastBestPlayer")
                        .document("LastBestPlayer").update("info", blueData)
                } //else {//Log.d("TAG", "Cached get failed: ", task.getException());}
            }
        //multiple
        val plrInfo = FirebaseDatabase.getInstance().getReference("MultiPlayer").child(
            key
        ).child("playerInfo") //he he
        val myRef =
            FirebaseDatabase.getInstance().getReference("ScoreBoard").child("allScore") //he he
        val key2 = myRef.push().key!!
        if (plyr1) {
            val c = intArrayOf(0)
            plrInfo.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (snapshot.exists()) {
                        if (snapshot.key == "plr2Id") {
                            ds.plr2Id = snapshot.getValue(String::class.java)!!
                            c[0]++
                        } else if (snapshot.key == "plr2Cup") {
                            ds.plr2Cup = snapshot.getValue(String::class.java)!!
                            c[0]++
                        }
                        if (c[0] == 2) db.collection("ScoreBoard").document(key2).set(ds)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
        } else {
            plrInfo.child("plr2Id").setValue(playerId)
            plrInfo.child("plr2Cup").setValue(plr2Cup)
            //db.collection("ScoreBoard").document(key).update("plr2Id",playerId);
            //db.collection("ScoreBoard").document(key).update("plr2Cup",plr2Cup);
        }
    }

    private fun volButton() {
        if (!isMuted) {
            findViewById<View>(R.id.volBtn).setBackgroundResource(R.drawable.btn_gry_bg)
            (findViewById<View>(R.id.volBtn) as ImageButton).setImageResource(R.drawable.icon_vol_mute)
            editor.putBoolean("muted", true).apply()
        } else {
            run {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            findViewById<View>(R.id.volBtn).setBackgroundResource(R.drawable.btn_ylw_bg)
            (findViewById<View>(R.id.volBtn) as ImageButton).setImageResource(R.drawable.icon_vol_unmute)
            editor.putBoolean("muted", false).apply()
        }
    }

    fun ideaBtn() {
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
        val builder = AlertDialog.Builder(this@GameActivity2)
        val view = LayoutInflater.from(this@GameActivity2).inflate(
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
        if (alertDialog.window != null) {
            alertDialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }
        try {
            alertDialog.show()
        } catch (npe: NullPointerException) {
            npe.printStackTrace()
        }
    }

    private fun closeKeyboard() {
        val view = this.currentFocus
        //if (view != null)
        //Log.d("TAG", "closeKeyboard: "+(view instanceof EditText));
        if (view is EditText) {
            val manager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0)
        }
    }

    private fun backBtn() {
        if (!isMuted) {
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
        var database = Firebase.database
        lateinit var myRef: DatabaseReference
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