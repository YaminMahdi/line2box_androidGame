package com.diu.yk_games.line2box.presentation.online.stats

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogLayoutProfileBinding
import com.diu.yk_games.line2box.databinding.FragmentDisplayBinding
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.presentation.adapter.ScoreListAdapter
import com.diu.yk_games.line2box.presentation.base.BaseFragment
import com.diu.yk_games.line2box.presentation.component.showProfileDialog
import com.diu.yk_games.line2box.presentation.main.SettingsFragment
import com.diu.yk_games.line2box.util.*

class ScoreBoardFragment : BaseFragment<FragmentDisplayBinding>(FragmentDisplayBinding::inflate) {
    private val scoreListAdapter by lazy { ScoreListAdapter() }

    companion object {
        private const val TAG = "ScoreBoardFragment"
    }

    private fun setupUI() {
        binding.fragLabel.text = getString(R.string.global_score_board)
        binding.statusLabel.text = getString(R.string.last_best_score)
        binding.recyclerView.adapter = scoreListAdapter
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        viewModel.fetchScoreBoard()
        viewModel.scoreboard.collectWithLifecycle {
            binding.loader.gone()
            Log.d(TAG, "isSuccessful: ${it.list.size}")
            scoreListAdapter.submitList(it.list)
            binding.status.text = "\uD83D\uDC51 ${it.lastBest}"
        }

        var itemClicked = false
        binding.homeRow.btnSetting.setBounceClickListener {
            viewModel.player.playButtonClickSound()
            SettingsFragment.show(childFragmentManager)
        }
        binding.homeRow.btnHome.setBounceClickListener {
            viewModel.player.playButtonClickSound()
            onBackPressed()
        }
        scoreListAdapter.onPlayerClick = playerClick@{ playerId ->
            if (playerId.isEmpty() || itemClicked) return@playerClick
            itemClicked = true
            viewModel.player.playButtonClickSound()
            viewModel.firestore.collection("gamerProfile").document(playerId)
                .get()
                .addOnSuccessListener { doc ->
                    val profile = doc.toObjectOrNull<GameProfile>()
                    if (profile == null) {
                        itemClicked = false
                        return@addOnSuccessListener
                    }
                    showProfileDialog(
                        context = parentActivity,
                        profile = profile,
                        onCopy = { context.setClipBoardData(profile.toString(), "Copied!") },
                        onDismiss = { itemClicked = false }
                    )
                }
                .addOnFailureListener {
                    itemClicked = false
                }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showPlayerProfile(
        profile: GameProfile,
        marginLeft: Int,
        onDismissed: () -> Unit
    ) {
        val dBinding = DialogLayoutProfileBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(parentActivity)
            .setView(dBinding.root)
            .create()
        alertDialog.setOnDismissListener {
            onDismissed()
        }
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(marginLeft, 0, 60, 0)
        dBinding.linearLayoutFrame.apply {
            layoutParams = params
            backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cocX))
            backgroundTintMode = ADD
        }
        dBinding.apply {
            if (profile.countryNm != "")
                countryTxt.text = profile.countryNm + " " + profile.countryEmoji
            else
                countryLayout.gone()
            lvlTxt.text = "" + profile.lvl
            coinHave.text = "" + profile.coin
            matchPlayedTxt.text = "" + profile.matchPlayed
            matchWonTxt.text = "" + profile.matchWinMulti
            nmTxt.isEnabled = false
            nmTxt.setText(profile.nm)
            profileTitle.textSize = 28f
            profileShapeLayout.gone()
            nmEditBtn.gone()
            nmLTxt.gone()
            countryLTxt.gone()
            buttonSaveInfo.gone()
        }
        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        dBinding.root.setOnClickListener {
            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
        }
        runCatching { alertDialog.show() }
    }
}