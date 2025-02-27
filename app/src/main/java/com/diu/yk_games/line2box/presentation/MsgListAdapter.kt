package com.diu.yk_games.line2box.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.CustomInvitationListViewBinding
import com.diu.yk_games.line2box.databinding.CustomMsgListViewBinding
import com.diu.yk_games.line2box.model.MsgStore
import com.diu.yk_games.line2box.model.typeEnum
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.diu.yk_games.line2box.util.setClipBoardData
import com.diu.yk_games.line2box.util.toDateTime
import com.diu.yk_games.line2box.util.toast

class MsgListAdapter(private val playerId: String) : ListAdapter<MsgStore, RecyclerView.ViewHolder>(MsgStoreDiffCallback()) {

    var onClickListener: ((MsgStore) -> Unit)? = null
    var onLongClickListener: ((MsgStore) -> Boolean)? = null
    var onJoinClickListener: ((MsgStore) -> Unit)? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) TextViewHolder(
            CustomMsgListViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        else InvitationViewHolder(
            CustomInvitationListViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        when(holder){
            is TextViewHolder -> holder.bind(item)
            is InvitationViewHolder -> holder.bind(item)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position)?.type == MsgStore.Type.Invitation.name) 1 else 0
    }

    inner class InvitationViewHolder(private val binding: CustomInvitationListViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MsgStore){
            binding.apply {
                timeShowId.text = item.time.toDateTime()
                nmId.text = item.nmData
                lvlId.text = item.lvlData
                msgId.text = item.msgData.split("Match ID").firstOrNull()?.trim() ?: item.msgData
                gameId.text = item.gameId
                if(item.playerId == playerId)
                    btnJoin.isEnabled = false
                btnJoin.setBounceClickListener {
                    if(item.gameId.isNotEmpty())
                        onJoinClickListener?.invoke(item)
                    else
                        root.context.toast("Invalid ID")
                }
                btnCopy.setBounceClickListener {
                    root.context.setClipBoardData(item.gameId, "ID copied")
                }
                root.setOnClickListener {
                    onClickListener?.invoke(item)
                }
                root.setOnLongClickListener {
                    onLongClickListener?.invoke(item) == true
                }
            }
        }
    }

    inner class TextViewHolder(private val binding: CustomMsgListViewBinding) : RecyclerView.ViewHolder(binding.root) {

        private val enter = ContextCompat.getColor(binding.root.context, R.color.color_match_action)
        private val exit = ContextCompat.getColor(binding.root.context, R.color.color_left_match)
        private val white = ContextCompat.getColor(binding.root.context, R.color.whiteY)

        fun bind(item: MsgStore) {
            binding.apply {
                // Set time data
                timeShowId.text = item.time.toDateTime()

                // Set name and level data
                nmId.text = item.nmData
                lvlId.text = item.lvlData

                // Set message data and color
                msgId.text = item.msgData
                val messageColor = when (item.type.typeEnum) {
                    MsgStore.Type.EnterText -> enter
                    MsgStore.Type.ExitText -> exit
                    else -> white
                }
                msgId.setTextColor(messageColor)
                root.setOnClickListener {
                    onClickListener?.invoke(item)
                }
                root.setOnLongClickListener {
                    onLongClickListener?.invoke(item) == true
                }
            }
        }
    }

    class MsgStoreDiffCallback : DiffUtil.ItemCallback<MsgStore>() {
        override fun areItemsTheSame(oldItem: MsgStore, newItem: MsgStore) = oldItem.key == newItem.key

        override fun areContentsTheSame(oldItem: MsgStore, newItem: MsgStore) = oldItem.key == newItem.key
    }
}