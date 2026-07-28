package com.diu.yk_games.line2box.presentation.offline

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.FragmentNminfoBinding
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.util.*
import kotlinx.coroutines.launch

class NameInfoFragment : Fragment() {
    lateinit var binding: FragmentNminfoBinding
    lateinit var parentActivity: FragmentActivity

    private var nm1 = "Red"
    private var nm2 = "Blue"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNminfoBinding.inflate(inflater, container, false)
        parentActivity = requireActivity()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListener()
    }

    private fun setupUI() {
        lifecycleScope.launch {
            binding.volBtn.applyState(isMuted())
            binding.apply {
                val tmpNm1 = IO { pref.read("plrNm1", "") }
                val tmpNm2 = IO { pref.read("plrNm2", "") }
                val prfNm = IO { GameProfile().nm }
                palyerRed.setText(if (tmpNm1 != "") tmpNm1 else prfNm)
                if (tmpNm2 != "") palyerBlue.setText(tmpNm2)
            }
        }
    }

    fun setupListener() {
        binding.volBtn.performOnClickF()
        binding.backBtn.setBounceClickListener {
            isNotMuted {
                val mediaPlayer = MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            onBackPressed()
        }
        binding.playBtn.setBounceClickListener {
            if (!pref.read("muted", false)) {
                val mediaPlayer = MediaPlayer.create(context, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            if (binding.palyerRed.text.toString() != "") nm1 = binding.palyerRed.text.toString()

            if (binding.palyerBlue.text.toString() != "") nm2 = binding.palyerBlue.text.toString()

            if (binding.nmSaveBox.isChecked) {
                if (binding.palyerRed.text.toString() != "")
                    pref.save("plrNm1", nm1)
                if (binding.palyerBlue.text.toString() != "")
                    pref.save("plrNm2", nm2)
            }
            navigateSafe(Routes.GameDual(nm1.trim(), nm2.trim())) {
                popUpTo(Routes.ChangeName::class){
                    inclusive = true
                }
            }
        }
        binding.nmSwanBtn.setBounceClickListener {
            if (!pref.read("muted", false)) {
                val mediaPlayer = MediaPlayer.create(context, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            binding.palyerRed.text.toString().let {
                binding.palyerRed.setText(binding.palyerBlue.text.toString())
                binding.palyerBlue.setText(it)
            }
        }
    }
}