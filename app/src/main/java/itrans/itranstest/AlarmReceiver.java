package itrans.itranstest;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.view.WindowManager;

import java.util.Calendar;

public class AlarmReceiver extends WakefulBroadcastReceiver {

    //alarm stuff
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP, "");
        wl.acquire();

        Intent Intent = new Intent("android.intent.action.MAIN");
        Intent.setClass(context, AlarmRing.class);
        Intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        Intent.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED +
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD +
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON +
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        context.startActivity(Intent);

        context.stopService(new Intent(context, MyLocationTrackingService.class));

        wl.release();
    }

    public void StartAlarm(Context context, String alarmTitle) {
        startNotification(alarmTitle, context);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND));

        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        alarmMgr.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
    }

    private void startNotification(String title, Context context){
        NotificationCompat.Builder noticeBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(title + " alarm")
                .setContentText("You have arrived at your destination.")
                .setSmallIcon(R.drawable.ic_access_alarms_black_24dp);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(100, noticeBuilder.build());
    }
}

