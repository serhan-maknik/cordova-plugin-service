package cordova.plugin.service;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;


public class CurrentLocationListener {
    private static final int REQUEST_CHECK_CODE = 8989;
    private static CurrentLocationListener instance;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest.Builder builder;
    private LocationCallback locationCallback;
    private MockListener listener;
    private static Context context;
    private LocationListener mListener;

    // Mock location rejection
    private android.location.Location lastMockLocation;
    private int numGoodReadings;
    private boolean mockLocationsEnabled;

    public static CurrentLocationListener getInstance(Context appContext) {
        context = appContext;
        if (instance == null) {
            instance = new CurrentLocationListener(appContext);
        }
        return instance;
    }
    public void setListener(LocationListener listener){
        this.mListener = listener;
    }

    @SuppressLint("MissingPermission")
    private CurrentLocationListener(Context appContext) {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext);
        createLocationRequest();
    }

    private void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(3000);
        mLocationRequest.setFastestInterval(1500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
    }

    public void stopLocation(){
        if (mLocationCallback != null)
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        instance = null;
    }


    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (android.location.Location location : locationResult.getLocations()) {
                if (location != null) {
                    if(onLocationChanged(location)){
                        mListener.onLocationChanged(location);
                    }
                }
            }
        }
    };
    public void setMockListener(MockListener listener){
        this.listener = listener;
    }
    private boolean onLocationChanged(android.location.Location location) {
        boolean plausible = isLocationPlausible(location);
        if (!plausible) {
            if (listener != null) listener.onMockLocationsDetected();
            return false;
        }else{
            return true;
        }
    }

    @SuppressLint("NewApi")
    private boolean isLocationPlausible(android.location.Location location) {
        if (location == null) return false;
        boolean isMock = mockLocationsEnabled ||  location.isFromMockProvider();
        if (isMock) {
            lastMockLocation = location;
            numGoodReadings = 0;
        } else
            numGoodReadings = Math.min(numGoodReadings + 1, 1000000); // Prevent overflow

        // We only clear that incident record after a significant show of good behavior
        if (numGoodReadings >= 20) lastMockLocation = null;

        // If there's nothing to compare against, we have to trust it
        if (lastMockLocation == null) return true;

        // And finally, if it's more than 1km away from the last known mock, we'll trust it
        double d = location.distanceTo(lastMockLocation);
        return (d > 1000);
    }
    public interface MockListener {
        void onMockLocationsDetected();
    }

    public interface LocationListener{
        void onLocationChanged(android.location.Location location);
    }

}
