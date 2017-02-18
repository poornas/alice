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

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.Date;

import static android.content.Context.LOCATION_SERVICE;
import static com.minio.io.alice.MainActivity.context;

public class LocationTracker implements LocationListener {

    private final Context mContext;
    // flag for GPS status
    boolean isGPSEnabled = false;

    // flag for network status
    boolean isNetworkEnabled = false;

    // flag for GPS status
    boolean canGetLocation = false;

    private Location mGPSLocation,mNetworkLocation; // location
    private Date mLastUpdateTime; //

    private AliceTask vTask;

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000; // 1 second


    // Declaring a Location Manager
    protected LocationManager locationManager;

    public LocationTracker()
    {
        this.mContext = context;
        startLocationUpdates();
    }

    protected void startLocationUpdates()
    {
        try {
            locationManager = (LocationManager) mContext
                    .getSystemService(LOCATION_SERVICE);

            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            // getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);


            // if GPS Enabled get lat/long using GPS Services
            if (isGPSEnabled) {
                this.canGetLocation = true;
                if (mGPSLocation == null) {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    if (XDebug.LOG)
                        Log.d(MainActivity.TAG, "GPS Enabled");

                }
            }
            // First get location from Network Provider
            if (isNetworkEnabled) {
                this.canGetLocation = true;
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                if (XDebug.LOG)
                    Log.d(MainActivity.TAG, "Network");

            }
            if (!this.canGetLocation) {
                // no network provider is enabled
                if (XDebug.LOG)
                    Log.d(MainActivity.TAG, "GPS and Network disabled");
            }
        } catch(SecurityException e)
        {
            Log.d(MainActivity.TAG,"Location service error");
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        mGPSLocation = location;
        if (XDebug.LOG)
            Log.d(MainActivity.TAG,location.toString());
        // Uncomment when server is ready to accept location data
        //write(location.toString());
    }

    protected void stopLocationUpdates() {
        if (this.locationManager != null)
        {
            try{
                this.locationManager.removeUpdates(LocationTracker.this);
            } catch(SecurityException e){
                Log.d(MainActivity.TAG,"GPS error");
            }
        }
    }


    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void write(String data){
        vTask = new AliceTask(data);
        vTask.execute();

    }

}
