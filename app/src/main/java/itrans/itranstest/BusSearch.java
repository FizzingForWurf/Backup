package itrans.itranstest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
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
import com.google.android.gms.common.api.BooleanResult;
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
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import itrans.itranstest.Internet.MyApplication;
import itrans.itranstest.Internet.VolleySingleton;

public class BusSearch extends AppCompatActivity implements OnMapReadyCallback{

    private GoogleMap map;
    private Marker selectedMarker;

    private FloatingSearchView mSearchView;

    private List<Double> singleCoordinates = new ArrayList<>();
    private List<List<Double>> busCoordinates = new ArrayList<>();
    private List<Integer> allBusStops = new ArrayList<>();

    private List<String> BusServiceNumberList = new ArrayList<String>();
    private List<BusRoutes> BusSearchSuggestionList = new ArrayList<>();

    private VolleySingleton volleySingleton;
    private RequestQueue requestQueue;

    private int count;
    private int lastCode;
    private int listCount;
    private boolean dontCall;
    private boolean end;
    private String query;
    private String selectedServiceNumber;
    private String timeInMin;
    private long diff;
    private String selectedBusStopName;

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

        mSearchView.setOnHomeActionClickListener(new FloatingSearchView.OnHomeActionClickListener() {
            @Override
            public void onHomeClicked() {
                BusSearch.this.finish();
            }
        });

