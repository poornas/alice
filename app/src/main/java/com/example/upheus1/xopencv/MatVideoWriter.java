package com.example.upheus1.xopencv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;

import static android.R.attr.path;
import static android.R.attr.type;

/**
 * Created by Upheus1 on 2/2/17.
 */

// Harsha this file can be ignored for now. I will include this later on to make the solution be more elegant and readable.

public class MatVideoWriter {

        boolean recording;
        VideoTask videoTask;


        public  MatVideoWriter() {
            recording = true;
          //  videoTask = new VideoTask();
        }

       /* public void stop(){
            recording = false;

            try{
                File file = new File(dir, "video.mp4");
                SequenceEncoder encoder = new SequenceEncoder(file);

                List<File> files = Arrays.asList(dir.listFiles());
                Collections.sort(files, new Comparator<File>(){
                    @Override
                    public int compare(File lhs, File rhs) {
                        return lhs.getName().compareTo(rhs.getName());
                    }
                });

                for(File f : files){
                    Log.i("VideoTest", "Encoding image: " + f.getAbsolutePath());
                    try{
                        Bitmap frame = BitmapFactory.decodeFile(f.getAbsolutePath());
                        encoder.encodeImage(frame);
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }

                }
                encoder.finish();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }*/

        public void write(Mat mat, VideoWebSocket videoWebSocket){

            int length = (int) (mat.total() * mat.elemSize());
            byte buffer[] = new byte[length];
            mat.get(0, 0, buffer);
            Log.d("--->", "Buffered the mat to bytes");
            try
            {

                videoTask = new VideoTask(buffer);
                videoTask.execute();
                // FileUtils.writeByteArrayToFile(file, buffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mat.release();

        }

        public boolean isRecording() {
            return recording;
        }

     /*   public static void save(Mat mat, String name)
        {
            File file = new File(path, name);
            int length = (int) (mat.total() * mat.elemSize());
            byte buffer[] = new byte[length];
            mat.get(0, 0, buffer);
            try
            {
               // FileUtils.writeByteArrayToFile(file, buffer);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        public static Mat load(String name)
        {
            File file = new File(path, name);
            byte[] buffer = new byte[0];
            try
            {
              //  buffer = FileUtils.readFileToByteArray(file);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            Mat mat = new Mat(row, col, type);
            mat.put(0, 0, buffer);
            return mat;
        }*/

}
