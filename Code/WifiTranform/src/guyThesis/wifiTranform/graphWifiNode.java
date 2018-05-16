package guyThesis.wifiTranform;

import cern.colt.matrix.tint.impl.SparseIntMatrix2D;
import guyThesis.wifiTranform.Utils.majorityEvaluator;
import guyThesis.wifiTranform.Wifi.Wifi_Window;

import java.util.*;

/**
 * Created by Guy on 27/01/2015.
 * The objects of this class, defines a node in the connectivity graph.
 * each of this nodes holds 1 or more wifi windows with small distance.
 * a cluster if windows in other words.
 * This nodes later build a graph of wifi connectivity, and defines features for DM techniques.
 */

public class graphWifiNode {

    /**
     * probability given to this node after running the classifier
     */
    private Double m_classifiedIndoorProb;


    /**
     * Classification for this node
     */
    private Double m_classification=null;


    /**
     * true if this is an empty node
     */
    private boolean m_isEmptyNode;

    /**
     * saves the time from calculating
     */
    private Boolean m_isSupervised;

    /**
     * holds list of devices in this node
     */
    private HashSet<String> m_deviceList= null;

    /**
     * used to save calculation time
     */
    private String m_majorityLocation;
    private boolean m_majorityLocationWasCalculated = false;


    /**
     * UID for this node. assigned using s_nodesCounter at constructor time
     */
    private int m_serial;

    /**
     * Holds all the wifi windows of this object
     */
    private Set<Integer> m_winList;

    /**
     * holds the features of this node
     */
    private HashMap<features,Double> m_nodeFeatures;



    /**
     * holds the features of this node based on distance on the graph
     */
    private HashMap<features,Double []> m_nodeFeaturesGraphDistance;

    public void setFeatureGraphDistance(features sumNodeAtDistance, Double[] sumNodeDistanceValue) {
        assert !m_nodeFeaturesGraphDistance.containsKey(sumNodeAtDistance);
        m_nodeFeaturesGraphDistance.put(sumNodeAtDistance,sumNodeDistanceValue);
    }

    public double[] getMultiFeatureValue(features featureName) {
        Double [] featureValue;
        double [] ans;
        featureValue = m_nodeFeaturesGraphDistance.get(featureName);
        assert featureValue!=null;
        ans = new double[featureValue.length];
        for (int i = 0; i < featureValue.length; i++) {
            ans[i]=featureValue[i];
        }
        return ans;
    }

    public void resetBetweenRuns() {
        m_classifiedIndoorProb=null;
    }

    public double[] getMultiFeatureValueWithClass(features featureName) {
        double[] ansNoClass = getMultiFeatureValue(featureName);
        double [] ans = new double[ansNoClass.length+1];
        for (int i = 0; i < ansNoClass.length; i++) {
            ans[i] = ansNoClass[i];
        }
        assert m_nodeFeatures.get(features.classification)!=null;
        ans[ans.length-1] = m_nodeFeatures.get(features.classification);
        return ans;
    }

    public Double getAverageNumberOfAps() {
        return m_nodeFeatures.get(features.APAverageCountForBase);
    }

    public Double getCountApInAllFingerprints() {
        double sum = 0 ;
        for (Integer WinID: this.m_winList) {
            sum += graphWrapperManager.m_dataStore.getWifiWinByID(WinID).getNormalizedCountAps(false);
        }
        return (sum);
    }

    /**
     * returns one of the devices this node contains
     * @return a device that this node contains
     */
    public String getDevice() {
        for (String s : this.getDeviceList()) {
            return s;
        }
        return null;
    }

    public void setM_Classificaiton(double v) {
        m_classification = v;
    }

    public int get_Classificaiton() {
        return m_classification.intValue();
    }


    /**
     * holds all possible feature for node
     * the features are choosing later and not all of them are being used in practice
     */
    public enum features {
        clusteringCoefficient, betweenness, closeness, AlphaCentrality, Authority,
        Degree, Hub, PageRank, numOfWindowsNoEmpty,  Eigenvector, location,
        windowAverageLength, APAverageCountForBase, APAveragePowerForBase, isAnEmptyNode,
        sumNodeAtDistance, sumCountApAtDistance, sumPowerAtDistance, averageWindowsAtDistance, sumEmptyNodesAtDistance, APAverageNormalizedPowerForBase, APSTDPower, SumApPower, CountApInAllFingerprints, numOfEmptyWindows, sumEmptyWindowsAtDistance, sumNonEmptyNodeAtDistance, sumAllWindowsAtDistance, maxAPPower, classification // class must be last!!!
    }



