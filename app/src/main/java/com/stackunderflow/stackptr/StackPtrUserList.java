package com.stackunderflow.stackptr;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.stackunderflow.stackptrapi.StackPtrApiGetUsers;
import com.stackunderflow.stackptrapi.StackPtrApiGetUsersParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class StackPtrUserList extends Activity {

    ArrayList<Integer> tUsers;
    SharedPreferences settings;
    StackPtrApiGetUsersParams spagup;
    StackPtrListViewArrayAdaptor adapter;
    JSONArray jUsers;
    StackPtrCompassViewGroup spcvg;
    Location lastloc;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stack_ptr_user_list);

        tUsers = new ArrayList<Integer>();

        setContentView(R.layout.activity_stack_ptr);
        Context ctx = getApplicationContext();
        settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        spagup = new StackPtrApiGetUsersParams(settings);

        ListView listview = (ListView) findViewById(R.id.listView);
        adapter = new StackPtrListViewArrayAdaptor(this);
        listview.setAdapter(adapter);

        spcvg = new StackPtrCompassViewGroup(ctx);
        spcvg.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200));

        listview.addHeaderView(spcvg);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_stack_ptr_user_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        new StackPtrMainScreenApiGetUsers().execute(spagup);

    }



private class StackPtrListViewArrayAdaptor extends ArrayAdapter<Integer> {
    private final Context context;

    public StackPtrListViewArrayAdaptor(Context context) {
        super(context, R.layout.userview, tUsers);
        this.context = context;
    }

    @Override
    public View getView(int position, View rowView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (rowView == null) {
            rowView = inflater.inflate(R.layout.userview, parent, false);
        }
        TextView firstLine = (TextView) rowView.findViewById(R.id.firstLine);
        TextView secondLine = (TextView) rowView.findViewById(R.id.secondLine);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        //ImageButton mapButton = (ImageButton) rowView.findViewById(R.id.mapButton);

        try {
            JSONObject jUser = jUsers.getJSONObject(position);
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

    private class StackPtrMainScreenApiGetUsers extends StackPtrApiGetUsers {
        @Override
        protected void onPostExecute(String result) {
            if (result.equals("")) {
                jUsers = _jUsers;
                adapter.notifyDataSetChanged();

                if (lastloc == null || lastloc.getProvider().equals("StackPtr")) {
                    lastloc = this.myLastServerLocation;
                }

                try {
                    tUsers.clear();
                    for (int i = 0; i < _jUsers.length(); i++) {
                        JSONObject thisUser = _jUsers.getJSONObject(i);
                        Integer user = thisUser.getInt("id");
                        tUsers.add(user);
                    }
                } catch (JSONException e) {
                    System.out.println(e);
                }

                spcvg.updateDataAndRepaint(_jUsers, lastloc);
            } else {
                Toast.makeText(getApplication().getBaseContext(), result, Toast.LENGTH_LONG).show();
            }
        }
    }

    private class StackPtrFGListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            lastloc = loc;
            new StackPtrMainScreenApiGetUsers().execute(spagup);
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



