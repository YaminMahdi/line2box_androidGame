package com.diu.yk_games.line2box.presentation.online

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.widget.doAfterTextChanged
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.ActivityGameMultiBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutProfileBinding
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.model.MsgStore
import com.diu.yk_games.line2box.pref
import com.diu.yk_games.line2box.prefEditor
import com.diu.yk_games.line2box.presentation.BlankFragment
import com.diu.yk_games.line2box.presentation.main.DisplayFragment
import com.diu.yk_games.line2box.util.applyState
import com.diu.yk_games.line2box.util.closeKeyboard
import com.diu.yk_games.line2box.util.getClipBoardData
import com.diu.yk_games.line2box.util.getSystemBars
import com.diu.yk_games.line2box.util.gone
import com.diu.yk_games.line2box.util.hideSystemBars
import com.diu.yk_games.line2box.util.isMuted
import com.diu.yk_games.line2box.util.isNotMuted
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.diu.yk_games.line2box.util.setClipBoardData
import com.diu.yk_games.line2box.util.setNavStatusPadding
import com.diu.yk_games.line2box.util.show
import com.diu.yk_games.line2box.util.toast
import com.google.android.gms.common.images.ImageManager
import com.google.android.gms.games.PlayGames
import com.google.firebase.Firebase
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.firestore.FirebaseFirestore
import io.ghyeok.stickyswitch.widget.StickySwitch
import io.ghyeok.stickyswitch.widget.StickySwitch.OnSelectedChangeListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Suppress("DEPRECATION")
@SuppressLint("SetTextI18n")
class MultiplayerActivity : AppCompatActivity() {
    private lateinit var bindingMain: ActivityGameMultiBinding
    private val binding by lazy { bindingMain.appBarGame2 }
    private lateinit var database: FirebaseDatabase
    lateinit var myRef: DatabaseReference
    var dsList = mutableListOf<String>()
    var nm1: String =""
    var nm2: String =""
    var lvl1: Int = 0
    var lvl2: Int = 0
    private var editing = false
    var mBundle = Bundle()
    lateinit var playerId: String
    var tmpKey: String? = null


