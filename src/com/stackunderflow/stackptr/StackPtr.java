package com.stackunderflow.stackptr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import com.stackunderflow.stackptr.R;

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

public class StackPtr extends Activity {

	EditText userField;
	EditText passField;
	EditText apikeyField;
	TextView statusField;
	CheckBox debug;
	SharedPreferences settings;
	SharedPreferences.Editor editor;

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
				URL csrfurl = new URL("https://ops.stackunderflow.com/csrf");
				HttpURLConnection urlConnection = (HttpURLConnection) csrfurl.openConnection();		  
				BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
				String token = br.readLine();

				editor = settings.edit();
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
					publishProgress("Logged in successfully");
				} else {
					publishProgress("Login failed, check user and password");
				}
				
				// now create the API key
				
				publishProgress("Creating API key");
				URL apikeyurl = new URL("https://ops.stackunderflow.com/api/new");
				HttpURLConnection uc3 = (HttpURLConnection) apikeyurl.openConnection();
				uc3.setRequestMethod("POST");
				uc3.setDoOutput(true);
				uc3.setDoInput(true);
				uc3.setRequestProperty("X-CSRFToken", token);
				uc3.setRequestProperty("Referer", "https://ops.stackunderflow.com/api/");
				OutputStream os2 = uc3.getOutputStream();
				BufferedWriter w3 = new BufferedWriter(new OutputStreamWriter(os2));
				String description = "StackPtr for Android on " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
				w3.write("description="+URLEncoder.encode(description, "UTF-8")+"&return=true");
				w3.flush();
				w3.close();
				int rc = uc3.getResponseCode();
				BufferedReader br3 = new BufferedReader(new InputStreamReader(uc3.getInputStream()));
				String key = br3.readLine();
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
	
}


