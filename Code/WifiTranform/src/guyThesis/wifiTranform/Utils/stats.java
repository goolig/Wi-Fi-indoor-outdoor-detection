package guyThesis.wifiTranform.Utils;

import guyThesis.wifiTranform.Wifi.Wifi_Row;
import guyThesis.wifiTranform.Wifi.Wifi_Window;
import guyThesis.wifiTranform.graphWifiNode;
import guyThesis.wifiTranform.graphWrapperManager;
import guyThesis.wifiTranform.mainTransform;

import java.util.HashMap;

/**
 * Created by Guy on 02/04/2015.
 * collects and prints stats
 */
public class stats {

    private int m_indoorRow =0;
    private int m_outdoorRow=0;
    private int m_supervisedRow=0;
    private int m_unsupervisedRow=0;
    private int m_totalRows;
    private int m_supervisedWin=0;
    private int m_unsupervisedWin=0;
    private int m_indoorWin=0;
    private int m_outdoorWin=0;
    private int m_totalWin=0;

    private long m_sumPower=0;
    private int m_nodesCount=0;
    private int m_nodeWinCount=0;
    private int m_nodeWinMax =0;
    private int m_nodeWinMin =Integer.MAX_VALUE;

    double m_minLengthWin=Double.MAX_VALUE;
    double m_maxLengthWin=0;
    double m_sumLengthWin=0;

    double m_minAPWin=Double.MAX_VALUE;
    double m_maxAPWin=0;
    double m_sumAPWin=0;

    double m_emptyWinCount=0;


    private HashMap<String,Double> m_devicePowerSumRow;
    private HashMap<String,Double> m_devicePowerSumSquareRow;
    private HashMap<String,Long> m_devicePowerCountRow;

    private HashMap<String,Long> m_deviceSupervisedCountRow;
    private HashMap<String,Long> m_deviceUnsupervisedCountRow;
    private HashMap<String,Long> m_deviceEmptyRowsCountRow;

    private HashMap<String,Double> m_devicePowerSumWin;
    private HashMap<String,Double> m_devicePowerSumSquareWin;
    private HashMap<String,Long> m_devicePowerCountWin;

    private HashMap<String,Double> m_deviceCountSumWin;
    private HashMap<String,Double> m_deviceCountSumSquareWin;
    private HashMap<String,Long> m_deviceCountCountWin;

    private HashMap<String,Long> m_deviceSupervisedCountWin;
    private HashMap<String,Long> m_deviceUnsupervisedCountWin;
    private HashMap<String,Long> m_deviceEmptyRowsCountWin;

    private HashMap<String,Long> m_deviceIndoorCountWin;
    private HashMap<String,Long> m_deviceOutdoorCountWin;

    private HashMap<String,Long> m_deviceFingerprintsInWindowCount;
    private HashMap<String,Long> m_deviceFingerprintsInWindowSum;


    public stats() {
        m_devicePowerSumRow = new HashMap<String, Double>();
        m_devicePowerCountRow = new HashMap<String, Long>();
        m_devicePowerSumSquareRow = new HashMap<String, Double>();
        m_deviceSupervisedCountRow = new HashMap<String, Long>();
        m_deviceUnsupervisedCountRow = new HashMap<String, Long>();
        m_deviceEmptyRowsCountRow = new HashMap<String, Long>();

        m_devicePowerSumWin= new HashMap<String, Double>();
        m_devicePowerCountWin= new HashMap<String, Long>();
        m_devicePowerSumSquareWin= new HashMap<String, Double>();
        m_deviceSupervisedCountWin= new HashMap<String, Long>();
        m_deviceUnsupervisedCountWin= new HashMap<String, Long>();
        m_deviceEmptyRowsCountWin= new HashMap<String, Long>();
        m_deviceCountSumWin = new HashMap<String, Double>();
        m_deviceCountSumSquareWin = new HashMap<String, Double>();
        m_deviceCountCountWin = new HashMap<String, Long>();

        m_deviceIndoorCountWin= new HashMap<String, Long>();
        m_deviceOutdoorCountWin= new HashMap<String, Long>();

        m_deviceFingerprintsInWindowCount= new HashMap<String, Long>();
        m_deviceFingerprintsInWindowSum= new HashMap<String, Long>();

    }

