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

/*
    ServerHandler handles all media communication with the server
    and manages the locationtracker, audio writer,  video writer and
    sensor logging operations
 */

public class ServerHandler  {
    private MatVideoWriter matVideoWriter;

    protected static LocationTracker locationTracker = null;
    protected static SensorDataLogger sensorLogger = null;
    protected static AudioWriter audioWriter = null;

    //turn this switch on when server can actually handle audio data
    private boolean audioFlag  = false;
    Context context;

    public ServerHandler(Context context) {
        this.context = context;
    }

    public void startLocationTracking() {
        if (locationTracker == null)
            locationTracker = new LocationTracker();
    }

    private void initAudioWriter() {
        if (this.audioWriter == null && audioFlag)
            this.audioWriter = new AudioWriter(context);
    }
    public void startRecordingAudio() {
        initAudioWriter();
        audioWriter.startRecording();
    }

    public void stopRecordingAudio() {
        if (audioWriter != null)
            audioWriter.stopRecording();
    }

    public void startSensorLogging() {
        if (sensorLogger == null) {
            sensorLogger = new SensorDataLogger();
        }
    }

    public void sendVideoFrame(byte[] data,int width,int height) {
        if (matVideoWriter.isRecording()) {
            matVideoWriter.write(data,width,height);
        }
    }

    private void initVideoWriter() {
        if (matVideoWriter == null)
            this.matVideoWriter = new MatVideoWriter(context);
    }

    public LocationTracker getLocationTracker() {
        return this.locationTracker;
    }

    public void start() {
        initVideoWriter();
        initAudioWriter();
        startLocationTracking();
        startSensorLogging();
    }

    public void stop() {
        matVideoWriter.stopRecording();
        stopRecordingAudio();
        locationTracker.stopLocationUpdates();
        sensorLogger = null;
    }
    public void release() {
       locationTracker = null;
        audioWriter = null;
        sensorLogger = null;
        matVideoWriter = null;
    }
}
