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

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.HashMap;

import static org.opencv.core.Core.flip;

public class MainActivity extends Activity implements CvCameraViewListener2 {

    private MatVideoWriter matVideoWriter;
    private ZoomCameraView mOpenCvCameraView;
    private ImageButton switchCameraButton;
    public static ClientWebSocket webSocket = null;
    public static Context context;
    public static String TAG = "__ALICE__";
    public static XRayDetectResult serverReply;
    Mat srcMat, blackMat;

    private static final int REQUEST_VIDEO_PERMISSIONS = 1;
    private boolean hasVideoPermission = false;
    private boolean hasLocationPermission = false;

    StoreService storeFramesService;
    boolean mServiceBound = false;

    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
    };

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
        Intent intent = new Intent(MainActivity.this, StoreService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
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
        requestVideoPermission();
        if (hasLocationPermission && locationTracker == null)
            locationTracker = new LocationTracker();

        if (hasVideoPermission) {
            if (!OpenCVLoader.initDebug()) {
                if (XDebug.LOG)
                    Log.d(MainActivity.TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
            } else {
                if (XDebug.LOG)
                    Log.d(MainActivity.TAG, "OpenCV library found inside package. Using it!");
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
            audioWriter.startRecording();
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
        if (mServiceBound) {
            context.unbindService(mServiceConnection);
            mServiceBound = false;
        }
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
        if (mServiceBound) {
            context.unbindService(mServiceConnection);
            mServiceBound = false;
        }
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

                Imgproc.rectangle(srcMat, serverReply.getP1(), serverReply.getP2(), serverReply.getScalar(), serverReply.getThickness());

                if (serverReply.getZoom() != 0)
                    mOpenCvCameraView.increaseZoom(serverReply.getZoom());

                if (XDebug.LOG) {
                    // TODO: This should be done only on server's command. Uncomment later.
                    if(mServiceBound) {
                      //  storeFramesService.save(matVideoWriter.captureBitmap(srcMat));
                    }
                }
                return srcMat;
            }

            if (serverReply.getDisplay()) {
                // Wake up if the display is set to true
                return srcMat;
            } else {
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
    // Use Hockey Framework to collect crash reports on clients.
    private void checkForCrashes() {
        CrashManager.register(this);
    }

    // Hockey App Distribution
    private void checkForUpdates() {
        // Remove this for store builds!
        UpdateManager.register(this);
    }

    private void unregisterManagers() {
        UpdateManager.unregister();
    }

    // Private class to handle touch events on camera.
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

    /**
     * Gets whether you should show UI with rationale for requesting permissions.
     *
     * @param permissions The permissions your app wants to request.
     * @return Whether you can show permission rationale UI.
     */
    private boolean shouldShowRequestPermissionRationale(String[] permissions) {
        boolean show =  false;
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                show =  true;
            }
        }
        return show;
    }

    private boolean checkSelfPermission(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED)
                return true;
        }
        return false;
    }

    private void requestVideoPermission() {

        if (checkSelfPermission(VIDEO_PERMISSIONS)) {
            if (shouldShowRequestPermissionRationale(VIDEO_PERMISSIONS)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                showDialog(VIDEO_PERMISSIONS);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        VIDEO_PERMISSIONS,
                        REQUEST_VIDEO_PERMISSIONS);

                // REQUEST_VIDEO_PERMISSIONS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

        } else {
            this.hasVideoPermission = true;
        }

    }
    // Find permissions that were not granted and return as an ArrayList
    private ArrayList<String> getPendingPermissions(int[] grantResults, String permissions[]) {
        HashMap<String,Integer> perms = new HashMap();
        ArrayList<String> pendingPermissions = new ArrayList<String>();
        for (int i = 0; i < permissions.length; i++) {
            perms.put(permissions[i],grantResults[i]);
            if (permissions[i] == Manifest.permission.ACCESS_FINE_LOCATION && grantResults[i] == PackageManager.PERMISSION_GRANTED)
                this.hasLocationPermission = true;
            if (permissions[i] == Manifest.permission.CAMERA  && grantResults[i] == PackageManager.PERMISSION_GRANTED)
                this.hasVideoPermission = true;
        }

        for (int i = 0; i < VIDEO_PERMISSIONS.length; i++) {
            if (grantResults.length == VIDEO_PERMISSIONS.length) {
                if (perms.get(VIDEO_PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
                    pendingPermissions.add(VIDEO_PERMISSIONS[i]);
                }
            } else {
                pendingPermissions.add(VIDEO_PERMISSIONS[i]);
            }
        }
        return pendingPermissions;
    }

    // Callback with the request from calling requestPermissions(...)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        // Make sure it's our original REQUEST_VIDEO_PERMISSIONS request

        if (requestCode == REQUEST_VIDEO_PERMISSIONS) {
           ArrayList<String> permissionsNeededYet = getPendingPermissions(grantResults,permissions);
            if (permissionsNeededYet.size() == 0){
                //all permissions granted - allow camera access
                return;

            } else {
                // showRationale = false if user clicks Never Ask Again, otherwise true
                boolean showRationale = shouldShowRequestPermissionRationale(permissionsNeededYet.toArray(new String[0]));

                if (!showRationale) {
                    Toast.makeText(this, "Video permission denied.Enable camera and location preferences on the App settings", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + BuildConfig.APPLICATION_ID)));
                    finish();
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    // show dialog to request for permissions
    void showDialog(final String permissions[]) {
        final Activity thisActivity = this;
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions, REQUEST_VIDEO_PERMISSIONS);
            }
        };

        Snackbar.make(mOpenCvCameraView, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    // Setup to be able to call frame saving to object storage.
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            StoreService.AliceServiceBinder myBinder = (StoreService.AliceServiceBinder) service;
            storeFramesService = myBinder.getService();
            mServiceBound = true;
        }
    };
}
