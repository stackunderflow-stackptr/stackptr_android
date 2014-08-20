package com.stackunderflow.stackptr;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;


public class StackPtrLogin extends Activity {

    EditText userField;
    EditText passField;
    EditText apikeyField;
    TextView statusField;
    TextView version;
    SharedPreferences settings;
    SharedPreferences.Editor editor;

    OkUrlFactory urlFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stack_ptr_login);

        userField = (EditText) findViewById(R.id.userField);
        passField = (EditText) findViewById(R.id.passField);
        apikeyField = (EditText) findViewById(R.id.ApiKeyField);
        version = (TextView) findViewById(R.id.version);

        Context ctx = getApplicationContext();
        settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        editor = settings.edit();

        userField.setText(settings.getString("username", ""));
        apikeyField.setText(settings.getString("apikey", ""));

        version.setText(String.format("Version %d", BuildConfig.VERSION_CODE));
        urlFactory = new OkUrlFactory(new OkHttpClient());

    }

    public void doLogin(View view )  {

        String username = userField.getText().toString();
        String password = passField.getText().toString();

        editor.putString("username", username);
        editor.apply();

        new ApiGetTask().execute(username, password);
    }

    public void scanQR(View view) {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.initiateScan();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            editor.putString("apikey", scanResult.getContents());
            editor.apply();
            apikeyField.setText(settings.getString("apikey", ""));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
		String apikey = apikeyField.getText().toString();

		editor.putString("apikey", apikey);
		editor.apply();
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
                String serverHost = settings.getString("server_address", "https://stackptr.com");
                URL csrfurl = new URL(serverHost + "/csrf");
                HttpURLConnection csrfConnection = urlFactory.open(csrfurl);
                BufferedReader br = new BufferedReader(new InputStreamReader(csrfConnection.getInputStream()));
                String token = br.readLine();
                br.close();
                csrfConnection.disconnect();

                editor = settings.edit();

                // now do the login
                //publishProgress("Sending login");
                URL loginurl = new URL(serverHost + "/login");
                HttpURLConnection urlConnection2 = urlFactory.open(loginurl);
                urlConnection2.setRequestMethod("POST");
                urlConnection2.setDoOutput(true);
                urlConnection2.setDoInput(true);
                urlConnection2.setRequestProperty("X-CSRFToken", token);
                urlConnection2.setRequestProperty("Referer", serverHost + "/login");
                urlConnection2.setInstanceFollowRedirects(false);

                OutputStream os = urlConnection2.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
                writer.write("email="+ URLEncoder.encode(username, "UTF-8")+
                        "&password="+URLEncoder.encode(password, "UTF-8")+
                        "&csrf_token="+URLEncoder.encode(token, "UTF-8"));
                writer.flush();
                writer.close();
                int responseCode = urlConnection2.getResponseCode();

                /*
                BufferedReader br2 = new BufferedReader(new InputStreamReader(urlConnection2.getInputStream()));
                String line;
                while ((line = br2.readLine()) != null) {
                	System.out.println(line);
                }
*/

                urlConnection2.disconnect();

                // TODO: fix properly so that this doesn't follow redirect
                if (responseCode == 302 || responseCode == 200) {
                    publishProgress("Logged in successfully, fetching API key");
                } else {
                    publishProgress("Login failed, check user and password");
                    return "Login failed";
                }

                // now create the API key

                publishProgress("Creating API key");
                URL apikeyurl = new URL(serverHost + "/api/new");
                HttpURLConnection uc3 = urlFactory.open(apikeyurl);
                uc3.setRequestMethod("POST");
                uc3.setDoOutput(true);
                uc3.setDoInput(true);
                uc3.setRequestProperty("X-CSRFToken", token);
                uc3.setRequestProperty("Referer", serverHost + "/api/");
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

                publishProgress("Created new API key.");


            } catch (Exception e) {
                e.printStackTrace();
                publishProgress("error fetching form");
            }
            return "Logged in successfully.";
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
            apikeyField.setText(settings.getString("apikey", ""));
        }


        @Override
        protected void onPreExecute() {
        }


        @Override
        protected void onProgressUpdate(String... text) {
            //Toast.makeText(getBaseContext(), text[0], Toast.LENGTH_SHORT).show();
            System.out.println(text[0]);
        }

    }


}