    public void considerRow(Wifi_Row row){
        if(row.isSupervised()&&row.getIndoorsState()) m_indoorRow++;
        if(row.isSupervised()&&!row.getIndoorsState()) m_outdoorRow++;
        if(row.isSupervised()) m_supervisedRow++;
        else
            m_unsupervisedRow++;

        initDeviceRowHashIfNeeded(row);

        if(row.isSupervised())
            m_deviceSupervisedCountRow.put(row.getDevice(), m_deviceSupervisedCountRow.get(row.getDevice()) + 1);
        else
            m_deviceUnsupervisedCountRow.put(row.getDevice(), m_deviceUnsupervisedCountRow.get(row.getDevice()) + 1);

        if(!row.isTheEmptyAp()) {
            m_totalRows++;
            m_sumPower += row.getProperRSIMeasure(graphWrapperManager.m_dataStore.usePowerRatio);
            m_devicePowerCountRow.put(row.getDevice(), m_devicePowerCountRow.get(row.getDevice()) + 1);
            m_devicePowerSumRow.put(row.getDevice(), m_devicePowerSumRow.get(row.getDevice()) + row.getProperRSIMeasure(graphWrapperManager.m_dataStore.usePowerRatio));
            m_devicePowerSumSquareRow.put(row.getDevice(), (m_devicePowerSumSquareRow.get(row.getDevice()) + Math.pow(row.getProperRSIMeasure(graphWrapperManager.m_dataStore.usePowerRatio), 2)));
        }
        else
            m_deviceEmptyRowsCountRow.put(row.getDevice(), m_deviceEmptyRowsCountRow.get(row.getDevice()) + 1);

    }

    private void initDeviceRowHashIfNeeded(Wifi_Row row) {
        if(!m_devicePowerSumRow.containsKey(row.getDevice()))
            m_devicePowerSumRow.put(row.getDevice(), (double) 0);
        if(!m_devicePowerCountRow.containsKey(row.getDevice()))
            m_devicePowerCountRow.put(row.getDevice(), (long) 0);
        if(!m_devicePowerSumSquareRow.containsKey(row.getDevice()))
            m_devicePowerSumSquareRow.put(row.getDevice(), (double) 0);
        if(!m_deviceSupervisedCountRow.containsKey(row.getDevice()))
            m_deviceSupervisedCountRow.put(row.getDevice(), (long) 0);
        if(!m_deviceUnsupervisedCountRow.containsKey(row.getDevice()))
            m_deviceUnsupervisedCountRow.put(row.getDevice(), (long) 0);
        if(!m_deviceEmptyRowsCountRow.containsKey(row.getDevice()))
            m_deviceEmptyRowsCountRow.put(row.getDevice(),(long)0);
    }

    private void initDeviceWinHashIfNeeded(Wifi_Window win) {
        if(!m_deviceCountCountWin.containsKey(win.getDevice()))
            m_deviceCountCountWin.put(win.getDevice(), (long) 0);
        if(!m_deviceCountSumSquareWin.containsKey(win.getDevice()))
            m_deviceCountSumSquareWin.put(win.getDevice(), (double) 0);
        if(!m_deviceCountSumWin.containsKey(win.getDevice()))
            m_deviceCountSumWin.put(win.getDevice(), (double) 0);

        if(!m_devicePowerSumWin.containsKey(win.getDevice()))
            m_devicePowerSumWin.put(win.getDevice(), (double) 0);
        if(!m_devicePowerCountWin.containsKey(win.getDevice()))
            m_devicePowerCountWin.put(win.getDevice(), (long) 0);
        if(!m_devicePowerSumSquareWin.containsKey(win.getDevice()))
            m_devicePowerSumSquareWin.put(win.getDevice(), (double) 0);
        if(!m_deviceSupervisedCountWin.containsKey(win.getDevice()))
            m_deviceSupervisedCountWin.put(win.getDevice(), (long) 0);
        if(!m_deviceUnsupervisedCountWin.containsKey(win.getDevice()))
            m_deviceUnsupervisedCountWin.put(win.getDevice(), (long) 0);
        if(!m_deviceEmptyRowsCountWin.containsKey(win.getDevice()))
            m_deviceEmptyRowsCountWin.put(win.getDevice(),(long)0);
        if(!m_deviceIndoorCountWin.containsKey(win.getDevice()))
            m_deviceIndoorCountWin.put(win.getDevice(),(long)0);
        if(!m_deviceOutdoorCountWin.containsKey(win.getDevice()))
            m_deviceOutdoorCountWin.put(win.getDevice(),(long)0);

        if(!m_deviceFingerprintsInWindowCount.containsKey(win.getDevice()))
            m_deviceFingerprintsInWindowCount.put(win.getDevice(),(long)0);
        if(!m_deviceFingerprintsInWindowSum.containsKey(win.getDevice()))
            m_deviceFingerprintsInWindowSum.put(win.getDevice(),(long)0);
    }
    
    private double getVarianceRowPowerForDevice(String deviceName){
        return m_devicePowerSumSquareRow.get(deviceName)/ m_devicePowerCountRow.get(deviceName)  - Math.pow(m_devicePowerSumRow.get(deviceName) / m_devicePowerCountRow.get(deviceName), 2);
    }
    private double getAverageRowPowerForDevice(String deviceName){
        return m_devicePowerSumRow.get(deviceName)/ m_devicePowerCountRow.get(deviceName);
    }

