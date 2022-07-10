package com.diu.yk_games.line2box;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NameInfoFragment# newInstance} factory method to
 * create an instance of this fragment.
 */
public class NameInfoFragment extends Fragment
{
    EditText nm1EditText, nm2EditText ;
    public String nm1="Red", nm2="Blue";


    @Override
    public void onDetach()
    {
        super.onDetach();
        ((GameActivity1) requireActivity()).onStopFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_nminfo, container, false);
        nm1EditText=view.findViewById(R.id.palyerRed);
        nm2EditText=view.findViewById(R.id.palyerBlue);

        //new ArrayAdapter<String>()

        Button btn = view.findViewById(R.id.playBtn);
        btn.setOnClickListener(arg0 ->
        {
            if(!nm1EditText.getText().toString().equals(""))
            {
                nm1=nm1EditText.getText().toString();
            }
            if(!nm2EditText.getText().toString().equals(""))
            {
                nm2=nm2EditText.getText().toString();
            }
            GameActivity1.nm1=nm1;
            GameActivity1.nm2=nm2;
            requireActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
            //String Item = getActivity().getIntent().getExtras().getString("name");
            //getContext().getView().findViewById(R.id.nmLayout).setVisibility(View.VISIBLE);


        });
        return view;
    }


}