package com.diu.yk_games.line2box.presentation

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.CustomListViewBinding
import com.diu.yk_games.line2box.model.DataStore
import com.diu.yk_games.line2box.util.toDateTime

class ScoreListAdapter : ListAdapter<DataStore, ScoreListAdapter.ViewHolder>(DataStoreDiffCallback()) {

    var onClickListener: ((DataStore) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(CustomListViewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: CustomListViewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: DataStore) {
            binding.apply {
                playFromImg.setImageResource(
                    if (item.starData == "globe") R.drawable.icon_globe
                    else R.drawable.icon_friends
                )
                timeId.text = "Time:  ${item.time.toDateTime()}"
                redShow.text = item.redData
                blueShow.text = item.blueData
                root.setOnClickListener {
                    onClickListener?.invoke(item)
                }

            }
        }
    }

    class DataStoreDiffCallback : DiffUtil.ItemCallback<DataStore>() {
        override fun areItemsTheSame(oldItem: DataStore, newItem: DataStore) = oldItem.time == newItem.time
        override fun areContentsTheSame(oldItem: DataStore, newItem: DataStore) = oldItem == newItem
    }
}