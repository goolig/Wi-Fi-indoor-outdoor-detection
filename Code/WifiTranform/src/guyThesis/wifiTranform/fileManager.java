package guyThesis.wifiTranform;

import guyThesis.Common.commonConstants;
import guyThesis.wifiTranform.Wifi.Wifi_Row;
import guyThesis.wifiTranform.Wifi.Wifi_Window;
import guyThesis.wifiTranform.Wifi.Wifi_Window_Container;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Guy on 27/01/2015.
 * This class manages the whole experiment process.
 * it reads and write files, execute the calculations.
 */
public abstract class fileManager {
    private static final Logger log = mainTransform.log;//Logger.getLogger(fileManager.class.getName());
    /**
     * Holds all the window containers.
     * usually the size of the list is the amount of files
     */
    public List<Wifi_Window_Container> s_windowsManagerList;



    public fileManager(){
        //commonConstants.setLoggerThings(log);
        s_windowsManagerList = new LinkedList<Wifi_Window_Container>();
    }


    /**
     * Adds this window to the list of windows managed by this object
     * @param win the window container to be added to the list
     */
    public void addWindowCreatorToList(Wifi_Window_Container win){
        s_windowsManagerList.add(win);
    }


    /**
     * Iterate through the folder recursively and read the files.
     * @param folder the input folder
     */
    public void createWindowsForInputRecursion(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                createWindowsForInputRecursion(fileEntry);
            } else {
                readDataFromFile(fileEntry.getAbsolutePath());
            }
        }


    }

    /**
     * Read file from the given folders, creates wifi windows from them
     * @param folder the input folder
     */
    public void createWindowsForInput(final File folder){
        createWindowsForInputRecursion(folder);
        createWindows();
        createAPtoWifiWindowsTable();
        PrintStatsAllFiles();

    }

    private void createWindows() {
        for (Wifi_Window_Container wifi_window_container : this.s_windowsManagerList) {
            wifi_window_container.CreateWindows();
        }
    }

    /**
     * Creates hash tables to improve the runtime
     * this is used to find the potential neighbors later
     */
    protected void createAPtoWifiWindowsTable(){
        log.fine("Creating AP to Windows Hash Table");
        for (String device : graphWrapperManager.m_dataStore.s_deviceSet) {
            HashMap<Integer, Set<Integer>> currentMap = new HashMap<Integer, Set<Integer>>();
            for (Integer wifiID : graphWrapperManager.m_dataStore.s_BSSID_IDtoBSSID_Hash.keySet()) {
                currentMap.put(wifiID,new HashSet<Integer>());
            }
            graphWrapperManager.m_dataStore.s_AP_IDtoWindowList_Hash.put(device, currentMap);
        }




        for (Wifi_Window_Container wifi_window_container : s_windowsManagerList) {
            for (Wifi_Window wifi_window : wifi_window_container.getWifiWindowList()) {
                HashMap<Integer, Set<Integer>> hashMap = graphWrapperManager.m_dataStore.s_AP_IDtoWindowList_Hash.get(wifi_window.getDevice());
                for (Integer ApID : wifi_window.getBSSID_ID_SetForWindow()) {

                    Set<Integer> currWinList = hashMap.get(ApID);
                    currWinList.add(wifi_window.getM_ID());
                }
                if(wifi_window.hasEmptyRow()){
                    hashMap.get(commonConstants.emptyWifiBSSID_ID).add(wifi_window.getM_ID());
                }
            }
        }
//        Double sum=0d;
//        for (Integer wifiID : graphWrapperManager.m_dataStore.s_BSSID_IDtoBSSID_Hash.keySet()) {
//            sum+= graphWrapperManager.m_dataStore.s_AP_IDtoWindowList_Hash.get(wifiID).size();
//        }
//        log.fine("Done Creating AP to Window Hash Table. Average amount of windows to AP: "+ sum/ graphWrapperManager.m_dataStore.s_AP_IDtoWindowList_Hash.size());
    log.fine("Done Creating AP to Window Hash Table. Average amount of windows to AP: ");

    }


    public Iterator<?> getIteratorForWindowsList() {
        return this.s_windowsManagerList.iterator();
    }

    /**
     * Reads the data stored in the paramter, and adds it to this wifi manager object
     * @param scanFile the path of the file to read
     */
    public void readDataFromFile(String scanFile) {
        String csvExtension = ".csv";
        String logFileExtension = "Log";
        assert scanFile.toLowerCase().endsWith(csvExtension);

        graphWrapperManager.m_dataStore.currFileName=Paths.get(scanFile).getFileName().toString();

        if(scanFile.endsWith(logFileExtension + csvExtension)) // dont read log files
            return;

        String logFilePath = scanFile.substring(0,scanFile.length()-csvExtension.length()) + logFileExtension + csvExtension;
        LLogFileManager logManager = null;
        if(new File(logFilePath).exists()){
            log.log(Level.FINE,"file: " + scanFile+ " have a log file");
            logManager = new LLogFileManager(logFilePath);
        }
        else {
            log.log(Level.FINE, "file: " + scanFile + " does not have a log file");
        }
        graphWrapperManager.m_dataStore.s_numOfFiles++;
        addWindowCreatorToList(new Wifi_Window_Container(ReadCSVtoWifiRowList(scanFile),logManager)); // this reads the entire input file into an object, which will later create windows
        log.log(Level.FINER, "Read file: {0}", scanFile);
    }


    public static List<Wifi_Row> ReadCSVtoWifiRowList(String filePath) {
        BufferedReader br = null;
        String line ;
        String cvsSplitBy = commonConstants.columnSeparator;
        List<Wifi_Row> WifiRowsList = new ArrayList<Wifi_Row>();
        try {

            br = new BufferedReader(new FileReader(filePath));
            //skip first line
            br.readLine();
            while ((line = br.readLine()) != null) {

                // use columnSeparator as separator
                String[] CSVRow = line.split(cvsSplitBy);
                if (CSVRow.length > 1) {

                    Wifi_Row currRow = new Wifi_Row(CSVRow);
                    WifiRowsList.add(currRow);
                    graphWrapperManager.m_dataStore.s_deviceSet.add(currRow.getDevice());
                    if(currRow.isSupervised())
                        graphWrapperManager.m_dataStore.s_locationSet.add(currRow.getConcatLocation());

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.log(Level.FINEST, "Cannot read file!!!! is it opened??");
            System.exit(1);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        printStatsForFile(WifiRowsList);
        return WifiRowsList;

    }

    private void PrintStatsAllFiles(){
        log.fine("------- Final Stats ------");
        log.fine("Location List: " + graphWrapperManager.m_dataStore.s_locationSet.toString());
        log.fine("Device List: " + graphWrapperManager.m_dataStore.s_deviceSet.toString());
/*
        log.fine("Train List: " + graphWrapperManager.m_dataStore.TrainLocationList.toString());
        List<String> testList = new ArrayList<String>(graphWrapperManager.m_dataStore.s_locationSet);
        testList.removeAll(graphWrapperManager.m_dataStore.TrainLocationList);
        log.fine("Test List: " + testList.toString());
*/
        graphWrapperManager.m_dataStore.statistics.printRowAndWinStats();
        log.fine("Number of files: " + graphWrapperManager.m_dataStore.s_numOfFiles);
    }

    private static void printStatsForFile(List<Wifi_Row> wifiRowsList) {
        log.fine("Unique BSSID List Size:" + graphWrapperManager.m_dataStore.s_BSSIDtoBSSID_ID_HashSet.size());
        log.fine("Row List Size: " + wifiRowsList.size());
        if(wifiRowsList.size()>0){
            log.fine("First scan (GMT Time): " + wifiRowsList.get(0).getTSDate().toString());
            log.fine("Last scan: (GMT Time): " + wifiRowsList.get(wifiRowsList.size() - 1).getTSDate().toString());
        }
        log.fine("Location List: " + graphWrapperManager.m_dataStore.s_locationSet.toString());
        log.fine("Device List: " + graphWrapperManager.m_dataStore.s_deviceSet.toString());

    }


    public void writeOutFile(BufferedWriter writer) {
        Iterator<?> iterator = getIteratorForWindowsList();
        while (iterator.hasNext()){
            Wifi_Window_Container window = (Wifi_Window_Container)iterator.next();
            try {
                window.WriteWindowsToCSV(writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void CloseFile(BufferedWriter writer) throws IOException {
        writer.flush();
        writer.close();
    }

    public static BufferedWriter CreateOutFile() {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(commonConstants.OutTransformedFilePath), "UTF8"
            ));
            out.write(commonConstants.filetitleH1);
            out.write(commonConstants.columnSeparator);
            out.write(commonConstants.filetitleH2);
            out.write(commonConstants.columnSeparator);
            out.write(commonConstants.filetitleH3);
            out.write(commonConstants.columnSeparator);
            out.write(commonConstants.filetitleRecordLabelsState);
            out.write(commonConstants.columnSeparator);
            out.write(commonConstants.filetitleInsideState);
            out.write(commonConstants.columnSeparator);
            out.write(commonConstants.filetitleTS);

            //GPS part
            out.write(commonConstants.columnSeparator);
            out.write(commonConstants.filetitleGPSLatitude);
            out.write(commonConstants.columnSeparator);
            out.write(commonConstants.filetitleGPSLongitude);

            for (int i = 0; i < graphWrapperManager.m_dataStore.s_BSSIDtoBSSID_ID_HashSet.size(); i++) {
                out.write(commonConstants.columnSeparator);
                out.write(graphWrapperManager.m_dataStore.s_BSSID_IDtoBSSID_Hash.get(i));
            }
            out.write(commonConstants.columnSeparator);
            out.write(commonConstants.filetitleNumOfVisibleAp);
            out.write(System.getProperty("line.separator"));
        } catch (IOException e) {
            e.printStackTrace();
            log.log(Level.FINEST, "Cannot write to out file!!!! is it opened??");
            System.exit(1);
        }
        return out;
    }

    /**
     * Create output file, write the Data and closes it.
     * Deprecated
     */
    public void CreateWriteCloseOut() {
        log.fine("writing out file");
        BufferedWriter writer = CreateOutFile();
        writeOutFile(writer);
        try {
            CloseFile(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.fine("Done writing out file");
    }

}
