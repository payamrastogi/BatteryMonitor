package com.coddicted.batteryMonitor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    int chargeLimit = 85;
    int dischargeLimit = 25;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SeekBar chargeAlarmSeekBar = (SeekBar)findViewById(R.id.chargeAlarmSeekBar);
        Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setText("C: " + chargeLimit + " Start Monitoring D:" + dischargeLimit);
        chargeAlarmSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                chargeLimit=progress*5;
                startButton.setText("C: " + chargeLimit + " Start D:" + dischargeLimit);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        SeekBar dischargeAlarmSeekBar = (SeekBar)findViewById(R.id.dischargeAlarmSeekBar);
        dischargeAlarmSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                dischargeLimit=progress*5;
                startButton.setText("C: " + chargeLimit + " Start D:" + dischargeLimit);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
       // this.registerReceiver(this.broadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    public void scheduleJob(View v){
        cancelJob(v);
        createNotificationChannel();
        scheduleChargingJob();
        scheduleDischargingJob();
        Toast.makeText(getApplicationContext(), "Monitoring Started", Toast.LENGTH_SHORT).show();
    }

    public void cancelJob(View v){
        SeekBar chargeAlarmSeekBar = (SeekBar)findViewById(R.id.chargeAlarmSeekBar);
        chargeAlarmSeekBar.setEnabled(true);
        SeekBar dischargeAlarmSeekBar = (SeekBar)findViewById(R.id.dischargeAlarmSeekBar);
        dischargeAlarmSeekBar.setEnabled(true);
        JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(BatteryMonitorConstant.CHARGING_JOB_ID);
        jobScheduler.cancel(BatteryMonitorConstant.DISCHARGING_JOB_ID);
        Toast.makeText(getApplicationContext(), "Monitoring Cancelled", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Job cancelled");
    }

    private void scheduleChargingJob() {
        ComponentName componentName = new ComponentName(this, MonitorChargingJobService.class);
        PersistableBundle persistableBundle = new PersistableBundle();
        persistableBundle.putInt(BatteryMonitorConstant.CHARGE_LIMIT, chargeLimit);
        JobInfo chargeJobInfo = new JobInfo.Builder(BatteryMonitorConstant.CHARGING_JOB_ID, componentName)
                .setPersisted(true)
                .setPeriodic(15 * 60 * 1000)
                .setExtras(persistableBundle)
                .build();
        JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = jobScheduler.schedule(chargeJobInfo);
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Job Scheduled charging");
        } else {
            Log.d(TAG, "Job Scheduling failed");
        }
    }

    private void scheduleDischargingJob(){
        ComponentName componentName = new ComponentName(this, MonitorDischargingJobService.class);
        PersistableBundle persistableBundle = new PersistableBundle();
        persistableBundle.putInt(BatteryMonitorConstant.DISCHARGE_LIMIT, dischargeLimit);
        JobInfo dischargeJobInfo = new JobInfo.Builder(BatteryMonitorConstant.DISCHARGING_JOB_ID, componentName)
                .setPersisted(true)
                .setPeriodic(15*60*1000)
                .setExtras(persistableBundle)
                .build();
        JobScheduler jobScheduler = (JobScheduler)getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = jobScheduler.schedule(dischargeJobInfo);
        if(resultCode == JobScheduler.RESULT_SUCCESS){
            Log.d(TAG, "Job Scheduled discharging");
        } else {
            Log.d(TAG, "Job Scheduling failed");
        }
    }

    private void createNotificationChannel() {
        NotificationChannel channel1 = new NotificationChannel(
                BatteryMonitorConstant.CHANNEL_ID_CHARGE,
                "Channel 1",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel1.setDescription("This is Channel 1");
        channel1.setSound(null, null);
        channel1.setLockscreenVisibility(NotificationCompat.PRIORITY_HIGH);
        channel1.setVibrationPattern(new long[] {2000, 2000, 2000, 2000});
        channel1.enableVibration(true);

        NotificationChannel channel2 = new NotificationChannel(
                BatteryMonitorConstant.CHANNEL_ID_DISCHARGE,
                "Channel 2",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel2.setDescription("This is Channel 2");
        channel2.setSound(null, null);
        channel2.setLockscreenVisibility(NotificationCompat.PRIORITY_HIGH);
        channel2.setVibrationPattern(new long[] {2000, 2000, 2000, 2000});
        channel2.enableVibration(true);

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel1);
        manager.createNotificationChannel(channel2);
    }

    //    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
//            //textView.setText("Battery Percentage: " + level);
//            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
//            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status ==
//                    BatteryManager.BATTERY_STATUS_FULL;
//            if (isCharging) {
//                Toast.makeText(getApplicationContext(), "Charger connected",
//                        Toast.LENGTH_SHORT).show();
//                scheduleChargingJob();
//            }
//            else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
//                Toast.makeText(getApplicationContext(), "Charger disconnected",
//                        Toast.LENGTH_SHORT).show();
//                scheduleDischargingJob();
//            }
//        }
//    };
}