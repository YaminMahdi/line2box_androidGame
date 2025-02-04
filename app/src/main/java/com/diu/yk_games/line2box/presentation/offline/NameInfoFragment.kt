package com.diu.yk_games.line2box.presentation.offline

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.FragmentNminfoBinding
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.pref
import com.diu.yk_games.line2box.prefEditor
import com.diu.yk_games.line2box.util.setBounceClickListener

class NameInfoFragment : Fragment() {

    lateinit var binding: FragmentNminfoBinding

    private var nm1 = "Red"
    private var nm2 = "Blue"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNminfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            val tmpNm1 = pref.getString("plrNm1", "")
            val tmpNm2 = pref.getString("plrNm2", "")
            val prfNm = GameProfile().nm
            palyerRed.setText(if (tmpNm1 != "") tmpNm1 else prfNm)
            if (tmpNm2 != "") palyerBlue.setText(tmpNm2)
            playBtn.setBounceClickListener {
                if (!pref.getBoolean("muted", false)) {
                    val mediaPlayer = MediaPlayer.create(context, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
                }
                if (binding.palyerRed.text.toString() != "") nm1 = palyerRed.text.toString()

                if (binding.palyerBlue.text.toString() != "") nm2 = palyerBlue.text.toString()

                if (binding.nmSaveBox.isChecked) {
                    if (binding.palyerRed.text.toString() != "") prefEditor.putString("plrNm1", nm1)
                        .apply()
                    if (binding.palyerBlue.text.toString() != "") prefEditor.putString(
                        "plrNm2",
                        nm2
                    )
                        .apply()
                }
                GameActivity1.nm1 = nm1
                GameActivity1.nm2 = nm2
                activity?.supportFragmentManager?.beginTransaction()?.remove(this@NameInfoFragment)
                    ?.commit()
            }
            nmSwanBtn.setBounceClickListener {
                if (!pref.getBoolean("muted", false)) {
                    val mediaPlayer = MediaPlayer.create(context, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
                }
                palyerRed.setText(binding.palyerBlue.text.toString())
                palyerBlue.setText(binding.palyerRed.text.toString())
            }
        }

    }
}