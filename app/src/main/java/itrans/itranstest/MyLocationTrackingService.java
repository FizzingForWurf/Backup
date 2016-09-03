package itrans.itranstest;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.math.BigDecimal;

public class MyLocationTrackingService extends Service {

    private boolean hasArrived = false;
    private boolean isNoticeActive = false;

    private BigDecimal result;

    //selected data
    private String destinationLatLng;
    private String alertRadius;
    private String alertTitle;
    private LatLng selectedLocation;
    private float distance;

    //Location stuff
    private LocationManager locManager;
    private LatLng LastLatLng;
    private Location mLastLocation;

    //notification stuff
    private NotificationManager notificationManager;
    private NotificationCompat.Builder noticeBuilder;
    private int noticeID = 99;

    public MyLocationTrackingService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        alertRadius = intent.getStringExtra("AlertRadius");
        destinationLatLng = intent.getStringExtra("AlertDestination");
        alertTitle = intent.getStringExtra("AlertTitle");

        String[] latANDlong = destinationLatLng.split(",");
        double latitude = Double.parseDouble(latANDlong[0]);
        double longitude = Double.parseDouble(latANDlong[1]);
        selectedLocation = new LatLng(latitude, longitude);

        result = round(distance / 1000, 2);
        createNotification(result);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        c.setAltitudeRequired(false);
        c.setBearingRequired(false);
        c.setCostAllowed(true);
        c.setPowerRequirement(Criteria.POWER_LOW);

        try {
            String provider = locManager.getBestProvider(c, true);
            Location loc = locManager.getLastKnownLocation(provider);

            locManager.requestLocationUpdates(provider, 2000, 10, locationListener);

        } catch (SecurityException e) {
            Toast.makeText(getApplicationContext(), "Cannot detect...", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locManager != null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locManager.removeUpdates(locationListener);
        }
        deleteNotification();
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mLastLocation = location;
            LastLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

            if (destinationLatLng != null && alertRadius != null) {

                checkDistanceFromDestination();

                result = round(distance/1000 , 2);
                updateNotification(result);

                float radius = Float.parseFloat(alertRadius);
                hasArrived = (distance <= radius);
            }

            //check if arriving
            if (hasArrived){
                hasArrived = false;
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("currentSelectedSwitch", -1);
                editor.apply();
                int positionOfActivatedSwitch = prefs.getInt("currentSelectedSwitch", -1);
                Log.e("POSITION FROM SERVICE", String.valueOf(positionOfActivatedSwitch));
                updateArrivedNotification();
                startAlarm();
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            //Nothing lol
        }

        @Override
        public void onProviderEnabled(String provider) {
            //Nothing lol
        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private void startAlarm() {
        AlarmReceiver alarm = new AlarmReceiver();
        alarm.StartAlarm(this, alertTitle);
    }

    private void checkDistanceFromDestination() {
        if (LastLatLng != null) {

            Location locationDestination = new Location("Destination");
            locationDestination.setLatitude(selectedLocation.latitude);
            locationDestination.setLongitude(selectedLocation.longitude);

            distance = mLastLocation.distanceTo(locationDestination);
        }else{
            Toast.makeText(getApplicationContext(), "Please turn on location services", Toast.LENGTH_SHORT).show();
        }
    }

    public static BigDecimal round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd;
    }

    private void createNotification(BigDecimal distance) {
        isNoticeActive = true;
        noticeBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(alertTitle + " alarm")
                .setContentText("Distance left: " + distance + "km")
                .setTicker("Starting " + alertTitle + " alarm...")
                .setSmallIcon(R.drawable.ic_directions_bus_white_24dp)
                .setOngoing(true);

        Intent resultIntent = new Intent(this, Splash.class);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        noticeBuilder.setContentIntent(resultPendingIntent);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(noticeID, noticeBuilder.build());
    }

    private void updateNotification(BigDecimal distance) {
        if (isNoticeActive) {
            if (distance != null) {
                noticeBuilder.setContentText("Distance left: " + distance + "km");
            } else {
                noticeBuilder.setContentText("Distance left: ");
            }
            notificationManager.notify(noticeID, noticeBuilder.build());
        }
    }

    private void updateArrivedNotification(){
        if (isNoticeActive) {
            noticeBuilder.setContentText("You have arrived at your destination.")
                    .setSmallIcon(R.drawable.ic_access_alarms_black_24dp)
                    .setOngoing(false);
            notificationManager.notify(noticeID, noticeBuilder.build());
        }
    }

    private void deleteNotification(){
        notificationManager.cancel(noticeID);
        isNoticeActive = false;
    }
}
