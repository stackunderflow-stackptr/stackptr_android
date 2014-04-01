package com.stackunderflow.ops;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;
import android.content.SharedPreferences;

public class StackOpsService extends Service {

	SharedPreferences settings;
	SharedPreferences.Editor editor;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
    public void onCreate() {
        Toast.makeText(this, "StackOps service launched", Toast.LENGTH_LONG).show();
        System.out.printf("service launched\n");

    }
	
	@Override
    public void onStart(Intent intent, int startId) {
    	// For time consuming an long tasks you can launch a new thread here...
        Toast.makeText(this, "StackOps service started", Toast.LENGTH_LONG).show();
        System.out.printf("service started\n");
        
        NotificationCompat.Builder mBuilder =
        	    new NotificationCompat.Builder(this)
        	    .setSmallIcon(R.drawable.ic_launcher)
        	    .setContentTitle("StackOps")
        	    .setContentText("Service started.")
        	    .setOngoing(true);
        int mNotificationId = 001;
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
        
        Context ctx = getApplicationContext();
        settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        
        //editor = settings.edit();
        String token = settings.getString("username", "none");
        
        mBuilder.setContentText("Service started 2 " + token);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());

        
        
		
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "StackOps service destroyed", Toast.LENGTH_LONG).show();
        System.out.printf("service destroyed\n");

    }

	
}