        mSearchView.setOnQueryChangeListener(new FloatingSearchView.OnQueryChangeListener() {
            @Override
            public void onSearchTextChanged(String oldQuery, String newQuery) {
                if (!BusSearchSuggestionList.isEmpty()){
                    BusSearchSuggestionList.clear();
                }
                if (!oldQuery.equals("") && newQuery.equals("")) {
                    mSearchView.clearSuggestions();
                } else {
                    mSearchView.showProgress();
                    List<String> filteredList = Lists.newArrayList(Collections2.filter(BusServiceNumberList,
                            Predicates.containsPattern(newQuery)));
                    for(int i = 0 ; i < filteredList.size(); i++){
                        if (filteredList.get(i).startsWith(newQuery)) {
                            BusSearchSuggestionList.add(new BusRoutes(filteredList.get(i)));
                            if (BusSearchSuggestionList.size() == 12){
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
                for(int i = 0; i < BusServiceNumberList.size();i++) {
                    if(BusServiceNumberList.get(i).equals(searchSuggestion.getBody())) {
                        selectedServiceNumber = BusServiceNumberList.get(i);
                        count = (int) Math.round(i*1.1)*50;
                        break;
                    }
                }
                if (!busCoordinates.isEmpty()){
                    busCoordinates.clear();
                }
                if (!allBusStops.isEmpty()){
                    allBusStops.clear();
                }
                lastCode = 0;
                listCount = 0 ;
                dontCall = false;
                end = false;
                query = null;
                timeInMin = null;
                diff = 0;
                map.clear();

                busStops();
            }

            @Override
            public void onSearchAction(String currentQuery) {

            }
        });
    }

    private void busStops(){
        dontCall = false;
        count += 50;
        JsonObjectRequest BusRoutesRequest = new JsonObjectRequest(Request.Method.GET, "http://datamall2.mytransport.sg/ltaodataservice/BusRoutes?$skip="+String.valueOf(count), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("value");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject services = jsonArray.getJSONObject(i);
                                String busNo = services.getString("ServiceNo");
                                if(busNo.equals(selectedServiceNumber)){
                                    String busStopCode = services.getString("BusStopCode");
                                    if(!busStopCode.matches("(A|B|C|P|N|E|T|S).*")){
                                        Integer busStop = Integer.parseInt(busStopCode);
                                        allBusStops.add(busStop);
                                    }
                                    end = true;
                                }else if(end){
                                    dontCall = true;
                                }
                            }
                            if(!dontCall){
                                busStops();
                            }else{
                                Toast.makeText(getApplicationContext(), "Please wait...", Toast.LENGTH_LONG).show();
                                Toast.makeText(getApplicationContext(), "Please wait...", Toast.LENGTH_SHORT).show();
                                Collections.sort(allBusStops);
                                count = -50;
                                for(int i = allBusStops.size() - 1; i >= 0; i--){
                                    if(i == allBusStops.size() - 1 || allBusStops.get(i) != lastCode) {
                                        lastCode = allBusStops.get(i);
                                    }else{
                                        allBusStops.remove(i);
                                    }
                                }
                                Log.e("BUSSTOP LIST", Integer.toString(allBusStops.size()));
                                setMarkers();
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
                        Toast.makeText(getApplicationContext(), "That did not work:(", Toast.LENGTH_LONG).show();
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
        requestQueue.add(BusRoutesRequest);
    }

    private void setMarkers(){
        dontCall = false;
        count += 50;
        JsonObjectRequest BusStopRequest = new JsonObjectRequest(Request.Method.GET, "http://datamall2.mytransport.sg/ltaodataservice/BusStops?$skip="+String.valueOf(count), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("value");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject services = jsonArray.getJSONObject(i);
                                String busCode = services.getString("BusStopCode");
                                if(listCount >= allBusStops.size()){
                                    dontCall = true;
                                }else{
                                    if(String.valueOf(allBusStops.get(listCount)).length() < 5){
                                        query = "0" + String.valueOf(allBusStops.get(listCount));
                                    }else{
                                        query = String.valueOf(allBusStops.get(listCount));
                                    }
                                    if(query.equals(busCode)){
                                        singleCoordinates = new ArrayList<>();
                                        Double Latitude = services.getDouble("Latitude");
                                        Double Longitude = services.getDouble("Longitude");
                                        singleCoordinates.add(Latitude);
                                        singleCoordinates.add(Longitude);
                                        busCoordinates.add(singleCoordinates);
                                        listCount++;
                                    }
                                }
                            }
                            if(!dontCall){
                                setMarkers();
                            }else{
                                placeMarkers();
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
                        Toast.makeText(getApplicationContext(), "That did not work:(", Toast.LENGTH_LONG).show();
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
        requestQueue.add(BusStopRequest);
    }

    public void placeMarkers(){
        count = 0;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for(List<Double> coordinates : busCoordinates) {
            LatLng location = new LatLng(coordinates.get(0),coordinates.get(1));

            int height = 50;
            int width = 50;
            BitmapDrawable bitmapdraw = (BitmapDrawable)getResources().getDrawable(R.drawable.nearby_marker);
            Bitmap b = bitmapdraw.getBitmap();
            Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);

            builder.include(location);

            map.addMarker(new MarkerOptions()
                    .position(location)
                    .title(String.valueOf(getbusStopId(allBusStops.get(count))))
                    .snippet("")
                    .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
            count++;
        }
        LatLngBounds bounds = builder.build();
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    public String getbusStopId(int nameCode){
        if(String.valueOf(nameCode).length() < 5){
            query = "0" + String.valueOf(nameCode);
        }else{
            query = String.valueOf(nameCode);
        }
        return query;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                selectedMarker = marker;
                getBusStopName(marker.getPosition());
                getETA(marker.getTitle());
                return false;
            }
        });

        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener(){
            @Override
            public void onInfoWindowClick(Marker marker){
                Intent sendBusStop  = new Intent(BusSearch.this, BusArrivalTiming.class);
                Bundle b = new Bundle();
                if (selectedBusStopName != null) {
                    b.putString("BusStopName", selectedBusStopName);
                }
                b.putString("busStopNo", marker.getTitle());
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
                        Toast.makeText(getApplicationContext(), "That did not work:(", Toast.LENGTH_LONG).show();
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

    private void getBusStopName(LatLng busStopLocation) {
        String url = getBusStopNameUrl(busStopLocation.latitude, busStopLocation.longitude);
        JsonObjectRequest BusStopNameRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        JSONArray nearbyJA;
                        try {
                            nearbyJA = response.getJSONArray("results");
                            JSONObject BusStops = nearbyJA.getJSONObject(0);
                            selectedBusStopName = BusStops.getString("name");
                            Log.e("LATLNG TEST", selectedBusStopName);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Snackbar.make(findViewById(R.id.bus_arrival_parent_view),
                                "Oh no! Something went wrong. Please check that you are connected to the internet.",
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
        requestQueue.add(BusStopNameRequest);
    }

    private String getBusStopNameUrl(double lat, double lon) {
        String latitude = Double.toString(lat);
        String longitude = Double.toString(lon);
        StringBuilder urlString = new StringBuilder();
        urlString.append("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        urlString.append("location=");
        try {
            urlString.append(URLEncoder.encode(latitude, "utf8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        urlString.append(",");
        try {
            urlString.append(URLEncoder.encode(longitude, "utf8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        urlString.append("&radius=1");
        urlString.append("&type=bus_station");
        urlString.append("&key=" + "AIzaSyBF6n8sKZwuq_kr5FXmL3k2xLO_7fz77eE");
        Log.i("NEARBY URL", urlString.toString());
        return urlString.toString();
    }
}
