package com.diu.yk_games.line2box.presentation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.diu.yk_games.line2box.R;
import com.diu.yk_games.line2box.model.GameProfile;

import java.util.ArrayList;

public class RankListAdapter extends ArrayAdapter<GameProfile>
{
    private final Context context;
    private final ArrayList<GameProfile> rankList;
    public static int myPosition=0;
    String playerId;

    public RankListAdapter(Context context, ArrayList<GameProfile> rankList, String playerId)
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
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.custom_rank_list_view, parent, false);
        }
        if(rankList.get(position).playerId.equals(playerId))
        {
            myPosition=position;
            convertView.findViewById(R.id.rankListItemBg).setBackgroundResource(R.drawable.box_chat_fill);

        }
        else
        {
            convertView.findViewById(R.id.rankListItemBg).setBackgroundResource(R.drawable.btn_rank_bg);

        }

        TextView serialNo = convertView.findViewById(R.id.serialId);
        TextView nmData = convertView.findViewById(R.id.nmId);
        TextView coinData = convertView.findViewById(R.id.coinId);

        serialNo.setText(position+1+".");
        nmData.setText(rankList.get(position).nm);
        coinData.setText(""+rankList.get(position).coin);
        ((TextView)convertView.findViewById(R.id.lvlId)).setText(""+rankList.get(position).lvl);

        return convertView;

    }
}
