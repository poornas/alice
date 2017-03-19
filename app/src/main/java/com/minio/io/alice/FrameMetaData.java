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

import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/* Constructs a JSON object from the frame meta data and faces array for send to XRay
 *
 * Frame Metadata: https://developers.google.com/android/reference/com/google/android/gms/vision/Frame.Metadata
 * Face metadata: https://developers.google.com/android/reference/com/google/android/gms/vision/face/Face
 *
 */

public class FrameMetaData {

    private JSONObject metaMap;

    public FrameMetaData(Frame.Metadata metadata, SparseArray<Face> facesArray) {

        try {
            metaMap = new JSONObject();
            metaMap.put("frame", getMetaDataJSON(metadata));
            metaMap.put("faces", getFacesJSON(facesArray));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /* Stuff the metadata object */
    private JSONObject getMetaDataJSON(Frame.Metadata metadata) {

        JSONObject meta = null;
        try {
            meta = new JSONObject();
            meta.put("id", Integer.toString(metadata.getId()));
            meta.put("format", Integer.toString(metadata.getFormat()));
            meta.put("width", Integer.toString(metadata.getWidth()));
            meta.put("height", Integer.toString(metadata.getHeight()));
            meta.put("rotation", Integer.toString(metadata.getRotation()));
            meta.put("timestamp", Long.toString(metadata.getTimestampMillis()));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return meta;
    }

    /* Construct the faces JSONArray */
    private JSONArray getFacesJSON(SparseArray<Face> faces) {

        JSONArray faceArray = null;
        try {
            faceArray = new JSONArray();
            for (int i = 0; i < faces.size(); i++) {
                int key = faces.keyAt(i);
                Face face = faces.get(key);
                JSONObject fObject = new JSONObject();
                fObject.put("id",Integer.toString(face.getId()));
                fObject.put("eulerY",Float.toString(face.getEulerY()));
                fObject.put("eulerZ",Float.toString(face.getEulerZ()));
                fObject.put("height",Float.toString(face.getHeight()));
                fObject.put("width",Float.toString(face.getWidth()));

                fObject.put("lefteyeopen",Float.toString(face.getIsLeftEyeOpenProbability()));
                fObject.put("righteyeopen",Float.toString(face.getIsRightEyeOpenProbability()));
                fObject.put("smiling",Float.toString(face.getIsSmilingProbability()));
                fObject.put("topX",Float.toString(face.getPosition().x));
                fObject.put("topY",Float.toString(face.getPosition().x));
                faceArray.put(i,fObject);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return faceArray;
    }

    public String toString() {
        return metaMap.toString();
    }
}
