package com.diu.yk_games.line2box;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PlayGamesAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import pl.droidsonroids.gif.GifImageView;

public class StartActivity extends AppCompatActivity
{
    private static final String TAG = "TAG: StartActivity";
    public static boolean scrBrdVisible =false, isFirstRun;
    static int errorCnt=0;
    SharedPreferences preferences;
    SharedPreferences.Editor preferencesEditor;
    String onlineVersionName=null;
    FirebaseAuth mAuth;
    Context context;
    public static String playerId;
    ImageView mode1, mode2, mode3;
    LoadingUI loadingUI;
    String onlineStatus;
    private static boolean showHadith = true;
// ...
// Initialize Firebase Auth


//    @Override
//    protected void onStart()
//    {
//        super.onStart();
//
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        PlayGamesSdk.initialize(this);
        context=this;
        setContentView(R.layout.activity_start);
        mode1=findViewById(R.id.mode1);
        mode2=findViewById(R.id.mode2);
        mode3=findViewById(R.id.mode3);
        loadingUI= new LoadingUI();
        loadingUI.start();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        preferences = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        //SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ApplicationConstants.PREFERENCES, Context.MODE_PRIVATE);
        preferencesEditor = preferences.edit();
        GameProfile.setPreferences(preferences);
        isFirstRun= preferences.getBoolean("firstRun", true);
        //if(isFirstRun)

//        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
//                .requestServerAuthCode(getString(R.string.default_web_client_id))
//                .build();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        findViewById(R.id.globalScoreFrag).setVisibility(View.GONE);

        if(showHadith&&!isFirstRun)
        {
            showAHadith();
            showHadith=false;
        }


