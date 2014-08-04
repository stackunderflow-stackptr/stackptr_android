package com.stackunderflow.stackptr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class StackPtrServiceStarter extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent SPServiceIntent = new Intent(context, StackPtrService.class);
            context.startService(SPServiceIntent);
        }
    }
}
