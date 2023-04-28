package cordova.plugin.service;


import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

import okhttp3.OkHttpClient;
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
            Log.d("SERSER","count sıfırlandı");
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SERSER","onStartCommand"+intent.getAction());
        COUNT_THRESHOLD = intent.getIntExtra("leftSensitivity",3);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);

        if (intent != null) {
            String action = intent.getAction();

            Log.d("SERSER","action: "+action);
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
        return START_STICKY;
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
                    // isPortrait = false;
                    //  Log.d("SERSER","Portrait: "+event.values[1]);
                } else  {
                    isPortrait = false;
                    //  Log.d("SERSER","landscpas");
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

                //     Log.d("SERSER","orientation[2]: "+orientation[1]);
                float yRotation = orientation[2];
                float xRotation = orientation[1];
                float xRotationDegrees = (float) Math.toDegrees(xRotation);
                float yRotationDegrees = (float) Math.toDegrees(yRotation);
                if (yRotationDegrees < 0) {
                    yRotationDegrees += 360;
                }

               /* if (lastYRotation == 0 && lastTime == 0) {
                    lastYRotation = yRotation;
                    lastTime = currentTimestamp;
                    return;
                }*/

                // Zaman farkını hesaplayın
                float deltaTime = (currentTimestamp - lastTime) / 1000f; // deltaTime saniye cinsinden olacak

                // Y ekseni etrafındaki dönüş hızını hesaplayın
                float deltaRotation = Math.abs(yRotation - lastYRotation);
                float yRotationSpeed = deltaRotation / deltaTime; // derece/saniye

                // Log.d("SERSER","yRotationSpeed: "+yRotationSpeed);
                //        Log.d("SERSER","XRotationDegrees: "+xRotationDegrees);
                //     Log.d("SERSER","yRotationDegrees: "+yRotationDegrees);

                //  Log.d("SERSER","degree fark: "+Math.abs(xRotationDegrees - lastXRotation));
                if (Math.abs(yRotationDegrees - lastYRotation) > ANGLE_THRESHOLD
                        && Math.abs(yRotationDegrees - lastYRotation) < 150
                        && Math.abs(xRotationDegrees - lastXRotation) < 30) {
                    shakeCount++;
                    //    Log.d("SERSER","shakeCount: "+shakeCount);
                    //   Log.d("SERSER","yRotationSpeed > SPEED_THRESHOLD: "+(yRotationSpeed > SPEED_THRESHOLD));
                    lastTime = currentTimestamp;
                    if (shakeCount >= COUNT_THRESHOLD && yRotationSpeed > SPEED_THRESHOLD ) {
                        //      Toast.makeText(this, "Y ekseni etrafındaki dönüş açısı çok hızlı değişiyor!", Toast.LENGTH_SHORT).show();
                        vibrate();
                        requestPost("Shake");
                        isOk = false;
                        shakeCount = 0;
                    } else {
                        Log.d("SERSER","RESET");
                        handler.removeCallbacks(resetRunnable);
                        handler.postDelayed(resetRunnable, RESET_INTERVAL);
                    }
                }

                /*if (shakeCount >= COUNT_THRESHOLD) {
                    long timeSinceLastShake = currentTimestamp - lastShakeTime;
                    if (timeSinceLastShake > 3000) {
                        shakeCount = 0;
                        lastShakeTime = 0;
                    }
                } else {
                    lastShakeTime = currentTimestamp;
                }*/
                lastYRotation = yRotationDegrees;
                lastXRotation = xRotationDegrees;
            }
        }
    }


   /* @Override
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
                    // isPortrait = false;
                    //  Log.d("SERSER","Portrait: "+event.values[1]);
                } else  {
                    isPortrait = false;
                    //  Log.d("SERSER","landscpas");
                }
            }
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);
            lastMagnetometerSet = true;
        }

        if ( lastMagnetometerSet && !isPortrait ) {
            long currentTimestamp = System.currentTimeMillis();
            if (currentTimestamp - lastTimestamp > SHAKE_INTERVAL) {
                lastTimestamp = currentTimestamp;

                float[] rotationMatrix = new float[9];
                float[] orientation = new float[3];

                SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer);
                SensorManager.getOrientation(rotationMatrix, orientation);

                //     Log.d("SERSER","orientation[2]: "+orientation[1]);
                float yRotation = orientation[2];
                float xRotation = orientation[1];
                float xRotationDegrees = (float) Math.toDegrees(xRotation);
                float yRotationDegrees = (float) Math.toDegrees(yRotation);
               *//* if (yRotationDegrees < 0) {
                    yRotationDegrees += 360;
                }*//*
                //        Log.d("SERSER","XRotationDegrees: "+xRotationDegrees);
                //      Log.d("SERSER","yRotationDegrees: "+yRotationDegrees);

                Log.d("SERSER","degree fark: "+Math.abs(yRotationDegrees - lastYRotation));
                if (Math.abs(yRotationDegrees - lastYRotation) > ANGLE_THRESHOLD && Math.abs(xRotationDegrees - lastXRotation) < 30 ) {
                    shakeCount++;

                    if (shakeCount == COUNT_THRESHOLD) {
                        //  Toast.makeText(this, "Y ekseni etrafındaki dönüş açısı çok hızlı değişiyor!", Toast.LENGTH_SHORT).show();
                        vibrate();
                        shakeCount = 0;
                    } else {
                        handler.removeCallbacks(resetRunnable);
                        handler.postDelayed(resetRunnable, RESET_INTERVAL);
                    }
                }

                lastYRotation = yRotationDegrees;
                lastXRotation = xRotationDegrees;
            }
        }
    }*/

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

    private void requestPost(String name){
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();
        String BASE_URL = "http://192.168.1.5:3000";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        HashMap<String, String> map = new HashMap<>();
        map.put("userId", "12");
        map.put("name", "Serhan");

        ApiService apiService = retrofit.create(ApiService.class);

        Call<ResponseBody> call = apiService.postData(map);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String responseString = response.body().string();
                        try {
                            JSONObject jObj = new JSONObject(responseString);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        // responseString ile cevap verisini kullanabilirsiniz
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

        startForeground(1, createNotification());

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
        ServiceTracker tracker = new ServiceTracker(this);
        tracker.setServiceState(ServiceState.STOPPED);
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Intent restartServiceIntent = new Intent(getApplicationContext(),EndlessService.class);
        restartServiceIntent.setPackage(getPackageName());
        PendingIntent restartServicePendingIntent = PendingIntent.getService(this,1,restartServiceIntent,PendingIntent.FLAG_IMMUTABLE);
        getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent);

    }

    private void startService(){
        if (isServiceStarted) return;

        Log.d("SERVICE","Starting the foreground service task");
        Toast.makeText(this, "Service starting its task", Toast.LENGTH_SHORT).show();
        isServiceStarted = true;
        ServiceTracker tracker = new ServiceTracker(this);
        tracker.setServiceState(ServiceState.STARTED);

        pm= (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"EndlessService::lock");
        wakeLock.acquire();

    }


    private void stopService(){

        Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show();
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

    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);

        final String notificationChannelId ="ENDLESS SERVICE CHANNEL";
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                |Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_IMMUTABLE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            NotificationChannel notificationChannel=new NotificationChannel(notificationChannelId,"open_geo",notificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("Endless Service notifications channel");
            notificationChannel.setName("Endless Service channel");
            /*notificationChannel.enableVibration(true);
            long a[] = {100, 200, 300, 400, 500, 400, 300, 200, 400};
            notificationChannel.setVibrationPattern(a);*/
            notificationManager.createNotificationChannel(notificationChannel);
        }
        Notification notificationBuilder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationBuilder = new Notification.Builder(this, notificationChannelId)
                    .setSmallIcon(getResources().getIdentifier("ic_launcer","mipmap",MainActivity.packagename))
                    .setContentTitle("Endless Service")
                    .setContentText("This is your favorite endless service working")
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent).build();
        }else{
            notificationBuilder = new Notification.Builder(this)
                    .setSmallIcon(getResources().getIdentifier("ic_launcer","mipmap",MainActivity.packagename))
                    .setContentTitle("Endless Service")
                    .setContentText("This is your favorite endless service working")
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent).build();
        }
        // notificationManager.notify((int) Calendar.getInstance().getTimeInMillis() /* ID of notification */, notificationBuilder.build());
        return notificationBuilder;
    }


}
