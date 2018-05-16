package com.guythesis.wificonnections;

import android.content.Context;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.util.Log;
import guyThesis.Common.commonConstants;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Guy on 20/11/2014.
 * Constant values and functions
 */
public class AppUtils {

    static final String PREF_STATE_IS_SCANNING = "scanState";
    static final String PREF_H1_TITLE = "default_H1";
    static final String PREF_H2_TITLE = "default_H2";
    static final String PREF_H3_TITLE = "default_H3";
    static final String PREF_DEVICE_TITLE = "device";
    static final String PREF_RECORD_LABELS_STATE="recordLabels";
    static final String PREF_INSIDE_STATE = "indoors";

    private static final String s_title = AppUtils.class.getName();

    // file variables
    public static String default_deviceName ="Guy";
    public static String default_H1 ="";
    public static String default_H2 ="";
    public static String default_H3 ="";
    public static boolean default_recordLabelState =false;
    public static boolean default_indoorsState = false;
    private static String lastTS =""; // the timestamp for the current scan
    private static Location currentLocation =null; // the timestamp for the current scan
    public static int FastestLocationInterval = 3 * 1000; //3 seconds = 3 * 1000 MS
    public static int LocationInterval = 20 * 1000; // 20 seconds = 20 * 1000 MS


    public static final String[] RawfileStructure = {
            commonConstants.filetitleDevice,// 0
            commonConstants.filetitleTS, // 1
            commonConstants.filetitleBSSID, // 2
            commonConstants.filetitleSSID, // 3
            commonConstants.filetitleCapabilities, // 4
            commonConstants.filetitleFrequency, // 5
            commonConstants.filetitleLevel, // 6
            commonConstants.filetitleH1, // 7
            commonConstants.filetitleH2, // 8
            commonConstants.filetitleH3, // 9
            commonConstants.filetitleRecordLabelsState, // 10
            commonConstants.filetitleInsideState, // 11
            commonConstants.filetitleGPSLatitude, // 12
            commonConstants.filetitleGPSLongitude, // 13
            commonConstants.filetitleGPSAltitude, // 14
            commonConstants.filetitleGPSAccuracy, // 15
            commonConstants.filetitleGPSProvider, // 16
            commonConstants.filetitleGPSTime, // 17
            commonConstants.filetitleGPSElapsedRealtimeNanos, // 18
            commonConstants.filetitleGPSisFromMockProvider, // 19
            commonConstants.filetitleGPSSpeed, // 20
            commonConstants.filetitleGPSHasSpeed // 21


    };



    /**
     * Sets the interval between scans
     */
    public static int scanIntervalInSeconds = 4;


    public static int numOfCol(){
        return RawfileStructure.length;
    }

    public static String getTitleNum(int i){
        return RawfileStructure[i];
    }

    public static void resetTimeAndLocation(){
        currentLocation = wifiScanService.mLastLocation;
        Calendar cal = Calendar.getInstance();
        lastTS ="" + cal.getTimeInMillis(); // ALL TIME IN APP ARE GMT 0//
         }

 /*
    This function returns the ith element of a row in the file.
    it should be synchronized manually with the titles
    resetTimeAndLocation must be called before each scan write
     */
    public static String getElementNum(int i, ScanResult scanResult , String deviceName, String H1,String  H2,String  H3,boolean recordLabelState,boolean indoorsState ){

        String ans="";
        try {
            switch (i) {
                case 0:
                    ans = deviceName;
                    break;
                case 1:
                    ans = lastTS;
                    break;
                case 2:
                    if (scanResult != null) {
                        ans = scanResult.BSSID;
                    } else {
                        ans = commonConstants.emptyWifiBSSID;
                    }
                    break;
                case 3:
                    if (scanResult != null) {
                        ans = scanResult.SSID;
                    } else {
                        ans = commonConstants.emptyWifiSSID;
                    }
                    break;
                case 4:
                    if (scanResult != null) {
                        ans = scanResult.capabilities;
                    } else {
                        ans = commonConstants.emptyWifiCapabilities;
                    }
                    break;
                case 5:
                    if (scanResult != null) {
                        ans = "" + scanResult.frequency;
                    } else {
                        ans = commonConstants.emptyWifiFrequency;
                    }
                    break;
                case 6:
                    if (scanResult != null) {
                        ans = "" + scanResult.level;
                    } else {
                        ans = commonConstants.emptyWifiLevel;
                    }
                    break;
                case 7:
                    ans = H1;
                    break;
                case 8:
                    ans = H2;
                    break;
                case 9:
                    ans = H3;
                    break;
                case 10:
                    ans = "" + recordLabelState;
                    break;
                case 11:
                    ans = "" + indoorsState;
                    break;
                case 12:
                    ans = "" + currentLocation.getLatitude();
                    break;
                case 13:
                    ans = "" + currentLocation.getLongitude();
                    break;
                case 14:
                    ans = "" + currentLocation.getAltitude();
                    break;
                case 15:
                    ans = "" + currentLocation.getAccuracy();
                    break;
                case 16:
                    ans = "" + currentLocation.getProvider();
                    break;
                case 17:
                    ans = "" + currentLocation.getTime();
                    break;
                case 18:
                    if(android.os.Build.VERSION.SDK_INT>17)
                        ans = "" + currentLocation.getElapsedRealtimeNanos();
                    break;
                case 19:
                    if(android.os.Build.VERSION.SDK_INT>17)
                        ans = "" + currentLocation.isFromMockProvider();
                    break;
                case 20:
                    ans = "" + currentLocation.getSpeed();
                    break;
                case 21:
                    ans = "" + currentLocation.hasSpeed();
                    break;
            }
        }
        catch (Exception E)
        {
            //Log.i(s_title,"Cannot get value to write in file, Location is probably null.");
        }
        return ans;
    }


    public static String getLogTitle(){
        return "TS"+commonConstants.columnSeparator +"eventID" + commonConstants.columnSeparator + "value" + commonConstants.columnSeparator + "comment";
    }

    public static void writeToLog(Context c,commonConstants.logEvents eventID, String value, String comment){
        String textToLog = "";
        textToLog += new Date().getTime();
        textToLog += commonConstants.columnSeparator;
        if(eventID!=null) textToLog +=eventID.name();
        textToLog += commonConstants.columnSeparator;
        if(value!=null) textToLog +=value;
        textToLog += commonConstants.columnSeparator;
        if(comment!=null) textToLog+=comment;
        textToLog+=System.getProperty("line.separator");

        new writeToFile(textToLog,c, writeToFile.fileTypes.log).doInBackground();

    }


}
