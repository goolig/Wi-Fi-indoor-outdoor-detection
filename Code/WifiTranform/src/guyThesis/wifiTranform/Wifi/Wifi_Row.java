package guyThesis.wifiTranform.Wifi;

import guyThesis.Common.commonConstants;
import guyThesis.wifiTranform.Utils.majorityEvaluator;
import guyThesis.wifiTranform.graphWrapperManager;

import java.util.*;

/**
 * Created by shtar on 20/03/14.
 *
 */
public class Wifi_Row {

    public final long m_SerialNumber;

    //GPS
    /*
    private String GPSLatitude;
    private String GPSLongitude;
    private String GPSAltitude;
    private String GPSAccuracy;
    private String GPSProvider;
    private String GPSTime;
    private String GPSElapsedRealTimeNanos;
    private String isFromMockProvider;
    private String GPSSpeed;
    private String GPSHasSpeed;*/
    //GPS

    private boolean isSupervised;
    private boolean IndoorsState;
    private String H1;
    private String H2;
    private String H3;
    //private String id;
    private String device;
    private String BSSID;
    //private String SSID;
    /*String capabilities;
    String distanceCm;
    String distanceSdCm;
    String frequency;*/
    private int m_level;
    //String timestamp2;
//    private String tsf;
    //String wifiSsid_octets_buf;
    //String wifiSsid_octets_count;
    private Date timeStampDate;
    //private String m_timeStampString;
    private boolean m_mistake=false;

    public Wifi_Row(List<Wifi_Row> value) {
        m_SerialNumber= graphWrapperManager.m_dataStore.s_count++;
        graphWrapperManager.m_dataStore.s_WifiRowIDToWifiRow_Hash.put(m_SerialNumber, this);

        m_mistake=false;
        int count = 0;
        int sumLevel = 0;
        majorityEvaluator H1Eval = new majorityEvaluator();
        majorityEvaluator H2Eval = new majorityEvaluator();
        majorityEvaluator H3Eval = new majorityEvaluator();
        majorityEvaluator isSupervisedEval = new majorityEvaluator();
        majorityEvaluator IndoorsStateEval = new majorityEvaluator();

        Date MinDate = new Date();
        long minDateValue = Long.MAX_VALUE;
        for (Wifi_Row wifi_row : value) {
            if(wifi_row.m_mistake)
                m_mistake=true;
            device=wifi_row.getDevice();
            //m_timeStampString=wifi_row.getM_timeStampString();
            BSSID=wifi_row.getBSSID();
            //SSID=wifi_row.getSSID();
            count++;
            sumLevel+=wifi_row.m_level;
            H1Eval.addNewObs(wifi_row.getH1());
            H2Eval.addNewObs(wifi_row.getH2());
            H3Eval.addNewObs(wifi_row.getH3());
            isSupervisedEval.addNewObs("" + wifi_row.isSupervised());
            if(!wifi_row.getMistake())
                IndoorsStateEval.addNewObs("" + wifi_row.getIndoorsState());
            if(wifi_row.getTimeStampDate().getTime()<minDateValue)
            {
                minDateValue = wifi_row.getTimeStampDate().getTime();
                MinDate = wifi_row.getTimeStampDate();
            }
        }
        assert count!=0;
        m_level = (int)((sumLevel*1.0)/(count*1.0));
        H1 = H1Eval.getCommonValue();
        H2 = H2Eval.getCommonValue();
        H3=H3Eval.getCommonValue();
        isSupervised=isSupervisedEval.getCommonValue().equals("" + true);
        if(!m_mistake)
            IndoorsState=IndoorsStateEval.getCommonValue().equals("" + true);
        timeStampDate = MinDate;
        assert timeStampDate.before(new Date()); // before now
        assert timeStampDate.after(new Date(1396807839000l)); // after Mon, 06 Apr 2014 18:10:39 GMT
    }


    public String getDevice() {
        return device.trim().toLowerCase();
    }

    public String getBSSID() {
        assert BSSID!=null;
        return BSSID;
    }

//    public String getSSID() {
//        return SSID;
//    }

    /**
     *
     * @return the RSI power, normalized
     */
    public double getProperRSINormalized(boolean usePowerRatio){
        double ans;
        if(usePowerRatio)
            ans = getPowerRatio();
        else
            ans = m_level;
        ans = graphWrapperManager.m_dataStore.statistics.getNormalizedPowerValueForDevice(getDevice(), ans);
        return ans;
    }

    /*
     * @return the RSI power, not normalized
     */
    public double getProperRSIMeasure(boolean usePowerRatio){
        double ans;
        if(usePowerRatio)
            ans = getPowerRatio();
        else
            ans = m_level;
        return ans;
    }

    private double getPowerRatio() {
        double level;
        level = Math.pow(10, this.m_level / 10); //10^(A/10);
        return level;
    }

    private static int getParsedLevel(String level) {
        int ans = 0;
        try {
            ans = Integer.parseInt(level);
        } catch (Exception E) {
            System.out.println("Cant cast (to double): " + level);
        }
        return ans;
    }


