package cordova.plugin.service;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;


import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.POWER_SERVICE;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import cordova.plugin.service.CancelShakeDialog;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import cordova.plugin.service.DefaultString;
import cordova.plugin.service.Actions;
import cordova.plugin.service.ServiceState;
import cordova.plugin.service.AutoStartHelper;
/**
 * This class echoes a string called from JavaScript.
 */
public class BackgroundService extends CordovaPlugin {

    private static Context context;
    private static final int REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = 101;
    private static final int REQUEST_CHECK_SETTINGS = 102;
    private static final int LOCATION_PERMISSION_CODE = 103;
    private static final int BACKGROUND_LOCATION_PERMISSION_CODE = 104;
    private static final int GPS_ENABLE = 105;
    private static final int GPS_ENABLED_MANUALLY = 106;
    private static final int XIAOMI_AUTO_START = 505;
    private static final int POST_NOTIFICATIONS = 112;

    private String batteryTitle = DefaultString.batteryTitle;
    private String batteryBody = DefaultString.batteryBody;
    private String batteryButton = DefaultString.batteryButton;

    private String enableGpsTitle = DefaultString.enableGpsTitle;
    private String enableGpsBody = DefaultString.enableGpsBody;
    private String enableGpsButton = DefaultString.enableGpsButton;

    private String fLocationTitle = DefaultString.fLocationTitle;
    private String fLocationBody = DefaultString.fLocationBody;
    private String fLocationButton = DefaultString.fLocationButton;

    private String bLocationTitle = DefaultString.bLocationTitle;
    private String bLocationBody = DefaultString.bLocationBody;
    private String bLocationPositiveButton = DefaultString.bLocationPositiveButton;
    private String bLocationNegativeButton = DefaultString.bLocationNegativeButton;

    private String cancelShakeTitle = DefaultString.cancelShakeTitle;
    private String cancelShakeBody = DefaultString.cancelShakeBody;
    private String cancelShakeButton = DefaultString.cancelShakeButton;
    private String cancelRemainingTime = DefaultString.cancelRemainingTime;
    private int cancelDuration = DefaultString.cancelDuration;

    private String videoUrl = DefaultString.autoStartVideoUrl;
    private String closeButtonText = DefaultString.autoStartCloseButton;

    private cordova.plugin.service.ServiceTracker pref;
    private CallbackContext callbackContext;
    private String message = null;
    private IntentFilter filter = null;
    private cordova.plugin.service.CancelShakePref shakePref;
    private Application app;
    private String package_name;
    private Resources resources;

    private CancelShakeDialog dBottom;
    private HashMap<String, Object> cancelShakeParams = new HashMap<>();
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        pref = new cordova.plugin.service.ServiceTracker(cordova.getContext());
        shakePref = new cordova.plugin.service.CancelShakePref(cordova.getContext());

        app = cordova.getActivity().getApplication();
        package_name = app.getPackageName();
        resources = app.getResources();


