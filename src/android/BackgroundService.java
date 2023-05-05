package cordova.plugin.service;


import android.app.AlarmManager;
import android.app.Notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
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
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EndlessService extends Service implements SensorEventListener{
    boolean isServiceStarted = false;
    PowerManager pm ;
    PowerManager.WakeLock wakeLock;
    private SensorManager mSensorManager;
    Vibrator vibrator;
    VibrationEffect vibrationEffect1;

    private static final int SHAKE_INTERVAL = 100; // ölçüm aralığı (ms)
    private static final float ANGLE_THRESHOLD = 60.0f; // dönüş açısı eşiği (derece)
    private  int COUNT_THRESHOLD = 3; // toast mesajı göstermek için gereken koşul sayısı
    private static final int RESET_INTERVAL = 1800; // count sıfırlama aralığı (ms)
    private static final float SPEED_THRESHOLD = 400f; // derece/saniye

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private boolean lastAccelerometerSet = false;
    private boolean lastMagnetometerSet = false;
    private float[] lastAccelerometer = new float[3];
    private float[] lastMagnetometer = new float[3];
    private long lastTimestamp = 0;
    private float lastYRotation = 0;
    private float lastXRotation = 0;
    private int shakeCount = 0;

    private long lastTime;

    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;

    float x;
    float y;
    float z ;
    boolean isPortrait = false;
    boolean isOk = true;
    private Handler handler = new Handler();
    private Runnable resetRunnable = new Runnable() {
        @Override
        public void run() {
            shakeCount = 0;
            isOk = true;
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    String url;
    String body = null;
    String params = null;
    HashMap<String,String> headers = null;
    HashMap<String, String> notification = null;
    HashMap<String, String> toast = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        params = intent.getStringExtra("params");
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
                    body = JsonBody.toString();
                }
                JSONObject jsonNotification = data.optJSONObject("notification");
                if(jsonNotification != null){
                    notification = new Gson().fromJson(String.valueOf(jsonNotification), new TypeToken<HashMap<String, String>>(){}.getType());
                }
                JSONObject jsonToast = data.optJSONObject("toast");
                if(jsonToast != null){
                    toast = new Gson().fromJson(String.valueOf(jsonToast), new TypeToken<HashMap<String, String>>(){}.getType());
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);

        if (intent != null) {
            String action = intent.getAction();
            if(Actions.START.name()== action){
                startService();
            }else if(Actions.STOP.name()== action){
                stopService();
            }else{
                Log.d("SERVİCE","This should never happen. No action in the received intent");
            }
        } else {
            Log.d("SERVİCE","with a null intent. It has been probably restarted by the system.");
        }
        // by returning this we make sure the service is restarted if the system kills the service

        if(isServiceStarted){
            startForeground(1, builtNotification());
        }
        return START_STICKY;
    }

    private String parseUrl(){

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

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);
            lastAccelerometerSet = true;
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];

            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;
            if(Math.abs(event.values[1]) > Math.abs(event.values[0])) {
                //Mainly portrait
                if (event.values[1] > 7) {
                    isPortrait = true;
                } else  {
                    isPortrait = false;
                }
            }
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);
            lastMagnetometerSet = true;
        }

        if ( lastMagnetometerSet && !isPortrait ) {
            long currentTimestamp = System.currentTimeMillis();
            if (currentTimestamp - lastTimestamp > SHAKE_INTERVAL && isOk) {
                lastTimestamp = currentTimestamp;

                float[] rotationMatrix = new float[9];
                float[] orientation = new float[3];

                SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer);
                SensorManager.getOrientation(rotationMatrix, orientation);

                float yRotation = orientation[2];
                float xRotation = orientation[1];
                float xRotationDegrees = (float) Math.toDegrees(xRotation);
                float yRotationDegrees = (float) Math.toDegrees(yRotation);
                if (yRotationDegrees < 0) {
                    yRotationDegrees += 360;
                }

                if (lastYRotation == 0 && lastTime == 0) {
                    lastYRotation = yRotation;
                    lastXRotation = xRotation;
                    lastTime = currentTimestamp;
                    return;
                }

                // Zaman farkı
                float deltaTime = (currentTimestamp - lastTime) / 1000f; // deltaTime saniye cinsinden olacak

                // Y ekseni etrafındaki dönüş hızı
                float deltaRotation = Math.abs(yRotation - lastYRotation);
                float yRotationSpeed = deltaRotation / deltaTime; // derece/saniye


                if (Math.abs(yRotationDegrees - lastYRotation) > ANGLE_THRESHOLD
                        && Math.abs(yRotationDegrees - lastYRotation) < 150
                        && Math.abs(xRotationDegrees - lastXRotation) < 20) {
                    shakeCount++;
                    lastTime = currentTimestamp;
                    if (shakeCount >= COUNT_THRESHOLD && yRotationSpeed > SPEED_THRESHOLD ) {
                        //      Toast.makeText(this, "Y ekseni etrafındaki dönüş açısı çok hızlı değişiyor!", Toast.LENGTH_SHORT).show();
                        vibrate();
                        requestPost();
                        isOk = false;

                        shakeCount = 0;
                        lastTime = 0;
                        lastYRotation = 0;
                        lastXRotation= 0;
                        handler.removeCallbacks(resetRunnable);
                        handler.postDelayed(resetRunnable, 4000);
                    } else {
                        handler.removeCallbacks(resetRunnable);
                        handler.postDelayed(resetRunnable, RESET_INTERVAL);
                    }
                }
                lastYRotation = yRotationDegrees;
                lastXRotation = xRotationDegrees;
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    public void vibrate(){
        // this is the only type of the vibration which requires system version Oreo (API 26)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            // this effect creates the vibration of default amplitude for 1000ms(1 sec)
            vibrationEffect1 = VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE);

            // it is safe to cancel other vibrations currently taking place
            vibrator.cancel();
            vibrator.vibrate(vibrationEffect1);
        }
    }
    private void requestPost(){
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();
        String BASE_URL = parseUrl();
        Retrofit retrofit = new Retrofit.Builder()
                .client(client)
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        cordova.plugin.service.ServiceApi apiService = retrofit.create(cordova.plugin.service.ServiceApi.class);
/* 
        HashMap<String,String> mBody = new HashMap<>();
        mBody.put("data",body); */
        Call<ResponseBody> call;
        if (headers == null && body !=null){
            call = apiService.postData(url,body);
        }else if(body == null && headers != null){
            call = apiService.postData(url,headers);
        }else if(body == null && headers == null){
            call = apiService.postData(url);
        }else{
            call = apiService.postData(url,headers,body);
        }
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String responseString = response.body().string();
                        /*
                        try {
                            JSONObject jObj = new JSONObject(responseString);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                         */
                        Log.d("SERSER","res: "+responseString );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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


    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceStarted = false;
        sensorManager.unregisterListener(this);
        cordova.plugin.service.ServiceTracker tracker = new cordova.plugin.service.ServiceTracker(this);
        tracker.setServiceState(cordova.plugin.service.ServiceState.STOPPED);
   //     Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Intent restartServiceIntent = new Intent(getApplicationContext(),EndlessService.class);
        restartServiceIntent.putExtra("params",params);
        restartServiceIntent.setPackage(getPackageName());
        PendingIntent restartServicePendingIntent = PendingIntent.getService(this,1,restartServiceIntent,PendingIntent.FLAG_IMMUTABLE);
        getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent);
    }

    private void startService(){
        if (isServiceStarted) return;

        if(toast != null){
            Toast.makeText(this, toast.get("start"), Toast.LENGTH_SHORT).show();

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

        if(toast != null){
            Toast.makeText(this, toast.get("stop"), Toast.LENGTH_SHORT).show();
        }

        try {
            Log.d("SERSER","wakeLock.isHeld(): "+wakeLock.isHeld());
            if(wakeLock.isHeld()){
                wakeLock.release();
            }
            stopForeground(true);
            stopSelf();

        }catch (Exception e){
            Log.d("SERSER","Service stopped without being started:"+e);
        }

        isServiceStarted = false;
        ServiceTracker tracker = new ServiceTracker(this);
        tracker.setServiceState(ServiceState.STOPPED);
    }



    public Notification builtNotification() {

        String title = "Bekapp Servis";
        String body = "Bekapp servis çalışıyor..";
        if(notification != null){
            title = notification.containsKey("title") ? notification.get("title") : title;
            body = notification.containsKey("body") ? notification.get(body): body;
        }

        NotificationManager notificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;

        NotificationCompat.Builder builder = null;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel =
                    new NotificationChannel("ID", "Name", importance);


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
}
