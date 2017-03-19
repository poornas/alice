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

import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;

/**
 * This is a wrapper around FaceDetector that captures meta data about frame and detected faces for the Xray server
 */
public class CustomFaceDetector extends Detector<Face> {

    private Detector<Face> mDelegate;
    private AliceTask vTask;
    /**
     * Creates a custom face detector to wrap around underlying face detector
     */
    public CustomFaceDetector(Detector<Face> delegate) {
        mDelegate = delegate;
    }

    @Override
    public void release() {
        mDelegate.release();
    }

    /**
     * Captures meta data about faces detected and affiliated frame info for the XRay server
     */
    @Override
    public SparseArray<Face> detect(Frame frame) {
        SparseArray<Face> faces = null;
        try{
            faces = mDelegate.detect(frame);
            if (faces.size() > 0) {
                vTask = new AliceTask(frame.getMetadata(), faces.clone());
                vTask.execute();
            }
        } catch (Exception e) {
            throw e;
        }

        return faces;

    }

    @Override
    public boolean isOperational() {
        return mDelegate.isOperational();
    }

    @Override
    public boolean setFocus(int id) {
        return mDelegate.setFocus(id);
    }

}