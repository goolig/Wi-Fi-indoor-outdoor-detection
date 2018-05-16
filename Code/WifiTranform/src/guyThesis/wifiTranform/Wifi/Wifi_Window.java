package guyThesis.wifiTranform.Wifi;


import cern.colt.matrix.tint.impl.SparseIntMatrix2D;
import guyThesis.Common.commonConstants;
import guyThesis.wifiTranform.Threads.calcDistanceOnWinThread;
import guyThesis.wifiTranform.Utils.Pair;
import guyThesis.wifiTranform.Utils.majorityEvaluator;
import guyThesis.wifiTranform.graphWifiNode;
import guyThesis.wifiTranform.graphWrapperManager;

import java.util.*;

/**
 * Created by shtar on 03/04/14.
 *
 */
public class Wifi_Window {

    public static long s_countRegionQuery = 0;

    private graphWifiNode m_assignedGraphNode;

    /**
     * this list presents all the wifiRows that make this window.
     * at the beginning it can contain duplicates, as the same AP can be seen few times in a window of few seconds.
     * the duplicates are being removed in the processes
     */
    private List<Wifi_Row> m_WifiRowList;


    /**
     * For each AP ID, its location according to power strength. only visible APs are stored
     */
    HashMap<Integer,Integer> m_AP_IDToRankIndexHash =null;

    /**
     * a hash set of BSSID id visible in this window
     */
    Set<Integer> m_BSSID_IDSet;

    private boolean m_hasEmptyAP;

    /**
     * used to save calculation time
     */
    private Boolean m_isSupervised;

    /**
     * used to save calculation time
     */
    private String m_majorityLocation;
    public boolean m_wasVistedDBSCAN;


    public int getM_ID() {
        return m_ID;
    }

    private int m_ID;

    private int numberOfFingerprints;

    public Wifi_Window() {
        m_WifiRowList = new ArrayList<Wifi_Row>();
        m_ID= graphWrapperManager.m_dataStore.s_winCounter++;
        graphWrapperManager.m_dataStore.s_IndexToWifiWindow.put(m_ID,this);
        m_assignedGraphNode=null;
        m_BSSID_IDSet = new HashSet<Integer>();
        m_hasEmptyAP=false;
        numberOfFingerprints=1;

    }

    public void isGarbage() {
        graphWrapperManager.m_dataStore.s_IndexToWifiWindow.remove(this.getM_ID());
        graphWrapperManager.m_dataStore.s_winCounter--;
    }

    public static Wifi_Window getWifiWinByIndex(int index){
        return graphWrapperManager.m_dataStore.s_IndexToWifiWindow.get(index);
    }



    /**
     * adds a wifi row to this window.
     * @param wifi_row wifi row to consider
     */
    public void considerNextRow(Wifi_Row wifi_row) {
        m_WifiRowList.add(wifi_row);
        if(!wifi_row.isTheEmptyAp())
            m_BSSID_IDSet.add(wifi_row.getBSSID_ID());
        else
            this.m_hasEmptyAP=true;
    }


    /*
    public HashMap<String, Wifi_AP_In_Window> getWindowSummery() {
        HashMap<String, Wifi_AP_In_Window> apWindowHash = new HashMap<String, Wifi_AP_In_Window>();
        for (String bssid : Wifi_Row.s_BSSIDtoBSSID_ID_HashSet.keySet()) {
            apWindowHash.put(bssid, new Wifi_AP_In_Window(bssid));
        }
        for (Wifi_Row row : m_WifiRowList) {
            apWindowHash.get(row.getBSSID()).considerNewRow(row);
        }
        return apWindowHash;

    }
*/
    /**
     * Create an hash table with bssid -> power.
     * for un visible Aps the key is present but set to 0
     * @return Hash list bssid to the Ap power
     */
    public HashMap<String, Double> getWindowRSI() {
        HashMap<String, Wifi_AP_In_Window> apWindowHash = new HashMap<String, Wifi_AP_In_Window>();
        HashMap<String, Double> result = new HashMap<String, Double>();

        for (String bssid : graphWrapperManager.m_dataStore.s_BSSIDtoBSSID_ID_HashSet.keySet()) {
            apWindowHash.put(bssid, new Wifi_AP_In_Window(bssid));
        }
        for (Wifi_Row row : m_WifiRowList) {
            apWindowHash.get(row.getBSSID()).considerNewRow(row);
        }

        for (String bssid : apWindowHash.keySet()) {
            result.put(bssid,apWindowHash.get(bssid).getAveragePower());
        }

        return result;

    }


