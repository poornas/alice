package com.example.upheus1.xopencv;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;


public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";
    private MatVideoWriter matVideoWriter = new MatVideoWriter();
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;
    public static VideoWebSocket videoWebSocket = null;

    VideoTask vTask;
    Mat mat;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i("====================>>>>>>", "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);


        if(videoWebSocket == null) {
            videoWebSocket = new VideoWebSocket();
            videoWebSocket.connect();

        }

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setMaxFrameSize(640, 480);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        Log.i("====================>>>>>>", "called onPause");
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        Log.i("====================>>>>>>", "called onResume");
        super.onResume();

        if(videoWebSocket == null) {
            videoWebSocket = new VideoWebSocket();
            videoWebSocket.connect();
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
        //videoWebSocket.disconnect();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
        if(mat != null) {
            mat.release();
        }
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        if (mat != null) {
            mat.release();
        }
        mat = inputFrame.rgba();
        byte buffer[];
           //  For later .... if(matVideoWriter.isRecording()) {
           // ignore.. matVideoWriter.write(matcopy, videoWebSocket);
        int length = (int) (mat.total() * mat.elemSize());
        buffer = new byte[length];
        mat.get(0, 0, buffer);
        try
        {
            vTask = new VideoTask(buffer);
            vTask.execute();
            // test if the frames are written to sd card. FileUtils.writeByteArrayToFile(file, buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }

            Log.v("DONE ----", "Finished now");
     // }

        return mat;
    }
}