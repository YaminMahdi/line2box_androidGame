package com.diu.yk_games.line2box.presentation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.diu.yk_games.line2box.R;
import com.diu.yk_games.line2box.model.DataStore;

import java.util.ArrayList;

public class MyListAdapter extends ArrayAdapter<DataStore> {

    private final Context context;
    private final ArrayList<DataStore> ds;

    public MyListAdapter(Context context, ArrayList<DataStore> ds) {
        //super(context, R.layout.custom_list_view);
        super(context, 0, ds);
        // TODO Auto-generated constructor stub

        this.context=context;
        this.ds = ds;

    }
    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position,View convertView,ViewGroup parent) {
        //Collections.reverse(ds);
        //LayoutInflater inflater=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //View rowView=inflater.inflate(R.layout.custom_list_view, parent,false);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.custom_list_view, parent, false);
        }

        TextView timeData = convertView.findViewById(R.id.timeId);
        TextView redData = convertView.findViewById(R.id.redShow);
        TextView blueData = convertView.findViewById(R.id.blueShow);
        ImageView imV =convertView.findViewById(R.id.playFromImg);

        if(ds.get(position).starData.charAt(0)=='★')
        {
            timeData.setText("Time:  "+ds.get(position).timeData+"                Rating  :"+ds.get(position).starData);
            imV.setVisibility(View.INVISIBLE);
        }
        else
        {
            imV.setVisibility(View.VISIBLE);
            timeData.setText("Time:  "+ds.get(position).timeData);
            if(ds.get(position).starData.equals("globe"))
                imV.setImageResource(R.drawable.icon_globe);
            else
                imV.setImageResource(R.drawable.icon_friends);
        }
        redData.setText(ds.get(position).redData);
        blueData.setText(ds.get(position).blueData);

        return convertView;

    }
}
