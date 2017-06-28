package com.stackunderflow.stackptr;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class StackPtrCompassViewGroup extends ViewGroup {

    int width, height, centre_x, centre_y;

    SparseArray<ImageView> views;

    public StackPtrCompassViewGroup(Context context) {
        super(context);
        views = new SparseArray<ImageView>();
        init(null, 0);
    }

    public void updateDataAndRepaint(JSONArray jUsers, Location lastloc) {
        try {

            Context context = this.getContext();

            ImageView bgTile = new ImageView(context);
            Picasso.with(context).load("https://tile1.stackcdn.com/osm_tiles_2x/16/59159/40213.png").into(bgTile);
            addView(bgTile);
            bgTile.layout(0,0,512,512);


            double half_width = width / 2.0;
            ArrayList<Integer> presentIds = new ArrayList<Integer>();

            for (int i = 0; i < jUsers.length(); i++) {
                JSONObject thisUser = jUsers.getJSONObject(i);
                Integer user = thisUser.getInt("id");
                presentIds.add(user);

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
                Location userLocation = new Location("StackPtr");
                userLocation.setLatitude(lat);
                userLocation.setLongitude(lon);

                float dist = lastloc.distanceTo(userLocation);
                float bearing = lastloc.bearingTo(userLocation);
                if (bearing < 0) {
                    bearing += 360;
                }

                double xvect = Math.sin( Math.toRadians(bearing) );
                double yvect = -Math.cos( Math.toRadians(bearing) );

                //dist = 100;
                double r = Math.log10(dist) - 1.0;
                r = Math.max(r, 0.0);
                r *= half_width / 3.0;

                double xcoord = xvect * r;
                double ycoord = yvect * r;

                xcoord = Math.max(-half_width + 16, xcoord);
                xcoord = Math.min(half_width - 16, xcoord);

                ycoord = Math.max(-half_width + 16, ycoord);
                ycoord = Math.min(half_width - 16, ycoord);

                iconMove(userView, (int) xcoord, (int) ycoord, 64);
            }

            // remove all the image views for users we didn't see again
            ArrayList<Integer> viewsToRemove = new ArrayList<Integer>();
            for (int i=0; i<views.size(); i++) {
                Integer viewId = views.keyAt(i);
                if (!presentIds.contains(viewId)) {
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
        int centre_x = (getWidth() - getPaddingLeft() - getPaddingRight())/2;
        int centre_y = (getHeight() - getPaddingTop() - getPaddingBottom())/2;
        int hsize = size / 2;
        icon.layout(centre_x + x - hsize, centre_y + y - hsize, centre_x + x + hsize, centre_y + y + hsize);
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

    Paint greenLine;

    private void init(AttributeSet attrs, int defStyle) {
        setWillNotDraw(false);
        greenLine = new Paint();
        greenLine.setColor(Color.GREEN);
        greenLine.setStyle(Paint.Style.STROKE);
        greenLine.setStrokeWidth(2.0f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int i=0; i<5; i++) {
            canvas.drawCircle(centre_x, centre_y, (float) ((i * width / 6.0) - 1.0), greenLine);
        }

        canvas.drawLine(0.0f, centre_y, width, centre_y, greenLine);
        canvas.drawLine(centre_x, 0, centre_x, height, greenLine);
        canvas.drawLine(0, 0, width, height, greenLine);
        canvas.drawLine(width, 0, 0, height, greenLine);
    }

}
