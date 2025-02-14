package com.diu.yk_games.line2box.presentation.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.ActivityStartBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutAlertBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutInfoBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutLoadingBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutShowHadithBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutUpdateuiBinding
import com.diu.yk_games.line2box.model.CountryInfo
import com.diu.yk_games.line2box.model.ErrorType
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.model.HadithStore
import com.diu.yk_games.line2box.model.msg
import com.diu.yk_games.line2box.pref
import com.diu.yk_games.line2box.prefEditor
import com.diu.yk_games.line2box.presentation.BlankFragment
import com.diu.yk_games.line2box.presentation.bot.GameActivity3
import com.diu.yk_games.line2box.presentation.offline.GameActivity1
import com.diu.yk_games.line2box.presentation.online.MultiplayerActivity
import com.diu.yk_games.line2box.util.ConnectivityObserver
import com.diu.yk_games.line2box.util.Constants
import com.diu.yk_games.line2box.util.InAppUpdate
import com.diu.yk_games.line2box.util.applyState
import com.diu.yk_games.line2box.util.gone
import com.diu.yk_games.line2box.util.hideSystemBars
import com.diu.yk_games.line2box.util.invisible
import com.diu.yk_games.line2box.util.isMuted
import com.diu.yk_games.line2box.util.isNotMuted
import com.diu.yk_games.line2box.util.loadDrawable
import com.diu.yk_games.line2box.util.log
import com.diu.yk_games.line2box.util.performOnClick
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.diu.yk_games.line2box.util.setNavStatusPadding
import com.diu.yk_games.line2box.util.show
import com.diu.yk_games.line2box.util.showCustomTab
import com.diu.yk_games.line2box.util.showOnMarket
import com.diu.yk_games.line2box.util.toast
import com.diu.yk_games.line2box.util.tryGet
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import com.google.firebase.Firebase
import com.google.firebase.auth.PlayGamesAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.util.Random

class StartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartBinding
    private var scrBrdVisible = false
    private val isFirstRun: Boolean by lazy { pref.getBoolean("firstRun", true) }
    companion object {
        private const val TAG = "TAG: StartActivity"
        lateinit var playerId: String
        private var showHadith = true
    }

    //String onlineVersionCode = null;
    private var countryEmojis = ArrayList(
        listOf(
            "🇦🇫", "🇦🇱", "🇩🇿", "🇦🇩", "🇦🇴", "🇦🇬", "🇦🇷", "🇦🇲", "🇦🇺", "🇦🇹", "🇦🇿", "🇧🇸", "🇧🇭", "🇧🇩", "🇧🇧", "🇧🇾", "🇧🇪",
            "🇧🇿", "🇧🇯", "🇧🇹", "🇧🇴", "🇧🇦", "🇧🇼", "🇧🇷", "🇧🇳", "🇧🇬", "🇧🇫", "🇧🇮", "🇨🇻", "🇰🇭", "🇨🇲", "🇨🇦", "🇨🇫", "🇹🇩",
            "🇨🇱", "🇨🇳", "🇨🇴", "🇰🇲", "🇨🇩", "🇨🇷", "🇭🇷", "🇨🇺", "🇨🇾", "🇨🇿", "🇨🇮", "🇩🇰", "🇩🇯", "🇩🇲", "🇩🇴", "🇨🇩", "🇪🇨",
            "🇪🇬", "🇸🇻", "🏴󠁧󠁢󠁥󠁮󠁧󠁿", "🇬🇶", "🇪🇷", "🇪🇪", "🇸🇿", "🇪🇹", "🇫🇯", "🇫🇮", "🇫🇷", "🇬🇦", "🇬🇲", "🇬🇪", "🇩🇪", "🇬🇭", "🇬🇷",
            "🇬🇩", "🇬🇹", "🇬🇳", "🇬🇼", "🇬🇾", "🇭🇹", "🇭🇳", "🇭🇰", "🇭🇺", "🇮🇸", "🇮🇳", "🇮🇩", "🇮🇷", "🇮🇶", "🇮🇪", "🇮🇱", "🇮🇹",
            "🇯🇲", "🇯🇵", "🇯🇴", "🇰🇿", "🇰🇪", "🇰🇮", "🇰🇼", "🇰🇬", "🇱🇦", "🇱🇻", "🇱🇧", "🇱🇸", "🇱🇷", "🇱🇾", "🇱🇮", "🇱🇹", "🇱🇺",
            "🇲🇬", "🇲🇼", "🇲🇾", "🇲🇻", "🇲🇱", "🇲🇹", "🇲🇭", "🇲🇶", "🇲🇺", "🇲🇽", "🇫🇲", "🇲🇩", "🇲🇨", "🇲🇳", "🇲🇪", "🇲🇦", "🇲🇿",
            "🇲🇲", "🇳🇦", "🇳🇷", "🇳🇵", "🇳🇱", "🇳🇿", "🇳🇮", "🇳🇪", "🇳🇬", "🇰🇵", "🇲🇰", "🇳🇴", "🇴🇲", "🇵🇰", "🇵🇼", "🇵🇸", "🇵🇦",
            "🇵🇬", "🇵🇾", "🇵🇪", "🇵🇭", "🇵🇱", "🇵🇹", "🇶🇦", "🇷🇴", "🇷🇺", "🇷🇼", "🇰🇳", "🇱🇨", "🇻🇨", "🇼🇸", "🇸🇲", "🇸🇹", "🇸🇦",
            "🏴󠁧󠁢󠁳󠁣󠁴󠁿", "🇸🇳", "🇷🇸", "🇸🇨", "🇸🇱", "🇸🇬", "🇸🇰", "🇸🇮", "🇸🇧", "🇸🇴", "🇿🇦", "🇰🇷", "🇸🇸", "🇪🇸", "🇱🇰", "🇸🇩", "🇸🇷",
            "🇸🇪", "🇨🇭", "🇸🇾", "🇹🇼", "🇹🇯", "🇹🇿", "🇹🇭", "🇹🇱", "🇹🇬", "🇹🇴", "🇹🇹", "🇹🇳", "🇹🇷", "🇹🇲", "🇹🇻", "🇺🇬", "🇺🇦",
            "🇦🇪", "🇬🇧", "🇺🇸", "🇺🇾", "🇺🇿", "🇻🇺", "🇻🇪", "🇻🇳", "🇾🇪", "🇿🇲", "🇿🇼"
        )
    )
    private var countryNm = ArrayList(
        listOf(
            "Afghanistan", "Albania", "Algeria", "Andorra", "Angola", "Antigua and Barbuda", "Argentina", "Armenia", "Australia", "Austria", "Azerbaijan", "Bahamas", "Bahrain", "Bangladesh", "Barbados", "Belarus", "Belgium", "Belize", "Benin", "Bhutan", "Bolivia", "Bosnia and Herzegovina", "Botswana", "Brazil", "Brunei",
            "Bulgaria", "Burkina Faso", "Burundi", "Cabo Verde", "Cambodia", "Cameroon", "Canada", "Central African Republic", "Chad", "Chile", "China", "Colombia", "Comoros", "Congo", "Costa Rica", "Croatia", "Cuba", "Cyprus", "Czechia", "Côte d'Ivoire", "Denmark", "Djibouti", "Dominica", "Dominican Republic", "DR Congo",
            "Ecuador", "Egypt", "El Salvador", "England", "Equatorial Guinea", "Eritrea", "Estonia", "Eswatini (Swaziland)", "Ethiopia", "Fiji", "Finland", "France", "Gabon", "Gambia", "Georgia", "Germany", "Ghana", "Greece", "Grenada", "Guatemala", "Guinea", "Guinea-Bissau", "Guyana", "Haiti", "Honduras", "Hong Kong", "Hungary",
            "Iceland", "India", "Indonesia", "Iran", "Iraq", "Ireland", "Israel", "Italy", "Jamaica", "Japan", "Jordan", "Kazakhstan", "Kenya", "Kiribati", "Kuwait", "Kyrgyzstan", "Laos", "Latvia", "Lebanon", "Lesotho", "Liberia", "Libya", "Liechtenstein", "Lithuania", "Luxembourg", "Madagascar", "Malawi", "Malaysia", "Maldives",
            "Mali", "Malta", "Marshall Islands", "Martinique", "Mauritius", "Mexico", "Micronesia", "Moldova", "Monaco", "Mongolia", "Montenegro", "Morocco", "Mozambique", "Myanmar", "Namibia", "Nauru", "Nepal", "Netherlands", "New Zealand", "Nicaragua", "Niger", "Nigeria", "North Korea", "North Macedonia", "Norway", "Oman",
            "Pakistan", "Palau", "Palestine", "Panama", "Papua New Guinea", "Paraguay", "Peru", "Philippines", "Poland", "Portugal", "Qatar", "Romania", "Russia", "Rwanda", "Saint Kitts and Nevis", "Saint Lucia", "Saint Vincent", "Samoa", "San Marino", "São Tomé and Príncipe", "Saudi Arabia", "Scotland", "Senegal", "Serbia",
            "Seychelles", "Sierra Leone", "Singapore", "Slovakia", "Slovenia", "Solomon Islands", "Somalia", "South Africa", "South Korea", "South Sudan", "Spain", "Sri Lanka", "Sudan", "Suriname", "Sweden", "Switzerland", "Syria", "Taiwan", "Tajikistan", "Tanzania", "Thailand", "Timor-Leste", "Togo", "Tonga",
            "Trinidad and Tobago", "Tunisia", "Turkey", "Turkmenistan", "Tuvalu", "Uganda", "Ukraine", "United Arab Emirates", "United Kingdom", "United States", "Uruguay", "Uzbekistan", "Vanuatu", "Venezuela", "Vietnam", "Yemen", "Zambia", "Zimbabwe"
        )
    )

    private val countryList = listOf(
        "Afghanistan" to "🇦🇫",
        "Albania" to "🇦🇱",
        "Algeria" to "🇩🇿",
        "Andorra" to "🇦🇩",
        "Angola" to "🇦🇴",
        "Antigua and Barbuda" to "🇦🇬",
        "Argentina" to "🇦🇷",
        "Armenia" to "🇦🇲",
        "Australia" to "🇦🇺",
        "Austria" to "🇦🇹",
        "Azerbaijan" to "🇦🇿",
        "Bahamas" to "🇧🇸",
        "Bahrain" to "🇧🇭",
        "Bangladesh" to "🇧🇩",
        "Barbados" to "🇧🇧",
        "Belarus" to "🇧🇾",
        "Belgium" to "🇧🇪",
        "Belize" to "🇧🇿",
        "Benin" to "🇧🇯",
        "Bhutan" to "🇧🇹",
        "Bolivia" to "🇧🇴",
        "Bosnia and Herzegovina" to "🇧🇦",
        "Botswana" to "🇧🇼",
        "Brazil" to "🇧🇷",
        "Brunei" to "🇧🇳",
        "Bulgaria" to "🇧🇬",
        "Burkina Faso" to "🇧🇫",
        "Burundi" to "🇧🇮",
        "Cabo Verde" to "🇨🇻",
        "Cambodia" to "🇰🇭",
        "Cameroon" to "🇨🇲",
        "Canada" to "🇨🇦",
        "Central African Republic" to "🇨🇫",
        "Chad" to "🇹🇩",
        "Chile" to "🇨🇱",
        "China" to "🇨🇳",
        "Colombia" to "🇨🇴",
        "Comoros" to "🇰🇲",
        "Congo" to "🇨🇩",
        "Costa Rica" to "🇨🇷",
        "Croatia" to "🇭🇷",
        "Cuba" to "🇨🇺",
        "Cyprus" to "🇨🇾",
        "Czechia" to "🇨🇿",
        "Côte d'Ivoire" to "🇨🇮",
        "Denmark" to "🇩🇰",
        "Djibouti" to "🇩🇯",
        "Dominica" to "🇩🇲",
        "Dominican Republic" to "🇩🇴",
        "DR Congo" to "🇨🇩",
        "Ecuador" to "🇪🇨",
        "Egypt" to "🇪🇬",
        "El Salvador" to "🇸🇻",
        "England" to "🏴",
        "Equatorial Guinea" to "🇬🇶",
        "Eritrea" to "🇪🇷",
        "Estonia" to "🇪🇪",
        "Eswatini (Swaziland)" to "🇸🇿",
        "Ethiopia" to "🇪🇹",
        "Fiji" to "🇫🇯",
        "Finland" to "🇫🇮",
        "France" to "🇫🇷",
        "Gabon" to "🇬🇦",
        "Gambia" to "🇬🇲",
        "Georgia" to "🇬🇪",
        "Germany" to "🇩🇪",
        "Ghana" to "🇬🇭",
        "Greece" to "🇬🇷",
        "Grenada" to "🇬🇩",
        "Guatemala" to "🇬🇹",
        "Guinea" to "🇬🇳",
        "Guinea-Bissau" to "🇬🇼",
        "Guyana" to "🇬🇾",
        "Haiti" to "🇭🇹",
        "Honduras" to "🇭🇳",
        "Hong Kong" to "🇭🇰",
        "Hungary" to "🇭🇺",
        "Iceland" to "🇮🇸",
        "India" to "🇮🇳",
        "Indonesia" to "🇮🇩",
        "Iran" to "🇮🇷",
        "Iraq" to "🇮🇶",
        "Ireland" to "🇮🇪",
        "Italy" to "🇮🇹",
        "Jamaica" to "🇯🇲",
        "Japan" to "🇯🇵",
        "Jordan" to "🇯🇴",
        "Kazakhstan" to "🇰🇿",
        "Kenya" to "🇰🇪",
        "Kiribati" to "🇰🇮",
        "Kuwait" to "🇰🇼",
        "Kyrgyzstan" to "🇰🇬",
        "Laos" to "🇱🇦",
        "Latvia" to "🇱🇻",
        "Lebanon" to "🇱🇧",
        "Lesotho" to "🇱🇸",
        "Liberia" to "🇱🇷",
        "Libya" to "🇱🇾",
        "Liechtenstein" to "🇱🇮",
        "Lithuania" to "🇱🇹",
        "Luxembourg" to "🇱🇺",
        "Madagascar" to "🇲🇬",
        "Malawi" to "🇲🇼",
        "Malaysia" to "🇲🇾",
        "Maldives" to "🇲🇻",
        "Mali" to "🇲🇱",
        "Malta" to "🇲🇹",
        "Marshall Islands" to "🇲🇭",
        "Mexico" to "🇲🇽",
        "Moldova" to "🇲🇩",
        "Monaco" to "🇲🇨",
        "Mongolia" to "🇲🇳",
        "Montenegro" to "🇲🇪",
        "Morocco" to "🇲🇦",
        "Mozambique" to "🇲🇿",
        "Myanmar" to "🇲🇲",
        "Namibia" to "🇳🇦",
        "Nauru" to "🇳🇷",
        "Nepal" to "🇳🇵",
        "Netherlands" to "🇳🇱",
        "New Zealand" to "🇳🇿",
        "Nicaragua" to "🇳🇮",
        "Niger" to "🇳🇪",
        "Nigeria" to "🇳🇬",
        "North Korea" to "🇰🇵",
        "North Macedonia" to "🇲🇰",
        "Norway" to "🇳🇴",
        "Oman" to "🇴🇲",
        "Pakistan" to "🇵🇰",
        "Palestine" to "🇵🇸",
        "Panama" to "🇵🇦",
        "Papua New Guinea" to "🇵🇬",
        "Paraguay" to "🇵🇾",
        "Peru" to "🇵🇪",
        "Philippines" to "🇵🇭",
        "Poland" to "🇵🇱",
        "Portugal" to "🇵🇹",
        "Qatar" to "🇶🇦",
        "Romania" to "🇷🇴",
        "Russia" to "🇷🇺",
        "Rwanda" to "🇷🇼",
        "Saudi Arabia" to "🇸🇦",
        "Scotland" to "🏴",
        "Serbia" to "🇷🇸",
        "South Korea" to "🇰🇷",
        "Spain" to "🇪🇸",
        "Sri Lanka" to "🇱🇰",
        "Turkey" to "🇹🇷",
        "United Arab Emirates" to "🇦🇪",
        "United Kingdom" to "🇬🇧",
        "United States" to "🇺🇸",
        "Uruguay" to "🇺🇾",
        "Uzbekistan" to "🇺🇿",
        "Vanuatu" to "🇻🇺",
        "Venezuela" to "🇻🇪",
        "Vietnam" to "🇻🇳",
        "Yemen" to "🇾🇪",
        "Zambia" to "🇿🇲",
        "Zimbabwe" to "🇿🇼"
    )

    lateinit var context: Context
