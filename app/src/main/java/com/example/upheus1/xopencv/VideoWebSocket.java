package com.example.upheus1.xopencv;


import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import java.io.UnsupportedEncodingException;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketConnectionHandler;
import de.tavendo.autobahn.WebSocketOptions;

import static android.content.ContentValues.TAG;

/**
 * Created by deekoder on 1/24/17.
 * VideoWebSocket is responsible for connecting to the Xray server.
 */

public class VideoWebSocket {

    private WebSocketConnection mConnection = new WebSocketConnection();
    Context context;

    public void connect(Context context) {
        this.context = context;
        Log.v("========>", "Control here");

        final String wsuri = "ws://192.168.1.225:8080";

        final WebSocketOptions webSocketOptions = new WebSocketOptions();
        webSocketOptions.setMaxMessagePayloadSize(100 * 1024 * 1024);

        try {
            mConnection.connect(wsuri, new WebSocketConnectionHandler() {
                @Override
                public void onOpen() {
                    Log.d("************>>>>>", "Status: Connected to " + wsuri);
                    //  mConnection.sendTextMessage("Hello, world!");
                }

                @Override
                public void onTextMessage(String payload) {
                    Log.i("************>>>>>", "Got echo: " + payload);
                    //broadcastIntent(payload);

                }

                @Override
                public void onRawTextMessage(byte[] payload) {
                    try {
                        //rawText = new String(payload, "UTF-8");
                        Log.i("************>>>>>", "ON RAW TEXT");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onBinaryMessage(byte[] payload) {
                    Log.i("************>>>>>", "ON BINARY MESSAGE");

                }

                @Override
                public void onClose(int code, String reason) {
                    Log.d("************>>>>>", "Connection lost.");
                }
            }, webSocketOptions);
        } catch (WebSocketException e) {

            Log.d("************>>>>>", e.toString());
        }
    }

    public void sendPayload(byte[] b) {
        if (mConnection.isConnected()) {
            Log.i("************>>>>>", "Is connected sending message......");
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
