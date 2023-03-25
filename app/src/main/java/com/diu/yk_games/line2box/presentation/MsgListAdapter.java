package com.diu.yk_games.line2box.presentation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.diu.yk_games.line2box.R;
import com.diu.yk_games.line2box.model.MsgStore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class MsgListAdapter extends ArrayAdapter<MsgStore> {

    private final Activity context;
    private final ArrayList<MsgStore> ms;

    public MsgListAdapter(Activity context, ArrayList<MsgStore> ms) {
        //super(context, R.layout.custom_list_view);
        super(context, 0, ms);
        // TODO Auto-generated constructor stub
        this.context=context;
        this.ms = ms;

    }
    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position,View convertView,ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.custom_msg_list_view, parent, false);
        }


        TextView timeData = convertView.findViewById(R.id.timeShowId);
        TextView nmData = convertView.findViewById(R.id.nmId);
        TextView msgData = convertView.findViewById(R.id.msgId);
        TextView lvlData = convertView.findViewById(R.id.lvlId);

        String[] timeServer=ms.get(position).timeData.split(",",2);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM");
        LocalDateTime now = LocalDateTime.now();
        String timeNow = dtf.format(now);
        if(timeNow.contains(timeServer[0])||timeServer[0].contains(timeNow))
            timeData.setText(timeServer[1]);
        else
            timeData.setText(ms.get(position).timeData);

        nmData.setText(ms.get(position).nmData);
        lvlData.setText(ms.get(position).lvlData);
        String data=ms.get(position).msgData;
        msgData.setText(data);
        if(data.equals("Created the match.")||data.equals("Joined the match.")||data.equals("Won the match."))
            msgData.setTextColor(0xFF60c235);
        else if(data.equals("Left the match."))
            msgData.setTextColor(0xFFDE2D45);
        else
            msgData.setTextColor(0xFFD9D9D9);
        return convertView;

    }
}
