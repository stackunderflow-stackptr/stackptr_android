package com.stackunderflow.stackptr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import com.stackunderflow.stackptr.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;
import android.widget.Toast;

public class StackPtrService extends Service {

	SharedPreferences settings;
	SharedPreferences.Editor editor;
	Boolean loggedIn;
	String apikey;
	NotificationCompat.Builder mBuilder;
	NotificationManager mNotifyMgr;
	static int mNotificationId = 1;
	Boolean debug;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		Toast.makeText(this, "StackOps service launched", Toast.LENGTH_LONG).show();
		System.out.printf("service launched\n");

	}

	@Override
	public void onStart(Intent intent, int startId) {
		Toast.makeText(this, "StackOps service started", Toast.LENGTH_LONG).show();
		System.out.printf("service started\n");

		//loggedIn = false;

		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		LocationListener locationListener = new StackLocationListener();
		locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 5000, 5.0f, locationListener);
		
		Context ctx = getApplicationContext();
		settings = PreferenceManager.getDefaultSharedPreferences(ctx);
		apikey = settings.getString("apikey", "");
		debug = settings.getBoolean("debug", true);

		mBuilder = new NotificationCompat.Builder(this)
		.setSmallIcon(R.drawable.ic_launcher)
		.setContentTitle("StackOps")
		.setContentText("Service started.")
		.setOngoing(true);
		
		mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification not = mBuilder.build();
		mNotifyMgr.notify(mNotificationId, not);
		
		startForeground(1,not);
	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "StackOps service destroyed", Toast.LENGTH_LONG).show();
		System.out.printf("service destroyed\n");
	}


	

	private class UpdateLocationTask extends AsyncTask<Location, String, Location> {

		@Override
		protected Location doInBackground(Location... params) {
			Location loc = params[0];
			try {
				CookieHandler.setDefault(null);
				// now do the login
				publishProgress("updating location");
				URL updateurl = new URL("https://ops.stackunderflow.com/update");
				HttpURLConnection urlConnection2 = (HttpURLConnection) updateurl.openConnection();
				urlConnection2.setRequestMethod("POST");
				urlConnection2.setDoOutput(true);
				urlConnection2.setDoInput(true);
				String apikey = settings.getString("apikey", "");
				urlConnection2.setInstanceFollowRedirects(false);
				OutputStream os = urlConnection2.getOutputStream();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
				writer.write(
						"lat="+URLEncoder.encode("" + loc.getLatitude(), "UTF-8")+
						"&lon="+URLEncoder.encode("" + loc.getLongitude(), "UTF-8")+
						"&alt="+URLEncoder.encode("" + loc.getAltitude(), "UTF-8")+
						"&hdg="+URLEncoder.encode("" + loc.getBearing(), "UTF-8")+
						"&spd="+URLEncoder.encode("" + loc.getSpeed(), "UTF-8")+
						"&apikey="+URLEncoder.encode(apikey, "UTF-8"));
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
			Time current = new Time(Time.getCurrentTimezone());
			current.set(loc.getTime());
			String notification_text = "Lat: " + loc.getLatitude() + " Lng: " + loc.getLongitude() + " at: " + current.format("%k:%M:%S") + "from: " + loc.getProvider();
			mBuilder.setContentText(notification_text);
			
			NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
			inboxStyle.setBigContentTitle("Details:");
			inboxStyle.addLine("a");
			inboxStyle.addLine("b");
			inboxStyle.addLine("c");
			inboxStyle.addLine("d");
			mBuilder.setStyle(inboxStyle);
			mNotifyMgr.notify(mNotificationId, mBuilder.build());
			//Toast.makeText(getBaseContext(),notification_text, Toast.LENGTH_SHORT).show();
		}


		@Override
		protected void onPreExecute() {
		}


		@Override
		protected void onProgressUpdate(String... text) {
			System.out.println(text[0]);
		}
	}

	private class StackLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location loc) {
			//if (loggedIn) {
				new UpdateLocationTask().execute(loc);
			//} else {
			//	Toast.makeText(getBaseContext(), "Tried to update location but you are not logged in.", Toast.LENGTH_SHORT).show();
			//}
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
