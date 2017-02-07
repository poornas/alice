package com.example.upheus1.xopencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import static android.R.attr.path;
import static android.R.attr.type;

/**
 * Created by deekoder on 2/2/17.
 * MatVideoWriter is responsible for managing conversion formats and dispatching a VideoTask
 * which will inturn connect to the XRay Server and stream data.
 */


public class MatVideoWriter {

        boolean recording;
        VideoTask vTask;
        Context context;
        Mat mat;

        public  MatVideoWriter(Context context) {
            this.context = context;
            recording = true;
        }

        public void write(Mat mat, VideoWebSocket videoWebSocket){
            vTask = new VideoTask(captureBitmap(mat));
            vTask.execute();
        }

        // Formats Mat Object to BitMap Byte Array.
        private byte[] captureBitmap(Mat mat) {
            Bitmap bitmap;
            try {
                bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(mat, bitmap);

                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteStream);

                // Convert ByteArrayOutputStream to byte array. Close stream.
                byte[] byteArray = byteStream.toByteArray();
                byteStream.close();
                byteStream = null;
                //mat.release();
                return byteArray;

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
            return null;
        }

        // Formats Mat Object to Raw Byte Array.
        private byte[] captureRawBytes(Mat mat) {
            int length = (int) (mat.total() * mat.elemSize());
            byte buffer[] = new byte[length];
            mat.get(0, 0, buffer);

            return buffer;
        }

        public boolean isRecording() {
            return recording;
        }

        // This is like a destructor.
        public void stopRecording() {
            recording = false;
            vTask = null;
        }


}
