package com.diu.yk_games.line2box;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Random;

public class GameProfile
{
    public static SharedPreferences preferences;
    public static SharedPreferences.Editor preferencesEditor;
    public String nm=preferences.getString("nm", "Noob"+(int)Math.floor(Math.random()*(900)+100));
    public Integer matchPlayed=preferences.getInt("matchPlayed",0);
    public Integer matchWinMulti=preferences.getInt("matchWinMulti",0);
    public Integer coin=preferences.getInt("coins",100);
    public Integer lvl=preferences.getInt("lvl",getLvlByCal());

    public String playerId;
    //preferences.getBoolean("needProfile",true)
    public static void setPreferences(SharedPreferences x)
    {
        preferences=x;
        preferencesEditor = preferences.edit();
    }

    public GameProfile() {}

    public GameProfile(String nm, Integer coin, Integer matchPlayed, Integer matchWinMulti) {
        this.nm = nm;
        this.coin = coin;
        this.matchPlayed = matchPlayed;
        this.matchWinMulti = matchWinMulti;
    }
    public void apply()
    {
        preferencesEditor.putString("nm",this.nm).apply();
        preferencesEditor.putInt("coins",this.coin).apply();
        preferencesEditor.putInt("matchPlayed",this.matchPlayed).apply();
        preferencesEditor.putInt("matchWinMulti",this.matchWinMulti).apply();

    }
    public void setNm(String nm) {
        this.nm = nm;
        //preferencesEditor.putString("nm",this.nm).apply();
    }
    public void setCoin(Integer coins) {
        this.coin = coins;
        //preferencesEditor.putInt("coins",this.coin).apply();
    }

    public Integer getLvlByCal()
    {
        int mul=matchWinMulti+1;
        int pld=matchPlayed+1;
        int lv= (int) Math.sqrt(mul*(mul/7.0)+pld*2);
        return lv;
    }
    public void setMatchPlayed() {
        this.matchPlayed++;
        //preferencesEditor.putInt("matchPlayed",this.matchPlayed).apply();
    }
    public void setMatchWinMulti() {
        this.matchWinMulti++;
        //preferencesEditor.putInt("matchWinMulti",this.matchWinMulti).apply();
    }
}
