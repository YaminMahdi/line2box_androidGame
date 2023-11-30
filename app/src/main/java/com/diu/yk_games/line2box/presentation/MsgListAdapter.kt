package com.diu.yk_games.line2box.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.MsgStore
import com.diu.yk_games.line2box.util.toDateTime

class MsgListAdapter(
    context: Context,
    private val ms: List<MsgStore>
) : ArrayAdapter<MsgStore>(context, 0, ms) {
    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convView: View?, parent: ViewGroup): View {
        var convertView = convView
        if (convertView == null) {
            convertView =
                LayoutInflater.from(context).inflate(R.layout.custom_msg_list_view, parent, false)
        }
        val timeData = convertView!!.findViewById<TextView>(R.id.timeShowId)
        val nmData = convertView.findViewById<TextView>(R.id.nmId)
        val msgData = convertView.findViewById<TextView>(R.id.msgId)
        val lvlData = convertView.findViewById<TextView>(R.id.lvlId)
        timeData.text = if(ms[position].time != 0L) ms[position].time.toDateTime() else ms[position].timeData
        nmData.text = ms[position].nmData
        lvlData.text = ms[position].lvlData
        val data = ms[position].msgData
        msgData.text = data
        when (data) {
            "Created the match.", "Joined the match.", "Won the match." -> msgData.setTextColor(-0x9f3dcb)
            "Left the match." -> msgData.setTextColor(-0x21d2bb)
            else -> msgData.setTextColor(-0x262627)
        }
        return convertView
    }
}