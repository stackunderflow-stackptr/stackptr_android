package com.stackunderflow.stackptr;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StackPtr extends Activity {

	CheckBox debug;
	SharedPreferences settings;
	SharedPreferences.Editor editor;
    LocationManager fglm;
    LocationListener fgll;
    Location lastloc;

    UserArrayAdapter adapter;
    JSONArray jUsers;
    ArrayList<String> tUsers;
    JSONObject jMe;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        tUsers = new ArrayList<String>();

        setContentView(R.layout.activity_stack_ptr);

		Context ctx = getApplicationContext();
		settings = PreferenceManager.getDefaultSharedPreferences(ctx);

		editor = settings.edit();


        //////////
        ListView listview = (ListView) findViewById(R.id.listView);

        adapter = new UserArrayAdapter(ctx);
        listview.setAdapter(adapter);

        /*listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                view.animate().setDuration(2000).alpha(0)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                list.remove(item);
                                adapter.notifyDataSetChanged();
                                view.setAlpha(1);
                            }
                        }
                 );
            }

        });*/
	}

	@Override
	public void onStart() {
        super.onStart();
	}

    @Override
    public void onResume() {
        super.onResume();
        //Toast.makeText(getBaseContext(), "onResume", Toast.LENGTH_SHORT).show();
        //statusField.setText("Waiting for GPS...\n");
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
		getMenuInflater().inflate(R.menu.stackptr_menu, menu);
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent("com.stackunderflow.stackptr.StackPtrSettings");
                startActivity(intent);
                return true;
            case R.id.action_refresh_userlist:
                new ApiGetUsers().execute();
                return true;
            case R.id.action_start_service:
                // TODO: Check API key validity
                startService(new Intent(this, StackPtrService.class));
                return true;
            case R.id.action_stop_service:
                stopService(new Intent(this, StackPtrService.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private class ApiGetUsers extends AsyncTask<Void, String, String> {
        @Override
        protected String doInBackground(Void... params) {

            OkUrlFactory urlFactory = new OkUrlFactory(new OkHttpClient());

            publishProgress("Fetching user list...");

            String apikey = settings.getString("apikey", "");

            if (apikey.equals("")) {
                return "No API key set.";
            }

            try {
                // fetch user list token
                URL userurl = new URL("https://stackptr.com/users?apikey=" + apikey);
                HttpURLConnection userConnection = urlFactory.open(userurl);
                //(HttpURLConnection) userurl.openConnection();
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

                JSONObject jObj = new JSONObject(json.toString());

                jUsers = jObj.getJSONArray("following");
                jMe = jObj.getJSONObject("me");

                //StringBuilder res = new StringBuilder();
                //tUsers = new ArrayList<String>();
                tUsers.clear();

                if (lastloc == null) {

                    JSONArray myloc = jMe.getJSONArray("loc");

                    double mylat = myloc.getDouble(0);
                    double mylon = myloc.getDouble(1);

                    lastloc = new Location("StackPtr");
                    lastloc.setLatitude(mylat);
                    lastloc.setLongitude(mylon);

                    //res.append("Using last location from web\n");
                    // fixme: this is only printed once
                }




                for (int i=0; i<jUsers.length(); i++) {
                    JSONObject thisUser = jUsers.getJSONObject(i);
                    String user = thisUser.getString("user");
                    tUsers.add(user);
                    /*JSONArray loc_s = thisUser.getJSONArray("loc");
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
                    res.append(prog);*/
                }

                br2.close();
                userConnection.disconnect();
                return "";//res.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "Error fetching list.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            adapter.notifyDataSetChanged();
            //System.out.printf("notifyDataSetChanged()\n");
            //statusField.setText(result + "\n");
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onProgressUpdate(String... text) {
            //for (String p: text) {
            //    statusField.append(p + "\n");
            //}
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

    private class UserArrayAdapter extends ArrayAdapter<String> {
        private final Context context;

        public UserArrayAdapter(Context context) {
            super(context, R.layout.userview, tUsers);
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.userview, parent, false);
            TextView firstLine = (TextView) rowView.findViewById(R.id.firstLine);
            TextView secondLine = (TextView) rowView.findViewById(R.id.secondLine);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);

            try {
                JSONObject jUser = jUsers.getJSONObject(position);
                final String username = jUser.getString("user");
                firstLine.setText(username);

                JSONArray jLoc = jUser.getJSONArray("loc");
                final double lat = jLoc.getDouble(0);
                final double lon = jLoc.getDouble(1);
                int lastupd = jUser.getInt("lastupd");

                Location userLocation = new Location("StackPtr");
                userLocation.setLatitude(lat);
                userLocation.setLongitude(lon);

                float dist = lastloc.distanceTo(userLocation);
                float bearing = lastloc.bearingTo(userLocation);


                String prog =  StackPtrUtils.distanceFormat(dist)
                       + " " + StackPtrUtils.getShortCompassName(bearing, context)
                       + " " + StackPtrUtils.timeFormat(lastupd, false, context) + "\n";

                String longProg = StackPtrUtils.distanceFormat(dist)
                        + " " + StackPtrUtils.getLongCompassName(bearing, context)
                        + " " + StackPtrUtils.timeFormat(lastupd, true, context) + "\n";

                secondLine.setText(prog);
                secondLine.setContentDescription(longProg);

                String iconURL = jUser.getString("icon");

                //System.out.println(iconURL);
                Picasso.with(context).load(iconURL).into(imageView);

                rowView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Uri.Builder b = new Uri.Builder();
                        b.scheme("geo");
                        b.encodedOpaquePart(lat + "," + lon + "?q=" + lat + "," + lon + "(" + username + ")");

                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(b.build());
                        if (i.resolveActivity(getPackageManager()) != null) {
                            startActivity(i);
                        }
                    }
                });


            } catch (JSONException e) {
                e.printStackTrace();
            }

            return rowView;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }

}


