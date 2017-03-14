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

import android.os.AsyncTask;


/**
 * AsyncTask that sends the video buffer over to the Xray Server.
 */

public class ServerResponseTask extends AsyncTask<Void, Void, Void> {

    XPly serverReply;
    boolean isAliceAwake = false;
    boolean setZoom = false;
    byte[] bufmat;
    String data;
    boolean textPayload = false;
    private FaceGraphic mFaceGraphic, mPrevFaceGraphic;
    CameraSourcePreview mPreview;
    GraphicOverlay mServerOverlay;
    boolean mServiceBound;
    public ServerResponseTask(XPly xply,GraphicOverlay serverOverlay, CameraSourcePreview preview,boolean servicebound) {
        serverReply = xply;
        mPreview =  preview;
        mServerOverlay = serverOverlay;
        mServiceBound = servicebound;
        mFaceGraphic = null;
        //mPrevFaceGraphic = null;
    }


    @Override
    protected void onPreExecute() {

    }

    @Override
    protected Void doInBackground(Void ... params) {
        if (serverReply != null) {
            if (serverReply.isReply() == true) {
                //TODO: Overlay rectangles on view
                mPrevFaceGraphic = mFaceGraphic;
                mFaceGraphic = new FaceGraphic(mServerOverlay, serverReply);
                //TODO: If zoom, increase zoom
                if (serverReply.getZoom() != 0)
                    setZoom = true;


                if (XDebug.LOG) {
                    // TODO: This should be done only on server's command. Uncomment later.
                    if (mServiceBound) {
                        //  storeFramesService.save(matVideoWriter.captureBitmap(srcMat));
                    }
                }
            }

            if (serverReply.getDisplay()) {
                // Wake up if the display is set to true
                //TODO Turn on display preview
                isAliceAwake = true;
            } else {
                // return a black mat when server replies with false for Display.
                //TODO: turn screen black
                isAliceAwake = false;
            }

        }
        return null;
    }

    protected void onPostExecute(String finish) {
        if (setZoom)
            mPreview.increaseZoom(serverReply.getZoom());
        if (isAliceAwake)
            mServerOverlay.showScreen();
        else
            mServerOverlay.hideScreen();

        if (mFaceGraphic != null)
            mServerOverlay.add(mFaceGraphic);
        mServerOverlay.postInvalidate();
    }
}