    /*
    public String getUID() {
        HashSet<String> tmp = new HashSet<String>();
        for(wifi_Row tmpRow : m_WifiRowList){
            tmp.add(tmpRow.getBSSID());
        }
        return ""+tmp.hashCode();
    }*/


    /**
     * returns the distance from this window to the given window.
     * @param otherWifiWin the window to compare to
     * @return the distance between the 2
     */
    public double getSimilartyWith(Wifi_Window otherWifiWin) {
        double ans;
        switch (graphWrapperManager.m_dataStore.distanceFunction) {
            case spearman:
                ans = spearmanDistance(otherWifiWin);//intersectDistance(thisSet, otherSet, IntersectSet);
            break;
            case tau:
                System.out.println("Kendal tau is not implemented yet!!");
                System.exit(-1);
                ans = kendallTau();
                break;
            default:
                System.out.println("getSimilartyWith, wrong distance function");
                ans=0;
                break;
        }
        return ans;
    }

    /**
     * Kendalls tau rank correlation. more sensitive to small mistakes in the ranks.
     * should fit worse to my needs.
     * @return kendalls tau correlation, between 1 and -1.
     */
    private double kendallTau() {

        return 0;

    }

    /**
     * Spearman rank correlation, will give bad scores to large mistakes, but eases with small mistakes.
     * @param otherWifiWin the other window to compare with
     * @return spearman rank correlation. between 1 and -1.
     */
    private double spearmanDistance(Wifi_Window otherWifiWin) {
        if(this.isEmptyWindow() || otherWifiWin.isEmptyWindow())
        {
            if( (this.isEmptyWindow() && otherWifiWin.isEmptyWindow()) && Math.abs(this.getM_ID()-otherWifiWin.getM_ID())<=1 &&(Math.abs(getStartTimeStampLong()-otherWifiWin.getStartTimeStampLong()))/1000 <graphWrapperManager.m_dataStore.edgeThreshold)
                return 1;
            else
                return 0;

        }
        double ans = 0;
        //List<Integer> intersect = IntersectAPIDSet(this, otherWifiWin);
        //if(intersect.size()==0) return ans;
        //List<Integer> UnionSet = UnionAP_IDSet(this, otherWifiWin, intersect);//new HashSet<wifi_Row>();//IntersectAPIDSet(this, otherWifiWin);//new HashSet<wifi_Row>();
        Set<Integer> UnionSet = new HashSet<Integer>(this.getBSSID_ID_SetForWindow()); //new HashSet<wifi_Row>();//IntersectAPIDSet(this, otherWifiWin);//new HashSet<wifi_Row>();
        UnionSet.addAll(otherWifiWin.getBSSID_ID_SetForWindow());
        int thisWindowUnavailableRank = numOfApInWin() + 1;
        int otherWindowUnavailableRank = otherWifiWin.numOfApInWin() + 1;
        //TODO: find better arrangement for different Ap's (at the moment they are at the end.
        for (Integer ApID : UnionSet) {
            int rankOther;
            int rankThis;
            if (!otherWifiWin.m_BSSID_IDSet.contains(ApID)) {
                //this AP is not visible here
                rankOther = otherWindowUnavailableRank++;
            } else {
                rankOther = otherWifiWin.getWifiRowIndexInList(ApID)+1;// sortedRSSListOther.indexOf(ApID) + 1;
            }
            if (getWifiRowIndexInList(ApID)==null) {
                //this ao is not visible here
                rankThis = thisWindowUnavailableRank++;
            } else {
                rankThis = getWifiRowIndexInList(ApID)+1;//sortedRSSListThis.indexOf(ApID) + 1;
            }
            ans += Math.pow(rankThis - rankOther, 2);
        }
        int n = UnionSet.size();
        if (n > 1){
            ans = 1 - (6 * ans) / (n * (n * n - 1));
        }
        else
        {
            // if there is only 1, it is 1 if they are the same, and 0 if not.
            if(numOfApInWin()==1&&otherWifiWin.numOfApInWin()==1){
                if(ans==0)
                    ans=1; // if ans ==0 then, the same wifiRow is in both windows...
                else
                    ans=0; // if ans !=0 then the lists are different
            }
            else
            {
                if(numOfApInWin()==0&&otherWifiWin.numOfApInWin()==0){
                    //System.out.print("Empty");
                    //System.out.print("Empty");

                    ans =1; // if they both have 0, its an empty scan...
                }
                else
                    ans=0;
            }

        }

        if(!(ans<=1&&ans>=-1))
            System.out.println("Bad Value: " + ans +"N "+ n);
        assert ans<=1&&ans>=-1;
        return ans;

    }

