package com.diu.yk_games.line2box.presentation.online

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.view.animation.AnticipateInterpolator
import android.view.inputmethod.EditorInfo
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.GravityCompat
import androidx.core.widget.doAfterTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogLayoutInfoMulBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutProfileBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutUpdateBinding
import com.diu.yk_games.line2box.databinding.FragmentMultiplayerBinding
import com.diu.yk_games.line2box.model.GameRoom
import com.diu.yk_games.line2box.presentation.base.BaseFragment
import com.diu.yk_games.line2box.presentation.main.SettingsFragment
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.util.*
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

@SuppressLint("SetTextI18n")
class MultiplayerFragment : BaseFragment<FragmentMultiplayerBinding>(FragmentMultiplayerBinding::inflate) {
    private val drawerLayout: DrawerLayout by lazy {
        parentActivity.findViewById(R.id.drawer_layout)
    }
    private val bubbleTabBar: BubbleTabBar by lazy {
        parentActivity.findViewById(R.id.bubbleTabBar)
    }
    private var editing = false

    override fun onResume() {
        super.onResume()
        viewModel.initGameProfile()
        binding.trophyTextId.text = "" + viewModel.gameProfile.coin
        lvlUpgrade()
        viewModel.fetchFriendlyChat()
        if (viewModel.isStickySwitchRight) {
            viewModel.gameOnline.isPlyr1 = true
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.initMultiplayer()
        viewModel.gameOnline.apply {
            plr2Id = viewModel.playerId
            nm2 = viewModel.gameProfile.nm
            lvl2 = viewModel.gameProfile.lvlByCal()
            isPlyr1 = false
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

        setupListener()
        setupObserver()
    }

    private fun setupObserver() {
        val activityRootView = parentActivity.window.decorView
        var isKeyboardOpen = false
        activityRootView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            //r will be populated with the coordinates of your view that area still visible.
            activityRootView.getWindowVisibleDisplayFrame(r)
            val maxHeight = activityRootView.height
            val heightDiff = maxHeight - r.height()

            // Determine the current state based on your 25% threshold
            val currentlyOpen = heightDiff > (0.25 * maxHeight)

            // Only animate if the keyboard state has actually toggled
            if (currentlyOpen != isKeyboardOpen) {
                isKeyboardOpen = currentlyOpen // Update the state flag

                if (isKeyboardOpen) {
                    cat("onGlobalLayout: Keyboard Opened")
                    binding.centerBox.animateCenterBox(400)
                } else {
                    cat("onGlobalLayout: Keyboard Closed")
                    binding.centerBox.animateCenterBox(0)
                }
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
            viewModel.gameOnline.gameKey = gameRoom.key
            if (gameRoom.playerCount == "1") {
                amiThePayer = true
                viewModel.sendInitialMessage(gameRoom)
/*                val ms = viewModel.gameProfile.toMessage(
                    playerId = viewModel.playerId,
                    msg = "Joined the match.",
                    type = MsgStore.MessageType.EnterText
                )
                viewModel.multiPlayerRef.child(gameRoom.key).updateChildren(
                    mapOf(
                        "playerCount" to "2",
                        "player2" to viewModel.gameProfile.toPlayerInfo(),
                        "friendlyChat" to gameRoom.friendlyChat.toMutableMap().apply {
                            put(getFriendlyChatKey(gameRoom.key), ms)
                        }
                    )
                )*/
                binding.startMatchBtn.isEnabled = true
                viewModel.gameOnline.apply {
                    plr1Id = gameRoom.player1.id
                    nm1 = gameRoom.player1.nm
                    lvl1 = gameRoom.player1.lvl
                }
//                viewModel.matchKey = gameRoom.key
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
                            viewModel.gameOnline.apply {
                                plr2Id = viewModel.playerId
                                nm2 = viewModel.gameProfile.nm
                                lvl2 = viewModel.gameProfile.lvlByCal()
                                isPlyr1 = false
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
                                viewModel.multiPlayerRef.push().key ?: viewModel.uuidV7
                            viewModel.gameOnline.apply {
                                plr1Id = viewModel.playerId
                                nm1 = viewModel.gameProfile.nm
                                lvl1 = viewModel.gameProfile.lvlByCal()
                                gameKey = key
                                isPlyr1 = true
                            }
                            Log.d("TAG", "onCreate key: $key")
                            viewModel.sendInitialMessage(
                                gameRoom = GameRoom(key = key),
                                isPlyr1 = true
                            )
/*
                            val msg = viewModel.gameProfile.toMessage(
                                playerId = viewModel.playerId,
                                msg = "Created the match.",
                                type = MsgStore.MessageType.EnterText
                            )
                            val gameRoom = GameRoom(
                                key = key,
                                player1 = viewModel.gameProfile.toPlayerInfo(),
                                friendlyChat = mapOf(getFriendlyChatKey(key) to msg)
                            )
                            viewModel.multiPlayerRef.child(key)
                                .setValue(gameRoom)
                                */
                            bubbleTabBar.setSelected(1, true)
                            viewModel.isStickySwitchRight = true
//                            viewModel.matchKey = key
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
            viewModel.player.playButtonClickSound()
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
        binding.settingBtn.setBounceClickListener {
            viewModel.player.playButtonClickSound()
            SettingsFragment.show(childFragmentManager)
        }
        binding.ideaBtn.setBounceClickListener {
            ideaBtn()
        }
        binding.profileBtn.setBounceClickListener {
            profileBtn()
        }
        binding.startMatchBtn.setBounceClickListener {
            binding.joinInputId.setText("")
            startBtn()
        }
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
                    viewModel.gameOnline.apply {
                        if (binding.stickySwitch.getDirection() == StickySwitch.Direction.LEFT) return@apply
                        plr2Id = gameRoom.player2.id
                        nm2 = gameRoom.player2.nm
                        lvl2 = gameRoom.player2.lvl
                    }
                    when (gameRoom.playerCount) {
                        "2" -> {
                            binding.startMatchBtn.isEnabled = true
                            bubbleTabBar.setSelected(1, true)
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
        if (drawerLayout.isDrawerOpen(GravityCompat.START)
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
            viewModel.player.playWinSound()
            Firebase.firestore.collection("gamerProfile").document((viewModel.playerId))
                .update("lvl", pf.lvlByCal())
            pref.save("tmpLvl", pf.lvlByCal())
            val dialogBinding = DialogLayoutUpdateBinding.inflate(layoutInflater)
            val alertDialog = AlertDialog.Builder(parentActivity)
                .setView(dialogBinding.root)
                .setCancelable(false)
                .create()
            dialogBinding.warningMessage.text =
                if (tmpLvl < pf.lvlByCal()) "Level Upgraded !" else "Level Downgraded !"
            dialogBinding.updateInfo.text =
                "$tmpLvl${if (tmpLvl < pf.lvlByCal()) " --> " else " <-- "}${pf.lvlByCal()}"
            dialogBinding.updateInfo.typeface = resources.getFont(R.font.baloopaaji)
            dialogBinding.updateInfo.textSize = 25f
            dialogBinding.buttonUpdate.text = "Continue"
            dialogBinding.buttonUpdate.setBounceClickListener {
                viewModel.player.playButtonClickSound()
                runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
            }
            alertDialog.window?.setBackgroundDrawable(0.toDrawable())
            runCatching { alertDialog.show() }
        }
    }

    private fun changeNameNeeded() {
        toast("Change Your Name.")
        profileBtn(false) { binding ->
            lifecycleScope.launch {
                delay(250.milliseconds)
                binding.nmTxt.isEnabled = true
                editing = true
                binding.nmEditBtn.setImageResource(R.drawable.icon_save)
                binding.nmTxt.showKeyboard()
                binding.nmTxt.setSelection(binding.nmTxt.text.length)
            }
        }
    }

    fun ideaBtn() {
        viewModel.player.playButtonClickSound()

        val dialogBinding = DialogLayoutInfoMulBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(parentActivity)
        builder.setView(dialogBinding.root)
        builder.setCancelable(false)
        val alertDialog = builder.create()
        dialogBinding.btnConfirm.setBounceClickListener {
            viewModel.player.playButtonClickSound()
            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
        }
        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        runCatching { alertDialog.show() }
    }

    private fun scoreBoard() {
        viewModel.player.playButtonClickSound()
        navigateSafe(Routes.ScoreBoard)
    }

    private fun leaderBoard() {
        viewModel.player.playButtonClickSound()
        navigateSafe(Routes.LeaderBoard)
    }

    private fun profileBtn(
        playSound: Boolean = true,
        onCreated: (DialogLayoutProfileBinding) -> Unit = {}
    ) {
        editing = false
        if (playSound)
            viewModel.player.playButtonClickSound()
        val bindingProfileDialog = DialogLayoutProfileBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(parentActivity)
            .setView(bindingProfileDialog.root)
            .create()
        onCreated.invoke(bindingProfileDialog)
        val profile = viewModel.gameProfile
        bindingProfileDialog.apply {
            countryTxt.text = profile.countryNm + " " + profile.countryEmoji
            lvlTxt.text = "" + profile.lvlByCal()
            matchPlayedTxt.text = "" + profile.matchPlayed
            matchWonTxt.text = "" + profile.matchWinMulti
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
            val oldName = profile.nm
            Log.d("TAG", "profileBtn nm: " + pref.read("nm", "x"))
            nmTxt.setText(oldName)
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
                viewModel.player.playButtonClickSound()
                openPlayGamesProfileChooser()
            }
            nmEditBtn.setBounceClickListener {
                viewModel.player.playButtonClickSound()
                if (!editing) {
                    nmTxt.isEnabled = true
                    nmTxt.setSelection(nmTxt.text.length)
                    nmEditBtn.setImageResource(R.drawable.icon_save)
                    nmTxt.showKeyboard()
                    editing = true
                } else {
                    closeKeyboard()
                    val newNm = nmTxt.text.toString().trim()
                    if (newNm.length < 2) {
                        toast("Can't be single character.")
                        nmTxt.setText(oldName)
                    } else {
                        viewModel.updateProfile {
                            copy(nm = newNm)
                        }
                        Log.d("TAG", "profileBtn: ${viewModel.playerId}")
                        viewModel.gamerProfileRef
                            .document(viewModel.playerId)
                            .update("nm", newNm)
                        nmTxt.isEnabled = false
                        nmEditBtn.setImageResource(R.drawable.icon_edit)
                        editing = false
                        pref.save("needName", false)
                    }
                }
            }

            // Button Click
            buttonSaveInfo.setBounceClickListener {
                saveProfileName(
                    oldName = oldName,
                    alertDialog = alertDialog
                )
            }

            // IME Done Keyboard Listener
            nmTxt.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    saveProfileName(
                        oldName = oldName,
                        alertDialog = alertDialog
                    )
                    true // Consumes the action
                } else {
                    false
                }
            }
            alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        }

        runCatching { alertDialog.show() }
    }

    private fun DialogLayoutProfileBinding.saveProfileName(
        oldName: String,
        alertDialog: AlertDialog
    ) {
        viewModel.player.playButtonClickSound()
        closeKeyboard()

        val newNm = nmTxt.text.toString().trim()

        if (newNm.isEmpty()) {
            toast("Can't be empty.")
            nmTxt.setText(oldName)
        } else if (newNm.length == 1) {
            toast("Can't be single character.")
            nmTxt.setText(oldName)
        } else {
            viewModel.updateProfile {
                copy(nm = newNm)
            }
            Log.d("TAG", "profileBtn: ${viewModel.playerId}")

            viewModel.gamerProfileRef
                .document(viewModel.playerId)
                .update("nm", newNm)

            nmTxt.isEnabled = false
            nmEditBtn.setImageResource(R.drawable.icon_edit)
            editing = false
            pref.save("needName", false)

            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
        }
    }

    private fun backBtn() {
        viewModel.player.playButtonClickSound()
        onBackPressed()
    }

    private fun startBtn() {
        if (viewModel.gameOnline.gameKey.isEmpty()) return
        viewModel.player.playButtonClickSound()
        navigateSafe(viewModel.gameOnline)
        binding.startMatchBtn.isEnabled = false
    }

    fun openPlayGamesProfileChooser() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(
                parentActivity.packageName,
                "com.google.android.gms.games.internal.v2.appshortcuts.PlayGamesAppShortcutsActivity"
            )
            // Adding required extras
            putExtra("com.google.android.gms.games.EXTRA_APP_SHORTCUT_ID", viewModel.playerId)
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
            toast("Unable to open Play Games profile chooser")
        }
    }


    companion object {
        var amiThePayer = false
    }
}