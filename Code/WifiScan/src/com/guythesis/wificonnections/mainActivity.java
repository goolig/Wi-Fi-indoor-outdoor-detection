package com.guythesis.wificonnections;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import guyThesis.Common.commonConstants;

public class mainActivity extends Activity
{

    private static final String s_title = mainActivity.class.getName();

    public static final String appPREFERENCES = "WifiScanPrefs" ;
    SharedPreferences sharedpreferences;

    private boolean m_scanning = false;
    CheckBox cbkRecordLabels;
    CheckBox cbkInsideBuildings;
    Button btnSubmit;
    private String m_H1;
    private String m_H2;
    private String m_H3;
    private boolean m_indoorsState;
    private boolean m_recordLabelState;
    private String m_deviceName;

    //final Drawable oldBackground = findViewById(R.id.deviceIDEdit).getBackground();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(s_title, "Starting main activity");
        sharedpreferences = getSharedPreferences(appPREFERENCES, Context.MODE_PRIVATE);
        setContentView(R.layout.activity_wifi_connections);
        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        cbkRecordLabels   = (CheckBox) findViewById(R.id.ckbRecordLabels);
        cbkInsideBuildings= (CheckBox) findViewById(R.id.ckbInside);
        new writeToFile("",this, writeToFile.fileTypes.scan).createFile();
        new writeToFile("",this, writeToFile.fileTypes.log).createFile();

