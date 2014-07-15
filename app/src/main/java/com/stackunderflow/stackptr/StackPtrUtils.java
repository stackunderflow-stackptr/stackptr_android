package com.stackunderflow.stackptr;

/**
 * Created by gm on 9/06/2014.
 * Based on stackops-utils.js
 */
public class StackPtrUtils {
    public static String distanceFormat(double distance) {
        if (distance > 1000) {
            return String.format("%.2f km", distance/1000);
        } else {
            return String.format("%.0f m", distance);
        }
    }

    public static String timeFormat(int time) {
        if (time == -1) {
            return "no upd";
        } else if (time < 60) {
            return String.format("%ds ago", time);
        } else if (time < 3600) {
            return String.format("%dm ago", time / 60);
        } else if (time < 28800) {
            return String.format("%dh%dm ago", time / 3600, (time % 3600) / 60);
        } else if (time < 86400) {
            return String.format("%dh ago", time / 3600);
        } else {
            return String.format("%dd ago", time/86400);
        }
    }

    public static String headingFormat(double heading) {
        if (heading < 0) {
            heading = 360 + heading;
        }

        return String.format("%.0f %s",heading,compassBox(heading));
    }

    public static String compassBox(double heading) {
        if (heading < 22.5) {
            return "N";
        } else if (heading < 67.5) {
            return "NE";
        } else if (heading < 112.5) {
            return "E";
        } else if (heading < 157.5) {
            return "SE";
        } else if (heading < 202.5) {
            return "S";
        } else if (heading < 247.5) {
            return "SW";
        } else if (heading < 292.5) {
            return "W";
        } else if (heading < 337.5) {
            return "NW";
        } else {
            return "N";
        }
    }
}