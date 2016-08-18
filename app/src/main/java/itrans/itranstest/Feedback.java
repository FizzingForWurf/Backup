package itrans.itranstest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

public class Feedback extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        WebView myWebView = (WebView) findViewById(R.id.feedbackWebView);
        myWebView.loadUrl("https://goo.gl/forms/67OuoN2NVIrenRgJ3");
    }
}
