package com.stackunderflow.stackptr;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
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
    JSONObject jUsers;
    ArrayList<Integer> tUsers;
    JSONObject jMe;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        tUsers = new ArrayList<Integer>();

        setContentView(R.layout.activity_stack_ptr);

		Context ctx = getApplicationContext();
		settings = PreferenceManager.getDefaultSharedPreferences(ctx);

		editor = settings.edit();



        //////////
        ListView listview = (ListView) findViewById(R.id.listView);

        // requires context of the current Activity
        adapter = new UserArrayAdapter(this);
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
            /*
            ///// DEBUG DEBUG DEBUG
            Location l1 = new Location("StackTest");
            l1.setLatitude(35);
            l1.setLongitude(-140);
            Location l2 = new Location("StackTest");
            l2.setLatitude(-35);
            l2.setLongitude(140);
            Location l3 = new Location("StackTest");
            l3.setLatitude(-32);
            l3.setLongitude(143);


            System.out.println("l1->l2 bearing: " + l1.bearingTo(l2));     // -124.18
            System.out.println("l1->l2 distance: " + l1.distanceTo(l2));   // 1.13E7

            System.out.println("l2->l1 bearing: " + l2.bearingTo(l1));     // 55.81
            System.out.println("l2->l1 distance: " + l2.distanceTo(l1));   // 1.13E7


            System.out.println("l2->l3 bearing: " + l2.bearingTo(l3));     // 40.79
            System.out.println("l2->l3 distance: " + l2.distanceTo(l3));   // 434028.9

            System.out.println("l1->l3 bearing: " + l1.bearingTo(l3));     // -123.16089
            System.out.println("l1->l3 distance: " + l1.distanceTo(l3));   // 1.09E7
            ///// DEBUG DEBUG DEBUG
            // */

            OkUrlFactory urlFactory = new OkUrlFactory(new OkHttpClient());

            publishProgress("Fetching user list...");

            String apikey = settings.getString("apikey", "");
            String serverHost = settings.getString("server_address", "https://stackptr.com");

            if (apikey.equals("")) {
                return "No API key set.";
            }

            try {
                // fetch user list token

                URL userurl = new URL(serverHost + "/users?apikey=" + apikey);
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

                JSONArray jObj = new JSONArray(json.toString());

                jUsers = null;
                jMe = null;

                for (int i=0; i<jObj.length(); i++) {
                    JSONObject msg = jObj.getJSONObject(i);
                    String name = msg.getString("type");
                    if (name.equals("user")) {
                        jUsers = msg.getJSONObject("data");
                    } else if (name.equals("user-me")) {
                        jMe = msg.getJSONObject("data");
                    }
                }

                if (jUsers == null) {
                    return "no data";
                }

                if (jMe == null) {
                    return "no data";
                }


                //return "";

                //jUsers = jObj.getJSONArray("following");

                //if (jObj.isNull("me")) {
                    //publishProgress(getString(R.string.my_location_is_unknown));
                //    return "My location is unknown to server";
                //}
                //jMe = jObj.getJSONObject("me");

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




                Iterator<String> userIterator = jUsers.keys();
                while (userIterator.hasNext()) {
                    JSONObject thisUser = jUsers.getJSONObject(userIterator.next());
                    Integer user = thisUser.getInt("id");
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
            System.out.printf("%s\n", (Object) text);
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

    private class UserArrayAdapter extends ArrayAdapter<Integer> {
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
            //ImageButton mapButton = (ImageButton) rowView.findViewById(R.id.mapButton);

            try {
                JSONObject jUser = jUsers.getJSONObject(Integer.toString(tUsers.get(position))); // FIXME
                final String username = jUser.getString("username");
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
                        } else {
                            // Mainly a concern for the emulator and non-GApps devices
                            StackPtrUtils.showAlertDialog(context, getString(R.string.no_map_title), getString(R.string.no_map_message));
                        }
                    }
                });

                /*
                mapButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent("com.stackunderflow.stackptr.StackPtrMap");
                        startActivity(intent);
                    }
                });*/


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


