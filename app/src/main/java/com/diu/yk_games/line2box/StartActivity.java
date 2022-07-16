package com.diu.yk_games.line2box;
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
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class StartActivity extends AppCompatActivity
{
    RadioGroup vsRadioGrp;
    public static boolean scrBrdVisible =false, isFirstRun=true;
    public static SharedPreferences sharedPref;
    public static SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_start);
        findViewById(R.id.btnBack).setVisibility(View.GONE);
        findViewById(R.id.scrBrdNm).setVisibility(View.GONE);
        vsRadioGrp=findViewById(R.id.vsRadioGrp);
        isFirstRun= isFirstRun("line2Box",this);
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        if(isFirstRun)
        {
            editor.putBoolean("muted", false).apply();
        }
        ifMuted();


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
    public static boolean isFirstRun(String forWhat, Context context)
    {
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
            startActivity(new Intent(this,GameActivity1.class));
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
                    R.layout.alert_dialog_layout, findViewById(R.id.layoutDialog)
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
        findViewById(R.id.linearLayoutStart).setVisibility(View.GONE);
        findViewById(R.id.scrBrdNm).setVisibility(View.VISIBLE);
        Handler handler = new Handler();
        handler.postDelayed(() ->
                findViewById(R.id.btnBack).setVisibility(View.VISIBLE), 1500);

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
        findViewById(R.id.linearLayoutStart).setVisibility(View.VISIBLE);
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

    public void ideaBtn(View view)
    {
        if(!isMuted())
            MediaPlayer.create(this, R.raw.btn_click_ef).start();

    }
}