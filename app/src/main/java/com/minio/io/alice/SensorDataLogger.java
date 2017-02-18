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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.List;

import static com.minio.io.alice.MainActivity.context;

public class SensorDataLogger implements SensorEventListener{
    //Sensor manager instance
    private SensorManager mSensorManager;
    private long lastUpdate;
    private static final int MIN_POLLING_DURATION = 1000; // 1 second
    private AliceTask vTask;

    public SensorDataLogger() {
        mSensorManager = (SensorManager)  context.getSystemService(Context.SENSOR_SERVICE);
        // Register all available sensors to this listener
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (int i = 0; i < sensors.size(); i++) {
            Sensor sensor = sensors.get(i);
            boolean success = mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            if (XDebug.LOG)
                Log.d(MainActivity.TAG,(success ? "REGISTERED: ": "FAILED: ") + sensor.toString());
        }
        lastUpdate = System.currentTimeMillis();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastUpdate) > MIN_POLLING_DURATION) {
            lastUpdate = System.currentTimeMillis();
            SensorRecord record = new SensorRecord(event);
            //Commented out until server can accept sensor data
            //write(record.toString());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    public void write(String data){
        vTask = new AliceTask(data);
        vTask.execute();

    }
}