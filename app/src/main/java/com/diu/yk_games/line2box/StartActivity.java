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
import android.net.Uri;
import android.os.Bundle;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.concurrent.atomic.AtomicInteger;
import pl.droidsonroids.gif.GifImageView;

public class StartActivity extends AppCompatActivity
{
    RadioGroup vsRadioGrp;
    public static boolean scrBrdVisible =false, isFirstRun=true;
    public static SharedPreferences sharedPref;
    public SharedPreferences.Editor editor;
    String onlineVersionName=null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_start);
        findViewById(R.id.btnBack).setVisibility(View.GONE);
        findViewById(R.id.scrBrdNm).setVisibility(View.GONE);
        vsRadioGrp=findViewById(R.id.vsRadioGrp);
        isFirstRun= isFirstRun(this);
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        if(isFirstRun)
        {
            editor.putBoolean("muted", false).apply();
        }
        ifMuted();
        isUpdateAvailable();





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
//                        } catch (android.content.ActivityNotFoundException anfe) {
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
        return sharedPref.getBoolean("muted", false);
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
    public static boolean isFirstRun(Context context)
    {
        String forWhat= "line2Box";
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor preferencesEditor = preferences.edit();
        if (preferences.getBoolean(forWhat, true))
        {
            preferencesEditor.putBoolean(forWhat, false).apply();
            return true;
        } else {
            return false;
        }
    }

    public void startBtn(View view)
    {
        if(!isMuted())
            MediaPlayer.create(this, R.raw.btn_click_ef).start();
        if(vsRadioGrp.getCheckedRadioButtonId()==R.id.radioBtnHuman)
        {
            startActivity(new Intent(this,GameActivity1.class));
            finish();
        }
        else
            Toast.makeText(this, "AI Coming soon ;)", Toast.LENGTH_SHORT).show();

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
            editor.putBoolean("muted", true).apply();
        }
        else
        {
            MediaPlayer.create(this, R.raw.btn_click_ef).start();
            findViewById(R.id.volBtn).setBackgroundResource(R.drawable.btn_ylw_bg);
            ((ImageButton)findViewById(R.id.volBtn)).setImageResource(R.drawable.icon_vol_unmute);
            editor.putBoolean("muted", false).apply();
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
}