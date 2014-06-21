package com.stackunderflow.stackptr;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.widget.CheckBox;
import android.widget.TextView;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import org.json.JSONArray;
import org.json.JSONObject;

public class StackPtr extends Activity {

	TextView statusField;
	CheckBox debug;
	SharedPreferences settings;
	SharedPreferences.Editor editor;
    LocationManager fglm;
    LocationListener fgll;
    Location lastloc;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stack_ptr);

		statusField = (TextView) findViewById(R.id.statusView);
		debug = (CheckBox) findViewById(R.id.debug);

		Context ctx = getApplicationContext();
		settings = PreferenceManager.getDefaultSharedPreferences(ctx);

		editor = settings.edit();

		debug.setChecked(settings.getBoolean("debug", true));
	}

	@Override
	public void onStart() {
        super.onStart();
	}

    @Override
    public void onResume() {
        super.onResume();
        //Toast.makeText(getBaseContext(), "onResume", Toast.LENGTH_SHORT).show();
        statusField.setText("Waiting for GPS...\n");
        fglm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        fgll = new StackPtrFGListener();
        fglm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0.0f, fgll);

    }

    @Override
    public void onPause() {
        super.onPause();
        fglm.removeUpdates(fgll);
        //Toast.makeText(getBaseContext(), "onPause", Toast.LENGTH_SHORT).show();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.stack_ops, menu);
		return true;
	}

    public void doTest(View view) {
        Intent intent = new Intent("com.stackunderflow.stackptr.login");
        startActivity(intent);
    }
    

	public void doStart(View view) {
		// check API key validity

		startService(new Intent(this, StackPtrService.class));
	}

    public void doStop(View view) {
        stopService(new Intent(this, StackPtrService.class));
    }

    public void doRefreshUsers(View view) {
        new ApiGetUsers().execute();
    }   

    private class ApiGetUsers extends AsyncTask<Void, String, String> {
        @Override
        protected String doInBackground(Void... params) {

            OkUrlFactory ouf = new OkUrlFactory(new OkHttpClient());

            publishProgress("Fetching user list...");

            String apikey = settings.getString("apikey", "");

            if (apikey.equals("")) {
                return "No API key set.";
            }

            try {
                // fetch user list token
                URL userurl = new URL("https://stackptr.com/users?apikey=" + apikey);
                HttpURLConnection userConnection = ouf.open(userurl); //(HttpURLConnection) userurl.openConnection();
                //BufferedReader br = new BufferedReader(new InputStrea/mReader(userConnection.getInputStream()));
                //String token = br.readLine();

                int responseCode = userConnection.getResponseCode();
                if(responseCode != 200) {
                    publishProgress("Failed to update position: " + responseCode);
                    return "";
                }

                InputStream in = userConnection.getInputStream();
                BufferedReader br2 = new BufferedReader(new InputStreamReader(in));

                // todo: check for request success

                StringBuilder json = new StringBuilder();
                String line;
                while ((line = br2.readLine()) != null) {
                	json.append(line);
                }

                JSONObject jobj = new JSONObject(json.toString());

                JSONArray following = jobj.getJSONArray("following");

                StringBuilder res = new StringBuilder();

                if (lastloc == null) {
                    JSONObject me = jobj.getJSONObject("me");
                    JSONArray myloc = me.getJSONArray("loc");

                    double mylat = myloc.getDouble(0);
                    double mylon = myloc.getDouble(1);

                    lastloc = new Location("StackPtr");
                    lastloc.setLatitude(mylat);
                    lastloc.setLongitude(mylon);

                    res.append("Using last location from web\n");
                    // fixme: this is only printed once
                }




                for (int i=0; i<following.length(); i++) {
                    JSONObject thisUser = following.getJSONObject(i);
                    String user = thisUser.getString("user");
                    JSONArray loc_s = thisUser.getJSONArray("loc");
                    double lat = loc_s.getDouble(0);
                    double lon = loc_s.getDouble(1);
                    int lastupd = thisUser.getInt("lastupd");

                    Location userLocation = new Location("StackPtr");
                    userLocation.setLatitude(lat);
                    userLocation.setLongitude(lon);

                    float dist = lastloc.distanceTo(userLocation);
                    float bearing = lastloc.bearingTo(userLocation);

                    //String prog = String.format("%s %.2f %.0f %d\n",user,dist,bearing,lastupd);
                    //String prog = user + " " + dist + "m " + bearing + " deg " + lastupd + "s ago\n";

                    //String prog = "user: " + user + " lat: " + lat + " lon: " + lon + " lastupd " + lastupd + "\n";
                    String prog = user + " " + StackPtrUtils.distanceFormat(dist)
                                       + " " + StackPtrUtils.headingFormat(bearing)
                                       + " " + StackPtrUtils.timeFormat(lastupd) + "\n";
                    res.append(prog);
                }

                br2.close();
                userConnection.disconnect();
                return res.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "Error fetching list.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            statusField.setText(result + "\n");
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onProgressUpdate(String... text) {
            for (String p: text) {
                statusField.append(p + "\n");
            }
        }

    }


    private class StackPtrFGListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            //Toast.makeText(getBaseContext(), "Loc Updated", Toast.LENGTH_SHORT).show();
            lastloc = loc;
            new ApiGetUsers().execute();
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


