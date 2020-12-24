package com.coddicted.batteryMonitor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class MonitorDischargingJobService extends JobService {
    private static final String TAG = "MonitorDischargingJobService";
    private boolean jobCancelled = false;
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "onStartJob");
        doBackgroundWork(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job Cancelled before completion");
        jobCancelled = true;
        return true;
    }

    private void doBackgroundWork(JobParameters parameters){
        int dischargeLimit = parameters.getExtras().getInt(BatteryMonitorConstant.DISCHARGE_LIMIT);
        Log.d(TAG, "dischargeLimit: "+dischargeLimit);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "doBackgroundWork");
                while(true){
                    if(jobCancelled){
                        return;
                    }
                    IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                    Intent batteryIntent = registerReceiver(null, iFilter);
                    int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status ==
                            BatteryManager.BATTERY_STATUS_FULL;
                    if(isCharging)
                        break;
                    int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    int batteryPct = (int)(level * 100 / (float)scale);
                    Log.d(TAG, "batteryPct: "+ batteryPct);
                    if(batteryPct > dischargeLimit ){
                        try {
                            Log.d(TAG, "sleep");
                            Thread.sleep(1 * 60 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        showNotification();
                        break;
                    }
                }
                Log.d(TAG, "Job finished");
                jobFinished(parameters, true);
            }
        }).start();
    }

    private void showNotification(){
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = new NotificationCompat.Builder(this, BatteryMonitorConstant.CHANNEL_ID_DISCHARGE)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Battery Monitor")
                .setContentText("Battery Low")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVibrate(new long[] {2000, 2000, 2000, 2000})
                .build();
        notificationManager.notify(2, notification);
    }
}
