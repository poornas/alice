package com.example.upheus1.xopencv;


import android.util.Log;

import java.io.UnsupportedEncodingException;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;
import de.tavendo.autobahn.WebSocketOptions;

import static android.content.ContentValues.TAG;

/**
 * Created by Upheus1 on 1/24/17.
 */

public class VideoWebSocket {

    private WebSocketConnection mConnection = new WebSocketConnection();

    public void connect() {
        Log.v("========>", "Control here");
        final String wsuri = "ws://192.168.1.106:8080";
        final WebSocketOptions webSocketOptions = new WebSocketOptions();
        webSocketOptions.setMaxMessagePayloadSize(100 * 1024 * 1024);

        try {
            mConnection.connect(wsuri, new WebSocketHandler() {
                @Override
                public void onOpen() {
                    Log.d("************>>>>>", "Status: Connected to " + wsuri);
                  //  mConnection.sendTextMessage("Hello, world!");
                }

                @Override
                public void onTextMessage(String payload) {
                    Log.i("************>>>>>", "Got echo: " + payload);
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


        if(mConnection.isConnected()) {
            Log.i("************>>>>>","Is connected sending message......");
            mConnection.sendBinaryMessage(b);
        }
    }

    public void disconnect() {
        if(mConnection.isConnected())
            mConnection.disconnect();
    }
}
