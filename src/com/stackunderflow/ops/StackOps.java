package com.stackunderflow.ops;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class StackOps extends Activity {

	EditText userField;
	EditText passField;
	TextView statusField;
	SharedPreferences settings;
	SharedPreferences.Editor editor;
	Boolean loggedIn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stack_ops);
		
		userField = (EditText) findViewById(R.id.userField);
		passField = (EditText) findViewById(R.id.passField);
		statusField = (TextView) findViewById(R.id.statusView);
		
        Context ctx = getApplicationContext();
        settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        
        
		editor = settings.edit();
		userField.setText(settings.getString("username", ""));
		passField.setText(settings.getString("password", ""));
		
		loggedIn = false;
		
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		LocationListener locationListener = new StackLocationListener();
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1, locationListener);
		
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (settings.contains("username") && settings.contains("password")) {
		//	doLogin(null);
		}
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
		
		editor.putString("username", username);
		editor.putString("password", password);
		editor.apply();
		
		new LoginTask().execute(username, password);
		
		startService(new Intent(this, StackOpsService.class));
	}

	private class LoginTask extends AsyncTask<String, String, String> {

		  @Override
		  protected String doInBackground(String... params) {
			  publishProgress("Fetching token");
			  String username = params[0];
			  String password = params[1];
			  try {
				  CookieManager cookieManager = new CookieManager();
				  CookieHandler.setDefault(cookieManager);
				  
				  // fetch CSRF token
				  URL csrfurl = new URL("https://ops.stackunderflow.com/csrf");
				  HttpURLConnection urlConnection = (HttpURLConnection) csrfurl.openConnection();		  
				  BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
				  String token = br.readLine();
				  editor.putString("csrftoken", token);
				  editor.apply();
				  
				  // now do the login
				  publishProgress("Sending login");
				  URL loginurl = new URL("https://ops.stackunderflow.com/login");
				  HttpURLConnection urlConnection2 = (HttpURLConnection) loginurl.openConnection();
				  urlConnection2.setRequestMethod("POST");
				  urlConnection2.setDoOutput(true);
				  urlConnection2.setDoInput(true);
				  urlConnection2.setRequestProperty("X-CSRFToken", token);
				  urlConnection2.setRequestProperty("Referer", "https://ops.stackunderflow.com/login");
				  urlConnection2.setInstanceFollowRedirects(false);
				  OutputStream os = urlConnection2.getOutputStream();
				  BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
				  writer.write("email="+URLEncoder.encode(username, "UTF-8")+
						  	   "&password="+URLEncoder.encode(password, "UTF-8")+
						  	   "&csrf_token="+URLEncoder.encode(token, "UTF-8"));
				  writer.flush();
				  writer.close();
				  int responseCode = urlConnection2.getResponseCode();
				  BufferedReader br2 = new BufferedReader(new InputStreamReader(urlConnection2.getInputStream()));
				  String line;
				  while ((line = br2.readLine()) != null) {
				    System.out.println(line);
				  }
				  
				  if (responseCode == 302) {
					  loggedIn = true;
					  publishProgress("Logged in successfully");
				  } else {
					  loggedIn = false;
					  publishProgress("Login failed, check user and password");
				  }
				  
				  
			  } catch (Exception e) {
				  e.printStackTrace();
				  publishProgress("error fetching form");
			  }
		   return "done";
		  }

		  @Override
		  protected void onPostExecute(String result) {
		   // execution of result of Long time consuming operation
		   //finalResult.setText(result);
		  }


		  @Override
		  protected void onPreExecute() {
		   // Things to be done before execution of long running operation. For
		   // example showing ProgessDialog
		  }

		 
		  @Override
		  protected void onProgressUpdate(String... text) {
			  statusField.append(text[0] + "\n");
			  final int scrollAmount = statusField.getLayout().getLineTop(statusField.getLineCount()) - statusField.getHeight();
			    if (scrollAmount > 0)
			        statusField.scrollTo(0, scrollAmount);
			    else
			        statusField.scrollTo(0, 0);
			  System.out.println(text[0]);
		  }
	}
	
	private class UpdateLocationTask extends AsyncTask<Location, String, Location> {

		  @Override
		  protected Location doInBackground(Location... params) {
			  Location loc = params[0];
			  try {
				  // now do the login
				  publishProgress("updating location");
				  URL updateurl = new URL("https://ops.stackunderflow.com/update");
				  HttpURLConnection urlConnection2 = (HttpURLConnection) updateurl.openConnection();
				  urlConnection2.setRequestMethod("POST");
				  urlConnection2.setDoOutput(true);
				  urlConnection2.setDoInput(true);
				  String token = settings.getString("csrftoken", "");
				  urlConnection2.setRequestProperty("X-CSRFToken", token);
				  urlConnection2.setRequestProperty("Referer", "https://ops.stackunderflow.com/");
				  urlConnection2.setInstanceFollowRedirects(false);
				  OutputStream os = urlConnection2.getOutputStream();
				  BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
				  writer.write("lat="+URLEncoder.encode("" + loc.getLatitude(), "UTF-8")+
						  	   "&lon="+URLEncoder.encode("" + loc.getLongitude(), "UTF-8")+
						  	   "&csrf_token="+URLEncoder.encode(token, "UTF-8"));
				  writer.flush();
				  writer.close();
				  int responseCode = urlConnection2.getResponseCode();
				  InputStream in;
				  if(responseCode != 200) {
					    in = urlConnection2.getErrorStream();
					    publishProgress("Failed to update position: " + responseCode);
				  } else {
					    in = urlConnection2.getInputStream();
					    publishProgress("Successfully updated position");
				  }
				  BufferedReader br2 = new BufferedReader(new InputStreamReader(in));
				  String line;
				  while ((line = br2.readLine()) != null) {
				    System.out.println(line);
				  }				  
				  
			  } catch (Exception e) {
				  e.printStackTrace();
				  publishProgress("Exception updating pos");
			  }
		   return loc;
		  }

		  @Override
		  protected void onPostExecute(Location loc) {
		   // execution of result of Long time consuming operation
		   //finalResult.setText(result);
				Toast.makeText(
		                getBaseContext(),
		                "Location updated: Lat: " + loc.getLatitude() + " Lng: "
		                    + loc.getLongitude(), Toast.LENGTH_SHORT).show();
			  
		  }


		  @Override
		  protected void onPreExecute() {
		   // Things to be done before execution of long running operation. For
		   // example showing ProgessDialog
		  }

		 
		  @Override
		  protected void onProgressUpdate(String... text) {
			  statusField.append(text[0] + "\n");
			  System.out.println(text[0]);
		  }
	}
	
	private class StackLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location loc) {
			if (loggedIn) {
				new UpdateLocationTask().execute(loc);
			}
			
		}

		@Override
		public void onProviderDisabled(String arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderEnabled(String arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			// TODO Auto-generated method stub
			
		}
		
	
	}
	
}


