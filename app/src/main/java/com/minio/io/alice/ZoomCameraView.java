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
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import org.opencv.android.JavaCameraView;

// All things Camera goes here.

public class ZoomCameraView extends JavaCameraView {

    GestureDetector gestureDetector;

    private int mCameraId = 0;
    public ZoomCameraView(Context context, int cameraId) {
        super(context, cameraId);

    }

    public ZoomCameraView(Context context, AttributeSet attrs) {

        super(context, attrs);


    }

    protected SeekBar seekBar;

    public void setZoomControl(SeekBar _seekBar) {
        seekBar = _seekBar;
    }

    protected void enableZoomControls(Camera.Parameters params) {

        final int maxZoom = params.getMaxZoom();
        seekBar.setMax(maxZoom);
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int progressvalue = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // TODO Auto-generated method stub
                progressvalue = progress;
                Camera.Parameters params = mCamera.getParameters();
                params.setZoom(progress);
                mCamera.setParameters(params);


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

        });

    }


    protected boolean initializeCamera(int width, int height) {

        boolean ret = super.initializeCamera(width, height);


        Camera.Parameters params = mCamera.getParameters();

        if (params.isZoomSupported())
            enableZoomControls(params);

        mCamera.setParameters(params);

        return ret;
    }
    public void increaseZoom(int zoomdiff) {
        seekBar.incrementProgressBy(zoomdiff);
    }

    public void swapCamera() {
        mCameraId = mCameraId^1;
        disableView();
        setCameraIndex(mCameraId);
        enableView();
    }


}
