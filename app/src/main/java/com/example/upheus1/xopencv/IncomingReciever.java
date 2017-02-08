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

package com.example.upheus1.xopencv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
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
