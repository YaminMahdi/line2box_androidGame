package com.diu.yk_games.line2box.presentation.online;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import com.diu.yk_games.line2box.presentation.BlankFragment;
import com.diu.yk_games.line2box.presentation.main.DisplayFragment;
import com.diu.yk_games.line2box.R;
import com.diu.yk_games.line2box.presentation.main.StartActivity;
import com.diu.yk_games.line2box.databinding.ActivityGameMultiBinding;
import com.diu.yk_games.line2box.model.GameProfile;
import com.diu.yk_games.line2box.model.MsgStore;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.PlayGames;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import io.ak1.BubbleTabBar;
import io.ghyeok.stickyswitch.widget.StickySwitch;

public class MultiplayerActivity extends AppCompatActivity{

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
    boolean editing=false;
    Bundle mBundle = new Bundle();
    String playerId, tmpKey=null;
    BubbleTabBar bubbleTabBar;
    View v;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivityGameMultiBinding.inflate(getLayoutInflater());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(binding.getRoot());

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        GameProfile.setPreferences(sharedPref);
        Log.d("TAG", "onCreate: local"+new GameProfile().coin);
        ((TextView)findViewById(R.id.trophyTextId)).setText(""+(int) new GameProfile().coin);
        findViewById(R.id.globalScoreFrag).setVisibility(View.GONE);
        findViewById(R.id.newMsgBoltu).setVisibility(View.GONE);
        findViewById(R.id.emojiPlay).setVisibility(View.GONE);
        String tmpNm=new GameProfile().nm;
        if(sharedPref.getBoolean("needName", true)||tmpNm.contains("Noob"))
            changeNameNeeded();



        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {}
            @Override public void onDrawerOpened(@NonNull View drawerView) {}
            @Override public void onDrawerClosed(@NonNull View drawerView) {}
            @Override public void onDrawerStateChanged(int newState) {
                Log.d("TAG", "onDrawerStateChanged: "+newState);
                if(newState==2)
                {
                    closeKeyboard();
                    if (!isMuted())
                    {
                        MediaPlayer mediaPlayer = MediaPlayer.create(MultiplayerActivity.this, R.raw.slide);
                        mediaPlayer.start();
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                    }
                }
            }
        });
        bubbleTabBar = findViewById(R.id.bubbleTabBar);
        joinId= findViewById(R.id.joinInputId);
        copyPastBtn=findViewById(R.id.copyPastBtn);
        startMatchBtn=findViewById(R.id.startMatchBtnId);
        playerId=getIntent().getExtras().getString("playerId");
        ChatFragmentGlobal.newInstance(playerId);
        lvlUpgrade();
        mBundle.putString("playerId",playerId);
        copyPastBtn.setImageResource(R.drawable.icon_paste);
        copyPastBtn.setTag(R.drawable.icon_paste);
        startMatchBtn.setEnabled(false);
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ifMuted();
        dsList = new ArrayList<>();
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("MultiPlayer");
        myRef.child(sharedPref.getString("tmpKey","69")).removeValue();
        myRef.addChildEventListener(new ChildEventListener()
        {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s)
            {
                Log.d("addList", "onChildAdded: "+dataSnapshot.getKey());
                dsList.add(dataSnapshot.getKey());
            }
            @Override public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("TAG", "Failed to read value.", databaseError.toException());}

        });
        joinId.addTextChangedListener(new TextWatcher()
        {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
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
                            if(dataSnapshot.exists())
                            {
                                int playerCount = Integer.parseInt(requireNonNull(dataSnapshot.getValue(String.class)));
                                if(playerCount==1&&getKey()!=null)
                                {
                                    myRef.child(getKey()).child("playerCount").setValue("2");
                                    //playerCountLocal=2;
                                    startMatchBtn.setEnabled(true);
                                    tmpKey=getKey();
                                    editor.putString("tmpKey",getKey()).apply();
                                    myRef.child(getKey()).child("playerInfo").child("nm2").setValue(new GameProfile().nm);
                                    myRef.child(getKey()).child("playerInfo").child("lvl2").setValue(new GameProfile().getLvlByCal());

                                    MsgStore ms =new MsgStore();
                                    ms.playerId=playerId;
                                    ms.nmData= nm2;
                                    ms.lvlData= lvl2.toString();
                                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM, hh:mm a");
                                    LocalDateTime now = LocalDateTime.now();
                                    ms.timeData = dtf.format(now);
                                    ms.msgData="Joined the match.";

                                    String key2 = myRef.child(getKey()).child("friendlyChat").push().getKey();
                                    assert key2 != null;
                                    myRef.child(getKey()).child("friendlyChat").child(key2).setValue(ms);

                                    bubbleTabBar.setSelected(1,true);
                                    FragmentManager fm=getSupportFragmentManager();
                                    FragmentTransaction ft=fm.beginTransaction();
                                    ft.replace(R.id.chatFragment,ChatFragmentFriendly.newInstance(getKey(),playerId));
                                    ft.commit();

                                    myRef.child(getKey()).child("playerInfo").addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if (dataSnapshot.exists())
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
                            else
                                startMatchBtn.setEnabled(false);

                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {
                            // Failed to read value
                            Log.w("TAG", "Failed to read value.", error.toException());
                        }
                    });
                    }
                }
            }
        });
        nm2=new GameProfile().nm;
        lvl2=new GameProfile().getLvlByCal();
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
                lvl2=new GameProfile().getLvlByCal();
                Log.d("TAG left", "ver: "+nm1+" "+lvl1);
                mBundle.putString("nm2", nm2);
                mBundle.putInt("lvl2", lvl2);

                bubbleTabBar.setSelected(0,true);
                FragmentManager fm=getSupportFragmentManager();
                FragmentTransaction ft=fm.beginTransaction();
                ft.replace(R.id.chatFragment,ChatFragmentGlobal.newInstance(playerId));
                ft.commit();
                findViewById(R.id.newMsgBoltu).setVisibility(View.GONE);

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
                lvl1=new GameProfile().getLvlByCal();
                Log.d("TAG", "ver: "+nm1+" "+lvl1);
                mBundle.putString("nm1", nm1);
                mBundle.putInt("lvl1", lvl1);
                key = myRef.push().getKey();
                editor.putString("tmpKey",key).apply();
                Log.d("TAG", "onCreate key: "+key);
                mBundle.putString("gameKey", key);
                assert key != null;
                myRef.child(key).child("playerCount").setValue("1");
                myRef.child(key).child("playerInfo").child("nm1").setValue(new GameProfile().nm);
                myRef.child(key).child("playerInfo").child("lvl1").setValue(new GameProfile().getLvlByCal());
                myRef.child(key).child("playerInfo").child("nm2").setValue("");
                myRef.child(key).child("playerInfo").child("lvl2").setValue(0);

                MsgStore ms =new MsgStore();
                ms.playerId=playerId;
                ms.nmData= nm1;
                ms.lvlData= lvl1.toString();
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM, hh:mm a");
                LocalDateTime now = LocalDateTime.now();
                ms.timeData = dtf.format(now);
                ms.msgData="Created the match.";

                String key2 = myRef.child(key).child("friendlyChat").push().getKey();
                assert key2 != null;
                myRef.child(key).child("friendlyChat").child(key2).setValue(ms);

                bubbleTabBar.setSelected(1,true);
                FragmentManager fm=getSupportFragmentManager();
                FragmentTransaction ft=fm.beginTransaction();
                ft.replace(R.id.chatFragment,ChatFragmentFriendly.newInstance(key,playerId));
                ft.commit();


                myRef.child(key).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists())
                        {
                            Log.d("TAG -int key", "onDataChange: "+key+" "+getKey()+" "+dataSnapshot.child("playerCount").getValue(String.class));
                            try {
                                int playerCount = Integer.parseInt(requireNonNull(dataSnapshot.child("playerCount").getValue(String.class)));
                                nm2=dataSnapshot.child("playerInfo").child("nm2").getValue(String.class);
                                lvl2=dataSnapshot.child("playerInfo").child("lvl2").getValue(Integer.class);
                                Log.d("TAG", "ver2: "+nm2+" "+lvl2);
                                mBundle.putString("nm2", nm2);
                                mBundle.putInt("lvl2", lvl2);
                                if (playerCount == 2)
                                {
                                    startMatchBtn.setEnabled(true);
                                    //playerCountLocal=2;
                                }
                            }
                            catch (NullPointerException npe) {npe.printStackTrace();}


                        }
                        else
                            startMatchBtn.setEnabled(false);
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
                    if(tmpKey!=null)
                    {
                        myRef.child(tmpKey).removeValue();
                        tmpKey=null;
                    }

                }, 400);
            }
        });

        copyPastBtn.setOnClickListener(v ->
        {
            if(!isMuted())
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);}
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

