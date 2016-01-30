package com.stackunderflow.stackptr;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.os.BatteryManager;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import org.json.JSONObject;

import static android.content.Intent.*;


public class StackPtrService extends Service {

	SharedPreferences settings;
	String apikey;
	NotificationCompat.Builder mBuilder;
	NotificationManager mNotifyMgr;
	static int mNotificationId = 1;

    OkUrlFactory urlFactory;

    StackPtrOverlay overlay;

    Context ctx;
    String versioncode;
    IntentFilter ifilter;
    Boolean hasStarted = false;


    @Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
        super.onCreate();
        ctx = getApplicationContext();
        urlFactory = new OkUrlFactory(new OkHttpClient());

        overlay = new StackPtrOverlay(ctx);
	}

	@Override
	public synchronized void onStart(Intent intent, int startId) {
        if (!hasStarted) {
            hasStarted = true;
            Toast.makeText(this, getString(R.string.service_started), Toast.LENGTH_LONG).show();
            //System.out.printf("service started\n");

            settings = PreferenceManager.getDefaultSharedPreferences(ctx);
            apikey = settings.getString("apikey", "");

            // check API key "looks" valid here i.e. right length and is at least set

            String apikey_reason = StackPtrUtils.apiKeyValid(apikey);

            if (apikey_reason != null) {
                Toast.makeText(this, apikey_reason, Toast.LENGTH_LONG).show();
                updateMessage("Service not started: " + apikey_reason);
            }

            int bg_update_time;
            try {
                bg_update_time = Integer.parseInt(settings.getString("update_interval", "5"));
            } catch (NumberFormatException e) {
                Toast.makeText(this, getString(R.string.invalid_update_interval), Toast.LENGTH_LONG).show();
                bg_update_time = 5;
            }
            boolean update_net_in_bg = settings.getBoolean("updates_network_in_bg", true);

            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            LocationListener locationListener = new StackLocationListener();
            locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 5000, 5.0f, locationListener);
            if (update_net_in_bg) {
                try {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60 * bg_update_time * 1000, 0.0f, locationListener);
                } catch (IllegalArgumentException e) {
                    Toast.makeText(this, getString(R.string.networkprovider_fail), Toast.LENGTH_LONG).show();

                }
            }

            ifilter = new IntentFilter(ACTION_BATTERY_CHANGED);
            versioncode = String.format("%d", BuildConfig.VERSION_CODE);

            PendingIntent overlayPendingIntent = PendingIntent.getBroadcast(ctx, 2, new Intent("com.stackunderflow.stackptr.overlay"), PendingIntent.FLAG_CANCEL_CURRENT);

            mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.service_started))
                    .setContentIntent(overlayPendingIntent)
                    .setOngoing(true);

            mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Notification not = mBuilder.build();
            mNotifyMgr.notify(mNotificationId, not);

            startForeground(mNotificationId, not);



            /////
            IntentFilter filter = new IntentFilter("com.stackunderflow.stackptr.overlay");
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    overlay.openOverlay();
                }
            };
            registerReceiver(receiver, filter);

        }
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
        Toast.makeText(this, getString(R.string.service_destroyed), Toast.LENGTH_LONG).show();
        overlay.closeOverlay();

	}

    public void updateMessage(String message) {
        mBuilder.setContentText(message);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("StackPtr");
        inboxStyle.addLine(message);

        Context ctx = getApplicationContext();
        PendingIntent overlayPendingIntent = PendingIntent.getBroadcast(ctx, 2, new Intent("com.stackunderflow.stackptr.overlay"), PendingIntent.FLAG_CANCEL_CURRENT);

        mBuilder.setStyle(inboxStyle);
        mBuilder.setContentIntent(overlayPendingIntent);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

	private class UpdateLocationTask extends AsyncTask<Location, String, Location> {

		@Override
		protected Location doInBackground(Location... params) {
			Location loc = params[0];
			try {
				CookieHandler.setDefault(null);
				publishProgress("Updating location");
                URL updateurl = new URL(settings.getString("server_address", StackPtrUtils.default_server) + "/update");
				//HttpsURLConnection updateConnection = (HttpsURLConnection) updateurl.openConnection();
                HttpURLConnection updateConnection = urlFactory.open(updateurl);
                // ^ closed?
				updateConnection.setRequestMethod("POST");
				updateConnection.setDoOutput(true);
				updateConnection.setDoInput(true);
                updateConnection.setInstanceFollowRedirects(false);

                String apikey = settings.getString("apikey", "");
				OutputStream os = updateConnection.getOutputStream();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));

                StringBuilder update = new StringBuilder();
                update.append("lat=" + URLEncoder.encode("" + loc.getLatitude(), "UTF-8"));
                update.append("&lon=" + URLEncoder.encode("" + loc.getLongitude(), "UTF-8"));

                if (loc.hasAltitude()) {
                    // Send altitude to 1 metre accuracy
                    update.append(String.format("&alt=%.0f", loc.getAltitude()));
                }

                if (loc.hasBearing()) {
                    // Send bearing to 1 degree accuracy
                    update.append(String.format("&hdg=%.0f", loc.getBearing()));
                }

                if (loc.hasSpeed()) {
                    // Send speed to 0.01m/s accuracy (about 0.036km/h)
                    update.append(String.format("&spd=%.2f", loc.getSpeed()));
                }

                HashMap<String,String> extra = new HashMap<String,String>();

                if (loc.hasAccuracy()) {
                    // Android defines this as the area that it is 68% confident the device is
                    // within.  We send this to an accuracy of 1 metre.
                    extra.put("acc", String.format("%.0f", loc.getAccuracy()));
                }

                Intent batteryStatus = ctx.registerReceiver(null, ifilter);
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

                extra.put("bat",String.format("%.2f", (float) level / (float) scale));

                switch (status) {
                    case BatteryManager.BATTERY_STATUS_CHARGING:
                        extra.put("bst","charging");
                        break;
                    case BatteryManager.BATTERY_STATUS_FULL:
                        extra.put("bst","full");
                        break;
                    default:
                        extra.put("bst","discharging");
                        break;
                }

                extra.put("prov",loc.getProvider());
                extra.put("v", versioncode);

                JSONObject extraJ = new JSONObject(extra);
                update.append("&ext=" + URLEncoder.encode(extraJ.toString(), "UTF-8"));

                update.append("&apikey=" + URLEncoder.encode(apikey, "UTF-8"));

                writer.write(update.toString());
				writer.flush();
				writer.close();

				int responseCode = updateConnection.getResponseCode();
				if(responseCode != 200) {
					publishProgress("Failed to update position: " + responseCode);
                    return loc;
				}
                os.close();
                updateConnection.disconnect();

			} catch (Exception e) {
				e.printStackTrace();
				publishProgress("Exception updating position");
			}
			return loc;
        }

        @Override
		protected void onPostExecute(Location loc) {
			Time current = new Time(Time.getCurrentTimezone());
			current.set(loc.getTime());
            String notification_text = "Updated at " + current.format("%k:%M:%S") + " with " + loc.getProvider();
			mBuilder.setContentText(notification_text);
			
			NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
			inboxStyle.setBigContentTitle("StackPtr");
            inboxStyle.addLine("Updated at " + current.format("%k:%M:%S") + " with " + loc.getProvider());

            Context ctx = getApplicationContext();
            //Intent appLaunchIntent = new Intent(ctx, StackPtr.class);
            //PendingIntent appLaunchPendingIntent = PendingIntent.getActivity(ctx, 1, appLaunchIntent, PendingIntent.FLAG_CANCEL_CURRENT); // FLAG_ACTIVITY_MULTIPLE_TASK |

            PendingIntent overlayPendingIntent = PendingIntent.getBroadcast(ctx, 2, new Intent("com.stackunderflow.stackptr.overlay"), PendingIntent.FLAG_CANCEL_CURRENT);

            mBuilder.setStyle(inboxStyle);
            mBuilder.setContentIntent(overlayPendingIntent);
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

        Location prevLoc = null;

        private boolean isGPSLoc(Location loc) {
            return !loc.getProvider().equals("gps");
        }

        private void rejectLocation(Location loc, String reason) {
            Time current = new Time(Time.getCurrentTimezone());
            current.set(loc.getTime());
            String notification_text = String.format("%s @ %s %s",loc.getProvider(), current.format("%k:%M:%S"), reason);
            updateMessage(notification_text);
        }

		@Override
		public void onLocationChanged(Location loc) {

            if (prevLoc != null) {
                long timeBetween = loc.getTime() - prevLoc.getTime();

                if (!isGPSLoc(loc) && isGPSLoc(prevLoc) && (timeBetween < 10)) {
                    // reject network positions if we have a GPS fix <10s ago
                    rejectLocation(loc, String.format("already have GPS fix %d ago", timeBetween));
                    return;
                }

                float distanceBetween = prevLoc.distanceTo(loc);
                float speed = (distanceBetween / (float)timeBetween) * 3.6f; // km/h

                if (!isGPSLoc(loc) && (speed > 200.0f)) {
                    // reject non-GPS fixes more than 200km/h away from current location
                    rejectLocation(loc, String.format("average speed %.0fkm/h", speed));
                    return;
                }
            }

            if (loc.getAccuracy() > 150.0) {
                rejectLocation(loc, String.format("too inaccurate (%.0fm)", + loc.getAccuracy()));
                return;
            }

            prevLoc = loc;
			new UpdateLocationTask().execute(loc);
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
