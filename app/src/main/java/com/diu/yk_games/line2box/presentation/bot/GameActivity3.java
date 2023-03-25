package com.diu.yk_games.line2box.presentation.bot;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import com.diu.yk_games.line2box.R;
import com.diu.yk_games.line2box.presentation.main.StartActivity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import pl.droidsonroids.gif.GifImageView;

public class GameActivity3 extends AppCompatActivity {
    public static int clickCount = 0, scoreRed = 0, scoreBlue = 0, bestScore = 9999;
    public static String idNm, fst = "r1c1", top, left, circle, nm1="AI", nm2="Blue";
    ArrayList<String> lineIDs = new ArrayList<>(
            Arrays.asList("r1c1T", "r1c1L", "r1c2T", "r1c2L", "r1c3T", "r1c3L", "r1c4T", "r1c4L", "r1c5T", "r1c5L", "r1c6T", "r1c6L", "r1c7L", "r2c1T", "r2c1L", "r2c2T", "r2c2L", "r2c3T", "r2c3L", "r2c4T", "r2c4L", "r2c5T", "r2c5L", "r2c6T", "r2c6L", "r2c7L", "r3c1T", "r3c1L", "r3c2T", "r3c2L", "r3c3T", "r3c3L", "r3c4T", "r3c4L", "r3c5T", "r3c5L", "r3c6T", "r3c6L", "r3c7L", "r4c1T", "r4c1L", "r4c2T", "r4c2L", "r4c3T", "r4c3L", "r4c4T", "r4c4L", "r4c5T", "r4c5L", "r4c6T", "r4c6L", "r4c7L", "r5c1T", "r5c1L", "r5c2T", "r5c2L", "r5c3T", "r5c3L", "r5c4T", "r5c4L", "r5c5T", "r5c5L", "r5c6T", "r5c6L", "r5c7L", "r7c1T", "r6c1T", "r6c1L", "r7c6T", "r6c2T", "r6c2L", "r7c2T", "r6c3T", "r6c3L", "r7c3T", "r6c4T", "r6c4L", "r7c4T", "r6c5T", "r6c5L", "r7c5T", "r6c6T", "r6c6L", "r6c7L"));
    Random random = new Random();

    TextView scoreRedView, scoreBlueView,redTxt, blueTxt;
    public static boolean one = true, flag = true;
    //MediaPlayer lineClick, boxPlus, winSoundEf, btnClick;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
    boolean isFirstRun, recursion =false, clickEnabled=false;
    int tmpLineId;


    public boolean isMuted() {
        return sharedPref.getBoolean("muted", false);
    }

