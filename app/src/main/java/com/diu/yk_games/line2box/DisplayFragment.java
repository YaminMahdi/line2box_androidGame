package com.diu.yk_games.line2box;

import android.annotation.SuppressLint;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize dataset, this data would usually come from a local content provider or
        // remote server.
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
                            }
                            catch (NullPointerException npe) {npe.printStackTrace();}
                        } else {Log.d(TAG, "Error getting documents: ", task.getException());}
                    }
                });
        return v;
    }

}