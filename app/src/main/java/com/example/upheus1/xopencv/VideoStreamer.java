package com.example.upheus1.xopencv;

import android.media.MediaRecorder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class VideoStreamer {

    Socket socket = null;
    public VideoStreamer()  {

        String host = "192.168.1.106";
        try {
            socket = new Socket(host, 8080);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connect(byte[] buf) {

        byte[] bytes =  buf;
        int len = buf.length;
        OutputStream out = null;
        try {
            out = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        int count;
        while ((count = len) > 0) {
            try {
                out.write(bytes, 0, count);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
