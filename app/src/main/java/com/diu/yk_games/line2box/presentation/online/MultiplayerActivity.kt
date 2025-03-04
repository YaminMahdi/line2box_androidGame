package com.diu.yk_games.line2box.presentation.online

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.Rect
import android.media.MediaPlayer
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.GravityCompat
import androidx.core.widget.doAfterTextChanged
import androidx.customview.widget.ViewDragHelper
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.ActivityGameMultiBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutAlertBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutInfoMulBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutProfileBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutUpdateBinding
import com.diu.yk_games.line2box.model.GameRoom
import com.diu.yk_games.line2box.model.MsgStore
import com.diu.yk_games.line2box.model.PlayerInfoOld
import com.diu.yk_games.line2box.model.toMessage
import com.diu.yk_games.line2box.model.toPlayerInfo
import com.diu.yk_games.line2box.pref
import com.diu.yk_games.line2box.presentation.BlankFragment
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.presentation.ViewPagerAdapter
import com.diu.yk_games.line2box.presentation.main.DisplayFragment
import com.diu.yk_games.line2box.util.Constants
import com.diu.yk_games.line2box.util.applyState
import com.diu.yk_games.line2box.util.closeKeyboard
import com.diu.yk_games.line2box.util.getClipBoardData
import com.diu.yk_games.line2box.util.getSystemBars
import com.diu.yk_games.line2box.util.gone
import com.diu.yk_games.line2box.util.hideSystemBars
import com.diu.yk_games.line2box.util.isMuted
import com.diu.yk_games.line2box.util.isNotMuted
import com.diu.yk_games.line2box.util.performOnClick
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.diu.yk_games.line2box.util.setNavStatusPadding
import com.diu.yk_games.line2box.util.show
import com.diu.yk_games.line2box.util.showOnMarket
import com.diu.yk_games.line2box.util.toast
import com.google.android.gms.common.images.ImageManager
import com.google.android.gms.games.PlayGames
import com.google.firebase.Firebase
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.firestore.firestore
import io.ghyeok.stickyswitch.widget.StickySwitch
import io.ghyeok.stickyswitch.widget.StickySwitch.OnSelectedChangeListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@SuppressLint("SetTextI18n")
class MultiplayerActivity : AppCompatActivity() {
    private lateinit var bindingMain: ActivityGameMultiBinding
    private val binding by lazy { bindingMain.appBarGame2 }
    private val viewModel: MainViewModel by viewModels()
//    var nm1: String =""
//    var nm2: String =""
//    var lvl1: Int = 0
//    var lvl2: Int = 0
    private var editing = false
    var mBundle = Bundle()
    lateinit var playerId: String


