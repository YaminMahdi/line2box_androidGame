package com.diu.yk_games.line2box;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class DisplayFragment extends Fragment
{


    private final String TAG="Disfrag";
    ArrayList<DataStore> dsList;
    String bestScore="\n\n\nNetwork Error";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize dataset, this data would usually come from a local content provider or
        // remote server.
        dsList = new ArrayList<>();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("ScoreBoard");

        myRef.child("Last Best Player").addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                bestScore = dataSnapshot.getValue(String.class);
                //Log.d(TAG, "Last Value is: " + bestScore);

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
        myRef.child("allScore").addChildEventListener(new ChildEventListener()
        {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s)
            {
                DataStore ds = dataSnapshot.getValue(DataStore.class);
                assert ds != null;
                //ds.timeData = dataSnapshot.getKey();
                dsList.add(ds);
                //Log.i(TAG,"key data = " + ds.timeData);

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

                Log.w(TAG, "Failed to read value.", databaseError.toException());

            }

        });
    }


    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {

        View v=inflater.inflate(R.layout.fragment_display, container, false);
        TextView lbs= v.findViewById(R.id.lastBestScore);

//        Handler handler = new Handler();
//        handler.postDelayed(() ->

        while (dsList.isEmpty())
        {
            lbs.setText("Last Best Score is from:     "+bestScore);
            //Log.d(TAG, "Last Value is: " + bestScore);
            MyListAdapter adapter=new MyListAdapter(getActivity(),dsList);  //
            ListView list = v.findViewById(R.id.showNotesList);
            list.setAdapter(adapter);
        }
        //, 1420);

        return v;
    }

}