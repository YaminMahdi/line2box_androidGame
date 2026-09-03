package com.diu.yk_games.line2box.presentation.online.stats

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.FragmentLeaderBoardBinding
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.presentation.adapter.RankListAdapter
import com.diu.yk_games.line2box.presentation.base.BaseFragment
import com.diu.yk_games.line2box.presentation.component.showProfileDialog
import com.diu.yk_games.line2box.presentation.main.SettingsFragment
import com.diu.yk_games.line2box.util.*

class LeaderBoardFragment : BaseFragment<FragmentLeaderBoardBinding>(FragmentLeaderBoardBinding::inflate) {
    private val rankListAdapter by lazy {
        RankListAdapter(viewModel.playerId)
    }

    private fun setupUI() {
        binding.fragLabel.text = getString(R.string.global_leader_board)
        binding.statusLabel.text = getString(R.string.total_player)
        binding.recyclerView.adapter = rankListAdapter
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        viewModel.fetchLeaderBoard()
        viewModel.leaderboard.collectWithLifecycle {
            binding.loader.gone()
            try {
                rankListAdapter.submitList(it.list)
                val pos = findIndex(it.list, viewModel.playerId)
                Log.d(TAG, "onComplete(pos): $pos playerId- ${viewModel.playerId}")
                if (pos > 5) binding.recyclerView.scrollToPosition(pos - 1)

                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 2000L
                    interpolator = DecelerateInterpolator()
                    addUpdateListener { animator ->
                        val progress = animator.animatedValue as Float
                        val current = (it.count * progress).toLong()
                        binding.status.text = "%,d".format(current)
                    }
                    start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        binding.homeRow.btnSetting.setBounceClickListener {
            viewModel.player.playButtonClickSound()
            SettingsFragment.show(childFragmentManager)
        }
        binding.homeRow.btnHome.setBounceClickListener {
            viewModel.player.playButtonClickSound()
            onBackPressed()
        }

        var itemClicked = false
        rankListAdapter.onClickListener = run@{ gamerPro ->
            if (itemClicked) return@run
            itemClicked = true
            viewModel.player.playButtonClickSound()
            viewModel.firestore.collection("gamerProfile").document(gamerPro.playerId)
                .get().addOnSuccessListener { documentSnapshot ->
                    val server2device = documentSnapshot.toObjectOrNull<GameProfile>()
                    server2device?.let {
                        showProfileDialog(
                            context = parentActivity,
                            profile = it,
                            onCopy = { context.setClipBoardData(it.toString(), "Copied!") },
                            onDismiss = { itemClicked = false }
                        )
                    }
                }
        }
    }

    private fun findIndex(list: List<GameProfile>, id: String?): Int {
        list.forEachIndexed { index, gameProfile ->
            if (gameProfile.playerId == id) return index
        }
        return -1
    }

    companion object {
        private const val TAG = "LeadBoardFrag"
    }
}