//    private var mode1: ImageView? = null
//    private var mode2: ImageView? = null
//    private var mode3: ImageView? = null
    private lateinit var loadingUI: LoadingUI
    private var onlineStatus =""
    private val db = Firebase.firestore

    private val inAppUpdate = InAppUpdate(this)


    override fun onResume() {
        super.onResume()
        inAppUpdate.onResume()
    }

    override fun onDestroy() {
        loadingUI.stop()
        super.onDestroy()
    }

    @SuppressLint("VisibleForTests")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        window.hideSystemBars()
        val firebaseAuth = Firebase.auth
        PlayGamesSdk.initialize(this)
        ConnectivityObserver.initialize(this)
        context = this
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.setNavStatusPadding(binding.mainLayout, binding.globalScoreFrag)
        inAppUpdate.checkForUpdate()

        loadingUI = LoadingUI()
        loadingUI.start()
        //if (!isFirstRun)

        //if(isFirstRun)

//        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
//                .requestServerAuthCode(getString(R.string.default_web_client_id))
//                .build()
        //window.insetsController?.hide(WindowInsets.Type.statusBars())
//        WindowInsetsControllerCompat(window, binding.root).let { controller ->
//            controller.hide(WindowInsetsCompat.Type.systemBars())
//            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            window.setDecorFitsSystemWindows(false)
//        } else {
//            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
//        }

        //window.decorView.windowInsetsController.hide(WindowInsets.Type.statusBars())