    override fun onResume() {
        super.onResume()
        viewModel.initGameProfile()
        binding.trophyTextId.text = ""+viewModel.gameProfile.coin
        lvlUpgrade()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.hideSystemBars()
        bindingMain = ActivityGameMultiBinding.inflate(layoutInflater)
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        setContentView(bindingMain.root)
        setNavStatusPadding(binding.multiConstraintLyt, binding.globalScoreFrag)
        intent.extras?.getString("playerId")?.let {
            playerId = it
            viewModel.initGameProfile(it)
            mBundle.putString("plr2Id", playerId)
            mBundle.putString("nm2", viewModel.gameProfile.nm)
            mBundle.putInt("lvl2", viewModel.gameProfile.lvlByCal)
            mBundle.putBoolean("plyr1", false)
        }
        Log.d("TAG", "onCreate: local" + viewModel.gameProfile.coin)
        binding.trophyTextId.text = ""+viewModel.gameProfile.coin
        binding.globalScoreFrag.gone()
        binding.newMsgBoltu.gone()
        binding.emojiPlay.gone()
        val tmpNm = viewModel.gameProfile.nm
        if (pref.getBoolean("needName", true) || tmpNm.contains("Noob"))
            changeNameNeeded()
//        binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        bindingMain.root.addDrawerListener(object : DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {
                Log.d("TAG", "onDrawerStateChanged: $newState")
                if (newState == ViewDragHelper.STATE_SETTLING) {
                    closeKeyboard()
                    binding.newMsgBoltu.gone()
                    if(viewModel.ignoreDrawerClosesSound) {
                        viewModel.ignoreDrawerClosesSound = false
                        return
                    }
                    isNotMuted {
                        val mediaPlayer = MediaPlayer.create(this@MultiplayerActivity, R.raw.slide)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                    }
                }
            }
        })
        //chat bug fix
        bindingMain.chatPager.isUserInputEnabled = false
        bindingMain.chatPager.adapter = ViewPagerAdapter(
            listOf(
                ChatFragmentGlobal(),
                ChatFragmentFriendly(),
                BlankChatFragment()
            ), this
        )
        bindingMain.bubbleTabBar.addBubbleListener { id: Int ->
            if (id == R.id.globalChat)
                bindingMain.chatPager.currentItem = 0
            else
                bindingMain.chatPager.currentItem = if (viewModel.matchKey.isNotEmpty()) 1 else 2
        }
        bindingMain.chatPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> bindingMain.bubbleTabBar.setSelected(0, true)
                    1 -> bindingMain.bubbleTabBar.setSelected(1, true)
                }
            }
        })
//        val fm = supportFragmentManager
//        fm.beginTransaction()
//            .replace(R.id.chatFragment, ChatFragmentGlobal.newInstance(playerId))
//            .commit()
        lvlUpgrade()
//        mBundle.putString("playerId", playerId)
        binding.copyPastBtn.setImageResource(R.drawable.icon_paste)
        binding.copyPastBtn.tag = R.drawable.icon_paste
        binding.startMatchBtn.isEnabled = false
        ifMuted()
        viewModel.removeTempMatch()
        viewModel.multiPlayerRef.limitToLast(100).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                Log.d("addList", "onChildAdded: " + dataSnapshot.key)
                dataSnapshot.key?.let { viewModel.matchKeys.add(it) }
            }
            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                dataSnapshot.key?.let { viewModel.matchKeys.remove(it) }
            }
            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("TAG", "Failed to read value.", databaseError.toException())
            }
        })
        binding.joinInputId.doAfterTextChanged { txt->
            if (txt?.length != 4) return@doAfterTextChanged
            val newKey = viewModel.getValidKey(txt.toString()) ?: run {
                toast("Invalid Key")
                return@doAfterTextChanged
            }
            Log.d("getKey", "afterTextChanged: " + newKey + " " + binding.joinInputId.text.toString().length)
            closeKeyboard()
            mBundle.putString("gameKey", newKey)
            viewModel.multiPlayerRef.child(newKey)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    @SuppressLint("SetTextI18n")
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val gameRoom = dataSnapshot.getValue<GameRoom>() ?: run {
                            binding.startMatchBtn.isEnabled = false
                            toast("Invalid Key")
                            return
                        }
                        if (gameRoom.playerCount == "1") {
                            amiThePayer = true
                            viewModel.multiPlayerRef.child(newKey).child("playerCount")
                                .setValue("2")
                            //playerCountLocal=2;
                            binding.startMatchBtn.isEnabled = true
                            viewModel.addTempKey(newKey)
                            //remove
                            viewModel.multiPlayerRef.child(newKey).child("playerInfo").child("nm2")
                                .setValue(viewModel.gameProfile.nm)
                            viewModel.multiPlayerRef.child(newKey).child("playerInfo")
                                .child("lvl2").setValue(viewModel.gameProfile.lvlByCal)
                            //remove
                            viewModel.multiPlayerRef.child(newKey).child("player2")
                                .setValue(viewModel.gameProfile.toPlayerInfo())
//                                    nm1 = gameRoom.player1.nm
//                                    lvl1 = gameRoom.player1.lvl
                            mBundle.putString("plr1Id", gameRoom.player1.id.ifEmpty { gameRoom.playerInfo.plr1Id }) // remove ifEmpty someday
                            mBundle.putString("nm1", gameRoom.player1.nm.ifEmpty { gameRoom.playerInfo.nm1 })
                            mBundle.putInt("lvl1", if(gameRoom.player1.lvl == 0) gameRoom.playerInfo.lvl1 else gameRoom.player1.lvl)
                            val ms = viewModel.gameProfile.toMessage(
                                playerId = playerId,
                                msg = "Joined the match.",
                                type = MsgStore.Type.EnterText
                            )
                            key = newKey
                            viewModel.multiPlayerRef.child(newKey).child("friendlyChat")
                                .push().setValue(ms)
                            viewModel.matchKey = newKey
                            bindingMain.bubbleTabBar.setSelected(1, true)
//                                        fm.beginTransaction()
//                                            .replace(R.id.chatFragment, ChatFragmentFriendly.newInstance(newKey, playerId))
//                                            .commit()
                        } else if(!amiThePayer){
                            binding.startMatchBtn.isEnabled = false
                            toast("Match already started")
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.w("TAG", "Failed to read value.", error.toException())
                        toast("Server Error")
                    }
                })
        }
