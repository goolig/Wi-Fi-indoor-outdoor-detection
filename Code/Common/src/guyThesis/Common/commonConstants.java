/**
 * Created by shtar on 01/04/14.
 */

package guyThesis.Common;

import java.io.IOException;
import java.util.List;
import java.util.logging.*;

//TODO: Fix hebrew file support
//TODO: Add inside\outside option
//TODO: Add GPS collection

public class commonConstants {


    public static final String filetitleInsideState = "indoorsState";
    public static final String filetitleRecordLabelsState = "RecordLabels";
    public static final String filetitleH1 = "H1";
    public static final String filetitleH2 = "H2";
    public static final String filetitleH3 = "H3";
    public static final String filetitleLevel = "Level";
    public static final String filetitleFrequency = "Frequency";
    public static final String filetitleCapabilities = "Capabilities";
    public static final String filetitleSSID = "SSID";
    public static final String filetitleBSSID = "BSSID";
    public static final String filetitleTS = "TS";
    public static final String filetitleGPSLatitude = "GPSLatitude";
    public static final String filetitleGPSLongitude ="GPSLongitude";
    public static final String filetitleGPSAltitude ="GPSAltitude";
    public static final String filetitleGPSAccuracy = "GPSAccuracy";
    public static final String filetitleGPSProvider="GPSProvider";
    public static final String filetitleGPSTime="GPSTime";
    public static final String filetitleGPSElapsedRealtimeNanos="GPSElapsedRealtimeNanos";
    public static final String filetitleGPSisFromMockProvider="isFromMockProvider";
    public static final String filetitleGPSSpeed ="GPSSpeed";
    public static final String filetitleGPSHasSpeed ="GPSHasSpeed";

    public static double[] doubleArrayListToArray(List<Double> instancesPredictions) {
        double ans [] = new double[instancesPredictions.size()];
        for (int i = 0; i < instancesPredictions.size(); i++) {
            ans[i] = instancesPredictions.get(i);
        }
        return ans;
    }


    public static int[] intArrayListToArray(List<Integer> instances) {
        int ans [] = new int[instances.size()];
        for (int i = 0; i < instances.size(); i++) {
            ans[i] = instances.get(i);
        }
        return ans;
    }

    public enum logEvents {
        click, mistake, scanStateChange, scanStateChanged, appStateChanged
    }

    //File Constants1
    public static final String WifiScanRawFileName = "WifiScan.csv"; // the file will be created in the download folder
    public static final String logFileName = "WifiScanLog.csv"; // the file will be created in the download folder
    public static final String columnSeparator = ",";
    public static String DBPathTrain = "C:\\guy\\ThesisFinal\\DB";
    public static String baseNameForTransformedFile = "transformedDataRSS13.csv";
    public static String baseNameForArffFile = "transformedDataRSS13.arff";

    public static String inputRawDataFolderName = "Input\\train";
    public static String inRawDataFolderPath = DBPathTrain +"\\" + inputRawDataFolderName;
    public static String outputRawDataFolderName = "Output";
    public static String OutTransformedFilePath = DBPathTrain + "\\" + outputRawDataFolderName + "\\" + baseNameForTransformedFile;

    public static String outputArffFolderName = "Arff";
    // Title constants
    public static String filetitleDevice = "Device";
    public static String filetitleNumOfVisibleAp = "NumOfVisibleAps";

    //These constants are used when a scan yeild no Aps and a mock AP is used
    public static String emptyWifiBSSID = "emptyScan";
    public static String emptyWifiSSID = "emptyScan";
    public static String emptyWifiCapabilities = "emptyScan";
    public static String emptyWifiFrequency = "1";
    public static String emptyWifiLevel = "-1";
    public static Integer emptyWifiBSSID_ID = -1;


    public static void setLoggerThings(Logger log) {
        log.setLevel(Level.ALL);
        FileHandler fileHandler=null;
     //   try {
       //     fileHandler = new FileHandler("log\\%u\\myapp-log%u.%g.txt",true);
       //     fileHandler.setFormatter(new SimpleFormatter());
       //     fileHandler.setLevel(Level.ALL);
       // } catch (IOException e) {
      //      e.printStackTrace();
     //   }
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter());
        consoleHandler.setLevel(Level.ALL);
        if(fileHandler!=null)
            log.addHandler(fileHandler);
        log.addHandler(consoleHandler);
    }

    public static String ArffFilePath = DBPathTrain + "\\" + outputArffFolderName + "\\" + baseNameForArffFile ;
}
