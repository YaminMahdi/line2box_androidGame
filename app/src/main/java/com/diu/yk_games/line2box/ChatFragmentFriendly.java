package com.diu.yk_games.line2box;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;

import pl.droidsonroids.gif.GifImageView;


public class ChatFragmentFriendly extends Fragment {
    private final String TAG="chat_frag_Friendly";
    ArrayList<MsgStore> msList;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef;
    EditText chatBoxFriendly;
    String tempMsg=null, lastMsg=null;
    static String key=null;
    Context context;
    Activity activity;

    public static ChatFragmentFriendly newInstance(String key)
    {
        ChatFragmentFriendly fragment = new ChatFragmentFriendly();
        Bundle args = new Bundle();
        args.putString("key", key);
        fragment.setArguments(args);
        return fragment;
    }

    public ChatFragmentFriendly() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            key = getArguments().getString("key");
        }
        myRef = database.getReference("MultiPlayer").child(key).child("friendlyChat");

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v= inflater.inflate(R.layout.fragment_chat_friendly, container, false);
        chatBoxFriendly = v.findViewById(R.id.chatBoxFriendly);
        chatBoxFriendly.isFocusableInTouchMode();
        chatBoxFriendly.requestFocus();

        context=requireContext();
        activity=requireActivity();
        SharedPreferences sharedPref = context.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        GameProfile.setPreferences(sharedPref);

        msList = new ArrayList<>();
        MediaPlayer mp=MediaPlayer.create(context, R.raw.pop);
        DrawerLayout mDrawerLayout = activity.findViewById(R.id.drawer_layout);

        myRef.addChildEventListener(new ChildEventListener()
        {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s)
            {
                MsgStore ms = dataSnapshot.getValue(MsgStore.class);
                assert ms != null;
                msList.add(ms);
                lastMsg=ms.msgData;
                if(getActivity()!=null)
                {
                    MsgListAdapter adapter=new MsgListAdapter(getActivity(),msList);  //
                    ListView list = v.findViewById(R.id.showMsgList);
                    list.setAdapter(adapter);
                    getActivity().findViewById(R.id.newMsgBoltu).setVisibility(View.VISIBLE);

                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());}
        });
        myRef.addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!sharedPref.getBoolean("muted", false))
                {
                    if(lastMsg.equals("🤣"))
                        emojiRunner(R.drawable.emoji_haha,R.raw.haha,v);
                    else if(lastMsg.equals("😭"))
                        emojiRunner(R.drawable.emoji_cry,R.raw.cry,v);
                    else if(lastMsg.equals("😱"))
                        emojiRunner(R.drawable.emoji_scream,R.raw.scream,v);
                    else if(lastMsg.equals("😘"))
                        emojiRunner(R.drawable.emoji_kiss,R.raw.kiss,v);
                    else if(lastMsg.equals("🥱"))
                        emojiRunner(R.drawable.emoji_yawn,R.raw.yawn,v);
                    else if(lastMsg.equals(tempMsg))
                        MediaPlayer.create(context, R.raw.pop).start();
                    else if(!mDrawerLayout.isDrawerOpen(GravityCompat.START))
                        mp.start();
                    tempMsg=" # # 69";
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());}
        });
        ImageButton sendMsg=v.findViewById(R.id.msgSendBtn);
        sendMsg.setOnClickListener(v1->
        {
            sendThisMsg(chatBoxFriendly.getText().toString());
            chatBoxFriendly.setText("");
        });
        v.findViewById(R.id.sendHaha).setOnClickListener(v1-> sendThisMsg("🤣"));
        v.findViewById(R.id.sendCry).setOnClickListener(v1-> sendThisMsg("😭"));
        v.findViewById(R.id.sendKiss).setOnClickListener(v1-> sendThisMsg("😘"));
        v.findViewById(R.id.sendScream).setOnClickListener(v1-> sendThisMsg("😱"));
        v.findViewById(R.id.sendYawn).setOnClickListener(v1-> sendThisMsg("🥱"));

        return v;
    }
    public void emojiRunner(int gif, int sound, View v)
    {
        v.findViewById(R.id.sendHaha).setEnabled(false);
        v.findViewById(R.id.sendCry).setEnabled(false);
        v.findViewById(R.id.sendKiss).setEnabled(false);
        v.findViewById(R.id.sendScream).setEnabled(false);
        v.findViewById(R.id.sendYawn).setEnabled(false);
        MediaPlayer.create(context, sound).start();
        ((GifImageView)activity.findViewById(R.id.emojiPlay)).setImageResource(gif);
        activity.findViewById(R.id.emojiPlay).setVisibility(View.VISIBLE);
        ((DrawerLayout)activity.findViewById(R.id.drawer_layout)).closeDrawer(GravityCompat.START);
        activity.findViewById(R.id.newMsgBoltu).setVisibility(View.GONE);
        Handler handler = new Handler();
        handler.postDelayed(() ->
                {
                    activity.findViewById(R.id.emojiPlay).setVisibility(View.GONE);
                    v.findViewById(R.id.sendHaha).setEnabled(true);
                    v.findViewById(R.id.sendCry).setEnabled(true);
                    v.findViewById(R.id.sendKiss).setEnabled(true);
                    v.findViewById(R.id.sendScream).setEnabled(true);
                    v.findViewById(R.id.sendYawn).setEnabled(true);
                }, 2500);

    }
    public void sendThisMsg(String msg)
    {
        GameProfile gp=new GameProfile();
        MsgStore ms =new MsgStore();
        ms.nmData= gp.nm;
        ms.lvlData= gp.lvl.toString();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM, hh:mm a");
        LocalDateTime now = LocalDateTime.now();
        ms.timeData = dtf.format(now);
        tempMsg=msg;
        ms.msgData=msg;
        String key = myRef.push().getKey();
        assert key != null;
        myRef.child(key).setValue(ms);
    }
}

//    Handler handler = new Handler();
//    handler.postDelayed(() ->
//            {
//
//            }, 600);