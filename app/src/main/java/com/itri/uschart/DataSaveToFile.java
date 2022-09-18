package com.itri.uschart;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

public class DataSaveToFile {

    Activity activity;
    public DataSaveToFile(Activity activity){
        this.activity = activity;
    }

    public void internalWrite(String fileName, String data){
        try {
            FileOutputStream outputStream = activity.openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(data.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("internalWrite Error", e.toString());
        }
    }

    public void extelnalPublicCreateFoler(String folderName, String fileName, String data){
        File dir = getExtermalStoragePublicDir(folderName);
        File f = new File(dir.getPath(), fileName);
        try {
            FileOutputStream outputStream = new FileOutputStream(f);
            outputStream.write(data.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("extelnalPublic Error", e.toString());
        }
    }

    private File getExtermalStoragePublicDir(String folderName) {
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if(file.mkdir()){
            File f = new File(file, folderName);
            if(f.mkdir()){
                return f;
            }
        }
        return new File(file, folderName);
    }

    public void extelnalPrivateCreateFoler(String folderName, String fileName, String data){
        File dir = getExtermalStoragePrivateDir(folderName);
        File f = new File(dir, fileName);
        try {
            FileOutputStream outputStream = new FileOutputStream(f);
            outputStream.write(data.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("extelnalPrivate Error", e.toString());
        }
    }

    public void extelnalPrivateCreateFolerImageCapture(String folderName, String fileName, Bitmap bitmap){
        File dir = getExtermalStoragePrivateDir(folderName);
        File f = new File(dir, fileName);
        try {
            FileOutputStream outputStream = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("extelnalPrivate Error", e.toString());
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private File getExtermalStoragePrivateDir(String albumName) {
        File file = new File(activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES), albumName);
        if (!file.mkdirs()) {
            Log.e("", "Directory not created or exist");
        }
        return file;
    }
}
