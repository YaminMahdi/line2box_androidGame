package com.diu.yk_games.line2box;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.SignInAccount;
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
import java.util.concurrent.atomic.AtomicInteger;
import pl.droidsonroids.gif.GifImageView;

public class StartActivity extends AppCompatActivity
{
    private static final String TAG = "TAG: StartActivity";
    RadioGroup vsRadioGrp;
    public static boolean scrBrdVisible =false, isFirstRun=true;
    SharedPreferences preferences;
    SharedPreferences.Editor preferencesEditor;
    String onlineVersionName=null;
    private FirebaseAuth mAuth;
    public static String playerId;
// ...
// Initialize Firebase Auth



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        PlayGamesSdk.initialize(this);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        preferences = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        //SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ApplicationConstants.PREFERENCES, Context.MODE_PRIVATE);
        preferencesEditor = preferences.edit();
        GameProfile.setPreferences(preferences);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                .requestServerAuthCode(getString(R.string.default_web_client_id))
                .build();
        final FirebaseAuth auth = FirebaseAuth.getInstance();
        GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(this);
        gamesSignInClient.isAuthenticated().addOnCompleteListener(isAuthenticatedTask ->
        {
            gamesSignInClient.requestServerSideAccess(getString(R.string.default_web_client_id),
                            /*forceRefreshToken=*/ false)
                    .addOnCompleteListener( task -> {
                        if (task.isSuccessful()) {
                            String serverAuthToken = task.getResult();
                            Toast.makeText(this, "serverAuthToken- "+serverAuthToken, Toast.LENGTH_SHORT).show();
                            AuthCredential credential = PlayGamesAuthProvider.getCredential(serverAuthToken);
                            //AuthCredential credential = PlayGamesAuthProvider.getCredential(PlayGamesAuthProvider.PLAY_GAMES_SIGN_IN_METHOD);
                            auth.signInWithCredential(credential)
                                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if (task.isSuccessful()) {
                                                // Sign in success, update UI with the signed-in user's information
                                                Log.d(TAG, "signInWithCredential:success");
                                                FirebaseUser user = auth.getCurrentUser();
                                                updateUI(user);
                                            } else {
                                                // If sign in fails, display a message to the user.
                                                Log.w(TAG, "signInWithCredential:failure", task.getException());
                                                Toast.makeText(StartActivity.this, "Authentication failed.",
                                                        Toast.LENGTH_SHORT).show();
                                                updateUI(null);
                                            }

                                            // ...
                                        }
                                    });
                        } else {
                            // Failed to retrieve authentication code.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(StartActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    });
            boolean isAuthenticated = (isAuthenticatedTask.isSuccessful() &&
                            isAuthenticatedTask.getResult().isAuthenticated());
            if (isAuthenticated)
            {
                PlayGames.getPlayersClient(this).getCurrentPlayer().addOnCompleteListener(mTask ->
                        {
                            GameProfile gameProfile=new GameProfile();
                            playerId= mTask.getResult().getPlayerId();
                            Toast.makeText(this, "id: "+mTask.getResult().getPlayerId() , Toast.LENGTH_SHORT).show();
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
                                                        Log.d(TAG, "Profile exists!");
                                                        Toast.makeText(StartActivity.this, "Profile exists!", Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Log.d(TAG, "Profile does not exist!");
                                                        Toast.makeText(StartActivity.this, "Profile does not exist!", Toast.LENGTH_SHORT).show();

                                                        db.collection("gamerProfile").document(mTask.getResult().getPlayerId())
                                                                .set(gameProfile)
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void unused) {
                                                                        preferencesEditor.putBoolean("needProfile", false).apply();
                                                                        Log.i(TAG, "onSuccess: Profile Created");
                                                                        Toast.makeText(StartActivity.this, "onSuccess: Profile Created", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                }).addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        Log.i("TAG", "onSuccess: Profile Creation Failed");
                                                                        Toast.makeText(StartActivity.this, "onSuccess: Profile Creation Failed", Toast.LENGTH_SHORT).show();

                                                                    }
                                                                });
                                                    }
                                                } else {
                                                    Log.d(TAG, "Failed with: ", task.getException());
                                                }
                                            }
                                        });
                            }
                            else
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
                        }

                );

                // Continue with Play Games Services




            }
            else
            {
                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();

                // Disable your integration with Play Games Services or show a
                // login button to ask  players to sign-in. Clicking it should
                // call GamesSignInClient.signIn();
            }
        });

        //gamesSignInClient.signIn();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_start);
        findViewById(R.id.btnBack).setVisibility(View.GONE);
        findViewById(R.id.scrBrdNm).setVisibility(View.GONE);
        vsRadioGrp=findViewById(R.id.vsRadioGrp);
        isFirstRun= isFirstRun(this);
        if(isFirstRun)
        {
            preferencesEditor.putBoolean("muted", false).apply();
        }
        ifMuted();
        isUpdateAvailable();

    }
    public void updateUI(FirebaseUser currentUser)
    {
        if(currentUser==null)
        {
            findViewById(R.id.scrBrdBtn).setEnabled(false);
        }
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);
                    View view = LayoutInflater.from(StartActivity.this).inflate(
                            R.layout.dialog_layout_update, findViewById(R.id.updateLayoutDialog)
                    );
                    builder.setView(view);

                    final AlertDialog alertDialog = builder.create();
                    view.findViewById(R.id.buttonUpdate).setOnClickListener(view1 ->
                    {
                        if(!isMuted())
                            MediaPlayer.create(context, R.raw.btn_click_ef).start();
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
                    alertDialog.show();
                }
                //Log.d(TAG, "Last Value is: " + bestScore);

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read value
                Log.w("TAG", "Failed to read value.", error.toException());
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
    public static boolean isFirstRun(Activity acc)
    {
        SharedPreferences sharedPreferences = acc.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor preferencesEditor = sharedPreferences.edit();
        String forWhat="FirstRun";
        if (sharedPreferences.getBoolean(forWhat, true))
        {
            preferencesEditor.putBoolean(forWhat, false).apply();
            return true;
        } else
            return false;
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
            view.findViewById(R.id.starSpinner).setVisibility(View.GONE);
            view.findViewById(R.id.starTxt).setVisibility(View.GONE);
            final AlertDialog alertDialog = builder.create();
            view.findViewById(R.id.buttonYes).setOnClickListener(view1 ->
            {
                if(!isMuted())
                    MediaPlayer.create(this, R.raw.btn_click_ef).start();
                alertDialog.dismiss();
                super.onBackPressed();
                //android.os.Process.killProcess(android.os.Process.myPid());
            });
            view.findViewById(R.id.buttonNo).setOnClickListener(view2 ->
            {
                if(!isMuted())
                    MediaPlayer.create(this, R.raw.btn_click_ef).start();
                alertDialog.dismiss();
            });
            if (alertDialog.getWindow() != null) {
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            alertDialog.show();
        }
    }

    public void scoreBoard(View view)
    {

        if(!isMuted())
            MediaPlayer.create(this, R.raw.btn_click_ef).start();
        scrBrdVisible =true;
        FragmentManager fm=getSupportFragmentManager();
        FragmentTransaction ft=fm.beginTransaction();
        ft.replace(R.id.disFragment,new DisplayFragment());
        ft.commit();
        findViewById(R.id.linearLayoutStart1).setVisibility(View.GONE);
        findViewById(R.id.linearLayoutStart2).setVisibility(View.GONE);
        findViewById(R.id.scrBrdNm).setVisibility(View.VISIBLE);
        findViewById(R.id.btnBack).setVisibility(View.VISIBLE);

    }

    public void goBack(View view)
    {
        if(!isMuted())
            MediaPlayer.create(this, R.raw.btn_click_ef).start();
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
        findViewById(R.id.btnBack).setVisibility(View.GONE);
        findViewById(R.id.scrBrdNm).setVisibility(View.GONE);
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
            MediaPlayer.create(this, R.raw.btn_click_ef).start();
            findViewById(R.id.volBtn).setBackgroundResource(R.drawable.btn_ylw_bg);
            ((ImageButton)findViewById(R.id.volBtn)).setImageResource(R.drawable.icon_vol_unmute);
            preferencesEditor.putBoolean("muted", false).apply();
        }
    }

    public void ideaBtn(View view) {
        if(!isMuted())
            MediaPlayer.create(this, R.raw.btn_click_ef).start();
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
                MediaPlayer.create(this, R.raw.btn_click_ef).start();
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
                MediaPlayer.create(this, R.raw.btn_click_ef).start();
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
        alertDialog.show();
    }

    public void startBtn(View view)
    {
        if(!isMuted())
            MediaPlayer.create(this, R.raw.btn_click_ef).start();
        if(vsRadioGrp.getCheckedRadioButtonId()==R.id.radioBtnHuman)
        {
            startActivity(new Intent(this,GameActivity1.class));
        }
        else
        {
            Toast.makeText(this, "Multiplayer Beta", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MultiplayerActivity.class));
        }
        finish();

    }
}