    public double getNormalizedPowerValueForDevice(String deviceName, double value){
        double ans =(value - getAverageRowPowerForDevice(deviceName));
        if (getVarianceRowPowerForDevice(deviceName)!=0)
            ans/= Math.sqrt(getVarianceRowPowerForDevice(deviceName));
        return ans;
    }

    private double getVarianceCountForDevice(String deviceName){
        return m_deviceCountSumSquareWin.get(deviceName)/ m_deviceCountCountWin.get(deviceName)  - Math.pow(m_deviceCountSumWin.get(deviceName) / m_deviceCountCountWin.get(deviceName), 2);
    }
    private double getAverageCountForDevice(String deviceName){
        return m_deviceCountSumWin.get(deviceName)/ m_deviceCountCountWin.get(deviceName);
    }

    public double getNormalizedCountValueForDevice(String deviceName, int value){
        double ans =(value - getAverageCountForDevice(deviceName));
        if (getVarianceCountForDevice(deviceName)!=0)
            ans/= Math.sqrt(getVarianceCountForDevice(deviceName));
        return ans;
    }


    public void considerWindow(Wifi_Window win){
        initDeviceWinHashIfNeeded(win);

        m_deviceFingerprintsInWindowCount.put(win.getDevice(), m_deviceFingerprintsInWindowCount.get(win.getDevice()) + 1);
        m_deviceFingerprintsInWindowSum.put(win.getDevice(),m_deviceFingerprintsInWindowSum.get(win.getDevice()) + win.getNumberOfFingerprints());

        if(win.isSupervised()) {
            m_deviceSupervisedCountWin.put(win.getDevice(), m_deviceSupervisedCountWin.get(win.getDevice()) + 1);
            m_supervisedWin++;
            if(win.getMajorityIndoorsStateBoolean()) {
                m_indoorWin++;
                m_deviceIndoorCountWin.put(win.getDevice(), m_deviceIndoorCountWin.get(win.getDevice()) + 1);

            }
            else {
                m_deviceOutdoorCountWin.put(win.getDevice(), m_deviceOutdoorCountWin.get(win.getDevice()) + 1);
                m_outdoorWin++;
            }
        }
        else {
            m_unsupervisedWin++;
            m_deviceUnsupervisedCountWin.put(win.getDevice(), m_deviceUnsupervisedCountWin.get(win.getDevice()) + 1);
        }
        m_totalWin++;
        m_sumLengthWin+=win.getTimeLength();
        m_minLengthWin = Math.min(win.getTimeLength(),m_minLengthWin);
        m_maxLengthWin = Math.max(win.getTimeLength(), m_maxLengthWin);

        m_sumAPWin+=win.numOfApInWin();
        m_minAPWin=Math.min(win.numOfApInWin(),m_minAPWin);
        m_maxAPWin=Math.max(win.numOfApInWin(), m_maxAPWin);
        if(win.numOfApInWin()==0) m_emptyWinCount++;
        

        if(!win.isEmptyWindow()) {

            m_devicePowerCountWin.put(win.getDevice(), m_devicePowerCountWin.get(win.getDevice()) + 1);
            m_devicePowerSumWin.put(win.getDevice(), m_devicePowerSumWin.get(win.getDevice()) + win.getAverageApPower(false,graphWrapperManager.m_dataStore.usePowerRatio));
            m_devicePowerSumSquareWin.put(win.getDevice(), (m_devicePowerSumSquareWin.get(win.getDevice()) + Math.pow(win.getAverageApPower(false,graphWrapperManager.m_dataStore.usePowerRatio), 2)));

            m_deviceCountCountWin.put(win.getDevice(), m_deviceCountCountWin.get(win.getDevice()) + 1);
            m_deviceCountSumWin.put(win.getDevice(), m_deviceCountSumWin.get(win.getDevice()) + win.numOfApInWin());
            m_deviceCountSumSquareWin.put(win.getDevice(), (m_deviceCountSumSquareWin.get(win.getDevice()) + Math.pow(win.numOfApInWin(), 2)));
        }
        else
            m_deviceEmptyRowsCountWin.put(win.getDevice(), m_deviceEmptyRowsCountWin.get(win.getDevice()) + 1);


    }

    public void considerGraphNode(graphWifiNode currNode){
        m_nodesCount++;
        m_nodeWinCount+=currNode.getNumOfWindow(true);
        m_nodeWinMax = Math.max(currNode.getNumOfWindow(true), m_nodeWinMax);
        m_nodeWinMin = Math.min(currNode.getNumOfWindow(true), m_nodeWinMin);
    }

    public double getTotalWinForDevice(String device){
        return (m_deviceUnsupervisedCountWin.get(device) +  m_deviceSupervisedCountWin.get(device));
    }

