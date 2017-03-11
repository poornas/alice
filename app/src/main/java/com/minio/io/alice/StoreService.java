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

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import io.minio.MinioClient;

/**
 *  This is a service that writes to Object Storage.
 */

public class StoreService extends Service {

    private IBinder mBinder = new AliceServiceBinder();
    MinioClient minioClient;

    @Override
    public void onCreate() {
        super.onCreate();


        // TODO: These params could come from the xray server. Make this configurable.
        try {
            minioClient = new MinioClient("https://play.minio.io:9000", "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {

        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {

        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }


    public void save(byte[] matByteArray){

        ByteArrayInputStream inputStream = new ByteArrayInputStream(matByteArray);
        try {
            // Check if the bucket already exists.
            boolean isExist = minioClient.bucketExists("alice");
            if (isExist) {
                Log.i(MainActivity.TAG, "Bucket already exists.");
            } else {
                // Make a new bucket called asiatrip to hold a zip file of photos.
                minioClient.makeBucket("alice");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        // Create an object.
        try {

            minioClient.putObject("alice", String.valueOf(new java.util.Date()), inputStream, inputStream.available(), "image/png");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public class AliceServiceBinder extends Binder {
        StoreService getService() {
            return StoreService.this;
        }
    }
}
