package cordova.plugin.service;

import android.app.Activity;
import android.app.Application;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;


public class CancelShakeDialog extends BottomSheetDialogFragment {

    private TextView tDuration,sTitle,sBody,sRemainTitleText;
    private Button cancelBtn;
    private long time;
    private Application app;
    private String package_name;
    private Resources resources;
    private int  remainTitleText,title,body,layout,cancelButton,duration;
    private CountDownTimer countDownTimer;
    private cordova.plugin.service.CancelShakePref shakePref;
    private HashMap<String, Object> params;
    public CancelShakeDialog(Activity activity, HashMap<String, Object> params){
        this.params = params;
        Log.d("HASHAS","CancelShakeDialog: "+params.toString());
        shakePref = new cordova.plugin.service.CancelShakePref(activity.getApplicationContext());
        app = activity.getApplication();
        package_name = app.getPackageName();
        resources = app.getResources();

        layout = resources.getIdentifier("bottom_layout", "layout", package_name);
        remainTitleText = resources.getIdentifier("remainTimeText", "id", package_name);
        title = resources.getIdentifier("title","id",package_name);
        body = resources.getIdentifier("body","id",package_name);
        cancelButton = resources.getIdentifier("cancel","id",package_name);
        duration = resources.getIdentifier("remainTime","id",package_name);

    }
    public void setTime(long time){
        this.time = time;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(layout,container,false);
        sTitle = v.findViewById(title);
        sBody = v.findViewById(body);
        sRemainTitleText = v.findViewById(remainTitleText);
        cancelBtn = v.findViewById(cancelButton);
        tDuration = v.findViewById(duration);

        sTitle.setText((String) params.get("cancelShakeTitle"));
        sBody.setText((String)params.get("cancelShakeBody"));
        sRemainTitleText.setText((String)params.get("remainingTime")+": ");
        cancelBtn.setText((String)params.get("cancelShakeButton"));
        cancelBtn.setOnClickListener((view)->cancelSos());

        return v;
    }



    @Override
    public void onStart() {
        super.onStart();
        startCountdown(time);
    }

    private void startCountdown(long countdownEndTime) {
        // 1 saniyelik aralıklarla geri sayımı yapacak bir CountDownTimer oluşturun
        countDownTimer = new CountDownTimer(countdownEndTime - System.currentTimeMillis(), 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Geri sayım devam ederken ekrana süreyi gösterin
                SimpleDateFormat sdf = new SimpleDateFormat("ss", Locale.getDefault());
                String formattedTime = sdf.format(new Date(millisUntilFinished));
                tDuration.setText(formattedTime);
            }

            @Override
            public void onFinish() {
                // Geri sayım tamamlandığında ekrana "Bitti!" mesajı gösterin
                dismiss();
            }
        }.start();
    }

    public void cancelSos(){
        shakePref.setShakeStatus(false);
        countDownTimer.cancel();
        dismiss();
    }
}
