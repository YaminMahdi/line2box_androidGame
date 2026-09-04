package com.diu.yk_games.line2box.presentation

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.*
import androidx.customview.widget.ViewDragHelper
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager2.widget.ViewPager2
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.ActivityMainDrawerBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutAlertBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutShowHadithBinding
import com.diu.yk_games.line2box.model.*
import com.diu.yk_games.line2box.presentation.adapter.ViewPagerAdapter
import com.diu.yk_games.line2box.presentation.island.DynamicIslandController
import com.diu.yk_games.line2box.presentation.island.installDynamicIsland
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.presentation.navigation.asRoute
import com.diu.yk_games.line2box.presentation.navigation.setupNavGraph
import com.diu.yk_games.line2box.presentation.online.chat.ChatFragment
import com.diu.yk_games.line2box.util.*
import com.google.firebase.firestore.AggregateSource
import kotlinx.coroutines.launch
import java.util.Random

@Suppress("DEPRECATION")
@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {
    private lateinit var bindingDrawer: ActivityMainDrawerBinding
    private val binding by lazy { bindingDrawer.main }
    private val navController by lazy { bindingDrawer.main.mainNavHost.getFragment<NavHostFragment>().navController }
    private val viewModel by viewModels<MainViewModel>()

    lateinit var playerId: String

    private val inAppUpdate = InAppUpdate(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingDrawer = ActivityMainDrawerBinding.inflate(layoutInflater)
        setContentView(bindingDrawer.root)
        setNavStatusPadding(binding.mainNavHost)
        inAppUpdate.checkForUpdate()
        window.hideSystemBars()

        navController.setupNavGraph()

        setupUI()
        setupListener()
        setupObserver()
        launchResumed {
            viewModel.initializePlayGameUser(this@MainActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        inAppUpdate.onResume()
    }

    private fun setupUI() {
        binding.composeView.installDynamicIsland(binding.mainNavHost)

        val activityRootView = window.decorView

        ViewCompat.setWindowInsetsAnimationCallback(
            activityRootView,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                    super.onPrepare(animation)
                    // Capture the current padding before the animation starts
                    val navBarHeight = activityRootView.getSystemBarsHeight().bottom
                    bindingDrawer.chatFragmentLinerLayout.updatePadding(bottom = navBarHeight)
                }

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    // Find the running IME animation
                    val imeAnimation = runningAnimations.find {
                        it.typeMask and WindowInsetsCompat.Type.ime() != 0
                    }

                    if (imeAnimation != null) {
                        // Get current animated IME height
                        val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                        onImeHeightChange(imeHeight)
                        val navBarHeight =
                            insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

                        // Calculate smooth animated padding
                        val targetPadding = if (imeHeight > 0) imeHeight else navBarHeight

                        bindingDrawer.chatFragmentLinerLayout.updatePadding(bottom = targetPadding)
                    }

                    return insets
                }
            }
        )
        bindingDrawer.root.addDrawerListener(object : DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) = closeKeyboard()
            override fun onDrawerStateChanged(newState: Int) {
                Log.d("TAG", "onDrawerStateChanged: $newState")
                if (newState == ViewDragHelper.STATE_SETTLING) {
                    closeKeyboard()
                    viewModel.setNewMsgBoltVisible(false)
                    if (viewModel.ignoreDrawerClosesSound) {
                        viewModel.ignoreDrawerClosesSound = false
                        return
                    }
                    viewModel.player.playSlideSound()
                }
            }
        })
        //chat bug fix
        val chatPager = bindingDrawer.chatPager
        bindingDrawer.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        chatPager.adapter = ViewPagerAdapter(
            listOf(
                ChatFragment.newInstance(GLOBAL),
                ChatFragment.newInstance(FRIENDLY),
            ), this
        )
        bindingDrawer.bubbleTabBar.addBubbleListener { id ->
            chatPager.currentItem = if (id == R.id.globalChat) 0 else 1
        }
        chatPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) =
                bindingDrawer.bubbleTabBar.setSelected(position, false)
        })
    }

    private fun setupListener() {
        binding.openNavBtn.setBounceClickListener {
            openNavBtn()
        }
        bindingDrawer.closeNavBtn.setBounceClickListener {
            closeNavBtn()
        }
        binding.sideNavGroup.layoutTransition = LayoutTransition()
        navController.currentBackStackEntryFlow.collectWithLifecycle {
            val route = it.destination.route.asRoute ?: return@collectWithLifecycle
            viewModel.currentRoute = route
            route.log("screen")
            when (route) {
                Routes.Home, Routes.ChangeName,
                Routes.GameBot, is Routes.GameDual -> binding.sideNavGroup.apply {
                    if (!isVisible) return@apply
                    translationX = 0f
                    animate()
                        .alpha(0f)
                        .translationX(-100f)
                        .setDuration(250L)
                        .withEndAction { gone() }
                        .start()
                }

                else -> binding.sideNavGroup.apply {
                    if (isVisible) return@apply
                    translationX = -100f
                    show()
                    animate()
                        .alpha(1f)
                        .translationX(0f)
                        .setDuration(250L)
                        .start()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this) {
            cat("onBackPressedDispatcher called ${isKeyboardOpen()}")
            if (isKeyboardOpen()) {
                closeKeyboard()
                return@addCallback
            }
            if (bindingDrawer.drawerLayout.isOpen) {
                closeKeyboard()
                bindingDrawer.drawerLayout.close()
                return@addCallback
            }
            when (navController.currentBackStackEntry?.destination?.route.asRoute) {
                is Routes.Home -> showBackPressDialog()
                is Routes.GameDual, is Routes.GameBot -> showBackPressDialog(getString(R.string.do_you_really_want_to_quit_the_match))
                is Routes.GameOnline -> showBackPressDialog(
                    confirmationText = getString(R.string.do_you_really_want_to_quit_the_match),
                    isOnline = true
                )

                is Routes.MultiPlayer -> {
                    onBackPressedIgnoreCallback()
                    viewModel.clearMultiPlayerData()
                }

                else -> onBackPressedIgnoreCallback()
            }
        }
    }

    fun OnBackPressedCallback.showBackPressDialog(
        confirmationText: String = "Do you really want to exit?",
        isOnline: Boolean = false
    ) {
        val builder = AlertDialog.Builder(this@MainActivity)
        val dialogBinding = DialogLayoutAlertBinding.inflate(LayoutInflater.from(this@MainActivity))
        builder.setView(dialogBinding.root)
        val alertDialog = builder.create()

        dialogBinding.textMessage.text = confirmationText
        dialogBinding.buttonYes.text = "YES"
        dialogBinding.buttonNo.text = "NO"
        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        dialogBinding.buttonYes.setBounceClickListener {
            viewModel.player.playButtonClickSound()
            val route = viewModel.matchRouteInfo.copy()
            if (isOnline && route.gameKey.isNotEmpty()) {
                viewModel.sendMessage(
                    text = "Left the match.",
                    chatMode = ChatMode.FRIENDLY,
                    type = MsgStore.MessageType.ExitText
                )
                val playerTimePath = if (route.isPlyr1) "player1/seenAt" else "player2/seenAt"
                viewModel.multiPlayerRef
                    .child(route.gameKey)
                    .updateChildren(
                        mapOf(
                            "playerCount" to "1",
                            playerTimePath to -2L
                        )
                    )
            }
            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
            onBackPressedIgnoreCallback()
        }
        dialogBinding.buttonNo.setBounceClickListener {
            viewModel.player.playButtonClickSound()
            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
        }
        runCatching { alertDialog.show() }
    }

    private fun setupObserver() {
        viewModel.uiEvents.collectWithLifecycle { event ->
            event.log("uiEvents")
            viewModel.uiEvents.value = null
            when (event) {
                MainUiEvent.ShowHadith -> showAHadith()
                is MainUiEvent.ShowToast -> DynamicIslandController.message(event.message)
                is MainUiEvent.UpdateUi -> DynamicIslandController.message(event.errorType.description)
            }
        }
        viewModel.joiningGame.collectWithLifecycle { route ->
            navController.navigate(route)
            bindingDrawer.drawerLayout.closeDrawer(GravityCompat.START)
            bindingDrawer.bubbleTabBar.setSelected(1, true)
        }
        viewModel.isNewMsgBoltVisible.collectWithLifecycle {
            binding.newMsgBoltu.changeVisibility(it)
        }
        lifecycleScope.launch {
            viewModel.settingsState.collect {
                it.log()
                binding.background.setDrawableWithFade(it.theme.background)
            }
        }
    }

    private fun closeNavBtn() {
        closeKeyboard()
        bindingDrawer.root.close()
        viewModel.setNewMsgBoltVisible(false)
    }

    private fun openNavBtn() {
        bindingDrawer.root.open()
        if (viewModel.friendlyChatList.value.isEmpty())
            bindingDrawer.bubbleTabBar.setSelected(0, true)
        viewModel.setNewMsgBoltVisible(false)
    }

    @SuppressLint("SetTextI18n")
    private fun showAHadith() {
        viewModel.firestore.collection("dailyHadith")
            .count().get(AggregateSource.SERVER)
            .addOnSuccessListener { snapshot ->
                val totalHadith = snapshot.count.toInt()
                if (totalHadith <= 0) {
                    Log.w(TAG, "No hadith documents found")
                    return@addOnSuccessListener
                }

                val randomDocId = Random().nextInt(totalHadith).toString()
                Log.d(TAG, "showAHadith: $randomDocId")

                viewModel.firestore.collection("dailyHadith")
                    .document(randomDocId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val hadith =
                            doc.toObjectOrNull<HadithStore>() ?: return@addOnSuccessListener
                        showHadithDialog(hadith)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to fetch hadith document", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get hadith count", e)
            }
    }

    private fun showHadithDialog(hadith: HadithStore) {
        val dialogBinding = DialogLayoutShowHadithBinding.inflate(LayoutInflater.from(this))
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        // Set up UI elements
        val headTxt = dialogBinding.warningMessage
        headTxt.text = when (hadith.t) {
            "h" -> "Read a Hadith"
            "q" -> "Read from Quran"
            else -> return
        }

        // Apply language settings
        applyLanguageToUI(dialogBinding, hadith, viewModel.settings.language)
        dialogBinding.hadithInfo.text = hadith.ref

        // Language toggle button
        dialogBinding.langBtn.setBounceClickListener {
            viewModel.player.playButtonClickSound()
            val newLanguage = viewModel.settings.language.flip()
            applyLanguageToUI(dialogBinding, hadith, newLanguage)
            viewModel.updateSettings { copy(language = newLanguage) }
            dialogBinding.langBtn.text = newLanguage.name
        }

        // Done button
        dialogBinding.buttonDone.setBounceClickListener {
            viewModel.player.playButtonClickSound()
            if (alertDialog.isShowing) {
                alertDialog.dismiss()
            }
        }

        // Source link
        dialogBinding.srcLink.setBounceClickListener {
            dialogBinding.srcLink.setTextColor(getColor(R.color.teal_700))
            viewModel.player.playButtonClickSound()
            var url = hadith.src
            if (hadith.t == "q" && viewModel.settings.language == Settings.Language.EN) {
                url = url.replace("bn", "en")
            }
            showCustomTab(url)
        }

        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        alertDialog.show()
    }

    data class LangBaseUiState(
        val narratorText: String,
        val hadithText: String,
        val fontRes: Int,
        val lineSpacing: Float
    )

    private fun applyLanguageToUI(
        binding: DialogLayoutShowHadithBinding,
        hadith: HadithStore,
        lang: Settings.Language
    ) {
        val state = if (lang == Settings.Language.BN)
            LangBaseUiState(hadith.b, hadith.bn, R.font.paapri, 0f)
        else
            LangBaseUiState(hadith.e, hadith.en, R.font.comfortaa, 7f)


        binding.narratorInfo.apply {
            text = state.narratorText
            typeface = resources.getFont(state.fontRes)
        }

        binding.hadithTxt.apply {
            text = state.hadithText
            typeface = resources.getFont(state.fontRes)
            setLineSpacing(state.lineSpacing, 1f)
        }
    }

    fun addSomeBlankHadith(startingIndex: Int) {
        for (i in startingIndex..startingIndex + 10) {
            viewModel.firestore.collection("dailyHadith").document(i.toString() + "")
                .set(HadithStore())
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        var onImeHeightChange: (Int) -> Unit = {}
    }
}
