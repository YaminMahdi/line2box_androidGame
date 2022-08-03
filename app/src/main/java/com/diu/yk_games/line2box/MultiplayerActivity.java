package com.diu.yk_games.line2box;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.diu.yk_games.line2box.databinding.ActivityGameMultiBinding;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Locale;

import io.ghyeok.stickyswitch.widget.StickySwitch;

public class MultiplayerActivity extends AppCompatActivity {

    private ActivityGameMultiBinding binding;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
    public static boolean scrBrdVisible =false;
    static String key = null;
    DrawerLayout mDrawerLayout;
    EditText joinId;
    ImageButton copyPastBtn;
    Button startMatchBtn;
    FirebaseDatabase database;
    DatabaseReference myRef;
    ArrayList<String> dsList;
    ClipboardManager clipboard;
    ClipData clipData;
    ClipData.Item item;
    String nm1,nm2;
    Integer lvl1,lvl2;
    Bundle mBundle = new Bundle();
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivityGameMultiBinding.inflate(getLayoutInflater());
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(binding.getRoot());

        sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        GameProfile.setPreferences(sharedPref);
        findViewById(R.id.btnBack).setVisibility(View.GONE);
        findViewById(R.id.scrBrdNm).setVisibility(View.GONE);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        joinId= findViewById(R.id.joinInputId);
        copyPastBtn=findViewById(R.id.copyPastBtn);
        startMatchBtn=findViewById(R.id.startMatchBtnId);
        copyPastBtn.setImageResource(R.drawable.icon_paste);
        copyPastBtn.setTag(R.drawable.icon_paste);
        startMatchBtn.setEnabled(false);
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ifMuted();
        dsList = new ArrayList<>();
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("MultiPlayer");
        myRef.addChildEventListener(new ChildEventListener()
        {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s)
            {
                Log.d("addList", "onChildAdded: "+dataSnapshot.getKey());
                dsList.add(dataSnapshot.getKey());
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

                Log.w("TAG", "Failed to read value.", databaseError.toException());

            }

        });
        joinId.addTextChangedListener(new TextWatcher()
        {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
            @Override
            public void afterTextChanged(Editable s)
            {
                Log.d("getkey", "afterTextChanged: "+getKey()+" "+joinId.getText().toString().length());
                if(joinId.getText().toString().length()==4)
                {
                    closeKeyboard();
                    if(getKey()!=null)
                    {
                        mBundle.putString("gameKey", getKey());
                        myRef.child(getKey()).child("playerCount").addValueEventListener(new ValueEventListener() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                        {
                            int playerCount = Integer.parseInt(requireNonNull(dataSnapshot.getValue(String.class)));
                            if(playerCount==1)
                            {
                                myRef.child(getKey()).child("playerCount").setValue("2");
                                startMatchBtn.setEnabled(true);
                                myRef.child(getKey()).child("playerInfo").child("nm2").setValue(new GameProfile().nm);
                                myRef.child(getKey()).child("playerInfo").child("lvl2").setValue(new GameProfile().lvl);
                                myRef.child(getKey()).child("playerInfo").addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (getKey() != null)
                                        {
                                            nm1 = dataSnapshot.child("nm1").getValue(String.class);
                                            lvl1 = dataSnapshot.child("lvl1").getValue(Integer.class);
                                            mBundle.putString("nm1", nm1);
                                            mBundle.putInt("lvl1", lvl1);
                                        }
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });

                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Failed to read value
                            Log.w("TAG", "Failed to read value.", error.toException());
                        }
                    });
                    }
                }
            }
        });
        nm2=new GameProfile().nm;
        lvl2=new GameProfile().lvl;
        Log.d("TAG left", "ver: "+nm1+" "+lvl1);
        mBundle.putString("nm2", nm2);
        mBundle.putInt("lvl2", lvl2);
        mBundle.putBoolean("plyr1", false);
        StickySwitch stickySwitch = findViewById(R.id.sticky_switch);
        stickySwitch.setOnSelectedChangeListener((direction, text) ->
        {
            if(stickySwitch.getDirection()==StickySwitch.Direction.LEFT)
            {
                joinId.setEnabled(true);
                startMatchBtn.setEnabled(false);
                joinId.setHint("");
                joinId.setText("");
                mBundle.putBoolean("plyr1", false);
                nm2=new GameProfile().nm;
                lvl2=new GameProfile().lvl;
                Log.d("TAG left", "ver: "+nm1+" "+lvl1);
                mBundle.putString("nm2", nm2);
                mBundle.putInt("lvl2", lvl2);
                final Handler handler = new Handler();
                handler.postDelayed(()->
                {
                    joinId.setHint("Input ID");
                    copyPastBtn.setImageResource(R.drawable.icon_paste);
                    copyPastBtn.setTag(R.drawable.icon_paste);
                    stickySwitch.setSwitchColor(0xFF2371FA);
                    if(key!=null)
                    {
                        myRef.child(key).removeValue();
                        key=null;
                    }
                }, 400);
            }
            else
            {
                joinId.setEnabled(false);
                startMatchBtn.setEnabled(false);
                joinId.setHint("");
                joinId.setText("");
                mBundle.putBoolean("plyr1", true);
                nm1=new GameProfile().nm;
                lvl1=new GameProfile().lvl;
                Log.d("TAG", "ver: "+nm1+" "+lvl1);
                mBundle.putString("nm1", nm1);
                mBundle.putInt("lvl1", lvl1);
                key = myRef.push().getKey();
                Log.d("TAG", "onCreate key: "+key);
                mBundle.putString("gameKey", key);
                assert key != null;
                myRef.child(key).child("playerCount").setValue("1");
                myRef.child(key).child("playerInfo").child("nm1").setValue(new GameProfile().nm);
                myRef.child(key).child("playerInfo").child("lvl1").setValue(new GameProfile().lvl);
                myRef.child(key).child("playerInfo").child("nm2").setValue("");
                myRef.child(key).child("playerInfo").child("lvl2").setValue(0);
                myRef.child(key).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(key!=null) {
                            Log.d("TAG -int key", "onDataChange: "+key+" "+getKey()+" "+dataSnapshot.child("playerCount").getValue(String.class));
                            int playerCount = Integer.parseInt(requireNonNull(dataSnapshot.child("playerCount").getValue(String.class)));
                            nm2=dataSnapshot.child("playerInfo").child("nm2").getValue(String.class);
                            lvl2=dataSnapshot.child("playerInfo").child("lvl2").getValue(Integer.class);
                            Log.d("TAG", "ver2: "+nm2+" "+lvl2);
                            mBundle.putString("nm2", nm2);
                            mBundle.putInt("lvl2", lvl2);
                            if (playerCount == 2)
                                startMatchBtn.setEnabled(true);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Failed to read value
                        Log.w("TAG", "Failed to read value.", error.toException());
                    }
                });

                final Handler handler = new Handler();
                handler.postDelayed(()->
                {
                    joinId.setHint(getKey4(key));
                    copyPastBtn.setImageResource(R.drawable.icon_copy);
                    copyPastBtn.setTag(R.drawable.icon_copy);
                    stickySwitch.setSwitchColor(ContextCompat.getColor(getApplicationContext(), R.color.greenY));

                }, 400);
            }
        });

        copyPastBtn.setOnClickListener(v ->
        {
            if(!isMuted())
                MediaPlayer.create(this, R.raw.btn_click_ef).start();
            Integer resource = (Integer) copyPastBtn.getTag();

            if(resource==R.drawable.icon_copy)
            {
                Toast.makeText(this, "ID copied", Toast.LENGTH_SHORT).show();
                ClipData clip = ClipData.newPlainText("key4", getKey4(key));
                clipboard.setPrimaryClip(clip);

            }
            else
            {
                Toast.makeText(this, "ID pasted", Toast.LENGTH_SHORT).show();
                // Access your context here using YourActivityName.this
                joinId.setText(getClipboardText());

            }
        });

    }
    public String getKey4(String key)
    {
        StringBuilder key4= new StringBuilder();
        for(int i=4,j=7;i<=j;i++)
        {
            if(key.charAt(i)=='0'||key.charAt(i)=='O'||key.charAt(i)=='o')
                key4.append('M');
            else
            {
                if(key.charAt(i)!='-'&&key.charAt(i)!='_')
                    key4.append(key.charAt(i));
                else
                    j++;
            }
        }
        return key4.toString().toUpperCase(Locale.ROOT);
    }
    public String getKey()
    {
        String t=joinId.getText().toString().toUpperCase(Locale.ROOT);
        for(int i=0;i<dsList.size();i++)
        {
            if(getKey4(dsList.get(i)).equals(t))
                return dsList.get(i);
        }
        return null;
    }
    public String getClipboardText()
    {
        clipData = clipboard.getPrimaryClip();
        item = clipData.getItemAt(0);
        return item.getText().toString();
    }

    private void closeKeyboard()
    {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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
            AlertDialog.Builder builder = new AlertDialog.Builder(MultiplayerActivity.this);
            View view = LayoutInflater.from(MultiplayerActivity.this).inflate(
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
                startActivity(new Intent(this,StartActivity.class));
                finish();
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
    public void ideaBtn(View view)
    {
        if(!isMuted())
            MediaPlayer.create(this, R.raw.btn_click_ef).start();
    }

    public void leaderBoard(View view)
    {
        if(!isMuted())
            MediaPlayer.create(this, R.raw.btn_click_ef).start();
    }

    public void profileBtn(View view)
    {
        if(!isMuted())
            MediaPlayer.create(this, R.raw.btn_click_ef).start();
    }

    public void closeNavBtn(View view)
    {
        if(!isMuted())
            MediaPlayer.create(this, R.raw.btn_click_ef).start();
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }
    public void openNavBtn(View view)
    {
        if(!isMuted())
            MediaPlayer.create(this, R.raw.btn_click_ef).start();
        mDrawerLayout.openDrawer(GravityCompat.START);
    }

    public void startBtn(View view)
    {
        if(!isMuted())
            MediaPlayer.create(this, R.raw.btn_click_ef).start();
        Intent mIntent = new Intent(this, GameActivity2.class);
        startActivity(mIntent.putExtras(mBundle));
        finish();
    }
}