//        window.insetsController?.hide(WindowInsets.Type.statusBars())
//        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
//        window.setDecorFitsSystemWindows(false)

        binding.startBtnId.setBounceClickListener {
            startBtn()
        }
        binding.volBtn.performOnClick()
        binding.ideaBtn.setBounceClickListener {
            ideaBtn()
        }
        binding.scrBrdBtn.setBounceClickListener {
            scoreBoard()
        }
        binding.goBackBtn.setBounceClickListener {
            goBack()
        }
        binding.logo.setBounceClickListener()
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            @SuppressLint("SetTextI18n")
            override fun handleOnBackPressed() {
                if (scrBrdVisible) {
                    onGoBack()
                } else
                {
                    val builder= AlertDialog.Builder(this@StartActivity)
                    val dialogBinding = DialogLayoutAlertBinding.inflate(LayoutInflater.from(this@StartActivity))
                    builder.setView(dialogBinding.root)
                    val alertDialog = builder.create()

                    dialogBinding.textMessage.text ="Do you really want to exit?"
                    dialogBinding.buttonYes.text = "YES"
                    dialogBinding.buttonNo.text = "NO"
                    alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
                    dialogBinding.buttonYes.setBounceClickListener {
                        isNotMuted {
                            val mediaPlayer = MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                            mediaPlayer?.start()
                            mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
                        }
                        alertDialog.dismiss()
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                    dialogBinding.buttonNo.setBounceClickListener {
                        isNotMuted {
                            val mediaPlayer = MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                            mediaPlayer?.start()
                            mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
                        }
                        alertDialog.dismiss()
                    }
                    try { alertDialog.show() }
                    catch (npe: NullPointerException) { npe.printStackTrace() }
                }
            }
        })

        if (showHadith && !isFirstRun) {
            showAHadith()
            showHadith = false
        }
        val gamesSignInClient = PlayGames.getGamesSignInClient(this)
        gamesSignInClient.isAuthenticated
            .addOnSuccessListener {authenticationResult->
                val isAuthenticated = authenticationResult.isAuthenticated
                if(ConnectivityObserver.isConnected){
                    gamesSignInClient.requestServerSideAccess(getString(R.string.default_web_client_id),  false /*forceRefreshToken=*/ )
                        .addOnSuccessListener{serverAuthToken->
                            //Toast.makeText(this, "serverAuthToken- "+serverAuthToken, Toast.LENGTH_SHORT).show()
                            val credential = PlayGamesAuthProvider.getCredential(serverAuthToken)
                            //AuthCredential credential = PlayGamesAuthProvider.getCredential(PlayGamesAuthProvider.PLAY_GAMES_SIGN_IN_METHOD)
                            firebaseAuth.signInWithCredential(credential)
                                .addOnSuccessListener{
                                    // Sign in success, update UI with the signed-in user's information

                                    Log.d(TAG, "signInWithCredential: success")
                                    if (showHadith && isFirstRun) {
                                        showAHadith()
                                        showHadith = false
                                    }
                                    val user = firebaseAuth.currentUser
                                    if (isAuthenticated && user != null) {
                                        PlayGames.getPlayersClient(this@StartActivity).currentPlayer.addOnSuccessListener { player ->
                                            playerId = player.playerId
                                            playerId.log("playerId")
                                            if (pref.getBoolean("needProfile", true)) {
                                                db.collection("gamerProfile")
                                                    .document(playerId)
                                                    .get().addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            val document = task.result
                                                            if (document.exists()) {
                                                                prefEditor.putBoolean("needProfile", false).apply()
                                                                loadProfileFromServer()
                                                                Log.d(TAG, "Profile exists!")
                                                                toast( "Profile Exists and Loaded!")
                                                            } else {
                                                                Log.d(TAG, "Profile does not exist!")
                                                                setupNewUserProfile()
                                                            }
                                                        } else {
                                                            Log.d(TAG, "Failed with: ", task.getException())
                                                            onlineStatus = "needReload"
                                                            loadingUI.stop()
                                                        }
                                                    }
                                            } else loadProfileFromServer()
                                        }
                                        // Continue with Play Games Services
                                    } else {
                                        //Toast.makeText(StartActivity.this, "Failed", Toast.LENGTH_SHORT).show()
                                        Log.d(TAG, "gamesSignInClient. isAuthenticated false: $it")
                                        // Disable your integration with Play Games Services or show a
                                        // login button to ask  players to sign-in. Clicking it should
                                        // call GamesSignInClient.signIn()
                                        updateUI(ErrorType.AuthenticationFailure)
                                        onlineStatus = "needReload"
                                        loadingUI.stop()
                                    }
                                    updateUI(ErrorType.NoError)
                                }
                                .addOnFailureListener {
                                    // If sign in fails, display a message to the user.
                                    Log.d(TAG, "firebaseAuth signInWithCredential: failure: $it")
                                    //Toast.makeText(StartActivity.this, "Authentication failed.",Toast.LENGTH_SHORT).show()
                                    updateUI(ErrorType.AuthenticationFailure)
                                    onlineStatus = "needReload"
                                    loadingUI.stop()
                                }
                        }
                        .addOnFailureListener {
                            // Failed to retrieve authentication code.
                            Log.d(TAG, "requestServerSideAccess:failure authentication code $it")
                            //Toast.makeText(StartActivity.this, "No Internet.",Toast.LENGTH_SHORT).show()
                            updateUI(ErrorType.PlayServiceNeeded)
                            onlineStatus = "needReload"
                            loadingUI.stop()
                        }
                }else{
                    Log.d(TAG, "No Internet")
                    updateUI(ErrorType.NoInternet)
                    onlineStatus = "needReload"
                    loadingUI.stop()
                }

            }
            .addOnFailureListener {
                //Toast.makeText(StartActivity.this, "Failed", Toast.LENGTH_SHORT).show()
                // Disable your integration with Play Games Services or show a
                // login button to ask  players to sign-in. Clicking it should
                // call GamesSignInClient.signIn()
                Log.d(TAG, "gamesSignInClient. isAuthenticated failure: $it")
                updateUI(ErrorType.PlayServiceNeeded)
                onlineStatus = "needReload"
                loadingUI.stop()
            }
            //gamesSignInClient.signIn()
            //vsRadioGrp=findViewById(R.id.vsRadioGrp)
            ifMuted()
            //throw RuntimeException("Test Crash") // Force a crash

    }

    private fun loadProfileFromServer() {
        db.collection("gamerProfile")
            .document(playerId).get()
            .addOnSuccessListener { 
                it.toObject<GameProfile>()?.apply()
            }
        onlineStatus = "pass"
        loadingUI.stop()
    }

    private fun getLocationOld() {
        lifecycleScope.launch(Dispatchers.IO) {
            val bodyTxt: String
            try {
                Log.d(TAG, "getLocation: Success")
                val url = "http://ip-api.com/json/?fields=country,city,query"
                val doc = Jsoup.connect(url).ignoreContentType(true).get()
                val body = doc.body()
                bodyTxt = body.text() //.replace("\"","\\\"")
                Log.d(TAG, "getLocation: $bodyTxt")
                //runOnUiThread(() -> {
                //result.setText(builder.toString())
                //JsonElement jelem = gson.fromJson(json, JsonElement.class)
                val g = GsonBuilder().serializeNulls().create()
                val je = g.fromJson(bodyTxt, JsonElement::class.java)
                val jd = je.asJsonObject
                Log.d(TAG, "JsonData.class ip: $jd")
                prefEditor.putString("cityNm", jd["city"].asString).apply()
                prefEditor.putString("query", jd["query"].asString).apply()
                var country = jd["country"].asString
                var tmp = 0
                if (country == "Israel") {
                    country = "Palestine"
                    tmp = 1
                }
                val index = countryNm.indexOf(jd["country"].asString)
                Log.d(TAG, "onCreate: index $index")
                if (index != -1)
                    prefEditor.putString("countryEmoji", countryEmojis[index]).apply()
                if (tmp == 1)
                    country = "Palestina"
                prefEditor.putString("countryNm", country).apply()
                Log.d(TAG, "onCreate: emo " + pref.getString("countryEmoji", ""))
                val upLoc = GameProfile()
                upLoc.playerId = playerId
                upLoc.countryEmoji = pref.getString("countryEmoji", "")!!
                upLoc.countryNm = pref.getString("countryNm", "")!!
                //if(!upLoc.countryNm.equals(""))
                db.collection("gamerProfile").document(playerId).set(upLoc)
                //})
            } catch (e: Exception) {
                //builder.append("Error : ").append(e.getMessage()).append("\n")
                e.printStackTrace()
            }
        }
    }

    private fun setupNewUserProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            val gameProfile = GameProfile()
            gameProfile.playerId = playerId
            val countryPair =  tryGet {
                val doc = Jsoup.connect(Constants.IP_INFO_URL).ignoreContentType(true).get()
                Log.d(TAG, "getLocation: Success")
                val bodyTxt = doc.body().text()
                val countryInfo = Gson().fromJson(bodyTxt, CountryInfo::class.java)
                Log.d(TAG, "getLocation: $countryInfo")
                gameProfile.query = countryInfo.query
                gameProfile.cityNm = countryInfo.city
                countryList.find { it.first == countryInfo.country } ?: countryList.find { it.first.contains(countryInfo.country, ignoreCase = true) }
            } ?: ("Palestina" to "🇵🇸")

            Log.d(TAG, "onCreate: country ${countryPair.first}, emoji ${countryPair.second}")
            
            gameProfile.countryNm = countryPair.first
            gameProfile.countryEmoji = countryPair.second

            gameProfile.apply()

            db.collection("gamerProfile").document(playerId).set(gameProfile)
                .addOnSuccessListener {
                    prefEditor.putBoolean("needProfile", false).apply()
                    onlineStatus = "pass"
                    loadingUI.stop()
                    Log.d(TAG, "onSuccess: Profile Created")
                    //Toast.makeText(StartActivity.this, "onSuccess: Profile Created", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { 
                    Log.d("TAG", "onSuccess: Profile Creation Failed")
                    //Toast.makeText(StartActivity.this, "onSuccess: Profile Creation Failed", Toast.LENGTH_SHORT).show()
                    onlineStatus = "needReload"
                    loadingUI.stop()
                }
        }
    }

