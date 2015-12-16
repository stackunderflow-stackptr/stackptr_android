package com.stackunderflow.stackptr;

import java.net.URLEncoder;
import java.util.ArrayList;

import android.app.ActivityManager;
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


    public class StackPtrAndroidShim {
        Activity parent;

        /** Instantiate the interface and set the context */
        StackPtrAndroidShim(Activity p) {
            parent = p;
            System.out.println("init shim");
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

        @JavascriptInterface
        public String serviceRunning() {
            ActivityManager m = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : m.getRunningServices(Integer.MAX_VALUE)) {
                String serviceName = service.service.getClassName();
                if (serviceName.equals("com.stackunderflow.stackptr.StackPtrService")) {
                    return "true";
                }
            }
            return "false";
        }

        @JavascriptInterface
        public void serviceStop() {
                stopService(new Intent(parent, StackPtrService.class));
        }

        @JavascriptInterface
        public void serviceStart() {
            System.out.println("starting service");
            startService(new Intent(parent, StackPtrService.class));
        }
    }

}







