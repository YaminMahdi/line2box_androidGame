package com.diu.yk_games.line2box.presentation.offline

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.databinding.FragmentNminfoBinding
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.presentation.base.BaseFragment
import com.diu.yk_games.line2box.presentation.main.SettingsFragment
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.util.*
import kotlinx.coroutines.launch

class NameInfoFragment : BaseFragment<FragmentNminfoBinding>(FragmentNminfoBinding::inflate) {
    private var nm1 = "Red"
    private var nm2 = "Blue"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListener()
    }

    private fun setupUI() {
        lifecycleScope.launch {
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
        binding.settingBtn.setBounceClickListener {
            viewModel.player.playButtonClickSound()
            SettingsFragment.show(childFragmentManager)
        }
        binding.backBtn.setBounceClickListener {
            viewModel.player.playButtonClickSound()
            onBackPressed()
        }
        binding.playBtn.setBounceClickListener {
            viewModel.player.playButtonClickSound()
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
            viewModel.player.playButtonClickSound()
            binding.palyerRed.text.toString().let {
                binding.palyerRed.setText(binding.palyerBlue.text.toString())
                binding.palyerBlue.setText(it)
            }
        }
    }
}