    public void ifMuted() {
        if (isMuted()) {
            findViewById(R.id.volBtn).setBackgroundResource(R.drawable.btn_gry_bg);
            ((ImageButton) findViewById(R.id.volBtn)).setImageResource(R.drawable.icon_vol_mute);
        } else {
            findViewById(R.id.volBtn).setBackgroundResource(R.drawable.btn_ylw_bg);
            ((ImageButton) findViewById(R.id.volBtn)).setImageResource(R.drawable.icon_vol_unmute);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_game3);

        scoreRedView = findViewById(R.id.scoreRed);
        scoreBlueView = findViewById(R.id.scoreBlue);
        redTxt = findViewById(R.id.red);
        blueTxt = findViewById(R.id.blue);
        clickCount = 0;
        scoreRed = 0;
        scoreBlue = 0;
        bestScore = 9999;
        sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        ifMuted();
        isFirstRun = sharedPref.getBoolean("firstRun", true);

        StringBuilder index = new StringBuilder();
        for (int i = 1; i <= 6; i++) {
            index.setLength(0);
            index.append(fst);
            index.append('T');
            index.deleteCharAt(1);
            index.insert(1, i);
            top = index.toString();
            index.deleteCharAt(4);
            index.append('L');
            left = index.toString();
            index.deleteCharAt(4);
            index.insert(3, 'r');
            circle = index.toString();
            for (int j = 1; j <= 6; j++) {
                int idTop = this.getResources().getIdentifier(top, "id", this.getPackageName());
                int idLeft = this.getResources().getIdentifier(left, "id", this.getPackageName());
                int idCircle = this.getResources().getIdentifier(circle, "id", this.getPackageName());
                View lineTop = findViewById(idTop);
                View lineLeft = findViewById(idLeft);
                View lineCircle = findViewById(idCircle);

                GradientDrawable bgTop = (GradientDrawable) lineTop.getBackground();
                GradientDrawable bgLeft = (GradientDrawable) lineLeft.getBackground();
                GradientDrawable bgCircle = (GradientDrawable) lineCircle.getBackground();
                bgTop.setColor(ContextCompat.getColor(getApplicationContext(), R.color.whiteX));
                bgLeft.setColor(ContextCompat.getColor(getApplicationContext(), R.color.whiteX));
                bgCircle.setColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                bgCircle.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.whiteY));
                if (i == 6) {
                    index.setLength(0);
                    index.append(top);
                    index.deleteCharAt(1);
                    index.insert(1, i + 1);
                    idTop = this.getResources().getIdentifier(index.toString(), "id", this.getPackageName());
                    index.setLength(0);
                    index.append(circle);
                    index.deleteCharAt(1);
                    index.insert(1, i + 1);
                    idCircle = this.getResources().getIdentifier(index.toString(), "id", this.getPackageName());
                    lineTop = findViewById(idTop);
                    lineCircle = findViewById(idCircle);
                    bgTop = (GradientDrawable) lineTop.getBackground();
                    bgCircle = (GradientDrawable) lineCircle.getBackground();
                    bgTop.setColor(ContextCompat.getColor(getApplicationContext(), R.color.whiteX));
                    bgCircle.setColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                    bgCircle.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.whiteY));
                    ////Log.d("TAG", "onDestroy: " + top + " " + circle);
                }
                index.setLength(0);
                index.append(top);
                index.deleteCharAt(3);
                index.insert(3, j + 1);
                top = index.toString();
                index.setLength(0);
                index.append(left);
                index.deleteCharAt(3);
                index.insert(3, j + 1);
                left = index.toString();
                index.setLength(0);
                index.append(circle);
                index.deleteCharAt(4);
                index.insert(4, j + 1);
                circle = index.toString();
                if (j == 6) {
                    index.setLength(0);
                    index.append(left);
                    index.deleteCharAt(3);
                    index.insert(3, j + 1);
                    left = index.toString();
                    idLeft = this.getResources().getIdentifier(left, "id", this.getPackageName());
                    index.setLength(0);
                    index.append(circle);
                    index.deleteCharAt(4);
                    index.insert(4, j + 1);
                    circle = index.toString();
                    idCircle = this.getResources().getIdentifier(circle, "id", this.getPackageName());
                    lineLeft = findViewById(idLeft);
                    lineCircle = findViewById(idCircle);
                    bgLeft = (GradientDrawable) lineLeft.getBackground();
                    bgCircle = (GradientDrawable) lineCircle.getBackground();
                    bgLeft.setColor(ContextCompat.getColor(getApplicationContext(), R.color.whiteX));
                    bgCircle.setColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                    bgCircle.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.whiteY));
                    if (i == 6) {
                        index.setLength(0);
                        index.append(circle);
                        index.deleteCharAt(1);
                        index.insert(1, i + 1);
                        circle = index.toString();
                        idCircle = this.getResources().getIdentifier(circle, "id", this.getPackageName());
                        lineCircle = findViewById(idCircle);
                        bgCircle = (GradientDrawable) lineCircle.getBackground();
                        bgCircle.setColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                        bgCircle.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.whiteY));
                    }
                }

            }
        }

        int indx = random.nextInt(84);
        int randLineId=this.getResources().getIdentifier(lineIDs.get(indx), "id", this.getPackageName());
        new Handler().postDelayed(() ->
        {
            if(isFirstRun)
                infoShow();
        }, 200);
        new Handler().postDelayed(() ->
        {
            clickEnabled=true;
            lineClick(findViewById(randLineId));
        }, 600);

    }

    @SuppressLint("SetTextI18n")
    public void lineClick(View view) {
        idNm = getResources().getResourceEntryName(view.getId());
        GradientDrawable bg = (GradientDrawable) view.getBackground();
        int color = getColorGrad(bg);
        boolean change = false;
        int red = getResources().getColor(R.color.redX, getTheme());
        int blue = getResources().getColor(R.color.blueX, getTheme());
        int white = getResources().getColor(R.color.whiteX, getTheme());
        if (color == white && clickEnabled && lineIDs.size()>0) {
            if (!isMuted())
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.line_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
            lineIDs.remove(idNm);
            clickCount++;
            if (clickCount % 2 == 1) {
                bg.setColor(ContextCompat.getColor(getApplicationContext(), R.color.redX));
            } else {
                bg.setColor(ContextCompat.getColor(getApplicationContext(), R.color.blueX));
            }
            if ((Character.getNumericValue(idNm.charAt(1)) > 1 && idNm.charAt(4) == 'T') || (Character.getNumericValue(idNm.charAt(3)) > 1 && idNm.charAt(4) == 'L')) {
                int idTopU = this.getResources().getIdentifier(getIdNm(idNm)[0], "id", this.getPackageName());
                int idTopL = this.getResources().getIdentifier(getIdNm(idNm)[1], "id", this.getPackageName());
                int idTopR = this.getResources().getIdentifier(getIdNm(idNm)[2], "id", this.getPackageName());
                View lineU = findViewById(idTopU);
                View lineL = findViewById(idTopL);
                View lineR = findViewById(idTopR);
                GradientDrawable bgTopU = (GradientDrawable) lineU.getBackground();
                GradientDrawable bgTopL = (GradientDrawable) lineL.getBackground();
                GradientDrawable bgTopR = (GradientDrawable) lineR.getBackground();
                if ((getColorGrad(bgTopU) == red || getColorGrad(bgTopU) == blue) && (getColorGrad(bgTopL) == red || getColorGrad(bgTopL) == blue) && (getColorGrad(bgTopR) == red || getColorGrad(bgTopR) == blue)) {
                    int txtId = this.getResources().getIdentifier(getIdNm(idNm)[6], "id", this.getPackageName());
                    int idMidC1 = this.getResources().getIdentifier(getIdNm(idNm)[8], "id", this.getPackageName());
                    int idMidC2 = this.getResources().getIdentifier(getIdNm(idNm)[9], "id", this.getPackageName());
                    int idUpC1 = this.getResources().getIdentifier(getIdNm(idNm)[10], "id", this.getPackageName());
                    int idUpC2 = this.getResources().getIdentifier(getIdNm(idNm)[11], "id", this.getPackageName());
                    View crMid1 = findViewById(idMidC1);
                    View crMid2 = findViewById(idMidC2);
                    View crUp1 = findViewById(idUpC1);
                    View crUp2 = findViewById(idUpC2);
                    GradientDrawable bgMidC1 = (GradientDrawable) crMid1.getBackground();
                    GradientDrawable bgMidC2 = (GradientDrawable) crMid2.getBackground();
                    GradientDrawable bgUpC1 = (GradientDrawable) crUp1.getBackground();
                    GradientDrawable bgUpC2 = (GradientDrawable) crUp2.getBackground();
                    TextView txt = findViewById(txtId);
                    if (clickCount % 2 == 1) {
                        if (!isMuted())
                        {
                            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.box_ef);
                            mediaPlayer.start();
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                        }
                        scoreRed++;
                        scoreRedView.setText("" + scoreRed);
                        txt.setText("" + nm1.charAt(0));
                        txt.setTypeface(ResourcesCompat.getFont(getApplicationContext(), R.font.bertram));

                        bgTopU.setColor(ContextCompat.getColor(getApplicationContext(), R.color.redX));
                        bgTopL.setColor(ContextCompat.getColor(getApplicationContext(), R.color.redX));
                        bgTopR.setColor(ContextCompat.getColor(getApplicationContext(), R.color.redX));

                        bgMidC1.setColor(ContextCompat.getColor(getApplicationContext(), R.color.redX));
                        bgMidC1.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.redY));
                        bgMidC2.setColor(ContextCompat.getColor(getApplicationContext(), R.color.redX));
                        bgMidC2.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.redY));

                        bgUpC1.setColor(ContextCompat.getColor(getApplicationContext(), R.color.redX));
                        bgUpC1.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.redY));
                        bgUpC2.setColor(ContextCompat.getColor(getApplicationContext(), R.color.redX));
                        bgUpC2.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.redY));
                    } else {
                        if (!isMuted())
                        {
                            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.box_ef);
                            mediaPlayer.start();
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                        }
                        scoreBlue++;
                        scoreBlueView.setText("" + scoreBlue);
                        txt.setText("" + nm2.charAt(0));
                        txt.setTypeface(ResourcesCompat.getFont(getApplicationContext(), R.font.bertram));
                        bgTopU.setColor(ContextCompat.getColor(getApplicationContext(), R.color.blueX));
                        bgTopL.setColor(ContextCompat.getColor(getApplicationContext(), R.color.blueX));
                        bgTopR.setColor(ContextCompat.getColor(getApplicationContext(), R.color.blueX));

                        bgMidC1.setColor(ContextCompat.getColor(getApplicationContext(), R.color.blueX));
                        bgMidC1.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.blueY));
                        bgMidC2.setColor(ContextCompat.getColor(getApplicationContext(), R.color.blueX));
                        bgMidC2.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.blueY));

                        bgUpC1.setColor(ContextCompat.getColor(getApplicationContext(), R.color.blueX));
                        bgUpC1.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.blueY));
                        bgUpC2.setColor(ContextCompat.getColor(getApplicationContext(), R.color.blueX));
                        bgUpC2.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.blueY));
                        if (one) {
                            one = false;
                            Toast.makeText(this, "Bonus TURN for you", Toast.LENGTH_SHORT).show();
                        }
                    }
                    change = true;

                }
            }
            if ((Character.getNumericValue(idNm.charAt(1)) < 7 && idNm.charAt(4) == 'T') || (Character.getNumericValue(idNm.charAt(3)) < 7 && idNm.charAt(4) == 'L')) {
                int idDownU = this.getResources().getIdentifier(getIdNm(idNm)[3], "id", this.getPackageName());
                int idDownL = this.getResources().getIdentifier(getIdNm(idNm)[4], "id", this.getPackageName());
                int idDownR = this.getResources().getIdentifier(getIdNm(idNm)[5], "id", this.getPackageName());
                View lineDownU = findViewById(idDownU);
                View lineDownL = findViewById(idDownL);
                View lineDownR = findViewById(idDownR);
                GradientDrawable bgDownU = (GradientDrawable) lineDownU.getBackground();
                GradientDrawable bgDownL = (GradientDrawable) lineDownL.getBackground();
                GradientDrawable bgDownR = (GradientDrawable) lineDownR.getBackground();
                if ((getColorGrad(bgDownU) == red || getColorGrad(bgDownU) == blue) && (getColorGrad(bgDownL) == red || getColorGrad(bgDownL) == blue) && (getColorGrad(bgDownR) == red || getColorGrad(bgDownR) == blue)) {
                    int txtId = this.getResources().getIdentifier(getIdNm(idNm)[7], "id", this.getPackageName());
                    int idMidC1 = this.getResources().getIdentifier(getIdNm(idNm)[8], "id", this.getPackageName());
                    int idMidC2 = this.getResources().getIdentifier(getIdNm(idNm)[9], "id", this.getPackageName());
                    int idDownC1 = this.getResources().getIdentifier(getIdNm(idNm)[12], "id", this.getPackageName());
                    int idDownC2 = this.getResources().getIdentifier(getIdNm(idNm)[13], "id", this.getPackageName());
                    View crMid1 = findViewById(idMidC1);
                    View crMid2 = findViewById(idMidC2);
                    View crDown1 = findViewById(idDownC1);
                    View crDown2 = findViewById(idDownC2);

                    GradientDrawable bgMidC1 = (GradientDrawable) crMid1.getBackground();
                    GradientDrawable bgMidC2 = (GradientDrawable) crMid2.getBackground();
                    GradientDrawable bgDownC1 = (GradientDrawable) crDown1.getBackground();
                    GradientDrawable bgDownC2 = (GradientDrawable) crDown2.getBackground();
                    TextView txt = findViewById(txtId);
                    if (clickCount % 2 == 1) {
                        if (!isMuted())
                        {
                            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.box_ef);
                            mediaPlayer.start();
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                        }
                        scoreRed++;
                        scoreRedView.setText("" + scoreRed);
                        txt.setText("" + nm1.charAt(0));
                        txt.setTypeface(ResourcesCompat.getFont(getApplicationContext(), R.font.bertram));
                        bgDownU.setColor(ContextCompat.getColor(getApplicationContext(), R.color.redX));
                        bgDownL.setColor(ContextCompat.getColor(getApplicationContext(), R.color.redX));
                        bgDownR.setColor(ContextCompat.getColor(getApplicationContext(), R.color.redX));

                        bgMidC1.setColor(ContextCompat.getColor(getApplicationContext(), R.color.redX));
                        bgMidC1.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.redY));
                        bgMidC2.setColor(ContextCompat.getColor(getApplicationContext(), R.color.redX));
                        bgMidC2.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.redY));

                        bgDownC1.setColor(ContextCompat.getColor(getApplicationContext(), R.color.redX));
                        bgDownC1.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.redY));
                        bgDownC2.setColor(ContextCompat.getColor(getApplicationContext(), R.color.redX));
                        bgDownC2.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.redY));

                    } else {
                        if (!isMuted())
                        {
                            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.box_ef);
                            mediaPlayer.start();
                            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                        }
                        scoreBlue++;
                        scoreBlueView.setText("" + scoreBlue);
                        txt.setText("" + nm2.charAt(0));
                        txt.setTypeface(ResourcesCompat.getFont(getApplicationContext(), R.font.bertram));
                        bgDownU.setColor(ContextCompat.getColor(getApplicationContext(), R.color.blueX));
                        bgDownL.setColor(ContextCompat.getColor(getApplicationContext(), R.color.blueX));
                        bgDownR.setColor(ContextCompat.getColor(getApplicationContext(), R.color.blueX));

                        bgMidC1.setColor(ContextCompat.getColor(getApplicationContext(), R.color.blueX));
                        bgMidC1.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.blueY));
                        bgMidC2.setColor(ContextCompat.getColor(getApplicationContext(), R.color.blueX));
                        bgMidC2.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.blueY));

                        bgDownC1.setColor(ContextCompat.getColor(getApplicationContext(), R.color.blueX));
                        bgDownC1.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.blueY));
                        bgDownC2.setColor(ContextCompat.getColor(getApplicationContext(), R.color.blueX));
                        bgDownC2.setStroke(14, ContextCompat.getColor(getApplicationContext(), R.color.blueY));
                        if (one) {
                            one = false;
                            Toast.makeText(this, "Bonus TURN for you", Toast.LENGTH_SHORT).show();
                        }
                    }
                    change = true;
                }
            }
            if (change)
            {
                clickCount--;
                recursion=false;
            }
            else {
                if (clickCount % 2 == 1) {
                    clickEnabled=true;
                    redTxt.setTextSize(30);
                    redTxt.setTextColor(getResources().getColor(R.color.whiteT, getTheme()));
                    blueTxt.setTextSize(35);
                    blueTxt.setTextColor(getResources().getColor(R.color.white, getTheme()));
                } else {
                    blueTxt.setTextSize(30);
                    blueTxt.setTextColor(getResources().getColor(R.color.whiteT, getTheme()));
                    redTxt.setTextSize(35);
                    redTxt.setTextColor(getResources().getColor(R.color.white, getTheme()));

                }
            }
            if(clickCount % 2 == 1)
            {
                clickEnabled=true;
            }
            else if(lineIDs.size()>0)
            {
                clickEnabled=false;

                int countColored=0, blankIndex=-69;
                boolean extraTurn=false;
                if ((Character.getNumericValue(idNm.charAt(1)) > 1 && idNm.charAt(4) == 'T') || (Character.getNumericValue(idNm.charAt(3)) > 1 && idNm.charAt(4) == 'L'))
                {
                    //Log.d("TAG", "lineClick: AI in top half");
                    int idTopU = this.getResources().getIdentifier(getIdNm(idNm)[0], "id", this.getPackageName());
                    int idTopL = this.getResources().getIdentifier(getIdNm(idNm)[1], "id", this.getPackageName());
                    int idTopR = this.getResources().getIdentifier(getIdNm(idNm)[2], "id", this.getPackageName());
                    View lineU = findViewById(idTopU);
                    View lineL = findViewById(idTopL);
                    View lineR = findViewById(idTopR);
                    GradientDrawable bgTopU = (GradientDrawable) lineU.getBackground();
                    GradientDrawable bgTopL = (GradientDrawable) lineL.getBackground();
                    GradientDrawable bgTopR = (GradientDrawable) lineR.getBackground();
                    if (getColorGrad(bgTopU) != white )
                        countColored++;
                    else
                        blankIndex=0;
                    if(getColorGrad(bgTopL) != white)
                        countColored++;
                    else
                        blankIndex=1;
                    if(getColorGrad(bgTopR) != white)
                        countColored++;
                    else
                        blankIndex=2;
                    if(countColored==2)
                    {
                        //Log.d("TAG", "lineClick: AI in top half countColored");
                        int LineId=this.getResources().getIdentifier(getIdNm(idNm)[blankIndex], "id", this.getPackageName());
                        Handler handler = new Handler();
                        handler.postDelayed(() ->
                        {
                            clickEnabled=true;
                            recursion=true;
                            lineClick(findViewById(LineId));
                            tmpLineId=LineId;
                            recursion=false;
                        }, 500);
                        extraTurn=true;
                    }
                }

                countColored=0; blankIndex=-69;
                if ((Character.getNumericValue(idNm.charAt(1)) < 7 && idNm.charAt(4) == 'T') || (Character.getNumericValue(idNm.charAt(3)) < 7 && idNm.charAt(4) == 'L')) {
                    //Log.d("TAG", "lineClick: AI in dwn half");
                    int idDownU = this.getResources().getIdentifier(getIdNm(idNm)[3], "id", this.getPackageName());
                    int idDownL = this.getResources().getIdentifier(getIdNm(idNm)[4], "id", this.getPackageName());
                    int idDownR = this.getResources().getIdentifier(getIdNm(idNm)[5], "id", this.getPackageName());
                    View lineDownU = findViewById(idDownU);
                    View lineDownL = findViewById(idDownL);
                    View lineDownR = findViewById(idDownR);
                    GradientDrawable bgDownU = (GradientDrawable) lineDownU.getBackground();
                    GradientDrawable bgDownL = (GradientDrawable) lineDownL.getBackground();
                    GradientDrawable bgDownR = (GradientDrawable) lineDownR.getBackground();
                    if (getColorGrad(bgDownU) != white)
                        countColored++;
                    else
                        blankIndex = 3;
                    if (getColorGrad(bgDownL) != white)
                        countColored++;
                    else
                        blankIndex = 4;
                    if (getColorGrad(bgDownR) != white)
                        countColored++;
                    else
                        blankIndex = 5;
                    if (countColored==2) {
                        int LineId=this.getResources().getIdentifier(getIdNm(idNm)[blankIndex], "id", this.getPackageName());
                        //Log.d("TAG", "lineClick: AI in dwn half countColored");
                        Handler handler = new Handler();
                        handler.postDelayed(() ->
                        {
                            if(tmpLineId!=LineId)
                            {
                                clickEnabled=true;
                                recursion=true;
                                lineClick(findViewById(LineId));
                            }

                        }, 650);
                        recursion=false;
                        extraTurn=true;
                    }
                }
                //Log.d("TAG", "lineClick: lineIDs.size(): "+lineIDs.size());
                if(!extraTurn && !recursion)
                {
                    //Log.d("TAG", "lineClick: AI in random");
                    //recursion=false;
                    int indx, countColoredUp=0, countColoredDn=0;
                    String randLineIdNm;
                    ArrayList<String> lineIDtemp=new ArrayList<>(lineIDs);

                    indx= random.nextInt(lineIDs.size());
                    randLineIdNm=lineIDs.get(indx);
                    while(true)
                    {
                        //Log.d("TAG", "lineClick: AI in random loop");
                        if ((Character.getNumericValue(randLineIdNm.charAt(1)) > 1 && randLineIdNm.charAt(4) == 'T') || (Character.getNumericValue(randLineIdNm.charAt(3)) > 1 && randLineIdNm.charAt(4) == 'L'))
                        {
                            int idTopU = this.getResources().getIdentifier(getIdNm(randLineIdNm)[0], "id", this.getPackageName());
                            int idTopL = this.getResources().getIdentifier(getIdNm(randLineIdNm)[1], "id", this.getPackageName());
                            int idTopR = this.getResources().getIdentifier(getIdNm(randLineIdNm)[2], "id", this.getPackageName());
                            View lineU = findViewById(idTopU);
                            View lineL = findViewById(idTopL);
                            View lineR = findViewById(idTopR);
                            GradientDrawable bgTopU = (GradientDrawable) lineU.getBackground();
                            GradientDrawable bgTopL = (GradientDrawable) lineL.getBackground();
                            GradientDrawable bgTopR = (GradientDrawable) lineR.getBackground();
                            if (getColorGrad(bgTopU) != white )
                                countColoredUp++;
                            if(getColorGrad(bgTopL) != white)
                                countColoredUp++;
                            if(getColorGrad(bgTopR) != white)
                                countColoredUp++;
                        }
                        if ((Character.getNumericValue(randLineIdNm.charAt(1)) < 7 && randLineIdNm.charAt(4) == 'T') || (Character.getNumericValue(randLineIdNm.charAt(3)) < 7 && randLineIdNm.charAt(4) == 'L')) {
                            int idDownU = this.getResources().getIdentifier(getIdNm(randLineIdNm)[3], "id", this.getPackageName());
                            int idDownL = this.getResources().getIdentifier(getIdNm(randLineIdNm)[4], "id", this.getPackageName());
                            int idDownR = this.getResources().getIdentifier(getIdNm(randLineIdNm)[5], "id", this.getPackageName());
                            View lineDownU = findViewById(idDownU);
                            View lineDownL = findViewById(idDownL);
                            View lineDownR = findViewById(idDownR);
                            GradientDrawable bgDownU = (GradientDrawable) lineDownU.getBackground();
                            GradientDrawable bgDownL = (GradientDrawable) lineDownL.getBackground();
                            GradientDrawable bgDownR = (GradientDrawable) lineDownR.getBackground();
                            if (getColorGrad(bgDownU) != white)
                                countColoredDn++;
                            if (getColorGrad(bgDownL) != white)
                                countColoredDn++;
                            if (getColorGrad(bgDownR) != white)
                                countColoredDn++;
                        }
                        if((countColoredUp>1||countColoredDn>1)&&!(countColoredUp==3||countColoredDn==3))
                        {
                            //Log.d("TAG", "lineClick: AI in random countColored u d. clk cnt: "+clickCount);
                            if(lineIDtemp.size()==1)
                            {
                                randLineIdNm=lineIDtemp.get(0);
                                break;
                            }
                            lineIDtemp.remove(indx);
                            indx= random.nextInt(lineIDtemp.size());
                            randLineIdNm=lineIDtemp.get(indx);
                            countColoredUp=0; countColoredDn=0;
                        }
                        else
                            break;
                    }

                    //Log.d("TAG", "AIlineClick: "+randLineIdNm+" Up- "+countColoredUp+" Dn- "+countColoredDn);
                    int lineId= this.getResources().getIdentifier(randLineIdNm, "id", this.getPackageName());
                    Handler handler = new Handler();
                    handler.postDelayed(() ->
                    {
                        clickEnabled=true;
                        lineClick(findViewById(lineId));
                    }, 800);
                }


            }
            Handler handler = new Handler();
            handler.postDelayed(() ->
            {
                if (scoreRed + scoreBlue == 36)
                {
                    if (!isMuted())
                    {
                        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.win_ef);
                        mediaPlayer.start();
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                    }

                    redTxt.setTextSize(30);
                    redTxt.setTextColor(getResources().getColor(R.color.white, getTheme()));
                    blueTxt.setTextSize(30);
                    blueTxt.setTextColor(getResources().getColor(R.color.white, getTheme()));
                    if (scoreRed > scoreBlue)
                        onGameOver("AI won the match.");
                    else if (scoreRed < scoreBlue)
                        onGameOver("You won the match.");
                    else
                        onGameOver("Match Draw.");
                }
            }, 950);

        }
    }


    public static String[] getIdNm(String idNm) {
        String[] idS = new String[14];
        StringBuilder id = new StringBuilder();
        if (idNm.charAt(4) == 'T') {
            if (Character.getNumericValue(idNm.charAt(1)) > 1) {
                //top up//
                id.append(idNm);
                id.deleteCharAt(1);
                id.insert(1, Character.getNumericValue(idNm.charAt(1)) - 1);
                idS[0] = id.toString();
                //txt up
                id.append('x');
                idS[6] = id.toString();
                //left up
                id.deleteCharAt(4);
                id.deleteCharAt(4);
                id.append('L');
                idS[1] = id.toString();
                //right up
                id.deleteCharAt(3);
                id.insert(3, Character.getNumericValue(idNm.charAt(3)) + 1);
                idS[2] = id.toString();
                //circle up right
                id.insert(3, 'r');
                id.deleteCharAt(5);
                idS[11] = id.toString();
                //circle up left
                id.deleteCharAt(4);
                id.insert(4, Character.getNumericValue(idNm.charAt(3)));
                idS[10] = id.toString();

            }
            if (Character.getNumericValue(idNm.charAt(1)) < 7) {
                //down down//
                id.setLength(0);
                id.append(idNm);
                id.deleteCharAt(1);
                id.insert(1, Character.getNumericValue(idNm.charAt(1)) + 1);
                idS[3] = id.toString();
                //left down
                id.setLength(0);
                id.append(idNm);
                id.deleteCharAt(4);
                id.append('L');
                idS[4] = id.toString();
                //right down
                id.deleteCharAt(3);
                id.insert(3, Character.getNumericValue(idNm.charAt(3)) + 1);
                idS[5] = id.toString();
                //txt down
                id.setLength(0);
                id.append(idNm);
                id.append('x');
                idS[7] = id.toString();
                //circle Down left
                id.setLength(0);
                id.append(idNm);
                id.deleteCharAt(4);
                id.deleteCharAt(1);
                id.insert(1, Character.getNumericValue(idNm.charAt(1)) + 1);
                id.insert(3, 'r');
                idS[13] = id.toString();
                //circle Down right
                id.deleteCharAt(4);
                id.insert(4, Character.getNumericValue(idNm.charAt(3)) + 1);
                idS[12] = id.toString();

            }
            //circle Middle left
            id.setLength(0);
            id.append(idNm);
            id.insert(3, 'r');
            id.deleteCharAt(5);
            idS[8] = id.toString();
            //circle Middle right
            id.deleteCharAt(4);
            id.append(Character.getNumericValue(idNm.charAt(3)) + 1);
            idS[9] = id.toString();

        } else if (idNm.charAt(4) == 'L') {
            if (Character.getNumericValue(idNm.charAt(3)) > 1) {
                //top up//
                id.append(idNm);
                id.deleteCharAt(3);
                id.insert(3, Character.getNumericValue(idNm.charAt(3)) - 1);
                idS[0] = id.toString();
                //right up
                id.deleteCharAt(4);
                id.append('T');
                idS[2] = id.toString();
                //txt up
                id.append('x');
                idS[6] = id.toString();
                //left up
                id.deleteCharAt(5);
                id.deleteCharAt(1);
                id.insert(1, Character.getNumericValue(idNm.charAt(1)) + 1);
                idS[1] = id.toString();
                //circle up left
                id.delete(4, 6);
                id.insert(3, 'r');
                idS[10] = id.toString();
                //circle up right
                id.deleteCharAt(1);
                id.insert(1, Character.getNumericValue(idNm.charAt(1)));
                idS[11] = id.toString();

            }
            if (Character.getNumericValue(idNm.charAt(3)) < 7) {
                //down down//
                id.setLength(0);
                id.append(idNm);
                id.deleteCharAt(3);
                id.insert(3, Character.getNumericValue(idNm.charAt(3)) + 1);
                idS[3] = id.toString();
                //right down
                id.setLength(0);
                id.append(idNm);
                id.deleteCharAt(4);
                id.insert(4, 'T');
                idS[5] = id.toString();
                //txt down
                id.append('x');
                idS[7] = id.toString();
                //left down
                id.deleteCharAt(1);
                id.insert(1, Character.getNumericValue(idNm.charAt(1)) + 1);
                id.deleteCharAt(5);
                idS[4] = id.toString();
                //circle Down right
                id.setLength(0);
                id.append(idNm);
                id.deleteCharAt(4);
                id.deleteCharAt(3);
                id.insert(3, Character.getNumericValue(idNm.charAt(3)) + 1);
                id.insert(3, 'r');
                idS[13] = id.toString();
                //circle Down left
                id.deleteCharAt(1);
                id.insert(1, Character.getNumericValue(idNm.charAt(1)) + 1);
                idS[12] = id.toString();


            }
            //circle Middle right
            id.setLength(0);
            id.append(idNm);
            id.insert(3, 'r');
            id.deleteCharAt(5);
            idS[9] = id.toString();
            //circle Middle left
            id.deleteCharAt(1);
            id.insert(1, Character.getNumericValue(idNm.charAt(1)) + 1);
            idS[8] = id.toString();

        }
        return idS;
    }

    public static int getColorGrad(GradientDrawable bg) {
        int color = 0;
        Class<? extends GradientDrawable> aClass = bg.getClass();
        try {
            @SuppressLint("DiscouragedPrivateApi") Field mFillPaint = aClass.getDeclaredField("mFillPaint");
            mFillPaint.setAccessible(true);
            Paint strokePaint = (Paint) mFillPaint.get(bg);
            color = Objects.requireNonNull(strokePaint).getColor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return color;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity3.this);
        View view = LayoutInflater.from(GameActivity3.this).inflate(
                R.layout.dialog_layout_alert, findViewById(R.id.layoutDialog)
        );
        builder.setView(view);

        ((TextView) view.findViewById(R.id.textMessage)).setText("Do you really want to QUIT the match?");
        ((Button) view.findViewById(R.id.buttonYes)).setText("YES");
        ((Button) view.findViewById(R.id.buttonNo)).setText("NO");
        final AlertDialog alertDialog = builder.create();
        view.findViewById(R.id.buttonYes).setOnClickListener(view1 ->
        {
            if (!isMuted())
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
            alertDialog.dismiss();
            scoreRed = 0;
            scoreBlue = 0;
            clickCount = 0;
            flag = true;
            super.onBackPressed();
            startActivity(new Intent(this, StartActivity.class));
            finish();
            //android.os.Process.killProcess(android.os.Process.myPid());
        });
        view.findViewById(R.id.buttonNo).setOnClickListener(view2 ->
        {
            if (!isMuted())
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
                catch (NullPointerException npe) {npe.printStackTrace();}
    }

    @SuppressLint("SetTextI18n")
    public void onGameOver(String winMsg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity3.this);
        View view = LayoutInflater.from(GameActivity3.this).inflate(
                R.layout.dialog_layout_alert, findViewById(R.id.layoutDialog)
        );
        builder.setView(view);
        //builder.setCancelable(false);

        ((TextView) view.findViewById(R.id.textMessage)).setText("" + winMsg);
        ((Button) view.findViewById(R.id.buttonNo)).setText("Exit");
        ((Button) view.findViewById(R.id.buttonYes)).setText("Retry!");

        final AlertDialog alertDialog = builder.create();
        view.findViewById(R.id.buttonYes).setOnClickListener(view1 ->
        {
            if (!isMuted())
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
            alertDialog.dismiss();
            startActivity(new Intent(GameActivity3.this, GameActivity3.class));
            finish();


            //recreate();

        });
        view.findViewById(R.id.buttonNo).setOnClickListener(view2 ->
        {
            if (!isMuted())
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
            alertDialog.dismiss();
            startActivity(new Intent(this, StartActivity.class));
            finish();
            flag = true;
            //android.os.Process.killProcess(android.os.Process.myPid());
        });
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        try {alertDialog.show();}
                catch (NullPointerException npe) {npe.printStackTrace();}
    }


    public void volButton(View view) {
        if (!isMuted()) {
            findViewById(R.id.volBtn).setBackgroundResource(R.drawable.btn_gry_bg);
            ((ImageButton) findViewById(R.id.volBtn)).setImageResource(R.drawable.icon_vol_mute);
            editor.putBoolean("muted", true).apply();
        } else {
            if (!isMuted())
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
            findViewById(R.id.volBtn).setBackgroundResource(R.drawable.btn_ylw_bg);
            ((ImageButton) findViewById(R.id.volBtn)).setImageResource(R.drawable.icon_vol_unmute);
            editor.putBoolean("muted", false).apply();
        }
    }

    public void ideaBtn(View view) {
        if (!isMuted())
        {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
        }
        infoShow();

    }

    public void infoShow() {
        if(isFirstRun)
            editor.putBoolean("firstRun", false).apply();
        AtomicInteger i = new AtomicInteger();
        int[] gifs = {R.drawable.g0,
                R.drawable.g1,
                R.drawable.g2,
                R.drawable.g3,
                R.drawable.g4};
        String[] msg = {"If the color of RED is popped, it's the TURN of the first player.",
                "Click on a LINE to connect two DOT.",
                "The player who makes a BOX gets a point.",
                "Take a bonus TURN after making a BOX.",
                "Click on this button anytime to see the rules again."};

        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity3.this);
        View view = LayoutInflater.from(GameActivity3.this).inflate(
                R.layout.dialog_layout_info, findViewById(R.id.layoutInfo)
        );
        builder.setView(view);
        builder.setCancelable(false);

        ((TextView) view.findViewById(R.id.textMessage)).setText(msg[0]);
        ((GifImageView) view.findViewById(R.id.playGif)).setImageResource(gifs[0]);
        view.findViewById(R.id.buttonPre).setVisibility(View.INVISIBLE);
        final AlertDialog alertDialog = builder.create();
        view.findViewById(R.id.buttonPre).setOnClickListener(view1 ->
        {
            if (!isMuted())
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
            if (i.get() != 0)
                i.getAndDecrement();
            if (i.get() == 0)
                view.findViewById(R.id.buttonPre).setVisibility(View.INVISIBLE);
            ((TextView) view.findViewById(R.id.textMessage)).setText(msg[i.get()]);
            ((GifImageView) view.findViewById(R.id.playGif)).setImageResource(gifs[i.get()]);

        });
        view.findViewById(R.id.buttonNext).setOnClickListener(view2 ->
        {
            if (!isMuted())
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
            i.getAndIncrement();
            if (!isFirstRun && i.get() == 4)
                i.getAndIncrement();
            if (i.get() == 1)
                view.findViewById(R.id.buttonPre).setVisibility(View.VISIBLE);
            if (i.get() == 5)
                alertDialog.dismiss();
            else {
                ((TextView) view.findViewById(R.id.textMessage)).setText(msg[i.get()]);
                ((GifImageView) view.findViewById(R.id.playGif)).setImageResource(gifs[i.get()]);
            }
        });
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        try {alertDialog.show();}
                catch (NullPointerException npe) {npe.printStackTrace();}
    }

    public void backBtn(View view) {
        if (!isMuted())
        {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
        }
        onBackPressed();
    }
}
