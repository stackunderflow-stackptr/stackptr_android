package com.stackunderflow.stackptrmap;

/**
 * Created by gm on 28/6/17.
 */

public class StackPtrMapTileCalc {
    public static double xtileForLon(double lon, int zoom) {
        return (lon + 180) / 360 * (1<<zoom);
    }

    public static double ytileForLat(double lat, int zoom) {
        return (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<zoom) ;
    }

    public static String mapUrl(double xtile, double ytile, int zoom) {
        int xtile_f = (int) Math.floor(xtile);
        int ytile_f = (int) Math.floor(ytile);

        return String.format("https://tile1.stackcdn.com/osm_tiles_2x/%d/%d/%d.png", zoom, xtile_f, ytile_f);
    }

}
