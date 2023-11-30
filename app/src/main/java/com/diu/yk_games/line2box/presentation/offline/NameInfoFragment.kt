package com.diu.yk_games.line2box.presentation.offline

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.model.GameProfile.Companion.setPreferences
import com.diu.yk_games.line2box.util.setBounceClickListener

class NameInfoFragment : Fragment() {
    private lateinit var nm1EditText: EditText
    private lateinit var nm2EditText: EditText
    private var nm1 = "Red"
    private var nm2 = "Blue"
    private var tNm1: String? = null
    private var tNm2: String? = null
    override fun onDetach() {
        super.onDetach()
        (requireActivity() as GameActivity1).onStopFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_nminfo, container, false)
        val sharedPref = requireContext().getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE
        )
        val prefEditor = sharedPref.edit()
        setPreferences(sharedPref)
        val nmSave = view.findViewById<CheckBox>(R.id.nmSaveBox)
        nm1EditText = view.findViewById(R.id.palyerRed)
        nm2EditText = view.findViewById(R.id.palyerBlue)

        //nm1EditText.setEnabled(false);
        val tmpNm1 = sharedPref.getString("plrNm1", "")
        val tmpNm2 = sharedPref.getString("plrNm2", "")
        val prfNm = GameProfile().nm
        if (tmpNm1 != "") nm1EditText.setText(tmpNm1) else nm1EditText.setText(prfNm)
        if (tmpNm2 != "") nm2EditText.setText(tmpNm2)

        //nmEditText.setSelection(nmEditText.getText().length());

        //nm1=new GameProfile().nm;

        //new ArrayAdapter<String>()
        val btn = view.findViewById<Button>(R.id.playBtn)
        btn.setBounceClickListener {
            if (!sharedPref.getBoolean("muted", false)) {
                val mediaPlayer = MediaPlayer.create(context, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
            }
            if (nm1EditText.text.toString() != "") {
                nm1 = nm1EditText.text.toString()
            }
            if (nm2EditText.text.toString() != "") {
                nm2 = nm2EditText.text.toString()
            }
            if (nmSave.isChecked) {
                if (nm1EditText.text.toString() != "") prefEditor.putString("plrNm1", nm1)
                    .apply()
                if (nm2EditText.text.toString() != "") prefEditor.putString("plrNm2", nm2)
                    .apply()
            }
            GameActivity1.nm1 = nm1
            GameActivity1.nm2 = nm2
            requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
        }
        view.findViewById<View>(R.id.nmSwanBtn).setOnClickListener {
            if (!sharedPref.getBoolean("muted", false)) {
                val mediaPlayer = MediaPlayer.create(context, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
            }
            tNm1 = nm1EditText.text.toString()
            tNm2 = nm2EditText.text.toString()
            nm1EditText.setText(tNm2)
            nm2EditText.setText(tNm1)
        }
        return view
    }
}