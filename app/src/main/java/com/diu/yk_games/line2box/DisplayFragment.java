package com.diu.yk_games.line2box;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Objects;


public class DisplayFragment extends Fragment
{


    private final String TAG="Dis_frag";
    ArrayList<DataStore> dsList;
    String bestScore="\n\n\nNetwork Error";
    Context context;
    GameProfile p1Pro,p2Pro;
    SharedPreferences sharedPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize dataset, this data would usually come from a local content provider or
        // remote server.
        context=getContext();
        sharedPref = requireContext().getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    }


    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {

        View v=inflater.inflate(R.layout.fragment_display, container, false);
        TextView lbs= v.findViewById(R.id.lastBestScore);
        dsList = new ArrayList<>();

        //FirebaseDatabase database = FirebaseDatabase.getInstance();
        //DatabaseReference myRef = database.getReference("ScoreBoard");

//        myRef.child("Last Best Player").addValueEventListener(new ValueEventListener() {
//            @SuppressLint("SetTextI18n")
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
//            {
//                if(dataSnapshot.exists())
//                {
//                    bestScore = dataSnapshot.getValue(String.class);
//                    lbs.setText("Last Best Score is from:     "+bestScore);
//                }
//            }
//            @Override public void onCancelled(@NonNull DatabaseError error) {
//                Log.w(TAG, "Failed to read value.", error.toException());}
//        });
//        myRef.child("allScore").addChildEventListener(new ChildEventListener()
//        {
//            @Override
//            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s)
//            {
//                if(dataSnapshot.exists())
//                {
//                    DataStore ds = dataSnapshot.getValue(DataStore.class);
//                    assert ds != null;
//                    dsList.add(0,ds);
//                    MyListAdapter adapter=new MyListAdapter(getActivity(),dsList);  //
//                    ListView list = v.findViewById(R.id.showScoreList);
//                    list.setAdapter(adapter);
//                }
//
//            }
//            @Override public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
//            @Override public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
//            @Override public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
//            @Override public void onCancelled(@NonNull DatabaseError databaseError) {
//                Log.w(TAG, "Failed to read value.", databaseError.toException());}
//
//        });

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        //Source source = Source.CACHE;
        db.collection("LastBestPlayer").document("LastBestPlayer")
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    Log.d(TAG, "Cached document data: " + document.getData());
                    bestScore= Objects.requireNonNull(Objects.requireNonNull(document.getData()).get("info")).toString();
                    lbs.setText("\uD83D\uDC51 "+bestScore);
                } else {Log.d(TAG, "Cached get failed: ", task.getException());}
            }
        });
        db.collection("ScoreBoard")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult())
                            {
                                DataStore ds = document.toObject(DataStore.class);
                                dsList.add(0,ds);
                            }
                            try {

                                MyListAdapter adapter=new MyListAdapter(getContext(),dsList);  //
                                ListView list = v.findViewById(R.id.showScoreList);
                                list.setAdapter(adapter);
                                list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        DataStore gamerPro = (DataStore) list.getItemAtPosition(position);

                                        if(gamerPro.plr1Id.equals("offline"))
                                            Toast.makeText(context, "Offline match doesn't have Profile Info.", Toast.LENGTH_SHORT).show();
                                        else if(gamerPro.plr1Id.equals(""))
                                            Toast.makeText(context, "Old match doesn't have Profile Info.", Toast.LENGTH_SHORT).show();
                                        else
                                        {
                                            if(!sharedPref.getBoolean("muted", false))
                                            {
                                                MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.btn_click_ef);
                                                mediaPlayer.start();
                                                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                                            }
                                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                            View v = LayoutInflater.from(context).inflate(
                                                    R.layout.dialog_layout_scr_globe, parent.findViewById(R.id.scoreDetailsLayoutDialog)
                                            );
                                            builder.setView(v);

                                            Log.d(TAG, position+ "onItemClick: 1id "+gamerPro.plr1Id);
                                            Log.d(TAG, "onItemClick: 2id "+gamerPro.plr2Id);
                                            db.collection("gamerProfile").document(gamerPro.plr1Id)
                                                    .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                        @SuppressLint("SetTextI18n")
                                                        @Override
                                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                            if(documentSnapshot.exists())
                                                            {
                                                                String[] scr=gamerPro.redData.split(" ");
                                                                Log.d(TAG, "onSuccess: scr "+scr[scr.length-1]);
                                                                ((TextView)v.findViewById(R.id.plr1Score)).setText(scr[scr.length-1]);
                                                                ((TextView)v.findViewById(R.id.plr1Cup)).setText(gamerPro.plr1Cup);
                                                                Log.d(TAG, "onSuccess: cup "+gamerPro.plr1Cup);
                                                                p1Pro = Objects.requireNonNull(documentSnapshot.toObject(GameProfile.class));
                                                                if(!p1Pro.countryEmoji.equals(""))
                                                                    ((TextView)v.findViewById(R.id.plr1Flag)).setText(p1Pro.countryEmoji);
                                                                ((TextView)v.findViewById(R.id.plr1Nm)).setText(p1Pro.nm);
                                                                Log.d(TAG, "onSuccess: nm "+ p1Pro.nm);
                                                                ((TextView)v.findViewById(R.id.plr1Lvl)).setText(""+p1Pro.lvl);

                                                            }
                                                        }
                                                    });
                                            db.collection("gamerProfile").document(gamerPro.plr2Id)
                                                    .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                        @SuppressLint("SetTextI18n")
                                                        @Override
                                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                            if(documentSnapshot.exists())
                                                            {
                                                                String[] scr=gamerPro.blueData.split(" ");
                                                                ((TextView)v.findViewById(R.id.plr2Score)).setText(scr[scr.length-1]);
                                                                ((TextView)v.findViewById(R.id.plr2Cup)).setText(gamerPro.plr2Cup);

                                                                p2Pro = Objects.requireNonNull(documentSnapshot.toObject(GameProfile.class));
                                                                if(!p2Pro.countryEmoji.equals(""))
                                                                    ((TextView)v.findViewById(R.id.plr2Flag)).setText(p2Pro.countryEmoji);
                                                                ((TextView)v.findViewById(R.id.plr2Nm)).setText(p2Pro.nm);
                                                                ((TextView)v.findViewById(R.id.plr2Lvl)).setText(""+p2Pro.lvl);

                                                            }
                                                            final AlertDialog alertDialog = builder.create();

                                                            if (alertDialog.getWindow() != null) {
                                                                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                                                            }
                                                            try {alertDialog.show();}
                                                            catch (Exception e) {e.printStackTrace();}
                                                        }

                                                    });


                                            v.findViewById(R.id.linLayoutPlr1).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    if(!sharedPref.getBoolean("muted", false))
                                                    {
                                                        MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.btn_click_ef);
                                                        mediaPlayer.start();
                                                        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                                                    }
                                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                                    View v2 = LayoutInflater.from(context).inflate(
                                                            R.layout.dialog_layout_profile, parent.findViewById(R.id.profileLayoutDialog)
                                                    );
                                                    builder.setView(v2);
                                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                                    );
                                                    params.setMargins(60, 0, 60, 0);
                                                    LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(
                                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                                            LinearLayout.LayoutParams.MATCH_PARENT
                                                    );
                                                    v2.findViewById(R.id.linearLayoutFrame).setLayoutParams(params);
                                                    v2.findViewById(R.id.linearLayoutFrame).setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cocX)));
                                                    v2.findViewById(R.id.linearLayoutFrame).setBackgroundTintMode(PorterDuff.Mode.ADD);
                                                    if(!p1Pro.countryNm.equals(""))
                                                        ((TextView) v2.findViewById(R.id.countryTxt)).setText(p1Pro.countryNm+" "+p1Pro.countryEmoji);
                                                    else
                                                        v2.findViewById(R.id.countryLayout).setVisibility(View.GONE);
                                                    ((TextView) v2.findViewById(R.id.lvlTxt)).setText(""+p1Pro.lvl);
                                                    ((TextView) v2.findViewById(R.id.coinHave)).setText(""+p1Pro.coin);
                                                    ((TextView) v2.findViewById(R.id.matchPlayedTxt)).setText(""+p1Pro.matchPlayed);
                                                    ((TextView) v2.findViewById(R.id.matchWonTxt)).setText(""+p1Pro.matchWinMulti);
                                                    EditText nmEditText = v2.findViewById(R.id.nmTxt);
                                                    nmEditText.setEnabled(false);
                                                    nmEditText.setText(p1Pro.nm);
                                                    //nmEditText.setVisibility(View.GONE);
                                                    ((TextView) v2.findViewById(R.id.profileTitle)).setTextSize(28);

                                                    v2.findViewById(R.id.profileShapeLayout).setVisibility(View.GONE);
                                                    v2.findViewById(R.id.nmEditBtn).setVisibility(View.GONE);
                                                    v2.findViewById(R.id.nmLTxt).setVisibility(View.GONE);
                                                    v2.findViewById(R.id.themeBox).setVisibility(View.GONE);
                                                    v2.findViewById(R.id.countryLTxt).setVisibility(View.GONE);
                                                    v2.findViewById(R.id.buttonSaveInfo).setVisibility(View.GONE);
                                                    final AlertDialog alertDialog = builder.create();

                                                    if (alertDialog.getWindow() != null) {
                                                        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                                                    }
                                                    try {alertDialog.show();}
                                                    catch (Exception e) {e.printStackTrace();}

                                                }
                                            });
                                            v.findViewById(R.id.linLayoutPlr2).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    if(!sharedPref.getBoolean("muted", false))
                                                    {
                                                        MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.btn_click_ef);
                                                        mediaPlayer.start();
                                                        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                                                    }
                                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                                    View v2 = LayoutInflater.from(context).inflate(
                                                            R.layout.dialog_layout_profile, parent.findViewById(R.id.profileLayoutDialog)
                                                    );
                                                    builder.setView(v2);
                                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                                    );
                                                    params.setMargins(420, 0, 60, 0);
                                                    v2.findViewById(R.id.linearLayoutFrame).setLayoutParams(params);
                                                    v2.findViewById(R.id.linearLayoutFrame).setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cocX)));
                                                    v2.findViewById(R.id.linearLayoutFrame).setBackgroundTintMode(PorterDuff.Mode.ADD);

                                                    if(!p2Pro.countryNm.equals(""))
                                                        ((TextView) v2.findViewById(R.id.countryTxt)).setText(p2Pro.countryNm+" "+p2Pro.countryEmoji);
                                                    else
                                                        v2.findViewById(R.id.countryLayout).setVisibility(View.GONE);
                                                    ((TextView) v2.findViewById(R.id.lvlTxt)).setText(""+p2Pro.lvl);
                                                    ((TextView) v2.findViewById(R.id.coinHave)).setText(""+p2Pro.coin);
                                                    ((TextView) v2.findViewById(R.id.matchPlayedTxt)).setText(""+p2Pro.matchPlayed);
                                                    ((TextView) v2.findViewById(R.id.matchWonTxt)).setText(""+p2Pro.matchWinMulti);
                                                    EditText nmEditText = v2.findViewById(R.id.nmTxt);
                                                    nmEditText.setEnabled(false);
                                                    nmEditText.setText(p2Pro.nm);
                                                    //nmEditText.setVisibility(View.GONE);
                                                    ((TextView) v2.findViewById(R.id.profileTitle)).setTextSize(28);

                                                    v2.findViewById(R.id.profileShapeLayout).setVisibility(View.GONE);
                                                    v2.findViewById(R.id.nmEditBtn).setVisibility(View.GONE);
                                                    v2.findViewById(R.id.nmLTxt).setVisibility(View.GONE);
                                                    v2.findViewById(R.id.themeBox).setVisibility(View.GONE);
                                                    v2.findViewById(R.id.countryLTxt).setVisibility(View.GONE);
                                                    v2.findViewById(R.id.buttonSaveInfo).setVisibility(View.GONE);
                                                    final AlertDialog alertDialog = builder.create();

                                                    if (alertDialog.getWindow() != null) {
                                                        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                                                    }
                                                    try {alertDialog.show();}
                                                    catch (Exception e) {e.printStackTrace();}

                                                }
                                            });




                                        }
                                    }
                                });
                            }
                            catch (NullPointerException npe) {npe.printStackTrace();}
                        } else {Log.d(TAG, "Error getting documents: ", task.getException());}
                    }
                });
        return v;
    }

}