package itrans.itranstest;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import itrans.itranstest.Internet.MyApplication;
import itrans.itranstest.Internet.VolleySingleton;

public class Splash extends Activity{

    private JsonObjectRequest jsonObjectRequest;
    private int count;
    private RequestQueue requestQueue;
    private MyApplication mApplication;
    private Boolean end;
    private Boolean isFirstNumber = true;
    private Boolean isFinalBus = false;
    private String lastNo;
    private List<String> busList;
    private TextView splashDescription;
    private int label;

    private ArrayList<String> firstDirection = new ArrayList<>();
    private ArrayList<String> secondDirection = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        end = false;
        count = -50;
        mApplication = MyApplication.getInstance();
        busList = mApplication.retrieveAll(getApplicationContext());
        requestQueue = VolleySingleton.getInstance().getRequestQueue();
        splashDescription = (TextView) findViewById(R.id.splashDescription);
        label = 0;

        Toast.makeText(Splash.this, String.valueOf(busList.size()), Toast.LENGTH_SHORT).show();
        if(busList.size() <= 410){
            if (!busList.isEmpty()) {
                Toast.makeText(Splash.this, "TRUE", Toast.LENGTH_SHORT).show();
                mApplication.deleteAll(getApplicationContext());
                busList.clear();
            }
            getBusNo();
        }else {
            Thread timer = new Thread(){
                public void run(){
                    try{
                        sleep(1000);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }finally{
                        Intent intent = new Intent(Splash.this, MainActivity.class);
                        startActivity(intent);
                    }
                }
            };
            timer.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    public void getBusNo(){
        count += 50;
        jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, "http://datamall2.mytransport.sg/ltaodataservice/BusRoutes?$skip="+String.valueOf(count), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("value");
                            if(jsonArray.length() < 50) {
                                //this means that its on the last page of bus routes...
                                end = true;
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject services = jsonArray.getJSONObject(i);
                                String busNo = services.getString("ServiceNo");
                                int busDirection = services.getInt("Direction");
                                String busID = services.getString("BusStopCode");
                                if (isFirstNumber){
                                    lastNo = busNo;
                                    isFirstNumber = false;
                                }
                                if (!busNo.equals(lastNo)){
                                    label++;
                                    String busStopsOne = firstDirection.toString();
                                    String busStopsTwo = secondDirection.toString();
                                    if (secondDirection.isEmpty()){
                                        //this means that the bus service is a loop
                                        busStopsTwo = "LOOP";
                                    }
                                    mApplication.addToDatabase(lastNo, busStopsOne, busStopsTwo, getApplication());
                                    lastNo = busNo;
                                    splashDescription.setText(Html.fromHtml("First time run initialisation" +
                                            "<br />" + "<small>"  + "Items downloaded: " + String.valueOf(label) + "/416" +
                                            "<br />" + "Please do not exit the application." + "</small>"));
                                    firstDirection.clear();
                                    secondDirection.clear();
                                    if (busDirection == 1){
                                        firstDirection.add(busID);
                                    }else{
                                        secondDirection.add(busID);
                                    }
                                    if (end){
                                        isFinalBus = true;
                                    }
                                }else{
                                    if (busDirection == 1){
                                        firstDirection.add(busID);
                                    }else{
                                        secondDirection.add(busID);
                                    }
                                    if (isFinalBus){
                                        if ((i + 1) == jsonArray.length()){
                                            //this means that this is final entry and final stop.
                                            String busStopsOne = firstDirection.toString();
                                            String busStopsTwo = secondDirection.toString();
                                            if (secondDirection.isEmpty()){
                                                //this means that the bus service is a loop
                                                busStopsTwo = "LOOP";
                                            }
                                            mApplication.addToDatabase(lastNo, busStopsOne, busStopsTwo, getApplication());
                                        }
                                    }
                                }
                            }
                            if(!end){
                                getBusNo();
                            }else{
                                count = -50;
                                end = false;
                                label = 0;
                                startSortingBusStops();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
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
        requestQueue.add(jsonObjectRequest);
    }

    private void startSortingBusStops(){
        ArrayList<Double> longitudinalArray = new ArrayList<>();
        longitudinalArray.add(103.597870);
        ArrayList<Double> latitudinalArray = new ArrayList<>();
        latitudinalArray.add(1.476153);
        final double latitudeIncrement = 4/110.574; //in kilometers
        final double longitudeIncrement = 4/(111.320 * Math.cos(1.476153)); //in kilometers
        double lastDistance1 = 0;
        double lastDistance2 = 0;

        HashMap<Integer, LatLngBounds> busStopsContainers = new HashMap<>();
        //Boundaries of Singapore
        //LatLng Northwest = new LatLng(1.476153, 103.597870);
        //LatLng Southeast = new LatLng(1.216673, 104.102554);

        Location topLeft = new Location("topleft");
        topLeft.setLatitude(1.476153);
        topLeft.setLongitude(103.597870);

        Location topRight = new Location("topright");
        topRight.setLatitude(1.476153);
        topRight.setLongitude(104.102554);

        Location bottomLeft = new Location("bottomleft");
        bottomLeft.setLatitude(1.216673);
        bottomLeft.setLongitude(103.597870);

        Location bottomRight = new Location("bottomright");
        bottomRight.setLatitude(1.216673);
        bottomRight.setLongitude(104.102554);

        double longitudinalDistance = topLeft.distanceTo(topRight);
        double latitudinalDistance = topLeft.distanceTo(bottomLeft);

        //while loop for longitude
        double variable1 = 103.597870;
        while (lastDistance1 <= longitudinalDistance){
            variable1 += longitudeIncrement;
            longitudinalArray.add(variable1);
            Location newTemporaryPoint = new Location("TemporaryPoint");
            newTemporaryPoint.setLatitude(1.476153);
            newTemporaryPoint.setLongitude(variable1);
            lastDistance1 = topLeft.distanceTo(newTemporaryPoint);
            Log.e("SORTING LONGITUDE", String.valueOf(variable1));
        }

        //while loop for latitude
        double variable2 = 1.476153;
        while (lastDistance2 <= latitudinalDistance){
            variable2 -= latitudeIncrement;
            latitudinalArray.add(variable2);
            Location newPoint = new Location("newPoint");
            newPoint.setLatitude(1.476153);
            newPoint.setLongitude(variable1);
            lastDistance2 = topLeft.distanceTo(newPoint);
            Log.e("SORTING LATITIUDE", String.valueOf(variable2));
        }

        int number = 1;
        Log.e("SORTING SIZES", String.valueOf(latitudinalArray.size()) + ", " + String.valueOf(longitudinalArray.size()));
        for (int i = 0; i < latitudinalArray.size() - 1; i++){
            for (int a = 0; a < longitudinalArray.size() - 1; a++){
                number++;
                LatLng Northwest = new LatLng(latitudinalArray.get(i), longitudinalArray.get(a));
                LatLng Southeast = new LatLng(latitudinalArray.get(i + 1), longitudinalArray.get(a + 1));
                LatLngBounds container = new LatLngBounds(Northwest, Southeast);
                busStopsContainers.put(number, container);
                Log.e("SORTING HASHMAP", Northwest.toString() + ", " + Southeast.toString());
            }
        }
        Log.e("HASHMAP SIZE", String.valueOf(busStopsContainers.size()));
        getBusStops();
    }

    private void getBusStops(){
        count += 50;
        JsonObjectRequest requestBusStop = new JsonObjectRequest(Request.Method.GET, "http://datamall2.mytransport.sg/ltaodataservice/BusStops?$skip=" + String.valueOf(count), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try{
                            JSONArray jsonArray = response.getJSONArray("value");
                            if(jsonArray.length() < 50) {
                                end = true;  //this means that its on the last page of bus routes...
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                label++;
                                splashDescription.setText(Html.fromHtml("First time run initialisation" +
                                        "<br />" + "<small>"  + "Items downloaded: " + String.valueOf(label) + "/5271" +
                                        "<br />" + "Please do not exit the application." + "</small>"));
                                JSONObject services = jsonArray.getJSONObject(i);
                                String busStopId = services.getString("BusStopCode");
                                String busStopName = services.getString("Description");
                                String roadName = services.getString("RoadName");
                                String lat = services.getString("Latitude");
                                String lng = services.getString("Longitude");

                                Double doubleLat = Double.parseDouble(lat);
                                Double doublelng = Double.parseDouble(lng);
                                LatLng coordinates = new LatLng(doubleLat, doublelng);

                                if (busStopId.startsWith("0")) {
                                    busStopId = busStopId.substring(1, 5);
                                }

                                String latitude = String.valueOf(round(coordinates.latitude, 5));
                                latitude = latitude.replaceAll("[.]","");
                                if (i == 0){
                                    Log.e("ENCODE NAME", latitude);
                                }

                                BusNumberDBAdapter db = new BusNumberDBAdapter(getApplicationContext());
                                db.open();
                                db.insertBusStop(busStopId, busStopName, latitude, roadName, coordinates.toString());
                                db.close();
                            }
                            if(!end){
                                getBusStops();
                            }else{
                                startSortingBusStops();
                                Intent mainActivity = new Intent(Splash.this, MainActivity.class);
                                startActivity(mainActivity);
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("AccountKey", "6oxHbzoDSzuXhgEvfYLqLQ==");
                headers.put("UniqueUserID", "2807eaf2-cf3e-4d9a-8468-edd50fd0c1cd");
                headers.put("accept", "application/json");
                return headers;
            }
        };
        requestQueue.add(requestBusStop);
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