    /**
     * This constructor is based on the application for scanningWifi
     *
     * @param CSVRow The csv line as string array - dividing the columns
     */
    public Wifi_Row(String[] CSVRow) {
        m_SerialNumber= graphWrapperManager.m_dataStore.s_count++;
        graphWrapperManager.m_dataStore.s_WifiRowIDToWifiRow_Hash.put(m_SerialNumber, this);
        try {
            m_mistake=false;
            // id  = "";// CSVRow[0]; never used
            device = CSVRow[0]; // the device name
            //timestamp = Double.parseDouble(CSVRow[2]);
            //m_timeStampString = CSVRow[1];
            BSSID = CSVRow[2].toLowerCase();
            //SSID = CSVRow[3];
            //capabilities = CSVRow[4];
            //distanceCm = "0";//CSVRow[6]; never used
            //distanceSdCm = "0";// CSVRow[7]; never used
            //frequency = CSVRow[5];
            m_level = getParsedLevel(CSVRow[6]);
            //timestamp2 = CSVRow[1]; // same as the first TM. this implementation does not contains 2
            //tsf = "";//CSVRow[11]; //never used
            // wifiSsid_octets_buf = ""; //= CSVRow[12]; never used
            //wifiSsid_octets_count = ""; //CSVRow[13]; never used

            H1 = CSVRow[7].toLowerCase().trim();
            H2 = CSVRow[8].toLowerCase().trim();
            H3 = CSVRow[9].toLowerCase().trim();
            isSupervised = CSVRow[10].equals("" + true);
            //System.out.println("Supervised: " + isSupervised);
            IndoorsState = CSVRow[11].equals("" + true);
            //System.out.println("Indoor: " + IndoorsState);
            timeStampDate = new Date(Long.parseLong(CSVRow[1].replace(".", "")) );
            assert timeStampDate.before(new Date()); // before now
            assert timeStampDate.after(new Date(1396807839000l)); // after Mon, 06 Apr 2014 18:10:39 GMT
            if (!this.isTheEmptyAp()) { // the empty AP should not appear as output
                if (!graphWrapperManager.m_dataStore.s_BSSIDtoBSSID_ID_HashSet.containsKey(getBSSID())) {
                    graphWrapperManager.m_dataStore.s_BSSIDtoBSSID_ID_HashSet.put(getBSSID(), graphWrapperManager.m_dataStore.s_BSSIDtoBSSID_ID_HashSet.size());
                    graphWrapperManager.m_dataStore.s_BSSID_IDtoBSSID_Hash.put(graphWrapperManager.m_dataStore.s_BSSID_IDtoBSSID_Hash.size(), getBSSID());
                }
            }
            else{
                if (!graphWrapperManager.m_dataStore.s_BSSIDtoBSSID_ID_HashSet.containsKey(commonConstants.emptyWifiBSSID)) {
                    graphWrapperManager.m_dataStore.s_BSSIDtoBSSID_ID_HashSet.put(getBSSID(), commonConstants.emptyWifiBSSID_ID);
                    graphWrapperManager.m_dataStore.s_BSSID_IDtoBSSID_Hash.put(commonConstants.emptyWifiBSSID_ID, getBSSID());
                }
            }
          /*  GPSLatitude = CSVRow[12];
            GPSLongitude = CSVRow[13];
            GPSAltitude = CSVRow[14];
            GPSAccuracy = CSVRow[15];
            GPSProvider = CSVRow[16];
            GPSTime = CSVRow[17];
            GPSElapsedRealTimeNanos = CSVRow[18];
            isFromMockProvider = CSVRow[19];
            GPSSpeed = CSVRow[20];
            GPSHasSpeed = CSVRow[21];*/
        }catch(Exception E){
            System.out.println("Error parsing file: " + E.toString());
        }
    }

    public String getConcatLocation() {
        String ans="";
        if(H1!=null) ans+=H1;
        ans += "-> ";
        if(H2!=null) ans+=H2;
        ans+="-> ";
        if(H3!=null) ans+=H3;
        return ans;
    }

    /**
     * returns true iff this wifi row is presenting an empty wifi scan.
     * its main use is to not add it to tht output as column, but add an row with all zeros.
     *
     * This function also tells the window manger not to add this row to the window.
     * @return true if this is the empty AP, false otherwise
     */

    public boolean isTheEmptyAp() {
        boolean ans;
        //ans = (commonConstants.emptyWifiBSSID.toLowerCase()).equals(this.getBSSID().toLowerCase()) && commonConstants.emptyWifiSSID.toLowerCase().equals(this.getSSID().toLowerCase());
        ans = (commonConstants.emptyWifiBSSID.toLowerCase()).equals(this.getBSSID().toLowerCase());
        return ans;
    }

    public String toString() {

        return "[" +
                ", Date=" + timeStampDate.getTime() +
                // "timestamp=" + getTimestamp() +
                ", BSSID=" + getBSSID() +

                ", m_level=" + m_level +
                ", power ratio=" + getPowerRatio() +

                "]";
    }

    public Date getTSDate() {

        return this.timeStampDate;

    }

    public int compareToByDate(Wifi_Row o2) {
        return o2.timeStampDate.compareTo(timeStampDate);
    }

    public String getH1() {
        return H1;
    }

    public String getH2() {
        return H2;
    }

    public String getH3() {
        return H3;
    }


    public boolean getIndoorsState() {
        assert !getMistake();

        return IndoorsState;
    }
    public boolean equalTime(Wifi_Row currWifiRow) {
        return this.getM_timeStampString().equals(currWifiRow.getM_timeStampString());
        //return currWifiRow.getTSDate().equals(this.getTSDate());
    }

    public String getGPSLatitude() {
        return "";// GPSLatitude;
    }

    public String getGPSLongitude() {
        return "";//GPSLongitude;
    }

    public Integer getBSSID_ID() {
        return graphWrapperManager.m_dataStore.s_BSSIDtoBSSID_ID_HashSet.get(getBSSID());
    }


    /**
     * Deprecated
     * @return nothing
     */
    public boolean isTrainRow() {
        return false;
    }

    public boolean isSupervised() {
        return isSupervised && !getMistake();
    }

    private boolean getMistake() {
        return m_mistake;
    }

    public void setMistake() {
        this.m_mistake = true;
    }

    public Date getTimeStampDate() {
        return timeStampDate;
    }

    public String getM_timeStampString() {
        return Long.toString(timeStampDate.getTime());
    }
}
