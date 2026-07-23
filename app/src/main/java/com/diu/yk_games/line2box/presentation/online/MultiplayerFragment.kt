package com.diu.yk_games.line2box.presentation.online

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.media.MediaPlayer
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnticipateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.GravityCompat
import androidx.core.widget.doAfterTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.ContentMultiplayerBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutInfoMulBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutProfileBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutUpdateBinding
import com.diu.yk_games.line2box.model.GameRoom
import com.diu.yk_games.line2box.model.MsgStore
import com.diu.yk_games.line2box.model.toMessage
import com.diu.yk_games.line2box.model.toPlayerInfo
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.util.Constants
import com.diu.yk_games.line2box.util.applyState
import com.diu.yk_games.line2box.util.cat
import com.diu.yk_games.line2box.util.closeKeyboard
import com.diu.yk_games.line2box.util.collectWithLifecycle
import com.diu.yk_games.line2box.util.getClipBoardData
import com.diu.yk_games.line2box.util.gone
import com.diu.yk_games.line2box.util.isMuted
import com.diu.yk_games.line2box.util.isNotMuted
import com.diu.yk_games.line2box.util.navigateSafe
import com.diu.yk_games.line2box.util.onBackPressed
import com.diu.yk_games.line2box.util.performOnClickF
import com.diu.yk_games.line2box.util.pref
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.diu.yk_games.line2box.util.show
import com.diu.yk_games.line2box.util.showOnMarket
import com.diu.yk_games.line2box.util.toast
import com.google.android.gms.common.images.ImageManager
import com.google.android.gms.games.PlayGames
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.firestore.firestore
import io.ak1.BubbleTabBar
import io.ghyeok.stickyswitch.widget.StickySwitch
import io.ghyeok.stickyswitch.widget.StickySwitch.OnSelectedChangeListener
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Suppress("DEPRECATION")
@SuppressLint("SetTextI18n")
class MultiplayerFragment : Fragment() {
    private lateinit var binding: ContentMultiplayerBinding
    private val viewModel by activityViewModels<MainViewModel>()
    private lateinit var parentActivity: Activity
    private lateinit var bubbleTabBar: BubbleTabBar
    private var editing = false
    lateinit var playerId: String

