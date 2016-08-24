package itrans.itranstest;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.transition.Slide;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.ArrayList;

import static android.widget.Toast.LENGTH_SHORT;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
        AbsListView.OnScrollListener, NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback{

    //Google Maps stuff
    private GoogleMap map;
    private LocationManager locationManager;
    private Location mLastLocation;
    private LatLng LastLatLng;
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
    private boolean isRestoreDestinationMarkerCalled = false;
    private boolean isRestoreSwitchCalled = false;
    private int positionOfActivatedSwitch = -1;
    private int currentPosition = -1;

    //tracking location for alarm
    private String returnedTitle = null;
    private String returnedLatLong = null;
    private String returnedRadius = null;
    private String returnedDestination = null;
    private float distance;

    private SharedPreferences prefs;
    int number;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("iTrans");

        tvtesting = (TextView) findViewById(R.id.tvtesting);

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
        registerForContextMenu(lvDestinations);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        FloatingActionButton AddDestinationFab = (FloatingActionButton) findViewById(R.id.AddDestinationFab);
        AddDestinationFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (lvDestinations.getCount() >= 4) {
                    Toast.makeText(MainActivity.this, "Maximum number of alarms reached", LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(MainActivity.this, AddDestination.class);
                    startActivity(intent);
                }
            }
        });

        setupWindowAnimations();
    }

    private void setupWindowAnimations() {
        Slide slide = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            slide = new Slide();
        }
        if (slide != null) {
            slide.setDuration(5000);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setExitTransition(new Fade());
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        View something = lvDestinations.getChildAt(position);//getViewByPosition(position, lvDestinations);
        Switch alarmSwitch = (Switch) something.findViewById(R.id.alarmSwitch);
        TextView tvDistance = (TextView) something.findViewById(R.id.tvAlarmDistance);
        if (alarmSwitch.isChecked()) {
            //clears the map, then switch off the active switch, then reset the destination variables and distance display
            //then change the mapCamera back to current location
            map.clear();
            alarmSwitch.setChecked(false);
            turnOffSwitch();
            mCurrLocationMarker = map.addMarker(new MarkerOptions().position(LastLatLng).title("You are here"));
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LastLatLng, 16));
            tvDistance.setText("Distance left:");
            isServiceRunning = isMyServiceRunning(MyLocationTrackingService.class);
            if (isServiceRunning) {
                stopService(new Intent(this, MyLocationTrackingService.class));
            }
        } else {
            if (!isOneSwitchChecked) {
                if (statusCheck()) {
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
                        if (mLastLocation != null) {
                            Toast.makeText(getApplicationContext(), "You are already near your destination!", Toast.LENGTH_SHORT).show();
                        }
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
                            serviceIntent.putExtra("AlertTitle", returnedTitle);
                            startService(serviceIntent);

                            AddDestinationMarkerOnMap(returnedLatLong, returnedRadius);
                        }
                        String hi = "Distance left: " + result + "km";
                        tvDistance.setText(hi);
                    }
                }else{
                    checkForGPS();
                }
            } else {
                alarmSwitch.setChecked(false);
                positionOfActivatedSwitch = number - 1;
                Toast.makeText(getApplicationContext(), "You can only set one alarm.", Toast.LENGTH_SHORT).show();
            }
        }
        something.refreshDrawableState();
    }

    private void checkForGPS(){
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!gps_enabled) {
            //gps is enabled, not network
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Location services disabled");
            builder.setCancelable(false);
            builder.setMessage("iTrans requires location services to be enabled to function properly." +
                    " Your GPS seems to be disabled, please enable location services before launching iTrans again.")
                    .setCancelable(false)
                    .setPositiveButton("ENABLE", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("CLOSE APP", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                            MainActivity.this.finish();
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();

            Button positiveButton = alert.getButton(DialogInterface.BUTTON_POSITIVE);
            Button negativeButton = alert.getButton(DialogInterface.BUTTON_NEGATIVE);
            positiveButton.setTextColor(Color.parseColor("#2196F3"));
            negativeButton.setTextColor(Color.parseColor("#2196F3"));
        }
    }

    private void turnOffSwitch() {
        positionOfActivatedSwitch = -1;
        prefs.edit().putInt("currentSelectedSwitch", -1).apply();
        isOneSwitchChecked = false;
        returnedTitle = null;
        returnedLatLong = null;
        returnedRadius = null;
        returnedDestination = null;
    }

    public View getViewByPosition(int pos, ListView listView) {
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

    public float checkDistanceFromDestination() {
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
        } else {
            Toast.makeText(getApplicationContext(), "Unable to find location.", Toast.LENGTH_SHORT).show();
        }
        return distance;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {

        return false;
    }

    public boolean statusCheck() {
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

        return !(!gps_enabled || !network_enabled);
    }

    private void checkForGPSorNetworkConnection() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!gps_enabled && haveNetworkConnection()){
            //gps is enabled, not network
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Location services disabled");
            builder.setCancelable(false);
            builder.setMessage("iTrans requires location services to be enabled to function properly." +
                    " Your GPS seems to be disabled, please enable location services before launching iTrans again.")
                    .setCancelable(false)
                    .setPositiveButton("ENABLE", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("CLOSE APP", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                            MainActivity.this.finish();
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();

            Button positiveButton = alert.getButton(DialogInterface.BUTTON_POSITIVE);
            Button negativeButton = alert.getButton(DialogInterface.BUTTON_NEGATIVE);
            positiveButton.setTextColor(Color.parseColor("#2196F3"));
            negativeButton.setTextColor(Color.parseColor("#2196F3"));
        }else if (gps_enabled && !haveNetworkConnection()){
            //network enabled, not gps
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("No network connection");
            builder.setCancelable(false);
            builder.setMessage("iTrans requires network connection to be enabled to function properly." +
                    " Please ensure that you have stable network connection before launching iTrans again.")
                    .setCancelable(false)
                    .setPositiveButton("OPEN SETTINGS", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("CLOSE APP", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                            MainActivity.this.finish();
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();

            Button positiveButton = alert.getButton(DialogInterface.BUTTON_POSITIVE);
            Button negativeButton = alert.getButton(DialogInterface.BUTTON_NEGATIVE);
            positiveButton.setTextColor(Color.parseColor("#2196F3"));
            negativeButton.setTextColor(Color.parseColor("#2196F3"));
        }else if (!gps_enabled && !haveNetworkConnection()){
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Both location services and network connection are disabled");
            builder.setCancelable(false);
            builder.setMessage("iTrans requires both location services and network connection to be enabled to function properly." +
                    " Please enable these in the settings before launching iTrans again.")
                    .setCancelable(false)
                    .setPositiveButton("OPEN SETTINGS", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("CLOSE APP", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                            MainActivity.this.finish();
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();

            Button positiveButton = alert.getButton(DialogInterface.BUTTON_POSITIVE);
            Button negativeButton = alert.getButton(DialogInterface.BUTTON_NEGATIVE);
            positiveButton.setTextColor(Color.parseColor("#2196F3"));
            negativeButton.setTextColor(Color.parseColor("#2196F3"));
        }
    }

    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

    private void populateListViewFromDatabase() {
        DBAdapter db = new DBAdapter(this);
        db.open();
        Cursor c = db.retrieveAllEntriesCursor();

        String[] from = {DBAdapter.ENTRY_TITLE, DBAdapter.ENTRY_DESTINATION};
        int[] to = {R.id.tvAlarmTitle, R.id.tvAlarmDestination};

        SimpleCursorAdapter myCursorAdapter = new SimpleCursorAdapter(this,
                R.layout.custom_alert_destination_row, c, from, to);
        db.close();
        lvDestinations.setAdapter(myCursorAdapter);
    }

    private void toggleVisibility() {
        if (lvDestinations.getCount() > 0) {
            tvtesting.setVisibility(View.GONE);
            lvDestinations.setVisibility(View.VISIBLE);
        } else {
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

        if (statusCheck()) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            Criteria c = new Criteria();
            c.setAccuracy(Criteria.ACCURACY_FINE);
            c.setAltitudeRequired(false);
            c.setBearingRequired(false);
            c.setCostAllowed(true);
            c.setPowerRequirement(Criteria.POWER_LOW);

            try {
                String provider = locationManager.getBestProvider(c, true);
                mLastLocation = locationManager.getLastKnownLocation(provider);
                if (mLastLocation != null) {
                    LastLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                }

                locationManager.requestLocationUpdates(provider, 2000, 10, locationListener);
            } catch (SecurityException e) {
                Toast.makeText(getApplicationContext(), "Cannot detect...", Toast.LENGTH_SHORT).show();
            }
        }else{
            checkForGPSorNetworkConnection();
        }
        //prefs.edit().putInt("currentSelectedSwitch", -1).apply(); //this is for debugging
        positionOfActivatedSwitch = prefs.getInt("currentSelectedSwitch", -1);
        if (positionOfActivatedSwitch != -1) {
            startRestoreState(positionOfActivatedSwitch);
        }
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(locationListener);
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("currentSelectedSwitch", positionOfActivatedSwitch);
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(locationListener);
        }
    }

    private void startRestoreState(int position) {
        DBAdapter db = new DBAdapter(this);
        db.open();
        number = position + 1;
        String numberInList = Integer.toString(number);
        returnedTitle = db.getTitle(numberInList);
        returnedLatLong = db.getLatLng(numberInList);
        returnedRadius = db.getRadius(numberInList);
        db.close();

        isRestoreDestinationMarkerCalled = true;
        isRestoreSwitchCalled = true;

        if (positionOfActivatedSwitch >= 4) {
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

    private void AddDestinationMarkerOnMap(String receivedLatLng, String alertRadius) {
        LatLngBounds bounds = null;
        int radius = Integer.parseInt(alertRadius);
        String[] latANDlong = receivedLatLng.split(",");
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
            bounds = builder.build();
        }

        final LatLngBounds finalBounds = bounds;
        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(finalBounds, zoom_padding));
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);

        if (mLastLocation != null) {
            LastLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            if (mCurrLocationMarker != null) {
                mCurrLocationMarker.remove();
            }
            mCurrLocationMarker = map.addMarker(new MarkerOptions()
                    .position(LastLatLng)
                    .title("You are here")
                    .icon(BitmapDescriptorFactory.defaultMarker()));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(LastLatLng)
                    .zoom(16)
                    .build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

        if (isRestoreDestinationMarkerCalled) {
            AddDestinationMarkerOnMap(returnedLatLong, returnedRadius);
            isRestoreDestinationMarkerCalled = false;
        }
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
        //getMenuInflater().inflate(R.menu.main, menu);
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

        if (id == R.id.nav_feedback) {
            Intent a = new Intent(MainActivity.this, Feedback.class);
            startActivity(a);
        } else if (id == R.id.nav_about) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("About us");
            builder.setCancelable(true);
            builder.setMessage("We are iTans, a group of Secondary 3s from Hwa Chong Institution, " +
                    "and we present an app that makes your everyday commuting much easier.");

            builder.setPositiveButton(
                    "Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            AlertDialog alert = builder.create();
            alert.show();
        } else if (id == R.id.nav_settings) {
            Intent c = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(c);
        } else if (id == R.id.nav_nearbyBus) {
            Intent i = new Intent(MainActivity.this, NearbyBusStops.class);
            if (mLastLocation != null) {
                i.putExtra("LastLat", mLastLocation.getLatitude());
                i.putExtra("LastLng", mLastLocation.getLongitude());
            }
            startActivity(i);

        } else if (id == R.id.nav_busStopsSearch) {
            Intent s = new Intent(MainActivity.this, BusSearch.class);
            startActivity(s);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.delete_edit_alarm_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.editAlarmMenu:
                AdapterView.AdapterContextMenuInfo information = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
                DBAdapter db1 = new DBAdapter(this);
                db1.open();
                String updateTitle = db1.getTitle(Integer.toString(information.position + 1));
                String updateDestination = db1.getDestination(Integer.toString(information.position + 1));
                String updateLatLng = db1.getLatLng(Integer.toString(information.position + 1));
                String updateRadius = db1.getRadius(Integer.toString(information.position + 1));
                db1.close();
                Intent i = new Intent(MainActivity.this, AddDestination.class);
                i.putExtra("updateRowNumber", information.position + 1);
                i.putExtra("updateTitle", updateTitle);
                i.putExtra("updateDestination", updateDestination);
                i.putExtra("updateLatLng", updateLatLng);
                i.putExtra("updateRadius", updateRadius);
                startActivity(i);
                break;
            case R.id.deleteAlarmMenu:
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                int positionOfDeletedEntry = info.position;
                if (positionOfDeletedEntry != positionOfActivatedSwitch) {
                    DBAdapter db = new DBAdapter(this);
                    db.open();
                    Log.e("DELETE POSITION", String.valueOf(positionOfDeletedEntry));
                    int lastEntryNumber = db.getNumberOfRows();
                    Log.e("DELETE ACTUAL", String.valueOf(lastEntryNumber));
                    int numberOfEntriesAfterDeletedEntry = lastEntryNumber - (positionOfDeletedEntry + 1);
                    db.deleteEntry(positionOfDeletedEntry + 1);
                    int testing = db.getNumberOfRows();
                    Log.e("DELETE TEST", String.valueOf(testing));

                    for (int w = 0; w < numberOfEntriesAfterDeletedEntry; w++) {
                        int positionOfEntryNeededToBeChanged = w + positionOfDeletedEntry + 2;

                        db.updateUniqueId(positionOfEntryNeededToBeChanged);
                        Log.e("DELETE W", String.valueOf(positionOfEntryNeededToBeChanged));
                    }

                    //check row sequence for debugging
                    ArrayList<String> arrayList;
                    arrayList = db.getIdList();
                    Log.e("DELETE ARRAY", String.valueOf(arrayList));

                    db.close();
                    Toast.makeText(MainActivity.this, "Entry deleted.", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(MainActivity.this, "Please turn off active alarm to delete entry.", Toast.LENGTH_SHORT).show();
                }
                populateListViewFromDatabase();
                toggleVisibility();
                break;
        }
        return super.onContextItemSelected(item);
    }

    private final android.location.LocationListener locationListener = new android.location.LocationListener() {

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
                    .build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            mCurrLocationMarker = map.addMarker(markerOptions);

            if (returnedRadius != null && returnedLatLong != null) {

                String[] latANDlong = returnedLatLong.split(",");
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

                float distanceleftInKm = distance / 1000;
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
            if (hasArrived) {
                if (positionOfActivatedSwitch > -1) {
                    View view = getViewByPosition(positionOfActivatedSwitch, lvDestinations);
                    if (view != null) {
                        if (view.getVisibility() == View.VISIBLE) {
                            Switch alarmSwitch = (Switch) view.findViewById(R.id.alarmSwitch);
                            TextView alarmDistance = (TextView) view.findViewById(R.id.tvAlarmDistance);
                            alarmDistance.setText("Distance left: ");
                            alarmSwitch.setChecked(false);
                            view.refreshDrawableState();
                        }
                    }
                }
                map.clear();
                mCurrLocationMarker = map.addMarker(new MarkerOptions()
                        .position(LastLatLng)
                        .title("You are here")
                        .icon(BitmapDescriptorFactory.defaultMarker()));
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LastLatLng, 16));
                turnOffSwitch();
                hasArrived = false;
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {

    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
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
                for (int i = 0; i < visibleItemCount; i++){
                    View loopView = lvDestinations.getChildAt(firstVisibleItem + i);
                    Switch alarmSwitch = (Switch) loopView.findViewById(R.id.alarmSwitch);
                    TextView alarmDistance = (TextView) loopView.findViewById(R.id.tvAlarmDistance);
                    if (alarmSwitch.isChecked()){
                        alarmSwitch.setChecked(false);
                        alarmDistance.setText("Distance left: ");
                        loopView.refreshDrawableState();
                    }
                }
            } else if (firstVisibleItem <= positionOfActivatedSwitch && lastVisibleItem >= positionOfActivatedSwitch) {
                //this means that the activated switch is in the view of the user
                //activateSwitchOnScroll();
                Log.e("VISIBILITY", "View selected visible");
                View activeView = getViewByPosition(positionOfActivatedSwitch, lvDestinations);
                Switch alarmSwitch = (Switch) activeView.findViewById(R.id.alarmSwitch);
                TextView alarmDistance = (TextView) activeView.findViewById(R.id.tvAlarmDistance);
                if (!alarmSwitch.isChecked()){
                    alarmSwitch.setChecked(true);
                    float distanceleftInKm = distance / 1000;
                    BigDecimal result;
                    result = round(distanceleftInKm, 2);
                    alarmDistance.setText("Distance left: " + result + "km");
                }
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
}