        UpdateAppFromSavedValues();
        setUIListens();
        AppUtils.writeToLog(this, commonConstants.logEvents.appStateChanged,"start","Device name is: " + sharedpreferences.getString(AppUtils.PREF_DEVICE_TITLE,""));
        AppUtils.writeToLog(this, commonConstants.logEvents.appStateChanged,"start","H1 is: " + sharedpreferences.getString(AppUtils.PREF_H1_TITLE,""));
        AppUtils.writeToLog(this, commonConstants.logEvents.appStateChanged,"start","H2 is: " + sharedpreferences.getString(AppUtils.PREF_H2_TITLE,""));
        AppUtils.writeToLog(this, commonConstants.logEvents.appStateChanged,"start","H3 is: " + sharedpreferences.getString(AppUtils.PREF_H3_TITLE,""));
    }

    private void stopScanService() {
        AppUtils.writeToLog(this, commonConstants.logEvents.scanStateChanged,"stop","");
        Intent i= new Intent(this, wifiScanService.class);
        stopService(i);
    }

    private void startScanServiceIfNeeded() {
        if(m_scanning) {
            Log.i(s_title, "Starting service");
            Intent i = new Intent(this, wifiScanService.class);
            i.putExtra(AppUtils.PREF_H1_TITLE, m_H1);
            i.putExtra(AppUtils.PREF_H2_TITLE, m_H2);
            i.putExtra(AppUtils.PREF_H3_TITLE, m_H3);
            i.putExtra(AppUtils.PREF_INSIDE_STATE, m_indoorsState);
            i.putExtra(AppUtils.PREF_DEVICE_TITLE, m_deviceName);
            i.putExtra(AppUtils.PREF_RECORD_LABELS_STATE, m_recordLabelState);
            i.putExtra(AppUtils.PREF_STATE_IS_SCANNING, m_scanning);

            startService(i);
            AppUtils.writeToLog(this, commonConstants.logEvents.scanStateChanged,"start","Device name is: " + sharedpreferences.getString(AppUtils.PREF_DEVICE_TITLE,""));
            AppUtils.writeToLog(this, commonConstants.logEvents.scanStateChanged,"start","H1 is: " + sharedpreferences.getString(AppUtils.PREF_H1_TITLE,""));
            AppUtils.writeToLog(this, commonConstants.logEvents.scanStateChanged,"start","H2 is: " + sharedpreferences.getString(AppUtils.PREF_H2_TITLE,""));
            AppUtils.writeToLog(this, commonConstants.logEvents.scanStateChanged,"start","H3 is: " + sharedpreferences.getString(AppUtils.PREF_H3_TITLE,""));
        }
    }

    private void setUIListens() {
        setEditTextListener();


        // create click listener
        View.OnClickListener oclBtnSubmit = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString(AppUtils.PREF_DEVICE_TITLE,((EditText) findViewById(R.id.deviceIDEdit)).getText().toString());
                editor.putString(AppUtils.PREF_H1_TITLE,((EditText) findViewById(R.id.deviceH1Edit)).getText().toString());
                editor.putString(AppUtils.PREF_H2_TITLE,((EditText) findViewById(R.id.deviceH2Edit)).getText().toString());
                editor.putString(AppUtils.PREF_H3_TITLE,((EditText) findViewById(R.id.deviceH3Edit)).getText().toString());

                m_deviceName=((EditText) findViewById(R.id.deviceIDEdit)).getText().toString();
                m_H1=((EditText) findViewById(R.id.deviceH1Edit)).getText().toString();
                m_H2=((EditText) findViewById(R.id.deviceH2Edit)).getText().toString();
                m_H3=((EditText) findViewById(R.id.deviceH3Edit)).getText().toString();
                editor.apply();
                Log.i(s_title, "Device name was set to: " + sharedpreferences.getString(AppUtils.PREF_DEVICE_TITLE,""));
                Log.i(s_title, "H1 was set to: " + sharedpreferences.getString(AppUtils.PREF_H1_TITLE,""));
                Log.i(s_title, "H2 was set to: " + sharedpreferences.getString(AppUtils.PREF_H2_TITLE,""));
                Log.i(s_title, "H3 was set to: " + sharedpreferences.getString(AppUtils.PREF_H3_TITLE,""));
                resetEditTextColor();
                startScanServiceIfNeeded();
                AppUtils.writeToLog(v.getContext(), commonConstants.logEvents.click,"setButton","Device name was set to: " + sharedpreferences.getString(AppUtils.PREF_DEVICE_TITLE,""));
                AppUtils.writeToLog(v.getContext(), commonConstants.logEvents.click,"setButton","H1 was set to: " + sharedpreferences.getString(AppUtils.PREF_H1_TITLE,""));
                AppUtils.writeToLog(v.getContext(), commonConstants.logEvents.click,"setButton","H2 was set to: " + sharedpreferences.getString(AppUtils.PREF_H2_TITLE,""));
                AppUtils.writeToLog(v.getContext(), commonConstants.logEvents.click,"setButton","H3 was set to: " + sharedpreferences.getString(AppUtils.PREF_H3_TITLE,""));


            }
        };

        cbkRecordLabels.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedpreferences.edit();
                if (((CheckBox) v).isChecked()) {
                    // Record labels
                    editor.putBoolean(AppUtils.PREF_RECORD_LABELS_STATE,true);
                    m_recordLabelState=true;
                    Log.i(s_title, "Recording Labels");
                    AppUtils.writeToLog(v.getContext(), commonConstants.logEvents.click,"cbkRecordLabels","true");

                }
                else
                {
                    //Don't record labels
                    editor.putBoolean(AppUtils.PREF_RECORD_LABELS_STATE,false);
                    m_recordLabelState=false;
                    Log.i(s_title, "NOT Recording Labels!!");
                    AppUtils.writeToLog(v.getContext(), commonConstants.logEvents.click,"cbkRecordLabels","false");
                }
                startScanServiceIfNeeded();
                editor.apply();
            }
        });

        cbkInsideBuildings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedpreferences.edit();
                if (((CheckBox) v).isChecked()) {
                    // Record labels
                    editor.putBoolean(AppUtils.PREF_INSIDE_STATE, true);
                    m_indoorsState = true;
                    Log.i(s_title, "Indoors");
                    AppUtils.writeToLog(v.getContext(), commonConstants.logEvents.click,"cbkInsideBuildings","true");


                } else {
                    //Don't record labels
                    editor.putBoolean(AppUtils.PREF_INSIDE_STATE, false);
                    m_indoorsState = false;
                    Log.i(s_title, "NOT Indoors!!");
                    AppUtils.writeToLog(v.getContext(), commonConstants.logEvents.click,"cbkInsideBuildings","false");
                }
                startScanServiceIfNeeded();
                editor.apply();
            }
        });
        btnSubmit.setOnClickListener(oclBtnSubmit);
    }

    private void resetEditTextColor() {
        (findViewById(R.id.deviceIDEdit)).getBackground().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
        (findViewById(R.id.deviceH1Edit)).getBackground().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
        (findViewById(R.id.deviceH2Edit)).getBackground().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
        (findViewById(R.id.deviceH3Edit)).getBackground().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);



    }

    private void setEditTextListener() {
        ((EditText)findViewById(R.id.deviceIDEdit)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            public void afterTextChanged(Editable s) {
                (findViewById(R.id.deviceIDEdit)).getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
            }
        });


        ((EditText)findViewById(R.id.deviceH1Edit)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            public void afterTextChanged(Editable s) {
                (findViewById(R.id.deviceH1Edit)).getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
            }
        });

        ((EditText)findViewById(R.id.deviceH2Edit)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            public void afterTextChanged(Editable s) {
                (findViewById(R.id.deviceH2Edit)).getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
            }
        });

        ((EditText)findViewById(R.id.deviceH3Edit)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            public void afterTextChanged(Editable s) {
                (findViewById(R.id.deviceH3Edit)).getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
            }
        });
    }

    private void UpdateAppFromSavedValues() {
        readDataFromSharedPrefs();
        setTextFields();
        setBGColor();
        startScanServiceIfNeeded();

    }




    private void readDataFromSharedPrefs() {
        // Check whether we're recreating a previously destroyed instance
        if (sharedpreferences != null) {
            // Restore value of members from saved state
            Log.i(s_title,"Reading from sharedPrefs");
            m_deviceName = sharedpreferences.getString(AppUtils.PREF_DEVICE_TITLE, AppUtils.default_deviceName);
            m_H1 = sharedpreferences.getString(AppUtils.PREF_H1_TITLE,AppUtils.default_H1);
            m_H2= sharedpreferences.getString(AppUtils.PREF_H2_TITLE,AppUtils.default_H2);
            m_H3 = sharedpreferences.getString(AppUtils.PREF_H3_TITLE,AppUtils.default_H3);
            m_recordLabelState = sharedpreferences.getBoolean(AppUtils.PREF_RECORD_LABELS_STATE, AppUtils.default_recordLabelState);
            m_indoorsState = sharedpreferences.getBoolean(AppUtils.PREF_INSIDE_STATE, AppUtils.default_indoorsState);
            m_scanning = sharedpreferences.getBoolean(AppUtils.PREF_STATE_IS_SCANNING,false);
        }
        else
        {
            Log.e(s_title,"SharedPrefs is null");
        }
    }

    private void setBGColor() {
        if(m_scanning){
            this.setBGColorGreen();
        }
        else{
            this.setBGColorWhite();
        }
    }


    private void setBGColorGreen() {
        ImageView bgnImage = (ImageView) findViewById(R.id.mainImageView);
        bgnImage.setImageResource(R.drawable.backgrndgreen);
    }

    private void setBGColorWhite() {
        ImageView bgnImage = (ImageView) findViewById(R.id.mainImageView);
        bgnImage.setImageResource(R.drawable.backgrndwhite);
    }

    private void setTextFields() {
        if(sharedpreferences!=null) {
            //String value = sharedpreferences.getString(PREF_DEVICE_TITLE, AppUtils.default_deviceName);
            ((EditText) findViewById(R.id.deviceIDEdit)).setText(m_deviceName);
            //value = sharedpreferences.getString(PREF_H1_TITLE, AppUtils.default_H1);
            ((EditText) findViewById(R.id.deviceH1Edit)).setText(m_H1);
            //value = sharedpreferences.getString(PREF_H2_TITLE, AppUtils.default_H2);
            ((EditText) findViewById(R.id.deviceH2Edit)).setText(m_H2);
            //value = sharedpreferences.getString(PREF_H3_TITLE, AppUtils.default_H3);
            ((EditText) findViewById(R.id.deviceH3Edit)).setText(m_H3);
            //boolean state = sharedpreferences.getBoolean(PREF_RECORD_LABELS_STATE, AppUtils.default_recordLabelState);
            cbkRecordLabels.setChecked(m_recordLabelState);
            cbkInsideBuildings.setChecked(m_indoorsState);
        }
        else
        {
            Log.e(s_title,"SharedPrefs is null");
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, R.string.menuStart);
        menu.add(0, 1, 0, R.string.menuStop);
        menu.add(0, 2, 0, R.string.mistake1);
        menu.add(0, 3, 0, R.string.mistake10);
        menu.add(0, 4, 0, R.string.mistake60);
        menu.add(0, 5, 0, R.string.mistake300);
        menu.add(0, 6, 0, R.string.mistake1440);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        switch (item.getItemId()) {
            case 0:
                AppUtils.writeToLog(this, commonConstants.logEvents.click,"menuStart","");
                if(!m_scanning){
                    Log.i(s_title, "Start pressed");
                    m_scanning = true;
                    editor.putBoolean(AppUtils.PREF_STATE_IS_SCANNING, m_scanning).apply();
                    setBGColor();
                    startScanServiceIfNeeded();
                }
                else
                {
                    Toast("Already on");
                }
                break;
            case 1:
                AppUtils.writeToLog(this, commonConstants.logEvents.click,"menuStop","");
                if(m_scanning) {
                    Log.i(s_title, "Stop pressed");
                    m_scanning = false;
                    editor.putBoolean(AppUtils.PREF_STATE_IS_SCANNING, m_scanning).apply();
                    setBGColor();
                    stopScanService();
                }
                else
                    Toast("Already off");
                break;
            case 2: // mistake 1 minute
                    callMistake(1);
                break;
            case 3: // mistake 10 minutes
                    callMistake(10);
                break;
            case 4: // mistake 1 hour
                    callMistake(60);
                break;
            case 5: // mistake 5 hours
                    callMistake(5*60);
                break;
            case 6: // mistake whole day
                    callMistake(24*60);
                break;
        }
        return super.onMenuItemSelected(featureId, item);
    }




    private void callMistake(int i) {
        AppUtils.writeToLog(this, commonConstants.logEvents.mistake,""+i,"");
    }


    public void Toast(String msg) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, msg, duration);
        toast.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppUtils.writeToLog(this, commonConstants.logEvents.appStateChanged,"onDestroy","");
    }

}
