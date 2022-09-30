package com.diu.yk_games.line2box;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class DataStore
{
    public String timeData="";
    public String redData="Red";
    public String blueData="Blue";
    public String starData="";
    public String plr1Id="";
    public String plr2Id="";
    public String plr1Cup="";
    public String plr2Cup="";
    public DataStore(){}


    public DataStore(String timeData, String redData, String blueData, String starData, String plr1Id, String plr2Id, String plr1Cup, String plr2Cup) {
        this.timeData = timeData;
        this.redData = redData;
        this.blueData = blueData;
        this.starData = starData;
        this.plr1Id = plr1Id;
        this.plr2Id = plr2Id;
        this.plr1Cup = plr1Cup;
        this.plr2Cup = plr2Cup;
    }
}
