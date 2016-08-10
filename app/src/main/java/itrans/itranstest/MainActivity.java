package itrans.itranstest;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.math.BigDecimal;

import static android.widget.Toast.LENGTH_SHORT;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
        AbsListView.OnScrollListener, NavigationView.OnNavigationItemSelectedListener,OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener{

    //Google Maps stuff
    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private LatLng LastLatLng;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 10; // 10 meters
    private Marker mCurrLocationMarker;
    private int zoom_padding = 130;

    TextView tvtesting;
    ListView lvDestinations;
    NavigationView navigationView;

    //boolean variables
    private boolean isOneSwitchChecked = false;
    private boolean isNearDestinationInitially = false;
    private boolean hasArrived = false;
    private boolean isServiceRunning = false;
    private boolean isLocationUpdateActive = false;
    private boolean isRestoreDestinationMarkerCalled = false;
    private boolean isRestoreSwitchCalled = false;
    private int positionOfActivatedSwitch = -1;
    private int currentPosition = -1;

    //tracking location for alarm
    private String returnedTitle = null;
    private String returnedLatLong = null;
    private String returnedRadius = null;
    private String returnedRingTone = null;
    private String returnedDestination = null;
    private float distance;

    private SharedPreferences prefs;
    int number;

    //Alarm stuff
    private Ringtone ringtone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("iTrans");

        tvtesting = (TextView) findViewById(R.id.tvtesting);

        if (checkPlayServices()) {
            buildGoogleApiClient();
        }

        statusCheck();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentMap);
        mapFragment.getMapAsync(this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        prefs = this.getPreferences(Context.MODE_PRIVATE);

        lvDestinations = (ListView) findViewById(R.id.lvDestinations);
        lvDestinations.setOnItemLongClickListener(this);
        lvDestinations.setOnItemClickListener(this);
        lvDestinations.setOnScrollListener(this);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        FloatingActionButton AddDestinationFab = (FloatingActionButton) findViewById(R.id.AddDestinationFab);
        AddDestinationFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (lvDestinations.getCount() >= 10) {
                    Toast.makeText(MainActivity.this, "Maximum number of alarms reached", LENGTH_SHORT).show();
                }else{
                    Intent intent = new Intent(MainActivity.this, AddDestination.class);
                    intent.putExtra("LastLat", mLastLocation.getLatitude());
                    intent.putExtra("LastLng", mLastLocation.getLongitude());
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        View something = lvDestinations.getChildAt(position);//getViewByPosition(position, lvDestinations);
        Switch alarmSwitch = (Switch) something.findViewById(R.id.alarmSwitch);
        TextView tvDistance = (TextView) something.findViewById(R.id.tvAlarmDistance);
        if (alarmSwitch.isChecked()){
            //clears the map, then switch off the active switch, then reset the destination variables and distance display
            //then change the mapCamera back to current location
            map.clear();
            alarmSwitch.setChecked(false);
            turnOffSwitch();
            mCurrLocationMarker = map.addMarker(new MarkerOptions().position(LastLatLng).title("You are here"));
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LastLatLng, 16));
            tvDistance.setText("Distance left:");
            isServiceRunning = isMyServiceRunning(MyLocationTrackingService.class);
            if (isServiceRunning){
                stopService(new Intent(this, MyLocationTrackingService.class));
            }
        }else {
            if (!isOneSwitchChecked) {
                positionOfActivatedSwitch = position;
                map.clear();
                if (mCurrLocationMarker != null && LastLatLng != null) {
                    mCurrLocationMarker = map.addMarker(new MarkerOptions().position(LastLatLng));
                }

                alarmSwitch.setChecked(true);
                isOneSwitchChecked = true;

                DBAdapter db = new DBAdapter(this);
                db.open();
                number = positionOfActivatedSwitch + 1;
                String numberInList = Integer.toString(number);
                returnedTitle = db.getTitle(numberInList);
                returnedLatLong = db.getLatLng(numberInList);
                returnedRadius = db.getRadius(numberInList);
                returnedRingTone = db.getRingTone(numberInList);
                returnedDestination = db.getDestination(numberInList);
                db.close();

                float distanceleftInMeters = checkDistanceFromDestination();
                float distanceleftInKm = distanceleftInMeters / 1000;
                BigDecimal result;
                result = round(distanceleftInKm, 2);

                float radius = Float.parseFloat(returnedRadius);
                if (distance <= radius) {
                    isNearDestinationInitially = true;
                }

                if (isNearDestinationInitially) {
                    isNearDestinationInitially = false;
                    Toast.makeText(getApplicationContext(), "You are already near your destination!", Toast.LENGTH_SHORT).show();
                    //trigger alarm and set switch to off
                    alarmSwitch.setChecked(false);
                    turnOffSwitch();
                    tvDistance.setText("Distance left:");
                } else {
                    //Start the location tracking service
                    isServiceRunning = isMyServiceRunning(MyLocationTrackingService.class);
                    if (!isServiceRunning) {
                        Intent serviceIntent = new Intent(this, MyLocationTrackingService.class);
                        serviceIntent.putExtra("AlertRadius", returnedRadius);
                        serviceIntent.putExtra("AlertDestination", returnedLatLong);
                        serviceIntent.putExtra("AlertRingTone", returnedRingTone);
                        serviceIntent.putExtra("AlertTitle", returnedTitle);
                        startService(serviceIntent);

                        AddDestinationMarkerOnMap(returnedLatLong, returnedRadius);
                    }
                }
                String hi = "Distance left: " + result + "km";
                tvDistance.setText(hi);
            }else{
                alarmSwitch.setChecked(false);
                positionOfActivatedSwitch = number - 1;
                Toast.makeText(getApplicationContext(), "You can only set one alarm.", Toast.LENGTH_SHORT).show();
            }
        }
        something.refreshDrawableState();
    }



    private void turnOffSwitch(){
        positionOfActivatedSwitch = -1;
        prefs.edit().putInt("currentSelectedSwitch", -1).apply();
        isOneSwitchChecked = false;
        returnedTitle = null;
        returnedLatLong = null;
        returnedRadius = null;
        returnedRingTone = null;
        returnedDestination = null;
    }

    public View getViewByPosition(int pos, ListView listView) {
//        final int firstListItemPosition = listView.getFirstVisiblePosition();
//        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;
//
//        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
//            return listView.getAdapter().getView(pos, null, listView);
//        } else {
//            final int childIndex = pos - firstListItemPosition;
//            return listView.getChildAt(childIndex);
//        }
        return listView.getChildAt(pos - listView.getFirstVisiblePosition());
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static BigDecimal round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd;
    }

    public float checkDistanceFromDestination(){
        if (LastLatLng != null) {
            //LatLng for set destination
            String[] latANDlong = returnedLatLong.split(",");
            double latitudeDestination = Double.parseDouble(latANDlong[0]);
            double longitudeDestination = Double.parseDouble(latANDlong[1]);

            //LatLng for current location
            double currentLatitude = LastLatLng.latitude;
            double currentLongitude = LastLatLng.longitude;

            Location currentLocation = new Location("Current Location");
            currentLocation.setLatitude(currentLatitude);
            currentLocation.setLongitude(currentLongitude);

            Location locationDestination = new Location("Destination");
            locationDestination.setLatitude(latitudeDestination);
            locationDestination.setLongitude(longitudeDestination);

            distance = currentLocation.distanceTo(locationDestination);
        }else{
            Toast.makeText(getApplicationContext(), "Please turn on location services", Toast.LENGTH_SHORT).show();
        }
        return distance;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {

        return false;
    }

    public void statusCheck(){
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!gps_enabled || !network_enabled) {
            buildAlertMessageNoGps();
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog,  final int id) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void populateListViewFromDatabase() {
        DBAdapter db = new DBAdapter(this);
        db.open();
        Cursor c = db.retrieveAllEntriesCursor();

        String[] from = {DBAdapter.ENTRY_TITLE, DBAdapter.ENTRY_DESTINATION};
        int[] to = {R.id.tvAlarmTitle, R.id.tvAlarmDestination};

        SimpleCursorAdapter myCursorAdapter = new SimpleCursorAdapter(this,
                R.layout.custom_alert_destination_row,c,from,to);
        db.close();
        lvDestinations.setAdapter(myCursorAdapter);
    }

    private void toggleVisibility()	{
        if(lvDestinations.getCount()>0) 	{
            tvtesting.setVisibility(View.GONE);
            lvDestinations.setVisibility(View.VISIBLE);
        }
        else {
            lvDestinations.setVisibility(View.GONE);
            tvtesting.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        populateListViewFromDatabase();
        toggleVisibility();
        checkPlayServices();

        //prefs.edit().putInt("currentSelectedSwitch", -1).apply(); //this is for debugging
        positionOfActivatedSwitch = prefs.getInt("currentSelectedSwitch", -1);
        isLocationUpdateActive = prefs.getBoolean("isLocationUpdateActive", false);
        if (positionOfActivatedSwitch != -1){
            startRestoreState(positionOfActivatedSwitch);
        }

        // if (!isLocationUpdateActive){
        //startLocationUpdates();
        //isLocationUpdateActive = true;
        prefs.edit().putBoolean("isLocationUpdateActive", isLocationUpdateActive).apply();
        //}

        //String hi = String.valueOf(isLocationUpdateActive);
        //Toast.makeText(getApplicationContext(), hi, Toast.LENGTH_SHORT).show();
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
            isLocationUpdateActive = false;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("currentSelectedSwitch", positionOfActivatedSwitch);
        editor.putBoolean("isLocationUpdateActive", isLocationUpdateActive);
        editor.apply();
    }

    private void startRestoreState(int position){
        DBAdapter db = new DBAdapter(this);
        db.open();
        number = position + 1;
        String numberInList = Integer.toString(number);
        returnedTitle = db.getTitle(numberInList);
        returnedLatLong = db.getLatLng(numberInList);
        returnedRadius = db.getRadius(numberInList);
        returnedRingTone = db.getRingTone(numberInList);
        db.close();

        isRestoreDestinationMarkerCalled = true;
        isRestoreSwitchCalled = true;

        if (positionOfActivatedSwitch >= 3) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    try {
                        int h1 = lvDestinations.getHeight();
                        lvDestinations.smoothScrollToPositionFromTop(positionOfActivatedSwitch, h1 / 2, 500);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 500);
        }
    }

    protected void startLocationUpdates() {
        createLocationRequest();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    private void AddDestinationMarkerOnMap(String receivedLatLng, String alertRadius) {
        int radius = Integer.parseInt(alertRadius);
        String[] latANDlong =  receivedLatLng.split(",");
        double latitude = Double.parseDouble(latANDlong[0]);
        double longitude = Double.parseDouble(latANDlong[1]);
        LatLng selectedLocation = new LatLng(latitude, longitude);

        map.addMarker(new MarkerOptions()
                .position(selectedLocation)
                .icon(BitmapDescriptorFactory.defaultMarker()));

        map.addCircle(new CircleOptions()
                .center(selectedLocation)
                .radius(radius)
                .fillColor(0x550000FF)
                .strokeColor(Color.BLUE)
                .strokeWidth(10.0f));

        double zoomRadius = radius + radius / 2;
        double scale = zoomRadius / 500;
        //zoomLevel = (int) (16 - Math.log(scale) / Math.log(2));

        if (LastLatLng != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(LastLatLng);
            builder.include(selectedLocation);
            LatLngBounds bounds = builder.build();
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, zoom_padding));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (isRestoreDestinationMarkerCalled){
            AddDestinationMarkerOnMap(returnedLatLong, returnedRadius);
            isRestoreDestinationMarkerCalled = false;
        }
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_nearbyBus){
            Intent i = new Intent(MainActivity.this, NearbyBusStops.class);
            i.putExtra("LastLat", mLastLocation.getLatitude());
            i.putExtra("LastLng", mLastLocation.getLongitude());
            startActivity(i);

        }else if (id == R.id.nav_busStopsSearch){
//            Intent i = new Intent(MainActivity.this, BusNumberSearch.class);
//            startActivity(i);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();
        isLocationUpdateActive = true;
        prefs.edit().putBoolean("isLocationUpdateActive", isLocationUpdateActive).apply();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        LastLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(LastLatLng);
        markerOptions.title("You are here");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker());

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(LastLatLng)
                .zoom(16)
                //.tilt(30)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        mCurrLocationMarker = map.addMarker(markerOptions);

        if (returnedRadius != null && returnedLatLong != null) {

            String[] latANDlong =  returnedLatLong.split(",");
            double latitude = Double.parseDouble(latANDlong[0]);
            double longitude = Double.parseDouble(latANDlong[1]);
            LatLng selectedLocation = new LatLng(latitude, longitude);

            if (LastLatLng != null) {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(LastLatLng);
                builder.include(selectedLocation);
                LatLngBounds bounds = builder.build();
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, zoom_padding));
            }

            distance = checkDistanceFromDestination();

            float distanceleftInKm = distance/1000;
            BigDecimal result;
            result = round(distanceleftInKm, 2);

            if (positionOfActivatedSwitch > -1) {
                View view = getViewByPosition(positionOfActivatedSwitch, lvDestinations);
                if (view.getVisibility() == View.VISIBLE) {
                    TextView alarmDistance = (TextView) view.findViewById(R.id.tvAlarmDistance);
                    Switch alarmSwitch = (Switch) view.findViewById(R.id.alarmSwitch);
                    alarmDistance.setText("Distance left: " + result + "km");
                    if (isRestoreSwitchCalled) {
                        alarmSwitch.setChecked(true);
                        isRestoreSwitchCalled = false;
                    }
                    view.refreshDrawableState();
                }
            }
            float radius = Float.parseFloat(returnedRadius);
            hasArrived = (distance <= radius);
        }

        //check if arriving
        if (hasArrived){
            if (positionOfActivatedSwitch > -1) {
                View view = getViewByPosition(positionOfActivatedSwitch, lvDestinations);
                if (view.getVisibility() == View.VISIBLE) {
                    Switch alarmSwitch = (Switch) view.findViewById(R.id.alarmSwitch);
                    TextView alarmDistance = (TextView) view.findViewById(R.id.tvAlarmDistance);
                    alarmDistance.setText("Distance left: ");
                    alarmSwitch.setChecked(false);
                    view.refreshDrawableState();
                }
            }

            turnOffSwitch();
            hasArrived = false;
        }
    }

    private void startAlarm(){
        Uri uri = Uri.parse(returnedRingTone);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), uri);
        ringtone.play();

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You have arrived!")
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog,  final int id) {
                        ringtone.stop();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {

    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//        if (selectedAlarmView != null && selectedAlarmView.getVisibility() != View.VISIBLE){
//            Log.e("HELLOO WORLD", "SELECTED NOT VISIBLE");
//        }
//        if (selectedAlarmView == null){
//            Log.e("HELLOO WORLD", "NULL");
//        }
        int lastVisibleItem = (firstVisibleItem + visibleItemCount) - 1;
        if (positionOfActivatedSwitch >= 0) {
            if (firstVisibleItem < positionOfActivatedSwitch && lastVisibleItem < positionOfActivatedSwitch ||
                    firstVisibleItem > positionOfActivatedSwitch) {
                //this means that the activated switch is either below or above the current view
                //disableSwitchOnScroll();
                Log.e("VISIBILITY", "View selected NOT visible");
//                View fakeView = getViewByPosition(positionOfActivatedSwitch, lvDestinations);
//                Switch alarmSwitch = (Switch) fakeView.findViewById(R.id.alarmSwitch);
//                TextView alarmDistance = (TextView) fakeView.findViewById(R.id.tvAlarmDistance);
//                alarmDistance.setText("Distance left: ");
//                alarmSwitch.setChecked(false);
//                fakeView.refreshDrawableState();
            } else if (firstVisibleItem <= positionOfActivatedSwitch && lastVisibleItem >= positionOfActivatedSwitch) {
                //this means that the activated switch is in the view of the user
                //activateSwitchOnScroll();
                Log.e("VISIBILITY", "View selected visible");
            }
        }
    }

    private void activateSwitchOnScroll() {
        positionOfActivatedSwitch = currentPosition;
//        BigDecimal result;
//        result = round(distance/2, 2);
//
//        View view = getViewByPosition(positionOfActivatedSwitch, lvDestinations);//lvDestinations.getChildAt(positionOfActivatedSwitch);
//        Switch alarmSwitch = (Switch) view.findViewById(R.id.alarmSwitch);
//        TextView alarmDistance = (TextView) view.findViewById(R.id.tvAlarmDistance);
//        alarmDistance.setText("Distance left: " + result + "km");
//        alarmSwitch.setChecked(true);
//        view.refreshDrawableState();
    }

    private void disableSwitchOnScroll() {
        currentPosition = positionOfActivatedSwitch;
        positionOfActivatedSwitch = -1;
    }

//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//
//        // Checks the orientation of the screen
//        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//
//        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
//
//        }
//    }
}

