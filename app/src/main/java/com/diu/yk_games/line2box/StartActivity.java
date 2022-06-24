package com.diu.yk_games.line2box;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class StartActivity extends AppCompatActivity
{
    RadioGroup vsRadioGrp;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        vsRadioGrp=findViewById(R.id.vsRadioGrp);


    }

    public void startBtn(View view)
    {
        if(vsRadioGrp.getCheckedRadioButtonId()==R.id.radioBtnHuman)
            startActivity(new Intent(this,GameActivity1.class));
        else
            Toast.makeText(this, "AI Coming soon ;)", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBackPressed()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);
        View view = LayoutInflater.from(StartActivity.this).inflate(
                R.layout.alert_dialog_layout, findViewById(R.id.layoutDialog)
        );
        builder.setView(view);

        ((TextView) view.findViewById(R.id.textMessage)).setText("Do you really want to exit?");
        ((Button) view.findViewById(R.id.buttonYes)).setText("YES");
        ((Button) view.findViewById(R.id.buttonNo)).setText("NO");
        final AlertDialog alertDialog = builder.create();
        view.findViewById(R.id.buttonYes).setOnClickListener(view1 ->
        {
            alertDialog.dismiss();
            super.onBackPressed();
            android.os.Process.killProcess(android.os.Process.myPid());
        });
        view.findViewById(R.id.buttonNo).setOnClickListener(view2 -> alertDialog.dismiss());
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();
    }
}