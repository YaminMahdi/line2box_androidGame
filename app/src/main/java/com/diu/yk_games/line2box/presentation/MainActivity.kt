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
import androidx.core.view.isVisible
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
import com.diu.yk_games.line2box.databinding.DialogLayoutUpdateuiBinding
import com.diu.yk_games.line2box.model.*
import com.diu.yk_games.line2box.presentation.adapter.ViewPagerAdapter
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.presentation.navigation.asRoute
import com.diu.yk_games.line2box.presentation.navigation.setupNavGraph
import com.diu.yk_games.line2box.presentation.online.BlankChatFragment
import com.diu.yk_games.line2box.presentation.online.ChatFragment
import com.diu.yk_games.line2box.util.*
import com.google.android.gms.games.PlayGames
import com.google.firebase.auth.PlayGamesAuthProvider
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.toObject
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
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
        viewModel.initGameProfile()
        window.hideSystemBars()

        navController.setupNavGraph()

        setupUI()
        setupListener()
        setupObserver()
    }

    override fun onResume() {
        super.onResume()
        inAppUpdate.onResume()
        viewModel.removeTempMatch()
    }

    private fun setupUI() {
        initializePlayGameUser()
        binding.loader.loadDrawable(R.drawable.g_loading)
        /*
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
                bindingDrawer.navCloseButtonLayout.updatePadding(bottom = targetPadding)
            }
        }
        */
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
//                ChatFragmentGlobal(),
//                ChatFragmentFriendly(),
                BlankChatFragment()
            ), this
        )
        bindingDrawer.bubbleTabBar.addBubbleListener { id ->
            if (id == R.id.globalChat)
                chatPager.currentItem = 0
            else
                chatPager.currentItem = if (viewModel.friendsChatList.value.isNotEmpty()) 1 else 2
        }
        chatPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = when (position) {
                0 -> bindingDrawer.bubbleTabBar.setSelected(0, true)
                1 -> bindingDrawer.bubbleTabBar.setSelected(1, true)
                else -> Unit
            }
        })
        binding.openNavBtn.setBounceClickListener {
            openNavBtn()
        }
        bindingDrawer.closeNavBtn.setBounceClickListener {
            closeNavBtn()
        }
    }

    private fun setupListener() {
        binding.sideNavGroup.layoutTransition = LayoutTransition()
        navController.currentBackStackEntryFlow.collectWithLifecycle {
            val route = it.destination.route.asRoute
            route.log("screen")
            when (route) {
                Routes.Home, Routes.ScoreBoard, Routes.LeaderBoard, Routes.ChangeName,
                Routes.GameBot, is Routes.GameDual -> binding.sideNavGroup.apply {
                    if (!isVisible) return@apply
                    animate()
                        .alpha(0f)
                        .translationX(-100f)
                        .setDuration(250L)
                        .withEndAction { gone() }
                        .start()
                }

                else -> binding.sideNavGroup.apply {
                    if (isVisible) return@apply
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
            if (isOnline) {
                if (viewModel.localPlayerCount != 2)
                    viewModel.multiPlayerRef.child(viewModel.gameOnline.gameKey).removeValue()
                else {
                    viewModel.sendMessage2FriendlyChat(
                        text = "Left the match.",
                        type = MsgStore.Type.ExitText
                    )
                    viewModel.multiPlayerRef.child(viewModel.gameOnline.gameKey)
                        .child("playerCount")
                        .setValue("-1")
                }
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
        viewModel.isLoading.collectWithLifecycle {
            binding.loadingLayout.changeVisibility(it)
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
        if (viewModel.friendsChatList.value.isEmpty())
            bindingDrawer.bubbleTabBar.setSelected(0, true)
        viewModel.setNewMsgBoltVisible(false)
    }

    private fun initializePlayGameUser() {
        val gamesSignInClient = PlayGames.getGamesSignInClient(this)
        gamesSignInClient.isAuthenticated
            .addOnSuccessListener { authenticationResult ->
                val isAuthenticated = authenticationResult.isAuthenticated
                if (ConnectivityObserver.isConnected) {
                    gamesSignInClient.requestServerSideAccess(
                        getString(R.string.default_web_client_id),
                        false /*forceRefreshToken=*/
                    ).addOnSuccessListener { serverAuthToken ->
                        //Toast.makeText(this, "serverAuthToken- "+serverAuthToken, Toast.LENGTH_SHORT).show()
                        val credential = PlayGamesAuthProvider.getCredential(serverAuthToken)
                        //AuthCredential credential = PlayGamesAuthProvider.getCredential(PlayGamesAuthProvider.PLAY_GAMES_SIGN_IN_METHOD)
                        viewModel.firebaseAuth.signInWithCredential(credential)
                            .addOnSuccessListener { _ ->
                                // Sign in success, update UI with the signed-in user's information

                                Log.d(TAG, "signInWithCredential: success")
                                if (viewModel.settings.showHadith)
                                    showAHadith()
                                val user = viewModel.firebaseAuth.currentUser
                                if (isAuthenticated && user != null) {
                                    PlayGames.getPlayersClient(this).currentPlayer.addOnSuccessListener { player ->
                                        val profileNeeded =
                                            viewModel.playerId != player.playerId
                                        viewModel.playerId = player.playerId
                                        player.playerId.log("playerId")
                                        if (profileNeeded || pref.read(
                                                "needProfile",
                                                true
                                            )
                                        ) {
                                            viewModel.firestore.collection("gamerProfile")
                                                .document(player.playerId)
                                                .get().addOnSuccessListener { document ->
                                                    if (document.exists()) {
                                                        pref.save(
                                                            "needProfile",
                                                            false
                                                        )
                                                        loadProfileFromServer()
                                                        Log.d(TAG, "Profile exists!")
                                                        toast("Profile Exists and Loaded!")
                                                    } else {
                                                        Log.d(TAG, "Profile does not exist!")
                                                        setupNewUserProfile()
                                                    }
                                                }.addOnFailureListener {
                                                    Log.d(TAG, "Failed with: ", it)
                                                    viewModel.onlineStatus = "needReload"
                                                    viewModel.setLoading(false)
                                                }
                                        } else loadProfileFromServer()
                                    }
                                    updateUI(ErrorType.NoError)
                                    // Continue with Play Games Services
                                } else {
                                    Log.d(TAG, "gamesSignInClient. isAuthenticated false")
                                    // Disable your integration with Play Games Services or show a
                                    // login button to ask  players to sign-in. Clicking it should
                                    // call GamesSignInClient.signIn()
                                    updateUI(ErrorType.AuthenticationFailure)
                                    viewModel.onlineStatus = "needReload"
                                    viewModel.setLoading(false)
                                }
                            }
                            .addOnFailureListener {
                                // If sign in fails, display a message to the user.
                                Log.d(TAG, "firebaseAuth signInWithCredential: failure: $it")
                                updateUI(ErrorType.AuthenticationFailure)
                                viewModel.onlineStatus = "needReload"
                                viewModel.setLoading(false)
                            }
                    }.addOnFailureListener {
                        // Failed to retrieve authentication code.
                        Log.d(TAG, "requestServerSideAccess:failure authentication code $it")
                        updateUI(ErrorType.PlayServiceNeeded)
                        viewModel.onlineStatus = "needReload"
                        viewModel.setLoading(false)
                    }
                } else {
                    Log.d(TAG, "No Internet")
                    updateUI(ErrorType.NoInternet)
                    viewModel.onlineStatus = "needReload"
                    viewModel.setLoading(false)
                }

            }.addOnFailureListener {
                // Disable your integration with Play Games Services or show a
                // login button to ask  players to sign-in. Clicking it should
                // call GamesSignInClient.signIn()
                Log.d(TAG, "gamesSignInClient. isAuthenticated failure: $it")
                updateUI(ErrorType.PlayServiceNeeded)
                viewModel.onlineStatus = "needReload"
                viewModel.setLoading(false)
            }
    }

    private fun loadProfileFromServer() {
        viewModel.firestore.collection("gamerProfile")
            .document(viewModel.playerId).get()
            .addOnSuccessListener {
                it.toObject<GameProfile>()?.let { profile ->
                    viewModel.gameProfile = profile
                }
            }
        viewModel.onlineStatus = "pass"
        viewModel.setLoading(false)
    }

    private fun setupNewUserProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            val gameProfile = GameProfile()
            gameProfile.playerId = viewModel.playerId
            val countryPair = tryGet {
                val doc = Jsoup.connect(Constants.IP_INFO_URL).ignoreContentType(true).get()
                Log.d(TAG, "getLocation: Success")
                val bodyTxt = doc.body().text()
                val countryInfo = Gson().fromJson(bodyTxt, CountryInfo::class.java)
                Log.d(TAG, "getLocation: $countryInfo")
                gameProfile.query = countryInfo.query
                gameProfile.cityNm = countryInfo.city
                GameUtils.countryList.find { it.first == countryInfo.country }
                    ?: GameUtils.countryList.find {
                        it.first.contains(
                            countryInfo.country,
                            ignoreCase = true
                        )
                    }
            } ?: ("Palestina" to "🇵🇸")

            Log.d(TAG, "onCreate: country ${countryPair.first}, emoji ${countryPair.second}")

            gameProfile.countryNm = countryPair.first
            gameProfile.countryEmoji = countryPair.second

            gameProfile.apply()

            viewModel.firestore.collection("gamerProfile").document(viewModel.playerId)
                .set(gameProfile)
                .addOnSuccessListener {
                    pref.save("needProfile", false)
                    viewModel.onlineStatus = "pass"
                    viewModel.setLoading(false)
                    Log.d(TAG, "onSuccess: Profile Created")
                }
                .addOnFailureListener {
                    Log.d("TAG", "onSuccess: Profile Creation Failed")
                    viewModel.onlineStatus = "needReload"
                    viewModel.setLoading(false)
                }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showAHadith() {
        viewModel.firestore.collection("dailyHadith")
            .count().get(AggregateSource.SERVER)
            .addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener
                val totalHadith = it.result.count
                val randDocId = Random().nextInt(totalHadith.toInt()).toString()
                Log.d(TAG, "showAHadith: $randDocId")
                viewModel.firestore.collection("dailyHadith").document(randDocId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val hadith = doc.toObject<HadithStore>() ?: return@addOnSuccessListener
                        val builder = AlertDialog.Builder(this)
                        val dialogBinding =
                            DialogLayoutShowHadithBinding.inflate(LayoutInflater.from(this))
                        builder.setView(dialogBinding.root)
                        builder.setCancelable(false)
                        val langBtn = dialogBinding.langBtn
                        val narratorInfo = dialogBinding.narratorInfo
                        val hadithTxt = dialogBinding.hadithTxt
                        val headTxt = dialogBinding.warningMessage
                        if (hadith.t == "h") headTxt.text =
                            "Read a Hadith" else if (hadith.t == "q") headTxt.text =
                            "Read from Quran"
                        if (pref.read("lang", "bn") == "bn") {
                            narratorInfo.text = hadith.b
                            hadithTxt.text = hadith.bn
                            narratorInfo.typeface = resources.getFont(R.font.paapri)
                            hadithTxt.typeface = resources.getFont(R.font.paapri)
                            hadithTxt.setLineSpacing(0f, 1f)
                            langBtn.text = "EN"
                        } else {
                            narratorInfo.text = hadith.e
                            hadithTxt.text = hadith.en
                            narratorInfo.typeface = resources.getFont(R.font.comfortaa)
                            hadithTxt.typeface = resources.getFont(R.font.comfortaa)
                            hadithTxt.setLineSpacing(7f, 1f)
                            langBtn.text = "BN"
                        }
                        dialogBinding.hadithInfo.text = hadith.ref
                        val alertDialog = builder.create()
                        langBtn.setBounceClickListener {
                            viewModel.player.playButtonClickSound()
                            if (langBtn.text == "EN") {
                                narratorInfo.text = hadith.e
                                hadithTxt.text = hadith.en
                                narratorInfo.typeface = resources.getFont(R.font.comfortaa)
                                hadithTxt.typeface = resources.getFont(R.font.comfortaa)
                                hadithTxt.setLineSpacing(7f, 1f)
                                pref.save("lang", "en")
                                langBtn.text = "BN"
                            } else {
                                narratorInfo.text = hadith.b
                                hadithTxt.text = hadith.bn
                                narratorInfo.typeface = resources.getFont(R.font.paapri)
                                hadithTxt.typeface = resources.getFont(R.font.paapri)
                                hadithTxt.setLineSpacing(0f, 1f)
                                pref.save("lang", "bn")
                                langBtn.text = "EN"
                            }
                        }
                        dialogBinding.buttonDone.setBounceClickListener {
                            viewModel.player.playButtonClickSound()
                            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
                        }
                        dialogBinding.srcLink.setBounceClickListener {
                            dialogBinding.srcLink.setTextColor(getColor(R.color.teal_700))
                            viewModel.player.playButtonClickSound()
                            var url = hadith.src
                            if (hadith.t == "q" && langBtn.text == "BN") url =
                                url.replace("bn", "en")
                            showCustomTab(url)
                        }
                        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
                        runCatching { alertDialog.show() }
                    }
            }
    }

    fun addSomeBlankHadith(startingIndex: Int) {
        for (i in startingIndex..startingIndex + 10) {
            viewModel.firestore.collection("dailyHadith").document(i.toString() + "")
                .set(HadithStore())
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateUI(errorType: ErrorType) {
        if (errorType == ErrorType.NoError) return
        val builder = AlertDialog.Builder(this)
        val dialogBinding = DialogLayoutUpdateuiBinding.inflate(LayoutInflater.from(this))
        builder.setView(dialogBinding.root)
        builder.setCancelable(false)
        val alertDialog = builder.create()
        dialogBinding.googlePlayWarning.gone()
        dialogBinding.warningMessage.text = errorType.msg
        val needProfile = pref.read("needProfile", true)
        when (errorType) {
            ErrorType.NoInternet -> {
                if (!needProfile) {
                    dialogBinding.updateInfo.text = "Some functionalities are disabled."
                    dialogBinding.buttonUpdate.text = "Continue"
                }
            }

            else -> {
                if (needProfile) {
                    dialogBinding.googlePlayWarning.show()
                    dialogBinding.updateInfo.text = "You may need to UPDATE an app.\n(Link Below)"
                }
            }
        }
        dialogBinding.buttonUpdate.setBounceClickListener {
            viewModel.player.playButtonClickSound()
            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
            if (pref.read("needProfile", true))
                recreate()
        }
        dialogBinding.playSvLink.setBounceClickListener {
            dialogBinding.playSvLink.setTextColor(getColor(R.color.teal_700))
            viewModel.player.playButtonClickSound()
            showCustomTab(Constants.PLAY_SERVICES_APP_URL) ?: showOnMarket(Constants.PLAY_SERVICES)
        }
        dialogBinding.playGmLink.setBounceClickListener {
            dialogBinding.playGmLink.setTextColor(getColor(R.color.teal_700))
            viewModel.player.playButtonClickSound()
            showCustomTab(Constants.PLAY_GAMES_APP_URL) ?: showOnMarket(Constants.PLAY_GAMES)
        }
        dialogBinding.restartLink.setBounceClickListener {
            dialogBinding.restartLink.setTextColor(getColor(R.color.teal_700))
            viewModel.player.playButtonClickSound()
            showCustomTab(Constants.RESTART_YOUTUBE_URL)
        }
        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        runCatching { alertDialog.show() }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