    /**
     * Constructor
     * @param currWin the window started this node
     */
    public graphWifiNode(Wifi_Window currWin) {
        currWin.setAssignedNode(this);
        m_winList =new HashSet<Integer>();
        m_serial = graphWrapperManager.m_dataStore.s_nodesCounter++;
        m_winList.add(currWin.getM_ID());
        m_nodeFeatures = new HashMap<features, Double>();
        m_nodeFeaturesGraphDistance= new HashMap<features, Double[]>();
        graphWrapperManager.m_dataStore.s_NodeIDToNodeHashMap.put(m_serial, this);
        m_classifiedIndoorProb =null;
        m_isEmptyNode=currWin.isEmptyWindow();
    }

    /**
     * returns the number of signatures in the node
     * if not using weights, returns always 1
     * @return weight for this node in the classifier
     */
    public int getInstanceWeight() {
        int ans = 1;
        if(graphWrapperManager.m_dataStore.useWeightsForInstances)
            ans = m_winList.size();
        return ans;
    }

    /**
     * adds the window to this node
     * @param currWin the window to add
     */
    public void mergeWith(Wifi_Window currWin) {
        m_isEmptyNode=m_isEmptyNode&&currWin.isEmptyWindow();
        assert currWin.getAssignedNode()==null; // a node should be assigned only once to a window
        m_winList.add(currWin.getM_ID());
        currWin.setAssignedNode(this);
    }
    /**
     * returns a giver feature for this node
     * @param feature the feature to return
     * @return the value of this feature in the node
     */
    public Double getFeature(features feature) {
        return m_nodeFeatures.get(feature);
    }

    /**
     * list of devices in this node
     * the list is cached in the memory
     * @return the list of devices in this node
     */
    public HashSet<String> getDeviceList() {
        if(m_deviceList==null) {
            m_deviceList= new HashSet<String>();
            for (Integer windowID : m_winList) {
                m_deviceList.add(Wifi_Window.getWifiWinByIndex(windowID).getDevice());
            }
        }
        return new HashSet<String>(m_deviceList);
    }

    /**
     * @return the classification of this node
     */
    public Boolean getM_classifiedAsIndoorStateUsingThreshold() {
        assert m_classifiedIndoorProb !=null;
        return m_classifiedIndoorProb > 0.5;
    }

    /**
     *
     * @return true if this node was classified
     */
    public boolean wasClassified(){
        return m_classifiedIndoorProb !=null;
    }

    public void setM_classifiedIndoorProb(double v) {
        assert m_classifiedIndoorProb ==null;
        assert v>=0 && v <=1;
        m_classifiedIndoorProb = v;
    }

    /**
     * tests if this node is a test node
     * @param trainList a list with train location \ devices
     * @param testList a list with test location \ devices
     * @return true if its test node false otherwise
     */
    public boolean isTestNode(HashSet<String> trainList, HashSet<String> testList){
        boolean ans=false;
        if(isSupervised()) {
                switch (graphWrapperManager.m_dataStore.evaluationMethod) {
                    case deviceBased:
                        HashSet<String> deviceListTest = getDeviceList();
                        deviceListTest.retainAll(testList);
                        if(!graphWrapperManager.m_dataStore.preferDataInTrain) {
                            ans = deviceListTest.size() > 0;
                        }
                        else
                        {
                            if (graphWrapperManager.m_dataStore.allowTrainAndTestSameNode)
                            {
                                ans = deviceListTest.size() > 0;
                            }
                            else {
                                ans = !isTrainNode(trainList, testList);
                            }
                        }
                        break;
                    case locationBasedFoldCrossValidation:
                        ans = !graphWrapperManager.m_dataStore.s_locationSetToExclude.contains(getMajorityConcatLocation()) && (testList == null || testList.contains(getMajorityConcatLocation())) ;
                        break;
                    default:
                        assert (false);
                        break;
                }
            }
        return ans;
    }

    /**
     * test if this node is a train node
     * @param trainList the list with train location \ devices
     * @param testList a list with test location \ devices
     * @return true if its train node false otherwise
     */
    public boolean isTrainNode( HashSet<String> trainList, HashSet<String> testList){
        boolean ans=false;
        if(isSupervised()) {
            switch (graphWrapperManager.m_dataStore.evaluationMethod)
            {
                case deviceBased:
                    HashSet<String> deviceListTrain = getDeviceList();
                    deviceListTrain.retainAll(trainList);
                    if(graphWrapperManager.m_dataStore.preferDataInTrain)
                    {
                        ans = deviceListTrain.size() > 0;
                    }
                    else
                    {
                        if (graphWrapperManager.m_dataStore.allowTrainAndTestSameNode) {
                            ans = deviceListTrain.size() > 0;
                        }
                        else
                        {
                            ans= !isTestNode(trainList, testList);
                        }
                    }
                    break;
                case locationBasedFoldCrossValidation:
                    ans = trainList != null && trainList.contains(getMajorityConcatLocation());
                    break;
                default:
                    assert (false);
                    break;
            }
        }
        return ans;
    }