//        viewModel.gameProfile.let {
//            nm2 = it.nm
//            lvl2 = it.lvlByCal
//        }
//        Log.d("TAG left", "ver: $nm1 $lvl1")
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
//                            viewModel.gameProfile.let {
//                                nm2 = it.nm
//                                lvl2 = it.lvlByCal
//                            }
//                            Log.d("TAG left", "ver: $nm1 $lvl1")
//                            mBundle.putString("nm2", nm2)
//                            mBundle.putInt("lvl2", lvl2)
//                            fm.beginTransaction()
//                                .replace(R.id.chatFragment, ChatFragmentGlobal.newInstance(playerId))
//                                .commit()

                            bindingMain.bubbleTabBar.setSelected(0, true)
                            binding.newMsgBoltu.gone()
                            viewModel.clearTempMatches()
                            lifecycleScope.launch {
                                delay(400)
                                binding.joinInputId.hint = "Game ID"
                                binding.copyPastBtn.setImageResource(R.drawable.icon_paste)
                                binding.copyPastBtn.tag = R.drawable.icon_paste
                                stickySwitch.switchColor = -0xdc8e06
                            }
                        }
                        StickySwitch.Direction.RIGHT -> {
                            binding.joinInputId.isEnabled = false
                            binding.startMatchBtn.isEnabled = false
                            binding.joinInputId.hint = ""
                            binding.joinInputId.setText("")
                            mBundle.putBoolean("plyr1", true)
//                            nm1 = viewModel.gameProfile.nm
//                            lvl1 = viewModel.gameProfile.lvlByCal
//                            Log.d("TAG", "ver: $nm1 $lvl1")
                            mBundle.putString("plr1Id", playerId)
                            mBundle.putString("nm1", viewModel.gameProfile.nm)
                            mBundle.putInt("lvl1", viewModel.gameProfile.lvlByCal)
                            key = viewModel.multiPlayerRef.push().key
                            viewModel.addTempKey(key)
                            Log.d("TAG", "onCreate key: $key")
                            mBundle.putString("gameKey", key)
                            val gameRoom = GameRoom(
                                player1 = viewModel.gameProfile.toPlayerInfo(),
                                playerInfo = PlayerInfoOld(nm1 = viewModel.gameProfile.nm, lvl1 = viewModel.gameProfile.lvlByCal, plr1Id = playerId)
                            )
                            viewModel.multiPlayerRef.child(key!!).setValue(gameRoom)
                            viewModel.multiPlayerRef.child(key!!)
                                .child("friendlyChat")
                                .push()
                                .setValue(viewModel.gameProfile.toMessage(
                                    playerId = playerId,
                                    msg = "Created the match.",
                                    type = MsgStore.Type.EnterText
                                ))
                            viewModel.matchKey = key.toString()
                            bindingMain.bubbleTabBar.setSelected(1, true)
//                            fm.beginTransaction()
//                                .replace(R.id.chatFragment, ChatFragmentFriendly.newInstance(key, playerId))
//                                .commit()
                            viewModel.multiPlayerRef.child(key!!)
                                .addValueEventListener(object : ValueEventListener {
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        val gameRoom = dataSnapshot.getValue<GameRoom>() ?: run {
                                            binding.startMatchBtn.isEnabled = false
                                            return
                                        }
//                                        Log.d("TAG -int key", "onDataChange: $key $validKey ${dataSnapshot.child("playerCount").getValue(String::class.java)}")
//                                        nm2 = dataSnapshot.child("playerInfo").child("nm2").getValue(String::class.java).orEmpty()
//                                        lvl2 = dataSnapshot.child("playerInfo").child("lvl2").getValue(Int::class.java) ?: 0
//                                        Log.d("TAG", "ver2: $nm2 $lvl2")
                                        mBundle.putString("plr2Id", gameRoom.player2.id.ifEmpty { gameRoom.playerInfo.plr2Id })  // remove ifEmpty someday
                                        mBundle.putString("nm2", gameRoom.player2.nm.ifEmpty { gameRoom.playerInfo.nm2 })
                                        mBundle.putInt("lvl2", if(gameRoom.player2.lvl == 0) gameRoom.playerInfo.lvl2 else gameRoom.player2.lvl)
                                        if (gameRoom.playerCount == "2") {
                                            binding.startMatchBtn.isEnabled = true
                                            //playerCountLocal=2;
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        // Failed to read value
                                        Log.w("TAG", "Failed to read value.", error.toException())
                                    }
                                })
                            lifecycleScope.launch{
                                delay(400)
                                binding.joinInputId.hint = viewModel.getKey4(key)
                                binding.copyPastBtn.setImageResource(R.drawable.icon_share)
                                binding.copyPastBtn.tag = R.drawable.icon_share
                                stickySwitch.switchColor =
                                    ContextCompat.getColor(applicationContext, R.color.greenY)
                            }
                        }
                    }
                }
            }
        binding.copyPastBtn.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            when (stickySwitch.getDirection()) {
                StickySwitch.Direction.RIGHT ->  {
                    viewModel.gameId = viewModel.getKey4(key)
                    ShareDialogFragment().show(supportFragmentManager, "share")
                }
                StickySwitch.Direction.LEFT -> {
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
//        bindingMain.bubbleTabBar.addBubbleListener { id: Int ->
//            val ft = fm.beginTransaction()
//            if (id == R.id.globalChat) {
//                ft.replace(R.id.chatFragment, ChatFragmentGlobal.newInstance(playerId))
//            } else {
//                if (key != null)
//                    ft.replace(R.id.chatFragment, ChatFragmentFriendly.newInstance(key, playerId)
//                ) else
//                    ft.replace(R.id.chatFragment, BlankChatFragment())
//            }
//            ft.commit()
//        }
        onBackPressedDispatcher.addCallback(this){
            if(bindingMain.drawerLayout.isDrawerOpen(GravityCompat.START))
                bindingMain.drawerLayout.closeDrawer(GravityCompat.START)
            else if (scrBrdVisible) {
                onGoBack()
            } else {
                val dialogBinding = DialogLayoutAlertBinding.inflate(layoutInflater)
                val alertDialog = AlertDialog.Builder(this@MultiplayerActivity)
                    .setView(dialogBinding.root)
                    .create()
                dialogBinding.textMessage.text =
                    "Do you really want to go back?"
                dialogBinding.buttonYes.text = "YES"
                dialogBinding.buttonNo.text = "NO"
                dialogBinding.buttonYes.setBounceClickListener {
                    isNotMuted {
                        val mediaPlayer = MediaPlayer.create(this@MultiplayerActivity, R.raw.btn_click_ef)
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
                dialogBinding.buttonNo.setBounceClickListener {
                    isNotMuted {
                        val mediaPlayer = MediaPlayer.create(this@MultiplayerActivity, R.raw.btn_click_ef)
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
        binding.volBtn.performOnClick()
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

    @SuppressLint("SetTextI18n")
    fun lvlUpgrade() {
        val pf = viewModel.gameProfile
        val tmpLvl = pref.getInt("tmpLvl", 1)
        if (tmpLvl != pf.lvlByCal) {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(this, R.raw.win_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            Firebase.firestore.collection("gamerProfile").document((playerId))
                .update("lvl", pf.lvlByCal)
            pref.edit{ putInt("tmpLvl", pf.lvlByCal) }
            val dialogBinding = DialogLayoutUpdateBinding.inflate(layoutInflater)
            val alertDialog = AlertDialog.Builder(this)
                .setView(dialogBinding.root)
                .setCancelable(false)
                .create()
            dialogBinding.warningMessage.text = if (tmpLvl < pf.lvlByCal) "Level Upgraded !" else "Level Downgraded !"
            dialogBinding.UpdateInfo.text =
                "$tmpLvl${if (tmpLvl < pf.lvlByCal) " --> " else " <-- "}${pf.lvlByCal}"
            dialogBinding.UpdateInfo.typeface = resources.getFont(R.font.baloopaaji)
            dialogBinding.UpdateInfo.textSize = 25f
            dialogBinding.buttonUpdate.text = "Continue"
            dialogBinding.buttonUpdate.setBounceClickListener {
                isNotMuted {
                    val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
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
    }

    @Suppress("DEPRECATION")
    private fun changeNameNeeded() {
        toast("Change Your Name.")
        pref.edit { putBoolean("muted", true) }
        profileBtn{binding->
            pref.edit { putBoolean("muted", false) }
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
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.disFragment, BlankFragment())
            .commit()
        binding.multiConstraintLyt.show()
        binding.globalScoreFrag.gone()
    }

    fun ideaBtn() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }

        val dialogBinding = DialogLayoutInfoMulBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogBinding.root)
        builder.setCancelable(false)
        val alertDialog = builder.create()
        dialogBinding.buttonOkey.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            alertDialog.dismiss()
        }
        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
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
        supportFragmentManager.beginTransaction()
            .replace(R.id.disFragment, DisplayFragment())
            .commit()
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
        supportFragmentManager.beginTransaction()
            .replace(R.id.disFragment, LeaderBoardFragment.newInstance(playerId))
            .commit()
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
        val bindingProfileDialog = DialogLayoutProfileBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(this@MultiplayerActivity)
            .setView(bindingProfileDialog.root)
            .create()
        onCreated.invoke(bindingProfileDialog)
//        val viewProfileDialog = LayoutInflater.from(this@MultiplayerActivity).inflate(
//            R.layout.dialog_layout_profile, findViewById(R.id.profileLayoutDialog)
//        )

        //builder.setCancelable(false)
        val x = viewModel.gameProfile
//        x.countryEmoji = (pref.getString("countryEmoji", ""))!!
//        x.countryNm = (pref.getString("countryNm", ""))!!
        bindingProfileDialog.apply {
            countryTxt.text = x.countryNm + " " + x.countryEmoji
            lvlTxt.text = "" + x.lvlByCal
            matchPlayedTxt.text = "" + x.matchPlayed
            matchWonTxt.text = "" + x.matchWinMulti
            coinShow.gone()
            buttonChangeAccount.show()
            val mgr = ImageManager.create(this@MultiplayerActivity)
            PlayGames.getPlayersClient(this@MultiplayerActivity).currentPlayer.addOnSuccessListener { player ->
                Log.d("TAG", "profileBtn: " + player.displayName)
                Log.d("TAG", "profileBtn: " + player.playerId)
                player.iconImageUri?.let {
                    mgr.loadImage(profileImage, it)
                }
            }
            val oldName = x.nm
            Log.d("TAG", "profileBtn nm: " + pref.getString("nm", "x"))
            nmTxt.setText(oldName)
            val db = Firebase.firestore
            //"com.google.android.play.games", "com.google.android.gms.games.ui.destination.main.MainActivity"
            profileShapeLayout.setBounceClickListener {
                val intent: Intent = Intent(Intent.ACTION_VIEW).apply {
                    setClassName(Constants.PLAY_GAMES, "${Constants.PLAY_SERVICES}.games.ui.destination.main.MainActivity")
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                try {
                    startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    showOnMarket(Constants.PLAY_GAMES)
                }
            }
            buttonChangeAccount.setBounceClickListener {
                isNotMuted {
                    val mediaPlayer = MediaPlayer.create(this@MultiplayerActivity, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
                openPlayGamesProfileChooser()
            }
            nmEditBtn.setBounceClickListener {
                isNotMuted {
                    val mediaPlayer = MediaPlayer.create(this@MultiplayerActivity, R.raw.btn_click_ef)
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
                        x.nm = newNm
                        x.apply()
                        Log.d("TAG", "profileBtn: $playerId")
                        db.collection("gamerProfile")
                            .document(playerId)
                            .update("nm", newNm)
                        nmTxt.isEnabled = false
                        nmEditBtn.setImageResource(R.drawable.icon_edit)
                        editing = false
                         pref.edit { putBoolean("needName", false) }
                    }
                }
            }
            buttonSaveInfo.setBounceClickListener {
                isNotMuted {
                    val mediaPlayer = MediaPlayer.create(this@MultiplayerActivity, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
                closeKeyboard()
                val newNm: String = nmTxt.text.toString()
                if ((newNm == "")) {
                    toast("Can't be empty.")
                    nmTxt.setText(oldName)
                } else if (newNm.length == 1) {
                    toast("Can't be single character.")
                    nmTxt.setText(oldName)
                } else {
                    x.nm = newNm
                    x.apply()
                    Log.d("TAG", "profileBtn: $playerId")
                    db.collection("gamerProfile")
                        .document(playerId)
                        .update("nm", newNm)
                    nmTxt.isEnabled = false
                    nmEditBtn.setImageResource(R.drawable.icon_edit)
                    editing = false
                    pref.edit { putBoolean("needName", false) }
                    alertDialog.dismiss()
                }
            }
            alertDialog.window?.setBackgroundDrawable(0.toDrawable())
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
        binding.newMsgBoltu.gone()
    }

    private fun openNavBtn() {
        bindingMain.root.openDrawer(GravityCompat.START)

//        when(binding.stickySwitch.getDirection()){
//            StickySwitch.Direction.LEFT ->
//                bindingMain.bubbleTabBar.setSelected(0,true)
//            StickySwitch.Direction.RIGHT ->
//                bindingMain.bubbleTabBar.setSelected(1,true)
//        }
        binding.newMsgBoltu.gone()
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
        startActivity(mIntent.putExtras(mBundle))
//        finish()
    }

    fun openPlayGamesProfileChooser() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(
                applicationContext.packageName,
                "com.google.android.gms.games.internal.v2.appshortcuts.PlayGamesAppShortcutsActivity"
            )
            // Adding required extras
            putExtra("com.google.android.gms.games.EXTRA_APP_SHORTCUT_ID", playerId)
            putExtra("com.google.android.gms.games.EXTRA_APP_SHORTCUT_EXTRAS", PersistableBundle().apply {
                putBoolean("com.google.android.gms.games.EXTRA_SWITCH_ACCOUNT", true)
            })
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Unable to open Play Games profile chooser", Toast.LENGTH_SHORT).show()
        }
    }


    companion object {
        var scrBrdVisible = false
        var key: String? = null
        var amiThePayer = false
    }
}