//        EditText myEditText = findViewById(R.id.chatBoxGlobal);
//        if(myEditText.hasFocus())
//        {
//            KeyboardUtil keyboardUtil = new KeyboardUtil(this, findViewById(R.id.chatFragment));
//            keyboardUtil.enable();
//        }

//        View view = this.getCurrentFocus();
//        if (view instanceof EditText) {
//            ((EditText)view).append("yourText");
//
//            KeyboardUtil keyboardUtil = new KeyboardUtil(this, findViewById(R.id.chatFragment));
//            keyboardUtil.enable();
//        }
//enable it
        //keyboardUtil.enable();

//disable it
        //keyboardUtil.disable();
        final View activityRootView = this.getWindow().getDecorView();
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                Rect r = new Rect();
                //r will be populated with the coordinates of your view that area still visible.

                activityRootView.getWindowVisibleDisplayFrame(r);
                int maxHight=activityRootView.getRootView().getHeight();
                int heightDiff = maxHight - r.height();
                Log.d("TAG", "onGlobalLayout: "+"heidiff: "+heightDiff+" "+r.height()+" "+maxHight);
                LinearLayout layout1 = findViewById(R.id.chatFragmentLinerLayout);
                LinearLayout layout2 = findViewById(R.id.navCloseButtonLayout);
                if (heightDiff > 0.25* maxHight)
                {
                    //Toast.makeText(MultiplayerActivity.this, "hi", Toast.LENGTH_SHORT).show();
                    // if more than 25% of the screen, its probably a keyboard......do something here
                    Log.d("TAG", "onGlobalLayout: here");
                    layout1.setPadding(0, 0, 0, heightDiff);
                    layout2.setPadding(0, 0, 0, heightDiff);

                }
                else
                {
                    layout1.setPadding(0, 0, 0, 0);
                    layout2.setPadding(0, 0, 0, 0);
                }

            }
        });
        bubbleTabBar.addBubbleListener(id ->
        {
            FragmentManager fm=getSupportFragmentManager();
            FragmentTransaction ft=fm.beginTransaction();
            if(id==R.id.globalChat)
            {
                ft.replace(R.id.chatFragment,ChatFragmentGlobal.newInstance(playerId));
            }
            else
            {
                if(key!=null)
                    ft.replace(R.id.chatFragment,ChatFragmentFriendly.newInstance(key,playerId));
                else
                    ft.replace(R.id.chatFragment,new BlankChatFragment());

            }
            ft.commit();
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
            {
                key=dsList.get(i);
                return dsList.get(i);
            }
        }
        return null;
    }
    @SuppressLint("SetTextI18n")
    public void lvlUpgrade()
    {
        GameProfile pf =new GameProfile();
        int tmpLvl=sharedPref.getInt("tmpLvl",1);
        if(tmpLvl!=pf.getLvlByCal())
        {
            if (!isMuted())
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.win_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
            FirebaseFirestore.getInstance().collection("gamerProfile").document(playerId).update("lvl" ,pf.getLvlByCal());
            editor.putInt("tmpLvl",pf.getLvlByCal()).apply();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.dialog_layout_update, findViewById(R.id.updateLayoutDialog)
            );
            builder.setView(view);
            builder.setCancelable(false);
            final AlertDialog alertDialog = builder.create();
            ((TextView)view.findViewById(R.id.warningMessage)).setText("Level Upgraded !");
            TextView updateInfo= view.findViewById(R.id.UpdateInfo);
            updateInfo.setText(tmpLvl+" --> "+ pf.getLvlByCal());
            updateInfo.setTypeface(getResources().getFont(R.font.baloopaaji));
            updateInfo.setTextSize(25);
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

            if (alertDialog.getWindow() != null) {
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            try {alertDialog.show();}
                catch (NullPointerException npe) {npe.printStackTrace();}
        }

    }
    public void changeNameNeeded()
    {
        Toast.makeText(this, "Change Your Name.", Toast.LENGTH_SHORT).show();
        editor.putBoolean("muted", true).apply();
        profileBtn(findViewById(R.id.profileBtn));
        editor.putBoolean("muted", false).apply();
        EditText nmEditText = v.findViewById(R.id.nmTxt);
        nmEditText.setEnabled(true);
        editing=true;
        ((ImageButton) v.findViewById(R.id.nmEditBtn)).setImageResource(R.drawable.icon_save);
        Handler handler = new Handler();
        handler.postDelayed(() ->
        {
            if(nmEditText.requestFocus()) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
                nmEditText.setSelection(nmEditText.getText().length());
            }
        }, 200);
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
        //if (view != null)
        Log.d("TAG", "closeKeyboard: "+(view instanceof EditText));
        if(view instanceof EditText)
        {
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

            ((TextView) view.findViewById(R.id.textMessage)).setText("Do you really want to go back?");
            ((Button) view.findViewById(R.id.buttonYes)).setText("YES");
            ((Button) view.findViewById(R.id.buttonNo)).setText("NO");
            final AlertDialog alertDialog = builder.create();
            view.findViewById(R.id.buttonYes).setOnClickListener(view1 ->
            {
                if(!isMuted())
                {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);}

                alertDialog.dismiss();
                super.onBackPressed();
                startActivity(new Intent(this, StartActivity.class));
                finish();
                //android.os.Process.killProcess(android.os.Process.myPid());
            });
            view.findViewById(R.id.buttonNo).setOnClickListener(view2 ->
            {
                if(!isMuted())
                {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);}
                alertDialog.dismiss();
            });
            if (alertDialog.getWindow() != null) {
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            try {alertDialog.show();}
                catch (NullPointerException npe) {npe.printStackTrace();}
        }
    }

    public void goBack(View view)
    {
        if(!isMuted())
        {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);}
        onGoBack();
    }

    public void onGoBack()
    {
        scrBrdVisible =false;
        FragmentManager fm=getSupportFragmentManager();
        FragmentTransaction ft=fm.beginTransaction();
        ft.replace(R.id.disFragment,new BlankFragment());
        ft.commit();
        findViewById(R.id.multiConstraintLyt).setVisibility(View.VISIBLE);
        findViewById(R.id.globalScoreFrag).setVisibility(View.GONE);
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
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            findViewById(R.id.volBtn).setBackgroundResource(R.drawable.btn_ylw_bg);
            ((ImageButton)findViewById(R.id.volBtn)).setImageResource(R.drawable.icon_vol_unmute);
            editor.putBoolean("muted", false).apply();
        }
    }
    public void ideaBtn(View v)
    {
        if(!isMuted())
        {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(
                R.layout.dialog_layout_info_mul, findViewById(R.id.infoMultiLayoutDialog)
        );
        builder.setView(view);
        builder.setCancelable(false);
        final AlertDialog alertDialog = builder.create();

        view.findViewById(R.id.buttonOkey).setOnClickListener(view1 ->
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

    public void scoreBoard(View view)
    {
        if(!isMuted())
        {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);}
        scrBrdVisible =true;
        FragmentManager fm=getSupportFragmentManager();
        FragmentTransaction ft=fm.beginTransaction();
        ft.replace(R.id.disFragment,new DisplayFragment());
        ft.commit();
        findViewById(R.id.multiConstraintLyt).setVisibility(View.GONE);
        ((TextView)findViewById(R.id.FragLabel)).setText("Global Score Board");
        findViewById(R.id.globalScoreFrag).setVisibility(View.VISIBLE);

    }

    public void leaderBoard(View view)
    {
        if(!isMuted())
        {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);}
        scrBrdVisible =true;
        FragmentManager fm=getSupportFragmentManager();
        FragmentTransaction ft=fm.beginTransaction();
        ft.replace(R.id.disFragment,LeaderBoardFragment.newInstance(playerId));
        ft.commit();
        findViewById(R.id.multiConstraintLyt).setVisibility(View.GONE);
        ((TextView)findViewById(R.id.FragLabel)).setText("Global Rank List");
        findViewById(R.id.globalScoreFrag).setVisibility(View.VISIBLE);
    }

    public void profileBtn(View view)
    {
        if(!isMuted())
        {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MultiplayerActivity.this);
        v = LayoutInflater.from(MultiplayerActivity.this).inflate(
                R.layout.dialog_layout_profile, findViewById(R.id.profileLayoutDialog)
        );
        builder.setView(v);
        //builder.setCancelable(false);
        GameProfile.setPreferences(sharedPref);
        GameProfile x=new GameProfile();
        x.countryEmoji=sharedPref.getString("countryEmoji","");
        x.countryNm=sharedPref.getString("countryNm","");
        ((TextView) v.findViewById(R.id.countryTxt)).setText(x.countryNm+" "+x.countryEmoji);
        ((TextView) v.findViewById(R.id.lvlTxt)).setText(""+x.getLvlByCal());
        ((TextView) v.findViewById(R.id.matchPlayedTxt)).setText(""+x.matchPlayed);
        ((TextView) v.findViewById(R.id.matchWonTxt)).setText(""+x.matchWinMulti);
        v.findViewById(R.id.coinShow).setVisibility(View.GONE);
        EditText nmEditText = v.findViewById(R.id.nmTxt);
        //CircleImageView civ= v.findViewById(R.id.profile_image);
        ImageView civ= v.findViewById(R.id.profile_image);
        ImageManager mgr = ImageManager.create(this);
        PlayGames.getPlayersClient(this).getCurrentPlayer().addOnCompleteListener(mTask ->
        {
            Log.d("TAG", "profileBtn: "+mTask.getResult().getDisplayName());
            Log.d("TAG", "profileBtn: "+mTask.getResult().getPlayerId());
            mgr.loadImage(civ, requireNonNull(mTask.getResult().getIconImageUri()));
        });


        String oldName=new GameProfile().nm;
        Log.d("TAG", "profileBtn nm: "+sharedPref.getString("nm","x"));
        nmEditText.setText(""+oldName);
        final AlertDialog alertDialog = builder.create();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        civ.setOnClickListener(vv->
        {
            Intent intent = new Intent(Intent.ACTION_VIEW).setClassName("com.google.android.play.games", "com.google.android.gms.games.ui.destination.main.MainActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.setData(Uri.parse("market://details?id=" + "com.google.android.play.games"));
                startActivity(intent);
            }
            startActivity(intent);
//            PlayGames.getPlayersClient(this).getCompareProfileIntent("a_6510766239748384229").addOnCompleteListener(TResult->
//            {
//                startActivity(TResult.getResult());
//            });
//            Intent intent = new Intent();
////Clear the activity so the back button returns to your app
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
////Manually specify the package and activity name
////Not really needed as default happens if you don't specify it.
//            intent.addCategory(Intent.CATEGORY_DEFAULT);
////You must specify the current players user. It ensures that Google Play Games is logged in as the same person.
//            intent.putExtra("com.google.android.gms.games.ACCOUNT_KEY", playerId);
//            intent.putExtra("com.google.android.gms.games.SCREEN", 1050); //"Magic" number from the source code for the about page
//            startActivity(intent);
        });
        v.findViewById(R.id.nmEditBtn).setOnClickListener(view1 ->
        {
            if(!isMuted())
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
            if(!editing)
            {
                nmEditText.setEnabled(true);
                nmEditText.setSelection(nmEditText.getText().length());
                ((ImageButton) v.findViewById(R.id.nmEditBtn)).setImageResource(R.drawable.icon_save);
                if(nmEditText.requestFocus()) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
                }
                editing=true;
            }
            else
            {
                closeKeyboard();
                String newNm=nmEditText.getText().toString();
                if(newNm.equals(""))
                {
                    Toast.makeText(this, "Can't be empty.", Toast.LENGTH_SHORT).show();
                    nmEditText.setText(""+oldName);
                }
                else if(newNm.length()==1)
                {
                    Toast.makeText(this, "Can't be single character.", Toast.LENGTH_SHORT).show();
                    nmEditText.setText(""+oldName);
                }
                else
                {
                    GameProfile z= new GameProfile();
                    z.nm = newNm;
                    z.apply();
                    Log.d("TAG", "profileBtn: "+playerId);
                    if(playerId!=null)
                        db.collection("gamerProfile").document(playerId).update("nm" ,""+newNm);
                    nmEditText.setEnabled(false);
                    ((ImageButton) v.findViewById(R.id.nmEditBtn)).setImageResource(R.drawable.icon_edit);
                    editing=false;
                    editor.putBoolean("needName",false).apply();
                }
            }
        });
        v.findViewById(R.id.buttonSaveInfo).setOnClickListener(view1 ->
        {
            if(!isMuted())
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
            closeKeyboard();
            String newNm=nmEditText.getText().toString();
            if(newNm.equals(""))
            {
                Toast.makeText(this, "Can't be empty.", Toast.LENGTH_SHORT).show();
                nmEditText.setText(""+oldName);
            }
            else if(newNm.length()==1)
            {
                Toast.makeText(this, "Can't be single character.", Toast.LENGTH_SHORT).show();
                nmEditText.setText(""+oldName);
            }
            else
            {
                GameProfile z= new GameProfile();
                z.nm = newNm;
                z.apply();
                Log.d("TAG", "profileBtn: "+playerId);
                if(playerId!=null)
                    db.collection("gamerProfile").document(playerId).update("nm" ,""+newNm);
                nmEditText.setEnabled(false);
                ((ImageButton) v.findViewById(R.id.nmEditBtn)).setImageResource(R.drawable.icon_edit);
                editing=false;
                editor.putBoolean("needName",false).apply();
                alertDialog.dismiss();
            }
        });


        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        try {alertDialog.show();}
                catch (NullPointerException npe) {npe.printStackTrace();}
    }

    public void closeNavBtn(View view)
    {
        closeKeyboard();
        mDrawerLayout.closeDrawer(GravityCompat.START);
        findViewById(R.id.newMsgBoltu).setVisibility(View.GONE);
       // getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

    }
    public void openNavBtn(View view)
    {
        mDrawerLayout.openDrawer(GravityCompat.START);

//        FragmentManager fm=getSupportFragmentManager();
//        FragmentTransaction ft=fm.beginTransaction();
//        if(playerCountLocal==1 && findViewById(R.id.newMsgBoltu).getVisibility() != View.VISIBLE)
//        {
//            bubbleTabBar.setSelected(0,true);
//            ft.replace(R.id.chatFragment,new ChatFragmentGlobal());
//        }
//        else
//        {
//            bubbleTabBar.setSelected(1,true);
//            if(key!=null)
//                ft.replace(R.id.chatFragment,ChatFragmentFriendly.newInstance(key));
//            else
//                ft.replace(R.id.chatFragment,new BlankFragment());
//        }
//        ft.commit();
        findViewById(R.id.newMsgBoltu).setVisibility(View.GONE);


/*        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);*/  //show keyboard


        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

    }

    public void backBtn(View view)
    {
        if (!isMuted())
        {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
        }
        onBackPressed();
    }

    public void startBtn(View view)
    {
        if(!isMuted())
        {
                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);}
        Intent mIntent = new Intent(this, GameActivity2.class);
        startActivity(mIntent.putExtra("bundleInfo",mBundle));
        finish();
    }


}