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
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity  implements PreviewCallback {

    public static ClientWebSocket webSocket = null;
    public static Context context;
    public static String TAG = "__ALICE__";
    public static XrayDetectResult serverReply;

    private static final int REQUEST_VIDEO_PERMISSIONS = 1;
    private boolean hasVideoPermission = false;
    private boolean hasLocationPermission = false;

    StoreService storeFramesService;
    boolean mServiceBound = false;
    boolean running = true;
    public static LocationTracker locationTracker;

    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
    };

    GestureDetector gestureDetector;

    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private CameraDeviceManager cameraManager;

    Thread frameHandlerThread;
    FrameHandler frameHandler;
    ServerHandler serverhandler;
    private ServerResponseHandler serverResponseHandler;
    private Thread serverResponseThread;
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
        if (webSocket == null) {
            webSocket = new ClientWebSocket();
            Log.i(MainActivity.TAG, "About to connect to WS");
            webSocket.connect(context);
        }
        //Init media writers and location,sensor trackers
        serverhandler = new ServerHandler(context);

        //Spawn thread for handler for server response to Alice
        serverResponseHandler = new ServerResponseHandler();
        serverResponseThread = new Thread(serverResponseHandler);

        //Spawn thread for frame handler
        initFrameHandler();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);


        mPreview = (CameraSourcePreview) findViewById(R.id.ZoomCameraView);
        mPreview.setZoomControl((SeekBar) findViewById(R.id.CameraZoomControls));
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

        if (cameraManager == null) {
            cameraManager = new CameraDeviceManager(context, new GraphicFaceTrackerFactory(), frameHandler);
        }

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            Log.d(MainActivity.TAG,"SWITCHING TO FRONT CAMERA");
            cameraManager.createCameraSource();
        } else {
            requestVideoPermission();
        }

        Intent intent = new Intent(MainActivity.this, StoreService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        checkForUpdates();
    }

    protected ServerHandler getServerHandler() {
        return this.serverhandler;
    }

    protected FrameHandler getFrameHandler() {
        return this.frameHandler;
    }
    protected Thread getFrameHandlerThread() {
        return this.frameHandlerThread;
    }


    @Override
    public void onPause() {
        super.onPause();

        mPreview.stop();
        serverhandler.stop();
        unregisterManagers();
    }

    //Spawns a new Framehandler thread
    private void initFrameHandler() {
        if (frameHandlerThread == null) {
            frameHandler = new FrameHandler(context);
            frameHandlerThread = new Thread(frameHandler);
        }
    }

    @Override
    public void onResume() {

        super.onResume();
        if (webSocket == null) {
            webSocket = new ClientWebSocket();
            webSocket.connect(context);
        }
        cameraManager.startCameraSource();
        serverhandler.start();
        initFrameHandler();

        checkForCrashes();
    }

    public void onDestroy() {
        super.onDestroy();
        frameHandler.stopRecording();
        frameHandlerThread = null;
        serverhandler.stop();

        unregisterManagers();
        if (mServiceBound) {
            context.unbindService(mServiceConnection);
            mServiceBound = false;
        }
    }

    // Use Hockey Framework to collect crash reports on clients.
    private void checkForCrashes() {
        CrashManager.register(this);
    }

    // Hockey App Distribution
    private void checkForUpdates() {
        // Remove this for store builds!
       // UpdateManager.register(this);
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
            //RIP-OCV --- COMMENTING THIS TEMPORARILY.NEED TO RESURRECT FUNCTIONALITY
            //mOpenCvCameraView.resetZoom();

            super.onLongPress(event);

        }
    }


    // Upon double tap, swap front  and back cameras
    public void swapCamera() {
        cameraManager.swapCameraSource();
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


    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    // Callback for CameraDeviceManager to start preview
    public void startPreview(CameraSource cameraSource) throws IOException {

        mPreview.start(cameraSource,mGraphicOverlay);
    }

    private class ServerResponseHandler implements Runnable {


        public ServerResponseHandler() {
            Log.d(MainActivity.TAG, "SERVER HANDLER STARTED");
            running = true;
        }

        @Override
        public void run() {
            while (true) {
                ServerResponseTask stask = new ServerResponseTask(serverReply, mGraphicOverlay, mPreview, mServiceBound);
                stask.execute();
            }

        }
    }
    // Get Video Permissions

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

        Snackbar.make(mPreview, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

}
