package cordova.plugin.service;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;
import org.json.JSONObject;

import cordova.plugin.service.EndlessService;

public class MyWorker extends Worker {
    private final Context context;
    private String TAG = "MyWorker";


    public MyWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @NonNull
    @Override
    public Result doWork() {
       
        String data = getInputData().getString("data");
        String action = "";
        String params = null;
        try {
            JSONObject jObj = new JSONObject(data);
            action = jObj.getString("action");
            params = jObj.getString("params");
        } catch (JSONException e) {
            e.printStackTrace();
        }

            Log.d(TAG, "starting service from doWork");
            /*
             * startForegroundService is similar to startService but with an implicit promise
             * that the service will call startForeground once it begins running.
             * The service is given an amount of time comparable to the ANR interval to do this,
             * otherwise the system will automatically stop the service and declare the app ANR.
             */
            Intent i = new Intent(context, EndlessService.class);
            i.setAction(action);
            i.putExtra("params",params);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context,i);
            }else{
                this.context.startService(i);
            }

        return Result.success();
    }

    @Override
    public void onStopped() {
        Log.d(TAG, "onStopped called for: " + this.getId());
        super.onStopped();
    }
}
