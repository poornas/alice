package com.example.upheus1.xopencv;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;

import static org.opencv.ml.SVM.C;


public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";
    private MatVideoWriter matVideoWriter;
    private CameraBridgeViewBase mOpenCvCameraView;

    public static VideoWebSocket videoWebSocket = null;
    public static Context context;
    VideoTask vTask;
    Mat src;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
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
        Log.i(TAG, "Instantiated new " + this.getClass());
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        context = this;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        matVideoWriter = new MatVideoWriter(context);
        setContentView(R.layout.activity_main);

        if (videoWebSocket == null) {
            videoWebSocket = new VideoWebSocket();
            videoWebSocket.connect(context);

        }

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
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

        if (videoWebSocket == null) {
            videoWebSocket = new VideoWebSocket();
            videoWebSocket.connect(context);
        }
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
        if (src != null) {
            src.release();
        }
        matVideoWriter.stopRecording();
    }



    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        if (src != null) {
            src.release();
        }
        src = inputFrame.rgba();
        if(matVideoWriter.isRecording()) {
            matVideoWriter.write(src, videoWebSocket);
        }

        return src;
    }
}