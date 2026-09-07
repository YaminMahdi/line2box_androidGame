package com.diu.yk_games.line2box.presentation.online

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.view.animation.AnticipateInterpolator
import android.view.inputmethod.EditorInfo
import androidx.annotation.OptIn
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.GravityCompat
import androidx.core.widget.doAfterTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogLayoutInfoMulBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutProfileBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutUpdateBinding
import com.diu.yk_games.line2box.databinding.FragmentMultiplayerBinding
import com.diu.yk_games.line2box.model.GameRoom
import com.diu.yk_games.line2box.presentation.MainActivity
import com.diu.yk_games.line2box.presentation.base.BaseFragment
import com.diu.yk_games.line2box.presentation.island.DynamicIslandController
import com.diu.yk_games.line2box.presentation.main.SettingsFragment
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.util.*
import com.google.android.gms.common.images.ImageManager
import com.google.android.gms.games.PlayGames
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import io.ak1.BubbleTabBar
import io.ghyeok.stickyswitch.widget.StickySwitch
import io.ghyeok.stickyswitch.widget.StickySwitch.OnSelectedChangeListener
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("SetTextI18n")
class MultiplayerFragment :
    BaseFragment<FragmentMultiplayerBinding>(FragmentMultiplayerBinding::inflate) {
    private val drawerLayout: DrawerLayout by lazy {
        parentActivity.findViewById(R.id.drawer_layout)
    }
    private val bubbleTabBar: BubbleTabBar by lazy {
        parentActivity.findViewById(R.id.bubbleTabBar)
    }
    val liveBadge by lazy {
        BadgeDrawable.create(parentActivity).apply {
            isVisible = false
            alpha = 0
            clearNumber()
            badgeGravity = BadgeDrawable.TOP_START
        }
    }
    private var editing = false

    override fun onResume() {
        super.onResume()
        viewModel.initGameProfile()
        binding.trophyTextId.text = "" + viewModel.gameProfile.coin
        lvlUpgrade()
        if (viewModel.isStickySwitchRight) {
            binding.apply {
                stickySwitch.setDirection(
                    direction = StickySwitch.Direction.RIGHT,
                    isAnimate = false,
                    shouldTriggerSelected = false
                )
                joinInputId.isEnabled = false
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
        Log.d("TAG", "onCreate: local" + viewModel.gameProfile.coin)
        binding.trophyTextId.text = "" + viewModel.gameProfile.coin
        val tmpNm = viewModel.gameProfile.nm
        if (pref.read("needName", true) || tmpNm.contains("Noob"))
            changeNameNeeded()
        lvlUpgrade()
        binding.copyPastBtn.setImageResource(R.drawable.icon_paste)
        binding.copyPastBtn.tag = R.drawable.icon_paste

        setupUI()
        setupListener()
        setupObserver()
    }

    @OptIn(ExperimentalBadgeUtils::class)
    private fun setupUI() {
        binding.btnLive.post {
            BadgeUtils.attachBadgeDrawable(liveBadge, binding.btnLive, binding.frmLive)
        }
    }

    private fun setupObserver() {
        MainActivity.onImeHeightChange = { imeHeight ->
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        // Apply margin instantly without TransitionManager (smooth because it runs every frame)
                        val parent = binding.centerBox.parent as? ConstraintLayout
                        if (parent != null) {
                            val constraintSet = ConstraintSet()
                            constraintSet.clone(parent)
                            constraintSet.setMargin(
                                binding.centerBox.id,
                                ConstraintSet.BOTTOM,
                                imeHeight
                            )
                            constraintSet.applyTo(parent)
                        }
                    }
                }
            }
        }
        viewModel.matches.collectWithLifecycle {
            if (it.isNotEmpty()) {
                liveBadge.isVisible = true
                liveBadge.number = it.size
                liveBadge.alpha = 255
            } else {
                liveBadge.isVisible = false
                liveBadge.clearNumber()
                liveBadge.alpha = 0
            }
        }
        viewModel.joiningGame.collectWithLifecycle {
            if (binding.stickySwitch.getDirection() == StickySwitch.Direction.LEFT) {
                cancel()
                return@collectWithLifecycle
            }
            binding.joinInputId.hint = ""
            binding.joinInputId.setText("")
            viewModel.isStickySwitchRight = false
            binding.stickySwitch.setDirection(
                direction = StickySwitch.Direction.LEFT,
                isAnimate = false,
                shouldTriggerSelected = false
            )
        }
    }

    private fun setupListener() {
        binding.joinInputId.doAfterTextChanged { txt ->
            if (txt?.length != 4) return@doAfterTextChanged
            val room = viewModel.getValidMatch(txt.toString()) ?: run {
                DynamicIslandController.message("Invalid Key")
                return@doAfterTextChanged
            }
            Log.d("getKey", "afterTextChanged: " + room.key)
            closeKeyboard()

            if (room.player2.shouldEnter(room.ver, viewModel.playerId) ||
                room.player1.shouldEnter(room.ver, viewModel.playerId)
            ) {
                viewModel.matchRouteInfo = room.toRoutes(viewModel.gameProfile)
                viewModel.sendInitialMessage(room)
                bubbleTabBar.setSelected(1, true)
                startMatch()
            } else DynamicIslandController.message("Match already started")
        }
        binding.stickySwitch.onSelectedChangeListener =
            object : OnSelectedChangeListener {
                override fun onSelectedChange(direction: StickySwitch.Direction, text: String) {
                    when (direction) {
                        StickySwitch.Direction.LEFT -> {
                            binding.joinInputId.isEnabled = true
                            binding.joinInputId.hint = ""
                            binding.joinInputId.setText("")
                            bubbleTabBar.setSelected(0, true)
                            viewModel.clearJoiningJob()
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
                            binding.joinInputId.hint = ""
                            binding.joinInputId.setText("")
                            viewModel.clearTempMatches()
                            val key =
                                viewModel.multiPlayerRef.push().key ?: viewModel.uuidV7
                            Log.d("TAG", "onCreate key: $key")
                            viewModel.createAndFetchJoiningPlayerInfo(key)
                            bubbleTabBar.setSelected(1, true)
                            viewModel.isStickySwitchRight = true
                            lifecycleScope.launch {
                                delay(400.milliseconds)
                                binding.joinInputId.hint = viewModel.getKey4(key)
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
        binding.btnLive.setBounceClickListener {
            viewModel.player.playButtonClickSound()
            navigateSafe(Routes.LiveStats)
        }
    }

    private fun fetchJoiningPlayerInfo(key: String) {
        viewModel.multiPlayerRef.child(key).asValueFlow<GameRoom>()
            .collectWithLifecycle { room ->
                if (binding.stickySwitch.getDirection() == StickySwitch.Direction.LEFT || key != room.key) {
                    cancel()
                    return@collectWithLifecycle
                }
                viewModel.matchRouteInfo = room.toRoutes(viewModel.gameProfile)
                when (room.playerCount) {
                    "2" -> {
                        cancel()
                        bubbleTabBar.setSelected(1, true)
                        startMatch()
                    }

                    "-1" -> {
                        cancel()
                        binding.stickySwitch.setDirection(StickySwitch.Direction.LEFT)
                    }
                }
            }
    }

    private fun View.animateCenterBox(bottomMargin: Int) {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) return
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
        if (tmpLvl != pf.lvlByCal() && viewModel.playerId.isNotEmpty()) {
            viewModel.player.playWinSound()
            viewModel.gamerProfileRef.document((viewModel.playerId))
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
        DynamicIslandController.message("Change Your Name.")
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
                runCatching {
                    startActivity(intent)
                }.onFailure {
                    val url = Constants.getPlayStoreUrl(Constants.PLAY_GAMES)
                    parentActivity.launchPlayStoreOverlayBypass(url)
                        ?: parentActivity.showOnMarket(Constants.PLAY_GAMES)
                        ?: parentActivity.showCustomTab(url)
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
                        DynamicIslandController.message("Can't be single character.")
                        nmTxt.setText(oldName)
                    } else {
                        viewModel.updateProfile {
                            copy(nm = newNm)
                        }
                        Log.d("TAG", "profileBtn: ${viewModel.playerId}")
                        if (viewModel.playerId.isNotEmpty())
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
            DynamicIslandController.message("Can't be empty.")
            nmTxt.setText(oldName)
        } else if (newNm.length == 1) {
            DynamicIslandController.message("Can't be single character.")
            nmTxt.setText(oldName)
        } else {
            viewModel.updateProfile {
                copy(nm = newNm)
            }
            Log.d("TAG", "profileBtn: ${viewModel.playerId}")
            if (viewModel.playerId.isNotEmpty())
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

    private fun startMatch() {
        if (viewModel.matchRouteInfo.gameKey.isEmpty()) return
        drawerLayout.closeDrawer(GravityCompat.START)
        viewModel.player.playButtonClickSound()
        viewModel.fetchServerLineClick()
        viewModel.fetchFriendlyChat()
        binding.joinInputId.hint = ""
        binding.joinInputId.setText("")
        viewModel.isStickySwitchRight = false
        binding.stickySwitch.setDirection(
            direction = StickySwitch.Direction.LEFT,
            isAnimate = false,
            shouldTriggerSelected = false
        )
        navigateSafe(viewModel.matchRouteInfo)
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
            DynamicIslandController.message("Unable to open Play Games profile chooser")
        }
    }
}