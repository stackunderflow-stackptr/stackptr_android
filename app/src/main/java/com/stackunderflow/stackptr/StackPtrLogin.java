package com.stackunderflow.stackptr;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;
import com.squareup.picasso.Picasso;


import org.json.JSONException;
import org.json.JSONObject;


public class StackPtrLogin extends Activity {

    EditText userField;
    EditText passField;
    EditText apikeyField;
    TextView version;
    TextView statusTextField;
    TextView usernameView;
    TextView lblLoginHeader;
    ImageView avatarView;
    SharedPreferences settings;
    SharedPreferences.Editor editor;

    OkUrlFactory urlFactory;

    Context ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stack_ptr_login);

        userField = (EditText) findViewById(R.id.userField);
        passField = (EditText) findViewById(R.id.passField);
        apikeyField = (EditText) findViewById(R.id.ApiKeyField);
        version = (TextView) findViewById(R.id.version);
        statusTextField = (TextView) findViewById(R.id.statusTextField);
        avatarView = (ImageView) findViewById(R.id.avatarView);
        usernameView = (TextView) findViewById(R.id.usernameView);
        lblLoginHeader = (TextView) findViewById(R.id.lblLoginHeader);


        Context ctx = getApplicationContext();

        settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        editor = settings.edit();

        setServerLabel();

        userField.setText(settings.getString("username", ""));
        apikeyField.setText(settings.getString("apikey", ""));

        version.setText(String.format("Version %d", BuildConfig.VERSION_CODE));
        urlFactory = new OkUrlFactory(new OkHttpClient());

        checkAPILogin();

        apikeyField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    System.out.println("onfocuschange()");
                    saveAPIKey();
                    checkAPILogin();
                }
            }
        });
    }

    public void doLogin(View view )  {

        String username = userField.getText().toString();
        String password = passField.getText().toString();

        editor.putString("username", username);
        editor.apply();

        new ApiDoLogin().execute(username, password);
    }

    public void scanQR(View view) {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.initiateScan();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            String apikey = scanResult.getContents();
            apikeyField.setText(apikey);
            saveAPIKey();
            checkAPILogin();
        }
    }

    public void checkAPILogin() {
        new ApiCheckLogin().execute();
    }

    public void changeServer(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.title_server_address));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(settings.getString("server_address", StackPtrUtils.default_server));
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String server_addr = input.getText().toString();

                editor.putString("server_address", server_addr);
                editor.putString("apikey","");
                apikeyField.setText("");
                editor.apply();

                setServerLabel();
                checkAPILogin();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }

    public void setServerLabel() {
        String headingPrefix = getString(R.string.heading_create_key);
        String serverName = settings.getString("server_address", StackPtrUtils.default_server);
        try {
            URI stackPtrServer = new URI(serverName);
            String heading = headingPrefix + " " + stackPtrServer.getHost();
            lblLoginHeader.setText(heading);
        } catch (URISyntaxException e) {
            lblLoginHeader.setText(headingPrefix + " <invalid server>");
        }
    }

    public void saveAPIKey() {
        String apikey = apikeyField.getText().toString();
        editor.putString("apikey", apikey);
        editor.apply();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveAPIKey();
    }

    private class ApiCheckLogin extends AsyncTask<Void, String, JSONObject> {
        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                CookieHandler.setDefault(null);

                String apikey = settings.getString("apikey", "");

                String apikey_reason = StackPtrUtils.apiKeyValid(apikey);

                if (apikey_reason != null) {
                    publishProgress(apikey_reason);
                    return null;
                }

                String serverHost = settings.getString("server_address", StackPtrUtils.default_server);
                URL apikeyurl = new URL(serverHost + "/uid");

                HttpURLConnection uc3 = urlFactory.open(apikeyurl);
                uc3.setInstanceFollowRedirects(false);
                uc3.setRequestMethod("POST");
                uc3.setDoOutput(true);
                uc3.setDoInput(true);
                OutputStream os2 = uc3.getOutputStream();
                BufferedWriter w3 = new BufferedWriter(new OutputStreamWriter(os2));

                w3.write("apikey=" + URLEncoder.encode(apikey, "UTF-8"));
                w3.flush();
                w3.close();
                int rc = uc3.getResponseCode();
                System.out.println("rc = " + rc);

                if (rc == 302 || rc == 401) {
                    publishProgress("Failed to log into the server with provided API key");
                    return null;
                } else if (rc != 200) {
                    publishProgress("Failed to communicate with the server (response code was " + rc + ")");
                    return null;
                }

                InputStream in = uc3.getInputStream();
                BufferedReader br2 = new BufferedReader(new InputStreamReader(in));

                StringBuilder json = new StringBuilder();
                String line;
                while ((line = br2.readLine()) != null) {
                    json.append(line);
                }

                JSONObject jUser = new JSONObject(json.toString());

                br2.close();
                uc3.disconnect();

                return jUser;

            } catch (Exception e) {
                publishProgress(e.toString());
            }
            return null;

        }

        @Override
        protected void onPostExecute(JSONObject jUser) {
            if (jUser != null) {
                try {
                    String username = jUser.getString("username");
                    String icon = jUser.getString("icon");

                    Context ctx = getApplicationContext();
                    Picasso.with(ctx).load(icon).into(avatarView);

                    usernameView.setText(username);
                    statusTextField.setText("Successfully logged in.");

                } catch (JSONException e) {
                    statusTextField.setText("Error parsing server response.");
                }
            }
        }



        @Override
        protected void onProgressUpdate(String... text) {
            statusTextField.setText(text[0]);
        }


    }

    private class ApiDoLogin extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            publishProgress("Fetching token");
            String username = params[0];
            String password = params[1];
            try {
                CookieManager cookieManager = new CookieManager();
                CookieHandler.setDefault(cookieManager);

                // fetch CSRF token
                String serverHost = settings.getString("server_address", StackPtrUtils.default_server);
                URL csrfurl = new URL(serverHost + "/csrf");
                HttpURLConnection csrfConnection = urlFactory.open(csrfurl);
                BufferedReader br = new BufferedReader(new InputStreamReader(csrfConnection.getInputStream()));
                String token = br.readLine();
                br.close();
                csrfConnection.disconnect();

                editor = settings.edit();

                // now do the login
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


                urlConnection2.disconnect();

                if (responseCode == 302 || responseCode == 200) {
                    publishProgress(getString(R.string.login_success));
                } else {
                    publishProgress(getString(R.string.login_fail));
                    return "Login failed";
                }

                // now create the API key

                publishProgress(getString(R.string.creating_api_key));
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

                publishProgress(getString(R.string.created_api_key));
                return "Logged in successfully.";


            } catch (java.net.ConnectException e) {
                return "Failed to connect to server";
            } catch (Exception e) {
                e.printStackTrace();
                publishProgress(getString(R.string.error_login_form));
                return "Login failed";
            }

        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
            apikeyField.setText(settings.getString("apikey", ""));
            checkAPILogin();
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
