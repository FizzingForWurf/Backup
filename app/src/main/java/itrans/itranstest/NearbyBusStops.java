package itrans.itranstest;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.SearchSuggestionsAdapter;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import itrans.itranstest.Internet.VolleySingleton;

public class NearbyBusStops extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap map;
    private LatLng previousLatLng; //from MainActivity only!
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Marker selectedPlaceMarker;
    private Circle nearbyBusStopsCirle;

    private FloatingSearchView mSearchView;
    private List<NearbySuggestions> mSuggestionsList = new ArrayList<>();
    private List<NearbySuggestions> mResultsList = new ArrayList<>();
    private List<NearbySuggestions> mBusStopList = new ArrayList<>();
    private FloatingActionButton fab;

    private List<List<Double>> busCoordinates = new ArrayList<>();
    private List<Integer> finished = new ArrayList<>();

    private VolleySingleton volleySingleton;
    private RequestQueue requestQueue;

    private static final String TAG = "AUTOCOMPLETE";
    private int Radius = 500;
    private int setRadius = 500;
    private String query;

    private String placeId;
    private String placeName;
    private Double placeLat;
    private Double placeLng;

    private SharedPreferences prefs;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_bus_stops);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.nearbyMap);
        mapFragment.getMapAsync(this);

        mSearchView = (FloatingSearchView) findViewById(R.id.floating_search_view);

        volleySingleton = VolleySingleton.getInstance();
        requestQueue = volleySingleton.getRequestQueue();

        prefs = this.getPreferences(Context.MODE_PRIVATE);
        Radius = prefs.getInt("nearbyBusStopRadius", 500);
        query = prefs.getString("searchQuery", "");
        mSearchView.setSearchText("");

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Double lat = extras.getDouble("LastLat");
            Double lon = extras.getDouble("LastLng");
            previousLatLng = new LatLng(lat, lon);
        }

        fab = (FloatingActionButton) findViewById(R.id.fab);

        mSearchView.setOnHomeActionClickListener(new FloatingSearchView.OnHomeActionClickListener() {
            @Override
            public void onHomeClicked() {
                NearbyBusStops.this.finish();
            }
        });

        mSearchView.setOnQueryChangeListener(new FloatingSearchView.OnQueryChangeListener() {
            @Override
            public void onSearchTextChanged(String oldQuery, final String newQuery) {
                query = newQuery;
                if (!mSuggestionsList.isEmpty()) {
                    mSuggestionsList.clear();
                }
                if (!mResultsList.isEmpty()) {
                    mResultsList.clear();
                }
                String hi = Integer.toString(mSuggestionsList.size());
                Log.i(TAG + " CLEAR", hi);
                if (!oldQuery.equals("") && newQuery.equals("")) {
                    mSearchView.clearSuggestions();
                } else {
                    mSearchView.showProgress();
                    String url = getPlaceAutoCompleteUrl(newQuery);
                    JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET, url, null,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    String secondaryName;
                                    JSONArray ja;
                                    try {
                                        ja = response.getJSONArray("predictions");
                                        for (int i = 0; i < ja.length(); i++) {
                                            secondaryName = null;
                                            JSONObject c = ja.getJSONObject(i);
                                            String placeid = c.getString("place_id");
                                            Log.i(TAG, placeid);

                                            JSONArray description = c.getJSONArray("terms");
                                            JSONObject primaryDescription = description.getJSONObject(0);
                                            String primaryDescriptionName = primaryDescription.getString("value");
                                            Log.i(TAG, primaryDescriptionName);
                                            if (description.length() > 1) {
                                                for (int s = 1; s < description.length(); s++) {
                                                    JSONObject secondaryDescription = description.getJSONObject(s);
                                                    String secondaryDescriptionName = secondaryDescription.getString("value");
                                                    if (secondaryName == null) {
                                                        secondaryName = secondaryDescriptionName;
                                                    } else {
                                                        secondaryName = secondaryName + ", " + secondaryDescriptionName;
                                                    }
                                                }
                                            } else if (description.length() == 1) {
                                                secondaryName = primaryDescriptionName;
                                            }
                                            Log.i(TAG + " 2nd", secondaryName);

                                            NearbySuggestions nearbySuggestions = new NearbySuggestions();
                                            nearbySuggestions.setmPlaceID(placeid);
                                            nearbySuggestions.setmPlaceName(primaryDescriptionName);
                                            nearbySuggestions.setmSecondaryPlaceName(secondaryName);
                                            mResultsList.add(nearbySuggestions);

                                            String finalPlaceResult = "(" + primaryDescriptionName + ")+" + secondaryName + "=";
                                            mSuggestionsList.add(new NearbySuggestions(finalPlaceResult));
                                            String hi = Integer.toString(mSuggestionsList.size());
                                            Log.i(TAG + " TEST", hi);
                                        }
                                        String hi = Integer.toString(mSuggestionsList.size());
                                        Log.i(TAG + " AFTER LOOP", hi);
                                        mSearchView.swapSuggestions(mSuggestionsList);
                                        mSearchView.hideProgress();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Snackbar.make(findViewById(R.id.parent_view),
                                    "Oh no! Something went wrong. Please check that you are connected to the internet.",
                                    Snackbar.LENGTH_SHORT).show();
                        }
                    });
                    requestQueue.add(jsonObjReq);
                }
            }
        });

        mSearchView.setOnBindSuggestionCallback(new SearchSuggestionsAdapter.OnBindSuggestionCallback() {
            @Override
            public void onBindSuggestion(View suggestionView, ImageView leftIcon, TextView textView,
                                         SearchSuggestion item, int itemPosition) {
                leftIcon.setImageResource(R.drawable.ic_search_black_24dp);

                String texttest = textView.getText().toString();
                String primaryText1 = texttest.substring(texttest.indexOf("(") + 1, texttest.indexOf(")"));
                String primaryText = capitalisePhrase(primaryText1);

                String secondaryText1 = texttest.substring(texttest.indexOf("+") + 1, texttest.indexOf("="));
                String secondaryText = capitalisePhrase(secondaryText1);

                textView.setText(Html.fromHtml("<b>" + primaryText + "</b>" + "<br />" +
                        "<small>" + secondaryText + "</small>"));
            }
        });

        mSearchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {
                String selectedPlace1 = searchSuggestion.getBody();
                String selectedPlace2 = selectedPlace1.substring(selectedPlace1.indexOf("(") + 1, selectedPlace1.indexOf(")"));
                final String selectedPlace = capitalisePhrase(selectedPlace2);

                int suggestionPosition = mSuggestionsList.indexOf(searchSuggestion);
                Collections.reverse(mResultsList);
                placeId = mResultsList.get(suggestionPosition).getmPlaceID();

                String placeUrl = getPlaceDetailsUrl(placeId);
                JsonObjectRequest placeDetailsRequest = new JsonObjectRequest(Request.Method.GET, placeUrl, null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                JSONObject detailsJO;
                                try {
                                    mSearchView.setSearchText(selectedPlace);
                                    detailsJO = response.getJSONObject("result");
                                    placeName = detailsJO.getString("name");
                                    JSONObject latlngObject = detailsJO.getJSONObject("geometry");
                                    JSONObject latlngLocation = latlngObject.getJSONObject("location");
                                    placeLat = latlngLocation.getDouble("lat");
                                    placeLng = latlngLocation.getDouble("lng");

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                findNearbyBusStops(placeName, placeLat, placeLng);
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Snackbar.make(findViewById(R.id.parent_view),
                                "Oh no! Something went wrong. Please check that you are connected to the internet.",
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
                requestQueue.add(placeDetailsRequest);
            }

            @Override
            public void onSearchAction(String currentQuery) {

            }
        });

        mSearchView.setOnMenuItemClickListener(new FloatingSearchView.OnMenuItemClickListener() {
            @Override
            public void onActionMenuItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_range:
                            createSetRangeDialog();
                        break;
                    case R.id.menu_bus_stop:
                        mSearchView.setSearchText("");
                        placeName = "You are here";
                        placeLat = mLastLocation.getLatitude();
                        placeLng = mLastLocation.getLongitude();
                        findNearbyBusStops(placeName, placeLat, placeLng);
                        break;
                }
            }
        });
    }

    private void createSetRangeDialog() {
        setRadius = prefs.getInt("nearbyBusStopRadius", 500);
        final int previousRadius = setRadius;
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT);
        textParams.weight = 2.0f;

        LinearLayout.LayoutParams seekBarParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT);
        seekBarParams.weight = 8.0f;

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setCancelable(false);
        alert.setTitle("Set Range");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(15,15,15,15);
        layout.setWeightSum(10);

        final TextView text = new TextView(this);
        text.setText(setRadius + "m");
        text.setLayoutParams(textParams);
        text.setPadding(10, 10, 10, 10);

        SeekBar seek = new SeekBar(this);
        seek.setLayoutParams(seekBarParams);
        seek.setMax(9);
        seek.setProgress((setRadius/100) - 1);
        SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBark, int progress, boolean fromUser) {
                Log.i("SeekBar", Integer.toString(progress));
                setRadius = 100 + (progress * 100);
                String hi = Integer.toString(setRadius) + "m";
                text.setText(hi);
            }
        };
        seek.setOnSeekBarChangeListener(seekBarListener);

        layout.addView(seek);
        layout.addView(text);

        alert.setView(layout);
        alert.setPositiveButton("Done",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                prefs.edit().putInt("nearbyBusStopRadius", setRadius).apply();
                Radius = setRadius;
                if (setRadius != previousRadius && nearbyBusStopsCirle != null){
                    findNearbyBusStops(placeName, placeLat, placeLng);
                }
            }
        });

        alert.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                prefs.edit().putInt("nearbyBusStopRadius", previousRadius).apply();
            }
        });
        alert.show();
    }

    private void findNearbyBusStops(String placeName, final double lat, final double lng) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(NearbyBusStops.this);
            progressDialog.setMessage("Locating nearby bus stops...");
            progressDialog.setCancelable(false);
        }
        progressDialog.show();
        map.clear();

        if (!mBusStopList.isEmpty()) {
            mBusStopList.clear();
        }
        if (!busCoordinates.isEmpty()){
            busCoordinates.clear();
        }
        if (!finished.isEmpty()){
            finished.clear();
        }
        LatLng placeLatLng = new LatLng(lat, lng);
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(placeLatLng, getZoomLevel()));
        if (selectedPlaceMarker != null) {
            selectedPlaceMarker.remove();
        }
        selectedPlaceMarker = map.addMarker(new MarkerOptions()
                .position(placeLatLng)
                .title(placeName));
        if (nearbyBusStopsCirle != null) {
            nearbyBusStopsCirle.remove();
        }
        nearbyBusStopsCirle = map.addCircle(new CircleOptions()
                .radius(Radius)
                .center(placeLatLng)
                .fillColor(0x550000FF)
                .strokeWidth(10.0f)
                .strokeColor(Color.BLUE));

        Thread thread = new Thread(){
            public void run(){
                searchNearby(lat, lng);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("BUS STOP LIST", Integer.toString(mBusStopList.size()));
                        for (int s = 0; s < mBusStopList.size(); s++) {
                            createMarker(mBusStopList.get(s).getBusStopName(),mBusStopList.get(s).getBusStopID(),
                                    mBusStopList.get(s).getBusStopLat(), mBusStopList.get(s).getBusStopLng());
                        }

                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                    }
                });
            }
        };
        thread.start();
    }

    private void searchNearby(double lat, double lng){
        BusNumberDBAdapter db = new BusNumberDBAdapter(getApplicationContext());
        db.open();
        int numOfRows = db.getNumberOfRows();
        Log.e("NUMBER OF BUS STOPS", String.valueOf(numOfRows));
        for (int a = 0; a < numOfRows; a++){
            Log.e("TEST", String.valueOf(a));
            String coordinatesString = db.getCoordinates(a + 1);
            Log.e("TEST", coordinatesString);
            String Name = db.getName(a + 1);
            Log.e("TEST", Name);
            String Id = db.getID(a + 1);
            if (Id.length() < 5){
                Id = "0" + Id;
            }
            Log.e("TEST", Id);

            String something = coordinatesString.substring(coordinatesString.indexOf("(") + 1, coordinatesString.indexOf(")"));
            String[] latANDlong = something.split(",");
            double latitude = Double.parseDouble(latANDlong[0]);
            double longitude = Double.parseDouble(latANDlong[1]);

            Location busStop = new Location("BusStop");
            busStop.setLatitude(latitude);
            busStop.setLongitude(longitude);

            Location searchLocation = new Location("Search");
            searchLocation.setLatitude(lat);
            searchLocation.setLongitude(lng);

            Log.e("DISTANCE FROM LOCATION", String.valueOf(busStop.distanceTo(searchLocation)));
            if (busStop.distanceTo(searchLocation) <= Radius){
                Log.e("TRUE", "IN RADIUS");
                Log.e("Testing", a+1 + "/" + Name + ", " + Id + ", " + latitude + ", " + longitude);
                NearbySuggestions nearbyBusStops = new NearbySuggestions();
                nearbyBusStops.setBusStopName(Name);
                nearbyBusStops.setBusStopID(Id);
                nearbyBusStops.setBusStopLat(latitude);
                nearbyBusStops.setBusStopLng(longitude);

                mBusStopList.add(nearbyBusStops);
            }
        }
        db.close();
    }

    private int getZoomLevel() {
        int zoomLevel = 11;
        double radiusNearby = Radius + Radius / 2;
        double scale = radiusNearby / 500;
        zoomLevel = (int) (16 - Math.log(scale) / Math.log(2));

        return zoomLevel;
    }


    private void createMarker(String busStopName, String busStopID, Double busStopLat, Double busStopLng) {
        LatLng latLng = new LatLng(busStopLat, busStopLng);
        int height = 35;
        int width = 35;
        BitmapDrawable bitmapdraw = (BitmapDrawable)getResources().getDrawable(R.drawable.nearby_marker);
        Bitmap b = bitmapdraw.getBitmap();
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
        map.addMarker(new MarkerOptions()
                .position(latLng)
                .title(busStopName)
                .snippet(busStopID)
                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
    }

    private String capitalisePhrase(String phrase) {
        String[] strArray = phrase.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String s : strArray) {
            String cap = s.substring(0, 1).toUpperCase() + s.substring(1);
            builder.append(cap + " ");
        }
        return builder.toString();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float zoom = map.getCameraPosition().zoom;
                if (mLastLocation != null) {
                    LatLng currLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    if (zoom >= 13) {
                        map.animateCamera(CameraUpdateFactory.newLatLng(currLatLng));
                    } else {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(currLatLng, 16));
                    }
                }
            }
        });

        if (previousLatLng != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(previousLatLng, 16));
        }
        buildGoogleApiClient();
        mGoogleApiClient.connect();

        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Intent sendBusStop = new Intent(NearbyBusStops.this, BusArrivalTiming.class);
                Bundle b = new Bundle();
                b.putString("BusStopName", marker.getTitle());
                b.putString("busStopNo", marker.getSnippet());
                b.putParcelable("busStopPt", marker.getPosition());
                sendBusStop.putExtras(b);
                startActivity(sendBusStop);
                overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
            }
        });
    }

    @Override
    public void onBackPressed() {
        //if mSearchView.setSearchFocused(false) causes the focused search
        //to close, then we don't want to close the activity. if mSearchView.setSearchFocused(false)
        //returns false, we know that the search was already closed so the call didn't change the focus
        //state and it makes sense to call supper onBackPressed() and close the activity
        if (!mSearchView.setSearchFocused(false)) {
            super.onBackPressed();
        }
    }

    private String getPlaceAutoCompleteUrl(String input) {
        StringBuilder urlString = new StringBuilder();
        urlString.append("https://maps.googleapis.com/maps/api/place/autocomplete/json");
        urlString.append("?input=");
        try {
            urlString.append(URLEncoder.encode(input, "utf8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        urlString.append("&language=en");
        urlString.append("&key=" + "AIzaSyBF6n8sKZwuq_kr5FXmL3k2xLO_7fz77eE");
        return urlString.toString();
    }

    private String getPlaceDetailsUrl(String placeid) {
        StringBuilder urlString = new StringBuilder();
        urlString.append("https://maps.googleapis.com/maps/api/place/details/json");
        urlString.append("?placeid=");
        try {
            urlString.append(URLEncoder.encode(placeid, "utf8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        urlString.append("&language=en");
        urlString.append("&key=" + "AIzaSyBF6n8sKZwuq_kr5FXmL3k2xLO_7fz77eE");
        return urlString.toString();
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000); //5 seconds
        mLocationRequest.setFastestInterval(3000); //3 seconds
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setSmallestDisplacement(0.1F); //1/10 meter

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null)
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient != null){
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        prefs.edit().putString("searchQuery", "").apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        if (!query.equals("")) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("searchQuery", query);
            editor.apply();
        }
    }
}
