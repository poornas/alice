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

import java.io.UnsupportedEncodingException;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketConnectionHandler;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketOptions;

/**
 * WebSocket is responsible for connecting to the Xray server.
 */

public class ClientWebSocket {

    private WebSocketConnection mConnection = new WebSocketConnection();
    Context context;

    public void connect(Context context) {
        this.context = context;

        /*  147.75.201.195 is the hosted xray server.
            Replace with the IP address of local xray server
            if needed.
        */
        // final String wsuri = "ws://147.75.201.195:80";
        final String wsuri = "ws://192.168.1.106:8080";

        final WebSocketOptions webSocketOptions = new WebSocketOptions();
        webSocketOptions.setMaxMessagePayloadSize(100 * 1024 * 1024);

        try {
            mConnection.connect(wsuri, new WebSocketConnectionHandler() {
                @Override
                public void onOpen() {
                    if(XDebug.LOG)
                        Log.d(MainActivity.TAG, "Status: Connected to " + wsuri);

                }

                @Override
                public void onTextMessage (String payload) {
                    broadcastIntent(payload);
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
                public void onBinaryMessage(byte[] payload) { broadcastIntent(payload); }

                @Override
                public void onClose(int code, String reason) {
                     if(XDebug.LOG)
                        Log.d(MainActivity.TAG, "Connection lost.");
                }
            }, webSocketOptions);

        } catch (WebSocketException e) {

            Log.d(MainActivity.TAG, e.toString());
        }
    }

    public void sendPayload(byte[] b) {
        if (mConnection.isConnected()) {
            mConnection.sendBinaryMessage(b);
            b = null;
        }
    }

    public void sendPayload(String payload) {
        if (mConnection.isConnected()) {
            mConnection.sendTextMessage(payload);
            payload = null;
        }
    }
    public void disconnect() {
        if (mConnection.isConnected())
            mConnection.disconnect();
    }

    public void broadcastIntent(String payload){
        if (payload == null) {
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(String.valueOf(R.string.xray_broadcast), payload);
        intent.setAction("com.minio.io.alice.xray_broadcast");
        context.sendBroadcast(intent);
    }

    public void broadcastIntent(byte[] payload) {
        if (payload == null) {
            return;
        }
        String strPayload = null;
        Intent intent = new Intent();
        try {
            strPayload = new String(payload, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        intent.putExtra(String.valueOf("XRayCast"), strPayload);
        intent.setAction("com.minio.io.alice.xray_broadcast");
        context.sendBroadcast(intent);
    }
}
