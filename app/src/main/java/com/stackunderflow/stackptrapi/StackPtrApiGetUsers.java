package com.stackunderflow.stackptrapi;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class StackPtrApiGetUsers extends AsyncTask<StackPtrApiGetUsersParams, String, String> {
    public JSONArray _jUsers;
    public Location myLastServerLocation;

    @Override
    protected String doInBackground(StackPtrApiGetUsersParams... params) {

        SharedPreferences settings = params[0].settings;

        OkUrlFactory urlFactory = new OkUrlFactory(new OkHttpClient());

        publishProgress("Fetching user list...");

        String apikey = settings.getString("apikey", "");
        String serverHost = settings.getString("server_address", "https://stackptr.com");

        if (apikey == null || apikey.equals("")) {
            return "No API key set.";
        }

        try {
            URL userurl = new URL(serverHost + "/users?apikey=" + apikey);
            HttpURLConnection userConnection = urlFactory.open(userurl);

            int responseCode = userConnection.getResponseCode();
            if(responseCode != 200) {
                return "Failed to update users: " + responseCode;
            }

            InputStream in = userConnection.getInputStream();
            BufferedReader br2 = new BufferedReader(new InputStreamReader(in));

            StringBuilder json = new StringBuilder();
            String line;
            while ((line = br2.readLine()) != null) {
                json.append(line);
            }

            JSONArray jUsersResponse = new JSONArray(json.toString());

            _jUsers = null;
            JSONObject jMe = null;

            // fixme: more than one object?
            for (int i=0; i<jUsersResponse.length(); i++) {
                JSONObject msg = jUsersResponse.getJSONObject(i);
                String name = msg.getString("type");
                if (name.equals("user")) {
                    _jUsers = msg.getJSONArray("data");
                } else if (name.equals("user-me")) {
                    jMe = msg.getJSONObject("data");
                }
            }

            if (_jUsers == null) return "no user data object in response";
            if (jMe == null) return "no me object in response";

            JSONArray myloc = jMe.getJSONArray("loc");
            double mylat = myloc.getDouble(0);
            double mylon = myloc.getDouble(1);
            myLastServerLocation  = new Location("StackPtr");
            myLastServerLocation.setLatitude(mylat);
            myLastServerLocation.setLongitude(mylon);

            br2.close();
            userConnection.disconnect();
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error fetching list.";
        }
    }

    @Override
    protected void onPostExecute(String result) {
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    protected void onProgressUpdate(String... text) {
        System.out.printf("%s\n", (Object) text);
    }

}
