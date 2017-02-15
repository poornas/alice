/*
 * Copyright (c) 2017 Minio, Inc. <https://www.minio.io>
 *
 * This file is part of Alice.
 *
 * Alice is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.minio.io.alice;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

// This class is populated when server sends data. It needs a lot of improvement.
public class XPly {


    private Point p1;
    private Point p2;

    private Scalar scalar;
    private int thickness;
    private int linetype;
    private int shift;
    private int zoom;
    private boolean display;

    JSONObject replyObject = null;
    JSONArray positions;

    public XPly(String replyMessage) {
        try {
            replyObject = new JSONObject(replyMessage) ;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            if(!replyObject.isNull("Positions"))

                positions = replyObject.getJSONArray("Positions");

            for (int i=0; i< positions.length(); i++) {
                int x1 = positions.getJSONObject(i).getJSONObject("PT1").getInt("X");
                int y1 = positions.getJSONObject(i).getJSONObject("PT1").getInt("Y");

                setP1(x1,y1);

                int x2 = positions.getJSONObject(i).getJSONObject("PT2").getInt("X");
                int y2 = positions.getJSONObject(i).getJSONObject("PT2").getInt("Y");

                setP2(x2,y2);

                double scalar = positions.getJSONObject(i).getInt("Scalar");
                setScalar(scalar);

                int thickness = positions.getJSONObject(i).getInt("Thickness");
                setThickness(thickness);

                int linetype = positions.getJSONObject(i).getInt("LineType");
                setLinetype(linetype);

                int shift = positions.getJSONObject(i).getInt("Shift");
                setShift(shift);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            setDisplay(replyObject.getBoolean("Display"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            setZoom(replyObject.getInt("Zoom"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Point getP1() {
        return p1;
    }

    public void setP1(int x1, int y1) {
        this.p1 = new Point(x1,y1);
    }

    public Point getP2() {
        return p2;
    }

    public void setP2(int x2, int y2) {
        this.p2 = new Point(x2,y2);
    }

    public Scalar getScalar() {
        return scalar;
    }

    public void setScalar(double scalar) {
        this.scalar = new Scalar(scalar);
    }

    public int getThickness() {
        return thickness;
    }

    public void setThickness(int thickness) {
        this.thickness = thickness;
    }

    public int getLinetype() {
        return linetype;
    }

    public void setLinetype(int linetype) {
        this.linetype = linetype;
    }

    public int getShift() {
        return shift;
    }

    public void setShift(int shift) {
        this.shift = shift;
    }

    public int getZoom() {
        return zoom;
    }

    public void setZoom(int zoom) {
        Log.i(MainActivity.TAG, "Setting Zoom");
        this.zoom = zoom;
    }

    public boolean isDisplay() {
        return display;
    }

    public void setDisplay(boolean display) {
        Log.i(MainActivity.TAG, "Setting display");
        this.display = display;
    }
}
