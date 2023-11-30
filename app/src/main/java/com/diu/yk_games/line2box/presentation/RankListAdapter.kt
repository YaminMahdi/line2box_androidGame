package com.diu.yk_games.line2box.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.GameProfile

class RankListAdapter(
    context: Context,
    private val rankList: List<GameProfile>,
    private var playerId: String
) : ArrayAdapter<GameProfile>(context, 0, rankList) {
    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convView: View?, parent: ViewGroup): View {
        var convertView = convView
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                .inflate(R.layout.custom_rank_list_view, parent, false)
        }
        if (rankList[position].playerId == playerId) {
            myPosition = position
            convertView!!.findViewById<View>(R.id.rankListItemBg)
                .setBackgroundResource(R.drawable.box_chat_fill)
        } else {
            convertView!!.findViewById<View>(R.id.rankListItemBg)
                .setBackgroundResource(R.drawable.btn_rank_bg)
        }
        val serialNo = convertView.findViewById<TextView>(R.id.serialId)
        val nmData = convertView.findViewById<TextView>(R.id.nmId)
        val coinData = convertView.findViewById<TextView>(R.id.coinId)
        serialNo.text = (position + 1).toString() + "."
        nmData.text = rankList[position].nm.split("\n")[0]
        coinData.text = "" + rankList[position].coin
        (convertView.findViewById<View>(R.id.lvlId) as TextView).text =
            "" + rankList[position].lvl
        return convertView
    }

    companion object {
        var myPosition = 0
    }
}