    public int[] getConfusionMatrixAfterPredictionAndPredAndClassForAUC(HashSet<String> trainList, HashSet<String> testList, List<Double> instancesPredictions, List<Integer> instanceClass) {
        int[] result = new int[4];
        for (Integer wifiWinID : m_winList) {
            if(graphWrapperManager.m_dataStore.getWifiWinByID(wifiWinID).isTestWindow(trainList, testList)) {
                //for AUC
                instancesPredictions.add(1-getPredictionProb()); // somewhere im taking the wrong probability, her its fix for the AUC calculation
                if(graphWrapperManager.m_dataStore.getWifiWinByID(wifiWinID).getMajorityIndoorsStateBoolean())
                    instanceClass.add(1);
                else
                    instanceClass.add(0);
                // for confusion matrix
                int index=0;
                if(graphWrapperManager.m_dataStore.getWifiWinByID(wifiWinID).getMajorityIndoorsStateBoolean())
                    index =2;
                if (get_Classificaiton()==graphWrapperManager.m_dataStore.indoorsClassNum)
                    index++; // prediction
                result[index]++;
            }
        }
        //if(wasClassified() &&isSupervised() && (getM_classifiedAsIndoorState()?1:0)!=getMajorityIndoorsStateInt() )//&& ((getPredictionProb()<=0.8&&getM_classifiedAsIndoorState())||(getPredictionProb()>=0.2&&!getM_classifiedAsIndoorState())))
        //    System.out.println(Arrays.toString(this.nodeAsInstanceForTrain(false)) + " ," + this.getNumOfWindow() + " ," + this.getPredictionProb());
        return result;
    }

    /**
     *
     * @return the probabilty of this node
     */
    private Double getPredictionProb() {
        assert m_classifiedIndoorProb!=null;



        return m_classifiedIndoorProb;
    }


    /**
     *
     * @return the label for the UI graph
     */
    public String getUILabel() {
        Double ans =this.getFeature(features.location);
        if(ans==null||!isSupervised())
            return "";
        return ""+(ans.intValue());
    }

    /**
     *
     * @return the id of this node
     */
    public int getUID() {
        return m_serial;
    }

    /**
     *
     * @return the class of this node, -1 if it was not labeled.
     */
    public int getMajorityIndoorsStateInt(){
        String state = getMajorityIndoorsState();
        if(state==null) return -1;
        if(state.equals(""+false)) return graphWrapperManager.m_dataStore.outdoorsClassNum;// 0;
        if(state.equals("" + true)) return graphWrapperManager.m_dataStore.indoorsClassNum;
        return 1;
    }

    /**
     * @return "true" if its indoors "false" in not, "null" if not supervised
     */
    public String getMajorityIndoorsState() {
        majorityEvaluator majorityEval = new majorityEvaluator();
        for (Integer windowID : m_winList)
            if(Wifi_Window.getWifiWinByIndex(windowID).isSupervised())
                majorityEval.addNewObs(Wifi_Window.getWifiWinByIndex(windowID).getMajorityIndoorsState());
        return majorityEval.getCommonValue();
    }

    /**
     *
     * @return true if this node was supervised, of false otherwise
     */
    public boolean isSupervised() {
        if(m_isSupervised ==null) {
            for (Integer windowID : m_winList) {
                if (Wifi_Window.getWifiWinByIndex(windowID).isSupervised()) {
                    m_isSupervised = true;
                    break;
                }
            }
            if(m_isSupervised==null)
                m_isSupervised = false;
        }
        return m_isSupervised;
    }

    /**
     * @return the majority of the locations in this node
     */
    public String getMajorityConcatLocation(){
        if(!m_majorityLocationWasCalculated) {
            majorityEvaluator majorityEval = new majorityEvaluator();
            for (Integer windowID : m_winList) {
                if (Wifi_Window.getWifiWinByIndex(windowID).isSupervised()) {
                    majorityEval.addNewObs(Wifi_Window.getWifiWinByIndex(windowID).getMajorityConcatLocation());
                }
            }
            m_majorityLocation = majorityEval.getCommonValue();
            m_majorityLocationWasCalculated = true;
        }
        return m_majorityLocation;
    }


    /**
     * sets a certain feature of this node
     * @param value the value
     * @param featureName the feature name
     */
    public void setFeature(double value, features featureName) {
        assert !Double.isNaN(value);
        m_nodeFeatures.put(featureName,value);
    }


