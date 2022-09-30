package com.diu.yk_games.line2box;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.util.Log;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;


public class LeaderBoardFragment extends Fragment {

    private final String TAG="LeadBoard_frag";
    ArrayList<GameProfile> rankList;
    String lastBest, playerId;
    SharedPreferences sharedPref;
    Context context;


    public static LeaderBoardFragment newInstance(String playerId)
    {
        LeaderBoardFragment fragment = new LeaderBoardFragment();
        Bundle args = new Bundle();
        args.putString("playerId", playerId);
        fragment.setArguments(args);
        return fragment;
    }

    public LeaderBoardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            playerId = getArguments().getString("playerId");
        }
        sharedPref = requireContext().getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        GameProfile.setPreferences(sharedPref);
        context=getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View v= inflater.inflate(R.layout.fragment_leader_board, container, false);
        TextView lbr= v.findViewById(R.id.lastBestRank);
        rankList = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("gamerProfile")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult())
                            {
                                //Log.d(TAG, document.getId() );
                                GameProfile xx=document.toObject(GameProfile.class);
                                rankList.add(xx);
                            }
                            rankList.sort(Comparator.comparing(a -> a.coin));
                            Collections.reverse(rankList);
                            int pos=findIndex(rankList,playerId);
                            try {
                                RankListAdapter adapter=new RankListAdapter(getContext(),rankList,playerId);
                                ListView list = v.findViewById(R.id.showRankList);
                                list.setAdapter(adapter);
                                list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        GameProfile gamerPro = (GameProfile) list.getItemAtPosition(position);
                                        if(position!=pos)
                                        {
                                            if(!sharedPref.getBoolean("muted", false))
                                            {
                                                MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.btn_click_ef);
                                                mediaPlayer.start();
                                                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                                            }
                                            if(!gamerPro.playerId.equals(""))
                                            {
                                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                                db.collection("gamerProfile").document(gamerPro.playerId)
                                                        .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                            @SuppressLint("SetTextI18n")
                                                            @Override
                                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                                if(documentSnapshot.exists())
                                                                {
                                                                    GameProfile server2device = Objects.requireNonNull(documentSnapshot.toObject(GameProfile.class));
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
                                                                    if(!server2device.countryNm.equals(""))
                                                                        ((TextView) v.findViewById(R.id.countryTxt)).setText(server2device.countryNm+" "+server2device.countryEmoji);
                                                                    else
                                                                        v.findViewById(R.id.countryLayout).setVisibility(View.GONE);
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

                                    }
                                });


                                        // rankList.indexOf(user);
                                //Log.d(TAG, "onComplete(pos): "+pos+" ser- "+rankList.get(pos).playerId+" "+playerId);
                                if(pos>5)
                                    list.setSelection(pos-1);
                                    //list.post(() -> list.smoothScrollToPosition(pos));
                            }
                            catch (NullPointerException npe) {npe.printStackTrace();}
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
        // Inflate the layout for this fragment
        return v;
    }
    public int findIndex(ArrayList<GameProfile> list, String id)
    {
        int index=0;
        for(GameProfile o : list)
        {
            if (o.playerId.equals(id))
                return index;
            index++;
        }
        return -1;
    }
}