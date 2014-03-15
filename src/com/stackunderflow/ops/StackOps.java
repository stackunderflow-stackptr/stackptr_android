package com.stackunderflow.ops;

import java.io.BufferedInputStream;
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

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.content.SharedPreferences;
import android.view.Menu;
import android.widget.EditText;
import android.widget.TextView;

public class StackOps extends Activity {

	EditText userField;
	EditText passField;
	TextView statusField;
	SharedPreferences settings;
	SharedPreferences.Editor editor;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stack_ops);
		
		userField = (EditText) findViewById(R.id.userField);
		passField = (EditText) findViewById(R.id.passField);
		statusField = (TextView) findViewById(R.id.statusView);
		
		settings = getPreferences(MODE_PRIVATE);
		editor = settings.edit();
		userField.setText(settings.getString("username", ""));
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
		editor.apply();
		
		new LoginTask().execute(username, password);
		 
	}

	private class LoginTask extends AsyncTask<String, String, String> {

		  @Override
		  protected String doInBackground(String... params) {
			  publishProgress("Fetching CSRF token");
			  String username = params[0];
			  String password = params[1];
			  try {
				  CookieManager cookieManager = new CookieManager();
				  CookieHandler.setDefault(cookieManager);
				  
				  // fetch CSRF token
				  URL csrfurl = new URL("http://ops.stackunderflow.com/csrf");
				  HttpURLConnection urlConnection = (HttpURLConnection) csrfurl.openConnection();		  
				  BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
				  String token = br.readLine();
				  publishProgress("csrf token: " + token);
				  editor.putString("csrftoken", token);
				  editor.apply();
				  
				  // now do the login
				  publishProgress("doing login");
				  URL loginurl = new URL("http://ops.stackunderflow.com/login");
				  HttpURLConnection urlConnection2 = (HttpURLConnection) loginurl.openConnection();
				  urlConnection2.setRequestMethod("POST");
				  urlConnection2.setDoOutput(true);
				  urlConnection2.setDoInput(true);
				  urlConnection2.setRequestProperty("X-CSRFToken", token);
				  urlConnection2.setInstanceFollowRedirects(false);
				  OutputStream os = urlConnection2.getOutputStream();
				  BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
				  writer.write("email="+URLEncoder.encode(username, "UTF-8")+
						  	   "&password="+URLEncoder.encode(password, "UTF-8")+
						  	   "&csrf_token="+URLEncoder.encode(token, "UTF-8"));
				  writer.flush();
				  writer.close();
				  int responseCode = urlConnection2.getResponseCode();
				  publishProgress("response code is " + responseCode);
				  BufferedReader br2 = new BufferedReader(new InputStreamReader(urlConnection2.getInputStream()));
				  String line;
				  while ((line = br2.readLine()) != null) {
				    System.out.println(line);
				  }
				  publishProgress("done");
				  
				  
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
			  System.out.println(text[0]);
		  }
	}
	
}


