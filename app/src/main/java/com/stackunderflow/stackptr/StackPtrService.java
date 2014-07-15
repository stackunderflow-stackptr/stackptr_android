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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import android.os.BatteryManager;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import org.json.JSONObject;


public class StackPtrService extends Service {

	SharedPreferences settings;
	String apikey;
	NotificationCompat.Builder mBuilder;
	NotificationManager mNotifyMgr;
	static int mNotificationId = 1;

    private WindowManager wm;
    WindowManager.LayoutParams wmp;
    private ImageView iv;

    OkUrlFactory urlFactory;

    Intent batteryStatus;


    @Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
        super.onCreate();
		//Toast.makeText(this, "StackPtr service launched", Toast.LENGTH_LONG).show();
        urlFactory = new OkUrlFactory(new OkHttpClient());

        /*
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        iv = new ImageView(this);
        iv.setImageResource(R.drawable.ic_launcher);

       wmp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        wmp.gravity = Gravity.TOP | Gravity.LEFT;
        wmp.x = 0;
        wmp.y = 100;

        iv.setOnTouchListener(new StackButtonDragListener());
        wm.addView(iv, wmp);
        */

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

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryStatus = ctx.registerReceiver(null, ifilter);

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
		super.onDestroy();
        Toast.makeText(this, "StackPtr service destroyed", Toast.LENGTH_LONG).show();

        if (iv != null) {
            wm.removeView(iv);
        }
		//System.out.printf("service destroyed\n");
	}


	private class StackButtonDragListener implements View.OnTouchListener {

        private int initialX;
        private int initialY;
        private float initialTouchX;
        private float initialTouchY;
        private boolean hasMoved;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = wmp.x;
                    initialY = wmp.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    hasMoved = false;
                    System.out.printf("down\n");
                    return true;
                case MotionEvent.ACTION_UP:
                    if (hasMoved) {
                        System.out.printf("up, drag\n");
                    } else {
                        System.out.printf("clicked\n");
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    wmp.x = initialX + (int) (event.getRawX() - initialTouchX);
                    wmp.y = initialY + (int) (event.getRawY() - initialTouchY);
                    wm.updateViewLayout(iv, wmp);
                    hasMoved = true;
                    System.out.printf("move\n");
                    return true;
                default:
                    return false;
            }
        }
    }

	private class UpdateLocationTask extends AsyncTask<Location, String, Location> {

		@Override
		protected Location doInBackground(Location... params) {
			Location loc = params[0];
			try {
				CookieHandler.setDefault(null);
				publishProgress("updating location");
				URL updateurl = new URL("https://stackptr.com/update");
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

                if (loc.hasAltitude()) update.append("&alt=" + URLEncoder.encode("" + loc.getAltitude(), "UTF-8"));
                if (loc.hasBearing()) update.append("&hdg=" + URLEncoder.encode("" + loc.getBearing(), "UTF-8"));
                if (loc.hasSpeed()) update.append("&spd=" + URLEncoder.encode("" + loc.getSpeed(), "UTF-8"));

                HashMap<String,String> extra = new HashMap<String,String>();

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
