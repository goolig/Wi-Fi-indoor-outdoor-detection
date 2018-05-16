package com.guythesis.wificonnections;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;
import guyThesis.Common.commonConstants;

import java.util.List;

/**
* Created by Guy on 11/11/2014.
*/
public class WifiBroadcastReceiver extends BroadcastReceiver {

    StringBuilder TextToFile = new StringBuilder();
    List<ScanResult> wifiList;


    // This method call when number of wifi connections changed
    public void onReceive(final Context c, Intent intent) {
        Log.i(this.getClass().getName(), "Got results from scan");
        TextToFile = new StringBuilder();
        wifiList = ((WifiManager)c.getSystemService(Context.WIFI_SERVICE)).getScanResults();
        AppUtils.resetTimeAndLocation();
        for(int row = 0; row < wifiList.size(); row++){
            for(int col = 0; col < AppUtils.numOfCol(); col++) {
                TextToFile.append(replaceBadCharsForCSV(AppUtils.getElementNum(col, wifiList.get(row), wifiScanService.m_deviceName, wifiScanService.m_H1, wifiScanService.m_H2, wifiScanService.m_H3, wifiScanService.m_recordLabelState, wifiScanService.m_indoorsState)));
                if(col< AppUtils.numOfCol()-1){
                    TextToFile.append(commonConstants.columnSeparator);
                }
            }
            TextToFile.append(System.getProperty("line.separator"));
        }
        if(wifiList.size()==0)
        {// adding empty wifi scan.
            for(int col = 0; col < AppUtils.numOfCol(); col++) {
               TextToFile.append(AppUtils.getElementNum(col, null, wifiScanService.m_deviceName, wifiScanService.m_H1, wifiScanService.m_H2, wifiScanService.m_H3, wifiScanService.m_recordLabelState, wifiScanService.m_indoorsState).replace(commonConstants.columnSeparator, "\",\""));
                if(col< AppUtils.numOfCol()-1){
                    TextToFile.append(commonConstants.columnSeparator);
                }
            }
            TextToFile.append(System.getProperty("line.separator"));
        }
        new writeToFile(TextToFile.toString(),c, writeToFile.fileTypes.scan).doInBackground();
    }

    private String replaceBadCharsForCSV(String elementNum) {
        elementNum=elementNum.replace(commonConstants.columnSeparator, "^"); // replace commas
        elementNum=elementNum.replace(System.getProperty("line.separator"), "%"); // replace new lines
        elementNum=elementNum.replace('"','&'); //replace double quotes
        return elementNum;
    }


}
