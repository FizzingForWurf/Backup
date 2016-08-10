package itrans.itranstest;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.math.BigDecimal;

public class MyLocationTrackingService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private boolean hasArrived = false;
    private boolean isNoticeActive = false;

    private BigDecimal result;

    //selected data
    private String destinationLatLng;
    private String alertRadius;
    private String alertRingTone;
    private String alertTitle;
    private LatLng selectedLocation;
    private float distance;

    //Google API client stuff
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
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
        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();

        //Toast.makeText(this, "Service started!", Toast.LENGTH_SHORT).show();
        alertRadius = intent.getStringExtra("AlertRadius");
        destinationLatLng = intent.getStringExtra("AlertDestination");
        alertRingTone = intent.getStringExtra("AlertRingTone");
        alertTitle = intent.getStringExtra("AlertTitle");

        String[] latANDlong =  destinationLatLng.split(",");
        double latitude = Double.parseDouble(latANDlong[0]);
        double longitude = Double.parseDouble(latANDlong[1]);
        selectedLocation = new LatLng(latitude, longitude);

        result = round(distance/1000 , 2);
        createNotification(result);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        buildGoogleApiClient();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        deleteNotification();
        stopLocationUpdate();
        //Toast.makeText(this, "Service stopped.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdate();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

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
            updateArrivedNotification();
            startAlarm();
        }
    }

    private void startAlarm() {
        AlarmReceiver alarm = new AlarmReceiver();
        alarm.StartAlarm(this, alertRingTone);
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

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private void startLocationUpdate() {
        createLocationRequest();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(7000);
        mLocationRequest.setFastestInterval(3000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setSmallestDisplacement(10);
    }

    private void stopLocationUpdate() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
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

        Intent resultIntent = new Intent(this, MainActivity.class);

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
                    .setOngoing(false);;
            notificationManager.notify(noticeID, noticeBuilder.build());
        }
    }

    private void deleteNotification(){
        notificationManager.cancel(noticeID);
        isNoticeActive = false;
    }
}