        this.callbackContext = callbackContext;
        if (action.equals("startService")) {
              start(args);
            return true;
        }
        else if(action.equals("stopService")){
            actionOnService(Actions.STOP);
            this.callbackFunction("message", callbackContext);
            return true;
        }else if(action.equals("serviceisRunning")){
            JSONObject data = pref.getPermissionText();
            parseJson(data);
            checkShake();
            if(pref.getServiceState() == ServiceState.STARTED){
                callbackContext.success("true");
            }else{
                callbackContext.success("false");
            }
            return true;
        }else if(action.equals("locationInterval")){

            String message = args.getString(0);
            JSONObject data = new JSONObject(message);

            Intent i = new Intent(cordova.getContext(), cordova.plugin.service.EndlessService.class);
            i.setAction(Actions.START.name());
            i.putExtra("locationInfo",message);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                cordova.getContext().startForegroundService(i);
                return true;
            }
            cordova.getContext().startService(i);
            return true;
        }
        return false;
    }

    private void start(JSONArray args) throws JSONException{
            message = args.getString(0);
            JSONObject jsonObject  = new JSONObject(message);
            JSONObject data = jsonObject.optJSONObject("data");
            parseJson(data);
            batteryOptimization();
            pref.setInitData(message);
    }


    private void callbackFunction(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private void parseJson(JSONObject data){
        JSONObject cancelShake = data.optJSONObject("cancelShakeDialog");
        JSONObject permissions = data.optJSONObject("permissions");
        JSONObject autoStartVideo = data.optJSONObject("autoStartVideo");

        if(permissions!=null){
            JSONObject batteryPermission = permissions.optJSONObject("batteryPermission");
            JSONObject enableLocation = permissions.optJSONObject("enableLocation");
            JSONObject forgroundPermission = permissions.optJSONObject("forgroundPermission");
            JSONObject backgroundPermission = permissions.optJSONObject("backgroundPermission");
            if(batteryPermission != null){
                batteryTitle = batteryPermission.has("title") ? batteryPermission.optString("title"):DefaultString.batteryTitle;
                batteryBody = batteryPermission.has("body") ? batteryPermission.optString("body"): DefaultString.batteryBody;
                batteryButton = batteryPermission.has("button") ? batteryPermission.optString("button"): DefaultString.batteryButton;
            }

            if(enableLocation != null){
                enableGpsTitle = enableLocation.has("title") ? enableLocation.optString("title"): DefaultString.enableGpsTitle;
                enableGpsBody = enableLocation.has("body") ? enableLocation.optString("body"): DefaultString.enableGpsBody;
                enableGpsButton = enableLocation.has("button") ? enableLocation.optString("button"): DefaultString.enableGpsButton;
            }

            if(forgroundPermission != null){
                fLocationTitle = forgroundPermission.has("title") ? forgroundPermission.optString("title"): DefaultString.fLocationTitle;
                fLocationBody = forgroundPermission.has("body") ? forgroundPermission.optString("body"): DefaultString.fLocationBody;
                fLocationButton = forgroundPermission.has("button") ? forgroundPermission.optString("button"): DefaultString.fLocationButton;
            }

            if(backgroundPermission != null){
                bLocationTitle = backgroundPermission.has("title") ? backgroundPermission.optString("title"): DefaultString.bLocationTitle;
                bLocationBody = backgroundPermission.has("body") ? backgroundPermission.optString("body"): DefaultString.bLocationBody;
                bLocationPositiveButton = backgroundPermission.has("positiveButton") ? backgroundPermission.optString("positiveButton"): DefaultString.bLocationPositiveButton;
                bLocationNegativeButton = backgroundPermission.has("negativeButtton") ? backgroundPermission.optString("negativeButtton"): DefaultString.bLocationNegativeButton;
            }

        }

        if(cancelShake != null){
            cancelShakeTitle = cancelShake.has("title") ? cancelShake.optString("title"): DefaultString.cancelShakeTitle;
            cancelShakeBody = cancelShake.has("body") ? cancelShake.optString("body"): DefaultString.cancelShakeBody;
            cancelShakeButton = cancelShake.has("button") ? cancelShake.optString("button"): DefaultString.cancelShakeButton;
            cancelRemainingTime =  cancelShake.has("remainingTime") ? cancelShake.optString("remainingTime"): DefaultString.cancelRemainingTime;
            cancelDuration = cancelShake.has("duration") ? cancelShake.optInt("duration"): DefaultString.cancelDuration;

            cancelShakeParams.put("cancelShakeTitle",cancelShakeTitle);
            cancelShakeParams.put("cancelShakeBody",cancelShakeBody);
            cancelShakeParams.put("remainingTime",cancelRemainingTime);
            cancelShakeParams.put("cancelDuration",cancelDuration);
            cancelShakeParams.put("cancelShakeButton",cancelShakeButton);

        }

        if(autoStartVideo != null){

            videoUrl = autoStartVideo.optString( "url",videoUrl);
            closeButtonText = autoStartVideo.optString("closeButton",closeButtonText);
        }
    }
    private void actionOnService(cordova.plugin.service.Actions action) {

        if(pref.getServiceState() == ServiceState.STOPPED  && action == Actions.STOP){
            return;
        }
        if(action == Actions.STOP){
            stopBackgrounService();
            return;
        }
        startBackgroundService(action);

        if(filter == null){
            filter = new IntentFilter("cancel-shake-action");
            LocalBroadcastManager.getInstance(cordova.getContext()).registerReceiver(receiver, filter);
        }

    }

    public void startBackgroundService(Actions action){
        JSONObject jObj = new JSONObject();
        try {
            jObj.put("action",action.name());
            jObj.put("params",message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Data data = new Data.Builder()
                .putString("data", jObj.toString())
                .build();
        Log.d("WORKER", "startServiceViaWorker called");

        String UNIQUE_WORK_NAME = "StartServiceViaWorker";
        String WORKER_TAG = "ServiceWorkerTag";
        WorkManager workManager = WorkManager.getInstance(cordova.getContext());

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
    public void stopBackgrounService(){
        WorkManager.getInstance(cordova.getContext()).cancelAllWorkByTag("ServiceWorkerTag");

        Intent i = new Intent(cordova.getContext(), cordova.plugin.service.EndlessService.class);
        i.setAction(Actions.STOP.name());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            cordova.getContext().startForegroundService(i);
            return;
        }
        cordova.getContext().startService(i);
    }


    private void checkPermission() {

       if (ContextCompat.checkSelfPermission(cordova.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
          
            // Fine Location permission is granted
            // Check if current android version >= 11, if >= 11 check for Background Location permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (ContextCompat.checkSelfPermission(cordova.getActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // Background Location Permission is granted so do your work here
                    actionOnService(Actions.START);
                    this.callbackFunction(message, callbackContext);
                } else {
                    // Ask for Background Location Permission
                    askPermissionForBackgroundUsage();

                }
            }else{
                actionOnService(Actions.START);
                this.callbackFunction(message, callbackContext);
            }
        } else {
            // Fine Location Permission is not granted so ask for permission
            askForLocationPermission();
        }
    }

    private void batteryOptimization(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = cordova.getActivity().getPackageName();
            PowerManager pm = (PowerManager) cordova.getActivity().getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                cordova.startActivityForResult(this,intent,REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                return;
            }
            if(checkGps()){
                checkPermission();
                return;
            }

            requestGPSEnabling();
        }
    }

    private void askForLocationPermission() {
        if (ContextCompat.checkSelfPermission(cordova.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            alertDialog(fLocationTitle,fLocationBody,fLocationButton,LOCATION_PERMISSION_CODE);
        } else {
            cordova.requestPermissions(this, LOCATION_PERMISSION_CODE,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
        }
    }

    private void askPermissionForBackgroundUsage() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(cordova.getActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
           // alertDialog(bLocationTitle,bLocationBody,bLocationButton,BACKGROUND_LOCATION_PERMISSION_CODE);
            backgroundLocationDialog();
        } else {
            cordova.requestPermissions(this, BACKGROUND_LOCATION_PERMISSION_CODE,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION});
        }
    }

    private boolean checkGps(){
        boolean isGpsEnable = false;
        ContentResolver contentResolver = cordova.getActivity().getContentResolver();
        // Find out what the settings say about which providers are enabled
        int mode = Settings.Secure.getInt(
                contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);

        if (mode == Settings.Secure.LOCATION_MODE_OFF) {
            // Location is turned OFF!

            isGpsEnable = false;
        } else {
            // Location is turned ON!
            isGpsEnable = true;
        }
        return isGpsEnable;
    }
    private void requestGPSEnabling() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        Task<LocationSettingsResponse> result =
                LocationServices.getSettingsClient(cordova.getActivity()).checkLocationSettings(builder.build());

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    // GPS zaten açık
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                cordova.setActivityResultCallback(BackgroundService.this);
                                resolvable.startResolutionForResult(cordova.getActivity(), REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException | ClassCastException e) {
                                e.printStackTrace();
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Yer ayarları değiştirilemez
                            break;
                    }
                }
            }
        });
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // User granted location permission
                // Now check if android version >= 11, if >= 11 check for Background Location Permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (ContextCompat.checkSelfPermission(cordova.getActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        // Background Location Permission is granted so do your work here
                        actionOnService(Actions.START);
                        this.callbackFunction(message, callbackContext);
                    } else {
                        // Ask for Background Location Permission
                        askPermissionForBackgroundUsage();

                    }
                }else{
                    if(Build.BRAND.toLowerCase().equals("xiaomi")){
                        int package_label = resources.getIdentifier("app_name", "string", package_name);
                        try {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"));
                            intent.putExtra("package_name", cordova.getContext().getPackageName());
                            intent.putExtra("package_label", cordova.getContext().getText(package_label));
                            cordova.startActivityForResult(this,intent,XIAOMI_AUTO_START);
                        } catch (ActivityNotFoundException anfe) {

                        }
                        return;
                    }

                    if(Build.BRAND.toLowerCase().equals("oppo") ||
                        Build.BRAND.toLowerCase().equals("realme"))
                    {
                        playVideo();
                        return;
                    }
                    actionOnService(Actions.START);
                    this.callbackFunction(message, callbackContext);
                }
            } else {
                // User denied location permission
                alertDialog(fLocationTitle,fLocationBody,fLocationButton,LOCATION_PERMISSION_CODE);
            }
        }

        if (requestCode == BACKGROUND_LOCATION_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // User granted for Background Location Permission.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getNotificationPermission();
                    return;
                }

                if(Build.BRAND.toLowerCase().equals("oppo") ||
                   Build.BRAND.toLowerCase().equals("realme"))
                {
                    playVideo();
                    return;
                }
                actionOnService(Actions.START);
                this.callbackFunction(message, callbackContext);

            } else {
                // User declined for Background Location Permission.
                alertDialog(bLocationTitle,bLocationBody,bLocationPositiveButton,BACKGROUND_LOCATION_PERMISSION_CODE);
            }
        }

        if(requestCode == POST_NOTIFICATIONS){
            actionOnService(Actions.START);
            this.callbackFunction(message, callbackContext);
            if(Build.BRAND.toLowerCase().equals("oppo") ||
                    Build.BRAND.toLowerCase().equals("realme")){
                playVideo();
                return;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) {
            if (resultCode == RESULT_OK) {
                if(checkGps()){
                    checkPermission();
                }else{
                    requestGPSEnabling();
                }
            } else {
                alertDialog(batteryTitle,batteryBody,batteryButton,REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            }
        }

        if(requestCode == REQUEST_CHECK_SETTINGS){
            if(resultCode == RESULT_OK){
                checkPermission();
            }else{
                alertDialog(enableGpsTitle,enableGpsBody,enableGpsButton,GPS_ENABLE);
            }
        }

        if(requestCode == GPS_ENABLED_MANUALLY){
            checkPermission();
        }
        if(requestCode == 801){
            actionOnService(Actions.START);
            this.callbackFunction(message, callbackContext);
        }
        if(requestCode == XIAOMI_AUTO_START){
            AutoStartHelper.getInstance().getAutoStartPermission(this);
        }
    }

    public void getNotificationPermission(){
        cordova.requestPermissions(BackgroundService.this,POST_NOTIFICATIONS,
                new String[]{Manifest.permission.POST_NOTIFICATIONS}
        );
    }
    private void appInfo(){
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", cordova.getContext().getPackageName(), null);
        intent.setData(uri);
        cordova.getContext().startActivity(intent);
    }
    boolean isPrepared = false;
    MediaPlayer mediaPly = null;
    private void playVideo(){
        Dialog dialog = new Dialog(cordova.getContext());

        int closeBtn = resources.getIdentifier("close", "id", package_name);
        int videoViewId = resources.getIdentifier("videoView", "id", package_name);
        int progressBar = resources.getIdentifier("progressBar","id",package_name);
        int relativeLayout = resources.getIdentifier("transparentRl","id",package_name);
        int layout = resources.getIdentifier("dialog", "layout", package_name);

        dialog.setContentView(layout);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);

        ProgressBar progress = dialog.findViewById(progressBar);
        progress.getIndeterminateDrawable().setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);

        RelativeLayout rl = dialog.findViewById(relativeLayout);


        Button close = dialog.findViewById(closeBtn);
        close.setText(closeButtonText);

        VideoView videoView = dialog.findViewById(videoViewId);
        MediaController mediaController = new MediaController(cordova.getContext());
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        videoView.setVideoPath(videoUrl);

        rl.setOnClickListener(v -> {
            mediaPly.seekTo(0); // Videonun başlangıcına git
            mediaPly.start();
            rl.setVisibility(View.INVISIBLE);
        });
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                progress.setVisibility(View.INVISIBLE);
                isPrepared = true;
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPly = mp;
                rl.setVisibility(View.VISIBLE);
            }
        });

        close.setOnClickListener(v->{
            videoView.stopPlayback();
            dialog.cancel();
            appInfo();
        });

        dialog.setOnShowListener(v->videoView.start());
        dialog.show();
    }

    private void backgroundLocationDialog(){
        int titleId = resources.getIdentifier("title","id",package_name);
        int contentId = resources.getIdentifier("content","id",package_name);
        int allowButton = resources.getIdentifier("allowBtn","id",package_name);
        int cancelButton = resources.getIdentifier("cancelBtn","id",package_name);
        int style = resources.getIdentifier("FullScreenDialogTheme", "style", package_name);
        int layout = resources.getIdentifier("background_permission_fragment", "layout", package_name);


        Dialog fullScreenDialog = new Dialog(cordova.getContext(),style);
        fullScreenDialog.setContentView(layout);
        TextView title = fullScreenDialog.findViewById(titleId);
        TextView content = fullScreenDialog.findViewById(contentId);
        Button allowBtn = fullScreenDialog.findViewById(allowButton);
        Button cancelBtn = fullScreenDialog.findViewById(cancelButton);

        title.setText(bLocationTitle);
        content.setText(bLocationBody);
        allowBtn.setText(bLocationPositiveButton);
        cancelBtn.setText(bLocationNegativeButton);

        allowBtn.setOnClickListener(v->{
            cordova.requestPermissions(this, BACKGROUND_LOCATION_PERMISSION_CODE,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION});
            fullScreenDialog.cancel();
        });

        cancelBtn.setOnClickListener(v->{
            // alertDialog(bLocationTitle,bLocationBody,bLocationButton,BACKGROUND_LOCATION_PERMISSION_CODE);
            fullScreenDialog.cancel();
        });

        fullScreenDialog.show();
    }


    private void alertDialog(String title,String body,String buttonText,int permissionCode){
        new AlertDialog.Builder(cordova.getContext(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setCancelable(false)
                .setTitle(title)
                .setMessage(body)
                .setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if(permissionCode == LOCATION_PERMISSION_CODE){
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package",cordova.getActivity().getPackageName(), null);
                            intent.setData(uri);
                            cordova.startActivityForResult(BackgroundService.this,intent,GPS_ENABLED_MANUALLY);
                        }

                        if(permissionCode == BACKGROUND_LOCATION_PERMISSION_CODE){
                            cordova.requestPermissions(BackgroundService.this, BACKGROUND_LOCATION_PERMISSION_CODE,
                                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION});
                        }

                        if(permissionCode == GPS_ENABLE){
                            requestGPSEnabling();
                        }

                        if(permissionCode == REQUEST_IGNORE_BATTERY_OPTIMIZATIONS){
                            batteryOptimization();
                        }
                    }
                })
                .create().show();
    }

    AlertDialog dialog = null;
    private void shakeCanceDialog(String title,String body,String buttonText){

        dialog = new AlertDialog.Builder(cordova.getContext(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setCancelable(false)
                .setTitle(title)
                .setMessage(body)
                .setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogs, int which) {
                        shakePref.setShakeStatus(false);
                        dialog =null;
                       // countDownTimer.cancel();
                    }
                }).create();
        if(dialog.isShowing()){
            return;
        }
        dialog.show();

    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String parameter = intent.getStringExtra("parameter");
            long time = intent.getLongExtra("time",0);
            // Parametre alındı, işlemlerinizi yapabilirsiniz
            try {
                JSONObject data = new JSONObject(parameter);
                String type = data.getString("action");
                if(type.equals("shake")){
                    dBottom = new CancelShakeDialog(cordova.getActivity(),cancelShakeParams);
                    dBottom.setTime(time);
                    dBottom.show(cordova.getActivity().getSupportFragmentManager(),dBottom.getTag());
                    return;
                }
                if(type.equals("sended")){
                    if(dBottom != null){
                        dBottom.cancelSos();
                        dBottom =null;
                    }
                    shakePref.setShakeStatus(false);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    };

    public void checkShake() {
        if(filter ==null){
            filter = new IntentFilter("cancel-shake-action");
            LocalBroadcastManager.getInstance(cordova.getContext()).registerReceiver(receiver, filter);
        }

        if(shakePref.getShakeStatus() && dBottom ==null){
            long time = pref.getTime();
            dBottom = new CancelShakeDialog(cordova.getActivity(),cancelShakeParams);
            dBottom.setTime(time);
            dBottom.show(cordova.getActivity().getSupportFragmentManager(),dBottom.getTag());
        }
    }


    @Override
    public void onStart() {
        super.onStart();

        if(filter ==null){
            filter = new IntentFilter("cancel-shake-action");
            LocalBroadcastManager.getInstance(cordova.getContext()).registerReceiver(receiver, filter);
        }
        if(shakePref.getShakeStatus()){
            long time = pref.getTime();
            dBottom = new CancelShakeDialog(cordova.getActivity(),cancelShakeParams);
            dBottom.setTime(time);
            dBottom.show(cordova.getActivity().getSupportFragmentManager(),dBottom.getTag());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(cordova.getContext()).unregisterReceiver(receiver);
        filter = null;
        dBottom = null;
    }
}