    override fun onResume() {
        super.onResume()
        binding.trophyTextId.text = ""+GameProfile().coin
        lvlUpgrade()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.hideSystemBars()
        bindingMain = ActivityGameMultiBinding.inflate(layoutInflater)
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(bindingMain.root)
        bindingMain.root.setNavStatusPadding(binding.multiConstraintLyt, binding.globalScoreFrag)
//        val drawer = binding.drawerLayout
//        val navigationView = binding.navView
        Log.d("TAG", "onCreate: local" + GameProfile().coin)
        binding.trophyTextId.text = ""+GameProfile().coin
        binding.globalScoreFrag.gone()
        binding.newMsgBoltu.gone()
        binding.emojiPlay.gone()
        val tmpNm = GameProfile().nm
        if (pref.getBoolean("needName", true) || tmpNm.contains("Noob"))
            changeNameNeeded()
//        binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        bindingMain.root.addDrawerListener(object : DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {
                Log.d("TAG", "onDrawerStateChanged: $newState")
                if (newState == 2) {
                    closeKeyboard()
                    binding.newMsgBoltu.gone()
                    isNotMuted {
                        val mediaPlayer = MediaPlayer.create(this@MultiplayerActivity, R.raw.slide)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                    }
                }
            }
        })
        playerId = intent.extras!!.getString("playerId")!!
        //chat bug fix
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.replace(R.id.chatFragment, ChatFragmentGlobal.newInstance(playerId))
        ft.commit()
        lvlUpgrade()
        mBundle.putString("playerId", playerId)
        binding.copyPastBtn.setImageResource(R.drawable.icon_paste)
        binding.copyPastBtn.tag = R.drawable.icon_paste
        binding.startMatchBtn.isEnabled = false
        ifMuted()
        dsList = mutableListOf()
        database = Firebase.database
        myRef = database.getReference("MultiPlayer")
        myRef.child((pref.getString("tmpKey", "69"))!!).removeValue()
        myRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                Log.d("addList", "onChildAdded: " + dataSnapshot.key)
                dataSnapshot.key?.let { dsList.add(it) }
            }
            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                dataSnapshot.key?.let { dsList.remove(it) }
            }
            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("TAG", "Failed to read value.", databaseError.toException())
            }
        })
        binding.joinInputId.doAfterTextChanged {txt->
            Log.d("getKey", "afterTextChanged: " + validKey + " " + binding.joinInputId.text.toString().length)
            if (txt?.length == 4) {
                closeKeyboard()
                val newKey = validKey
                if (!newKey.isNullOrEmpty()) {
                    mBundle.putString("gameKey", newKey)
                    myRef.child(newKey).child("playerCount")
                        .addValueEventListener(object : ValueEventListener {
                            @SuppressLint("SetTextI18n")
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    val playerCount = dataSnapshot.getValue(String::class.java)?.toIntOrNull()
                                    if (playerCount == 1) {
                                        amiThePayer = true
                                        myRef.child(newKey).child("playerCount")
                                            .setValue("2")
                                        //playerCountLocal=2;
                                        binding.startMatchBtn.isEnabled = true
                                        tmpKey = newKey
                                        prefEditor.putString("tmpKey", newKey).apply()
                                        myRef.child(newKey).child("playerInfo").child("nm2")
                                            .setValue(GameProfile().nm)
                                        myRef.child(newKey).child("playerInfo")
                                            .child("lvl2").setValue(GameProfile().lvlByCal)
                                        val ms = MsgStore()
                                        ms.playerId = playerId
                                        ms.nmData = nm2
                                        ms.lvlData = lvl2.toString()
                                        val dtf = DateTimeFormatter.ofPattern("dd MMM, hh:mm a")
                                        val now = LocalDateTime.now()
                                        ms.timeData = dtf.format(now)
                                        ms.msgData = "Joined the match."
                                        val key2 = myRef.child(newKey).child("friendlyChat")
                                            .push().key!!
                                        myRef.child(newKey).child("friendlyChat")
                                            .child(key2).setValue(ms)
                                        bindingMain.bubbleTabBar.setSelected(1, true)
                                        val ft2 = fm.beginTransaction()
                                        ft2.replace(R.id.chatFragment, ChatFragmentFriendly.newInstance(tmpKey, playerId))
                                        ft2.commit()
                                        myRef.child(newKey).child("playerInfo")
                                            .addValueEventListener(object : ValueEventListener {
                                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                    if (dataSnapshot.exists()) {
                                                        nm1 = dataSnapshot.child("nm1").getValue(String::class.java) ?: "No Name"
                                                        lvl1 = dataSnapshot.child("lvl1").getValue(Int::class.java) ?: 0
                                                        mBundle.putString("nm1", nm1)
                                                        mBundle.putInt("lvl1", lvl1)
                                                    }
                                                }
                                                override fun onCancelled(error: DatabaseError) {}
                                            })
                                    }else if(!amiThePayer){
                                        binding.startMatchBtn.isEnabled = false
                                        toast("Match already started")
                                    }
                                } else{
                                    binding.startMatchBtn.isEnabled = false
                                    toast("Invalid Key")
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {
                                Log.w("TAG", "Failed to read value.", error.toException())
                                toast("Server Error")
                            }
                        })
                }
                else toast("Invalid Key")
            }
        }
        nm2 = GameProfile().nm
        lvl2 = GameProfile().lvlByCal
        Log.d("TAG left", "ver: $nm1 $lvl1")
        mBundle.putString("nm2", nm2)
        mBundle.putInt("lvl2", lvl2)
        mBundle.putBoolean("plyr1", false)
        val stickySwitch = findViewById<StickySwitch>(R.id.sticky_switch)
        stickySwitch.onSelectedChangeListener =
            object : OnSelectedChangeListener {
                override fun onSelectedChange(direction: StickySwitch.Direction, text: String) {
                    amiThePayer = false
                    when (direction) {
                        StickySwitch.Direction.LEFT -> {
                            binding.joinInputId.isEnabled = true
                            binding.startMatchBtn.isEnabled = false
                            binding.joinInputId.hint = ""
                            binding.joinInputId.setText("")
                            mBundle.putBoolean("plyr1", false)
                            nm2 = GameProfile().nm
                            lvl2 = GameProfile().lvlByCal
                            Log.d("TAG left", "ver: $nm1 $lvl1")
                            mBundle.putString("nm2", nm2)
                            mBundle.putInt("lvl2", lvl2)
                            val ft2: FragmentTransaction = fm.beginTransaction()
                            ft2.replace(R.id.chatFragment, ChatFragmentGlobal.newInstance(playerId))
                            ft2.commit()
                            findViewById<View>(R.id.newMsgBoltu).gone()
                            lifecycleScope.launch {
                                delay(400)
                                binding.joinInputId.hint = "Game ID"
                                binding.copyPastBtn.setImageResource(R.drawable.icon_paste)
                                binding.copyPastBtn.tag = R.drawable.icon_paste
                                stickySwitch.switchColor = -0xdc8e06
                                if (key != null) {
                                    myRef.child(key!!).removeValue()
                                    key = null
                                }
                            }
                        }
                        StickySwitch.Direction.RIGHT -> {
                            binding.joinInputId.isEnabled = false
                            binding.startMatchBtn.isEnabled = false
                            binding.joinInputId.hint = ""
                            binding.joinInputId.setText("")
                            mBundle.putBoolean("plyr1", true)
                            nm1 = GameProfile().nm
                            lvl1 = GameProfile().lvlByCal
                            Log.d("TAG", "ver: $nm1 $lvl1")
                            mBundle.putString("nm1", nm1)
                            mBundle.putInt("lvl1", lvl1)
                            key = myRef.push().key
                            prefEditor.putString("tmpKey", key).apply()
                            Log.d("TAG", "onCreate key: $key")
                            mBundle.putString("gameKey", key)
                            assert(key != null)
                            myRef.child((key)!!).child("playerCount").setValue("1")
                            myRef.child((key)!!).child("playerInfo").child("nm1")
                                .setValue(GameProfile().nm)
                            myRef.child((key)!!).child("playerInfo").child("lvl1")
                                .setValue(GameProfile().lvlByCal)
                            myRef.child((key)!!).child("playerInfo").child("nm2")
                                .setValue("")
                            myRef.child((key)!!).child("playerInfo").child("lvl2")
                                .setValue(0)
                            val ms = MsgStore()
                            ms.playerId = (playerId)
                            ms.nmData = nm1
                            ms.lvlData = lvl1.toString()
                            val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM, hh:mm a")
                            val now: LocalDateTime = LocalDateTime.now()
                            ms.timeData = dtf.format(now)
                            ms.msgData = "Created the match."
                            val key2: String? = myRef.child((key)!!).child("friendlyChat").push().key!!
                            myRef
                                .child((key)!!)
                                .child("friendlyChat")
                                .child((key2)!!)
                                .setValue(ms)
                            bindingMain.bubbleTabBar.setSelected(1, true)
                            val ft2: FragmentTransaction = fm.beginTransaction()
                            ft2.replace(R.id.chatFragment, ChatFragmentFriendly.newInstance(key, playerId))
                            ft2.commit()
                            myRef.child(key!!)
                                .addValueEventListener(object : ValueEventListener {
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            Log.d("TAG -int key", "onDataChange: " + key + " " + validKey + " " + dataSnapshot.child("playerCount").getValue(String::class.java))
                                            try {
                                                val playerCount = dataSnapshot.child("playerCount").getValue(String::class.java)?.toInt()!!
                                                nm2 = dataSnapshot.child("playerInfo").child("nm2").getValue(String::class.java)!!
                                                lvl2 = dataSnapshot.child("playerInfo").child("lvl2").getValue(Int::class.java) ?: 0
                                                Log.d("TAG", "ver2: $nm2 $lvl2")
                                                mBundle.putString("nm2", nm2)
                                                mBundle.putInt("lvl2", (lvl2))
                                                if (playerCount == 2) {
                                                    binding.startMatchBtn.isEnabled = true
                                                    //playerCountLocal=2;
                                                }
                                            } catch (npe: NullPointerException) {
                                                npe.printStackTrace()
                                            }
                                        } else binding.startMatchBtn.isEnabled = false
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        // Failed to read value
                                        Log.w("TAG", "Failed to read value.", error.toException())
                                    }
                                })
                            lifecycleScope.launch{
                                delay(400)
                                binding.joinInputId.hint = getKey4(key)
                                binding.copyPastBtn.setImageResource(R.drawable.icon_copy)
                                binding.copyPastBtn.tag = R.drawable.icon_copy
                                stickySwitch.switchColor =
                                    ContextCompat.getColor(applicationContext, R.color.greenY)
                                if (tmpKey != null) {
                                    myRef.child(tmpKey!!).removeValue()
                                    tmpKey = null
                                }
                            }
                        }
                    }

                }
            }
        binding.copyPastBtn.setBounceClickListener {
            isNotMuted {
                val mediaPlayer: MediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            when (stickySwitch.getDirection()) {
                StickySwitch.Direction.RIGHT ->  {
                    setClipBoardData(getKey4(key), "ID copied")
                }
                StickySwitch.Direction.LEFT -> {
                    // Access your context here using YourActivityName.this
                    binding.joinInputId.setText(getClipBoardData())
                }
            }
        }

        val activityRootView = window.decorView
        activityRootView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            //r will be populated with the coordinates of your view that area still visible.
            activityRootView.getWindowVisibleDisplayFrame(r)
            val maxHight = activityRootView.height
            val heightDiff = maxHight - r.height()
            Log.d("TAG", "onGlobalLayout: " + "heidiff: " + heightDiff + " " + r.height() + " " + maxHight)
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
        bindingMain.bubbleTabBar.addBubbleListener { id: Int ->
            val ft2: FragmentTransaction = fm.beginTransaction()
            if (id == R.id.globalChat) {
                ft2.replace(R.id.chatFragment, ChatFragmentGlobal.newInstance(playerId))
            } else {
                if (key != null)
                    ft2.replace(R.id.chatFragment, ChatFragmentFriendly.newInstance(key, playerId)
                ) else
                    ft2.replace(R.id.chatFragment, BlankChatFragment())
            }
            ft2.commit()
        }
        onBackPressedDispatcher.addCallback{
            if(bindingMain.drawerLayout.isDrawerOpen(GravityCompat.START))
                bindingMain.drawerLayout.closeDrawer(GravityCompat.START)
            else if (scrBrdVisible) {
                onGoBack()
            } else {
                val builder = AlertDialog.Builder(this@MultiplayerActivity)
                val view = LayoutInflater.from(this@MultiplayerActivity).inflate(
                    R.layout.dialog_layout_alert, findViewById(R.id.layoutDialog)
                )
                builder.setView(view)
                (view.findViewById<View>(R.id.textMessage) as TextView).text =
                    "Do you really want to go back?"
                (view.findViewById<View>(R.id.buttonYes) as Button).text = "YES"
                (view.findViewById<View>(R.id.buttonNo) as Button).text = "NO"
                val alertDialog = builder.create()
                view.findViewById<View>(R.id.buttonYes).setBounceClickListener {
                    isNotMuted {
                        val mediaPlayer: MediaPlayer = MediaPlayer.create(this@MultiplayerActivity, R.raw.btn_click_ef)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener (MediaPlayer::release)
                    }
                    alertDialog.dismiss()
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
//                super.onBackPressed()
//                startActivity(Intent(this, StartActivity::class.java))
//                        finish()
                }
                view.findViewById<View>(R.id.buttonNo).setBounceClickListener {
                    isNotMuted {
                        val mediaPlayer: MediaPlayer = MediaPlayer.create(this@MultiplayerActivity, R.raw.btn_click_ef)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                    }
                    alertDialog.dismiss()
                }
                alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
                try {
                    alertDialog.show()
                } catch (npe: NullPointerException) {
                    npe.printStackTrace()
                }
            }
        }
        binding.backBtn.setBounceClickListener{
            backBtn()
        }
        binding.goBackBtn.setBounceClickListener{
            goBack()
        }
        binding.scoreBoardBtn.setBounceClickListener{
            scoreBoard()
        }
        binding.leaderBoardBtn.setBounceClickListener{
            leaderBoard()
        }
        binding.volBtn.setBounceClickListener{
            volButton()
//            val x = GameProfile()     // testing
//            x.coin = x.coin + 69
//            x.matchPlayed = x.matchPlayed + 50
//            x.apply()
        }
        binding.ideaBtn.setBounceClickListener{
            ideaBtn()
        }
        binding.profileBtn.setBounceClickListener{
            profileBtn()
        }
        binding.startMatchBtn.setBounceClickListener{
            startBtn()
        }
        binding.openNavBtn.setBounceClickListener{
            openNavBtn()
        }
        bindingMain.closeNavBtn.setBounceClickListener{
            closeNavBtn()
        }
    }

    fun getKey4(key: String?): String {
        val key4 = StringBuilder("")
        var i = 4
        var j = 7
        key?.let {
            while (i <= j) {
                if ((key[i] == '0') || (key[i] == 'O') || (key[i] == 'o')) key4.append('M') else {
                    if (key[i] != '-' && key[i] != '_') key4.append(key[i]) else j++
                }
                i++
            }
        }
        return key4.toString().uppercase()
    }

    val validKey: String?
        get() {
            val input = binding.joinInputId.text.toString().uppercase()
            for (i in dsList.indices) {
                if ((getKey4(dsList[i]) == input)) {
                    key = dsList[i]
                    return dsList[i]
                }
            }
            return null
        }

    @SuppressLint("SetTextI18n")
    fun lvlUpgrade() {
        val pf = GameProfile()
        val tmpLvl = pref.getInt("tmpLvl", 1)
        if (tmpLvl != pf.lvlByCal) {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(this, R.raw.win_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            FirebaseFirestore.getInstance().collection("gamerProfile").document((playerId))
                .update("lvl", pf.lvlByCal)
            prefEditor.putInt("tmpLvl", pf.lvlByCal).apply()
            val builder = AlertDialog.Builder(this)
            val v = LayoutInflater.from(this).inflate(
                R.layout.dialog_layout_update, findViewById(R.id.updateLayoutDialog)
            )
            builder.setView(v)
            builder.setCancelable(false)
            val alertDialog = builder.create()
            (v.findViewById<View>(R.id.warningMessage) as TextView).text = "Level Upgraded !"
            val updateInfo = v.findViewById<TextView>(R.id.UpdateInfo)
            updateInfo.text = tmpLvl.toString() + " --> " + pf.lvlByCal
            updateInfo.typeface = resources.getFont(R.font.baloopaaji)
            updateInfo.textSize = 25f
            (v.findViewById<View>(R.id.buttonUpdate) as Button).text = "Continue"
            v.findViewById<View>(R.id.buttonUpdate).setBounceClickListener {
                isNotMuted {
                    val mediaPlayer: MediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
                alertDialog.dismiss()
            }
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
            try {
                alertDialog.show()
            } catch (npe: NullPointerException) {
                npe.printStackTrace()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun changeNameNeeded() {
        toast("Change Your Name.")
        prefEditor.putBoolean("muted", true).apply()
        profileBtn{binding->
            prefEditor.putBoolean("muted", false).apply()
            lifecycleScope.launch {
                delay(250)
                binding.nmTxt.isEnabled = true
                editing = true
                binding.nmEditBtn.setImageResource(R.drawable.icon_save)
                if (binding.nmTxt.requestFocus()) {
                    val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY)
                    binding.nmTxt.setSelection(binding.nmTxt.text.length)
                }
            }

        }
    }

    private fun ifMuted() {
        lifecycleScope.launch {
            binding.volBtn.applyState(isMuted())
        }
    }

    private fun goBack() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        onGoBack()
    }

    private fun onGoBack() {
        scrBrdVisible = false
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.replace(R.id.disFragment, BlankFragment())
        ft.commit()
        binding.multiConstraintLyt.show()
        binding.globalScoreFrag.gone()
    }

    private fun volButton() {
        isNotMuted( ifTrue = {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            binding.volBtn.setBackgroundResource(R.drawable.btn_ylw_bg)
            binding.volBtn.setImageResource(R.drawable.icon_vol_unmute)
            prefEditor.putBoolean("muted", false).apply()
        }, ifNotTrue = {
            binding.volBtn.setBackgroundResource(R.drawable.btn_gry_bg)
            binding.volBtn.setImageResource(R.drawable.icon_vol_mute)
            prefEditor.putBoolean("muted", true).apply()
        })
    }

    fun ideaBtn() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        val builder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(
            R.layout.dialog_layout_info_mul, findViewById(R.id.infoMultiLayoutDialog)
        )
        builder.setView(view)
        builder.setCancelable(false)
        val alertDialog = builder.create()
        view.findViewById<View>(R.id.buttonOkey).setBounceClickListener {
            isNotMuted {
                val mediaPlayer: MediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            alertDialog.dismiss()
        }
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
        try {
            alertDialog.show()
        } catch (npe: NullPointerException) {
            npe.printStackTrace()
        }
    }

    private fun scoreBoard() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        scrBrdVisible = true
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.replace(R.id.disFragment, DisplayFragment())
        ft.commit()
        findViewById<View>(R.id.multiConstraintLyt).gone()
        (findViewById<View>(R.id.FragLabel) as TextView).text = "Global Score Board"
        findViewById<View>(R.id.globalScoreFrag).show()
    }

    private fun leaderBoard() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        scrBrdVisible = true
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.replace(R.id.disFragment, LeaderBoardFragment.newInstance(playerId))
        ft.commit()
        findViewById<View>(R.id.multiConstraintLyt).gone()
        (findViewById<View>(R.id.FragLabel) as TextView).text = "Global Rank List"
        findViewById<View>(R.id.globalScoreFrag).show()
    }

    private fun profileBtn(onCreated : (DialogLayoutProfileBinding) -> Unit = {}) {
        editing = false
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        val builder = AlertDialog.Builder(this@MultiplayerActivity)
        val bindingProfileDialog = DialogLayoutProfileBinding.inflate(LayoutInflater.from(this))
        onCreated.invoke(bindingProfileDialog)
//        val viewProfileDialog = LayoutInflater.from(this@MultiplayerActivity).inflate(
//            R.layout.dialog_layout_profile, findViewById(R.id.profileLayoutDialog)
//        )
        builder.setView(bindingProfileDialog.root)
        val alertDialog = builder.create()

        //builder.setCancelable(false);
        val x = GameProfile()
        x.countryEmoji = (pref.getString("countryEmoji", ""))!!
        x.countryNm = (pref.getString("countryNm", ""))!!
        bindingProfileDialog.apply {
            countryTxt.text = x.countryNm + " " + x.countryEmoji
            lvlTxt.text = "" + x.lvlByCal
            matchPlayedTxt.text = "" + x.matchPlayed
            matchWonTxt.text = "" + x.matchWinMulti
            coinShow.gone()
            val mgr = ImageManager.create(this@MultiplayerActivity)
            PlayGames.getPlayersClient(this@MultiplayerActivity).currentPlayer.addOnSuccessListener { player ->
                Log.d("TAG", "profileBtn: " + player.displayName)
                Log.d("TAG", "profileBtn: " + player.playerId)
                mgr.loadImage(profileImage, player.iconImageUri!!)
            }
            val oldName = GameProfile().nm
            Log.d("TAG", "profileBtn nm: " + pref.getString("nm", "x"))
            nmTxt .setText(oldName)
            val db = FirebaseFirestore.getInstance()
            profileShapeLayout.setBounceClickListener {
                var intent: Intent = Intent(Intent.ACTION_VIEW).setClassName(
                    "com.google.android.play.games",
                    "com.google.android.gms.games.ui.destination.main.MainActivity"
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    intent = Intent(Intent.ACTION_VIEW)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    intent.data = Uri.parse("market://details?id=" + "com.google.android.play.games")
                    startActivity(intent)
                }
                startActivity(intent)
            }
            nmEditBtn.setBounceClickListener {
                isNotMuted {
                    val mediaPlayer: MediaPlayer = MediaPlayer.create(this@MultiplayerActivity, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
                if (!editing) {
                    nmTxt.isEnabled = true
                    nmTxt.setSelection(nmTxt.text.length)
                    nmEditBtn.setImageResource(R.drawable.icon_save)
                    if (nmTxt.requestFocus()) {
                        val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY)
                    }
                    editing = true
                } else {
                    closeKeyboard()
                    val newNm: String = nmTxt.text.toString()
                     if (newNm.length <2) {
                         toast("Can't be single character.")
                         nmTxt.setText(oldName)
                    } else {
                        val z = GameProfile()
                        z.nm = newNm
                        z.apply()
                        Log.d("TAG", "profileBtn: $playerId")
                        db
                            .collection("gamerProfile")
                            .document(playerId)
                            .update("nm", "" + newNm)
                        nmTxt.isEnabled = false
                        nmEditBtn.setImageResource(R.drawable.icon_edit)
                        editing = false
                        prefEditor.putBoolean("needName", false).apply()
                    }
                }
            }
            buttonSaveInfo.setBounceClickListener {
                isNotMuted {
                    val mediaPlayer: MediaPlayer = MediaPlayer.create(this@MultiplayerActivity, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
                closeKeyboard()
                val newNm: String = nmTxt.text.toString()
                if ((newNm == "")) {
                    toast("Can't be empty.")
                    nmTxt.setText("" + oldName)
                } else if (newNm.length == 1) {
                    toast("Can't be single character.")
                    nmTxt.setText("" + oldName)
                } else {
                    val z = GameProfile()
                    z.nm = newNm
                    z.apply()
                    Log.d("TAG", "profileBtn: $playerId")
                    db
                        .collection("gamerProfile")
                        .document(playerId)
                        .update("nm", "" + newNm)
                    nmTxt.isEnabled = false
                    nmEditBtn.setImageResource(R.drawable.icon_edit)
                    editing = false
                    prefEditor.putBoolean("needName", false).apply()
                    alertDialog.dismiss()
                }
            }
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
        }

        try {
            alertDialog.show()
        } catch (npe: NullPointerException) {
            npe.printStackTrace()
        }
    }

    private fun closeNavBtn() {
        closeKeyboard()
        bindingMain.root.closeDrawer(GravityCompat.START)
        findViewById<View>(R.id.newMsgBoltu).gone()
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    private fun openNavBtn() {
        bindingMain.root.openDrawer(GravityCompat.START)

        when(binding.stickySwitch.getDirection()){
            StickySwitch.Direction.LEFT ->
                bindingMain.bubbleTabBar.setSelected(0,true)
            StickySwitch.Direction.RIGHT ->
                bindingMain.bubbleTabBar.setSelected(1,true)
        }

//        FragmentManager fm=getSupportFragmentManager();
//        FragmentTransaction ft=fm.beginTransaction();
//        if(playerCountLocal==1 && findViewById(R.id.newMsgBoltu).getVisibility() != View.VISIBLE)
//        {
//            bubbleTabBar.setSelected(0,true);
//            ft.replace(R.id.chatFragment,new ChatFragmentGlobal());
//        }
//        else
//        {
//            bubbleTabBar.setSelected(1,true);
//            if(key!=null)
//                ft.replace(R.id.chatFragment,ChatFragmentFriendly.newInstance(key));
//            else
//                ft.replace(R.id.chatFragment,new BlankFragment());
//        }
//        ft.commit();
        findViewById<View>(R.id.newMsgBoltu).gone()


        /*        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);*/
        //show keyboard


        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    }

    private fun backBtn() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        onBackPressedDispatcher.onBackPressed()
    }

    private fun startBtn() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        val mIntent = Intent(this, GameActivity2::class.java)
        startActivity(mIntent.putExtra("bundleInfo", mBundle))
//        finish()
    }

    companion object {
        var scrBrdVisible = false
        var key: String? = null
        var amiThePayer = false
    }
}