    /**
     * empty node is a node that all the scans in it are the empty wifi scan
     * @return true if it is an empty node
     */
    public boolean isEmptyNode(){
        return m_isEmptyNode;
    }


    /**
     * sets the internal features of this node
     * @param locationList the locations in the db (used for indexes)
     */
    public void setInternalFeatures(List<String> locationList) {
        //setFeature(this.getAverageLength(),features.windowAverageLength); // kappa little worse with it
        setFeature(this.getSumApPower(false, true),features.SumApPower);
        //setFeature(this.getMaxApPower(false, true),features.maxAPPower);

        setFeature(this.getCountApInAllFingerprints(),features.CountApInAllFingerprints);
        setFeature(this.getAverageApCount(false),features.APAverageCountForBase);
        setFeature(this.getAverageApPower(false, true),features.APAveragePowerForBase);
        //setFeature(this.getAverageApPower(true,true),features.APAverageNormalizedPowerForBase);
        //setFeature(this.getStdApPower(false, true),features.APSTDPower);
        setFeature(this.getNumOfWindow(false),features.numOfWindowsNoEmpty);
        setFeature(this.getNumOfWindow(true) - this.getNumOfWindow(false),features.numOfEmptyWindows);
        //setFeature(isEmptyNode()?1:0,features.isAnEmptyNode);


        String location = this.getMajorityConcatLocation();
        if(location!=null)
            setFeature(locationList.lastIndexOf(location),features.location);
        int classification = getMajorityIndoorsStateInt();
        if(classification!=-1)
            setFeature(classification,features.classification);
    }


    /**
     *
     * @return number of windows in this node
     */
    public int getNumOfWindow(boolean includeEmpty){
        int ans = 0;
        if(includeEmpty){
            for (Integer WinID : m_winList) {
                if(!graphWrapperManager.m_dataStore.getWifiWinByID(WinID).isEmptyWindow())
                    ans++;
            }
        }
        else
            ans= m_winList.size();
        return ans;
    }



    /**
     * @return average power of the APs
     * @param normalize if true the value will be normailzed by device
     */
    private double getStdApPower(boolean normalize, boolean usePowerRatio) {
        double sum = 0 ;
        double sumSq = 0 ;
        double count =0;
        if (isEmptyNode()) return 0;
        for (Integer WinID: this.m_winList) {
            //sum += graphWrapperManager.m_dataStore.getWifiWinByID(WinID).getAverageApPower(normalize, usePowerRatio); //TODO: try weighted average
            sum += graphWrapperManager.m_dataStore.getWifiWinByID(WinID).getSumApPower(normalize, usePowerRatio);
            sumSq += graphWrapperManager.m_dataStore.getWifiWinByID(WinID).getSumSqApPower(normalize, usePowerRatio);
            count +=graphWrapperManager.m_dataStore.getWifiWinByID(WinID).getCountApPower(normalize, usePowerRatio);
        }
        //System.out.println(sum + " " + sumSq + " " + count);

        if (count!=0) {
            sum = sumSq/count - Math.pow(sum/count,2);
        }
        //double ans=Math.sqrt(sum);
        return (sum);
    }

    /**
     * @return average power of the APs
     * @param normalize if true the value will be normailzed by device
     */
    private double getAverageApPower(boolean normalize, boolean usePowerRatio) {
        double sum = 0 ;
        double count =0;
        if (isEmptyNode()) return 0;
        for (Integer WinID: this.m_winList) {
            sum += graphWrapperManager.m_dataStore.getWifiWinByID(WinID).getSumApPower(normalize, usePowerRatio);
            count +=graphWrapperManager.m_dataStore.getWifiWinByID(WinID).getCountApPower(normalize, usePowerRatio);
        }
        if (count!=0) sum/=count;
        return sum;
    }

    public double getSumApPower(boolean normalize, boolean usePowerRatio) {
        double sum = 0 ;
        if (isEmptyNode()) return 0;
        for (Integer WinID: this.m_winList) {
            sum += graphWrapperManager.m_dataStore.getWifiWinByID(WinID).getSumApPower(normalize, usePowerRatio);
        }
        return sum;
    }

    private double getMaxApPower(boolean normalize, boolean usePowerRatio) {
        if (isEmptyNode()) return 0;
        double max = Double.MIN_VALUE ;
        for (Integer WinID: this.m_winList) {
            max = Math.max(graphWrapperManager.m_dataStore.getWifiWinByID(WinID).getMaxAPPower(normalize, usePowerRatio),max);
        }
        return max;
    }


