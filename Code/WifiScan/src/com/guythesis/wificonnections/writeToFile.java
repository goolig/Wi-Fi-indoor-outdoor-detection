package com.guythesis.wificonnections;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import guyThesis.Common.commonConstants;

import java.io.*;

/**
 * Created by Guy on 13/11/2014.
 *
 */
public class writeToFile extends AsyncTask<Void, Void, Long> {


    public enum fileTypes {
        log,scan;
    }

    public String s_filePath=null;
    private static String s_title = "writeToFile";
    private String m_textToFile;
    private fileTypes m_fileType;
    Context m_c;

    public writeToFile(String text, Context c, fileTypes fileType){
        m_fileType=fileType;
        m_textToFile =text;
        m_c=c;
    }

    @Override
    protected Long doInBackground(Void... params) {
        Log.i(this.getClass().getName(),"Writing scan results to file.");
        new Thread() {
            public void run() {
                try {
                    if(s_filePath==null)
                    {
                        Log.i(this.getClass().getName(),"File does not exist, creating it.");
                        createFile();
                    }
                    if(!new File(s_filePath).exists()){

                        Log.i(this.getClass().getName(), "The file does not exist after creation.. somthing is wrong");
                    }
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                            new FileOutputStream(s_filePath,true), "UTF8"
                    ));
                    if(m_textToFile ==null) Log.i(this.getClass().getName(),"The text to write is null...");
                    out.append(m_textToFile);
                    out.close();
                    m_c.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + s_filePath)));
                    Log.i(this.getClass().getName(),"Wrote to file successfully.");
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(s_title, e.toString());
                }
            }
        }.start();


        return null;
    }


    public void createScanFile(){

        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/";
        final File outFile = new File(root + commonConstants.WifiScanRawFileName);
        s_filePath = outFile.getAbsolutePath();
        if(!outFile.exists()){
            Thread cheatNewFile = new Thread( new Runnable()
            {
                @Override
                public void run()
                {
                    try {
                        FileWriter fw = new FileWriter(outFile);
                        for (int i=0; i< AppUtils.numOfCol();i++)
                        {
                            fw.append(AppUtils.getTitleNum(i));
                            if(i< AppUtils.numOfCol()-1){
                                fw.append(commonConstants.columnSeparator);
                            }
                        }
                        fw.append(System.getProperty("line.separator"));
                        fw.close();
                        m_c.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + s_filePath)));
                        Log.i(s_title, "file was created: " + s_filePath);

                    } catch (Exception e) {
                        Log.i(s_title, e.toString());
                    }
                }
            }
            );
            cheatNewFile.start();
            try {
                cheatNewFile.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        else
            Log.i(s_title, "file exits: " + s_filePath);

        if(!outFile.exists()){
            Log.i(s_title, "File was not created. this is not good!");

        }


    }

    public void createFile() {
        switch (m_fileType){
            case scan:
                createScanFile();
                break;
            case log:
                createLogFile();
                break;
        }
    }

    private void createLogFile() {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/";
        final File outFile = new File(root + commonConstants.logFileName);
        s_filePath = outFile.getAbsolutePath();
        if(!outFile.exists()){
            Thread cheatNewFile = new Thread( new Runnable()
            {
                @Override
                public void run()
                {
                    try {
                        FileWriter fw = new FileWriter(outFile);
                        fw.append(AppUtils.getLogTitle());
                        fw.append(System.getProperty("line.separator"));
                        fw.close();
                        m_c.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + s_filePath)));
                        Log.i(s_title, "file was created: " + s_filePath);

                    } catch (Exception e) {
                        Log.i(s_title, e.toString());
                    }
                }
            }
            );
            cheatNewFile.start();
            try {
                cheatNewFile.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        else
            Log.i(s_title, "file exits: " + s_filePath);

        if(!outFile.exists()){
            Log.i(s_title, "File was not created. this is not good!");

        }
    }
}
