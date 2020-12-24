package com.coddicted.batteryMonitor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.sql.BatchUpdateException;

public class MonitorChargingJobService extends JobService {
    private static final String TAG = "MonitorChargingJobService";
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
        int chargeLimit = parameters.getExtras().getInt(BatteryMonitorConstant.CHARGE_LIMIT);
        Log.d(TAG, "chargeLimit: "+chargeLimit);

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
                    if(status == BatteryManager.BATTERY_STATUS_DISCHARGING)
                        break;

                    int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    int batteryPct = (int) (level * 100 / (float) scale);
                    Log.d(TAG, "batteryPct: " + batteryPct);
                    if (batteryPct >= chargeLimit) {
                        showNotification();
                        break;
                    } else {
                        try {
                            Thread.sleep(1 * 60 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                Log.d(TAG, "Job finished");
                jobFinished(parameters, true);
            }
        }).start();
    }


    private void showNotification(){
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = new NotificationCompat.Builder(this, BatteryMonitorConstant.CHANNEL_ID_CHARGE)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Battery Monitor")
                .setContentText("Battery Charged")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVibrate(new long[] {2000, 2000, 2000, 2000})
                .build();
        notificationManager.notify(1, notification);
    }
}
