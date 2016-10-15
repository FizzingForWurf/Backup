package itrans.itranstest;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.SearchSuggestionsAdapter;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import itrans.itranstest.Internet.MyApplication;
import itrans.itranstest.Internet.VolleySingleton;

public class BusSearch extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private Marker selectedMarker;
    private LocationManager locationManager;
    private Location mLastLocation;
    private LatLng mLastLatLng;

    private FloatingSearchView mSearchView;
    private FloatingActionButton fab;

    private List<String> BusServiceNumberList = new ArrayList<String>();
    private List<BusRoutes> BusSearchSuggestionList = new ArrayList<>();

    private ArrayList<String> direction1 = new ArrayList<>();
    private ArrayList<String> direction2 = new ArrayList<>();
    private ArrayList<NearbySuggestions> busStopsList = new ArrayList<>();
    private LatLngBounds.Builder builder = new LatLngBounds.Builder();

    private VolleySingleton volleySingleton;
    private RequestQueue requestQueue;

    private String selectedServiceNumber;
    private String timeInMin;
    private long diff;
    private String selectedBusStopName;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Search Bus");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.busSearchMap);
        mapFragment.getMapAsync(this);

        mSearchView = (FloatingSearchView) findViewById(R.id.bus_search_floating_search_view);

        volleySingleton = VolleySingleton.getInstance();
        requestQueue = volleySingleton.getRequestQueue();

        MyApplication ma = MyApplication.getInstance();
        BusServiceNumberList = ma.retrieveAll(getApplicationContext());

        fab = (FloatingActionButton) findViewById(R.id.bus_search_fab);

        mSearchView.setOnHomeActionClickListener(new FloatingSearchView.OnHomeActionClickListener() {
            @Override
            public void onHomeClicked() {
                BusSearch.this.finish();
            }
        });

        mSearchView.setOnQueryChangeListener(new FloatingSearchView.OnQueryChangeListener() {
            @Override
            public void onSearchTextChanged(String oldQuery, String newQuery) {
                if (!BusSearchSuggestionList.isEmpty()) {
                    BusSearchSuggestionList.clear();
                }
                if (!oldQuery.equals("") && newQuery.equals("")) {
                    mSearchView.clearSuggestions();
                } else {
                    mSearchView.showProgress();
                    List<String> filteredList = Lists.newArrayList(Collections2.filter(BusServiceNumberList,
                            Predicates.containsPattern(newQuery)));
                    for (int i = 0; i < filteredList.size(); i++) {
                        if (filteredList.get(i).startsWith(newQuery)) {
                            BusSearchSuggestionList.add(new BusRoutes(filteredList.get(i)));
                            if (BusSearchSuggestionList.size() == 12) {
                                break;
                            }
                        }
                    }
                    mSearchView.swapSuggestions(BusSearchSuggestionList);
                    mSearchView.hideProgress();
                }

            }
        });

        mSearchView.setOnBindSuggestionCallback(new SearchSuggestionsAdapter.OnBindSuggestionCallback() {
            @Override
            public void onBindSuggestion(View suggestionView, ImageView leftIcon, TextView textView,
                                         SearchSuggestion item, int itemPosition) {
                String text = textView.getText().toString();
                String newText = text.toUpperCase();
                textView.setText(newText);
            }
        });

        mSearchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {
                if (map != null) {
                    map.clear();
                }
                direction1.clear();
                direction2.clear();
                busStopsList.clear();
                builder = new LatLngBounds.Builder();

                if (progressDialog == null) {
                    progressDialog = new ProgressDialog(BusSearch.this);
                    progressDialog.setMessage("Calculating...");
                    progressDialog.setCancelable(false);
                }
                progressDialog.show();

                for (int i = 0; i < BusServiceNumberList.size(); i++) {
                    Log.e("BUS SERVICES IN LIST", String.valueOf(BusServiceNumberList.get(i)));
                    String clickedBusService = searchSuggestion.getBody().toUpperCase();
                    if (BusServiceNumberList.get(i).equals(clickedBusService)) {
                        selectedServiceNumber = BusServiceNumberList.get(i);
                        BusServiceDBAdapter db = new BusServiceDBAdapter(getApplicationContext());
                        db.open();
                        String direction_one_string = db.getdirectionone(i + 1);
                        String direction_two_string = db.getdirectiontwo(i + 1);

                        String something = direction_one_string.substring(direction_one_string.indexOf("[") + 1, direction_one_string.indexOf("]"));
                        String[] resplit = something.split( ", ");
                        for (String s : resplit) {
                            direction1.add(s);
                            System.out.println(s);
                        }

                        if (!direction_two_string.equals("LOOP")) {
                            String noBraces = direction_two_string.substring(direction_two_string.indexOf("[") + 1, direction_two_string.indexOf("]"));
                            String[] split = noBraces.split( ", ");
                            for (String a : split) {
                                direction2.add(a);
                                System.out.println(a);
                            }
                        }else{
                            direction2.clear();
                        }

                        db.close();
                        break;
                    }
                }

                mSearchView.setSearchFocused(false);
                if (direction1.size() > 50){
                    Thread thread = new Thread(){
                        public void run(){
                            getBusStops();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    for (int w = 0; w < busStopsList.size(); w++) {
                                        createMarker(busStopsList.get(w).getBusStopName(), busStopsList.get(w).getBusStopID(),
                                                busStopsList.get(w).getBusStopLat(), busStopsList.get(w).getBusStopLng());
                                    }
                                    LatLngBounds bounds = builder.build();
                                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                                    if (progressDialog != null && progressDialog.isShowing()) {
                                        progressDialog.dismiss();
                                    }
                                }
                            });
                        }
                    };
                    thread.start();
                }else {
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Thread thread = new Thread(){
                                public void run(){
                                    getBusStops();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            for (int w = 0; w < busStopsList.size(); w++) {
                                                createMarker(busStopsList.get(w).getBusStopName(), busStopsList.get(w).getBusStopID(),
                                                        busStopsList.get(w).getBusStopLat(), busStopsList.get(w).getBusStopLng());
                                            }
                                            LatLngBounds bounds = builder.build();
                                            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                                            if (progressDialog != null && progressDialog.isShowing()) {
                                                progressDialog.dismiss();
                                            }
                                        }
                                    });
                                }
                            };
                            thread.start();
                        }
                    }, 1000);
                }
            }

            @Override
            public void onSearchAction(String currentQuery) {

            }
        });
    }

    private void getBusStops(){
        BusNumberDBAdapter db = new BusNumberDBAdapter(getApplicationContext());
        db.open();
        for (int e = 0; e < direction1.size(); e++) {
            String stopID = direction1.get(e);
            char c = stopID.charAt(0);
            if (!(c >= 'A' && c <= 'Z')){
                Log.e("BUS Number Test", stopID);
                String stopLatLngString = db.getBusStopLatLng(stopID);
                String stopName = db.getBusStopName(stopID);
                if (stopID.length() < 5){
                    stopID = "0" + stopID;
                }
                Log.e("LATLNG", stopID + ", " + stopLatLngString + ", " + stopName);

                String withoutBraces = stopLatLngString.substring(stopLatLngString.indexOf("(") + 1, stopLatLngString.indexOf(")"));
                String[] split = withoutBraces.split(",");
                Double lat = Double.parseDouble(split[0]);
                Double lon = Double.parseDouble(split[1]);
                LatLng coordinates = new LatLng(lat, lon);
                builder.include(coordinates);

                NearbySuggestions busStop = new NearbySuggestions();
                busStop.setBusStopName(stopName);
                busStop.setBusStopID(stopID);
                busStop.setBusStopLat(lat);
                busStop.setBusStopLng(lon);

                busStopsList.add(busStop);
            }
        }
        if (!direction2.isEmpty()) {
            for (int e = 0; e < direction2.size(); e++) {
                String stopID = direction2.get(e);
                char c = stopID.charAt(0);
                if (!(c >= 'A' && c <= 'Z')) {
                    String stopLatLngString = db.getBusStopLatLng(stopID);
                    String stopName = db.getBusStopName(stopID);
                    if (stopID.length() < 5) {
                        stopID = "0" + stopID;
                    }

                    Log.e("LATLNG", stopID + ", " + stopLatLngString + ", " + stopName);

                    String withoutBraces = stopLatLngString.substring(stopLatLngString.indexOf("(") + 1, stopLatLngString.indexOf(")"));
                    String[] split = withoutBraces.split(",");
                    Double lat = Double.parseDouble(split[0]);
                    Double lon = Double.parseDouble(split[1]);
                    LatLng secondCoordinates = new LatLng(lat, lon);
                    builder.include(secondCoordinates);

                    NearbySuggestions busStop = new NearbySuggestions();
                    busStop.setBusStopName(stopName);
                    busStop.setBusStopID(stopID);
                    busStop.setBusStopLat(lat);
                    busStop.setBusStopLng(lon);

                    busStopsList.add(busStop);
                }
            }
        }
        db.close();
    }

    private void createMarker(String busStopName, String busStopID, Double busStopLat, Double busStopLng) {
        if (map != null) {
            LatLng latLng = new LatLng(busStopLat, busStopLng);
            int height = 35;
            int width = 35;
            BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.drawable.nearby_marker);
            Bitmap b = bitmapdraw.getBitmap();
            Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);

            map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(busStopName + " (" + busStopID + ")")
                    .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float zoom = map.getCameraPosition().zoom;
                if (mLastLocation != null) {
                    LatLng currLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    if (zoom >= 12) {
                        map.animateCamera(CameraUpdateFactory.newLatLng(currLatLng));
                    } else {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(currLatLng, 16));
                    }
                }
            }
        });

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                String BusStopId;
                selectedMarker = marker;
                BusStopId = marker.getTitle().substring(marker.getTitle().indexOf("(") + 1, marker.getTitle().indexOf(")"));

                getETA(BusStopId);
                return false;
            }
        });

        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener(){
            @Override
            public void onInfoWindowClick(Marker marker){
                Intent sendBusStop  = new Intent(BusSearch.this, BusArrivalTiming.class);
                Bundle b = new Bundle();

                selectedBusStopName = marker.getTitle().substring(0, marker.getTitle().indexOf("(") - 1);
                b.putString("BusStopName", selectedBusStopName);

                String busStopId = marker.getTitle().substring(marker.getTitle().indexOf("(") + 1, marker.getTitle().indexOf(")"));
                b.putString("busStopNo", busStopId);
                b.putParcelable("busStopPt", marker.getPosition());
                sendBusStop.putExtras(b);
                startActivity(sendBusStop);
                overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
            }
        });
    }

    private void getETA(String stopCode){
        JsonObjectRequest BusArrivalRequest = new JsonObjectRequest(Request.Method.GET, "http://datamall2.mytransport.sg/ltaodataservice/BusArrival?BusStopID=" + stopCode + "&SST=True", null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("Services");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject services = jsonArray.getJSONObject(i);
                                String busNo = services.getString("ServiceNo");

                                if (busNo.equals(selectedServiceNumber)) {
                                    JSONObject nextBus = services.getJSONObject("NextBus");
                                    String eta = nextBus.getString("EstimatedArrival");
                                    Calendar c = Calendar.getInstance();
                                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                    String sst = format.format(c.getTime());
                                    Date ETA;
                                    Date current;
                                    try {
                                        String[] splitString = eta.split("T");
                                        splitString[1].replace("+08:00","");
                                        ETA = format.parse(splitString[0]+" "+splitString[1]);
                                        current = format.parse(sst);

                                        diff = ETA.getTime() - current.getTime();

                                    }catch(Exception e){
                                        e.printStackTrace();
                                    }
                                    if(diff/(60*1000) > 0) {
                                        timeInMin = String.valueOf(diff / (60 * 1000)) + " min";
                                    }else{
                                        timeInMin = "Arriving";
                                    }
                                    selectedMarker.setSnippet(timeInMin);
                                    selectedMarker.hideInfoWindow();
                                    selectedMarker.showInfoWindow();
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("VOLLEY", "ERROR");
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Snackbar.make(findViewById(R.id.bus_arrival_parent_view),
                                "Oh no! Something went wrong. Please check that you are connected to the internet.",
                                Snackbar.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("AccountKey", "6oxHbzoDSzuXhgEvfYLqLQ==");
                headers.put("UniqueUserID", "2807eaf2-cf3e-4d9a-8468-edd50fd0c1cd");
                headers.put("accept", "application/json");
                return headers;
            }
        };
        requestQueue.add(BusArrivalRequest);
    }

    @Override
    protected void onResume() {
        super.onResume();

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
                mLastLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            }

            locationManager.requestLocationUpdates(provider, 2000, 10, locationListener);
        } catch (SecurityException e) {
            Toast.makeText(getApplicationContext(), "Cannot detect...", Toast.LENGTH_SHORT).show();
        }
    }

    private final android.location.LocationListener locationListener = new android.location.LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            mLastLocation = location;
            mLastLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };
}
