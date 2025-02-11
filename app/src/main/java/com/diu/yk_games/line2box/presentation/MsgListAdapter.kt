package com.diu.yk_games.line2box.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.CustomMsgListViewBinding
import com.diu.yk_games.line2box.model.MsgStore
import com.diu.yk_games.line2box.util.toDateTime

class MsgListAdapter : ListAdapter<MsgStore, MsgListAdapter.ViewHolder>(MsgStoreDiffCallback()) {

    var onClickListener: ((MsgStore) -> Unit)? = null
    var onLongClickListener: ((MsgStore) -> Boolean)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(CustomMsgListViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: CustomMsgListViewBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MsgStore) {
            binding.apply {
                // Set time data
                timeShowId.text = if (item.time != 0L) item.time.toDateTime() else item.timeData

                // Set name and level data
                nmId.text = item.nmData
                lvlId.text = item.lvlData

                // Set message data and color
                msgId.text = item.msgData
                val messageColor = when (item.msgData) {
                    "Created the match.", "Joined the match.", "Won the match." -> R.color.color_match_action
                    "Left the match." -> R.color.color_left_match
                    else -> R.color.whiteY
                }
                msgId.setTextColor(ContextCompat.getColor(itemView.context, messageColor))
                root.setOnClickListener {
                    onClickListener?.invoke(item)
                }
                root.setOnLongClickListener {
                    onLongClickListener?.invoke(item) ?: false
                }
            }
        }
    }

    class MsgStoreDiffCallback : DiffUtil.ItemCallback<MsgStore>() {
        override fun areItemsTheSame(oldItem: MsgStore, newItem: MsgStore) = oldItem.time == newItem.time && oldItem.msgData == newItem.msgData
        override fun areContentsTheSame(oldItem: MsgStore, newItem: MsgStore)= oldItem == newItem
    }
}