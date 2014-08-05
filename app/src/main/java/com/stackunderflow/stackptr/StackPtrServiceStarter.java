package com.stackunderflow.stackptr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public class StackPtrServiceStarter extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            boolean start_on_boot = settings.getBoolean("startup_autostart", true);

            if (start_on_boot) {
                Intent SPServiceIntent = new Intent(context, StackPtrService.class);
                context.startService(SPServiceIntent);
            }
        }
    }
}