//    private val isNetworkConnected: Boolean
//        get() {
//            val cm = this.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
//            return cm.activeNetworkInfo != null
//        }
//
//    fun internetIsConnected(): Boolean {
//        return try {
//            val command = "ping -c 1 google.com"
//            Runtime.getRuntime().exec(command).waitFor() == 0
//        } catch (e: Exception) {
//            false
//        }
//    }

    inner class LoadingUI {
        private var alertDialog: AlertDialog
        init {
            val dialogBinding = DialogLayoutLoadingBinding.inflate(LayoutInflater.from(this@StartActivity))
            alertDialog = AlertDialog
                .Builder(this@StartActivity)
                .setView(dialogBinding.root)
                .setCancelable(false)
                .create()
            dialogBinding.loader.loadDrawable(R.drawable.g_loading)
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
        }
        fun start() {
            try {
                alertDialog.show()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            lifecycleScope.launch {
                delay(15000)
                if (alertDialog.isShowing) {
                    onlineStatus = "needReload"
                    stop()
                    updateUI(ErrorType.ServerNotResponding)
                }
            }
        }
        fun stop() {
            if(alertDialog.isShowing) {
                try {
                    alertDialog.dismiss()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
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
        val needProfile = pref.getBoolean("needProfile", true)
        when(errorType){
            ErrorType.NoInternet -> {
                if (!needProfile) {
                    dialogBinding.UpdateInfo.text = "Some functionalities are disabled."
                    dialogBinding.buttonUpdate.text = "Continue"
                }
            }
            else -> {
                if (needProfile) {
                    dialogBinding.googlePlayWarning.show()
                    dialogBinding.UpdateInfo.text = "You may need to UPDATE an app.\n(Link Below)"
                }
            }
        }
        dialogBinding.buttonUpdate.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer?.start()
                mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
            }
            alertDialog.dismiss()
            if (pref.getBoolean("needProfile", true))
                recreate()
        }
        dialogBinding.playSvLink.setBounceClickListener {
            dialogBinding.playSvLink.setTextColor(getColor(R.color.teal_700))
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                mediaPlayer?.start()
                mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
            }
            showCustomTab(Constants.PLAY_SERVICES_APP_URL) ?: showOnMarket(Constants.PLAY_SERVICES)
        }
        dialogBinding.playGmLink.setBounceClickListener {
            dialogBinding.playGmLink.setTextColor(getColor(R.color.teal_700))
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                mediaPlayer?.start()
                mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
            }
            showCustomTab(Constants.PLAY_GAMES_APP_URL) ?: showOnMarket(Constants.PLAY_GAMES)
        }
        dialogBinding.restartLink.setBounceClickListener {
            dialogBinding.restartLink.setTextColor(getColor(R.color.teal_700))
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                mediaPlayer?.start()
                mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
            }
            showCustomTab(Constants.RESTART_YOUTUBE_URL)
        }
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
        try { alertDialog.show() }
        catch (ex: Exception) { ex.printStackTrace() }
    }

    fun addSomeBlankHadith(db: FirebaseFirestore, x: Int) {
        for (i in x..x + 10) {
            db.collection("dailyHadith").document(i.toString() + "").set(HadithStore())
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showAHadith() {
//        val hadithList = ArrayList<HadithStore>()
        val db = Firebase.firestore
        db.collection("dailyHadith")
            .count().get(AggregateSource.SERVER)
            .addOnCompleteListener {
                if(!it.isSuccessful) return@addOnCompleteListener
                val totalHadith = it.result.count
                val randDocId = Random().nextInt(totalHadith.toInt()).toString()
                Log.d(TAG, "showAHadith: $randDocId")
                db.collection("dailyHadith").document(randDocId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val hadith = doc.toObject<HadithStore>() ?: return@addOnSuccessListener
                        val builder = AlertDialog.Builder(this@StartActivity)
                        val dialogBinding = DialogLayoutShowHadithBinding.inflate(LayoutInflater.from(this@StartActivity))
                        builder.setView(dialogBinding.root)
                        builder.setCancelable(false)
                        val langBtn = dialogBinding.langBtn
                        val narratorInfo = dialogBinding.narratorInfo
                        val hadithTxt = dialogBinding.hadithTxt
                        val headTxt = dialogBinding.warningMessage
                        if (hadith.t == "h") headTxt.text =
                            "Read a Hadith" else if (hadith.t == "q") headTxt.text =
                            "Read from Quran"
                        if (pref.getString("lang", "bn") == "bn") {
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
                            isNotMuted {
                                val mediaPlayer =
                                    MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                                mediaPlayer?.start()
                                mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
                            }
                            if (langBtn.text == "EN") {
                                narratorInfo.text = hadith.e
                                hadithTxt.text = hadith.en
                                narratorInfo.typeface = resources.getFont(R.font.comfortaa)
                                hadithTxt.typeface = resources.getFont(R.font.comfortaa)
                                hadithTxt.setLineSpacing(7f, 1f)
                                prefEditor.putString("lang", "en").apply()
                                langBtn.text = "BN"
                            } else {
                                narratorInfo.text = hadith.b
                                hadithTxt.text = hadith.bn
                                narratorInfo.typeface = resources.getFont(R.font.paapri)
                                hadithTxt.typeface = resources.getFont(R.font.paapri)
                                hadithTxt.setLineSpacing(0f, 1f)
                                prefEditor.putString("lang", "bn").apply()
                                langBtn.text = "EN"
                            }
                        }
                        dialogBinding.buttonDone.setBounceClickListener {
                            isNotMuted {
                                val mediaPlayer = MediaPlayer.create(
                                    this@StartActivity,
                                    R.raw.btn_click_ef
                                )
                                mediaPlayer?.start()
                                mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
                            }
                            alertDialog.dismiss()
                        }
                        dialogBinding.srcLink.setBounceClickListener {
                            dialogBinding.srcLink.setTextColor(getColor(R.color.teal_700))
                            isNotMuted {
                                val mediaPlayer =
                                    MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                                mediaPlayer?.start()
                                mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
                            }
                            var url = hadith.src
                            if (hadith.t == "q" && langBtn.text == "BN") url =
                                url.replace("bn", "en")
                            showCustomTab(url)
                        }
                        alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
                        try {
                            alertDialog.show()
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
            }
    }

    private fun ifMuted() {
        lifecycleScope.launch {
            binding.volBtn.applyState(isMuted())
        }
    }

    private fun scoreBoard() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
        }
        scrBrdVisible = true
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.replace(R.id.disFragment, DisplayFragment())
        ft.commit()
        binding.linearLayoutStart1.gone()
        binding.linearLayoutStart2.gone()
        binding.motionLayout.gone()
        binding.globalScoreFrag.show()
    }

    private fun goBack() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
        }
        onGoBack()
    }

    private fun onGoBack() {
        scrBrdVisible = false
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.replace(R.id.disFragment, BlankFragment())
        ft.commit()
        binding.linearLayoutStart1.show()
        binding.linearLayoutStart2.show()
        binding.motionLayout.show()
        binding.globalScoreFrag.gone()
    }
    
    fun ideaBtn() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
        }
        infoShow()
    }

    private fun infoShow() {
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
        val builder = AlertDialog.Builder(this@StartActivity)
        val dialogBinding = DialogLayoutInfoBinding.inflate(LayoutInflater.from(this@StartActivity))
        builder.setView(dialogBinding.root)
        builder.setCancelable(false)

        dialogBinding.textMessage.text = msg[0]
        dialogBinding.playGif.loadDrawable(gifs[0])
        dialogBinding.buttonPre.invisible()
        val alertDialog = builder.create()
        dialogBinding.buttonPre.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer?.start()
                mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
            }
            if (i != 0) i--
            if (i == 0) dialogBinding.buttonPre.invisible()
            dialogBinding.textMessage.text = msg[i]
            dialogBinding.playGif.loadDrawable(gifs[i])
        }
        dialogBinding.buttonNext.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer?.start()
                mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
            }
            i++
            if (!isFirstRun && i == 4) i++
            if (i == 1) dialogBinding.buttonPre.show()
            if (i == 5) alertDialog.dismiss() else {
                dialogBinding.textMessage.text = msg[i]
                dialogBinding.playGif.loadDrawable(gifs[i])
            }
        }
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
        try { alertDialog.show() }
        catch (npe: NullPointerException) { npe.printStackTrace() }
    }

    @SuppressLint("SetTextI18n")
    fun startBtn() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
        }
        if (binding.mode1.alpha < .5) {
            if (onlineStatus == "pass") {
                startActivity(Intent(this, MultiplayerActivity::class.java).putExtra("playerId", playerId))
                //finish()
            } else if (onlineStatus == "needReload") {
                //updateUI()
                val builder = AlertDialog.Builder(this@StartActivity)
                val dialogBinding = DialogLayoutUpdateuiBinding.inflate(LayoutInflater.from(this@StartActivity))

                builder.setView(dialogBinding.root)
                builder.setCancelable(false)
                dialogBinding.googlePlayWarning.gone()
                dialogBinding.UpdateInfo.text = "You must have INTERNET connection to play in ONLINE mode"
                val alertDialog = builder.create()
                dialogBinding.buttonUpdate.setBounceClickListener {
                    isNotMuted {
                        val mediaPlayer = MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                        mediaPlayer?.start()
                        mediaPlayer?.setOnCompletionListener(MediaPlayer::release)
                    }
                    recreate()
                    alertDialog.dismiss()
                }
                alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
                try { alertDialog.show() }
                catch (ex: Exception) { ex.printStackTrace() }
            }
        } else if (binding.mode3.alpha < .5){
            startActivity(Intent(this, GameActivity1::class.java))
            //finish()
        } else{
            startActivity(Intent(this, GameActivity3::class.java))
            //finish()
        }
    }


}