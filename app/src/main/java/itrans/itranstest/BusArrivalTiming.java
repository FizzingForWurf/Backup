package itrans.itranstest;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import itrans.itranstest.Internet.VolleySingleton;

public class BusArrivalTiming extends AppCompatActivity{

    private RecyclerView rvBusServices;
    private BusArrivalAdapter mBusArrivalAdapter;
    private List<BusDetails> busServiceList = new ArrayList<>();
    private BusDetails busDetails = new BusDetails();
    private List<Integer> toggleCounter = new ArrayList<>();

    private String selectedBusStopName;
    private String selectedBusStopId;
    private LatLng selectedBusStopLatLng;

    private VolleySingleton volleySingleton;
    private RequestQueue requestQueue;

    private int selection;
    private int ending;
    private boolean select;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_arrival_timing);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        select = false;
        selection = 0;

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Bundle b = getIntent().getExtras();
        if(b != null) {
            if (b.getString("BusStopName") != null) {
                selectedBusStopName = b.getString("BusStopName");
            }
            selectedBusStopId = b.getString("busStopNo");
            selectedBusStopLatLng = b.getParcelable("busStopPt");
        }

        if (selectedBusStopName != null){
            setTitle(selectedBusStopName);
            toolbar.setSubtitle("Bus stop Id: " + selectedBusStopId);
        }else{
            setTitle(selectedBusStopId);
        }

        volleySingleton = VolleySingleton.getInstance();
        requestQueue = volleySingleton.getRequestQueue();

        mBusArrivalAdapter = new BusArrivalAdapter(busServiceList);
        rvBusServices = (RecyclerView) findViewById(R.id.rvBusServices);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        rvBusServices.setLayoutManager(mLayoutManager);
        rvBusServices.setItemAnimator(new DefaultItemAnimator());
        rvBusServices.setAdapter(mBusArrivalAdapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.bus_arrival_swipe_refresh);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!busServiceList.isEmpty()){
                    busServiceList.clear();
                }
                if (!toggleCounter.isEmpty()){
                    toggleCounter.clear();
                }
                call(selectedBusStopId, 0);
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });

        call(selectedBusStopId, 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //For back button...
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        this.finish();
        overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
    }

    private void call(String busStop, final int toggle){
        JsonObjectRequest BusStopRequest = new JsonObjectRequest(Request.Method.GET, "http://datamall2.mytransport.sg/ltaodataservice/BusArrival?BusStopID="+busStop+"&SST=True", null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("Services");
                            if(select){
                                ending = selection + 1;
                            } else{
                                ending = jsonArray.length();
                            }
                            for (int i = selection; i < ending; i++) {
                                JSONObject services = jsonArray.getJSONObject(i);
                                String busNo = services.getString("ServiceNo");
                                String inService = services.getString("Status");
                                if(inService.equals("In Operation")) {
                                    JSONObject nextBus = services.getJSONObject("NextBus");
                                    String eta = nextBus.getString("EstimatedArrival");
                                    String wheelC = nextBus.getString("Feature");
                                    String load = nextBus.getString("Load");
                                    if (select && toggle == 1) {
                                        JSONObject subBus = services.getJSONObject("SubsequentBus");
                                        String NEta = subBus.getString("EstimatedArrival");
                                        String NWheelC = subBus.getString("Feature");
                                        String NLoad = subBus.getString("Load");
                                        if (NEta != null) {
                                            busDetails = busServiceList.get(selection);
                                            busDetails.setBF(NWheelC);
                                            busDetails.setBusT(NEta);
                                            busDetails.setSpace(NLoad);
                                        }
                                    } else if (select && toggle == 0) {
                                        busDetails = busServiceList.get(selection);
                                        busDetails.setBF(wheelC);
                                        busDetails.setBusT(eta);
                                        busDetails.setSpace(load);
                                    } else {
                                        toggleCounter.add(0);
                                        busDetails = new BusDetails(busNo, eta, wheelC, load);
                                        busServiceList.add(busDetails);
                                    }
                                }
                            }
                            if(select){
                                mBusArrivalAdapter.notifyItemChanged(selection);
                            }else {
                                mBusArrivalAdapter.notifyDataSetChanged();
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
                headers.put("AccountKey", "3SnRYzr/X0eKp2HvwTYtmg==");
                headers.put("UniqueUserID", "0bf7760d-15ec-4a1b-9c82-93562fcc9798");
                headers.put("accept", "application/json");
                return headers;
            }
        };
        requestQueue.add(BusStopRequest);
    }
}
