package cordova.plugin.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class PrefUtil {

    public static final String PREF_KEY_APP_AUTO_START = "AUTO_START";
    private static final String NAME = "STATUS";

    // Shared pref mode
    private static final int PRIVATE_MODE = 0;

    public static void writeBoolean(Context context, String key , boolean _default){

        SharedPreferences pref = context.getSharedPreferences(NAME, 0);
        SharedPreferences.Editor editor = pref.edit();

        editor.putBoolean(key,_default);
        editor.commit();
    }

    public static boolean getAutoStartStatus(Context context){
        SharedPreferences pref = context.getSharedPreferences(NAME, 0);
        boolean value = pref.getBoolean(PREF_KEY_APP_AUTO_START, false);
        Log.d("SERSER","getAutoStartStatus value: "+value);
        return value;
    }
}
