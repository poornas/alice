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
 * Created by Upheus1 on 1/22/17.
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
            Log.i("=========>>","Socket is alive");

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

