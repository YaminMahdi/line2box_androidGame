package com.diu.yk_games.line2box.presentation.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.diu.yk_games.line2box.BuildConfig
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.ActivityStartBinding
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.model.HadithStore
import com.diu.yk_games.line2box.presentation.BlankFragment
import com.diu.yk_games.line2box.presentation.bot.GameActivity3
import com.diu.yk_games.line2box.presentation.offline.GameActivity1
import com.diu.yk_games.line2box.presentation.online.MultiplayerActivity
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import pl.droidsonroids.gif.GifImageView
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class StartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartBinding
    private var scrBrdVisible = false
    private var isFirstRun: Boolean = false
    private lateinit var preferences: SharedPreferences
    private lateinit var preferencesEditor: SharedPreferences.Editor

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
            "Afghanistan", "Albania", "Algeria", "Andorra", "Angola", "Antigua and Barbuda", "Argentina", "Armenia", "Australia", "Austria", "Azerbaijan", "Bahamas", "Bahrain", "Bangladesh", "Barbados", "Belarus", "Belgium", "Belize", "Benin", "Bhutan", "Bolivia", "Bosnia and Herzegovina", "Botswana", "Brazil", "Brunei", "Bulgaria", "Burkina Faso", "Burundi", "Cabo Verde", "Cambodia", "Cameroon", "Canada", "Central African Republic", "Chad", "Chile", "China", "Colombia", "Comoros", "Congo", "Costa Rica", "Croatia", "Cuba", "Cyprus", "Czechia", "Côte d'Ivoire", "Denmark", "Djibouti", "Dominica", "Dominican Republic", "DR Congo",
            "Ecuador", "Egypt", "El Salvador", "England", "Equatorial Guinea", "Eritrea", "Estonia", "Eswatini (Swaziland)", "Ethiopia", "Fiji", "Finland", "France", "Gabon", "Gambia", "Georgia", "Germany", "Ghana", "Greece", "Grenada", "Guatemala", "Guinea", "Guinea-Bissau", "Guyana", "Haiti", "Honduras", "Hong Kong", "Hungary", "Iceland", "India", "Indonesia", "Iran", "Iraq", "Ireland", "Israel", "Italy", "Jamaica", "Japan", "Jordan", "Kazakhstan", "Kenya", "Kiribati", "Kuwait", "Kyrgyzstan", "Laos", "Latvia", "Lebanon", "Lesotho", "Liberia", "Libya", "Liechtenstein", "Lithuania", "Luxembourg",
            "Madagascar", "Malawi", "Malaysia", "Maldives", "Mali", "Malta", "Marshall Islands", "Martinique", "Mauritius", "Mexico", "Micronesia", "Moldova", "Monaco", "Mongolia", "Montenegro", "Morocco", "Mozambique", "Myanmar", "Namibia", "Nauru", "Nepal", "Netherlands", "New Zealand", "Nicaragua", "Niger", "Nigeria", "North Korea", "North Macedonia", "Norway", "Oman", "Pakistan", "Palau", "Palestine", "Panama", "Papua New Guinea", "Paraguay", "Peru", "Philippines", "Poland", "Portugal", "Qatar", "Romania", "Russia", "Rwanda", "Saint Kitts and Nevis", "Saint Lucia", "Saint Vincent", "Samoa", "San Marino", "São Tomé and Príncipe",
            "Saudi Arabia", "Scotland", "Senegal", "Serbia", "Seychelles", "Sierra Leone", "Singapore", "Slovakia", "Slovenia", "Solomon Islands", "Somalia", "South Africa", "South Korea", "South Sudan", "Spain", "Sri Lanka", "Sudan", "Suriname", "Sweden", "Switzerland", "Syria", "Taiwan", "Tajikistan", "Tanzania", "Thailand", "Timor-Leste", "Togo", "Tonga", "Trinidad and Tobago", "Tunisia", "Turkey", "Turkmenistan", "Tuvalu", "Uganda", "Ukraine", "United Arab Emirates", "United Kingdom", "United States", "Uruguay", "Uzbekistan", "Vanuatu", "Venezuela", "Vietnam", "Yemen", "Zambia", "Zimbabwe"
        )
    )
    private lateinit var mAuth: FirebaseAuth
    lateinit var context: Context
