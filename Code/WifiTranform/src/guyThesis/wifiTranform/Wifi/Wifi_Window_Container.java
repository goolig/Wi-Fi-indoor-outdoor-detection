package guyThesis.wifiTranform.Wifi;

import guyThesis.Common.commonConstants;
import guyThesis.wifiTranform.graphWrapperManager;
import guyThesis.wifiTranform.LLogFileManager;
import guyThesis.wifiTranform.mainTransform;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by shtar on 01/04/14.
 *
 */
public class Wifi_Window_Container {

    public List<Wifi_Window> getWifiWindowList() {
        return m_WifiWindowList;
    }

    List<Wifi_Window> m_WifiWindowList; // hold list for sorted windows

    List<Wifi_Row> m_wifiRowList;

    /**
     *  Constructor, Sort Wifi Rows by date from earlier to later
     *  actually, they are suppose to be in the right order,
     *  but we cannot relay on the collection app.
     */
    public Wifi_Window_Container(List<Wifi_Row> wifiRowList, LLogFileManager logManager) {
        m_wifiRowList=wifiRowList;
        Collections.sort(m_wifiRowList, new Comparator<Wifi_Row>() {
            @Override
            public int compare(Wifi_Row o1, Wifi_Row o2) {
                return (o2).compareToByDate(o1);
            }
        });
        if(logManager!=null) {
            for (Wifi_Row aM_WifiRowList : m_wifiRowList) {
                logManager.setMistakeIfNeededForRow(aM_WifiRowList); // must run by the right order by time.
            }
        }
        m_WifiWindowList = new ArrayList<Wifi_Window>();
        calculateStatsForAllRows(m_wifiRowList);
    }

    /**
     * Creates from the wifi list object wifiWindows list.
     */
    public void CreateWindows() {
        Wifi_Row lastWifiRow = null;
        Wifi_Window currWindow;
        for (int i = 0; i < m_wifiRowList.size(); i++) {
            Wifi_Row currWifiRow = m_wifiRowList.get(i);
            //Start a new window in the first row || (if its not the same AP as last one (because in some scan implementation
            // there are more than 1 appearance of AP in a scan) & the the time of this scan is different than the last one.
            //this is because the smallest element is a scan - which happens at a giver moment, and not an AP (in most devices)
            if (lastWifiRow == null ||
                    (!lastWifiRow.equals(currWifiRow) && !lastWifiRow.equalTime(currWifiRow))
                    ) {
                //Start a new window
                //System.out.println("NEW window");
                currWindow = new Wifi_Window();
                int scansLeft=graphWrapperManager.m_dataStore.windowsSizeLimitScans;
                String lastTime = m_wifiRowList.get(i).getM_timeStampString();

                Date windowStartTime = m_wifiRowList.get(i).getTimeStampDate();
                //add window to list
                // add all rows to this window
                int j = i;
                for (;
                     j < m_wifiRowList.size() &&
                              scansLeft > 0; j++) { // m_wifiRowList.get(j).getTimeStampDate().getTime() - m_wifiRowList.get(i).getTimeStampDate().getTime() <= graphWrapperManager.m_dataStore.windowsSizeLimitSeconds * 1000 &&
                    if(m_wifiRowList.get(j).getTimeStampDate().getTime() - windowStartTime.getTime() > graphWrapperManager.m_dataStore.windowsSizeLimitSeconds*1000)
                    {
                        break;
                    }

                    if (!lastTime.equals(m_wifiRowList.get(j).getM_timeStampString())) {
                        scansLeft--;
                        currWindow.increaseFingerpritCount();
                    }

                    if (scansLeft > 0) {
                        assert m_wifiRowList.get(j).getTimeStampDate().getTime() - m_wifiRowList.get(i).getTimeStampDate().getTime() >= 0;
                        //add this row to the window
                        currWindow.considerNextRow(m_wifiRowList.get(j));
                    if(j==m_wifiRowList.size()-1)
                        scansLeft--;
                    }
                    lastTime = m_wifiRowList.get(j).getM_timeStampString();


                }
                //if(scansLeft==0)
                if(true)
                {
                    m_WifiWindowList.add(currWindow);
                    graphWrapperManager.m_dataStore.statistics.considerWindow(currWindow);
                    currWindow.removeDuplicatedWifiRowsAndCreateRankHash();
                }
                else
                    currWindow.isGarbage();

            }
            lastWifiRow = currWifiRow;
        }
        mainTransform.log.fine("Windows in list: " + m_WifiWindowList.size());
        m_wifiRowList=null; //not needed any more
    }

    /**
     * writes the contained wifi windows to the given buffered writer
     * @param writer writer object to write into
     * @throws IOException
     */
    public void WriteWindowsToCSV(BufferedWriter writer) throws IOException {
        for (Wifi_Window currWindow : m_WifiWindowList) {
            HashMap<String, Double> currWinSum = currWindow.getWindowRSI();
            //writer.append("a"+ Wifi_Window_Manager.s_ID);
            writer.append(currWindow.getMajorityH1());
            writer.append(commonConstants.columnSeparator);
            writer.append(currWindow.getMajorityH2());
            writer.append(commonConstants.columnSeparator);
            writer.append(currWindow.getMajorityH3());
            writer.append(commonConstants.columnSeparator);
            writer.append("" + currWindow.isSupervised());
            writer.append(commonConstants.columnSeparator);
            writer.append(currWindow.getMajorityIndoorsState());
            writer.append(commonConstants.columnSeparator);
            writer.append(currWindow.getStartTimeStamp());
            writer.append(commonConstants.columnSeparator);
            writer.append(currWindow.getLon());
            writer.append(commonConstants.columnSeparator);
            writer.append(currWindow.getLat());

            int numOfVisibleAps = 0;
            for (int i = 0; i < graphWrapperManager.m_dataStore.s_BSSIDtoBSSID_ID_HashSet.size(); i++) {
                //TODO: remove APs with low visibility
                assert (!graphWrapperManager.m_dataStore.s_BSSID_IDtoBSSID_Hash.get(i).equals(commonConstants.emptyWifiBSSID)); // the empty Ap should not appear in the file
                writer.append(commonConstants.columnSeparator);
                writer.append(currWinSum.get(graphWrapperManager.m_dataStore.s_BSSID_IDtoBSSID_Hash.get(i)).toString());
                if (currWinSum.get(graphWrapperManager.m_dataStore.s_BSSID_IDtoBSSID_Hash.get(i))!= 0) numOfVisibleAps++;
            }
            writer.append(commonConstants.columnSeparator);
            writer.append(Integer.toString(numOfVisibleAps));
            writer.append(System.getProperty("line.separator"));
        }
    }

    /**
     * adds all of the rows to the statistics
     * @param wifiRowList
     */
    private void calculateStatsForAllRows(List<Wifi_Row> wifiRowList) {
        assert wifiRowList!=null;
        for (Wifi_Row wifi_row : wifiRowList) {
            graphWrapperManager.m_dataStore.statistics.considerRow(wifi_row);
        }
    }
}
