package com.stackunderflow.stackptr;

/**
 * Created by gm on 28/6/17.
 */

public class StackPtrTileCalc {
    public static double xtileForLon(double lon, int zoom) {
        return (lon + 180) / 360 * (1<<zoom);
    }

    public static double ytileForLat(double lat, int zoom) {
        return (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<zoom) ;
    }

    public static 

}
