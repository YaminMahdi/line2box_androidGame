package com.diu.yk_games.line2box;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChatFragmentGlobal#//newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragmentGlobal extends Fragment {

    private final String TAG="chat_frag";
    ArrayList<MsgStore> msList;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference("globalChat");
    EditText chatBoxGlobal;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    // TODO: Rename and change types of parameters
//    private String nmData;
//    private String lvlData;



    public ChatFragmentGlobal() {
        // Required empty public constructor
    }


//    public static ChatFragmentGlobal newInstance(String nmData, String lvlData) {
//        ChatFragmentGlobal fragment = new ChatFragmentGlobal();
//        Bundle args = new Bundle();
//        args.putString("nmData", nmData);
//        args.putString("lvlData", lvlData);
//        fragment.setArguments(args);
//        return fragment;
//    }

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }
//
//    }

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
                MsgStore ms = dataSnapshot.getValue(MsgStore.class);
                assert ms != null;
                msList.add(ms);
                if(getActivity()!=null)
                {
                    MsgListAdapter adapter=new MsgListAdapter(getActivity(),msList);  //
                    ListView list = v.findViewById(R.id.showMsgList);
                    list.setAdapter(adapter);
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());}
        });
        ImageButton sendMsg=v.findViewById(R.id.msgSendBtn);
        sendMsg.setOnClickListener(v1->
        {
            GameProfile gp=new GameProfile();
            MsgStore ms =new MsgStore();
            ms.nmData= gp.nm;
            ms.lvlData= gp.lvl.toString();
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