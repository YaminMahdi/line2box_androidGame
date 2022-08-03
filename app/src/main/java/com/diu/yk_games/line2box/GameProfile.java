package com.diu.yk_games.line2box;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Random;

public class GameProfile
{
    public static SharedPreferences preferences;
    public static SharedPreferences.Editor preferencesEditor;
    public String nm=preferences.getString("nm", "noob"+(int)Math.floor(Math.random()*(999-100+1)+100));
    public Integer matchWinAI=preferences.getInt("matchWinAI",0);
    public Integer matchWinMulti=preferences.getInt("matchWinMulti",0);
    public Integer coin=preferences.getInt("coin",0);
    public Integer lvl=preferences.getInt("lvl",getLvl());
    //preferences.getBoolean("needProfile",true)
    public static void setPreferences(SharedPreferences x)
    {
        preferences=x;
        preferencesEditor = preferences.edit();
    }

    public GameProfile() {}

    public GameProfile(String nm, Integer coin, Integer matchWinAI, Integer matchWinMulti) {
        this.nm = nm;
        this.coin = coin;
        this.matchWinAI = matchWinAI;
        this.matchWinMulti = matchWinMulti;

        preferencesEditor.putString("nm",nm).apply();
        preferencesEditor.putInt("coin",coin).apply();
        preferencesEditor.putInt("lvl",lvl).apply();
        preferencesEditor.putInt("matchWinAI",matchWinAI).apply();
        preferencesEditor.putInt("matchWinMulti",matchWinMulti).apply();
    }
    public void apply()
    {
        preferencesEditor.putString("nm",this.nm).apply();
        preferencesEditor.putInt("coin",this.coin).apply();
        preferencesEditor.putInt("lvl",this.lvl).apply();
        preferencesEditor.putInt("matchWinAI",this.matchWinAI).apply();
        preferencesEditor.putInt("matchWinMulti",this.matchWinMulti).apply();

    }
    public void setNm(String nm) {
        this.nm = nm;
    }
    public void setCoin(Integer coin) {
        this.coin = coin;
    }

    private Integer getLvl()
    {
        int mul=matchWinMulti+1;
        int ai=matchWinAI+1;
        int lv= (int) Math.sqrt(mul*(mul/7.0)+ai*2);
        return lv;
    }
    public void setMatchWinAI() {
        this.matchWinAI++;
    }
    public void setMatchWinMulti() {
        this.matchWinMulti++;
    }
}