    private double getCountApPower(boolean normalize, boolean usePowerRatio) {
        double count =0;
        if (isEmptyNode()) return 0;
        for (Integer WinID: this.m_winList) {
            count +=graphWrapperManager.m_dataStore.getWifiWinByID(WinID).getCountApPower(normalize, usePowerRatio);
        }
        return count;
    }

    /**
     * @return number of Aps in the node
     */
    private double getAverageApCount(boolean normalize) {
        double sum = 0 ;
        double count =0;
        for (Integer WinID: this.m_winList) {
            sum += graphWrapperManager.m_dataStore.getWifiWinByID(WinID).getNormalizedCountAps(normalize);
            count ++;
        }
        if (sum!=0) sum/=count;
        return sum;
    }

    /**
     * deprecated
     * @return average lenght in time
     */
    private double getAverageLength() {
        double sum = 0 ;
        double count =0;
        for (Integer WinID: this.m_winList) {
            sum+= graphWrapperManager.m_dataStore.getWifiWinByID(WinID).getTimeLength();
            count ++;
        }
        if (sum!=0) sum/=count;
        return sum;
    }

    /**
     * header for the features used in R
     * @return string array of the header
     */
    public static String[] getColHeaderForInstanceTrain(boolean useBaseline){
        List<String> res = new ArrayList<String>();
        res.add(features.location.name());

        if(useBaseline){
            res.add(features.APAverageCountForBase.name());
            res.add(features.APAveragePowerForBase.name());
            res.add(features.averageWindowsAtDistance.name() + 0);


        }
        else
        {
//            res.add(features.APSTDPower.name());


//            res.add(features.sumEmptyNodesAtDistance.name() + 0);
//            res.add(features.sumEmptyNodesAtDistance.name() + 1);
//            res.add(features.sumEmptyNodesAtDistance.name() + 2);
//            res.add(features.sumEmptyNodesAtDistance.name() + 3);
//            res.add(features.sumEmptyNodesAtDistance.name() + 4);
//            res.add(features.sumEmptyNodesAtDistance.name() + 5);
//            res.add(features.sumEmptyNodesAtDistance.name() + 6);

//            res.add(features.sumEmptyWindowsAtDistance.name() + 0);
//            res.add(features.sumEmptyWindowsAtDistance.name() + 1);
//            res.add(features.sumEmptyWindowsAtDistance.name() + 2);
//            res.add(features.sumEmptyWindowsAtDistance.name() + 3);
//            res.add(features.sumEmptyWindowsAtDistance.name() + 4);
//            res.add(features.sumEmptyWindowsAtDistance.name() + 5);
//            res.add(features.sumEmptyWindowsAtDistance.name() + 6);

//            res.add(features.averageWindowsAtDistance.name() + 0);
//            res.add(features.averageWindowsAtDistance.name() + 1);
//            res.add(features.averageWindowsAtDistance.name() + 2);
//            res.add(features.averageWindowsAtDistance.name() + 3);
//            res.add(features.averageWindowsAtDistance.name() + 4);
//            res.add(features.averageWindowsAtDistance.name() + 5);
//            res.add(features.averageWindowsAtDistance.name() + 6);

            res.add(features.sumAllWindowsAtDistance.name() + 0);
            res.add(features.sumAllWindowsAtDistance.name() + 1);
            res.add(features.sumAllWindowsAtDistance.name() + 2);
            res.add(features.sumAllWindowsAtDistance.name() + 3);
            res.add(features.sumAllWindowsAtDistance.name() + 4);
//            res.add(features.sumAllWindowsAtDistance.name() + 5);
//            res.add(features.sumAllWindowsAtDistance.name() + 6);

            res.add(features.sumCountApAtDistance.name() + 0);
            res.add(features.sumCountApAtDistance.name() + 1);
            res.add(features.sumCountApAtDistance.name() + 2);
            res.add(features.sumCountApAtDistance.name() + 3);
            res.add(features.sumCountApAtDistance.name() + 4);
//            res.add(features.sumCountApAtDistance.name() + 5);
//            res.add(features.sumCountApAtDistance.name() + 6);

            res.add(features.sumPowerAtDistance.name() + 0);
            res.add(features.sumPowerAtDistance.name() + 1);
            res.add(features.sumPowerAtDistance.name() + 2);
            res.add(features.sumPowerAtDistance.name() + 3);
            res.add(features.sumPowerAtDistance.name() + 4);
//            res.add(features.sumPowerAtDistance.name() + 5);
//            res.add(features.sumPowerAtDistance.name() + 6);

//            res.add(features.maxAPPower.name() + 0);
//            res.add(features.maxAPPower.name() + 1);
//            res.add(features.maxAPPower.name() + 2);
//            res.add(features.maxAPPower.name() + 3);
//            res.add(features.maxAPPower.name() + 4);
//            res.add(features.maxAPPower.name() + 5);
//            res.add(features.maxAPPower.name() + 6);

            res.add(features.sumNodeAtDistance.name() + 2);
            res.add(features.sumNodeAtDistance.name() + 3);
            res.add(features.sumNodeAtDistance.name() + 4);
            res.add(features.sumNodeAtDistance.name() + 5);
            res.add(features.sumNodeAtDistance.name() + 6);
//            res.add(features.sumNodeAtDistance.name() + 7);
//            res.add(features.sumNodeAtDistance.name() + 8);

//            res.add(features.sumNonEmptyNodeAtDistance.name() + 2);
//            res.add(features.sumNonEmptyNodeAtDistance.name() + 3);
//            res.add(features.sumNonEmptyNodeAtDistance.name() + 4);
//            res.add(features.sumNonEmptyNodeAtDistance.name() + 5);
//            res.add(features.sumNonEmptyNodeAtDistance.name() + 6);
//            res.add(features.sumNonEmptyNodeAtDistance.name() + 7);
//            res.add(features.sumNonEmptyNodeAtDistance.name() + 8);

//            res.add(features.AlphaCentrality.name());
//            res.add(features.PageRank.name());
//            res.add(features.Hub.name());




        }
        res.add(features.classification.name());

        return res.toArray(new String[res.size()]);
    }

