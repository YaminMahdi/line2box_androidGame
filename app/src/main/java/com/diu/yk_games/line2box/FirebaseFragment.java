package com.diu.yk_games.line2box;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FirebaseFragment# newInstance} factory method to
 * create an instance of this fragment.
 */
public class FirebaseFragment extends Fragment
{
    EditText nm1EditText, nm2EditText ;
    Spinner starSpinner;
    String redData, blueData, starData;
    ArrayAdapter<CharSequence> arrAdapter;



    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_firebase, container, false);
        nm1EditText=view.findViewById(R.id.palyerRed);
        nm2EditText=view.findViewById(R.id.palyerBlue);
        starSpinner=view.findViewById(R.id.starSpinner);
        arrAdapter= ArrayAdapter.createFromResource(getActivity(),R.array.star, R.layout.spinner_list);
        arrAdapter.setDropDownViewResource(R.layout.spinner_list);
        starSpinner.setAdapter(arrAdapter);
        //new ArrayAdapter<String>()

        Button btn = view.findViewById(R.id.saveToFirebase);
        btn.setOnClickListener(arg0 ->
        {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d MMMM, h:m a");
            LocalDateTime now = LocalDateTime.now();
            //dtf.format(now));                  //  2021/03/22 16:37:15
//            Calendar cal = Calendar.getInstance();
//            cal.clear(Calendar.YEAR);
//            Date currentTime = cal.getTime();

            int x= starSpinner.getSelectedItemPosition();
            if(!arrAdapter.getItem(x).toString().equals("--(Give Star)--"))
                starData = arrAdapter.getItem(x).toString();
            else
                starData = "★";

            if(!nm1EditText.getText().toString().equals(""))
                redData= nm1EditText.getText().toString()+": "+GameActivity1.scoreRed;
            else
                redData= "Red"+": "+GameActivity1.scoreRed;

            if(!nm2EditText.getText().toString().equals(""))
                blueData= nm2EditText.getText().toString()+": "+GameActivity1.scoreBlue;
            else
                blueData= "Blue"+": "+GameActivity1.scoreBlue;


            String timeData = dtf.format(now);
            DataStore ds = new DataStore(redData,blueData,starData);


            // Write a message to the database
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("ScoreBoard");
            //single
            if(Math.max(GameActivity1.scoreRed, GameActivity1.scoreBlue)== GameActivity1.scoreRed)
                myRef.child("Last Best Player").setValue(redData);
            else
                myRef.child("Last Best Player").setValue(blueData);

            //multiple
            myRef=myRef.child("allScore");
            myRef.child(timeData).setValue(ds);
//            myRef.child(currentTime.toString()).child("blueData").setValue(blueData);
//            myRef.child(currentTime.toString()).child("starData").setValue(starData);

            Toast.makeText(this.getActivity(), "Data Saved", Toast.LENGTH_SHORT).show();
            //requireActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();


        });
        return view;
    }


}