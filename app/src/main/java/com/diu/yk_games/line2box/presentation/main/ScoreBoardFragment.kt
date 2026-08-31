package com.diu.yk_games.line2box.presentation.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogLayoutProfileBinding
import com.diu.yk_games.line2box.databinding.FragmentDisplayBinding
import com.diu.yk_games.line2box.model.DataStore
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.model.Score
import com.diu.yk_games.line2box.presentation.adapter.ScoreListAdapter
import com.diu.yk_games.line2box.presentation.base.BaseFragment
import com.diu.yk_games.line2box.util.gone
import com.diu.yk_games.line2box.util.onBackPressed
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.diu.yk_games.line2box.util.toObjectOrNull
import com.google.firebase.firestore.Query

class ScoreBoardFragment : BaseFragment<FragmentDisplayBinding>(FragmentDisplayBinding::inflate) {
    private var bestScore = "\n\n\nNetwork Error"

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
        viewModel.firestore.collection("LastBestPlayer").document("LastBestPlayer")
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    Log.d(TAG, "Cached document data: " + document.data)
                    bestScore = document.data?.get("info")?.toString().orEmpty()
                    binding.status.text = "\uD83D\uDC51 $bestScore"
                } else {
                    Log.d(TAG, "Cached get failed: ", task.exception)
                }
            }
        viewModel.firestore.collection("ScoreBoard")
            .orderBy("time", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnSuccessListener { task ->
                val dsList = task.documents.mapNotNull {
                    if (it.contains("starData"))
                        it.toObjectOrNull<DataStore>()?.toScore()
                    else
                        it.toObjectOrNull<Score>()
                }
                Log.d(TAG, "isSuccessful: ${dsList.size}")
                scoreListAdapter.submitList(dsList)
            }

        var itemClicked = false
        binding.btnBack.setBounceClickListener {
            viewModel.player.playButtonClickSound()
            onBackPressed()
        }
        scoreListAdapter.onPlayerClick = playerClick@{ playerId, isLeft ->
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
                    showPlayerProfile(
                        profile = profile,
                        marginLeft = if (isLeft) 60 else 420,
                        onDismissed = {
                            itemClicked = false
                        }
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
            backgroundTintMode = PorterDuff.Mode.ADD
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