package cordova.plugin.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import android.util.Log;

import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import cordova.plugin.service.ServiceTracker;
import  cordova.plugin.service.EndlessService;
import cordova.plugin.service.Actions;
import cordova.plugin.service.ServiceState;

public class StartReceiver extends BroadcastReceiver {
    Context context;
    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        ServiceTracker pref = new ServiceTracker(context);
        String params = pref.getParams();
        if (intent.getAction() == Intent.ACTION_BOOT_COMPLETED && pref.getServiceState() == ServiceState.STARTED) {
            startBackgroundService(Actions.START, params);
        }

    }

    public void startBackgroundService(Actions action,String params){
        JSONObject jObj = new JSONObject();
        try {
            jObj.put("action",action.name());
            jObj.put("params",params);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Data data = new Data.Builder()
                .putString("data", jObj.toString())
                .build();
        Log.d("WORKER", "startServiceViaWorker called");

        String UNIQUE_WORK_NAME = "StartServiceViaWorker";
        String WORKER_TAG = "ServiceWorkerTag";
        WorkManager workManager = WorkManager.getInstance(context);

        // As per Documentation: The minimum repeat interval that can be defined is 15 minutes (
        // same as the JobScheduler API), but in practice 15 doesn't work. Using 16 here
        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(
                        cordova.plugin.service.MyWorker.class,
                        16*60*1000,
                        TimeUnit.MILLISECONDS)
                        .setInputData(data)
                        .addTag(WORKER_TAG)
                        .build();
        // below method will schedule a new work, each time app is opened
        //workManager.enqueue(request);

        // to schedule a unique work, no matter how many times app is opened i.e. startServiceViaWorker gets called
        // https://developer.android.com/topic/libraries/architecture/workmanager/how-to/unique-work
        // do check for AutoStart permission
        workManager.enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request);

    }
}
