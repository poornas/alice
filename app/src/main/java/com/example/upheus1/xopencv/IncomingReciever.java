package com.example.upheus1.xopencv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
/**
 * Created by deekoder on 1/24/17.
 * Incoming Reciever reacts to messages coming back from the XRay Server. It can toast or to Text2Speech readout of the response.
 */

public class IncomingReciever extends BroadcastReceiver {
    public IncomingReciever() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i("--->", "Sending Toast");

        String msg = intent.getStringExtra(String.valueOf(R.string.xray_broadcast));
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();

    }
}
