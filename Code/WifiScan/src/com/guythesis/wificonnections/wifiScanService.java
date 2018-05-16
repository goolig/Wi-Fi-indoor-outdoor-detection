package com.guythesis.wificonnections;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Guy on 16/03/2015.
 *
 */public class wifiScanService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    public static boolean s_scanning = true;
    private static final String s_title = wifiScanService.class.getName();

    private int m_failedWifiStart=0;
    private WifiBroadcastReceiver m_receiverWifi=null;
    private GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    public static Location mLastLocation;
    private boolean receiverRegistered =false;

    /**
     * Holds the scheduler for scanning. used to stop the scan.
     */
    ScheduledExecutorService m_scheduler;


    public static String m_H1;
    public static String m_H2;
    public static String m_H3;
    public static boolean m_indoorsState;
    public static boolean m_recordLabelState;
    public static String m_deviceName;
    private boolean m_scanning =false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(s_title,"On start service");
        m_deviceName = intent.getStringExtra(AppUtils.PREF_DEVICE_TITLE);
        m_H1= intent.getStringExtra(AppUtils.PREF_H1_TITLE);
        m_H2= intent.getStringExtra(AppUtils.PREF_H2_TITLE);
        m_H3= intent.getStringExtra(AppUtils.PREF_H3_TITLE);
        m_recordLabelState = intent.getBooleanExtra(AppUtils.PREF_RECORD_LABELS_STATE,false);
        m_scanning = intent.getBooleanExtra(AppUtils.PREF_STATE_IS_SCANNING,false);
        m_indoorsState= intent.getBooleanExtra(AppUtils.PREF_INSIDE_STATE,false);

        if(m_scheduler==null&&m_scanning)
            StartScanService();
        if(m_scheduler!=null&&!m_scanning)
            stopScanService();

        startForeground();


        return Service.START_REDELIVER_INTENT;
    }

    private void startForeground() {
        Intent notificationIntent = new Intent(this, mainActivity.class);
        notificationIntent.setAction("wifi.startApplication");
        if(android.os.Build.VERSION.SDK_INT>=11)
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        else
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,notificationIntent, 0);
        Bitmap icon = BitmapFactory.decodeResource(getResources(),R.drawable.backgrndgreen);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Wifi Scan")
                .setTicker("Wifi Scan")
                .setContentText("Now scanning")
                .setSmallIcon(R.drawable.backgrndgreen)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
        startForeground(1,notification);
    }

    @Override
    public void onCreate(){
        Log.i(s_title,"creating the service");
        m_receiverWifi = new WifiBroadcastReceiver();
        enableWifi();
        setGPSUp();
        registerReceiver();
        startLocationUpdates();
        super.onCreate();

    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    private void disconnectGoogleApi(){
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
    }


    private void registerReceiver(){
        this.registerReceiver(m_receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        receiverRegistered=true;

    }


    private void unRegisterReceiver(){
        if(this.receiverRegistered)
            this.unregisterReceiver(m_receiverWifi);
        receiverRegistered=false;

    }


    private void setGPSUp() {
        // locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //  locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        buildGoogleApiClient();
        createLocationRequest();
    }

    protected synchronized void buildGoogleApiClient() {
        Log.i(s_title,"Connecting to google play");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }


    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(s_title,"Google api is connected");
        //mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient); // set initial location
        if (mLocationRequest!=null && s_scanning) {
            startLocationUpdates();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(s_title,"Connection to google api client is suspended");
    }

    @Override
    public void onLocationChanged(Location location) {

        if(mLastLocation==null ){
            mLastLocation = location; // any coordinate is better than no coordinate
        }
        else
        {
            if (mLastLocation.hasAccuracy() && location.hasAccuracy()) // all location should have accuracy
            {
                long deltaTime = location.getTime() - mLastLocation.getTime();
                if(location.getAccuracy() < mLastLocation.getAccuracy() && deltaTime>0){
                    //if the new one has better accuracy, and is newer its better.
                    mLastLocation=location;
                }
                else
                {
                    if(deltaTime<0){
                        //i got an older coordinate...
                        if(Math.abs(deltaTime/1000) > Math.abs(mLastLocation.getAccuracy() - location.getAccuracy()))
                        {
                            mLastLocation=location; // each second that passes, im willing to give another meter
                        }
                    }
                    else
                    {
                        // new coordinate has worse accuracy, but its newer.
                        if(Math.abs(deltaTime/1000)> Math.abs(mLastLocation.getAccuracy() - location.getAccuracy()))
                        {
                            mLastLocation=location; // each second that passes, im willing to give another meter
                            //TODO, if the device hasn't moved, its doesn't make sense. (maybe Google is taking care if it,,,)
                        }
                    }
                }
            }
            else
            {
                if(location.hasAccuracy()){ // a location that has accuracy is better than not knowing
                    mLastLocation=location;
                }
            }
        }


        Log.i(s_title,"Got new location: " + location.getLatitude());
        //do somthing here

    }



    protected void startLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        else
        {
            Log.i(s_title,"Cannot start location updates! the API is not connection");
        }
    }

    protected void stopLocationUpdates() {
        if(mGoogleApiClient.isConnected()){
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        }
        else
        {
            Log.i(s_title,"the API is not connected, cannot disconnect");
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(AppUtils.LocationInterval);
        mLocationRequest.setFastestInterval(AppUtils.FastestLocationInterval);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY); // use balanced power
    }

    private boolean enableWifi() {
        boolean ans=false;
        if(m_failedWifiStart<50){
            WifiManager mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            if (!mainWifi.isWifiEnabled())
            {
                Toast.makeText(getApplicationContext(), "wifi is disabled... enabling it", Toast.LENGTH_LONG).show();
                mainWifi.setWifiEnabled(true);
            }
            if(!mainWifi.isWifiEnabled())
                m_failedWifiStart++;
            else {
                m_failedWifiStart = 0;
                ans=true;
            }
        }
        else
            Toast.makeText(getApplicationContext(), "Cannot enable Wifi after 50 attempts. Stop the scan and contact Guy.", Toast.LENGTH_LONG).show();
        return ans;
    }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(s_title,"failed to connect to google api");
    }


    public void StartScanService() {
        if(enableWifi()) {
            m_scheduler =
                    Executors.newSingleThreadScheduledExecutor();
            final Context c = this;
            m_scheduler.scheduleAtFixedRate
                    (new Runnable() {
                        public void run() {
                            Log.i("", "Starting scan");
                            ((WifiManager) c.getSystemService(Context.WIFI_SERVICE)).startScan();
                        }
                    }, 0, AppUtils.scanIntervalInSeconds, TimeUnit.SECONDS);
        }
    }

    public void stopScanService(){
        Log.i("", "Stopping scan");
        if(m_scheduler!=null)
        {
            m_scheduler.shutdown();
            m_scheduler=null;
        }
    }

    @Override
    public void onDestroy() {
        stopScanService();
        unRegisterReceiver();
        disconnectGoogleApi();
        stopForeground(true);
        super.onDestroy();
    }


}
