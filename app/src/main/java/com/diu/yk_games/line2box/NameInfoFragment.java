package com.diu.yk_games.line2box;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NameInfoFragment# newInstance} factory method to
 * create an instance of this fragment.
 */
public class NameInfoFragment extends Fragment
{
    EditText nm1EditText, nm2EditText ;
    public String nm1="Red", nm2="Blue",tNm1,tNm2;
    boolean nm1Def=true;

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
        SharedPreferences sharedPref = requireContext().getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        GameProfile.setPreferences(sharedPref);

        CheckBox nmSave=view.findViewById(R.id.nmSaveBox);
        nm1EditText=view.findViewById(R.id.palyerRed);
        nm2EditText=view.findViewById(R.id.palyerBlue);

        //nm1EditText.setEnabled(false);

        String tmpNm1= sharedPref.getString("plrNm1",""),
                tmpNm2= sharedPref.getString("plrNm2",""),
                prfNm=new GameProfile().nm;
        if(!tmpNm1.equals(""))
            nm1EditText.setText(tmpNm1);
        else
            nm1EditText.setText(prfNm);
        if(!tmpNm2.equals(""))
            nm2EditText.setText(tmpNm2);

        //nmEditText.setSelection(nmEditText.getText().length());

        //nm1=new GameProfile().nm;

        //new ArrayAdapter<String>()

        Button btn = view.findViewById(R.id.playBtn);
        btn.setOnClickListener(arg0 ->
        {
            if(!sharedPref.getBoolean("muted", false))
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(getContext(), R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
            if(!nm1EditText.getText().toString().equals(""))
            {
                nm1=nm1EditText.getText().toString();
            }
            if(!nm2EditText.getText().toString().equals(""))
            {
                nm2=nm2EditText.getText().toString();
            }
            if(nmSave.isChecked())
            {
                if(!nm1EditText.getText().toString().equals(""))
                    prefEditor.putString("plrNm1",nm1).apply();
                if(!nm2EditText.getText().toString().equals(""))
                    prefEditor.putString("plrNm2",nm2).apply();
            }
            GameActivity1.nm1=nm1;
            GameActivity1.nm2=nm2;
            requireActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
            //String Item = getActivity().getIntent().getExtras().getString("name");
            //getContext().getView().findViewById(R.id.nmLayout).setVisibility(View.VISIBLE);


        });
        view.findViewById(R.id.nmSwanBtn).setOnClickListener(x ->
        {
            if(!sharedPref.getBoolean("muted", false))
            {
                MediaPlayer mediaPlayer = MediaPlayer.create(getContext(), R.raw.btn_click_ef);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
            tNm1=nm1EditText.getText().toString();
            tNm2=nm2EditText.getText().toString();
            nm1EditText.setText(tNm2);
            nm2EditText.setText(tNm1);

        });
        return view;
    }


}