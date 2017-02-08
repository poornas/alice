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
import android.content.Intent;
import android.util.Log;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketConnectionHandler;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketOptions;

/**
 * VideoWebSocket is responsible for connecting to the Xray server.
 */

public class VideoWebSocket {

    private WebSocketConnection mConnection = new WebSocketConnection();
    Context context;

    public void connect(Context context) {
        this.context = context;

        final String wsuri = "ws://192.168.1.225:8080";

        final WebSocketOptions webSocketOptions = new WebSocketOptions();
        webSocketOptions.setMaxMessagePayloadSize(100 * 1024 * 1024);

        try {
            mConnection.connect(wsuri, new WebSocketConnectionHandler() {
                @Override
                public void onOpen() {
                    Log.d(String.valueOf(R.string.TAG), "Status: Connected to " + wsuri);

                }

                @Override
                public void onTextMessage(String payload) {
                    Log.i(String.valueOf(R.string.TAG), "Recieved: " + payload);
                    //broadcastIntent(payload);

                }

                @Override
                public void onRawTextMessage(byte[] payload) {
                    try {
                        String rawText = new String(payload, "UTF-8");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onBinaryMessage(byte[] payload) {

                }

                @Override
                public void onClose(int code, String reason) {
                    Log.d(String.valueOf(R.string.TAG), "Connection lost.");
                }
            }, webSocketOptions);
        } catch (WebSocketException e) {

            Log.d(String.valueOf(R.string.TAG), e.toString());
        }
    }

    public void sendPayload(byte[] b) {
        if (mConnection.isConnected()) {
            mConnection.sendBinaryMessage(b);
            b = null;
        }
    }

    public void disconnect() {
        if (mConnection.isConnected())
            mConnection.disconnect();
    }

    public void broadcastIntent(String payload){
        Intent intent = new Intent();
        intent.putExtra(String.valueOf(R.string.xray_broadcast), payload);
        intent.setAction("com.example.upheus1.xopencv.xray_broadcast");
        context.sendBroadcast(intent);
    }
}
