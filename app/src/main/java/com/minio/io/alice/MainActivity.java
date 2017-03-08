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
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import static org.opencv.core.Core.flip;

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
    protected static AudioWriter audioWriter = null;

    //turn this switch on when server can actually handle audio data
    private boolean audioFlag  = false;

    // Front camera orientation is default
    private int mCameraId = 1;
    private Display display;

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
        display = ((WindowManager) this.getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

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

        audioWriter = new AudioWriter(context,audioFlag);
        mOpenCvCameraView = (ZoomCameraView) findViewById(R.id.ZoomCameraView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setZoomControl((SeekBar) findViewById(R.id.CameraZoomControls));
        mOpenCvCameraView.enableFpsMeter();
        mOpenCvCameraView.setCvCameraViewListener(this);

        // Set front camera as default
        mOpenCvCameraView.setCameraIndex(mCameraId);
        checkForUpdates();
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        if (audioWriter != null)
            audioWriter.stopRecording();
        unregisterManagers();
    }

    @Override
    public void onResume() {

        super.onResume();

        if (webSocket == null) {
            webSocket = new ClientWebSocket();
            webSocket.connect(context);
        }
        audioWriter.startRecording();
        if (!OpenCVLoader.initDebug()) {
            if (XDebug.LOG)
                Log.d(MainActivity.TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            if (XDebug.LOG)
                Log.d(MainActivity.TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        checkForCrashes();

    }

    public void onDestroy() {
        super.onDestroy();
        if (matVideoWriter != null) {
            matVideoWriter.stopRecording();
        }
        if (audioWriter != null)
            audioWriter.stopRecording();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

        unregisterManagers();
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
        audioWriter.stopRecording();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        if (srcMat != null) {
            srcMat.release();
        }

        // Flip image frame only for front camera to fix orientation
        if ((mCameraId == 1) && (display.getRotation() == Surface.ROTATION_90)) {
            int flipFlags = +1;
            flip(inputFrame.rgba(), srcMat, flipFlags);
        } else {
            srcMat = inputFrame.rgba();
        }

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

    private void checkForCrashes() {
        CrashManager.register(this);
    }

    private void checkForUpdates() {
        // Remove this for store builds!
        UpdateManager.register(this);
    }

    private void unregisterManagers() {
        UpdateManager.unregister();
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        // event when double tap occurs
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();
            if (XDebug.LOG)
                Log.d(MainActivity.TAG, "Tapped at: (" + x + "," + y + ")");
            swapCamera();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent event) {
            // triggers after onDown only for long press
            if(XDebug.LOG)
                Log.i(MainActivity.TAG, "Long Press");
            mOpenCvCameraView.resetZoom();
            super.onLongPress(event);

        }
    }

    // Upon double tap, swap front  and back cameras
    public void swapCamera() {
        mCameraId = mCameraId^1;
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setCameraIndex(mCameraId);
        mOpenCvCameraView.enableView();
    }
}