    override fun onResume() {
        super.onResume()
        viewModel.initGameProfile()
        binding.trophyTextId.text = "" + viewModel.gameProfile.coin
        lvlUpgrade()
        viewModel.fetchFriendlyChat()
        if (viewModel.isStickySwitchRight) {
            viewModel.matchBundle.value.putBoolean("plyr1", true)
            fetchJoiningPlayerInfo()
            binding.apply {
                stickySwitch.setDirection(
                    direction = StickySwitch.Direction.RIGHT,
                    isAnimate = false,
                    shouldTriggerSelected = false
                )
                joinInputId.isEnabled = false
                startMatchBtn.isEnabled = false
                joinInputId.hint = viewModel.getKey4()
                copyPastBtn.setImageResource(R.drawable.icon_share)
                copyPastBtn.tag = R.drawable.icon_share
                stickySwitch.switchColor =
                    ContextCompat.getColor(parentActivity, R.color.greenY)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ContentMultiplayerBinding.inflate(layoutInflater)
        parentActivity = requireActivity()
        bubbleTabBar = parentActivity.findViewById(R.id.bubbleTabBar)
        playerId = viewModel.playerId
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.initMultiplayer()
        viewModel.matchBundle.value.apply {
            putString("plr2Id", playerId)
            putString("nm2", viewModel.gameProfile.nm)
            putInt("lvl2", viewModel.gameProfile.lvlByCal())
            putBoolean("plyr1", false)
        }
        Log.d("TAG", "onCreate: local" + viewModel.gameProfile.coin)
        binding.trophyTextId.text = "" + viewModel.gameProfile.coin
        binding.emojiPlay.gone()
        val tmpNm = viewModel.gameProfile.nm
        if (pref.read("needName", true) || tmpNm.contains("Noob"))
            changeNameNeeded()
        lvlUpgrade()
        binding.copyPastBtn.setImageResource(R.drawable.icon_paste)
        binding.copyPastBtn.tag = R.drawable.icon_paste
        binding.startMatchBtn.isEnabled = false
        ifMuted()

        setupListener()
        setupObserver()
        setupObserver()
    }

    private fun setupObserver() {
        val activityRootView = parentActivity.window.decorView
        activityRootView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            //r will be populated with the coordinates of your view that area still visible.
            activityRootView.getWindowVisibleDisplayFrame(r)
            val maxHeight = activityRootView.height
            val heightDiff = maxHeight - r.height()
            cat("onGlobalLayout: height diff: $heightDiff ${r.height()} $maxHeight")
            if (heightDiff > 0.25 * maxHeight) {
                // if more than 25% of the screen, it's probably a keyboard......do something here
                cat("onGlobalLayout: here")
                binding.centerBox.animateCenterBox(400)
            } else {
                binding.centerBox.animateCenterBox(0)
            }
        }
    }

    private fun setupListener() {
        binding.joinInputId.doAfterTextChanged { txt ->
            if (txt?.length != 4) return@doAfterTextChanged
            val gameRoom = viewModel.getValidMatch(txt.toString()) ?: run {
                toast("Invalid Key")
                return@doAfterTextChanged
            }
            if (gameRoom.key.isEmpty()) {
                toast("Invalid Key")
                return@doAfterTextChanged
            }
            Log.d(
                "getKey",
                "afterTextChanged: " + gameRoom.key + " " + binding.joinInputId.text.toString().length
            )
            closeKeyboard()
            viewModel.matchBundle.value.putString("gameKey", gameRoom.key)
            if (gameRoom.playerCount == "1") {
                amiThePayer = true
//                viewModel.multiPlayerRef.child(gameRoom.key).child("playerCount")
//                    .setValue("2")
//                viewModel.multiPlayerRef.child(gameRoom.key).child("player2")
//                    .setValue(viewModel.gameProfile.toPlayerInfo())

                val ms = viewModel.gameProfile.toMessage(
                    playerId = playerId,
                    msg = "Joined the match.",
                    type = MsgStore.Type.EnterText
                )
                viewModel.multiPlayerRef.child(gameRoom.key).updateChildren(
                    mapOf(
                        "playerCount" to "2",
                        "player2" to viewModel.gameProfile.toPlayerInfo(),
                        "friendlyChat" to gameRoom.friendlyChat.toMutableMap().apply {
                            put(getFriendlyChatKey(gameRoom.key), ms)
                        }
                    )
                )
                binding.startMatchBtn.isEnabled = true
                viewModel.matchBundle.value.apply {
                    putString("plr1Id", gameRoom.player1.id)
                    putString("nm1", gameRoom.player1.nm)
                    putInt("lvl1", gameRoom.player1.lvl)
                }
//                viewModel.multiPlayerRef.child(gameRoom.key).child("friendlyChat")
//                    .push().setValue(ms)
                viewModel.matchKey = gameRoom.key
                viewModel.friendsChatList.collectWithLifecycle {
                    if (it.isEmpty()) return@collectWithLifecycle
                    bubbleTabBar.setSelected(1, true)
                    cancel()
                }
            } else if (!amiThePayer) {
                binding.startMatchBtn.isEnabled = false
                toast("Match already started")
            }
        }
        binding.stickySwitch.onSelectedChangeListener =
            object : OnSelectedChangeListener {
                override fun onSelectedChange(direction: StickySwitch.Direction, text: String) {
                    amiThePayer = false
                    when (direction) {
                        StickySwitch.Direction.LEFT -> {
                            binding.joinInputId.isEnabled = true
                            binding.startMatchBtn.isEnabled = false
                            binding.joinInputId.hint = ""
                            binding.joinInputId.setText("")
                            viewModel.matchBundle.value.apply {
                                putString("plr2Id", playerId)
                                putString("nm2", viewModel.gameProfile.nm)
                                putInt("lvl2", viewModel.gameProfile.lvlByCal())
                                putBoolean("plyr1", false)
                            }


                            bubbleTabBar.setSelected(0, true)
                            viewModel.clearFriendlyChat()
                            viewModel.setNewMsgBoltVisible(false)
                            viewModel.isStickySwitchRight = false
                            lifecycleScope.launch {
                                delay(400.milliseconds)
                                binding.joinInputId.hint = "Game ID"
                                binding.copyPastBtn.setImageResource(R.drawable.icon_paste)
                                binding.copyPastBtn.tag = R.drawable.icon_paste
                                binding.stickySwitch.switchColor = -0xdc8e06
                            }
                        }

                        StickySwitch.Direction.RIGHT -> {
                            binding.joinInputId.isEnabled = false
                            binding.startMatchBtn.isEnabled = false
                            binding.joinInputId.hint = ""
                            binding.joinInputId.setText("")
                            viewModel.clearTempMatches()
                            val key =
                                viewModel.multiPlayerRef.push().key.orEmpty().ifEmpty { return }
                            viewModel.matchBundle.value.apply {
                                putString("plr1Id", playerId)
                                putBoolean("plyr1", true)
                                putString("nm1", viewModel.gameProfile.nm)
                                putInt("lvl1", viewModel.gameProfile.lvlByCal())
                                putString("gameKey", key)
                            }
                            Log.d("TAG", "onCreate key: $key")
                            val msg = viewModel.gameProfile.toMessage(
                                playerId = playerId,
                                msg = "Created the match.",
                                type = MsgStore.Type.EnterText
                            )
                            val gameRoom = GameRoom(
                                key = key,
                                player1 = viewModel.gameProfile.toPlayerInfo(),
                                friendlyChat = mapOf(getFriendlyChatKey(key) to msg)
                            )
                            viewModel.multiPlayerRef.child(key)
                                .setValue(gameRoom)
//                            viewModel.multiPlayerRef.child(key)
//                                .child("friendlyChat")
//                                .push()
//                                .setValue(
//                                    viewModel.gameProfile.toMessage(
//                                        playerId = playerId,
//                                        msg = "Created the match.",
//                                        type = MsgStore.Type.EnterText
//                                    )
//                                )
                            bubbleTabBar.setSelected(1, true)
                            viewModel.isStickySwitchRight = true
                            viewModel.matchKey = key
                            fetchJoiningPlayerInfo(key)
                            lifecycleScope.launch {
                                delay(400.milliseconds)
                                binding.joinInputId.hint = viewModel.getKey4()
                                binding.copyPastBtn.setImageResource(R.drawable.icon_share)
                                binding.copyPastBtn.tag = R.drawable.icon_share
                                binding.stickySwitch.switchColor =
                                    ContextCompat.getColor(parentActivity, R.color.greenY)
                            }
                        }
                    }
                }
            }
        binding.copyPastBtn.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            when (binding.stickySwitch.getDirection()) {
                StickySwitch.Direction.RIGHT -> {
                    viewModel.gameId = viewModel.getKey4()
                    ShareDialogFragment().show(childFragmentManager, "share")
                }

                StickySwitch.Direction.LEFT -> {
                    binding.joinInputId.setText(parentActivity.getClipBoardData())
                }
            }
        }
        binding.homeBtn.setBounceClickListener {
            backBtn()
        }
        binding.scoreBoardBtn.setBounceClickListener {
            scoreBoard()
        }
        binding.leaderBoardBtn.setBounceClickListener {
            leaderBoard()
        }
        binding.volBtn.performOnClickF()
        binding.ideaBtn.setBounceClickListener {
            ideaBtn()
        }
        binding.profileBtn.setBounceClickListener {
            profileBtn()
        }
        binding.startMatchBtn.setBounceClickListener {
            startBtn()
        }
    }