//    private var mode1: ImageView? = null
//    private var mode2: ImageView? = null
//    private var mode3: ImageView? = null
    private lateinit var loadingUI: LoadingUI
    lateinit var onlineStatus: String

    // ...
    // Initialize Firebase Auth
    //    @Override
    //    protected void onStart()
    //    {
    //        super.onStart();
    //
    //    }
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


        //if(isFirstRun)

//        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
//                .requestServerAuthCode(getString(R.string.default_web_client_id))
//                .build();
        //window.insetsController?.hide(WindowInsets.Type.statusBars())
        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        findViewById<View>(R.id.globalScoreFrag).visibility = View.GONE
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
                                                    .document(playerId!!)
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
                                                                gameProfile.playerId =
                                                                    playerId
                                                                db.collection("gamerProfile")
                                                                    .document(playerId!!)
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

        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            @SuppressLint("SetTextI18n")
            override fun handleOnBackPressed() {
                if (scrBrdVisible) {
                    onGoBack()
                } else
                {
                    val builder = AlertDialog.Builder(this@StartActivity)
                    val view = LayoutInflater.from(this@StartActivity).inflate(
                        R.layout.dialog_layout_alert, findViewById(R.id.layoutDialog)
                    )
                    builder.setView(view)
                    (view.findViewById<View>(R.id.textMessage) as TextView).text =
                        "Do you really want to exit?"
                    (view.findViewById<View>(R.id.buttonYes) as Button).text = "YES"
                    (view.findViewById<View>(R.id.buttonNo) as Button).text = "NO"
                    val alertDialog = builder.create()
                    view.findViewById<View>(R.id.buttonYes).setOnClickListener {
                        if (!isMuted) {
                            val mediaPlayer = MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
                        }
                        alertDialog.dismiss()
                        finish()
                    }
                    view.findViewById<View>(R.id.buttonNo).setOnClickListener {
                        if (!isMuted) {
                            val mediaPlayer = MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
                        }
                        alertDialog.dismiss()
                    }
                    if (alertDialog.window != null) {
                        alertDialog.window!!.setBackgroundDrawable(ColorDrawable(0))
                    }
                    try {
                        alertDialog.show()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        })
    }

    private fun loadProfileFromServer(db: FirebaseFirestore) {
        db.collection("gamerProfile").document(playerId!!)
            .get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val server2device = documentSnapshot.toObject(
                        GameProfile::class.java
                    )!!
                    server2device.apply()
                    //remove some day
                    getLocation(db)
                }
            }
    }

    private fun getLocation(db: FirebaseFirestore) {
        Thread {
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
                if (index != -1) preferencesEditor.putString("countryEmoji", countryEmojis[index])
                    .apply()
                if (tmp == 1) country = "Palestina"
                preferencesEditor.putString("countryNm", country).apply()
                Log.d(TAG, "onCreate: emo " + preferences.getString("countryEmoji", ""))
                val upLoc = GameProfile()
                upLoc.playerId = playerId
                upLoc.countryEmoji = preferences.getString("countryEmoji", "")
                upLoc.countryNm = preferences.getString("countryNm", "")
                //if(!upLoc.countryNm.equals(""))
                db.collection("gamerProfile").document(playerId!!).set(upLoc)
                //});
            } catch (e: Exception) {
                //builder.append("Error : ").append(e.getMessage()).append("\n");
                e.printStackTrace()
            }
        }.start()
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
        val view = LayoutInflater.from(this@StartActivity).inflate(
            R.layout.dialog_layout_loading, findViewById(R.id.updateLayoutLoadingUI)
        )!!
        private lateinit var alertDialog: AlertDialog
        var visibility = false
        @OptIn(DelicateCoroutinesApi::class)
        fun start() {
            builder.setView(view)
            builder.setCancelable(false)
            alertDialog = builder.create()
            if (alertDialog.window != null) alertDialog.window!!
                .setBackgroundDrawable(ColorDrawable(0))
            try {
                alertDialog.show()
                visibility = true
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            GlobalScope.launch {
                delay(15000)
                if (visibility) {
                    onlineStatus = "needReload"
                    stop()
                    updateUI(null)
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
            val view = LayoutInflater.from(this@StartActivity).inflate(
                R.layout.dialog_layout_updateui, findViewById(R.id.updateLayoutDialogUI)
            )
            builder.setView(view)
            builder.setCancelable(false)
            val alertDialog = builder.create()
            view.findViewById<View>(R.id.googlePlayWarning).visibility = View.GONE
            errorCnt++
            if (errorCnt > 2) {
                view.findViewById<View>(R.id.googlePlayWarning).visibility = View.VISIBLE
                (view.findViewById<View>(R.id.warningMessage) as TextView).text = "Warning !"
                (view.findViewById<View>(R.id.UpdateInfo) as TextView).text =
                    "You may need to UPDATE those two apps. (Link Below)"
                view.findViewById<View>(R.id.playSvLink).setOnClickListener {
                    (view.findViewById<View>(R.id.playSvLink) as TextView).setTextColor(getColor(R.color.teal_700))
                    if (!isMuted) {
                        val mediaPlayer = MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
                    }
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.gms")
                        )
                    )
                }
                view.findViewById<View>(R.id.playGmLink).setOnClickListener {
                    (view.findViewById<View>(R.id.playGmLink) as TextView).setTextColor(getColor(R.color.teal_700))
                    if (!isMuted) {
                        val mediaPlayer = MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
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
                view.findViewById<View>(R.id.buttonUpdate).setOnClickListener {
                    if (!isMuted) {
                        val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
                    }
                    recreate()
                    alertDialog.dismiss()
                }
            } else {
                //findViewById(R.id.scrBrdBtn).setEnabled(false);
                if (errorCnt < 3) (view.findViewById<View>(R.id.UpdateInfo) as TextView).text =
                    "Some functionalities are disabled."
                (view.findViewById<View>(R.id.buttonUpdate) as Button).text = "Continue"
                view.findViewById<View>(R.id.buttonUpdate).setOnClickListener {
                    if (!isMuted) {
                        val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
                    }
                    alertDialog.dismiss()
                }
            }
            if (alertDialog.window != null) {
                alertDialog.window!!.setBackgroundDrawable(ColorDrawable(0))
            }
            try {
                alertDialog.show()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun addSomeBlankHadith(db: FirebaseFirestore, x: Int) {
        for (i in x..x + 10) {
            db.collection("dailyHadith").document(i.toString() + "").set(HadithStore())
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showAHadith() {
        val hadithList = ArrayList<HadithStore>()
        val db = FirebaseFirestore.getInstance()
        db.collection("dailyHadith")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result) {
                        val h = document.toObject(HadithStore::class.java)
                        //                                if(h.b.equals(""))
                        //                                    db.collection("dailyHadith").document(document.getId()).delete();
                        //                                else
                        hadithList.add(h)
                    }
                    if (hadithList.size > 0) {
                        //AddSomeBlankHadith(db, hadithList.size());
                        val index = Random().nextInt(hadithList.size)
                        val builder = AlertDialog.Builder(this@StartActivity)
                        val view = LayoutInflater.from(this@StartActivity).inflate(
                            R.layout.dialog_layout_show_hadith,
                            findViewById(R.id.hadithLayoutDialog)
                        )
                        builder.setView(view)
                        builder.setCancelable(false)
                        val langBtn = view.findViewById<Button>(R.id.langBtn)
                        val narratorInfo = view.findViewById<TextView>(R.id.narratorInfo)
                        val hadithTxt = view.findViewById<TextView>(R.id.hadithTxt)
                        val headTxt = view.findViewById<TextView>(R.id.warningMessage)
                        if (hadithList[index].t == "h") headTxt.text =
                            "Read a Hadith" else if (hadithList[index].t == "q") headTxt.text =
                            "Read from Quran"
                        if (preferences.getString("lang", "bn") == "bn") {
                            narratorInfo.text = hadithList[index].b
                            hadithTxt.text = hadithList[index].bn
                            narratorInfo.typeface = resources.getFont(R.font.paapri)
                            hadithTxt.typeface = resources.getFont(R.font.paapri)
                            hadithTxt.setLineSpacing(0f, 1f)
                            langBtn.text = "EN"
                        } else {
                            narratorInfo.text = hadithList[index].e
                            hadithTxt.text = hadithList[index].en
                            narratorInfo.typeface = resources.getFont(R.font.comfortaa)
                            hadithTxt.typeface = resources.getFont(R.font.comfortaa)
                            hadithTxt.setLineSpacing(7f, 1f)
                            langBtn.text = "BN"
                        }
                        (view.findViewById<View>(R.id.hadithInfo) as TextView).text =
                            hadithList[index].ref
                        val alertDialog = builder.create()
                        langBtn.setOnClickListener {
                            if (!isMuted) {
                                val mediaPlayer =
                                    MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                                mediaPlayer.start()
                                mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
                            }
                            if (langBtn.text == "EN") {
                                narratorInfo.text = hadithList[index].e
                                hadithTxt.text = hadithList[index].en
                                narratorInfo.typeface = resources.getFont(R.font.comfortaa)
                                hadithTxt.typeface = resources.getFont(R.font.comfortaa)
                                hadithTxt.setLineSpacing(7f, 1f)
                                preferencesEditor.putString("lang", "en").apply()
                                langBtn.text = "BN"
                            } else {
                                narratorInfo.text = hadithList[index].b
                                hadithTxt.text = hadithList[index].bn
                                narratorInfo.typeface = resources.getFont(R.font.paapri)
                                hadithTxt.typeface = resources.getFont(R.font.paapri)
                                hadithTxt.setLineSpacing(0f, 1f)
                                preferencesEditor.putString("lang", "bn").apply()
                                langBtn.text = "EN"
                            }
                        }
                        view.findViewById<View>(R.id.buttonDone)
                            .setOnClickListener {
                                if (!isMuted) {
                                    val mediaPlayer = MediaPlayer.create(
                                        this@StartActivity,
                                        R.raw.btn_click_ef
                                    )
                                    mediaPlayer.start()
                                    mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
                                }
                                alertDialog.dismiss()
                            }
                        view.findViewById<View>(R.id.srcLink).setOnClickListener {
                            (view.findViewById<View>(R.id.srcLink) as TextView).setTextColor(
                                getColor(R.color.teal_700)
                            )
                            if (!isMuted) {
                                val mediaPlayer =
                                    MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                                mediaPlayer.start()
                                mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
                            }
                            var uri = hadithList[index].src
                            if (hadithList[index].t == "q" && langBtn.text == "BN") uri =
                                uri.replace("bn", "en")
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                        }
                        if (alertDialog.window != null) {
                            alertDialog.window!!.setBackgroundDrawable(ColorDrawable(0))
                        }
                        try {
                            alertDialog.show()
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                } else {
                    //Log.d(TAG, "Error getting documents: ", task.getException());
                }
            }
    }// Failed to read value

    //Log.d("TAG", "Failed to read value.", error.toException());
// getPackageName() from Context or Activity object
    //                        try {
//                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
//                        } catch (android.content.ActivityNotFoundException ante) {
    //}
    ////Log.d(TAG, "Last Value is: " + bestScore);
    // This method is called once with the initial value and again
    // whenever data at this location is updated.
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
                        val view = LayoutInflater.from(this@StartActivity).inflate(
                            R.layout.dialog_layout_update, findViewById(R.id.updateLayoutDialog)
                        )
                        builder.setView(view)
                        val alertDialog = builder.create()
                        view.findViewById<View>(R.id.buttonUpdate)
                            .setOnClickListener {
                                if (!isMuted) {
                                    val mediaPlayer =
                                        MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                                    mediaPlayer.start()
                                    mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
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
                        if (alertDialog.window != null) {
                            alertDialog.window!!.setBackgroundDrawable(ColorDrawable(0))
                        }
                        try {
                            alertDialog.show()
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
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
            mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
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
            mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
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
                mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
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
            mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
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
        val view = LayoutInflater.from(this@StartActivity).inflate(
            R.layout.dialog_layout_info, findViewById(R.id.layoutInfo)
        )
        builder.setView(view)
        builder.setCancelable(false)
        (view.findViewById<View>(R.id.textMessage) as TextView).text = msg[0]
        (view.findViewById<View>(R.id.playGif) as GifImageView).setImageResource(gifs[0])
        view.findViewById<View>(R.id.buttonPre).visibility = View.INVISIBLE
        val alertDialog = builder.create()
        view.findViewById<View>(R.id.buttonPre).setOnClickListener {
            if (!isMuted) {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
            }
            if (i.get() != 0) i.getAndDecrement()
            if (i.get() == 0) view.findViewById<View>(R.id.buttonPre).visibility = View.INVISIBLE
            (view.findViewById<View>(R.id.textMessage) as TextView).text = msg[i.get()]
            (view.findViewById<View>(R.id.playGif) as GifImageView).setImageResource(gifs[i.get()])
        }
        view.findViewById<View>(R.id.buttonNext).setOnClickListener {
            if (!isMuted) {
                val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
            }
            i.getAndIncrement()
            if (!isFirstRun && i.get() == 4) i.getAndIncrement()
            if (i.get() == 1) view.findViewById<View>(R.id.buttonPre).visibility = View.VISIBLE
            if (i.get() == 5) alertDialog.dismiss() else {
                (view.findViewById<View>(R.id.textMessage) as TextView).text = msg[i.get()]
                (view.findViewById<View>(R.id.playGif) as GifImageView).setImageResource(
                    gifs[i.get()]
                )
            }
        }
        if (alertDialog.window != null) {
            alertDialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }
        try {
            alertDialog.show()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    fun startBtn(view: View) {
        if (!isMuted) {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
        }
        if (binding.mode1.alpha < .5) {
            if (onlineStatus == "pass") {
                startActivity(
                    Intent(this, MultiplayerActivity::class.java)
                        .putExtra("playerId", playerId)
                )
                finish()
            } else if (onlineStatus == "needReload") {
                //updateUI();
                val builder = AlertDialog.Builder(this@StartActivity)
                val v = LayoutInflater.from(this@StartActivity).inflate(
                    R.layout.dialog_layout_updateui, findViewById(R.id.updateLayoutDialogUI)
                )
                builder.setView(v)
                builder.setCancelable(false)
                v.findViewById<View>(R.id.googlePlayWarning).visibility = View.GONE
                (v.findViewById<View>(R.id.UpdateInfo) as TextView).text =
                    "You must have INTERNET connection to play in ONLINE mode"
                val alertDialog = builder.create()
                v.findViewById<View>(R.id.buttonUpdate).setOnClickListener {
                    if (!isMuted) {
                        val mediaPlayer = MediaPlayer.create(this@StartActivity, R.raw.btn_click_ef)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
                    }
                    recreate()
                    alertDialog.dismiss()
                }
                if (alertDialog.window != null) {
                    alertDialog.window!!.setBackgroundDrawable(ColorDrawable(0))
                }
                try {
                    alertDialog.show()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        } else if (binding.mode3.alpha < .5) //(mode3.getVisibility()==View.INVISIBLE)
        {
            startActivity(Intent(this, GameActivity1::class.java))
            finish()
        } else {
            startActivity(Intent(this, GameActivity3::class.java))
            finish()
        }
    }

    companion object {
        private const val TAG = "TAG: StartActivity"
        var errorCnt = 0
        var playerId: String? = null
        private var showHadith = true
    }
}