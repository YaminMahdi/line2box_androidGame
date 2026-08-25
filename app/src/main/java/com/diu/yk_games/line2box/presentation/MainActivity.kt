package com.diu.yk_games.line2box.presentation

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
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
import com.diu.yk_games.line2box.presentation.component.installDynamicIsland
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.presentation.navigation.asRoute
import com.diu.yk_games.line2box.presentation.navigation.setupNavGraph
import com.diu.yk_games.line2box.presentation.online.ChatFragment
import com.diu.yk_games.line2box.util.*
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.toObject
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
        viewModel.initializePlayGameUser(this)
    }

    override fun onResume() {
        super.onResume()
        inAppUpdate.onResume()
    }

    private fun setupUI() {
        binding.loader.loadDrawable(R.drawable.g_loading)
        val activityRootView = window.decorView
        activityRootView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            val systemBarInsets = getSystemBars()
            //r will be populated with the coordinates of your view that area still visible.
            activityRootView.getWindowVisibleDisplayFrame(r)
            val maxHeight = activityRootView.height
            val heightDiff = maxHeight - r.height()

            // Calculate what the target padding should be
            val targetPadding = if (heightDiff > 0.25 * maxHeight)
                heightDiff - systemBarInsets.top
            else
                systemBarInsets.bottom

            // Only update if the padding has actually changed
            if (bindingDrawer.chatFragmentLinerLayout.paddingBottom != targetPadding) {
                bindingDrawer.chatFragmentLinerLayout.updatePadding(bottom = targetPadding)
//                bindingDrawer.navCloseButtonLayout.updatePadding(bottom = targetPadding)
            }
        }
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
        chatPager.isUserInputEnabled = false
        chatPager.adapter = ViewPagerAdapter(
            listOf(
                ChatFragment.newInstance(ChatMode.GLOBAL),
                ChatFragment.newInstance(ChatMode.FRIENDLY),
            ), this
        )
        bindingDrawer.bubbleTabBar.addBubbleListener { id ->
            chatPager.currentItem = if (id == R.id.globalChat) 0 else 1
        }
        chatPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) =
                bindingDrawer.bubbleTabBar.setSelected(position, false)
        })
        binding.composeView.installDynamicIsland(
            sourceView = binding.mainNavHost,
            onClick = {
                when(it) {
                    is DynamicBubble.Message if it.text.isNotBlank()->
                        openNavBtn()
                    else -> Unit
                }
            }
        )
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
                Routes.Home, Routes.ScoreBoard, Routes.LeaderBoard, Routes.ChangeName,
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
                val playerPath = if (route.isPlyr1) "player1/id" else "player2/id"
                viewModel.multiPlayerRef
                    .child(route.gameKey)
                    .updateChildren(
                        mapOf(
                            "playerCount" to "1",
                            playerPath to ""
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
                is MainUiEvent.ShowToast -> toast(event.message)
                is MainUiEvent.UpdateUi -> toast(event.errorType.description)
            }
        }
//        viewModel.isLoading.collectWithLifecycle {
//            binding.loadingLayout.changeVisibility(it)
//        }
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
                        val hadith = doc.toObject<HadithStore>() ?: return@addOnSuccessListener
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
    }
}