    private List<Integer> UnionAP_IDSet(Wifi_Window wifi_window, Wifi_Window otherWifiWin, List<Integer> Intersect) {
        //HashSet<wifi_Row> ans = new HashSet<wifi_Row>();
        List<Integer> result = new ArrayList<Integer>();
        //List<Wifi_Row> Intersect = IntersectAPIDSet(this, otherWifiWin);
        for (Integer currAP : wifi_window.getBSSID_ID_SetForWindow()) {
            result.add(currAP);
        }
        result.removeAll(Intersect);
        for (Integer currAP : otherWifiWin.getBSSID_ID_SetForWindow()) {
            result.add(currAP);
        }
        assert !result.contains(commonConstants.emptyWifiBSSID_ID);
        return result;
    }


    /**
     * creates hash tables for quick distance calculation
     * @param considerTies spearman doesnot takes into account the ties (false) tau does..
     */
    public void generateAPtoRankHash(boolean considerTies){

        List<Wifi_Row> sortedWifiRowList;

        assert m_AP_IDToRankIndexHash ==null;
        m_AP_IDToRankIndexHash = new HashMap<Integer, Integer>();
        sortedWifiRowList = new ArrayList<Wifi_Row>();
        for (Wifi_Row wifi_row : m_WifiRowList) {
            if(!wifi_row.isTheEmptyAp())
                sortedWifiRowList.add(wifi_row);
        }

        Collections.sort(sortedWifiRowList, new CustomWifiRowComparator());
        if(considerTies){
            int currentLocation=0;
            double currentPower= Double.MAX_VALUE;
            for (int i=0;i<sortedWifiRowList.size();i++) {
                if(currentPower!=sortedWifiRowList.get(i).getProperRSINormalized(graphWrapperManager.m_dataStore.usePowerRatio)){
                    currentLocation=i;
                    currentPower = sortedWifiRowList.get(i).getProperRSINormalized(graphWrapperManager.m_dataStore.usePowerRatio);
                }
                m_AP_IDToRankIndexHash.put(sortedWifiRowList.get(i).getBSSID_ID(), currentLocation);
            }
        }
        else
        {
            for (int i=0;i<sortedWifiRowList.size();i++) {
                assert(!m_AP_IDToRankIndexHash.containsKey(sortedWifiRowList.get(i)));
                m_AP_IDToRankIndexHash.put(sortedWifiRowList.get(i).getBSSID_ID(), i);
            }
        }



    }


    public Integer getWifiRowIndexInList(Integer ApID){
        assert !ApID.equals(commonConstants.emptyWifiBSSID_ID);
        return m_AP_IDToRankIndexHash.get(ApID);
    }

    public double[] toInstance() {
        double[] ans =new double[graphWrapperManager.m_dataStore.s_BSSID_IDtoBSSID_Hash.size()];
        HashMap<String, Double> wifiRSIList = getWindowRSI();
        for (int i = 0; i < graphWrapperManager.m_dataStore.s_BSSIDtoBSSID_ID_HashSet.size(); i++) {
            assert (!graphWrapperManager.m_dataStore.s_BSSID_IDtoBSSID_Hash.get(i).equals(commonConstants.emptyWifiBSSID)); // the empty Ap should not appear in the file
            ans[i] = wifiRSIList.get(graphWrapperManager.m_dataStore.s_BSSID_IDtoBSSID_Hash.get(i));
        }
        return ans;
    }

    public graphWifiNode getAssignedNode() {
        return m_assignedGraphNode;
    }


    public void setAssignedNode(graphWifiNode assignedGraphNode) {
        assert m_assignedGraphNode==null||m_assignedGraphNode==assignedGraphNode;
        m_assignedGraphNode = assignedGraphNode;
    }

    public Set<Integer> getNeighbors (SparseIntMatrix2D distMatrix){
        return getNeighborsUsingDistMatrix(distMatrix);
    }

    public Set<Integer> getNeighborsUsingDistMatrix(SparseIntMatrix2D distMatrix) {
        Set<Integer> result = new HashSet<Integer>();
        for (Integer target : getPotentialNeighborsSet(null)) {
            int i = this.getM_ID();
            int j = target;
            if(j<i) {
                j=getM_ID();
                i=target;
            }
            if(distMatrix.getQuick(i, j)!=0){
                assert(i!= j);
                result.add(target);
            }
        }
        return result;
    }


    /**
     * used to measure number of average neighbors the data structre returns
     */
    public static long s_sumPotNeig = 0;
    /**
     * used to measure number of average neighbors the data structre returns
     */
    public static long s_countPotNeig = 0;

