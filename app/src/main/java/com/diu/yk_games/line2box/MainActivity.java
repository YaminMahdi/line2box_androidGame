package com.diu.yk_games.line2box;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.annotation.SuppressLint;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.Objects;

public class MainActivity extends AppCompatActivity
{
    int clickCount=0;
    String idNm;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
// Hide the status bar.
        //WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        //getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().SYSTEM_UI_FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        //getActionBar().hide();

        setContentView(R.layout.activity_main);

    }

    //@SuppressLint("ResourceAsColor")
    public void lineClick(View view)
    {
        idNm = getResources().getResourceEntryName(view.getId());
        GradientDrawable bg = (GradientDrawable) view.getBackground();
        int color = getColorGrad(bg);

        Toast.makeText(this, "clicked "+getResources().getResourceEntryName(view.getId()), Toast.LENGTH_SHORT).show();
        int red= getResources().getColor(R.color.redX, getTheme());
        int blue= getResources().getColor(R.color.blueX, getTheme());
        if(color==getResources().getColor(R.color.whiteX, getTheme()))
        {
            clickCount++;
            if(clickCount%2==1)
            {

                bg.setColor(ContextCompat.getColor(getApplicationContext(),R.color.redX));
                if(idNm.charAt(4)=='T')
                {

                    if(Character.getNumericValue(idNm.charAt(1))>1)
                    {
                        int idTopU = this.getResources().getIdentifier(getIdNmH(idNm)[0], "id", this.getPackageName());
                        int idTopL = this.getResources().getIdentifier(getIdNmH(idNm)[1], "id", this.getPackageName());
                        int idTopR = this.getResources().getIdentifier(getIdNmH(idNm)[2], "id", this.getPackageName());
                        View lineU= findViewById(idTopU);
                        View lineL= findViewById(idTopL);
                        View lineR= findViewById(idTopR);
                        GradientDrawable bgTopU = (GradientDrawable) lineU.getBackground();
                        GradientDrawable bgTopL = (GradientDrawable) lineL.getBackground();
                        GradientDrawable bgTopR = (GradientDrawable) lineR.getBackground();
                        if((getColorGrad(bgTopU)==red||getColorGrad(bgTopU)==blue)&&(getColorGrad(bgTopL)==red||getColorGrad(bgTopL)==blue)&&(getColorGrad(bgTopR)==red||getColorGrad(bgTopR)==blue))
                        {
                            Log.d("midL", "lineClick: "+getIdNmH(idNm)[8]);
                            int txtId = this.getResources().getIdentifier(getIdNmH(idNm)[6], "id", this.getPackageName());
                            int idMidC1 = this.getResources().getIdentifier(getIdNmH(idNm)[8], "id", this.getPackageName());
                            int idMidC2 = this.getResources().getIdentifier(getIdNmH(idNm)[9], "id", this.getPackageName());
                            int idUpC1 = this.getResources().getIdentifier(getIdNmH(idNm)[10], "id", this.getPackageName());
                            int idUpC2 = this.getResources().getIdentifier(getIdNmH(idNm)[11], "id", this.getPackageName());
                            View crMid1= findViewById(idMidC1);
                            View crMid2= findViewById(idMidC2);
                            View crUp1= findViewById(idUpC1);
                            View crUp2= findViewById(idUpC2);
                            GradientDrawable bgMidC1 = (GradientDrawable) crMid1.getBackground();
                            GradientDrawable bgMidC2 = (GradientDrawable) crMid2.getBackground();
                            GradientDrawable bgUpC1 = (GradientDrawable) crUp1.getBackground();
                            GradientDrawable bgUpC2 = (GradientDrawable) crUp2.getBackground();
                            TextView txt= findViewById(txtId);
                            txt.setText("R");
                            txt.setTypeface(ResourcesCompat.getFont(getApplicationContext(), R.font.bertram));
                            //txt.setTextSize(5,5);
                            bgTopU.setColor(ContextCompat.getColor(getApplicationContext(),R.color.redX));
                            bgTopL.setColor(ContextCompat.getColor(getApplicationContext(),R.color.redX));
                            bgTopR.setColor(ContextCompat.getColor(getApplicationContext(),R.color.redX));

                            bgMidC1.setColor(ContextCompat.getColor(getApplicationContext(),R.color.redX));
                            bgMidC1.setStroke(14,ContextCompat.getColor(getApplicationContext(),R.color.redY));
                            bgMidC2.setColor(ContextCompat.getColor(getApplicationContext(),R.color.redX));
                            bgMidC2.setStroke(14,ContextCompat.getColor(getApplicationContext(),R.color.redY));

                            bgUpC1.setColor(ContextCompat.getColor(getApplicationContext(),R.color.redX));
                            bgUpC1.setStroke(14,ContextCompat.getColor(getApplicationContext(),R.color.redY));
                            bgUpC2.setColor(ContextCompat.getColor(getApplicationContext(),R.color.redX));
                            bgUpC2.setStroke(14,ContextCompat.getColor(getApplicationContext(),R.color.redY));

                        }
                    }

                    if(Character.getNumericValue(idNm.charAt(1))<7)
                    {
                        int idDownU = this.getResources().getIdentifier(getIdNmH(idNm)[3], "id", this.getPackageName());
                        int idDownL = this.getResources().getIdentifier(getIdNmH(idNm)[4], "id", this.getPackageName());
                        int idDownR = this.getResources().getIdentifier(getIdNmH(idNm)[5], "id", this.getPackageName());
                        View lineDownU= findViewById(idDownU);
                        View lineDownL= findViewById(idDownL);
                        View lineDownR= findViewById(idDownR);
                        GradientDrawable bgDownU = (GradientDrawable) lineDownU.getBackground();
                        GradientDrawable bgDownL = (GradientDrawable) lineDownL.getBackground();
                        GradientDrawable bgDownR = (GradientDrawable) lineDownR.getBackground();
                        if((getColorGrad(bgDownU)==red||getColorGrad(bgDownU)==blue)&&(getColorGrad(bgDownL)==red||getColorGrad(bgDownL)==blue)&&(getColorGrad(bgDownR)==red||getColorGrad(bgDownR)==blue))
                        {
                            int txtId = this.getResources().getIdentifier(getIdNmH(idNm)[7], "id", this.getPackageName());
                            int idMidC1 = this.getResources().getIdentifier(getIdNmH(idNm)[8], "id", this.getPackageName());
                            int idMidC2 = this.getResources().getIdentifier(getIdNmH(idNm)[9], "id", this.getPackageName());
                            int idDownC1 = this.getResources().getIdentifier(getIdNmH(idNm)[12], "id", this.getPackageName());
                            int idDownC2 = this.getResources().getIdentifier(getIdNmH(idNm)[13], "id", this.getPackageName());
                            View crMid1= findViewById(idMidC1);
                            View crMid2= findViewById(idMidC2);
                            View crDown1= findViewById(idDownC1);
                            View crDown2= findViewById(idDownC2);
                            //Toast.makeText(this, "clicked "+idC2 +" "+getIdNmH(idNm)[1], Toast.LENGTH_SHORT).show();
                            GradientDrawable bgMidC1 = (GradientDrawable) crMid1.getBackground();
                            GradientDrawable bgMidC2 = (GradientDrawable) crMid2.getBackground();
                            GradientDrawable bgDownC1 = (GradientDrawable) crDown1.getBackground();
                            GradientDrawable bgDownC2 = (GradientDrawable) crDown2.getBackground();
                            //Toast.makeText(this, ""+txtId, Toast.LENGTH_SHORT).show();
                            TextView txt= findViewById(txtId);
                            txt.setText("R");
                            txt.setTypeface(ResourcesCompat.getFont(getApplicationContext(), R.font.bertram));
                            bgDownU.setColor(ContextCompat.getColor(getApplicationContext(),R.color.redX));
                            bgDownL.setColor(ContextCompat.getColor(getApplicationContext(),R.color.redX));
                            bgDownR.setColor(ContextCompat.getColor(getApplicationContext(),R.color.redX));

                            bgMidC1.setColor(ContextCompat.getColor(getApplicationContext(),R.color.redX));
                            bgMidC1.setStroke(14,ContextCompat.getColor(getApplicationContext(),R.color.redY));
                            bgMidC2.setColor(ContextCompat.getColor(getApplicationContext(),R.color.redX));
                            bgMidC2.setStroke(14,ContextCompat.getColor(getApplicationContext(),R.color.redY));

                            bgDownC1.setColor(ContextCompat.getColor(getApplicationContext(),R.color.redX));
                            bgDownC1.setStroke(10,ContextCompat.getColor(getApplicationContext(),R.color.redY));
                            bgDownC2.setColor(ContextCompat.getColor(getApplicationContext(),R.color.redX));
                            bgDownC2.setStroke(10,ContextCompat.getColor(getApplicationContext(),R.color.redY));
                        }
                    }

                }
            }
            else
            {
                bg.setColor(ContextCompat.getColor(getApplicationContext(),R.color.blueX));
            }
        }


    }
    public static String[] getIdNmH(String idNm)
    {
        String[] idS=new String[14];
        StringBuilder id=new StringBuilder();
        if(Character.getNumericValue(idNm.charAt(1))>1)
        {
            //top up//
            id.append(idNm);
            id.deleteCharAt(1);
            id.insert(1,Character.getNumericValue(idNm.charAt(1))-1);
            idS[0]=id.toString();
            //txt up
            id.append('x');
            idS[6]=id.toString();
            //left up
            id.deleteCharAt(4);
            id.deleteCharAt(4);
            id.append('L');
            idS[1]=id.toString();
            //right up
            id.deleteCharAt(3);
            id.insert(3,Character.getNumericValue(idNm.charAt(3))+1);
            idS[2]=id.toString();
            //circle up right
            id.insert(3,'r');
            id.deleteCharAt(5);
            idS[11]=id.toString();
            //circle up left
            id.deleteCharAt(4);
            id.insert(4,Character.getNumericValue(idNm.charAt(3)));
            idS[10]=id.toString();

        }
        if(Character.getNumericValue(idNm.charAt(1))<7)
        {
            //down down//
            id.setLength(0);
            id.append(idNm);
            id.deleteCharAt(1);
            id.insert(1,Character.getNumericValue(idNm.charAt(1))+1);
            idS[3]=id.toString();
            //left down
            id.setLength(0);
            id.append(idNm);
            id.deleteCharAt(4);
            id.append('L');
            idS[4]=id.toString();
            //right down
            id.deleteCharAt(3);
            id.insert(3,Character.getNumericValue(idNm.charAt(3))+1);
            idS[5]=id.toString();
            //txt down
            id.setLength(0);
            id.append(idNm);
            id.append('x');
            idS[7]=id.toString();
            //circle Down left
            id.setLength(0);
            id.append(idNm);
            id.deleteCharAt(4);
            id.deleteCharAt(1);
            id.insert(1,Character.getNumericValue(idNm.charAt(1))+1);
            id.insert(3,'r');
            idS[13]=id.toString();
            //circle Down right
            id.deleteCharAt(4);
            id.insert(4,Character.getNumericValue(idNm.charAt(3))+1);
            idS[12]=id.toString();

        }
        //circle Middle left
        id.setLength(0);
        id.append(idNm);
        id.insert(3,'r');
        id.deleteCharAt(5);
        idS[8]=id.toString();
        //circle Middle right
        id.deleteCharAt(4);
        id.append(Character.getNumericValue(idNm.charAt(3))+1);
        idS[9]=id.toString();
        return idS;

    }

    public static String[] getIdNmV(String idNm)  //vertical
    {
        String[] idS=new String[14];
        StringBuilder id=new StringBuilder();
        if(Character.getNumericValue(idNm.charAt(1))>1)
        {
            //top up//
            id.append(idNm);
            id.deleteCharAt(3);
            id.insert(3,Character.getNumericValue(idNm.charAt(3))-1);
            idS[0]=id.toString();
            //right up
            id.deleteCharAt(4);
            id.append('T');
            idS[2]=id.toString();
            //txt up
            id.append('x');
            idS[6]=id.toString();
            //left up
            id.deleteCharAt(5);
            id.deleteCharAt(1);
            id.insert(1,Character.getNumericValue(idNm.charAt(1))+1);
            idS[1]=id.toString();
            //circle up right
            id.delete(4,6);
            id.insert(3,'r');
            idS[11]=id.toString();
            //circle up left
            id.deleteCharAt(1);
            id.insert(1,Character.getNumericValue(idNm.charAt(1))+1);
            idS[10]=id.toString();

        }
        if(Character.getNumericValue(idNm.charAt(1))<7)
        {
            //down down//
            id.setLength(0);
            id.append(idNm);
            id.deleteCharAt(3);
            id.insert(3,Character.getNumericValue(idNm.charAt(3))+1);
            idS[3]=id.toString();
            //right down
            id.setLength(0);
            id.append(idNm);
            id.deleteCharAt(4);
            id.insert(3,'T');
            idS[5]=id.toString();
            //txt down
            id.append('x');
            idS[7]=id.toString();
            //left down
            id.deleteCharAt(5);
            id.deleteCharAt(1);
            id.insert(3,Character.getNumericValue(idNm.charAt(1))+1);
            idS[4]=id.toString();
            //circle Down right
            id.setLength(0);
            id.append(idNm);
            id.deleteCharAt(4);
            id.deleteCharAt(3);
            id.insert(1,Character.getNumericValue(idNm.charAt(3))+1);
            id.insert(3,'r');
            idS[13]=id.toString();
            //circle Down left
            id.deleteCharAt(1);
            id.insert(1,Character.getNumericValue(idNm.charAt(1))+1);
            idS[12]=id.toString();

        }
        //circle Middle right
        id.setLength(0);
        id.append(idNm);
        id.insert(3,'r');
        id.deleteCharAt(5);
        idS[9]=id.toString();
        //circle Middle left
        id.deleteCharAt(1);
        id.append(Character.getNumericValue(idNm.charAt(1))+1);
        idS[8]=id.toString();
        return idS;

    }

    public static int getColorGrad(GradientDrawable bg)
    {
        int color=0;
        Class<? extends GradientDrawable> aClass = bg.getClass();
        try {
            @SuppressLint("DiscouragedPrivateApi") Field mFillPaint = aClass.getDeclaredField("mFillPaint");
            mFillPaint.setAccessible(true);
            Paint strokePaint= (Paint) mFillPaint.get(bg);
            color = Objects.requireNonNull(strokePaint).getColor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return color;
    }



}