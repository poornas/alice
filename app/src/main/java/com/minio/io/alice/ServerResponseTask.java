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
    XrayResult serverResult;
    boolean setZoom = false;
    CameraSourcePreview mPreview;
    GraphicOverlay mServerOverlay;

    public ServerResponseTask(XrayResult xresult, GraphicOverlay serverOverlay, CameraSourcePreview preview) {
        serverResult = xresult;
        mPreview =  preview;
        mServerOverlay = serverOverlay;
    }


    @Override
    protected void onPreExecute() {}

    @Override
    protected Void doInBackground(Void ... params) {
        if (serverResult != null) {
            if (serverResult.isReply() == true) {
                // TODO: If zoom, increase zoom
                if (serverResult.getZoom() != 0)
                    setZoom = true;

                if (XDebug.LOG) {
                    // TODO: This should be done only on server's command. Uncomment later.
                }
            }
        }
        return null;
    }

    protected void onPostExecute(String finish) {
        if (setZoom)
            mPreview.increaseZoom(serverResult.getZoom());

        mServerOverlay.showScreen();
        mServerOverlay.postInvalidate();
    }
}
