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
import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;

/**
 * MatVideoWriter is responsible for managing conversion formats and dispatching a VideoTask
 * which will inturn connect to the Xray Server and stream data.
 */

public class MatVideoWriter {

        boolean recording;
        AliceTask vTask;
        Context context;
        Mat mat;
        byte[] matByteArray;

        public  MatVideoWriter(Context context) {
            this.context = context;
            recording = true;

        }

        public void write(Mat mat, ClientWebSocket webSocket){
            vTask = new AliceTask(captureBitmap(mat));
            vTask.execute();
        }

        // Formats Mat Object to BitMap Byte Array.
        public byte[] captureBitmap(Mat mat) {
            Bitmap bitmap;
            try {
                bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(mat, bitmap);

                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteStream);

                // Convert ByteArrayOutputStream to byte array. Close stream.
                matByteArray = byteStream.toByteArray();

                byteStream.close();
                return matByteArray;

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            return null;
        }

        // Formats Mat Object to Raw Byte Array.
        private byte[] captureRawBytes(Mat mat) {
            int length = (int) (mat.total() * mat.elemSize());
            matByteArray = new byte[length];
            mat.get(0, 0, matByteArray);
            return matByteArray;
        }

        public boolean isRecording() {
            return recording;
        }

        // This is like a destructor.
        public void stopRecording() {
            recording = false;
            vTask = null;
            matByteArray = null;
        }

}
