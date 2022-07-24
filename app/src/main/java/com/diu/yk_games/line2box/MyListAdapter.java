package com.diu.yk_games.line2box;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

public class MyListAdapter extends ArrayAdapter<DataStore> {

    private final Activity context;
    private final ArrayList<DataStore> ds;

    public MyListAdapter(Activity context, ArrayList<DataStore> ds) {
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

        timeData.setText("Time:  "+ds.get(position).timeData+"                Rating  :"+ds.get(position).starData);
        redData.setText(ds.get(position).redData);
        blueData.setText(ds.get(position).blueData);

        return convertView;

    }
}
