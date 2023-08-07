package cordova.plugin.service;

import android.content.Context;
import android.content.SharedPreferences;

public class CancelShakePref {
    public static final String  NAME = "SHAKE_STATUS";
    private static final String SHAKE_STATUS = "status";

    // Shared pref mode
    private static final int PRIVATE_MODE = 0;
    private Context _context;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;


    public CancelShakePref(Context context){
        this._context = context;
        pref = _context.getSharedPreferences(NAME, 0);
        editor = pref.edit();
    }
    public  void setShakeStatus( boolean status){
        editor.putBoolean(SHAKE_STATUS, status);
        // commit changes
        editor.commit();
    }

    public boolean getShakeStatus(){
        boolean value = pref.getBoolean(SHAKE_STATUS, false);
        return value;
    }
}