    /**
     * calculate the neighbors using multi thread calculation
     *
     * @param currDevice
     */
    public List<Pair<Integer, Double>> getNeighborsUsingRealTimeCalculation(String currDevice) {
        s_countRegionQuery++;
        Set<Integer> potNeigh = getPotentialNeighborsSet(currDevice);
        s_sumPotNeig+=potNeigh.size();
        s_countPotNeig++;
        int potNeighSize = potNeigh.size();
        //List<Pair<Integer,Double>> nodeIDToDistance = new ArrayList<Pair<Integer,Double>>(potNeighSize);
        List<Pair<Integer,Double>> nodeIDToDistance = new LinkedList<Pair<Integer, Double>>();
        List<calcDistanceOnWinThread> listThreads = new ArrayList<calcDistanceOnWinThread>();
        calcDistanceOnWinThread thread = calcDistanceOnWinThread.getNewCalcDistanceThread(potNeighSize);
        listThreads.add(thread);
        for (Integer targetWifiWindowID : potNeigh) {
            thread.addCalculation(this, getWifiWinByIndex(targetWifiWindowID));
            if(thread.reachedMax(potNeighSize))
            {
                thread.start();
                thread = calcDistanceOnWinThread.getNewCalcDistanceThread(potNeighSize);
                listThreads.add(thread);
            }
        }
        thread.start();
        for (calcDistanceOnWinThread currThread : listThreads) {
            try {
                currThread.join();
                nodeIDToDistance.addAll(currThread.getM_matrix());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return nodeIDToDistance;
    }

    public Set<Integer> getPotentialNeighborsSet(String currDevice) {
        Set<Integer> resultAllDevices;
        ArrayList<Integer> neighborsListWithDuplicates = new ArrayList<Integer>(10000);
/*        if(m_WifiRowList.size()==0){ // empty scan
            Set<Integer> wifiWinList = graphWrapperManager.m_dataStore.s_AP_IDtoWindowList_Hash.get(commonConstants.emptyWifiBSSID_ID);
            for (Integer WifiID : wifiWinList) {
                neighborsListWithDuplicates.add(WifiID);
            }
        }*/
        /*else*/ {
            for (Integer ApID : this.getBSSID_ID_SetForWindow()) {
                Set<Integer> wifiWinList = graphWrapperManager.m_dataStore.s_AP_IDtoWindowList_Hash.get(currDevice).get(ApID);
                neighborsListWithDuplicates.addAll(wifiWinList);
                //neighborsListWithDuplicates.ensureCapacity(neighborsListWithDuplicates.size()+wifiWinList.size());
                /*for (Integer WifiID : wifiWinList) {
                    neighborsListWithDuplicates.add(WifiID);
                }*/
            }
            if(m_hasEmptyAP){
                Set<Integer> wifiWinList = graphWrapperManager.m_dataStore.s_AP_IDtoWindowList_Hash.get(currDevice).get(commonConstants.emptyWifiBSSID_ID);
                neighborsListWithDuplicates.addAll(wifiWinList);
            }
        }
        resultAllDevices = new HashSet<Integer>(neighborsListWithDuplicates);
        resultAllDevices.remove(this.getM_ID());
        Set<Integer> result = new HashSet<Integer>(resultAllDevices.size());
        for (Integer currWindowID : resultAllDevices) {
            if(graphWrapperManager.m_dataStore.getWifiWinByID(currWindowID).getDevice().equals(currDevice))
                result.add(currWindowID);
        }
        return result;
    }

    public int getNumOfTrainTest(boolean trainMode) {
        int result=0;
        for (Wifi_Row wifi_row : m_WifiRowList) {
            if(wifi_row.isSupervised()){
                if (wifi_row.isTrainRow()&&trainMode)
                    result++;
                if(!wifi_row.isTrainRow()&&!trainMode)
                    result++;
            }
        }
        return result;
    }

    /**
     * find all the neighbors of this node as defined by the distance threshold
     * add the node and its neighbors to an existing node, or creates a new node
     * this function will add all the nodes that has no node, and should be members of this node.
     * @return true if the node exist, false if a new node was created
     * @param currDevice
     */
    public boolean mergeWithNeighborsNode(String currDevice) {
        boolean ans = false;
        double maxDistance = Double.MIN_VALUE;
        Integer neighborWithClosestNode=null;
        List<Pair<Integer, Double>> neighbors = getNeighborsUsingRealTimeCalculation(currDevice);
        List<Integer> noNodeNeighborWindows = new ArrayList<Integer>(neighbors.size());
        for (Pair<Integer, Double> neighborsAndDistance : neighbors) {
            if(Wifi_Window.getWifiWinByIndex(neighborsAndDistance.getFirst()).getAssignedNode()!=null)
            {
                maxDistance= Math.max(neighborsAndDistance.getSecond(),maxDistance);
                neighborWithClosestNode = neighborsAndDistance.getFirst();
            }
            else
                noNodeNeighborWindows.add(neighborsAndDistance.getFirst());
        }
        graphWifiNode bestNode;
        if(neighborWithClosestNode==null){
            bestNode = new graphWifiNode(this);
        }
        else
        {
            ans=true;
            bestNode = Wifi_Window.getWifiWinByIndex(neighborWithClosestNode).getAssignedNode();
            bestNode.mergeWith(this);
        }
        for (Integer noNodeNeighborWindow : noNodeNeighborWindows) {
            bestNode.mergeWith(Wifi_Window.getWifiWinByIndex(noNodeNeighborWindow));
        }
        return ans;
    }

    public boolean mergeWithNeighborsNodeDBSCAN(String currDevice) {
        new graphWifiNode(this);
        List<Pair<Integer, Double>> neighbors =getNeighborsUsingRealTimeCalculation(currDevice);
        HashSet<Integer> neighborsSet = new HashSet<Integer>(100000);
        HashSet<Integer> visitedSet = new HashSet<Integer>(100000);
        for (Pair<Integer, Double> neighbor : neighbors) {
            neighborsSet.add(neighbor.getFirst());
        }
        //ListIterator<Pair<Integer, Double>> iter = neighbors.listIterator();

        while (!neighborsSet.isEmpty())
        {

            Integer id=0;
            for (Integer integer : neighborsSet) {
                id=integer;
                break;
            }
            neighborsSet.remove(id);
            visitedSet.add(id);
            Wifi_Window otherWindow = Wifi_Window.getWifiWinByIndex(id);
            //Wifi_Window otherWindow = Wifi_Window.getWifiWinByIndex(id.getFirst());
            if(!otherWindow.m_wasVistedDBSCAN) {
                otherWindow.m_wasVistedDBSCAN = true;
                List<Pair<Integer, Double>> neighbors2 = otherWindow.getNeighborsUsingRealTimeCalculation(currDevice);
                for (Pair<Integer, Double> integerDoublePair : neighbors2) {
                    //neighbors.add(integerDoublePair);
                    if(visitedSet.contains(integerDoublePair.getFirst()))
                        continue;
                    neighborsSet.add(integerDoublePair.getFirst());
                }
            }
            if(otherWindow.getAssignedNode()==null)
                this.getAssignedNode().mergeWith(otherWindow);
            //System.out.println(neighbors.size());
        }
        /*
        while(iter.hasNext()){
            Pair<Integer, Double> id = iter.next();
            Wifi_Window otherWindow = Wifi_Window.getWifiWinByIndex(id.getFirst());
            if(!otherWindow.m_wasVistedDBSCAN) {
                otherWindow.m_wasVistedDBSCAN = true;
                List<Pair<Integer, Double>> neighbors2 = otherWindow.getNeighborsUsingRealTimeCalculation(currDevice);
                for (Pair<Integer, Double> integerDoublePair : neighbors2) {
                    iter.add(integerDoublePair);
                }
            }
            if(otherWindow.getAssignedNode()==null)
                this.getAssignedNode().mergeWith(otherWindow);
        }*/
        return false;
    }

    public boolean hasEmptyRow() {
        return m_hasEmptyAP;
    }

    public double getAverageApPower(boolean normalize, boolean usePowerRatio) {
        double sum=0;
        int count=0;
        for (Wifi_Row wifi_row : m_WifiRowList) {
            if(!wifi_row.isTheEmptyAp()) {
                if(normalize)
                    sum += wifi_row.getProperRSINormalized(usePowerRatio);
                else
                    sum+= wifi_row.getProperRSIMeasure(usePowerRatio);
                count++;
            }
            else
                return 0;
        }
        if(count != 0)
            sum /=count;
        return sum ;
    }

    public double getCountApPower(boolean normalize, boolean usePowerRatio) {
        int count=0;
        for (Wifi_Row wifi_row : m_WifiRowList) {
            if(!wifi_row.isTheEmptyAp()) {
                count++;
            }
            else
                return 0;
        }

        return count ;
    }
    public double getSumSqApPower(boolean normalize, boolean usePowerRatio)
    {
        double sum=0;
        for (Wifi_Row wifi_row : m_WifiRowList) {
            if(!wifi_row.isTheEmptyAp()) {
                if(normalize)
                    sum += Math.pow(wifi_row.getProperRSINormalized(usePowerRatio),2);
                else
                    sum+=  Math.pow( wifi_row.getProperRSIMeasure(usePowerRatio),2);
            }
            else
                return 0;
        }
        return sum ;
    }

    public double getSumApPower(boolean normalize, boolean usePowerRatio)
    {
        double sum=0;
        for (Wifi_Row wifi_row : m_WifiRowList) {
            if(!wifi_row.isTheEmptyAp()) {
                if(normalize)
                    sum += wifi_row.getProperRSINormalized(usePowerRatio);
                else
                    sum+= wifi_row.getProperRSIMeasure(usePowerRatio);
            }
            else
                return 0;
        }
        //System.out.println(sum);
        return sum ;
    }

    public double getMaxAPPower(boolean normalize, boolean usePowerRatio) {
        double max=Double.MIN_VALUE;
        for (Wifi_Row wifi_row : m_WifiRowList) {
            if(!wifi_row.isTheEmptyAp()) {
                if(normalize)
                    max = Math.max(wifi_row.getProperRSINormalized(usePowerRatio), max);
                else
                    max = Math.max(wifi_row.getProperRSIMeasure(usePowerRatio), max);
            }

        }
        if(max==Double.MIN_VALUE)
            max=0;
        return max ;
    }

    public boolean isEmptyWindow() {

        return numOfApInWin()==0;
/*        boolean ans = true;
        for (Wifi_Row wifi_row : m_WifiRowList) {
            if(!wifi_row.isTheEmptyAp()) {
                ans = false;
                break;
            }
        }
        return ans;*/
    }

    public HashSet<String> getAsDeviceList() {
        HashSet<String> ans = new HashSet<String>();
        ans.add(getDevice());
        return ans;
    }

    public String getDevice() {
        String ans =null;
        for (Wifi_Row wifi_row : m_WifiRowList) {
            ans = wifi_row.getDevice();
            break;
        }
     return ans;
    }


    public int numOfApInWin(){
        assert !this.getBSSID_ID_SetForWindow().contains(commonConstants.emptyWifiBSSID_ID);
        return this.getBSSID_ID_SetForWindow().size();
    }

    public double getNormalizedCountAps(boolean normalize) {
        if(normalize)
            return graphWrapperManager.m_dataStore.statistics.getNormalizedCountValueForDevice(getDevice(), numOfApInWin()) ;
        return numOfApInWin();
    }

    public void increaseFingerpritCount() {
        this.numberOfFingerprints++;
    }

    public int getNumberOfFingerprints() {
        return this.numberOfFingerprints;
    }

    public void cleanMemoryAfterGraph() {
        m_AP_IDToRankIndexHash=null;
    }


    private class CustomWifiRowComparator implements Comparator<Wifi_Row> {
        @Override
        public int compare(Wifi_Row o1, Wifi_Row o2) {
            //first, compare using the RSI,
            if(o2.getProperRSINormalized(graphWrapperManager.m_dataStore.usePowerRatio)>o1.getProperRSINormalized(graphWrapperManager.m_dataStore.usePowerRatio))
                return 1;
            if(o2.getProperRSINormalized(graphWrapperManager.m_dataStore.usePowerRatio)<o1.getProperRSINormalized(graphWrapperManager.m_dataStore.usePowerRatio))
                return -1;
            //tie breaking, helps spearman rank to handel ties better.
            if(o2.getBSSID_ID()>(o1.getBSSID_ID())) return 1;
            if(o2.getBSSID_ID()<(o1.getBSSID_ID())) return -1;
            return 0;
        }
    }


    /**
     * returns intersect of the wifiRows of this 2 windows.
     * the wifiRow lists should not contain duplicates
     * @param wifi_window first window to intersect
     * @param otherWifiWin second window to intersect
     * @return the intersect of both groups
     */
    private static List<Integer> IntersectAPIDSet(Wifi_Window wifi_window, Wifi_Window otherWifiWin) {
        List<Integer> ans = new ArrayList<Integer>();
        if(otherWifiWin.m_BSSID_IDSet.size()<wifi_window.m_BSSID_IDSet.size()){
            Wifi_Window tmp = otherWifiWin;
            otherWifiWin=wifi_window;
            wifi_window=tmp;
        }
        assert !wifi_window.m_BSSID_IDSet.contains(commonConstants.emptyWifiBSSID_ID);
        assert !otherWifiWin.m_BSSID_IDSet.contains(commonConstants.emptyWifiBSSID_ID);
        Iterator<?> iterator = wifi_window.m_BSSID_IDSet.iterator();
        while (iterator.hasNext()) {
            Integer currAp = (Integer) iterator.next();
            if(otherWifiWin.getBSSID_ID_SetForWindow().contains(currAp)){
                assert !ans.contains(currAp);
                ans.add(currAp);
            }
        }

        return ans;
    }

    public Set<Integer> getBSSID_ID_SetForWindow() {
        assert !m_BSSID_IDSet.contains(commonConstants.emptyWifiBSSID_ID);
        return m_BSSID_IDSet;
    }

    /**
     * Removes duplicate wifiRows from the list.
     * this might be improved in the future to allow summing some info from the similar rows
     */
    public void removeDuplicatedWifiRowsAndCreateRankHash() {
        //HashSet<Integer> set = new HashSet<Integer>();
        List<Wifi_Row> newList = new ArrayList<Wifi_Row>();
        List<Wifi_Row> oldList = m_WifiRowList;

        HashMap<Integer,List<Wifi_Row>> bssidMap= new HashMap<Integer,List<Wifi_Row>>();

        for (Wifi_Row row : m_WifiRowList){
            if(!bssidMap.containsKey(row.getBSSID_ID())){
                bssidMap.put(row.getBSSID_ID(), new ArrayList<Wifi_Row>());
                bssidMap.get(row.getBSSID_ID()).add(row);
            }
        }

        for (Map.Entry<Integer, List<Wifi_Row>> integerListEntry : bssidMap.entrySet()) {
            newList.add(new Wifi_Row(integerListEntry.getValue()));
        }

/*        for (Wifi_Row row : m_WifiRowList){
            if(!set.contains(row.getBSSID_ID())){
                set.add(row.getBSSID_ID());
                newList.add(row);
            }
        }*/
        m_WifiRowList=newList;
        //assert (m_WifiRowList.size()==m_BSSID_IDSet.size());
        switch (graphWrapperManager.m_dataStore.distanceFunction){
            case spearman:
                generateAPtoRankHash(false); // spearman
                break;
            case tau:
                generateAPtoRankHash(true); //kendal's tau
                break;
            default:
                System.out.println("Error!! invalid distance function for sorting Aps");
        }
        m_WifiRowList=oldList;
    }


    /**
     * returns the length of this window in seconds
     * @return the length of this window in seconds
     */
    public double getTimeLength() {

        return (getEndTimeStampLong()-getStartTimeStampLong())*1.0 / 1000;
    }


    //returns the start time of the window
    public String getEndTimeStamp() {
        return "" + getEndTimeStampLong() ;
    }

    public long getEndTimeStampLong(){
        long MaxDate = Long.MIN_VALUE;
        for (Wifi_Row row : m_WifiRowList) {
            try {
                MaxDate = Math.max(row.getTSDate().getTime(), MaxDate);
            } catch (Exception e) {
                // if cant parse just move on...
            }
        }
        return (MaxDate );
    }

    //returns the start time of the window
    public String getStartTimeStamp() {
        return "" + getStartTimeStampLong() ;
    }

    public long getStartTimeStampLong(){
        long MinDate = Long.MAX_VALUE;
        for (Wifi_Row row : m_WifiRowList) {
            try {
                MinDate = Math.min(row.getTSDate().getTime(), MinDate);
            } catch (Exception e) {
                // if cant parse just move on...
            }
        }
        return (MinDate);
    }

    public String getLon() {
        //TODO: get average\med point
        String ans = "";
        for (Wifi_Row row : m_WifiRowList) {
            ans = row.getGPSLongitude();
            if(ans != null && !ans.equals("")) break;
        }
        return ans;
    }

    public String getLat() {
        //TODO: get average\med point
        String ans = "";
        for (Wifi_Row row : m_WifiRowList) {
            ans = row.getGPSLatitude();
            if(ans != null && !ans.equals("")) break;
        }
        return ans;
    }

    /**
     * test of this window is test window
     * @param trainList the list of train location \ devices
     * @param testList the list of test location \ devices
     * @return true if its test window, false otherwise
     */
    public boolean isTestWindow(HashSet<String> trainList, HashSet<String> testList){
        boolean ans=false;
        if (isSupervised()) {
                switch (graphWrapperManager.m_dataStore.evaluationMethod) {
                    case deviceBased:
                        HashSet<String> deviceListTest = getAsDeviceList();
                        deviceListTest.retainAll(testList);

                        if(!graphWrapperManager.m_dataStore.preferDataInTrain) {
                            ans = deviceListTest.size() > 0;
                        }
                        else
                        {
                            if (graphWrapperManager.m_dataStore.allowTrainAndTestSameNode) {
                                ans = deviceListTest.size() > 0;
                            }
                            else {
                                if (getAssignedNode().isTestNode(trainList, testList))
                                    ans = !isTrainWindow(trainList, testList);
                            }
                        }
                        break;
                    case locationBasedFoldCrossValidation:
                        if (getAssignedNode().isTestNode(trainList, testList)) // if the node is not test, this window was not classified...
                        // this may occur when a small portion of windows from 1 location is with a large number of windows from another location in the same node
                        {
                            ans =  !graphWrapperManager.m_dataStore.s_locationSetToExclude.contains(getMajorityConcatLocation()) && (testList == null || testList.contains(getMajorityConcatLocation()));
                        }
                        break;
                    default:
                        assert (false);
                        break;
                }
            }
        return ans;
    }

    /**
     * test if this window is train window
     * @param trainList the list of train location \ devices
     * @param testList the list of train locations \ devices
     * @return true if its train window, false otherwise
     */
    public boolean isTrainWindow( HashSet<String> trainList, HashSet<String> testList){
        boolean ans=false;
        if(isSupervised()) {
            switch (graphWrapperManager.m_dataStore.evaluationMethod) {
                case deviceBased:
                    HashSet<String> deviceListTrain = getAsDeviceList();
                    deviceListTrain.retainAll(trainList);
                    if(graphWrapperManager.m_dataStore.preferDataInTrain) {
                        ans = deviceListTrain.size() > 0;
                    }
                    else {
                        if (graphWrapperManager.m_dataStore.allowTrainAndTestSameNode) {
                            ans = deviceListTrain.size() > 0;
                        }
                        else
                        {
                            if (getAssignedNode().isTrainNode(trainList, testList))
                                ans= !isTestWindow(trainList, testList);
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

    public boolean isSupervised() {
        if(m_isSupervised ==null) {
            for (Wifi_Row row : m_WifiRowList) {
                if (row.isSupervised()) {
                    m_isSupervised = true;
                    break;
                }
            }
            if(m_isSupervised==null)
                m_isSupervised = false;
        }
        return m_isSupervised;
    }

    public String getMajorityConcatLocation() {
        assert isSupervised(); // cant do this for unsupervised
        if(m_majorityLocation == null) {
            majorityEvaluator majorEval = new majorityEvaluator();
            for (Wifi_Row row : m_WifiRowList)
                majorEval.addNewObs(row.getConcatLocation());
            m_majorityLocation = majorEval.getCommonValue();
        }
        return m_majorityLocation;
    }

    public String getMajorityH1() {
        assert isSupervised(); // cant do this for unsupervised
        majorityEvaluator majorEval = new majorityEvaluator();
        for (Wifi_Row row : m_WifiRowList)
            majorEval.addNewObs(row.getH1());
        return majorEval.getCommonValue();
    }

    public String getMajorityH2() {
        assert isSupervised(); // cant do this for unsupervised
        majorityEvaluator majorEval = new majorityEvaluator();
        for (Wifi_Row row : m_WifiRowList)
            majorEval.addNewObs(row.getH2());
        return majorEval.getCommonValue();
    }

    public String getMajorityH3() {
        assert isSupervised(); // cant do this for unsupervised
        majorityEvaluator majorEval = new majorityEvaluator();
        for (Wifi_Row row : m_WifiRowList)
            majorEval.addNewObs(row.getH3());
        return majorEval.getCommonValue();
    }


    public boolean getMajorityIndoorsStateBoolean() {
        assert isSupervised(); // cant do this for unsupervised
        Boolean ans = null;
        String state= getMajorityIndoorsState();
        if(state.equals(""+true)) return true;
        if(state.equals(""+false)) return false;
        System.out.println(state);
        assert false;
        return ans;
    }

    public String getMajorityIndoorsState() {
        assert isSupervised(); // cant do this for unsupervised
        majorityEvaluator majorEval = new majorityEvaluator();
        for (Wifi_Row row : m_WifiRowList) {
            if(row.isSupervised())
                majorEval.addNewObs("" + row.getIndoorsState());
        }
        return majorEval.getCommonValue();
    }


    /**
     * return the majority H2 of this window.
     * @return majority of H2 of this window.
     */
    public String toString(){
        return getMajorityH1() + getMajorityH2() + getMajorityH3();
    }

    private class Wifi_AP_In_Window {

        private String m_Bssid;
        private int m_numOfTimes;
        private double m_sumOfLevels;
        public Wifi_AP_In_Window(String bssid) {
            m_Bssid = bssid;
            m_numOfTimes = 0;
            m_sumOfLevels = 0;
        }

        public double getAveragePower() {
            if (m_numOfTimes != 0)
                return m_sumOfLevels / m_numOfTimes;
            return 0;
        }

        public void considerNewRow(Wifi_Row row) {
            assert m_Bssid.equals(row.getBSSID());
            if (m_Bssid == null) m_Bssid = row.getBSSID();
            m_numOfTimes++;
            m_sumOfLevels += row.getProperRSINormalized(graphWrapperManager.m_dataStore.usePowerRatio);

        }
    }


}