        final FirebaseAuth auth = FirebaseAuth.getInstance();
        GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(this);
        gamesSignInClient.isAuthenticated().addOnCompleteListener(isAuthenticatedTask ->
        {
            boolean isAuthenticated = (isAuthenticatedTask.isSuccessful() &&
                    isAuthenticatedTask.getResult().isAuthenticated());
            gamesSignInClient.requestServerSideAccess(getString(R.string.default_web_client_id),
                            /*forceRefreshToken=*/ false)
                    .addOnCompleteListener( task -> {
                        if (task.isSuccessful())
                        {
                            isUpdateAvailable();

                            String serverAuthToken = task.getResult();
                            //Toast.makeText(this, "serverAuthToken- "+serverAuthToken, Toast.LENGTH_SHORT).show();
                            AuthCredential credential = PlayGamesAuthProvider.getCredential(serverAuthToken);
                            //AuthCredential credential = PlayGamesAuthProvider.getCredential(PlayGamesAuthProvider.PLAY_GAMES_SIGN_IN_METHOD);
                            auth.signInWithCredential(credential)
                                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if (task.isSuccessful())
                                            {
                                                // Sign in success, update UI with the signed-in user's information

                                                //Log.d(TAG, "signInWithCredential: success");
                                                if(showHadith&&isFirstRun)
                                                {
                                                    showAHadith();
                                                    showHadith=false;
                                                }
                                                FirebaseUser user = auth.getCurrentUser();
                                                if (isAuthenticated)
                                                {
                                                    PlayGames.getPlayersClient(StartActivity.this).getCurrentPlayer().addOnCompleteListener(mTask ->
                                                            {
                                                                GameProfile gameProfile=new GameProfile();
                                                                playerId= mTask.getResult().getPlayerId();
                                                                //Toast.makeText(StartActivity.this, "id: "+mTask.getResult().getPlayerId() , Toast.LENGTH_SHORT).show();
                                                                if(preferences.getBoolean("needProfile",true))
                                                                {
                                                                    db.collection("gamerProfile").document(playerId)
                                                                            .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                                    if (task.isSuccessful()) {
                                                                                        DocumentSnapshot document = task.getResult();
                                                                                        if (document.exists()) {
                                                                                            preferencesEditor.putBoolean("needProfile", false).apply();
                                                                                            preferencesEditor.putBoolean("firstRun", false).apply();
                                                                                            loadProfileFromServer(db);
                                                                                            if(loadingUI.visibility)
                                                                                            {
                                                                                                onlineStatus="pass";
                                                                                                loadingUI.stop();
                                                                                            }
                                                                                            //Log.d(TAG, "Profile exists!");
                                                                                            Toast.makeText(StartActivity.this, "Profile Exists and Loaded!", Toast.LENGTH_SHORT).show();
                                                                                        } else {
                                                                                            //Log.d(TAG, "Profile does not exist!");
                                                                                            //Toast.makeText(StartActivity.this, "Profile does not exist!", Toast.LENGTH_SHORT).show();
                                                                                            gameProfile.playerId=playerId;
                                                                                            db.collection("gamerProfile").document(mTask.getResult().getPlayerId())
                                                                                                    .set(gameProfile)
                                                                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                                        @Override
                                                                                                        public void onSuccess(Void unused) {
                                                                                                            preferencesEditor.putBoolean("needProfile", false).apply();
                                                                                                            gameProfile.apply();
                                                                                                            if(loadingUI.visibility)
                                                                                                            {
                                                                                                                onlineStatus="pass";
                                                                                                                loadingUI.stop();
                                                                                                            }
                                                                                                            //Log.d(TAG, "onSuccess: Profile Created");
                                                                                                            //Toast.makeText(StartActivity.this, "onSuccess: Profile Created", Toast.LENGTH_SHORT).show();
                                                                                                        }
                                                                                                    }).addOnFailureListener(new OnFailureListener() {
                                                                                                        @Override
                                                                                                        public void onFailure(@NonNull Exception e) {
                                                                                                            //Log.d("TAG", "onSuccess: Profile Creation Failed");
                                                                                                            //Toast.makeText(StartActivity.this, "onSuccess: Profile Creation Failed", Toast.LENGTH_SHORT).show();
                                                                                                            if(loadingUI.visibility)
                                                                                                            {
                                                                                                                onlineStatus="needReload";
                                                                                                                loadingUI.stop();
                                                                                                            }

                                                                                                        }
                                                                                                    });
                                                                                        }
                                                                                    } else {
                                                                                        //Log.d(TAG, "Failed with: ", task.getException());
                                                                                        if(loadingUI.visibility)
                                                                                        {
                                                                                            onlineStatus="needReload";
                                                                                            loadingUI.stop();
                                                                                        }
                                                                                    }
                                                                                }
                                                                            });
                                                                }
                                                                else
                                                                {
                                                                    loadProfileFromServer(db);
                                                                    if(loadingUI.visibility)
                                                                    {
                                                                        onlineStatus="pass";
                                                                        loadingUI.stop();
                                                                    }
                                                                }
                                                            }

                                                    );

                                                    // Continue with Play Games Services

                                                }
                                                else
                                                {
                                                    //Toast.makeText(StartActivity.this, "Failed", Toast.LENGTH_SHORT).show();

                                                    // Disable your integration with Play Games Services or show a
                                                    // login button to ask  players to sign-in. Clicking it should
                                                    // call GamesSignInClient.signIn();
                                                    updateUI(null);
                                                    if(loadingUI.visibility)
                                                    {
                                                        onlineStatus="needReload";
                                                        loadingUI.stop();
                                                    }
                                                }
                                                updateUI(user);
                                            } else {
                                                // If sign in fails, display a message to the user.
                                                //Log.d(TAG, "signInWithCredential: failure", task.getException());
                                                //Toast.makeText(StartActivity.this, "Authentication failed.",Toast.LENGTH_SHORT).show();
                                                updateUI(null);
                                                if(loadingUI.visibility)
                                                {
                                                    onlineStatus="needReload";
                                                    loadingUI.stop();
                                                }
                                            }

