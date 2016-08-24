package itrans.itranstest;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.transition.Explode;
import android.transition.Fade;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
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

import java.text.DecimalFormat;

import itrans.itranstest.Internet.VolleySingleton;

public class AddDestination extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener,
        OnMapReadyCallback{

    private GoogleMap map;
    private Marker myMapMarker;
    private Circle myCirleRadius;
    private LatLng selectedLocation;

    private String finalLatLong;
    private EditText etTitle;
    private CardView cvDestination;
    private ImageView ivPickMap;
    private Button btnDone, btnCancel;
    private TextView tvDestination, tvRadiusIndicator;
    private SeekBar radiusSeekbar;

    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final int PLACE_PICKER_REQUEST = 2;

    private int progressChange = 0;
    private double radius = 0;
    private int finalRadius = 1100;
    private String entryRadius = "1100";

    private VolleySingleton volleySingleton;
    private RequestQueue requestQueue;

    private boolean isUpdate = false;
    private int updateDestinationRowNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }

        setContentView(R.layout.activity_add_destination);
        setTitle("Add destination");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentMapAddDestination);
        mapFragment.getMapAsync(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        volleySingleton = VolleySingleton.getInstance();
        requestQueue = volleySingleton.getRequestQueue();

        ivPickMap = (ImageView) findViewById(R.id.ivPickMap);
        etTitle = (EditText) findViewById(R.id.etTitle);
        cvDestination = (CardView) findViewById(R.id.cvDestination);
        btnCancel = (Button) findViewById(R.id.btnCancel);
        btnDone = (Button) findViewById(R.id.btnDone);
        tvDestination = (TextView) findViewById(R.id.tvDestination);
        tvRadiusIndicator = (TextView) findViewById(R.id.tvRadiusIndicator);
        radiusSeekbar = (SeekBar) findViewById(R.id.radiusSeekbar);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            btnDone.setText("Update");
            setTitle("Edit destination");
            isUpdate = true;
            updateDestinationRowNumber = extras.getInt("updateRowNumber");
            String Title = extras.getString("updateTitle");
            String Destination = extras.getString("updateDestination");
            String LatLng = extras.getString("updateLatLng");
            String Radius = extras.getString("updateRadius");

            etTitle.setText(Title);
            tvDestination.setText(Destination);
            finalRadius = Integer.parseInt(Radius);
            entryRadius = Integer.toString(finalRadius);
            DecimalFormat df = new DecimalFormat("####0.00");
            if (radius >= 1000) {
                tvRadiusIndicator.setText(df.format(radius / 1000) + "km");
            } else {
                tvRadiusIndicator.setText(finalRadius + "m");
            }

            progressChange = (finalRadius - 50)/50;
            radiusSeekbar.setProgress(progressChange);

            if (finalRadius >= 900 && finalRadius <= 1300){
                tvRadiusIndicator.setTextColor(Color.parseColor("#4CAF50"));
            }else if ((finalRadius >= 500 && finalRadius < 900) || (finalRadius > 1300 && finalRadius <= 1600)){
                tvRadiusIndicator.setTextColor(Color.parseColor("#FF9800"));
            }else {
                tvRadiusIndicator.setTextColor(Color.parseColor("#F44336"));
            }

            finalLatLong = LatLng;
            String[] latANDlong = new String[0];
            if (LatLng != null) {
                latANDlong = LatLng.split(",");
            }
            double latitude = Double.parseDouble(latANDlong[0]);
            double longitude = Double.parseDouble(latANDlong[1]);
            selectedLocation = new LatLng(latitude, longitude);
        }

        btnCancel.setOnClickListener(this);
        btnDone.setOnClickListener(this);
        ivPickMap.setOnClickListener(this);
        cvDestination.setOnClickListener(this);
        radiusSeekbar.setOnSeekBarChangeListener(this);

        setupWindowAnimations();
    }

    private void setupWindowAnimations() {
        Fade fade = new Fade();
        fade.setDuration(5000);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setEnterTransition(new Explode());
        }
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
                if (isUpdate){
                    String addTitle = etTitle.getText().toString();
                    String addDestination = tvDestination.getText().toString();
                    try {
                        if (tvDestination.getText().toString().equals("")) {
                            Toast.makeText(getApplicationContext(), "Please select your destination before proceeding", Toast.LENGTH_SHORT).show();
                        } else if (etTitle.getText().toString().equals("")) {
                            Toast.makeText(getApplicationContext(), "Please enter a title before proceeding", Toast.LENGTH_SHORT).show();
                        }else {
                            DBAdapter dbAdapter = new DBAdapter(AddDestination.this);
                            dbAdapter.open();
                            dbAdapter.updateEntry(updateDestinationRowNumber, addTitle, addDestination, finalLatLong, entryRadius);
                            dbAdapter.close();
                            Toast.makeText(getApplicationContext(), "Entry updated!", Toast.LENGTH_SHORT).show();
                            this.finish();
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else {
                    if (tvDestination.getText().toString().equals("")) {
                        Toast.makeText(getApplicationContext(), "Please select your destination before proceeding", Toast.LENGTH_SHORT).show();
                    } else if (etTitle.getText().toString().equals("")) {
                        Toast.makeText(getApplicationContext(), "Please enter a title before proceeding", Toast.LENGTH_SHORT).show();
                    } else {
                        String addTitle = etTitle.getText().toString();
                        String addDestination = tvDestination.getText().toString();
                        try {
                            DBAdapter inputDestination = new DBAdapter(AddDestination.this);
                            inputDestination.open();
                            int numRows = inputDestination.getNumberOfRows();
                            inputDestination.insertEntry(numRows + 1, addTitle, addDestination, finalLatLong, entryRadius);
                            inputDestination.close();
                            Toast.makeText(getApplicationContext(), "Entry added!", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        this.finish();
                    }
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
                }else {
                    tvDestination.setText(address);
                }
                processlatlng(latlong);
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
        }
    }

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

        if (isUpdate && selectedLocation != null){
            changeMapLocation(selectedLocation);
        }
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
}
