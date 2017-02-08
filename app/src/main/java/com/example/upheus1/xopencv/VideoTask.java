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

package com.example.upheus1.xopencv;

import android.app.ProgressDialog;
import android.content.Context;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.opencv.core.Mat;

import java.net.Socket;

/**
 * AsyncTask that sends the video buffer over to the Xray Server.
 */

public class VideoTask extends AsyncTask<Void, Integer, String> {


    byte[] bufmat;

    public VideoTask(byte[] buf) {
        bufmat = buf;
        Log.i("-->", "In VideoTask");
    }



    @Override
    protected void onPreExecute() {
        Log.i("PRE EXECUTE", "pre execute");
    }

    @Override
    protected String doInBackground(Void ... params) {
        Log.i("Do BGD", "do background");

        Log.i("-->",String.valueOf(bufmat.length));
        if(MainActivity.videoWebSocket != null) {
            MainActivity.videoWebSocket.sendPayload(bufmat);

        }
        else {
            Log.i("=========>>","Socket is NOT alive");
        }
        bufmat = null;
        return "done";

    }


    @Override
    protected void onCancelled() {

    }



    protected void onPostExecute(String finish) {
        Log.i("Post EXECUTE", "in here");

        if (isCancelled()) {
            Log.i("in cancelled", "post if");

        } else {
            Log.i("Non cancelled", "post else");
        }

    }
}
