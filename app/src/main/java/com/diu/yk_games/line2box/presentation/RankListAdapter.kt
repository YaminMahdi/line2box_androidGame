package com.diu.yk_games.line2box.presentation

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.CustomRankListViewBinding
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.util.setBounceClickListener

class RankListAdapter(
    private var playerId: String
) : ListAdapter<GameProfile, RankListAdapter.ViewHolder>(GameProfileDiffCallback()) {

    var onClickListener: ((GameProfile) -> Unit)? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(CustomRankListViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(
        private val binding: CustomRankListViewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: GameProfile, position: Int) {
            binding.apply {
                // Highlight current player's item
                binding.root.setBackgroundResource(
                    if (item.playerId == playerId) R.drawable.box_chat_fill
                    else R.drawable.btn_rank_bg
                )

                serialId.text = "${position + 1}."
                nmId.text = item.nm.split("\n")[0]
                coinId.text = item.coin.toString()
                lvlId.text = item.lvl.toString()
                root.setBounceClickListener {
                    if (item.playerId.isNotEmpty())
                        onClickListener?.invoke(item)
                }
            }
        }
    }

    class GameProfileDiffCallback : DiffUtil.ItemCallback<GameProfile>() {
        override fun areItemsTheSame(oldItem: GameProfile, newItem: GameProfile) = oldItem.playerId == newItem.playerId
        override fun areContentsTheSame(oldItem: GameProfile, newItem: GameProfile) = oldItem.playerId == newItem.playerId
    }
}