    /**
     * this node as an instance for classification
     * @return double array representing this node as an instance
     */
    public double[] nodeAsInstanceForTrain(boolean useBaseline){
        List<Double> ans = new ArrayList<Double>();
        Double value;

        value =m_nodeFeatures.get(features.location);
        ans.add(value == null ? -1 : value);




        if(useBaseline){
//            value =m_nodeFeatures.get(features.isAnEmptyNode);
//            ans.add(value == null ? -1 : value);
            value =m_nodeFeatures.get(features.APAverageCountForBase);
            ans.add(value == null ? -1 : value);
            value =m_nodeFeatures.get(features.APAveragePowerForBase);
            ans.add(value == null ? -1 : value);
            value =m_nodeFeaturesGraphDistance.get(features.averageWindowsAtDistance)[0];
            ans.add(value == null ? -1 : value);
        }
        else
        {
//            value =m_nodeFeatures.get(features.APSTDPower);
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumEmptyNodesAtDistance)[0];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumEmptyNodesAtDistance)[1];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumEmptyNodesAtDistance)[2];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumEmptyNodesAtDistance)[3];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumEmptyNodesAtDistance)[4];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumEmptyNodesAtDistance)[5];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumEmptyNodesAtDistance)[6];
//            ans.add(value == null ? -1 : value);

//            value =m_nodeFeaturesGraphDistance.get(features.sumEmptyWindowsAtDistance)[0];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumEmptyWindowsAtDistance)[1];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumEmptyWindowsAtDistance)[2];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumEmptyWindowsAtDistance)[3];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumEmptyWindowsAtDistance)[4];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumEmptyWindowsAtDistance)[5];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumEmptyWindowsAtDistance)[6];
//            ans.add(value == null ? -1 : value);

//            value =m_nodeFeaturesGraphDistance.get(features.averageWindowsAtDistance)[0];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.averageWindowsAtDistance)[1];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.averageWindowsAtDistance)[2];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.averageWindowsAtDistance)[3];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.averageWindowsAtDistance)[4];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.averageWindowsAtDistance)[5];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.averageWindowsAtDistance)[6];
//            ans.add(value == null ? -1 : value);

            value =m_nodeFeaturesGraphDistance.get(features.sumAllWindowsAtDistance)[0];
            ans.add(value == null ? -1 : value);
            value =m_nodeFeaturesGraphDistance.get(features.sumAllWindowsAtDistance)[1];
            ans.add(value == null ? -1 : value);
            value =m_nodeFeaturesGraphDistance.get(features.sumAllWindowsAtDistance)[2];
            ans.add(value == null ? -1 : value);
            value =m_nodeFeaturesGraphDistance.get(features.sumAllWindowsAtDistance)[3];
            ans.add(value == null ? -1 : value);
            value =m_nodeFeaturesGraphDistance.get(features.sumAllWindowsAtDistance)[4];
            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumAllWindowsAtDistance)[5];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumAllWindowsAtDistance)[6];