    private fun getFriendlyChatKey(key: String): String {
        return viewModel.multiPlayerRef.child(key)
            .child("friendlyChat").push().key.orEmpty()
            .ifEmpty { "0" }
    }

    private fun fetchJoiningPlayerInfo(key: String = viewModel.matchKey) {
        viewModel.friendsChatList.collectWithLifecycle {
            if (it.isEmpty()) return@collectWithLifecycle
            bubbleTabBar.setSelected(1, true)
            cancel()
        }
        viewModel.multiPlayerRef.child(key)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val gameRoom = dataSnapshot.getValue<GameRoom>() ?: run {
                        binding.startMatchBtn.isEnabled = false
                        return
                    }
                    viewModel.matchBundle.value.apply {
                        if (binding.stickySwitch.getDirection() == StickySwitch.Direction.LEFT) return@apply
                        putString("plr2Id", gameRoom.player2.id)
                        putString("nm2", gameRoom.player2.nm)
                        putInt("lvl2", gameRoom.player2.lvl)
                    }
                    when (gameRoom.playerCount) {
                        "2" -> {
                            binding.startMatchBtn.isEnabled = true
                            bubbleTabBar.setSelected(1, true)
                            //playerCountLocal=2;
                        }

                        "-1" -> {
                            viewModel.multiPlayerRef.child(key).removeEventListener(this)
                            binding.stickySwitch.setDirection(StickySwitch.Direction.LEFT)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Failed to read value
                    Log.w("TAG", "Failed to read value.", error.toException())
                }
            })
    }

    private fun View.animateCenterBox(bottomMargin: Int) {
        if (parentActivity.findViewById<DrawerLayout>(R.id.drawer_layout)
                .isDrawerOpen(GravityCompat.START)
        ) return
        val parent = parent as? ConstraintLayout ?: return
        val constraintSet = ConstraintSet()
        constraintSet.clone(parent)
        constraintSet.setMargin(id, ConstraintSet.BOTTOM, bottomMargin)

        // Create a ChangeBounds transition to animate layout changes (like margin updates)
        val transition = ChangeBounds().apply {
            interpolator = AnticipateInterpolator(1F) // Smooth effect
            duration = 3000L // Custom duration (duration works now)
        }
        TransitionManager.beginDelayedTransition(parent, transition) // Begin the delayed transition
        constraintSet.applyTo(parent) // Apply the new margin to the layout
    }


    @SuppressLint("SetTextI18n")
    private fun lvlUpgrade() {
        val pf = viewModel.gameProfile
        val tmpLvl = pref.read("tmpLvl", 1)
        if (tmpLvl != pf.lvlByCal()) {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.win_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            Firebase.firestore.collection("gamerProfile").document((playerId))
                .update("lvl", pf.lvlByCal())
            pref.save("tmpLvl", pf.lvlByCal())
            val dialogBinding = DialogLayoutUpdateBinding.inflate(layoutInflater)
            val alertDialog = AlertDialog.Builder(parentActivity)
                .setView(dialogBinding.root)
                .setCancelable(false)
                .create()
            dialogBinding.warningMessage.text =
                if (tmpLvl < pf.lvlByCal()) "Level Upgraded !" else "Level Downgraded !"
            dialogBinding.UpdateInfo.text =
                "$tmpLvl${if (tmpLvl < pf.lvlByCal()) " --> " else " <-- "}${pf.lvlByCal()}"
            dialogBinding.UpdateInfo.typeface = resources.getFont(R.font.baloopaaji)
            dialogBinding.UpdateInfo.textSize = 25f
            dialogBinding.buttonUpdate.text = "Continue"
            dialogBinding.buttonUpdate.setBounceClickListener {
                isNotMuted {
                    val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
                runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
            }
            alertDialog.window?.setBackgroundDrawable(0.toDrawable())
            runCatching { alertDialog.show() }
        }
    }

    @Suppress("DEPRECATION")
    private fun changeNameNeeded() {
        toast("Change Your Name.")
        val muted = pref.read("muted", false)
        if (!muted) pref.save("muted", true)
        profileBtn { binding ->
            if (!muted) pref.save("muted", false)
            lifecycleScope.launch {
                delay(250.milliseconds)
                binding.nmTxt.isEnabled = true
                editing = true
                binding.nmEditBtn.setImageResource(R.drawable.icon_save)
                if (binding.nmTxt.requestFocus()) {
                    val imm: InputMethodManager =
                        parentActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.toggleSoftInput(
                        InputMethodManager.SHOW_IMPLICIT,
                        InputMethodManager.HIDE_IMPLICIT_ONLY
                    )
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

    fun ideaBtn() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }

        val dialogBinding = DialogLayoutInfoMulBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(parentActivity)
        builder.setView(dialogBinding.root)
        builder.setCancelable(false)
        val alertDialog = builder.create()
        dialogBinding.btnConfirm.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
        }
        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        runCatching { alertDialog.show() }
    }

    private fun scoreBoard() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        navigateSafe(Routes.ScoreBoard)
    }

    private fun leaderBoard() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        navigateSafe(Routes.LeaderBoard)
    }

    private fun profileBtn(onCreated: (DialogLayoutProfileBinding) -> Unit = {}) {
        editing = false
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        val bindingProfileDialog = DialogLayoutProfileBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(parentActivity)
            .setView(bindingProfileDialog.root)
            .create()
        onCreated.invoke(bindingProfileDialog)
//        val viewProfileDialog = LayoutInflater.from(parentActivity@MultiplayerActivity).inflate(
//            R.layout.dialog_layout_profile, findViewById(R.id.profileLayoutDialog)
//        )

        //builder.setCancelable(false)
        val x = viewModel.gameProfile
//        x.countryEmoji = (pref.getString("countryEmoji", ""))!!
//        x.countryNm = (pref.getString("countryNm", ""))!!
        bindingProfileDialog.apply {
            countryTxt.text = x.countryNm + " " + x.countryEmoji
            lvlTxt.text = "" + x.lvlByCal()
            matchPlayedTxt.text = "" + x.matchPlayed
            matchWonTxt.text = "" + x.matchWinMulti
            coinShow.gone()
            buttonChangeAccount.show()
            val mgr = ImageManager.create(parentActivity)
            PlayGames.getPlayersClient(parentActivity).currentPlayer.addOnSuccessListener { player ->
                Log.d("TAG", "profileBtn: " + player.displayName)
                Log.d("TAG", "profileBtn: " + player.playerId)
                player.iconImageUri?.let {
                    mgr.loadImage(profileImage, it)
                }
            }
            val oldName = x.nm
            Log.d("TAG", "profileBtn nm: " + pref.read("nm", "x"))
            nmTxt.setText(oldName)
            val db = Firebase.firestore
            //"com.google.android.play.games", "com.google.android.gms.games.ui.destination.main.MainActivity"
            profileShapeLayout.setBounceClickListener {
                val intent: Intent = Intent(Intent.ACTION_VIEW).apply {
                    setClassName(
                        Constants.PLAY_GAMES,
                        "${Constants.PLAY_SERVICES}.games.ui.destination.main.MainActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                try {
                    startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    parentActivity.showOnMarket(Constants.PLAY_GAMES)
                }
            }
            buttonChangeAccount.setBounceClickListener {
                isNotMuted {
                    val mediaPlayer =
                        MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
                openPlayGamesProfileChooser()
            }
            nmEditBtn.setBounceClickListener {
                isNotMuted {
                    val mediaPlayer =
                        MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
                if (!editing) {
                    nmTxt.isEnabled = true
                    nmTxt.setSelection(nmTxt.text.length)
                    nmEditBtn.setImageResource(R.drawable.icon_save)
                    if (nmTxt.requestFocus()) {
                        val imm: InputMethodManager =
                            parentActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.toggleSoftInput(
                            InputMethodManager.SHOW_IMPLICIT,
                            InputMethodManager.HIDE_IMPLICIT_ONLY
                        )
                    }
                    editing = true
                } else {
                    closeKeyboard()
                    val newNm: String = nmTxt.text.toString()
                    if (newNm.length < 2) {
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
                        pref.save("needName", false)
                    }
                }
            }
            buttonSaveInfo.setBounceClickListener {
                isNotMuted {
                    val mediaPlayer =
                        MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
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
                    pref.save("needName", false)
                    runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
                }
            }
            alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        }

        runCatching { alertDialog.show() }
    }

    private fun backBtn() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        onBackPressed()
    }

    private fun startBtn() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        val mIntent = Intent(parentActivity, GameActivity2::class.java)
        startActivity(mIntent.putExtras(viewModel.matchBundle.value))
        binding.startMatchBtn.isEnabled = false
//        finish()
    }

    fun openPlayGamesProfileChooser() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(
                parentActivity.packageName,
                "com.google.android.gms.games.internal.v2.appshortcuts.PlayGamesAppShortcutsActivity"
            )
            // Adding required extras
            putExtra("com.google.android.gms.games.EXTRA_APP_SHORTCUT_ID", playerId)
            putExtra(
                "com.google.android.gms.games.EXTRA_APP_SHORTCUT_EXTRAS",
                PersistableBundle().apply {
                    putBoolean("com.google.android.gms.games.EXTRA_SWITCH_ACCOUNT", true)
                })
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                parentActivity,
                "Unable to open Play Games profile chooser",
                Toast.LENGTH_SHORT
            )
                .show()
        }
    }


    companion object {
        var amiThePayer = false
    }
}