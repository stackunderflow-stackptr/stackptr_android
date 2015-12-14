package com.stackunderflow.stackptr;

import java.net.URLEncoder;
import java.util.ArrayList;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.stackunderflow.stackptrapi.StackPtrApiGetUsers;
import com.stackunderflow.stackptrapi.StackPtrApiGetUsersParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StackPtr extends Activity {
    SharedPreferences settings;
    LocationManager fglm;
    LocationListener fgll;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_stack_ptr);
        Context ctx = getApplicationContext();
        settings = PreferenceManager.getDefaultSharedPreferences(ctx);


        //////////


        //////////////

        String apikey = settings.getString("apikey", "");

        WebView wv = (WebView) findViewById(R.id.webview);
        WebSettings ws = wv.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setAllowUniversalAccessFromFileURLs(true);

        //wv.loadUrl("http://172.16.0.196:8080/?apikey=" + URLEncoder.encode(apikey));

        wv.addJavascriptInterface(new StackPtrAndroidShim(this), "StackPtrAndroidShim");

        wv.loadUrl("file:///android_asset/ui.html");

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        //fglm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //fgll = new StackPtrFGListener();
        //fglm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0.0f, fgll);

    }

    @Override
    public void onPause() {
        super.onPause();
        //fglm.removeUpdates(fgll);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.stackptr_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:

                return true;
            case R.id.action_refresh_userlist:
                //new StackPtrMainScreenApiGetUsers().execute(spagup);
                return true;
            case R.id.action_start_service:
                // TODO: Check API key validity
                startService(new Intent(this, StackPtrService.class));
                return true;
            case R.id.action_stop_service:
                stopService(new Intent(this, StackPtrService.class));
                return true;
            case R.id.action_new_placemark:
                Intent intent2 = new Intent("com.stackunderflow.stackptr.StackPtrPlacemark");
                startActivity(intent2);
                return true;
            case R.id.action_web_view:
                Intent intent3 = new Intent("com.stackunderflow.stackptr.StackPtrWebView");
                startActivity(intent3);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public class StackPtrAndroidShim {
        Context mContext;

        /** Instantiate the interface and set the context */
        StackPtrAndroidShim(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public String get_host() {
            return settings.getString("server_address", "");
        }

        @JavascriptInterface
        public String get_apikey() {
            return settings.getString("apikey", "");
        }

        @JavascriptInterface
        public void showSettings() {
            Intent intent = new Intent("com.stackunderflow.stackptr.StackPtrSettings");
            startActivity(intent);
        }
    }

}