//            ans.add(value == null ? -1 : value);

            value =m_nodeFeaturesGraphDistance.get(features.sumCountApAtDistance)[0];
            ans.add(value == null ? -1 : value);
            value =m_nodeFeaturesGraphDistance.get(features.sumCountApAtDistance)[1];
            ans.add(value == null ? -1 : value);
            value =m_nodeFeaturesGraphDistance.get(features.sumCountApAtDistance)[2];
            ans.add(value == null ? -1 : value);
            value =m_nodeFeaturesGraphDistance.get(features.sumCountApAtDistance)[3];
            ans.add(value == null ? -1 : value);
            value =m_nodeFeaturesGraphDistance.get(features.sumCountApAtDistance)[4];
            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumCountApAtDistance)[5];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumCountApAtDistance)[6];
//            ans.add(value == null ? -1 : value);

            value =m_nodeFeaturesGraphDistance.get(features.sumPowerAtDistance)[0];
            ans.add(value == null ? -1 : value);
            value =m_nodeFeaturesGraphDistance.get(features.sumPowerAtDistance)[1];
            ans.add(value == null ? -1 : value);
            value =m_nodeFeaturesGraphDistance.get(features.sumPowerAtDistance)[2];
            ans.add(value == null ? -1 : value);
            value =m_nodeFeaturesGraphDistance.get(features.sumPowerAtDistance)[3];
            ans.add(value == null ? -1 : value);
            value =m_nodeFeaturesGraphDistance.get(features.sumPowerAtDistance)[4];
            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumPowerAtDistance)[5];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumPowerAtDistance)[6];
//            ans.add(value == null ? -1 : value);

//            value =m_nodeFeaturesGraphDistance.get(features.maxAPPower)[0];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.maxAPPower)[1];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.maxAPPower)[2];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.maxAPPower)[3];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.maxAPPower)[4];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.maxAPPower)[5];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.maxAPPower)[6];
//            ans.add(value == null ? -1 : value);

            value =m_nodeFeaturesGraphDistance.get(features.sumNodeAtDistance)[2];
            ans.add(value == null ? -1 : value);
            value =m_nodeFeaturesGraphDistance.get(features.sumNodeAtDistance)[3];
            ans.add(value == null ? -1 : value);
            value =m_nodeFeaturesGraphDistance.get(features.sumNodeAtDistance)[4];
            ans.add(value == null ? -1 : value);
            value =m_nodeFeaturesGraphDistance.get(features.sumNodeAtDistance)[5];
            ans.add(value == null ? -1 : value);
            value =m_nodeFeaturesGraphDistance.get(features.sumNodeAtDistance)[6];
            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumNodeAtDistance)[7];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumNodeAtDistance)[8];
//            ans.add(value == null ? -1 : value);

//            value =m_nodeFeaturesGraphDistance.get(features.sumNonEmptyNodeAtDistance)[2];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumNonEmptyNodeAtDistance)[3];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumNonEmptyNodeAtDistance)[4];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumNonEmptyNodeAtDistance)[5];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumNonEmptyNodeAtDistance)[6];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumNonEmptyNodeAtDistance)[7];
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeaturesGraphDistance.get(features.sumNonEmptyNodeAtDistance)[8];
//            ans.add(value == null ? -1 : value);

