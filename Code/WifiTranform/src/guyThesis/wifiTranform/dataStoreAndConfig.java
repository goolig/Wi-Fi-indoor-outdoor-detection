package guyThesis.wifiTranform;

import guyThesis.Common.commonConstants;
import guyThesis.wifiTranform.Utils.stats;
import guyThesis.wifiTranform.Wifi.Wifi_Row;
import guyThesis.wifiTranform.Wifi.Wifi_Window;

import java.io.File;
import java.util.*;

/**
 * Created by Guy on 23/03/2015.
 *
 */
public class dataStoreAndConfig {

    /**
     * if set to true, will write the graph as CSV on disk, with its features and classificaiton
     */
    public boolean writeGraphAsCSV = false;

    /**
     * the max distance in the graph, to calculate features. for example: the number of neighbors at distance
     */
    public int maxDistanceForFeatures = 10;

    /**
     * if set to true, the instances will be written to the disc in a csv file
     */
    public boolean printInstancePredToFile=false;

    /**
     * if set to true, features for each node will be caclculated in r.
     * Hub, Authority etc. this features are not used anymore, so this variable is set to false.
     */
    public boolean calculateFeaturesForGraphUsingR = false;

    /**
     * if set to true, the cluster assignment file will be written to disk
     */
    public boolean createClusterAssignFile = true;

    /**
     * this is used to give unique names for the output files
     */
    public String currFileName="";

    /**
     * enum for evaluation the data
     */
    public enum evaluationMethodEnum {locationBasedFoldCrossValidation, deviceBased}

    /**
     * spearman test or kendels tau
     */
    public enum distanceFunctionEnum {spearman, tau}

    /**
     * enum for validation method for training the models
     */
    public enum validationMethodEnum {locationBasedCV, tenFoldCV}

    /**
     * if set to true, the nodes will be classified using the weights: the number of windows in the node.
     */
    public final boolean useWeightsForInstances = true;

    /**
     * if set to true, a graph will be shown, with the nodes.
     */
    public  final boolean showUIGraph = false;


    /**
     * if set to true, labels will be shown on the graph
     */
    public  final boolean showLabelOnUIGraph=true;

    /**
     * path to write the graph into a file
     */
    public  final String contiguityMatrixFileLocation = commonConstants.DBPathTrain + "\\tmp\\" + "tmpContiguityGraphFile.csv";

    /**
     * if set to true, an output file with the windows will be written.
     * the path is written in common constants
     */
    public  final boolean writeOutFile = false;

    /**
     * distance function for windows
     */
    public  final distanceFunctionEnum distanceFunction=distanceFunctionEnum.spearman; //0=spearman, 1=tau

    /**
     * validation method for training the models
     */
    public final validationMethodEnum validationMethod = validationMethodEnum.locationBasedCV;



    /**
     * the condition makes sure that the diff is less than this time in seconds
     */
    public  double windowsSizeLimitSeconds = 0;

    /**
     * number of scans to hold in a window. must be greater than 0
     */
    public  int windowsSizeLimitScans = 20000;

    /**
     * seconds. maximum time difference between windows in order to create an edge between them
     */
    public double edgeThreshold =  300;

    /**
     * used in r classifier
     */
    public final int indoorsClassNum = 1;

    /**
     * used in r classifier
     */
    public final int outdoorsClassNum = 0;

    /**
     * minimum correlation to allow two windows in the same node
     */
    public  double distanceThreshHold = 0.78; // number of allowed diff in the Signature AP in range


    public  HashMap<Integer, graphWifiNode> s_NodeIDToNodeHashMap = new HashMap<Integer, graphWifiNode>();

    /**
     * counts number of files read
     */
    public  int s_numOfFiles = 0;

    /**
     * the locations in the system
     */
    public  Set<String> s_locationSet = new HashSet<String>();
    /**
     * the devices in the system
     */
    public  Set<String> s_deviceSet = new HashSet<String>();

    /**
     * these location will not use to train nor to test
     */
    public  Set<String> s_locationSetToExclude = new HashSet<String>(Arrays.asList("-> -> ","holon-> pharmecy aaron-> ","netaim-> horev-> ")); // Arrays.asList("netaim-> horev-> ") // the locations in this list will be ignored during test and train

