package itrans.itranstest;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.AlarmClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class AlarmReceiver extends WakefulBroadcastReceiver {

    //alarm stuff
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;
    private String RingTone;
    private Ringtone ringtone;

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP, "");
        wl.acquire();

        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE); //Uri.parse(RingTone);
        if (uri != null) {
            ringtone = RingtoneManager.getRingtone(context, uri);
            ringtone.play();
        }
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                ringtone.stop();
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 3000);

        Toast.makeText(context, "Alarm !!!!!!!!!!", Toast.LENGTH_LONG).show();
        context.stopService(new Intent(context, MyLocationTrackingService.class));

        wl.release();
    }

    public void StartAlarm(Context context, String ringTone) {
        this.RingTone = ringTone;
        Toast.makeText(context, RingTone, Toast.LENGTH_SHORT).show();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND));

        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        alarmMgr.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + 1000, alarmIntent);
    }

    public void CancelAlarm(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmMgr.cancel(alarmIntent);
    }
}

