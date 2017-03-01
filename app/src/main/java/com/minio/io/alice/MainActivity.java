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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;


public class MainActivity extends Activity implements CvCameraViewListener2 {

    private MatVideoWriter matVideoWriter;
    private ZoomCameraView mOpenCvCameraView;
    private ImageButton switchCameraButton;
    public static ClientWebSocket webSocket = null;
    public static Context context;
    public static String TAG = "__ALICE__";
    public static XPly serverReply;
    Mat srcMat, blackMat;

    GestureDetector gestureDetector;

    protected static LocationTracker locationTracker = null;
    protected static SensorDataLogger sensorLogger = null;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    if (XDebug.LOG)
                        Log.i(MainActivity.TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public MainActivity() {
        gestureDetector = new GestureDetector(context, new GestureListener());
        if (XDebug.LOG)
            Log.i(MainActivity.TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {

        return gestureDetector.onTouchEvent(e);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        context = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        matVideoWriter = new MatVideoWriter(context);
        setContentView(R.layout.activity_main);

        if (webSocket == null) {
            webSocket = new ClientWebSocket();
            Log.i(MainActivity.TAG, "About to connect to WS");
            webSocket.connect(context);
        }
        if (locationTracker == null) {
            locationTracker = new LocationTracker();
        }
        if (sensorLogger == null) {
            sensorLogger = new SensorDataLogger();
        }

        mOpenCvCameraView = (ZoomCameraView) findViewById(R.id.ZoomCameraView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setZoomControl((SeekBar) findViewById(R.id.CameraZoomControls));
        mOpenCvCameraView.enableFpsMeter();
        mOpenCvCameraView.setCvCameraViewListener(this);

        // fix to front camera for now. remove this to use back camera for now.
        mOpenCvCameraView.setCameraIndex(1);
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {

        super.onResume();

        if (webSocket == null) {
            webSocket = new ClientWebSocket();
            webSocket.connect(context);
        }
        if (!OpenCVLoader.initDebug()) {
            if (XDebug.LOG)
                Log.d(MainActivity.TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            if (XDebug.LOG)
                Log.d(MainActivity.TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

    }

    public void onDestroy() {
        super.onDestroy();
        if (matVideoWriter != null) {
            matVideoWriter.stopRecording();
        }
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        srcMat = new Mat();
        blackMat = new Mat();
    }

    public void onCameraViewStopped() {
        if (srcMat != null) {
            srcMat.release();
        }
        if (blackMat != null) {
            blackMat.release();
        }
        matVideoWriter.stopRecording();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        if (srcMat != null) {
            srcMat.release();
        }

        srcMat = inputFrame.rgba();
        if (matVideoWriter.isRecording()) {
            matVideoWriter.write(srcMat, webSocket);
        }
        if (serverReply != null) {

            if (serverReply.isReply() == true) {
                if (XDebug.LOG) {
                    // Log.i(MainActivity.TAG, " Alice found someone");
                }
                Imgproc.rectangle(srcMat, serverReply.getP1(), serverReply.getP2(), serverReply.getScalar(), serverReply.getThickness());
                if (serverReply.getZoom() != 0)
                    mOpenCvCameraView.increaseZoom(serverReply.getZoom());
                return srcMat;
            }

            if (serverReply.getDisplay()) {
                // Wake up if the display is set to true
                if (XDebug.LOG) {
                    //Log.i(MainActivity.TAG, " Alice Wakes up");
                    // Log.i(MainActivity.TAG, String.valueOf(serverReply.isReply()));
                }
                return srcMat;
            } else {
                if (XDebug.LOG) {
                    //  Log.i(MainActivity.TAG, "Alice Sleeps");
                }
                // return a black mat when server replies with false for Display.
                blackMat = srcMat.clone();
                blackMat.setTo(new Scalar(0, 0, 0));
                return blackMat;
            }
        } else {
            // return black frame unless woken up explicitly by server.
            blackMat = srcMat.clone();
            blackMat.setTo(new Scalar(0, 0, 0));
            return blackMat;
        }
    }


    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        // event when double tap occurs
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();
            if (XDebug.LOG)
                Log.d(MainActivity.TAG, "Tapped at: (" + x + "," + y + ")");
            mOpenCvCameraView.swapCamera();
            return true;
        }
    }

}