    public double getAverageFingerprintsPerWindowForDevice(String device){
        if(m_deviceFingerprintsInWindowCount.get(device)!=0)
            return (m_deviceFingerprintsInWindowSum.get(device)*1.0)/(m_deviceFingerprintsInWindowCount.get(device)*1.0);
        return 0;
    }

    public void printRowAndWinStats(){
        String result ="";
        result+=("----------- Overall Rows: ------------" +'\n');
        result+=("Indoor rows count : " + m_indoorRow +'\n');
        result+=("outdoor rows count : " + m_outdoorRow +'\n' );
        result+=("Supervised rows count : " + m_supervisedRow +'\n');
        result+=("unsupervised rows count : " + m_unsupervisedRow +'\n');
        result+=("Total non empty rows : " + m_totalRows+'\n');
        result+=("Average power in non empty rows : " +m_sumPower*1.0 / m_totalRows+'\n');
        result+=("----------- Overall Windows: ------------" +'\n');
        result+=("Indoor win  count : " + m_indoorWin +'\n');
        result+=("outdoor win count : " + m_outdoorWin +'\n' );
        result+=("Supervised win count : " + m_supervisedWin +'\n');
        result+=("unsupervised win count : " + m_unsupervisedWin +'\n');
        result+=("Total win : " + m_totalWin+'\n');
        result+=("minWinLength : " + m_minLengthWin +'\n');
        result+=("maxWinLength : " + m_maxLengthWin +'\n');
        result+=("Average Win Length : " + m_sumLengthWin*1.0 / m_totalWin+'\n');
        result+=("empty win count : " + m_emptyWinCount +'\n');
        result+=("minWinAp : " + m_minAPWin +'\n');
        result+=("maxWinAp : " + m_maxAPWin +'\n');
        result+=("Average Win AP count : " + m_sumAPWin*1.0 / m_totalWin+'\n');

        for (String s : m_devicePowerCountRow.keySet()) {
            result+=("---------- Device: " + s +" -----------" +'\n');
            result+=("----------- Rows: ------------" +'\n');
            result+=("Average power for device rows: " + s+" is :" + getAverageRowPowerForDevice(s) +" " +'\n');
            result+=("Variance power for device rows: " + s+" is :" + getVarianceRowPowerForDevice(s) +" " +'\n');
            result+=("Count non empty rows for device: " + s+" is :" + m_devicePowerCountRow.get(s) + " " +'\n');
            result+=("Count empty rows for device: " + s+" is :" + m_deviceEmptyRowsCountRow.get(s) + " " +'\n');
            result+=("Count Supervised rows for device: " + s+" is :" + m_deviceSupervisedCountRow.get(s) + " " +'\n');
            result+=("Count Unsupervised rows for device: " + s+" is :" + m_deviceUnsupervisedCountRow.get(s) + " " +'\n');
            result+=("Total rows for device: " + s+" is :" + (m_deviceUnsupervisedCountRow.get(s)+m_deviceSupervisedCountRow.get(s)) + " " +'\n');

            result+=("----------- Windows: ------------" +'\n');
            //result+=("Average power for device win: " + s+" is :" + getAverageRowPowerForDevice(s) +" " +'\n');
            //result+=("Variance power for device win: " + s+" is :" + getVarianceRowPowerForDevice(s) +" " +'\n');
            result+=("Count non empty win for device: " + s+" is :" + m_devicePowerCountWin.get(s) + " " +'\n');
            result+=("Count empty win for device: " + s+" is :" + m_deviceEmptyRowsCountWin.get(s) + " " +'\n');
            result+=("Count Supervised win for device: " + s+" is :" + m_deviceSupervisedCountWin.get(s) + " " +'\n');
            result+=("Count Unsupervised win for device: " + s+" is :" + m_deviceUnsupervisedCountWin.get(s) + " " +'\n');

            result+=("Total win for device: " + s+" is :" + (m_deviceUnsupervisedCountWin.get(s) +  m_deviceSupervisedCountWin.get(s)) + " " +'\n');

            result+=("Count Indoor win for device: " + s+" is :" + m_deviceIndoorCountWin.get(s) + " " +'\n');
            result+=("Count Outdoor win for device: " + s+" is :" + m_deviceOutdoorCountWin.get(s) + " " +'\n');


            result+=("Average fingerprints per window for device: " + s+" is :" + getAverageFingerprintsPerWindowForDevice(s) + " " +'\n');

        }
        mainTransform.log.fine(result);
    }

    public void printGraphNodesStats() {
        String result ="";
        result+=("graph node count : " + m_nodesCount +'\n');
        result+=("graph node average win count : " + m_nodeWinCount*1.0 / m_nodesCount  +'\n');
        result+=("min win in node : " + m_nodeWinMin +'\n');
        result+=("max win in node : " + m_nodeWinMax +'\n');


        mainTransform.log.fine(result);

    }
}
