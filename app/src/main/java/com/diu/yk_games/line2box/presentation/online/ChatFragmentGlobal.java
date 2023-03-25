package com.diu.yk_games.line2box.presentation.online;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.diu.yk_games.line2box.presentation.MsgListAdapter;
import com.diu.yk_games.line2box.R;
import com.diu.yk_games.line2box.model.GameProfile;
import com.diu.yk_games.line2box.model.MsgStore;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChatFragmentGlobal#//newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragmentGlobal extends Fragment {

    ArrayList<MsgStore> msList;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference("globalChat");
    EditText chatBoxGlobal;
    String playerId;
    Context context;
    Activity activity;


    public static ChatFragmentGlobal newInstance(String playerId)
    {
        ChatFragmentGlobal fragment = new ChatFragmentGlobal();
        Bundle args = new Bundle();
        args.putString("playerId", playerId);
        fragment.setArguments(args);
        return fragment;
    }


    public ChatFragmentGlobal() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            playerId = getArguments().getString("playerId");
        }
        context=getContext();
        activity=getActivity();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View v=inflater.inflate(R.layout.fragment_chat_global, container, false);
        // Inflate the layout for this fragment
        chatBoxGlobal = v.findViewById(R.id.chatBoxGlobal);
        chatBoxGlobal.isFocusableInTouchMode();
        chatBoxGlobal.requestFocus();
        SharedPreferences sharedPref = requireContext().getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        GameProfile.setPreferences(sharedPref);

        msList = new ArrayList<>();
        myRef.addChildEventListener(new ChildEventListener()
        {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s)
            {
                if(dataSnapshot.exists())
                {
                    MsgStore ms = dataSnapshot.getValue(MsgStore.class);
                    assert ms != null;
                    msList.add(ms);
                    try
                    {
                        MsgListAdapter adapter=new MsgListAdapter(getActivity(),msList);  //
                        ListView list = v.findViewById(R.id.showMsgList);
                        list.setAdapter(adapter);
                        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                MsgStore msgData = (MsgStore) list.getItemAtPosition(position);
                                //prestationEco str = (prestationEco)o; //As you are using Default String Adapter

                                if(!sharedPref.getBoolean("muted", false))
                                {
                                    MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.btn_click_ef);
                                    mediaPlayer.start();
                                    mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                                }
                                if(!msgData.playerId.equals(""))
                                {
                                    Toast.makeText(context,"Long Press To Copy Text/ID",Toast.LENGTH_SHORT).show();
                                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                                    db.collection("gamerProfile").document(msgData.playerId)
                                            .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                @SuppressLint("SetTextI18n")
                                                @Override
                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                    if(documentSnapshot.exists())
                                                    {
                                                        GameProfile server2device = documentSnapshot.toObject(GameProfile.class);
                                                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                                        View v = LayoutInflater.from(context).inflate(
                                                                R.layout.dialog_layout_profile, parent.findViewById(R.id.profileLayoutDialog)
                                                        );
                                                        builder.setView(v);
                                                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                                                LinearLayout.LayoutParams.WRAP_CONTENT
                                                        );
                                                        params.setMargins(60, 150, 60, 0);
                                                        v.findViewById(R.id.linearLayoutFrame).setLayoutParams(params);
                                                        //v.findViewById(R.id.linearLayoutFrame).setPadding(20,0,20,0);
                                                        assert server2device != null;
                                                        ((TextView) v.findViewById(R.id.countryTxt)).setText(server2device.countryNm+" "+server2device.countryEmoji);
                                                        ((TextView) v.findViewById(R.id.lvlTxt)).setText(""+server2device.lvl);
                                                        ((TextView) v.findViewById(R.id.coinHave)).setText(""+server2device.coin);
                                                        ((TextView) v.findViewById(R.id.matchPlayedTxt)).setText(""+server2device.matchPlayed);
                                                        ((TextView) v.findViewById(R.id.matchWonTxt)).setText(""+server2device.matchWinMulti);
                                                        EditText nmEditText = v.findViewById(R.id.nmTxt);
                                                        nmEditText.setEnabled(false);
                                                        nmEditText.setText(server2device.nm);
                                                        //nmEditText.setVisibility(View.GONE);
                                                        ((TextView) v.findViewById(R.id.profileTitle)).setTextSize(28);

                                                        v.findViewById(R.id.profileShapeLayout).setVisibility(View.GONE);
                                                        v.findViewById(R.id.nmEditBtn).setVisibility(View.GONE);
                                                        v.findViewById(R.id.nmLTxt).setVisibility(View.GONE);
                                                        v.findViewById(R.id.themeBox).setVisibility(View.GONE);
                                                        v.findViewById(R.id.countryLTxt).setVisibility(View.GONE);
                                                        v.findViewById(R.id.buttonSaveInfo).setVisibility(View.GONE);
                                                        final AlertDialog alertDialog = builder.create();

                                                        if (alertDialog.getWindow() != null) {
                                                            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                                                        }
                                                        try {alertDialog.show();}
                                                        catch (NullPointerException npe) {npe.printStackTrace();}
                                                    }
                                                }
                                            });
                                }
                                else
                                    Toast.makeText(context,"Older messages don't have profile info.",Toast.LENGTH_SHORT).show();

                            }
                        });
                        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                            @Override
                            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
                            {
                                Toast.makeText(context, "Text/ID copied", Toast.LENGTH_SHORT).show();
                                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("msgData", ms.msgData);
                                clipboard.setPrimaryClip(clip);
                                return true;
                            }
                        });
                    }
                    catch (NullPointerException npe) {npe.printStackTrace();}
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
        ImageButton sendMsg=v.findViewById(R.id.msgSendBtn);
        sendMsg.setOnClickListener(v1->
        {
            MediaPlayer mp = MediaPlayer.create(requireContext(), R.raw.pop);
            mp.start();
            mp.setOnCompletionListener(MediaPlayer::release);
            GameProfile gp=new GameProfile();
            MsgStore ms =new MsgStore();
            ms.playerId=playerId;
            ms.nmData= gp.nm;
            ms.lvlData= gp.getLvlByCal().toString();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM, hh:mm a");
            LocalDateTime now = LocalDateTime.now();
            ms.timeData = dtf.format(now);
            ms.msgData=chatBoxGlobal.getText().toString();

            String key = myRef.push().getKey();
            assert key != null;
            myRef.child(key).setValue(ms);
            chatBoxGlobal.setText("");
        });
        return v;
    }
}