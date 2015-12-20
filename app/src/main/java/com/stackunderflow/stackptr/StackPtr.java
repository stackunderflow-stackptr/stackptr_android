package com.stackunderflow.stackptr;

import android.app.ActivityManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;


public class StackPtr extends Activity {
    SharedPreferences settings;
    LocationManager fglm;
    LocationListener fgll;
    WebView wv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_stack_ptr);
        Context ctx = getApplicationContext();
        settings = PreferenceManager.getDefaultSharedPreferences(ctx);


        //////////

        String apikey = settings.getString("apikey", "");

        wv = (WebView) findViewById(R.id.webview);
        wv.setBackgroundColor(Color.TRANSPARENT);

        CookieSyncManager.createInstance(wv.getContext());
        CookieManager.setAcceptFileSchemeCookies(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieSyncManager.getInstance().startSync();

        WebSettings ws = wv.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setAllowUniversalAccessFromFileURLs(true);

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
        CookieSyncManager.getInstance().startSync();

        fglm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        fgll = new StackPtrFGListener();
        fglm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0.0f, fgll);

        wv.loadUrl("javascript:StackPtrConnect()");

    }

    @Override
    public void onPause() {
        super.onPause();
        fglm.removeUpdates(fgll);
        CookieSyncManager.getInstance().stopSync();

        wv.loadUrl("javascript:StackPtrDisconnect()");

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

    private class StackPtrFGListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
        }

        @Override
        public void onProviderDisabled(String arg0) {
        }

        @Override
        public void onProviderEnabled(String arg0) {
        }

        @Override
        public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        }
    }

}







