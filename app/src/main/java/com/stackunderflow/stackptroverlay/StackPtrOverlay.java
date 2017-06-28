package com.stackunderflow.stackptroverlay;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.stackunderflow.stackptrapi.StackPtrApiGetUsers;
import com.stackunderflow.stackptrapi.StackPtrApiGetUsersParams;

public class StackPtrOverlay {
    private WindowManager wm;
    private LocationManager lm;
    private StackPtrOverlayLocationListener ll;
    private WindowManager.LayoutParams wmp;
    private StackPtrCompassViewGroup vg;
    private StackPtrApiGetUsersParams spagup;
    private Boolean overlayShown;
    private Location lastloc;

    public StackPtrOverlay(Context ctx) {
        wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        ll = new StackPtrOverlayLocationListener();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        spagup = new StackPtrApiGetUsersParams(settings);

        overlayShown = false;

        vg = new StackPtrCompassViewGroup(ctx);

        wmp = new WindowManager.LayoutParams(
                512,
                512,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        wmp.gravity = Gravity.TOP | Gravity.START;
        wmp.x = 0;
        wmp.y = 100;

        vg.setOnTouchListener(new StackPtrOverlayDragListener());
    }

    public void openOverlay() {
        if (!overlayShown) {
            wm.addView(vg, wmp);
            overlayShown = true;
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0.0f, ll);
            new StackPtrOverlayApiGetUsers().execute(spagup);
        }
    }

    public void closeOverlay() {
        if (vg != null && overlayShown) {
            wm.removeView(vg);
            overlayShown = false;
            lm.removeUpdates(ll);
        }
    }

    private class StackPtrOverlayDragListener implements View.OnTouchListener {
        private int initialX;
        private int initialY;
        private float initialTouchX;
        private float initialTouchY;
        private int hasMovedBy;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = wmp.x;
                    initialY = wmp.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    hasMovedBy = 0;
                    //System.out.printf("down\n");
                    return true;
                case MotionEvent.ACTION_UP:
                    if (hasMovedBy < 10) {
                        closeOverlay();
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    wmp.x = initialX + (int) (event.getRawX() - initialTouchX);
                    wmp.y = initialY + (int) (event.getRawY() - initialTouchY);
                    wm.updateViewLayout(vg, wmp);
                    hasMovedBy += (int) (event.getRawX() - initialTouchX);
                    hasMovedBy += (int) (event.getRawY() - initialTouchY);
                    return true;
                default:
                    return false;
            }
        }
    }

    private class StackPtrOverlayLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            System.out.println("overlay update!");
            lastloc = loc;
            new StackPtrOverlayApiGetUsers().execute(spagup);
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

    private class StackPtrOverlayApiGetUsers extends StackPtrApiGetUsers {
        @Override
        protected void onPostExecute(String result) {
            if (result.equals("")) {
                if (lastloc == null || lastloc.getProvider().equals("StackPtr")) {
                    lastloc = this.myLastServerLocation;
                }

                vg.updateDataAndRepaint(_jUsers, lastloc);
            } else {
                //Toast.makeText(ctx, result, Toast.LENGTH_LONG).show();
            }
        }
    }
}