    /**
     * for converting ID to the actual window
     */
    public  HashMap<Integer, Wifi_Window> s_IndexToWifiWindow = new HashMap<Integer, Wifi_Window>();
    public  HashMap<String, Integer> s_BSSIDtoBSSID_ID_HashSet = new HashMap<String, Integer>(); // for BSSID, keeps the Integer ID of it
    public  HashMap<Integer, String> s_BSSID_IDtoBSSID_Hash = new HashMap<Integer, String>(); // Inverted. for each Int ID of a BSSID, keeps its name
    public  HashMap<String,HashMap<Integer, Set<Integer>>> s_AP_IDtoWindowList_Hash = new HashMap<String,HashMap<Integer, Set<Integer>>>(); // an inverted list of wifi APs to the UID of all the windows it participates in
    public  HashMap<Long, Wifi_Row> s_WifiRowIDToWifiRow_Hash = new HashMap<Long, Wifi_Row>();

    public stats statistics = new stats();
    /**
     * A m_counter for the windows in this node
     * use to give a UID to nodes
     */
    public  int s_nodesCounter = 0;
    public  int s_winCounter = 0;
    public  long s_count = 0;
    public final String nodesInstancesFilePath = commonConstants.DBPathTrain + "\\tmp\\" + "tmpNodesInstancesGraphFile.csv";
    public final String distMatrixPath = "F:\\thesis\\" + "dist.csv";
    public final String clusterAssignFile="F:\\thesis\\" + "cluster";
    public final String instancePredFile="F:\\thesis\\" + "predictionAndClassificaiton.csv";


    public int minNumOfWindowsInNodeForTrain=1; // amount of windows to be in a node for it to act as train node. raising the value showed no improvement
    public boolean printModelTrainStats=true; // if set the true, an output each fold of the train model will be print. final confusion matrix for the model will be print for all the data.

    /**
     * returns the amount of nodes
     * @return the number of nodes
     */
    public int getNumOfNodes() {
        return this.s_NodeIDToNodeHashMap.size();
    }

    /**
     * returns a node by its id
     * @param nodeID the id of the node
     * @return the node
     */
    public graphWifiNode getNodeByID(int nodeID) {
        return s_NodeIDToNodeHashMap.get(nodeID);
    }

    /**
     * return the location list sorted
     * @return sorted list with the locations
     */
    public List<String> getLocationList(){
        List<String> locationList = new ArrayList<String>(graphWrapperManager.m_dataStore.s_locationSet);
        Collections.sort(locationList); // this list will give numeric index to each location.
        return locationList;
    }

    /**
     * returns wifi window by its id
     * @param id the id of the window
     * @return the window
     */
    public Wifi_Window getWifiWinByID(int id) {
        return this.s_IndexToWifiWindow.get(id);
    }


    /**
     * if set to true, the classification algorithm will return class and not probability (in R)
     */
    public final boolean useClassAndNotProb = false;

    /**
     * creates distance matrix on disk (path is on common constants) for clustering compare in r
     */
    public final boolean createDistFile = false;

    /**
     * if set to true, a graph node being trained for classification can also be in test set
     * effects only device based evaluation
     */
    public final boolean allowTrainAndTestSameNode = true;

    /**
     * if a node is capable of being test and train, setting this to true will cause it to be used as train,
     * and false will cause it to be in test
     * * effects only device based evaluation
     */
    public final boolean preferDataInTrain = true;

    /**
     * if set to true, the model will train on the baseline features
     */
   // public boolean useBaseline = false;

    /**
     * how to evaluate the data
     */
    public final evaluationMethodEnum evaluationMethod = evaluationMethodEnum.deviceBased;

    /**
     * if set to false, the original RSI will be used. if set to true, it will be converted.
     */
    public  boolean usePowerRatio = false;

    /**
     * this devices will be used to train
     */
    public  Set<String> s_deviceSetTrain = new HashSet<String>(Arrays.asList("guy")); // only relevant if using devicebased evaluation
    /**
     * these devices will be used to test
     */
    public  Set<String> s_deviceSetTest = new HashSet<String>(Arrays.asList("livnat","noam","aviad","nofar","matan","avi","igal","rotem","aaron","sagi","genady","idan","guy2","yossi", "samasung", "nexus")); // only relevant if using devicebased evaluation


    public boolean useDBSCANLight = true;


}
