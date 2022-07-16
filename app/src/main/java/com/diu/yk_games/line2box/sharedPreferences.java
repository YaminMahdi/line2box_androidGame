//package com.diu.yk_games.line2box;
//
//import android.content.Context;
//import android.content.SharedPreferences;
//
//public class sharedPreferences
//{
//    boolean mute=false;
//    preferences = PreferenceManager.getDefaultSharedPreferences(context);
//    preferencesEditor = preferences.edit();
//    public static boolean isFirstRun(String forWhat)
//    {
//        if (preferences.getBoolean(forWhat, true))
//        {
//            preferencesEditor.putBoolean(forWhat, false).commit();
//            return true;
//        } else {
//            return false;
//        }
//    }
//    public boolean getData()
//    {
//        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
//        int defaultValue = getResources().getInteger(R.integer.saved_high_score_default_key);
//        int highScore = sharedPref.getInt(getString(R.string.saved_high_score_key), defaultValue);
//    }
//
//    public boolean getData(boolean mute)
//    {
//        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPref.edit();
//        editor.putInt(getString(R.string.saved_high_score_key), newHighScore);
//        editor.apply();
//
//    }
//
//}
