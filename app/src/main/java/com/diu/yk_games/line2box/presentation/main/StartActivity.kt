package com.diu.yk_games.line2box.presentation.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.diu.yk_games.line2box.BuildConfig
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.ActivityStartBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutAlertBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutInfoBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutLoadingBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutShowHadithBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutUpdateBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutUpdateuiBinding
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.model.HadithStore
import com.diu.yk_games.line2box.presentation.BlankFragment
import com.diu.yk_games.line2box.presentation.bot.GameActivity3
import com.diu.yk_games.line2box.presentation.offline.GameActivity1
import com.diu.yk_games.line2box.presentation.online.MultiplayerActivity
import com.diu.yk_games.line2box.util.hideSystemBars
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.google.android.gms.games.*
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PlayGamesAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class StartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartBinding
    private var scrBrdVisible = false
    private var isFirstRun: Boolean = false
    private lateinit var preferences: SharedPreferences
    private lateinit var preferencesEditor: SharedPreferences.Editor

    companion object {
        private var errorCnt = 0
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
    private lateinit var mAuth: FirebaseAuth
    lateinit var context: Context
//    private var mode1: ImageView? = null
//    private var mode2: ImageView? = null
//    private var mode3: ImageView? = null
    private lateinit var loadingUI: LoadingUI
    private lateinit var onlineStatus: String

    // ...
    // Initialize Firebase Auth
    //    @Override
    //    protected void onStart()
    //    {
    //        super.onStart();
    //
    //    }
    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("VisibleForTests")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = Firebase.auth
        PlayGamesSdk.initialize(this)
        context = this
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        mode1 = findViewById(R.id.mode1)
//        mode2 = findViewById(R.id.mode2)
//        mode3 = findViewById(R.id.mode3)
        loadingUI = LoadingUI()
        loadingUI.start()
        val db = FirebaseFirestore.getInstance()
        preferences = getSharedPreferences(
            getString(R.string.preference_file_key), MODE_PRIVATE
        )
        //SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ApplicationConstants.PREFERENCES, Context.MODE_PRIVATE);
        preferencesEditor = preferences.edit()
        GameProfile.setPreferences(preferences)
        //if (!isFirstRun)
        isFirstRun = preferences.getBoolean("firstRun", true)

        window.hideSystemBars()

        //if(isFirstRun)

//        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
//                .requestServerAuthCode(getString(R.string.default_web_client_id))
//                .build();
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

        binding.globalScoreFrag.visibility = View.GONE
        binding.startBtnId.setBounceClickListener {
            startBtn(it)
        }
        binding.volBtn.setBounceClickListener {
            volButton(it)
        }
        binding.ideaBtn.setBounceClickListener {
            ideaBtn(it)
        }
        binding.scrBrdBtn.setBounceClickListener {
            scoreBoard(it)
        }
        binding.goBackBtn.setBounceClickListener {
            goBack(it)
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
                        if (!isMuted) {
                            val mediaPlayer = MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                        }
                        alertDialog.dismiss()
                        finish()
                    }
                    dialogBinding.buttonNo.setBounceClickListener {
                        if (!isMuted) {
                            val mediaPlayer = MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
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
        val auth = Firebase.auth
        val gamesSignInClient = PlayGames.getGamesSignInClient(this)
        gamesSignInClient.isAuthenticated.addOnCompleteListener { isAuthenticatedTask ->
            val isAuthenticated = isAuthenticatedTask.isSuccessful &&
                    isAuthenticatedTask.result.isAuthenticated
            gamesSignInClient.requestServerSideAccess(getString(R.string.default_web_client_id),  /*forceRefreshToken=*/false)
                .addOnCompleteListener { task: Task<String?> ->
                    if (task.isSuccessful) {
                        isUpdateAvailable
                        val serverAuthToken = task.result!!
                        //Toast.makeText(this, "serverAuthToken- "+serverAuthToken, Toast.LENGTH_SHORT).show();
                        val credential = PlayGamesAuthProvider.getCredential(serverAuthToken)
                        //AuthCredential credential = PlayGamesAuthProvider.getCredential(PlayGamesAuthProvider.PLAY_GAMES_SIGN_IN_METHOD);
                        auth.signInWithCredential(credential)
                            .addOnCompleteListener(this
                            ) { task1 ->
                                if (task1.isSuccessful) {
                                    // Sign in success, update UI with the signed-in user's information

                                    //Log.d(TAG, "signInWithCredential: success");
                                    if (showHadith && isFirstRun) {
                                        showAHadith()
                                        showHadith = false
                                    }
                                    val user = auth.currentUser
                                    if (isAuthenticated) {
                                        PlayGames.getPlayersClient(this@StartActivity).currentPlayer.addOnCompleteListener { mTask ->
                                            playerId = mTask.result.playerId
                                            //Toast.makeText(StartActivity.this, "id: "+mTask.getResult().getPlayerId() , Toast.LENGTH_SHORT).show();
                                            if (preferences.getBoolean("needProfile", true)) {
                                                val gameProfile = GameProfile()
                                                db.collection("gamerProfile")
                                                    .document(playerId)
                                                    .get().addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            val document = task.result
                                                            if (document.exists()) {
                                                                preferencesEditor.putBoolean(
                                                                    "needProfile",
                                                                    false
                                                                ).apply()
                                                                loadProfileFromServer(db)
                                                                if (loadingUI.visibility) {
                                                                    onlineStatus = "pass"
                                                                    loadingUI.stop()
                                                                }
                                                                //Log.d(TAG, "Profile exists!");
                                                                Toast.makeText(
                                                                    this@StartActivity,
                                                                    "Profile Exists and Loaded!",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            } else {
                                                                //Log.d(TAG, "Profile does not exist!");
                                                                //Toast.makeText(StartActivity.this, "Profile does not exist!", Toast.LENGTH_SHORT).show();
                                                                gameProfile.playerId = playerId
                                                                db.collection("gamerProfile")
                                                                    .document(playerId)
                                                                    .set(gameProfile)
                                                                    .addOnSuccessListener {
                                                                        preferencesEditor.putBoolean(
                                                                            "needProfile",
                                                                            false
                                                                        ).apply()
                                                                        gameProfile.apply()
                                                                        if (loadingUI.visibility) {
                                                                            onlineStatus = "pass"
                                                                            loadingUI.stop()
                                                                        }
                                                                        //Log.d(TAG, "onSuccess: Profile Created");
                                                                        //Toast.makeText(StartActivity.this, "onSuccess: Profile Created", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                    .addOnFailureListener { //Log.d("TAG", "onSuccess: Profile Creation Failed");
                                                                        //Toast.makeText(StartActivity.this, "onSuccess: Profile Creation Failed", Toast.LENGTH_SHORT).show();
                                                                        if (loadingUI.visibility) {
                                                                            onlineStatus =
                                                                                "needReload"
                                                                            loadingUI.stop()
                                                                        }
                                                                    }
                                                                getLocation(db)
                                                            }
                                                        } else {
                                                            //Log.d(TAG, "Failed with: ", task.getException());
                                                            if (loadingUI.visibility) {
                                                                onlineStatus = "needReload"
                                                                loadingUI.stop()
                                                            }
                                                        }
                                                    }
                                            } else {
                                                loadProfileFromServer(db)
                                                onlineStatus = "pass"
                                                loadingUI.stop()
                                            }
                                        }

                                        // Continue with Play Games Services
                                    } else {
                                        //Toast.makeText(StartActivity.this, "Failed", Toast.LENGTH_SHORT).show();

                                        // Disable your integration with Play Games Services or show a
                                        // login button to ask  players to sign-in. Clicking it should
                                        // call GamesSignInClient.signIn();
                                        updateUI(null)
                                        if (loadingUI.visibility) {
                                            onlineStatus = "needReload"
                                            loadingUI.stop()
                                        }
                                    }
                                    updateUI(user)
                                } else {
                                    // If sign in fails, display a message to the user.
                                    //Log.d(TAG, "signInWithCredential: failure", task.getException());
                                    //Toast.makeText(StartActivity.this, "Authentication failed.",Toast.LENGTH_SHORT).show();
                                    updateUI(null)
                                    if (loadingUI.visibility) {
                                        onlineStatus = "needReload"
                                        loadingUI.stop()
                                    }
                                }

                                // ...
                            }
                    } else {
                        // Failed to retrieve authentication code.
                        //Log.d(TAG, "signInWithCredential:failure", task.getException());
                        //Toast.makeText(StartActivity.this, "No Internet.",Toast.LENGTH_SHORT).show();
                        if (isAuthenticated) {
                            PlayGames.getPlayersClient(this@StartActivity).currentPlayer.addOnCompleteListener { mTask: Task<Player> ->
                                GameProfile()
                                playerId = mTask.result.playerId
                            }
                        }
                        updateUI(null)
                        if (loadingUI.visibility) {
                            onlineStatus = "needReload"
                            loadingUI.stop()
                        }
                    }
                }
        }


        //gamesSignInClient.signIn();
        //vsRadioGrp=findViewById(R.id.vsRadioGrp);
        ifMuted()

    }

    private fun loadProfileFromServer(db: FirebaseFirestore) {
        db.collection("gamerProfile")
//            .whereEqualTo("playerId",playerId)
            .document(playerId).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val server2device = documentSnapshot.toObject(GameProfile::class.java)!!
                    server2device.apply()
                    //remove some day
                    getLocation(db)
                }
            }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun getLocation(db: FirebaseFirestore) {
        GlobalScope.launch(Dispatchers.IO) {
            val bodyTxt: String
            try {
                Log.d(TAG, "getLocation: Success")
                val url = "http://ip-api.com/json/?fields=country,city,query"
                val doc = Jsoup.connect(url).ignoreContentType(true).get()
                val body = doc.body()
                bodyTxt = body.text() //.replace("\"","\\\"");
                Log.d(TAG, "getLocation: $bodyTxt")
                //runOnUiThread(() -> {
                //result.setText(builder.toString());
                //JsonElement jelem = gson.fromJson(json, JsonElement.class);
                val g = GsonBuilder().serializeNulls().create()
                val je = g.fromJson(bodyTxt, JsonElement::class.java)
                val jd = je.asJsonObject
                Log.d(TAG, "JsonData.class ip: $jd")
                preferencesEditor.putString("cityNm", jd["city"].asString).apply()
                preferencesEditor.putString("query", jd["query"].asString).apply()
                var country = jd["country"].asString
                var tmp = 0
                if (country == "Israel") {
                    country = "Palestine"
                    tmp = 1
                }
                val index = countryNm.indexOf(jd["country"].asString)
                Log.d(TAG, "onCreate: index $index")
                if (index != -1)
                    preferencesEditor.putString("countryEmoji", countryEmojis[index]).apply()
                if (tmp == 1)
                    country = "Palestina"
                preferencesEditor.putString("countryNm", country).apply()
                Log.d(TAG, "onCreate: emo " + preferences.getString("countryEmoji", ""))
                val upLoc = GameProfile()
                upLoc.playerId = playerId
                upLoc.countryEmoji = preferences.getString("countryEmoji", "")!!
                upLoc.countryNm = preferences.getString("countryNm", "")!!
                //if(!upLoc.countryNm.equals(""))
                db.collection("gamerProfile").document(playerId).set(upLoc)
                //});
            } catch (e: Exception) {
                //builder.append("Error : ").append(e.getMessage()).append("\n");
                e.printStackTrace()
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
        private var builder = AlertDialog.Builder(this@StartActivity)
        private val dialogBinding = DialogLayoutLoadingBinding.inflate(LayoutInflater.from(this@StartActivity))

//        val view = LayoutInflater.from(this@StartActivity).inflate(
//            R.layout.dialog_layout_loading, findViewById(R.id.updateLayoutLoadingUI)
//        )!!
        private lateinit var alertDialog: AlertDialog
        var visibility = false
        @OptIn(DelicateCoroutinesApi::class)
        fun start() {
            builder.setView(dialogBinding.root)
            builder.setCancelable(false)
            alertDialog = builder.create()
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
            try {
                alertDialog.show()
                visibility = true
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            GlobalScope.launch(Dispatchers.Main) {
                delay(15000)
                if (visibility) {
                    onlineStatus = "needReload"
                    stop()
//                    Looper.prepare()
                    updateUI(null)
//                    Looper.loop()
                }
            }
        }

        fun stop() {
            alertDialog.dismiss()
            visibility = false
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser == null) {
            val builder = AlertDialog.Builder(this@StartActivity)
            val dialogBinding = DialogLayoutUpdateuiBinding.inflate(LayoutInflater.from(this@StartActivity))
            builder.setView(dialogBinding.root)
            builder.setCancelable(false)
            val alertDialog = builder.create()
            dialogBinding.googlePlayWarning.visibility = View.GONE
            errorCnt++
            if (errorCnt > 2 && preferences.getBoolean("needProfile", true)) {
                dialogBinding.googlePlayWarning.visibility = View.VISIBLE
                dialogBinding.warningMessage.text = "Warning !"
                dialogBinding.UpdateInfo.text = "You may need to UPDATE those two apps. (Link Below)"
                dialogBinding.playSvLink.setBounceClickListener {
                    dialogBinding.playSvLink.setTextColor(getColor(R.color.teal_700))
                    if (!isMuted) {
                        val mediaPlayer = MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                    }
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.gms")
                        )
                    )
                }
                dialogBinding.playGmLink.setBounceClickListener {
                    dialogBinding.playGmLink.setTextColor(getColor(R.color.teal_700))
                    if (!isMuted) {
                        val mediaPlayer = MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                    }
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.play.games")
                        )
                    )
                }
            }
            if (isFirstRun) {
                dialogBinding.buttonUpdate.setBounceClickListener {
                    if (!isMuted) {
                        val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                    }
                    recreate()
                    alertDialog.dismiss()
                }
            } else {
                //findViewById(R.id.scrBrdBtn).setEnabled(false);
                if (errorCnt < 3 || !preferences.getBoolean("needProfile", true))
                    dialogBinding.UpdateInfo.text = "Some functionalities are disabled."
                dialogBinding.buttonUpdate.text = "Continue"
                dialogBinding.buttonUpdate.setBounceClickListener {
                    if (!isMuted) {
                        val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                    }
                    alertDialog.dismiss()
                }
            }
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
            try { alertDialog.show() }
            catch (ex: Exception) { ex.printStackTrace() }
        }
    }

    fun addSomeBlankHadith(db: FirebaseFirestore, x: Int) {
        for (i in x..x + 10) {
            db.collection("dailyHadith").document(i.toString() + "").set(HadithStore())
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showAHadith() {
//        val hadithList = ArrayList<HadithStore>()
        val db = FirebaseFirestore.getInstance()
        db.collection("dailyHadith")
            .count().get(AggregateSource.SERVER)
            .addOnSuccessListener {
                val totalHadith = it.count + 1
                val randDocId = Random().nextInt(totalHadith.toInt()).toString()
                Log.d(TAG, "showAHadith: $randDocId")
                db.collection("dailyHadith").document(randDocId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val hadith = doc.toObject(HadithStore::class.java)!!
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
                        if (preferences.getString("lang", "bn") == "bn") {
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
                            if (!isMuted) {
                                val mediaPlayer =
                                    MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                                mediaPlayer.start()
                                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                            }
                            if (langBtn.text == "EN") {
                                narratorInfo.text = hadith.e
                                hadithTxt.text = hadith.en
                                narratorInfo.typeface = resources.getFont(R.font.comfortaa)
                                hadithTxt.typeface = resources.getFont(R.font.comfortaa)
                                hadithTxt.setLineSpacing(7f, 1f)
                                preferencesEditor.putString("lang", "en").apply()
                                langBtn.text = "BN"
                            } else {
                                narratorInfo.text = hadith.b
                                hadithTxt.text = hadith.bn
                                narratorInfo.typeface = resources.getFont(R.font.paapri)
                                hadithTxt.typeface = resources.getFont(R.font.paapri)
                                hadithTxt.setLineSpacing(0f, 1f)
                                preferencesEditor.putString("lang", "bn").apply()
                                langBtn.text = "EN"
                            }
                        }
                        dialogBinding.buttonDone.setBounceClickListener {
                            if (!isMuted) {
                                val mediaPlayer = MediaPlayer.create(
                                    this@StartActivity,
                                    R.raw.btn_click_ef
                                )
                                mediaPlayer.start()
                                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                            }
                            alertDialog.dismiss()
                        }
                        dialogBinding.srcLink.setBounceClickListener {
                            dialogBinding.srcLink.setTextColor(getColor(R.color.teal_700))
                            if (!isMuted) {
                                val mediaPlayer =
                                    MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                                mediaPlayer.start()
                                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                            }
                            var uri = hadith.src
                            if (hadith.t == "q" && langBtn.text == "BN") uri =
                                uri.replace("bn", "en")
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
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
    private val isUpdateAvailable: Unit
        get() {
            val context: Context = this
            val localVersionCode = BuildConfig.VERSION_CODE
            val database = FirebaseDatabase.getInstance()
            val myRef = database.getReference("versionCode")
            myRef.addValueEventListener(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    val onlineVersionCode = dataSnapshot.getValue(Int::class.java)!!
                    if (localVersionCode < Objects.requireNonNull(onlineVersionCode)) {
                        val builder = AlertDialog.Builder(context)
                        val dialogBinding = DialogLayoutUpdateBinding.inflate(LayoutInflater.from(this@StartActivity))

                        builder.setView(dialogBinding.root)
                        val alertDialog = builder.create()
                        dialogBinding.buttonUpdate.setBounceClickListener {
                            if (!isMuted) {
                                val mediaPlayer =
                                    MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                                mediaPlayer.start()
                                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                            }
                            val appPackageName =
                                packageName // getPackageName() from Context or Activity object
                                //                        try {
//                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
//                        } catch (android.content.ActivityNotFoundException ante) {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                                )
                            )
                                //}
                            alertDialog.dismiss()
                        }
                        alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
                        try { alertDialog.show() }
                        catch (ex: Exception) { ex.printStackTrace() }
                    }
                    ////Log.d(TAG, "Last Value is: " + bestScore);
                }

                override fun onCancelled(error: DatabaseError) {
                    // Failed to read value
                    //Log.d("TAG", "Failed to read value.", error.toException());
                }
            })
        }
    val isMuted: Boolean
        get() = preferences.getBoolean("muted", false)

    private fun ifMuted() {
        if (isMuted) {
            findViewById<View>(R.id.volBtn).setBackgroundResource(R.drawable.btn_gry_bg)
            (findViewById<View>(R.id.volBtn) as ImageButton).setImageResource(R.drawable.icon_vol_mute)
        } else {
            findViewById<View>(R.id.volBtn).setBackgroundResource(R.drawable.btn_ylw_bg)
            (findViewById<View>(R.id.volBtn) as ImageButton).setImageResource(R.drawable.icon_vol_unmute)
        }
    }


    fun scoreBoard(view: View) {
        if (!isMuted) {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        scrBrdVisible = true
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.replace(R.id.disFragment, DisplayFragment())
        ft.commit()
        findViewById<View>(R.id.linearLayoutStart1).visibility = View.GONE
        findViewById<View>(R.id.linearLayoutStart2).visibility = View.GONE
        findViewById<View>(R.id.motionLayout).visibility = View.GONE
        findViewById<View>(R.id.globalScoreFrag).visibility = View.VISIBLE
    }

    fun goBack(view: View) {
        if (!isMuted) {
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
        findViewById<View>(R.id.linearLayoutStart1).visibility = View.VISIBLE
        findViewById<View>(R.id.linearLayoutStart2).visibility = View.VISIBLE
        findViewById<View>(R.id.motionLayout).visibility = View.VISIBLE
        findViewById<View>(R.id.globalScoreFrag).visibility = View.GONE
    }

    fun volButton(view: View) {
        if (!isMuted) {
            findViewById<View>(R.id.volBtn).setBackgroundResource(R.drawable.btn_gry_bg)
            (findViewById<View>(R.id.volBtn) as ImageButton).setImageResource(R.drawable.icon_vol_mute)
            preferencesEditor.putBoolean("muted", true).apply()
        } else {
            run {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            findViewById<View>(R.id.volBtn).setBackgroundResource(R.drawable.btn_ylw_bg)
            (findViewById<View>(R.id.volBtn) as ImageButton).setImageResource(R.drawable.icon_vol_unmute)
            preferencesEditor.putBoolean("muted", false).apply()
        }
    }

    fun ideaBtn(view: View) {
        if (!isMuted) {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        infoShow()
    }

    private fun infoShow() {
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
        val builder = AlertDialog.Builder(this@StartActivity)
        val dialogBinding = DialogLayoutInfoBinding.inflate(LayoutInflater.from(this@StartActivity))
        builder.setView(dialogBinding.root)
        builder.setCancelable(false)

        dialogBinding.textMessage.text = msg[0]
        dialogBinding.playGif.setImageResource(gifs[0])
        dialogBinding.buttonPre.visibility = View.INVISIBLE
        val alertDialog = builder.create()
        dialogBinding.buttonPre.setBounceClickListener {
            if (!isMuted) {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            if (i.get() != 0) i.getAndDecrement()
            if (i.get() == 0) dialogBinding.buttonPre.visibility = View.INVISIBLE
            dialogBinding.textMessage.text = msg[i.get()]
            dialogBinding.playGif.setImageResource(gifs[i.get()])
        }
        dialogBinding.buttonNext.setBounceClickListener {
            if (!isMuted) {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            i.getAndIncrement()
            if (!isFirstRun && i.get() == 4) i.getAndIncrement()
            if (i.get() == 1) dialogBinding.buttonPre.visibility = View.VISIBLE
            if (i.get() == 5) alertDialog.dismiss() else {
                dialogBinding.textMessage.text = msg[i.get()]
                dialogBinding.playGif.setImageResource(gifs[i.get()])
            }
        }
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
        try { alertDialog.show() }
        catch (npe: NullPointerException) { npe.printStackTrace() }
    }

    @SuppressLint("SetTextI18n")
    fun startBtn(view: View) {
        if (!isMuted) {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
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
                dialogBinding.googlePlayWarning.visibility = View.GONE
                dialogBinding.UpdateInfo.text = "You must have INTERNET connection to play in ONLINE mode"
                val alertDialog = builder.create()
                dialogBinding.buttonUpdate.setBounceClickListener {
                    if (!isMuted) {
                        val mediaPlayer = MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                    }
                    recreate()
                    alertDialog.dismiss()
                }
                alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
                try { alertDialog.show() }
                catch (ex: Exception) { ex.printStackTrace() }
            }
        } else if (binding.mode3.alpha < .5) //(mode3.getVisibility()==View.INVISIBLE)
        {
            startActivity(Intent(this, GameActivity1::class.java))
            //finish()
        } else {
            startActivity(Intent(this, GameActivity3::class.java))
            //finish()
        }
    }


}