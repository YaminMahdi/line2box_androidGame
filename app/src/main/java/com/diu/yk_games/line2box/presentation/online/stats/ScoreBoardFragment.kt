package com.diu.yk_games.line2box.presentation.online.stats

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.viewpager2.widget.ViewPager2
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.FragmentScoreBoardBinding
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.model.Score
import com.diu.yk_games.line2box.presentation.adapter.ScoreListAdapter
import com.diu.yk_games.line2box.presentation.adapter.ViewPagerAdapter
import com.diu.yk_games.line2box.presentation.base.BaseFragment
import com.diu.yk_games.line2box.presentation.component.showProfileDialog
import com.diu.yk_games.line2box.presentation.main.SettingsFragment
import com.diu.yk_games.line2box.presentation.online.stats.component.AnimatedTabRow
import com.diu.yk_games.line2box.presentation.online.stats.component.RecyclerViewFragment
import com.diu.yk_games.line2box.ui.theme.Line2BoxTheme
import com.diu.yk_games.line2box.util.*
import kotlinx.collections.immutable.toPersistentList

class ScoreBoardFragment :
    BaseFragment<FragmentScoreBoardBinding>(FragmentScoreBoardBinding::inflate) {
    private val scoreFriendlyAdapter by lazy { ScoreListAdapter() }
    private val scoreGlobalAdapter by lazy { ScoreListAdapter() }
    var selectedTabIndex by mutableIntStateOf(0)

    companion object {
        private const val TAG = "ScoreBoardFragment"
    }

    private fun setupUI() {
        binding.fragLabel.text = getString(R.string.global_score_board)
        binding.statusLabel.text = getString(R.string.last_best_score)
        val pager = ViewPagerAdapter(
            listOf(
                RecyclerViewFragment.newInstance(scoreFriendlyAdapter),
                RecyclerViewFragment.newInstance(scoreGlobalAdapter)
            ), parentActivity
        )
        binding.scorePager.adapter = pager
        binding.scorePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.loader.gone()
                selectedTabIndex = position
                when (position) {
                    0 -> viewModel.fetchScoreBoard(Score.Type.Friendly)
                    1 -> viewModel.fetchScoreBoard(Score.Type.Globe)
                }
            }
        })
        binding.composeView.apply {
            setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                Line2BoxTheme {
                    val tabs = remember { Score.Type.entries.toPersistentList() }
                    AnimatedTabRow(
                        tabs = tabs,
                        selectedIndex = selectedTabIndex,
                        onTabSelected = { index ->
                            binding.scorePager.currentItem = index
                        }
                    )
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        viewModel.scoreboard.collectWithLifecycle {
            binding.loader.changeVisibility(it.isLoading)
            Log.d(TAG, "isSuccessful: ${it.friendlyMatches.size}")
            scoreFriendlyAdapter.submitList(it.friendlyMatches)
            scoreGlobalAdapter.submitList(it.globalMatches)
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
        scoreGlobalAdapter.onPlayerClick = playerClick@{ playerId ->
            if (playerId.isEmpty() || itemClicked) return@playerClick
            itemClicked = true
            viewModel.player.playButtonClickSound()
            viewModel.gamerProfileRef.document(playerId)
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
}