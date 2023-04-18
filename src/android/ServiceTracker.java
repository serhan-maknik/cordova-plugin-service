package cordova.plugin.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

enum ServiceState {
    STARTED,
    STOPPED,
}
public class ServiceTracker {


    private static final String name = "MSERVICE_KEY";
    private static final String key = "MSERVICE_STATE";
    // Shared Preferences
    SharedPreferences pref;

    SharedPreferences.Editor editor;
    Context _context;

    // Shared pref mode
    int PRIVATE_MODE = 0;

    // Shared preferences file name
    private static final String PREF_NAME = "Service";

    private static final String IS_SAVE = "is_save";
    private static final String USER_MAIL = "userMail";

    public ServiceTracker(Context context) {

        this._context = context;
        pref = _context.getSharedPreferences(name, PRIVATE_MODE);
        editor = pref.edit();
    }
    public void setServiceState(ServiceState state){
        Log.d("SERSER","ServiceState: "+state);
        editor.putString(key, state.name());

        // commit changes
        editor.commit();
    }

    public ServiceState getServiceState(){
        String value = pref.getString(key,ServiceState.STOPPED.name());
        Log.d("SERSER","getServiceState value: "+value);
        return ServiceState.valueOf(value);
    }
}
