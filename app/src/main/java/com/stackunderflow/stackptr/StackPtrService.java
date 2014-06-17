package com.stackunderflow.stackptr;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.URL;
import java.net.URLEncoder;

import com.stackunderflow.stackptr.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

import javax.net.ssl.HttpsURLConnection;

public class StackPtrService extends Service {

	SharedPreferences settings;
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
		//Toast.makeText(this, "StackPtr service launched", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Toast.makeText(this, "StackPtr service started", Toast.LENGTH_LONG).show();
		//System.out.printf("service started\n");

		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		LocationListener locationListener = new StackLocationListener();
		locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 5000, 5.0f, locationListener);
		
		Context ctx = getApplicationContext();
		settings = PreferenceManager.getDefaultSharedPreferences(ctx);
		apikey = settings.getString("apikey", "");
		debug = settings.getBoolean("debug", true);

		mBuilder = new NotificationCompat.Builder(this)
		.setSmallIcon(R.drawable.ic_launcher)
		.setContentTitle("StackPtr")
		.setContentText("Service started.")
		.setOngoing(true);
		
		mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification not = mBuilder.build();
		mNotifyMgr.notify(mNotificationId, not);
		
		startForeground(mNotificationId,not);
	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "StackPtr service destroyed", Toast.LENGTH_LONG).show();
		//System.out.printf("service destroyed\n");
	}


	

	private class UpdateLocationTask extends AsyncTask<Location, String, Location> {

		@Override
		protected Location doInBackground(Location... params) {
			Location loc = params[0];
			try {
				CookieHandler.setDefault(null);
				publishProgress("updating location");
				URL updateurl = new URL("https://stackptr.com/update");
				HttpsURLConnection updateConnection = (HttpsURLConnection) updateurl.openConnection();
                // ^ closed?
				updateConnection.setRequestMethod("POST");
				updateConnection.setDoOutput(true);
				updateConnection.setDoInput(true);
                updateConnection.setInstanceFollowRedirects(false);

                String apikey = settings.getString("apikey", "");
				OutputStream os = updateConnection.getOutputStream();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
				writer.write(
                        "lat=" + URLEncoder.encode("" + loc.getLatitude(), "UTF-8") +
                                "&lon=" + URLEncoder.encode("" + loc.getLongitude(), "UTF-8") +
                                "&alt=" + URLEncoder.encode("" + loc.getAltitude(), "UTF-8") +
                                "&hdg=" + URLEncoder.encode("" + loc.getBearing(), "UTF-8") +
                                "&spd=" + URLEncoder.encode("" + loc.getSpeed(), "UTF-8") +
                                "&apikey=" + URLEncoder.encode(apikey, "UTF-8")
                );
				writer.flush();
				writer.close();

				int responseCode = updateConnection.getResponseCode();
				if(responseCode != 200) {
					publishProgress("Failed to update position: " + responseCode);
                    return loc;
				}
                os.close();
                updateConnection.disconnect();

                //InputStream in = updateConnection.getInputStream();
				//publishProgress("Successfully updated position");
				//BufferedReader br2 = new BufferedReader(new InputStreamReader(in));
				//String line;
				//while ((line = br2.readLine()) != null) {
				//	System.out.println(line);
				//}
			} catch (Exception e) {
				//e.printStackTrace();
				//publishProgress("Exception updating pos");
			}
			return loc;
		}

		@Override
		protected void onPostExecute(Location loc) {
			Time current = new Time(Time.getCurrentTimezone());
			current.set(loc.getTime());
			String notification_text = "Lat: " + loc.getLatitude() + " Lon: " + loc.getLongitude() + " at: " + current.format("%k:%M:%S") + "from: " + loc.getProvider();
			mBuilder.setContentText(notification_text);
			
			NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
			inboxStyle.setBigContentTitle("Details:");
			inboxStyle.addLine("Loc: " + loc.getLatitude() + " " + loc.getLongitude());
            inboxStyle.addLine("at: " + current.format("%k:%M:%S"));
            inboxStyle.addLine("from: " + loc.getProvider());

            Context ctx = getApplicationContext();
            Intent appLaunchIntent = new Intent(ctx, StackPtr.class);
            PendingIntent appLaunchPendingIntent = PendingIntent.getActivity(ctx, 1, appLaunchIntent, Intent.FLAG_ACTIVITY_MULTIPLE_TASK | PendingIntent.FLAG_CANCEL_CURRENT);

			mBuilder.setStyle(inboxStyle);
            mBuilder.setContentIntent(appLaunchPendingIntent);
			mNotifyMgr.notify(mNotificationId, mBuilder.build());
			//Toast.makeText(getBaseContext(),notification_text, Toast.LENGTH_SHORT).show();
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected void onProgressUpdate(String... text) {
            //System.out.println(text[0]);
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
