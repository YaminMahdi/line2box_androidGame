package com.diu.yk_games.line2box;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class LeaderBoardFragment extends Fragment {

    private final String TAG="LeadBoard_frag";
    ArrayList<GameProfile> rankList;
    String lastBest, playerId;

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
                                Log.d(TAG, document.getId() );
                                GameProfile xx=document.toObject(GameProfile.class);
                                //xx.playerId=document.getId();
                                rankList.add(xx);
                            }
                            rankList.sort(Comparator.comparing(a -> a.coin));
                            Collections.reverse(rankList);
                            try {
                                RankListAdapter adapter=new RankListAdapter(getContext(),rankList,playerId);
                                ListView list = v.findViewById(R.id.showRankList);
                                list.setAdapter(adapter);
                                //list.smoothScrollToPosition(RankListAdapter.myPosition);
                                int pos=RankListAdapter.myPosition;
                                if(pos>5)
                                    list.post(() -> list.smoothScrollToPosition(pos-2));
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
}