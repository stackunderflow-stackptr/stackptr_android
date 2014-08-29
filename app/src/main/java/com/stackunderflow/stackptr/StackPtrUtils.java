package com.stackunderflow.stackptr;

import android.content.Context;
import android.content.res.Resources;

/**
 * Created by gm on 9/06/2014.
 * Based on stackops-utils.js
 */
public class StackPtrUtils {
    public static String distanceFormat(double distance) {
        if (distance > 100000) {
            return String.format("%.0f km", distance/1000);
        } else if (distance > 1000) {
            return String.format("%.2f km", distance/1000);
        } else {
            return String.format("%.0f m", distance);
        }
    }

    public static String timeFormat(long time, boolean long_form, Context ctx) {
        time = (System.currentTimeMillis() / (long)1000) - time;
        Resources res = ctx.getResources();
        if (time == -1) {
            return res.getString(R.string.no_update);
        } else if (time < 60) {
            return res.getQuantityString(long_form ?
                    R.plurals.seconds_ago_long : R.plurals.seconds_ago_short, (int) time, time);
        } else if (time < 3600) {
            long v = time / 60;
            return res.getQuantityString(long_form ?
                    R.plurals.minutes_ago_long : R.plurals.minutes_ago_short, (int) v, v);
        } else if (time < 86400) {
            long v = time / 3600;
            return res.getQuantityString(long_form ?
                    R.plurals.hours_ago_long : R.plurals.hours_ago_short, (int)v, v);
        } else {
            long v = time / 86400;
            return res.getQuantityString(long_form ?
                    R.plurals.days_ago_long : R.plurals.days_ago_short, (int)v, v);
        }
    }

    public static String getShortCompassName(double heading, Context ctx) {
        // Sometimes we get back a -180 .. 180 value if it's a big distance?
        // TODO: Check the documentation to see if this is mentioned.
        heading %= 360;

        if (heading < 22.5) {
            return ctx.getString(R.string.compass_short_N);
        } else if (heading < 67.5) {
            return ctx.getString(R.string.compass_short_NE);
        } else if (heading < 112.5) {
            return ctx.getString(R.string.compass_short_E);
        } else if (heading < 157.5) {
            return ctx.getString(R.string.compass_short_SE);
        } else if (heading < 202.5) {
            return ctx.getString(R.string.compass_short_S);
        } else if (heading < 247.5) {
            return ctx.getString(R.string.compass_short_SW);
        } else if (heading < 292.5) {
            return ctx.getString(R.string.compass_short_W);
        } else if (heading < 337.5) {
            return ctx.getString(R.string.compass_short_NW);
        } else {
            return ctx.getString(R.string.compass_short_N);
        }
    }

    public static String getLongCompassName(double heading, Context ctx) {
        if (heading < 22.5) {
            return ctx.getString(R.string.compass_long_N);
        } else if (heading < 67.5) {
            return ctx.getString(R.string.compass_long_NE);
        } else if (heading < 112.5) {
            return ctx.getString(R.string.compass_long_E);
        } else if (heading < 157.5) {
            return ctx.getString(R.string.compass_long_SE);
        } else if (heading < 202.5) {
            return ctx.getString(R.string.compass_long_S);
        } else if (heading < 247.5) {
            return ctx.getString(R.string.compass_long_SW);
        } else if (heading < 292.5) {
            return ctx.getString(R.string.compass_long_W);
        } else if (heading < 337.5) {
            return ctx.getString(R.string.compass_long_NW);
        } else {
            return ctx.getString(R.string.compass_long_N);
        }
    }

    
}