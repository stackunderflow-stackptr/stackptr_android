package com.stackunderflow.stackptr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.net.URLEncoder;

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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class StackPtr extends Activity {

	EditText userField;
	EditText passField;
	EditText apikeyField;
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

		userField = (EditText) findViewById(R.id.userField);
		passField = (EditText) findViewById(R.id.passField);
		apikeyField = (EditText) findViewById(R.id.ApiKeyField);
		statusField = (TextView) findViewById(R.id.statusView);
		debug = (CheckBox) findViewById(R.id.debug);

		Context ctx = getApplicationContext();
		settings = PreferenceManager.getDefaultSharedPreferences(ctx);

		editor = settings.edit();
		userField.setText(settings.getString("username", ""));
		passField.setText(settings.getString("password", ""));
		apikeyField.setText(settings.getString("apikey", ""));
		debug.setChecked(settings.getBoolean("debug", true));
	}

	@Override
	public void onStart() {
        super.onStart();
	}

    @Override
    public void onResume() {
        super.onResume();
        Toast.makeText(getBaseContext(), "onResume", Toast.LENGTH_SHORT).show();
        statusField.setText("Waiting for GPS...\n");
        fglm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        fgll = new StackPtrFGListener();
        fglm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0.0f, fgll);

    }

    @Override
    public void onPause() {
        super.onPause();
        fglm.removeUpdates(fgll);
        Toast.makeText(getBaseContext(), "onPause", Toast.LENGTH_SHORT).show();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.stack_ops, menu);
		return true;
	}

	public void doLogin(View view )  {

		String username = userField.getText().toString();
		String password = passField.getText().toString();
		Boolean debugEn = debug.isChecked();

		editor.putString("username", username);
		editor.putString("password", password);
		editor.putBoolean("debug", debugEn);
		editor.apply();
		
		new ApiGetTask().execute(username, password);
	}
	
	public void doStart(View view) {
		// check API key validity
		
		String apikey = apikeyField.getText().toString();

		editor.putString("apikey", apikey);
		editor.apply();
		
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
            publishProgress("Fetching user list...");

            String apikey = settings.getString("apikey", "");

            if (apikey.equals("")) {
                return "No API key set.";
            }

            try {
                // fetch user list token
                URL userurl = new URL("https://stackptr.com/users?apikey=" + apikey);
                HttpsURLConnection userConnection = (HttpsURLConnection) userurl.openConnection();
                //BufferedReader br = new BufferedReader(new InputStreamReader(userConnection.getInputStream()));
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

	private class ApiGetTask extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {
			publishProgress("Fetching token");
			String username = params[0];
			String password = params[1];
			try {
				CookieManager cookieManager = new CookieManager();
				CookieHandler.setDefault(cookieManager);

				// fetch CSRF token
				URL csrfurl = new URL("https://stackptr.com/csrf");
				HttpsURLConnection csrfConnection = (HttpsURLConnection) csrfurl.openConnection();
				BufferedReader br = new BufferedReader(new InputStreamReader(csrfConnection.getInputStream()));
				String token = br.readLine();
                br.close();
                csrfConnection.disconnect();

				editor = settings.edit();
				editor.putString("csrftoken", token);
				editor.apply();

				// now do the login
				//publishProgress("Sending login");
				URL loginurl = new URL("https://stackptr.com/login");
				HttpsURLConnection urlConnection2 = (HttpsURLConnection) loginurl.openConnection();
				urlConnection2.setRequestMethod("POST");
				urlConnection2.setDoOutput(true);
				urlConnection2.setDoInput(true);
				urlConnection2.setRequestProperty("X-CSRFToken", token);
				urlConnection2.setRequestProperty("Referer", "https://stackptr.com/login");
				urlConnection2.setInstanceFollowRedirects(false);
				OutputStream os = urlConnection2.getOutputStream();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
				writer.write("email="+URLEncoder.encode(username, "UTF-8")+
						"&password="+URLEncoder.encode(password, "UTF-8")+
						"&csrf_token="+URLEncoder.encode(token, "UTF-8"));
				writer.flush();
				writer.close();
				int responseCode = urlConnection2.getResponseCode();
				//BufferedReader br2 = new BufferedReader(new InputStreamReader(urlConnection2.getInputStream()));
				//String line;
				//while ((line = br2.readLine()) != null) {
				//	System.out.println(line);
				//}

				if (responseCode == 302) {
					publishProgress("Logged in successfully");
				} else {
					publishProgress("Login failed, check user and password");
					return "failed";
				}
                urlConnection2.disconnect();
				
				// now create the API key
				
				publishProgress("Creating API key");
				URL apikeyurl = new URL("https://stackptr.com/api/new");
				HttpsURLConnection uc3 = (HttpsURLConnection) apikeyurl.openConnection();
				uc3.setRequestMethod("POST");
				uc3.setDoOutput(true);
				uc3.setDoInput(true);
				uc3.setRequestProperty("X-CSRFToken", token);
				uc3.setRequestProperty("Referer", "https://stackptr.com/api/");
				OutputStream os2 = uc3.getOutputStream();
				BufferedWriter w3 = new BufferedWriter(new OutputStreamWriter(os2));
				String description = "StackPtr for Android on " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
				w3.write("description="+URLEncoder.encode(description, "UTF-8")+"&return=true");
				w3.flush();
				w3.close();
				int rc = uc3.getResponseCode();
				BufferedReader br3 = new BufferedReader(new InputStreamReader(uc3.getInputStream()));
				String key = br3.readLine();
				br3.close();
                uc3.disconnect();
                editor.putString("apikey", key);
				editor.apply();
				
			
			} catch (Exception e) {
				e.printStackTrace();
				publishProgress("error fetching form");
			}
			return "done";
		}

		@Override
		protected void onPostExecute(String result) {
			apikeyField.setText(settings.getString("apikey", ""));
		}


		@Override
		protected void onPreExecute() {
		}


		@Override
		protected void onProgressUpdate(String... text) {
			Toast.makeText(getBaseContext(), text[0], Toast.LENGTH_SHORT).show();
			System.out.println(text[0]);
		}

	}

    private class StackPtrFGListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            Toast.makeText(getBaseContext(), "Loc Updated", Toast.LENGTH_SHORT).show();
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