//            value =m_nodeFeatures.get(features.AlphaCentrality);
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeatures.get(features.PageRank);
//            ans.add(value == null ? -1 : value);
//            value =m_nodeFeatures.get(features.Hub);
//            ans.add(value == null ? -1 : value);

        }
        value =m_nodeFeatures.get(features.classification);
        ans.add(value==null ? -1 : value);

        double[] ansAsPrimitiveArray = new double[ans.size()];
        for (int i = 0; i < ans.size(); i++) {
            ansAsPrimitiveArray[i]=ans.get(i);
        }
        return ansAsPrimitiveArray;
    }

    /**
     * @return this node as a csv row
     */
    public double[] nodeAsInstanceForCsv(){
        double [] result = new double[features.values().length];
        for(int i=0;i<result.length;i++){
            Double tmp = m_nodeFeatures.get(features.values()[i]);
            if(tmp!=null)
                result[i]=m_nodeFeatures.get(features.values()[i]);
            else result[i] = -1;
        }
        return result;
    }

    /**
     *
     * @return the header column for the nodes
     */
    public static String[] getColHeaderForInstanceCSV(){
        String [] result = new String[features.values().length];
        for(int i=0;i<result.length;i++){
            result[i]=(features.values()[i].name());
        }
        return result;
    }

    /**
     * Deprecated
     * @param tmpNode bla
     * @return bla
     */
    public double getDistanceFrom(graphWifiNode tmpNode) {
        assert !this.equals(tmpNode);
        Iterator<?> iterator = m_winList.iterator();
        double ans = Double.MAX_VALUE;
        while(iterator.hasNext()){
            Wifi_Window currWin = (Wifi_Window)iterator.next();
            Iterator<?> iterator2 = tmpNode.getWifiWinList().iterator();
            while(iterator2.hasNext()) {
                Wifi_Window currWin2 = (Wifi_Window) iterator2.next();
                if(currWin.equals(currWin2)){
                    System.out.println("same window in 2 nodes");
                }
                double currDis= currWin.getSimilartyWith(currWin2);


                ans = Math.min(ans,currDis);
            }
        }
        return ans;
    }


    /**
     * Deprecated
     * @param otherWin bla
     * @return bla
     */
    public double getDistanceFrom(Wifi_Window otherWin) {
        double ans = Double.MAX_VALUE;
        Iterator<?> iterator = m_winList.iterator();
        while(iterator.hasNext()) {
            Wifi_Window currWin = (Wifi_Window) iterator.next();
            double currDis= otherWin.getSimilartyWith(currWin);
            ans = Math.min(ans,currDis);
        }
        return ans;
    }

    /**
     * Deprecated
     * @param distMatrix bla
     * @param openList bla
     * @return bla
     */
    private HashSet<Integer> generateAllWindowInNodeFromSingleWindowUsingMatrix(SparseIntMatrix2D distMatrix, HashSet<Integer> openList) {
        HashSet<Integer> closedList=new HashSet<Integer> ();
        while(openList.size()>0) {
            Integer currNode=null;
            for (Integer integer : openList) {
                currNode = integer;
                break; // just a stupid way to get an item from a set..
            }
            openList.remove(currNode);
            if(!closedList.contains(currNode)) {
                closedList.add(currNode);
                Set<Integer> neighbors = graphWrapperManager.m_dataStore.s_IndexToWifiWindow.get(currNode).getNeighbors(distMatrix);
                for (Integer neighbor : neighbors) {
                    if (!closedList.contains(neighbor)) {
                        if (!openList.contains(neighbor))
                            openList.add(neighbor);
                    }
                }
            }
        }
        return closedList;
    }


    /**
     * deprecated
     * @param distMatrix bla
     * @return bla
     */
    private boolean addAllNodesNeighbors(SparseIntMatrix2D distMatrix) {
        Set<Integer> newList = new HashSet<Integer>();


        for (Integer wifi_window : m_winList) {
            Set<Integer> neighbors = graphWrapperManager.m_dataStore.s_IndexToWifiWindow.get(wifi_window).getNeighbors(distMatrix);
            for (Integer neighbor : neighbors) {
                newList.add(neighbor);
                Wifi_Window.getWifiWinByIndex(neighbor).setAssignedNode(this);
            }
        }

        return m_winList.addAll(newList);
    }


    /**
     * Depreciated
     * @param graphWifiNode bla
     */
    public graphWifiNode(graphWifiNode graphWifiNode) {
        m_winList =new HashSet<Integer>();
        m_serial = graphWrapperManager.m_dataStore.s_nodesCounter++;
        m_winList.addAll(graphWifiNode.m_winList);
        m_nodeFeatures = new HashMap<features, Double>();
        m_nodeFeaturesGraphDistance= new HashMap<features, Double[]>();
        graphWrapperManager.m_dataStore.s_NodeIDToNodeHashMap.put(m_serial, this);
        m_classifiedIndoorProb =null;
        m_isEmptyNode=graphWifiNode.isEmptyNode();
    }

    /**
     * Deprecated
     * @return - bla
     */
    public Set<Integer> getWifiWinList(){
        return new HashSet<Integer>(m_winList);
    }


    /**
     * Deprecated
     * @param tmpNode bla
     */
    public void mergeWith(graphWifiNode tmpNode) {
        this.m_winList.addAll(tmpNode.getWifiWinList());
        m_isEmptyNode=m_isEmptyNode&&tmpNode.isEmptyNode();
    }



    /**
     * depreicated
     * @param i bla
     */
    public void setNewIndex(int i) {
        m_serial = i;
    }


    /**
     * depreciated
     * @param distMatrix bla
     */
    public void mergeWithAllNodesInNeighborhood(SparseIntMatrix2D distMatrix) {
        HashSet<Integer> allWifiWinInNode = generateAllWindowInNodeFromSingleWindowUsingMatrix(distMatrix, new HashSet<Integer>(this.m_winList));
        m_winList.clear();
        for (Integer currWinID : allWifiWinInNode) {
            graphWrapperManager.m_dataStore.s_IndexToWifiWindow.get(currWinID).setAssignedNode(this);
            m_winList.add(currWinID);
        }
/*        boolean improvement = true;
        while (improvement){
            improvement = addAllNodesNeighbors(distMatrix);
        }*/
    }
}
