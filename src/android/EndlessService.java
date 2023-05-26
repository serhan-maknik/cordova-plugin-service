package cordova.plugin.service;


import android.app.AlarmManager;
import android.app.Notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;

import android.graphics.Color;

import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import safety.com.br.android_shake_detector.core.ShakeCallback;
import safety.com.br.android_shake_detector.core.ShakeDetector;
import safety.com.br.android_shake_detector.core.ShakeOptions;
import cordova.plugin.service.CurrentLocationListener;


public class EndlessService extends Service implements  CurrentLocationListener.LocationListener, CurrentLocationListener.MockListener {
    boolean isServiceStarted = false;
    PowerManager pm ;
    PowerManager.WakeLock wakeLock;
    Vibrator vibrator;

    private ShakeDetector shakeDetector;
    private static final String TITLE = "BackgroundService is running";
    private static final String BODY = "";

    public Location location = null;

    private JSONObject SOS = null;
    private JSONObject jsonLocation = null;
    private String url;
    private String params = null;
    private HashMap<String,String> headers = null;
    private HashMap<String, String> notification = null;
    private String startToast = "";
    private String stopToast = "" ;
    private CurrentLocationListener currentLocationListener;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        params = intent.getStringExtra("params");

        if(!isServiceStarted){
            shakeDedector();
            checkGps();
            currentLocationListener = CurrentLocationListener.getInstance(getApplicationContext());
            currentLocationListener.setListener(this);
            currentLocationListener.setMockListener(this);
        }
        try {
            if(params != null){
                JSONObject message  = new JSONObject(params);
                JSONObject data = message.getJSONObject("data");
                url = data.getString("url");
                JSONObject jsonHeader = data.optJSONObject("header");
                if(jsonHeader != null){
                    headers = new Gson().fromJson(String.valueOf(jsonHeader), new TypeToken<HashMap<String, String>>(){}.getType());
                }
                JSONObject JsonBody = data.optJSONObject("body");
                if(JsonBody != null){
                   SOS = JsonBody.optJSONObject("SOS");
                   jsonLocation = JsonBody.optJSONObject("location");
                }
                JSONObject jsonNotification = data.optJSONObject("notification");
                if(jsonNotification != null){
                    notification = new Gson().fromJson(String.valueOf(jsonNotification), new TypeToken<HashMap<String, String>>(){}.getType());
                }
                JSONObject jsonToast = data.optJSONObject("toast");
                if(jsonToast != null){
                    startToast = jsonToast.optString("start");
                    stopToast = jsonToast.optString("stop");
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);


        if (intent != null) {
            String action = intent.getAction();
            if(cordova.plugin.service.Actions.START.name()== action){
                startService();
            }else if(cordova.plugin.service.Actions.STOP.name()== action){
                stopService();
            }else{
                Log.d("SERVICE","This should never happen. No action in the received intent");
            }
        } else {
            Log.d("SERVICE","with a null intent. It has been probably restarted by the system.");
        }
        // by returning this we make sure the service is restarted if the system kills the service

        if(isServiceStarted){
            startForeground(1, builtNotification());
        }
        return START_STICKY;
    }

    boolean isGpsEnable = false;
    private boolean checkGps(){
        ContentResolver contentResolver = getBaseContext().getContentResolver();
        // Find out what the settings say about which providers are enabled
        int mode = Settings.Secure.getInt(
                contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);

        if (mode == Settings.Secure.LOCATION_MODE_OFF) {
            // Location is turned OFF!
            gpsClosed(parseUrl(url));
            isGpsEnable = false;
        } else {
            // Location is turned ON!
            isGpsEnable = true;
        }
        return isGpsEnable;
    }


    private void shakeDedector(){
        ShakeOptions options = new ShakeOptions()
                .background(true)
                .interval(1000)
                .shakeCount(4)
                .sensibility(5f);
        shakeDetector = new ShakeDetector(options);

            shakeDetector.stopShakeDetector(getApplicationContext());
            //     shakeDetector.destroy(getApplicationContext());
            shakeDetector = new ShakeDetector(options).start(getApplicationContext(), new ShakeCallback() {
                @Override
                public void onShake() {
                    requestPost(parseUrl(url));
                }
            });
    }

    public void vibrate(){
        // this is the only type of the vibration which requires system version Oreo (API 26)
        final VibrationEffect vibrationEffect1;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            // this effect creates the vibration of default amplitude for 1000ms(1 sec)
            vibrationEffect1 = VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE);

            // it is safe to cancel other vibrations currently taking place
            vibrator.cancel();
            vibrator.vibrate(vibrationEffect1);
        }
    }


    private Handler locationHandler;
    private Runnable runnable;

    public void mLocationHandler() {
        locationHandler = new Handler(Looper.getMainLooper());
        runnable = new Runnable() {
            @Override
            public void run() {
                if(checkGps()){
                    String latidute = String.valueOf(location.getLatitude());
                    String longitude = String.valueOf(location.getLongitude());
                    locationPost(parseUrl(url),latidute,longitude);
                }
                locationHandler.postDelayed(runnable, 15000);
            }
        };
    }

    public void startHandler() {
        locationHandler.post(runnable);
    }

    public void stopHandler() {
        if(locationHandler!=null)
           locationHandler.removeCallbacks(runnable);
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

    }
    boolean isFirst = true;
    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        if(isFirst){
            mLocationHandler();
            startHandler();
            isFirst = false;
        }
    }

    public Notification builtNotification() {
       String title = TITLE;
       String body = BODY;
        if(notification != null){
            title = notification.containsKey("title") ? notification.get("title") : title;
            body = notification.containsKey("body") ? notification.get("body"): body;
        }

        NotificationManager notificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;

        NotificationCompat.Builder builder = null;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel =
                    new NotificationChannel("ID", "Background Service", importance);


            notificationManager.createNotificationChannel(notificationChannel);
            builder = new NotificationCompat.Builder(this, notificationChannel.getId());
        } else {
            builder = new NotificationCompat.Builder(this);
        }

        builder.setDefaults(Notification.DEFAULT_LIGHTS);
        builder.setSmallIcon(getResources().getIdentifier("ic_launcher","mipmap",getPackageName()))
                .setAutoCancel(false)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setColor(Color.parseColor("#0352C9"))
                .setContentTitle(title)
                .setContentText(body);

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        Notification notification = builder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        return notification;
    }

    private void startService(){
        if (isServiceStarted) return;

        if(!startToast.isEmpty()){
            Toast.makeText(this, startToast, Toast.LENGTH_SHORT).show();
        }
        Log.d("SERVICE","Starting the foreground service task");
        isServiceStarted = true;
        cordova.plugin.service.ServiceTracker tracker = new cordova.plugin.service.ServiceTracker(this);
        tracker.setServiceState(cordova.plugin.service.ServiceState.STARTED);

        pm= (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"EndlessService::lock");
        wakeLock.acquire();

    }

    private void stopService(){
        shakeDetector.stopShakeDetector(getApplicationContext());
        shakeDetector.destroy(getApplicationContext());
        if(!stopToast.isEmpty()){
            Toast.makeText(this, stopToast, Toast.LENGTH_SHORT).show();
        }
        stopHandler();
        currentLocationListener.stopLocation();
        try {
            if(wakeLock.isHeld()){
                wakeLock.release();
            }
            stopForeground(true);
            stopSelf();

        }catch (Exception e){
            Log.d("SERVICE","Service stopped without being started:"+e);
        }

        isServiceStarted = false;
        cordova.plugin.service.ServiceTracker tracker = new cordova.plugin.service.ServiceTracker(this);
        tracker.setServiceState(cordova.plugin.service.ServiceState.STOPPED);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceStarted = false;
        cordova.plugin.service.ServiceTracker tracker = new cordova.plugin.service.ServiceTracker(this);
        tracker.setServiceState(cordova.plugin.service.ServiceState.STOPPED);
    }

    @Override
    public void onMockLocationsDetected() {
        mockLocationPost(parseUrl(url));
    }


    /* ***************** Api Requests ******************* */

    private String parseUrl(String url){
        try {
            URL mUrl = new URL(url);
            if(mUrl.getPort() == -1){ // port is not
                return mUrl.getProtocol()+"://"+mUrl.getHost()+"/";
            } else {
                return mUrl.getProtocol()+"://"+mUrl.getHost()+":"+mUrl.getPort()+"/";
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }


    private Retrofit retrofitConf(String baseUrl){
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(client)
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit;
    }

    private void requestPost(String baseUrl){

        Retrofit retrofit = retrofitConf(baseUrl);
        cordova.plugin.service.ServiceApi apiService = retrofit.create(cordova.plugin.service.ServiceApi.class);

        HashMap<String,String> body = new HashMap<>();
        body.put("latitude", String.valueOf(location.getLatitude()));
        body.put("longitude", String.valueOf(location.getLongitude()));

        try {
            SOS.getJSONObject("args").put("location",body);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        HashMap<String,String> mBody = new HashMap<>();
        mBody.put("data",SOS.toString());

        Call<ResponseBody> call;
        if (headers == null ){
            call = apiService.postData(url,mBody);
        }else{
            call = apiService.postData(url,headers,mBody);
        }
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    vibrate();
                } else {
                    // Hata durumlarında yapılacak işlemler

                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // İstek başarısız olduysa yapılacak işlemler

            }
        });
    }


    private void locationPost(String baseUrl, String latitude,String longitude){

        Retrofit retrofit = retrofitConf(baseUrl);

        cordova.plugin.service.ServiceApi apiService = retrofit.create(cordova.plugin.service.ServiceApi.class);

        HashMap<String,String> body = new HashMap<>();
        body.put("latitude",latitude);
        body.put("longitude",longitude);

        try {
            jsonLocation.getJSONObject("args").put("location",body);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        HashMap<String,String> mBody = new HashMap<>();
        mBody.put("data",jsonLocation.toString());


        Call<ResponseBody> call;
        if (headers == null){
            call = apiService.postData(url,mBody);
        }else{
            call = apiService.postData(url,headers,mBody);
        }

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {

                } else {
                    // Hata durumlarında yapılacak işlemler

                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // İstek başarısız olduysa yapılacak işlemler
            }
        });
    }


    private void gpsClosed(String baseUrl){

        Retrofit retrofit = retrofitConf(baseUrl);

        cordova.plugin.service.ServiceApi apiService = retrofit.create(cordova.plugin.service.ServiceApi.class);

        JSONObject gpsClosed = jsonLocation;

        try {
            gpsClosed.putOpt("action","gps_close");
            gpsClosed.getJSONObject("args").put("location","null");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        HashMap<String,String> mBody = new HashMap<>();
        mBody.put("data",gpsClosed.toString());

        Call<ResponseBody> call;
        if (headers == null){
            call = apiService.postData(url,mBody);
        }else{
            call = apiService.postData(url,headers,mBody);
        }

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {

                } else {
                    // Hata durumlarında yapılacak işlemler

                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // İstek başarısız olduysa yapılacak işlemler
            }
        });
    }


    private void mockLocationPost(String baseUrl){

        Retrofit retrofit = retrofitConf(baseUrl);

        cordova.plugin.service.ServiceApi apiService = retrofit.create(cordova.plugin.service.ServiceApi.class);

        JSONObject mockLocation = jsonLocation;

        try {
            mockLocation.putOpt("action","mock_location");
            mockLocation.getJSONObject("args").put("location","null");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        HashMap<String,String> mBody = new HashMap<>();
        mBody.put("data",mockLocation.toString());

        Call<ResponseBody> call;

        if (headers == null){
            call = apiService.postData(url,mBody);
        }else{
            call = apiService.postData(url,headers,mBody);
        }

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {

                } else {
                    // Hata durumlarında yapılacak işlemler

                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // İstek başarısız olduysa yapılacak işlemler
            }
        });
    }


}
