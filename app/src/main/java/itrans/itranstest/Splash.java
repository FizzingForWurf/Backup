package itrans.itranstest;

import android.app.Activity;
import android.content.Intent;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    private String lastNo;
    private List<String> busList;
    private TextView splashDescription;
    private int label;

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

        if(busList.size() <= 410){
            if (!busList.isEmpty()) {
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
                            if(jsonArray.length()<50) {
                                end = true;
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject services = jsonArray.getJSONObject(i);
                                String busNo = services.getString("ServiceNo");
                                if(!busNo.equals(lastNo)){
                                    label++;
                                    lastNo = busNo;
                                    mApplication.addToDatabase(busNo,getApplication());
                                    splashDescription.setText(Html.fromHtml("First time run initialisation" +
                                            "<br />" + "<small>"  +"Items downloaded: " + String.valueOf(label) + "/416" +
                                            "<br />" + "Please do not exit the application." + "</small>"));
                                }
                            }
                            if(!end){
                                getBusNo();
                            }else{
                                Intent mainActivity = new Intent(Splash.this, MainActivity.class);
                                startActivity(mainActivity);
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
        requestQueue.add(jsonObjectRequest);
    }
}
