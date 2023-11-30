package com.diu.yk_games.line2box.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.DataStore
import com.diu.yk_games.line2box.util.toDateTime

class MyListAdapter(
    context: Context,
    private val ds: List<DataStore>
) :
    ArrayAdapter<DataStore>(context, 0, ds) {
    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convView: View?, parent: ViewGroup): View {
        //Collections.reverse(ds);
        //LayoutInflater inflater=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //View rowView=inflater.inflate(R.layout.custom_list_view, parent,false);
        var convertView = convView
        if (convertView == null)
            convertView = LayoutInflater.from(context).inflate(R.layout.custom_list_view, parent, false)

        val timeData = convertView!!.findViewById<TextView>(R.id.timeId)
        val redData = convertView.findViewById<TextView>(R.id.redShow)
        val blueData = convertView.findViewById<TextView>(R.id.blueShow)
        val imV = convertView.findViewById<ImageView>(R.id.playFromImg)
        if (ds[position].starData == "globe")
            imV.setImageResource(R.drawable.icon_globe)
        else
            imV.setImageResource(R.drawable.icon_friends)
        timeData.text = "Time:  " + ds[position].time.toDateTime()
        redData.text = ds[position].redData
        blueData.text = ds[position].blueData

        return convertView
    }
}