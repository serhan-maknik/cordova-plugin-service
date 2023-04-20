package cordova.plugin.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;


import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import static android.content.Context.ALARM_SERVICE;

/**
 * This class echoes a string called from JavaScript.
 */
public class BackgroundService extends CordovaPlugin {

    static Context context;
    static protected Resources resources;
    static protected String packagename;

    private ServiceTracker pref;


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        pref = new cordova.plugin.service.ServiceTracker(cordova.getActivity());

        if (action.equals("startService")) {
             String message = args.getString(0);
            actionOnService(Actions.START);
            Log.d("SERSER","message: "+message);
            this.coolMethod(message, callbackContext);
            return true;
        }
        else if(action.equals("stopService")){
            actionOnService(Actions.STOP);

            this.coolMethod("message", callbackContext);
            return true;
        }
        return false;
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private void actionOnService(Actions action) {

        if(pref.getServiceState() == ServiceState.STOPPED  && action == Actions.STOP){
            return;
        }
        Intent i = new Intent(cordova.getContext(),EndlessService.class);
        i.setAction(action.name());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            cordova.getContext().startForegroundService(i);
            return;
        }
        cordova.getContext().startService(i);
    }

    static protected int getResourceId(String type,String name){
        return resources.getIdentifier(name,type,BackgroundService.packagename);
    }

}