                                            // ...
                                        }
                                    });
                        } else {
                            // Failed to retrieve authentication code.
                            //Log.d(TAG, "signInWithCredential:failure", task.getException());
                            //Toast.makeText(StartActivity.this, "No Internet.",Toast.LENGTH_SHORT).show();
                            if (isAuthenticated)
                            {
                                PlayGames.getPlayersClient(StartActivity.this).getCurrentPlayer().addOnCompleteListener(mTask ->
                                {
                                    GameProfile gameProfile = new GameProfile();
                                    playerId = mTask.getResult().getPlayerId();
                                });
                            }
                            updateUI(null);
                            if(loadingUI.visibility)
                            {
                                onlineStatus="needReload";
                                loadingUI.stop();
                            }
                        }
                    });

        });


        //gamesSignInClient.signIn();
        //vsRadioGrp=findViewById(R.id.vsRadioGrp);
        ifMuted();

    }
    public void loadProfileFromServer(FirebaseFirestore db)
    {
        db.collection("gamerProfile").document(playerId)
                .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>(){
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        GameProfile server2device =documentSnapshot.toObject(GameProfile.class);
                        assert server2device != null;
                        server2device.apply();
                    }
                });
    }
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }
    public boolean internetIsConnected() {
        try {
            String command = "ping -c 1 google.com";
            return (Runtime.getRuntime().exec(command).waitFor() == 0);
        } catch (Exception e) {
            return false;
        }
    }
    class LoadingUI
    {
        public AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);
        public View view = LayoutInflater.from(StartActivity.this).inflate(
                R.layout.dialog_layout_loading, findViewById(R.id.updateLayoutLoadingUI));
        public AlertDialog alertDialog;
        public boolean visibility=false;

        public LoadingUI() {}

        public void start()
        {
            builder.setView(view);
            builder.setCancelable(false);
            alertDialog = builder.create();

            if (alertDialog.getWindow() != null)
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            try {alertDialog.show();visibility=true;}
                catch (Exception ex) {ex.printStackTrace();}
            Handler handler = new Handler();
            handler.postDelayed(() ->
            {
                if(visibility)
                {
                    onlineStatus="needReload";
                    stop();
                    updateUI(null);
                }
            }, 15000);


        }
        public void stop()
        {
            alertDialog.dismiss();
            visibility=false;
        }
    }
    @SuppressLint("SetTextI18n")
    public void updateUI(FirebaseUser currentUser)
    {
        if(currentUser==null)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);
            View view = LayoutInflater.from(StartActivity.this).inflate(
                    R.layout.dialog_layout_updateui, findViewById(R.id.updateLayoutDialogUI)
            );
            builder.setView(view);
            builder.setCancelable(false);
            final AlertDialog alertDialog = builder.create();
            view.findViewById(R.id.googlePlayWarning).setVisibility(View.GONE);
            errorCnt++;
            if(errorCnt>2)
            {
                view.findViewById(R.id.googlePlayWarning).setVisibility(View.VISIBLE);
                ((TextView)view.findViewById(R.id.warningMessage)).setText("Warning !");
                ((TextView)view.findViewById(R.id.UpdateInfo)).setText("You may need to UPDATE those two apps. (Link Below)");

                view.findViewById(R.id.playSvLink).setOnClickListener(v ->
                {
                    ((TextView)view.findViewById(R.id.playSvLink)).setTextColor(getColor(R.color.teal_700));
                    if (!isMuted())
                    {
                        MediaPlayer mediaPlayer = MediaPlayer.create(StartActivity.this, R.raw.btn_click_ef);
                        mediaPlayer.start();
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                    }
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.gms")));
                });
                view.findViewById(R.id.playGmLink).setOnClickListener(v ->
                {
                    ((TextView)view.findViewById(R.id.playGmLink)).setTextColor(getColor(R.color.teal_700));
                    if (!isMuted())
                    {
                        MediaPlayer mediaPlayer = MediaPlayer.create(StartActivity.this, R.raw.btn_click_ef);
                        mediaPlayer.start();
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                    }
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.play.games")));
                });
            }

            if(isFirstRun)
            {
                view.findViewById(R.id.buttonUpdate).setOnClickListener(view1 ->
                {
                    if (!isMuted())
                    {
                        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                        mediaPlayer.start();
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                    }
                    recreate();
                    alertDialog.dismiss();
                });
            }
            else
            {
                //findViewById(R.id.scrBrdBtn).setEnabled(false);
                if(errorCnt<3)
                    ((TextView)view.findViewById(R.id.UpdateInfo)).setText("Some functionalities are disabled.");
                ((Button)view.findViewById(R.id.buttonUpdate)).setText("Continue");
                view.findViewById(R.id.buttonUpdate).setOnClickListener(view1 ->
                {
                    if (!isMuted())
                    {
                        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                        mediaPlayer.start();
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                    }
                    alertDialog.dismiss();
                });
            }
            if (alertDialog.getWindow() != null) {
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            try {alertDialog.show();}
                catch (Exception ex) {ex.printStackTrace();}
        }
    }
    public void AddSomeBlankHadith(FirebaseFirestore db, int x)
    {
        for(int i=x;i<=x+10;i++)
        {
            db.collection("dailyHadith").document(i+"").set(new HadithStore());
        }
    }
    public void showAHadith()
    {
        ArrayList<HadithStore> hadithList = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("dailyHadith")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful())
                        {
                            for (QueryDocumentSnapshot document : task.getResult())
                            {
                                HadithStore h=document.toObject(HadithStore.class);
//                                if(h.b.equals(""))
//                                    db.collection("dailyHadith").document(document.getId()).delete();
//                                else
                                    hadithList.add(h);
                            }
                            if(hadithList.size()>0)
                            {
                                //AddSomeBlankHadith(db, hadithList.size());
                                int index= new Random().nextInt(hadithList.size());
                                AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);
                                View view = LayoutInflater.from(StartActivity.this).inflate(
                                        R.layout.dialog_layout_show_hadith, findViewById(R.id.hadithLayoutDialog)
                                );
                                builder.setView(view);
                                builder.setCancelable(false);
                                Button langBtn=view.findViewById(R.id.langBtn);
                                TextView narratorInfo= view.findViewById(R.id.narratorInfo);
                                TextView hadithTxt= view.findViewById(R.id.hadithTxt);
                                TextView headTxt= view.findViewById(R.id.warningMessage);
                                if(hadithList.get(index).t.equals("h"))
                                    headTxt.setText("Read a Hadith");
                                else if(hadithList.get(index).t.equals("q"))
                                    headTxt.setText("Read a Āyah");

                                if(preferences.getString("lang","bn").equals("bn"))
                                {
                                    narratorInfo.setText(hadithList.get(index).b);
                                    hadithTxt.setText(hadithList.get(index).bn);
                                    narratorInfo.setTypeface(getResources().getFont(R.font.paapri));
                                    hadithTxt.setTypeface(getResources().getFont(R.font.paapri));
                                    hadithTxt.setLineSpacing(0,1);
                                    langBtn.setText("EN");
                                }
                                else
                                {
                                    narratorInfo.setText(hadithList.get(index).e);
                                    hadithTxt.setText(hadithList.get(index).en);
                                    narratorInfo.setTypeface(getResources().getFont(R.font.comfortaa));
                                    hadithTxt.setTypeface(getResources().getFont(R.font.comfortaa));
                                    hadithTxt.setLineSpacing(7,1);
                                    langBtn.setText("BN");
                                }
                                ((TextView) view.findViewById(R.id.hadithInfo)).setText(hadithList.get(index).ref);

                                final AlertDialog alertDialog = builder.create();
                                langBtn.setOnClickListener(v ->
                                {
                                    if (!isMuted())
                                    {
                                        MediaPlayer mediaPlayer = MediaPlayer.create(StartActivity.this, R.raw.btn_click_ef);
                                        mediaPlayer.start();
                                        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                                    }
                                    if(langBtn.getText().equals("EN"))
                                    {
                                        narratorInfo.setText(hadithList.get(index).e);
                                        hadithTxt.setText(hadithList.get(index).en);
                                        narratorInfo.setTypeface(getResources().getFont(R.font.comfortaa));
                                        hadithTxt.setTypeface(getResources().getFont(R.font.comfortaa));
                                        hadithTxt.setLineSpacing(7,1);
                                        preferencesEditor.putString("lang","en").apply();
                                        langBtn.setText("BN");
                                    }
                                    else
                                    {
                                        narratorInfo.setText(hadithList.get(index).b);
                                        hadithTxt.setText(hadithList.get(index).bn);
                                        narratorInfo.setTypeface(getResources().getFont(R.font.paapri));
                                        hadithTxt.setTypeface(getResources().getFont(R.font.paapri));
                                        hadithTxt.setLineSpacing(0,1);
                                        preferencesEditor.putString("lang","bn").apply();
                                        langBtn.setText("EN");
                                    }
                                });
                                view.findViewById(R.id.buttonDone).setOnClickListener(v ->
                                {
                                    if (!isMuted())
                                    {
                                        MediaPlayer mediaPlayer = MediaPlayer.create(StartActivity.this, R.raw.btn_click_ef);
                                        mediaPlayer.start();
                                        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                                    }
                                    alertDialog.dismiss();
                                });
                                view.findViewById(R.id.srcLink).setOnClickListener(v ->
                                {
                                    ((TextView)view.findViewById(R.id.srcLink)).setTextColor(getColor(R.color.teal_700));
                                    if (!isMuted())
                                    {
                                        MediaPlayer mediaPlayer = MediaPlayer.create(StartActivity.this, R.raw.btn_click_ef);
                                        mediaPlayer.start();
                                        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                                    }
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(hadithList.get(index).src)));
                                });


                                if (alertDialog.getWindow() != null) {
                                    alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                                }
                                try {alertDialog.show();}
                                catch (Exception ex) {ex.printStackTrace();}
                            }
                        }
                        else {
                            //Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }
    public void isUpdateAvailable()
    {
        Context context=this;
        String localVersionName = BuildConfig.VERSION_NAME;
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("version");
        myRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                onlineVersionName = dataSnapshot.getValue(String.class);
                if(!localVersionName.equals(onlineVersionName))
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    View view = LayoutInflater.from(StartActivity.this).inflate(
                            R.layout.dialog_layout_update, findViewById(R.id.updateLayoutDialog)
                    );
                    builder.setView(view);

                    final AlertDialog alertDialog = builder.create();
                    view.findViewById(R.id.buttonUpdate).setOnClickListener(view1 ->
                    {
                        if (!isMuted())
                        {
                            MediaPlayer mediaPlayer = MediaPlayer.create(StartActivity.this, R.raw.btn_click_ef);
                            mediaPlayer.start();
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                        }
                        final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
//                        try {
//                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
//                        } catch (android.content.ActivityNotFoundException ante) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                        //}
                        alertDialog.dismiss();
                    });
                    if (alertDialog.getWindow() != null) {
                        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                    }
                    try {alertDialog.show();}
                catch (Exception ex) {ex.printStackTrace();}
                }
                ////Log.d(TAG, "Last Value is: " + bestScore);

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read value
                //Log.d("TAG", "Failed to read value.", error.toException());
            }
        });

    }
    public boolean isMuted()
    {
        return preferences.getBoolean("muted", false);
    }
    public void ifMuted()
    {
        if(isMuted())
        {
            findViewById(R.id.volBtn).setBackgroundResource(R.drawable.btn_gry_bg);
            ((ImageButton)findViewById(R.id.volBtn)).setImageResource(R.drawable.icon_vol_mute);
        }
        else
        {
            findViewById(R.id.volBtn).setBackgroundResource(R.drawable.btn_ylw_bg);
            ((ImageButton)findViewById(R.id.volBtn)).setImageResource(R.drawable.icon_vol_unmute);
        }

    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBackPressed()
    {
        if(scrBrdVisible)
        {
            onGoBack();
        }
        else
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);
            View view = LayoutInflater.from(StartActivity.this).inflate(
                    R.layout.dialog_layout_alert, findViewById(R.id.layoutDialog)
            );
            builder.setView(view);

            ((TextView) view.findViewById(R.id.textMessage)).setText("Do you really want to exit?");
            ((Button) view.findViewById(R.id.buttonYes)).setText("YES");
            ((Button) view.findViewById(R.id.buttonNo)).setText("NO");
            final AlertDialog alertDialog = builder.create();
            view.findViewById(R.id.buttonYes).setOnClickListener(view1 ->
            {
                if(!isMuted())
                {
                    MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                    mediaPlayer.start();
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                }
                alertDialog.dismiss();
                super.onBackPressed();
                //android.os.Process.killProcess(android.os.Process.myPid());
            });
            view.findViewById(R.id.buttonNo).setOnClickListener(view2 ->
            {
                if(!isMuted())
                {
                    MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                    mediaPlayer.start();
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                }
                alertDialog.dismiss();
            });
            if (alertDialog.getWindow() != null) {
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            try {alertDialog.show();}
                catch (Exception ex) {ex.printStackTrace();}
        }
    }

    public void scoreBoard(View view)
    {

        if(!isMuted())
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
        scrBrdVisible =true;
        FragmentManager fm=getSupportFragmentManager();
        FragmentTransaction ft=fm.beginTransaction();
        ft.replace(R.id.disFragment,new DisplayFragment());
        ft.commit();
        findViewById(R.id.linearLayoutStart1).setVisibility(View.GONE);
        findViewById(R.id.linearLayoutStart2).setVisibility(View.GONE);
        findViewById(R.id.motionLayout).setVisibility(View.GONE);
        findViewById(R.id.globalScoreFrag).setVisibility(View.VISIBLE);

    }

    public void goBack(View view)
    {
        if(!isMuted())
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
        onGoBack();
    }

    public void onGoBack()
    {
        scrBrdVisible =false;
        FragmentManager fm=getSupportFragmentManager();
        FragmentTransaction ft=fm.beginTransaction();
        ft.replace(R.id.disFragment,new BlankFragment());
        ft.commit();
        findViewById(R.id.linearLayoutStart1).setVisibility(View.VISIBLE);
        findViewById(R.id.linearLayoutStart2).setVisibility(View.VISIBLE);
        findViewById(R.id.motionLayout).setVisibility(View.VISIBLE);
        findViewById(R.id.globalScoreFrag).setVisibility(View.GONE);
    }

    public void volButton(View view)
    {
        if(!isMuted())
        {
            findViewById(R.id.volBtn).setBackgroundResource(R.drawable.btn_gry_bg);
            ((ImageButton)findViewById(R.id.volBtn)).setImageResource(R.drawable.icon_vol_mute);
            preferencesEditor.putBoolean("muted", true).apply();
        }
        else
        {
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
            findViewById(R.id.volBtn).setBackgroundResource(R.drawable.btn_ylw_bg);
            ((ImageButton)findViewById(R.id.volBtn)).setImageResource(R.drawable.icon_vol_unmute);
            preferencesEditor.putBoolean("muted", false).apply();
        }
    }

    public void ideaBtn(View view) {
        if(!isMuted())
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
        infoShow();

    }

    public void infoShow() {
        AtomicInteger i= new AtomicInteger();
        int[] gifs={R.drawable.g0,
                R.drawable.g1,
                R.drawable.g2,
                R.drawable.g3,
                R.drawable.g4};
        String[] msg={"If the color of RED is popped, it's the TURN of the first player.",
                "Click on a LINE to connect two DOT.",
                "The player who makes a BOX gets a point.",
                "Take a bonus TURN after making a BOX.",
                "Click on this button anytime to see the rules again."};

        AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);
        View view = LayoutInflater.from(StartActivity.this).inflate(
                R.layout.dialog_layout_info, findViewById(R.id.layoutInfo)
        );
        builder.setView(view);
        builder.setCancelable(false);

        ((TextView) view.findViewById(R.id.textMessage)).setText(msg[0]);
        ((GifImageView)view.findViewById(R.id.playGif)).setImageResource(gifs[0]);
        view.findViewById(R.id.buttonPre).setVisibility(View.INVISIBLE);
        final AlertDialog alertDialog = builder.create();
        view.findViewById(R.id.buttonPre).setOnClickListener(view1 ->
        {
            if(!isMuted())
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
            if(i.get()!=0)
                i.getAndDecrement();
            if(i.get()==0)
                view.findViewById(R.id.buttonPre).setVisibility(View.INVISIBLE);
            ((TextView) view.findViewById(R.id.textMessage)).setText(msg[i.get()]);
            ((GifImageView)view.findViewById(R.id.playGif)).setImageResource(gifs[i.get()]);

        });
        view.findViewById(R.id.buttonNext).setOnClickListener(view2 ->
        {
            if(!isMuted())
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
            i.getAndIncrement();
            if(!isFirstRun&&i.get()==4)
                i.getAndIncrement();
            if(i.get()==1)
                view.findViewById(R.id.buttonPre).setVisibility(View.VISIBLE);
            if(i.get()==5)
                alertDialog.dismiss();
            else
            {
                ((TextView) view.findViewById(R.id.textMessage)).setText(msg[i.get()]);
                ((GifImageView)view.findViewById(R.id.playGif)).setImageResource(gifs[i.get()]);
            }
        });
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        try {alertDialog.show();}
                catch (Exception ex) {ex.printStackTrace();}
    }

    public void startBtn(View view)
    {
        if(!isMuted())
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
        if(mode1.getAlpha()<.5)
        {
            if(onlineStatus.equals("pass"))
            {
                startActivity(new Intent(this, MultiplayerActivity.class).putExtra("playerId",playerId));
                finish();
            }
            else if(onlineStatus.equals("needReload"))
            {
                //updateUI();
                AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);
                View v = LayoutInflater.from(StartActivity.this).inflate(
                        R.layout.dialog_layout_updateui, findViewById(R.id.updateLayoutDialogUI)
                );
                builder.setView(v);
                builder.setCancelable(false);
                v.findViewById(R.id.googlePlayWarning).setVisibility(View.GONE);
                ((TextView)v.findViewById(R.id.UpdateInfo)).setText("You must have INTERNET connection to play in ONLINE mode");
                final AlertDialog alertDialog = builder.create();
                v.findViewById(R.id.buttonUpdate).setOnClickListener(view1 ->
                {
                    if (!isMuted())
                    {
                        MediaPlayer mediaPlayer = MediaPlayer.create(StartActivity.this, R.raw.btn_click_ef);
                        mediaPlayer.start();
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                    }
                    recreate();
                    alertDialog.dismiss();
                });
                if (alertDialog.getWindow() != null) {
                    alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));}
                try {alertDialog.show();}
                catch (Exception ex) {ex.printStackTrace();}
            }
        }
        else if (mode3.getAlpha()<.5)//(mode3.getVisibility()==View.INVISIBLE)
        {
            startActivity(new Intent(this,GameActivity1.class));
            finish();
        }
        else
        {
            startActivity(new Intent(this,GameActivity3.class));
            finish();
        }

    }
}