package com.stackunderflow.stackptroverlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.stackunderflow.stackptrmap.StackPtrMapTileCalc;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class StackPtrCompassViewGroup extends ViewGroup {

    int width, height, centre_x, centre_y;

    SparseArray<ImageView> views;
    SparseArray<JSONObject> users;
    ImageView bgTile;
    int fuser = 3;
    int zoom = 16;

    public StackPtrCompassViewGroup(Context context) {
        super(context);
        views = new SparseArray<>();
        users = new SparseArray<>();
        bgTile = new ImageView(context);
        addView(bgTile);
        bgTile.layout(0,0,512,512);

    }

    public void updateDataAndRepaint(JSONArray jUsers, Location lastloc) {
        try {

            Context context = this.getContext();

            for (int i = 0; i < jUsers.length(); i++) {
                JSONObject thisUser = jUsers.getJSONObject(i);
                Integer user = thisUser.getInt("id");
                users.append(user, thisUser);
            }

            JSONObject tracked_user = users.get(fuser);
            JSONArray tu_loc = tracked_user.getJSONArray("loc");
            final double tracked_user_lat = tu_loc.getDouble(0);
            final double tracked_user_lon = tu_loc.getDouble(1);
            double tracked_user_xtile = StackPtrMapTileCalc.xtileForLon(tracked_user_lon,zoom);
            double tracked_user_ytile = StackPtrMapTileCalc.ytileForLat(tracked_user_lat,zoom);
            Picasso.with(context).load(
                    StackPtrMapTileCalc.mapUrl(tracked_user_xtile,tracked_user_ytile,zoom)
            ).into(bgTile);


            for(int i = 0; i < users.size(); i++) {
                int user = users.keyAt(i);
                JSONObject thisUser = users.valueAt(i);

                ImageView userView = views.get(user);
                if (userView == null) {
                    userView = new ImageView(context);
                    views.append(user, userView);
                    addView(userView);
                }

                String iconURL = thisUser.getString("icon");
                Picasso.with(context).load(iconURL).into(userView);

                JSONArray jLoc = thisUser.getJSONArray("loc");
                final double lat = jLoc.getDouble(0);
                final double lon = jLoc.getDouble(1);

                double xtile = StackPtrMapTileCalc.xtileForLon(lon,zoom);
                double ytile = StackPtrMapTileCalc.ytileForLat(lat,zoom);

                double xcoord = StackPtrMapTileCalc.pxCoord(xtile,512);
                double ycoord = StackPtrMapTileCalc.pxCoord(ytile,512);

                if ((Math.floor(xtile) == Math.floor(tracked_user_xtile)) &&
                (Math.floor(ytile) == Math.floor(tracked_user_ytile))) {
                    userView.setVisibility(VISIBLE);
                } else {
                    userView.setVisibility(INVISIBLE);
                }

                iconMove(userView, (int) xcoord, (int) ycoord, 96);

                /*Location userLocation = new Location("StackPtr");
                userLocation.setLatitude(lat);
                userLocation.setLongitude(lon);*/

                /*float dist = lastloc.distanceTo(userLocation);
                float bearing = lastloc.bearingTo(userLocation);
                if (bearing < 0) {
                    bearing += 360;
                }*/

            }

            // remove all the image views for users we didn't see again
            ArrayList<Integer> viewsToRemove = new ArrayList<Integer>();
            for (int i=0; i<views.size(); i++) {
                Integer viewId = views.keyAt(i);
                if (users.get(viewId) == null) {
                    viewsToRemove.add(viewId);
                }
            }
            for (Integer rmView : viewsToRemove) {
                ImageView view = views.get(rmView);
                removeView(view);
                views.remove(rmView);
            }



        } catch (JSONException e) {
            System.out.print(e);
        }
    }

    private void iconMove(ImageView icon, int x, int y, int size) {
        int hsize = size / 2;
        icon.layout(x - hsize, y - hsize, x + hsize, y + hsize);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.setLayoutParams(new ViewGroup.LayoutParams(w, w));
        width = w; centre_x = w/2;
        height = h; centre_y = h/2;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        //int contentWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        //int contentHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        //System.out.printf("left %d, top %d, right %d, bottom %d, width %d, height %d\n", left, top, right, bottom, contentWidth, contentHeight);
    }


}
