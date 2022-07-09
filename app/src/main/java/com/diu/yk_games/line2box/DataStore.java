package com.diu.yk_games.line2box;

import android.util.Log;

public class DataStore
{
    public String timeData="";
    public String redData="Red";
    public String blueData="Blue";
    public String starData="★";

    public DataStore(){}

    public DataStore(String timeData, String redData, String blueData, String starData) {
        this.timeData = timeData;
        this.redData = redData;
        this.blueData = blueData;
        this.starData = starData;
    }

}
