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
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

import java.io.ByteArrayOutputStream;

/**
 * MatVideoWriter is responsible for managing conversion formats and dispatching a VideoTask
 * which will inturn connect to the Xray Server and stream data.
 */

public class MatVideoWriter {
        boolean recording;
        AliceTask vTask;
        Context context;

        public MatVideoWriter(Context context) {
            this.context = context;
            recording = true;

        }

        public void write(byte[] data,int width, int height) {
            vTask = new AliceTask(YUVtoJPEG(context, width, height, data));
            vTask.execute();
        }

        public boolean isRecording() {
            return recording;
        }

        // This is like a destructor.
        public void stopRecording() {
            recording = false;
            vTask = null;
        }

        // Converts Android's NV21 image format to RGBA_8888, and then
        // to the compressed JPEG format recognized by the server
         public byte[] YUVtoJPEG(Context context, int width, int height, byte[] nv21) {
            // Uses Renderscript intrinsic function to convert from NV21 image format to RGBA_8888
            RenderScript rs = RenderScript.create(context);
            ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
            Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
            Allocation in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
            Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
            Allocation out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
            in.copyFrom(nv21);
            nv21 = null;

            yuvToRgbIntrinsic.setInput(in);
            yuvToRgbIntrinsic.forEach(out);

            // Convert RGBA_8888 to ARGB_8888 compressed JPEG format
            Bitmap bitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
            out.copyTo(bitmap);
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteStream);
            bitmap.recycle();
            return byteStream.toByteArray();
        }
}
