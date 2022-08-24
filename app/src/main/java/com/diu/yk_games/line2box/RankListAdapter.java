package com.diu.yk_games.line2box;

import static androidx.constraintlayout.widget.ConstraintLayout.*;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;

public class RankListAdapter extends ArrayAdapter<GameProfile>
{
    private final Activity context;
    private final ArrayList<GameProfile> rankList;
    String playerId;

    public RankListAdapter(Activity context, ArrayList<GameProfile> rankList, String playerId)
    {
        //super(context, R.layout.custom_list_view);
        super(context, 0, rankList);
        // TODO Auto-generated constructor stub

        this.context=context;
        this.rankList = rankList;
        this.playerId=playerId;

    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.custom_rank_list_view, parent, false);
        }
        if(rankList.get(position).playerId.equals(playerId))
        {
            convertView.findViewById(R.id.rankListItemBg).setBackgroundResource(R.drawable.box_chat_fill);
        }
        else
        {
            convertView.findViewById(R.id.rankListItemBg).setBackgroundResource(R.drawable.box_chat);

        }

        TextView serialNo = convertView.findViewById(R.id.serialId);
        TextView nmData = convertView.findViewById(R.id.nmId);
        TextView coinData = convertView.findViewById(R.id.coinId);

        serialNo.setText(position+1+".");
        nmData.setText(rankList.get(position).nm);
        coinData.setText(""+rankList.get(position).coin);

        return convertView;

    }
}
