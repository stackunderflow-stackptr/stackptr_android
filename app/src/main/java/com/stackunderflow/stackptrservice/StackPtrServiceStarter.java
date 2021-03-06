package com.stackunderflow.stackptrservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.stackunderflow.stackptrservice.StackPtrService;


public class StackPtrServiceStarter extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals(Intent.ACTION_BOOT_COMPLETED) || action.equals(Intent.ACTION_MY_PACKAGE_REPLACED)) {

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            boolean start_on_boot = settings.getBoolean("startup_autostart", false);

            if (start_on_boot) {
                Intent SPServiceIntent = new Intent(context, StackPtrService.class);
                context.startService(SPServiceIntent);
            }
        }
    }
}
