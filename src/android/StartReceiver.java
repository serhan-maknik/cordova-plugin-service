package cordova.plugin.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import android.util.Log;
import cordova.plugin.service.ServiceTracker;
import  cordova.plugin.service.EndlessService;
import cordova.plugin.service.Actions;
import cordova.plugin.service.ServiceState;

public class StartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ServiceTracker pref = new ServiceTracker(context);

        if (intent.getAction() == Intent.ACTION_BOOT_COMPLETED && pref.getServiceState() == ServiceState.STARTED) {
            Intent i = new Intent(context, EndlessService.class);
            i.setAction(Actions.START.name());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d("StartReceiver","Starting the service in >=26 Mode from a BroadcastReceiver");
                context.startForegroundService(i);
                return;
            }
            context.startService(i);
        }

    }
}
