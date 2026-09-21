package com.diu.yk_games.line2box.presentation.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.diu.yk_games.line2box.databinding.ItemScoreFriendlyBinding
import com.diu.yk_games.line2box.databinding.ItemScoreGlobeBinding
import com.diu.yk_games.line2box.model.Score
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.diu.yk_games.line2box.util.toTimePassed

class ScoreListAdapter : ListAdapter<Score, RecyclerView.ViewHolder>(ScoreDiffCallback()) {

    var onClickListener: ((Score) -> Unit)? = null
    var onPlayerClick: ((playerId: String) -> Unit)? = null


    override fun getItemViewType(position: Int) = getItem(position).type.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == Score.Type.Globe.ordinal)
            GlobeViewHolder(ItemScoreGlobeBinding.inflate(inflater, parent, false))
        else
            FriendlyViewHolder(ItemScoreFriendlyBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is FriendlyViewHolder -> holder.bind(item)
            is GlobeViewHolder -> holder.bind(item)
        }
    }

    // Friendly: no player id/cup, just a plain score line
    inner class FriendlyViewHolder(
        private val binding: ItemScoreFriendlyBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: Score) {
            binding.apply {
                timeId.text = item.time.toTimePassed()
                redShow.text = "${item.player1.nm}: ${item.result.score1}"
                blueShow.text = "${item.player2.nm}: ${item.result.score2}"
                root.setBounceClickListener {
                    onClickListener?.invoke(item)
                }
            }
        }
    }

    // Globe: full player nm/lvl + cup + score, no more dialog needed
    inner class GlobeViewHolder(
        private val binding: ItemScoreGlobeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Score) {
            binding.apply {
                timeId.text = item.time.toTimePassed()

                plr1Nm.text = item.player1.nm
                plr1Lvl.text = item.player1.lvl.toString()
                plr1Cup.text = item.result.cup1
                plr1Score.text = item.result.score1.toString()

                plr2Nm.text = item.player2.nm
                plr2Lvl.text = item.player2.lvl.toString()
                plr2Cup.text = item.result.cup2
                plr2Score.text = item.result.score2.toString()

                root.setBounceClickListener {
                    onClickListener?.invoke(item)
                }

                linLayoutPlr1.setBounceClickListener {
                    if (item.player1.id.isNotEmpty())
                        onPlayerClick?.invoke(item.player1.id)
                }
                linLayoutPlr2.setBounceClickListener {
                    if (item.player2.id.isNotEmpty())
                        onPlayerClick?.invoke(item.player2.id)
                }
            }
        }
    }

    class ScoreDiffCallback : DiffUtil.ItemCallback<Score>() {
        override fun areItemsTheSame(oldItem: Score, newItem: Score) = oldItem.time == newItem.time
        override fun areContentsTheSame(oldItem: Score, newItem: Score) = oldItem == newItem
    }
}