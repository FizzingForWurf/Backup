package itrans.itranstest;

import android.content.Intent;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlacePicker;
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import itrans.itranstest.Internet.VolleySingleton;

public class AddDestination extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener,
        OnMapReadyCallback{

    private GoogleMap map;
    private Marker myMapMarker;
    private Circle myCirleRadius;
    private LatLng selectedLocation;
    private LatLng previousLatLng; //From MainActivity only!

    //Testing geocoding:
    private String geoCodedAddress;

    private String finalLatLong;
    private EditText etTitle;
    private CardView cvDestination, cvRingTone, cvBusRoutes;
    private ImageView ivPickMap;
    private Button btnDone, btnCancel;
    private TextView tvDestination, tvRadiusIndicator, tvCurrentRingTone, tvBusRoutes;
    private SeekBar radiusSeekbar;

    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final int PLACE_PICKER_REQUEST = 2;
    private static final int TONE_PICKER = 3;

    private int progressChange = 0;
    private double radius = 0;
    private int finalRadius = 1100;
    private String entryRadius = "1100";
    private Uri uriRingTone;
    private String selectedRingTone;

    private VolleySingleton volleySingleton;
    private RequestQueue requestQueue;

    private List<BusRoutes> busRoutesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_destination);
        setTitle("Add destination");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentMapAddDestination);
        mapFragment.getMapAsync(this);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Double lat = extras.getDouble("LastLat");
            Double lon = extras.getDouble("LastLng");
            previousLatLng = new LatLng(lat, lon);
        }

        volleySingleton = VolleySingleton.getInstance();
        requestQueue = volleySingleton.getRequestQueue();

        ivPickMap = (ImageView) findViewById(R.id.ivPickMap);
        etTitle = (EditText) findViewById(R.id.etTitle);
        cvDestination = (CardView) findViewById(R.id.cvDestination);
        cvRingTone = (CardView) findViewById(R.id.cvRingTone);
        cvBusRoutes = (CardView) findViewById(R.id.cvBusRoutes);
        btnCancel = (Button) findViewById(R.id.btnCancel);
        btnDone = (Button) findViewById(R.id.btnDone);
        tvDestination = (TextView) findViewById(R.id.tvDestination);
        tvRadiusIndicator = (TextView) findViewById(R.id.tvRadiusIndicator);
        tvCurrentRingTone = (TextView) findViewById(R.id.tvCurrentRingTone);
        tvBusRoutes = (TextView) findViewById(R.id.tvBusRoutes);
        radiusSeekbar = (SeekBar) findViewById(R.id.radiusSeekbar);

        btnCancel.setOnClickListener(this);
        btnDone.setOnClickListener(this);
        ivPickMap.setOnClickListener(this);
        cvDestination.setOnClickListener(this);
        cvRingTone.setOnClickListener(this);
        cvBusRoutes.setOnClickListener(this);
        radiusSeekbar.setOnSeekBarChangeListener(this);

        uriRingTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        selectedRingTone = uriRingTone.toString();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //For back button...
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ivPickMap:
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                try {
                    Intent placeIntent = builder.build(AddDestination.this);
                    startActivityForResult(placeIntent, PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.cvDestination:
                try {
                    Intent searchIntent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY).build(this);
                    startActivityForResult(searchIntent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btnCancel:
                this.finish();
                break;
            case R.id.btnDone:
                if (tvDestination.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(), "Please select your destination before proceeding", Toast.LENGTH_SHORT).show();
                }else if (etTitle.getText().toString().equals("")){
                    Toast.makeText(getApplicationContext(), "Please enter a title before proceeding", Toast.LENGTH_SHORT).show();
                }else {
                    String addTitle = etTitle.getText().toString();
                    String addDestination = tvDestination.getText().toString();
                    try {
                        DBAdapter inputDestination = new DBAdapter(AddDestination.this);
                        inputDestination.open();
                        inputDestination.insertEntry(addTitle, addDestination, finalLatLong, entryRadius, selectedRingTone);
                        inputDestination.close();
                    } catch (Exception e) {
                        e.printStackTrace();

                    } finally {
                        Toast.makeText(getApplicationContext(), "Destination entry added!", Toast.LENGTH_SHORT).show();
                    }
                    this.finish();
                }
                break;
            case R.id.cvRingTone:
                final Uri currentTone= RingtoneManager.getActualDefaultRingtoneUri(AddDestination.this, RingtoneManager.TYPE_ALARM);
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentTone);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                startActivityForResult(intent, TONE_PICKER);
                break;
            case R.id.cvBusRoutes:
                if (tvDestination.getText().equals("")){
                    Toast.makeText(AddDestination.this, "Please select a destination first.", Toast.LENGTH_SHORT).show();
                }else{
                    //check if have routes available in the list then
                    //create dialog with listview?
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST){
            String latlong;
            if (resultCode == RESULT_OK){
                Place chosenDestination = PlacePicker.getPlace(this, data);
                latlong = String.format("%s", chosenDestination.getLatLng());
                String address = String.format("%s", chosenDestination.getAddress());
                if (address.equals("")) {
                    tvDestination.setText(latlong);
//                    geocodeCoordinates(latlong);
//                    tvDestination.setText(geoCodedAddress);
                }else {
                    tvDestination.setText(address);
                }
                processlatlng(latlong);
                if (latlong != null) {
                    findBusRoutes();
                }
            }
        }
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            String latlong = null;
            if (resultCode == RESULT_OK) {
                Place searchedDestination = PlaceAutocomplete.getPlace(this, data);
                latlong = String.format("%s", searchedDestination.getLatLng());
                String address = String.format("%s", searchedDestination.getAddress());
                tvDestination.setText(address);

                processlatlng(latlong);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
            }
            if (latlong != null) {
                findBusRoutes();
            }
        }
        if (requestCode == TONE_PICKER) {
            if (resultCode == RESULT_OK) {
                uriRingTone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                Ringtone ringTone = RingtoneManager.getRingtone(getApplicationContext(), uriRingTone);
                if (uriRingTone != null) {
                    String NameOfRingTone = ringTone.getTitle(getApplicationContext());
                    selectedRingTone = uriRingTone.toString();
                    tvCurrentRingTone.setText(selectedRingTone);//NameOfRingTone);
                }
            }
        }
    }

    private void findBusRoutes() {
        if (!busRoutesList.isEmpty()){
            busRoutesList.clear();
        }
        //connect to the internet, get results, display textView
        String busRouteUrl = getDirectionUrl();
        JsonObjectRequest busRouteRequest = new JsonObjectRequest(Request.Method.GET, busRouteUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        JSONArray resultsJA;
                        try {
                            resultsJA = response.getJSONArray("routes");
                            for (int i = 0; i < resultsJA.length(); i++){
                                JSONObject resultObj = resultsJA.getJSONObject(i);

                                JSONArray routesJA = resultObj.getJSONArray("legs");
                                for (int s = 0; s < routesJA.length(); s++) {
                                    JSONObject infoObj = routesJA.getJSONObject(s);
                                    JSONObject departureObj = infoObj.getJSONObject("departure_time");
                                    JSONObject arrivalObj = infoObj.getJSONObject("arrival_time");
                                    String departureTime = departureObj.getString("text");
                                    String arrivalTime = arrivalObj.getString("text");
                                    String duration = infoObj.getString("duration");

                                    JSONArray stepsJA = infoObj.getJSONArray("steps");
                                    for (int a = 0; a < stepsJA.length(); a++){
                                        JSONObject segments = stepsJA.getJSONObject(a);
                                        JSONObject durationObj = segments.getJSONObject("duration");
                                        String segmentDuration = durationObj.getString("text");
                                        String transportMode = segments.getString("travel_mode");

                                        if (segments.has("transit_details")){
                                            JSONObject transitDetails = segments.getJSONObject("transit_details");
                                            String numberOfStops = transitDetails.getString("num_stops");
                                            JSONObject busDetails = transitDetails.getJSONObject("line");
                                            String busNumber = busDetails.getString("short_name");
                                        }
                                    }
                                }
                            }
                            if (resultsJA.length() >= 1) {
                                if (resultsJA.length() == 1) {
                                    tvBusRoutes.setText(Integer.toString(resultsJA.length()) + " route available.");
                                }else{
                                    tvBusRoutes.setText(Integer.toString(resultsJA.length()) + " routes available.");
                                }
                            }else{
                                tvBusRoutes.setText("No routes available.");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        requestQueue.add(busRouteRequest);
    }

//    private String geocodeCoordinates(String latlong){
//        String rawlatlong = latlong.substring(latlong.indexOf("(")+1,latlong.indexOf(")"));
//        String[] latANDlong =  rawlatlong.split(",");
//        double Lat = Double.parseDouble(latANDlong[0]);
//        double Lon = Double.parseDouble(latANDlong[1]);
//        Geocoder gc = new Geocoder(AddDestination.this, Locale.getDefault());
//
//        try {
//            List<Address> addresses = gc.getFromLocation(Lat,Lon,1);
//            StringBuilder builderString = new StringBuilder();
//            if (addresses.size() > 0){
//                Address address = addresses.get(0);
//                for (int i = 0; i < address.getMaxAddressLineIndex(); i++){
//                    builderString.append(address.get)
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return geoCodedAddress;
//    }

    private void processlatlng(String latlong) {
        String rawlatlong = latlong.substring(latlong.indexOf("(")+1,latlong.indexOf(")"));
        finalLatLong = rawlatlong;
        String[] latANDlong =  rawlatlong.split(",");
        double latitude = Double.parseDouble(latANDlong[0]);
        double longitude = Double.parseDouble(latANDlong[1]);
        selectedLocation = new LatLng(latitude, longitude);

        changeMapLocation(selectedLocation);
    }

    private void changeMapLocation(LatLng latlng) {

        if (myMapMarker != null) {
            myMapMarker.remove();
        }

        createRadiusVisualisation(selectedLocation, finalRadius);

        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, getZoomLevel(myCirleRadius)));

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latlng);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker());
        myMapMarker = map.addMarker(markerOptions);
    }

    private void createRadiusVisualisation(LatLng location, int finalRadius) {

        if (myCirleRadius != null){
            myCirleRadius.remove();
        }

        CircleOptions co = new CircleOptions();
        co.center(location);
        co.radius(finalRadius);
        co.fillColor(0x550000FF);
        co.strokeColor(Color.BLUE);
        co.strokeWidth(10.0f);
        myCirleRadius = map.addCircle(co);
    }

    public int getZoomLevel(Circle circle) {
        int zoomLevel = 11;
        if (circle != null) {
            double radius = circle.getRadius() + circle.getRadius() / 2;
            double scale = radius / 500;
            zoomLevel = (int) (16 - Math.log(scale) / Math.log(2));
        }
        return zoomLevel;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
        DecimalFormat df = new DecimalFormat("####0.00");
        progressChange = progress;
        radius = 50 + (progress * 50);
        finalRadius = (int) radius;
        if (radius >= 1000) {
            tvRadiusIndicator.setText(df.format(radius / 1000) + "km");
        } else {
            tvRadiusIndicator.setText(finalRadius + "m");
        }
        if (selectedLocation != null){
            createRadiusVisualisation(selectedLocation, finalRadius);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, getZoomLevel(myCirleRadius)));
        }
        if (finalRadius >= 900 && finalRadius <= 1300){
            tvRadiusIndicator.setTextColor(Color.parseColor("#4CAF50"));
        }else if ((finalRadius >= 500 && finalRadius < 900) || (finalRadius > 1300 && finalRadius <= 1600)){
            tvRadiusIndicator.setTextColor(Color.parseColor("#FF9800"));
        }else {
            tvRadiusIndicator.setTextColor(Color.parseColor("#F44336"));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        radius = 50 + (progressChange * 50);
        finalRadius = (int) radius;
        entryRadius = Integer.toString(finalRadius);
    }

    private String getDirectionUrl() {
        String origin = Double.toString(previousLatLng.latitude) + "," + Double.toString(previousLatLng.longitude);
        StringBuilder urlString = new StringBuilder();
        urlString.append("https://maps.googleapis.com/maps/api/directions/json?");
        urlString.append("origin=");
        try {
            urlString.append(URLEncoder.encode(origin, "utf8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        urlString.append("&destination=");
        try {
            urlString.append(URLEncoder.encode(finalLatLong, "utf8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        urlString.append("&mode=transit");
        urlString.append("&transit_mode=bus");
        urlString.append("&traffic_model=best_guess");
        urlString.append("&language=en");
        urlString.append("&key=" + "AIzaSyBF6n8sKZwuq_kr5FXmL3k2xLO_7fz77eE");
        return urlString.toString();
    }
}
