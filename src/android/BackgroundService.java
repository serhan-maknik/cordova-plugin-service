package cordova.plugin.service;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;


import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.POWER_SERVICE;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import cordova.plugin.service.DefaultString;

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
    private String bLocationButton = DefaultString.bLocationButton;

    private cordova.plugin.service.ServiceTracker pref;
    private CallbackContext callbackContext;
    private String message = null;
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        pref = new cordova.plugin.service.ServiceTracker(cordova.getActivity());

        this.callbackContext = callbackContext;
        if (action.equals("startService")) {
            message = args.getString(0);
            JSONObject jsonObject  = new JSONObject(message);
            JSONObject data = jsonObject.optJSONObject("data");
            JSONObject permissions = data.optJSONObject("permissions");
            parseJson(permissions);
            batteryOptimization();
            return true;
        }
        else if(action.equals("stopService")){
            actionOnService(Actions.STOP);

            this.callbackFunction("message", callbackContext);
            return true;
        }
        return false;
    }


    private void callbackFunction(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private void parseJson(JSONObject permissions){
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
                bLocationButton = backgroundPermission.has("button") ? backgroundPermission.optString("button"): DefaultString.bLocationButton;
            }
        }
    }
    private void actionOnService(Actions action) {

        if(pref.getServiceState() == ServiceState.STOPPED  && action == Actions.STOP){
            return;
        }
        Intent i = new Intent(cordova.getContext(), cordova.plugin.service.EndlessService.class);
        i.putExtra("params",message);
        i.setAction(action.name());

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
            }
        } else {
            // Fine Location Permission is not granted so ask for permission
            askForLocationPermission();
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
            alertDialog(bLocationTitle,bLocationBody,bLocationButton,BACKGROUND_LOCATION_PERMISSION_CODE);
        } else {
            cordova.requestPermissions(this, BACKGROUND_LOCATION_PERMISSION_CODE,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION});
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
            }else{
                if(checkGps()){
                    checkPermission();
                }else{
                    requestGPSEnabling();
                }
            }
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
                }
            } else {
                // User denied location permission
                alertDialog(fLocationTitle,fLocationBody,fLocationButton,LOCATION_PERMISSION_CODE);
            }
        } else if (requestCode == BACKGROUND_LOCATION_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // User granted for Background Location Permission.
                actionOnService(Actions.START);
                this.callbackFunction(message, callbackContext);
            } else {
                // User declined for Background Location Permission.
                alertDialog(bLocationTitle,bLocationBody,bLocationButton,BACKGROUND_LOCATION_PERMISSION_CODE);
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
        }else if(requestCode == REQUEST_CHECK_SETTINGS){
                 if(resultCode == RESULT_OK){
                     checkPermission();
                 }else{
                     alertDialog(enableGpsTitle,enableGpsBody,enableGpsButton,GPS_ENABLE);
                 }
        }else if(requestCode == GPS_ENABLED_MANUALLY){
                    alertDialog(bLocationTitle,bLocationBody,bLocationButton,GPS_ENABLE);

        }
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
                           // cordova.getActivity().startActivity(intent);
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
}
