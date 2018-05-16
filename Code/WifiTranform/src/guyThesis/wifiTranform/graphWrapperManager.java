
package guyThesis.wifiTranform;
import cern.colt.matrix.tint.impl.SparseIntMatrix2D;
import guyThesis.wifiTranform.Threads.*;
import guyThesis.wifiTranform.Wifi.Wifi_Window;
import guyThesis.wifiTranform.Wifi.Wifi_Window_Container;
import guyThesis.wifiTranform.r.rGraphUtil;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swingViewer.Viewer;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Guy on 27/01/2015.
 *
 */
public class graphWrapperManager extends fileManager {


    public static dataStoreAndConfig m_dataStore;

    private final Logger log = mainTransform.log;//Logger.getLogger(graphWrapperManager.class.getName());
    List<graphWifiNode> m_nodeList;

    /**
     * Simple graph to hold graphWifiNode
     */
    private UndirectedGraph<graphWifiNode, DefaultEdge> m_logicGraph;

    /**
     * UI graph to display on screen
     */
    Graph m_GraphUi;

    /**
     * allow assigning UID to the edges
     */
    private long edgeUIGraphID;

    public graphWrapperManager(dataStoreAndConfig dataStoreAndConfig) {
        m_dataStore = dataStoreAndConfig;
        //commonConstants.setLoggerThings(log);
        m_nodeList = new ArrayList<graphWifiNode>();
        edgeUIGraphID = 0;

    }

    public UndirectedGraph<graphWifiNode, DefaultEdge> getM_logicGraph() {
        return m_logicGraph;
    }


    /**
     * For each window in this nodeA, sets the hash table value to this node.
     * this later allows mapping a window to the proper node
     *
     * @param nodeA the node we are setting the hash for
     */
    private void setWinNodeHash(graphWifiNode nodeA) {
        for (Integer integer : nodeA.getWifiWinList()) {
            Wifi_Window currWin = Wifi_Window.getWifiWinByIndex(integer);
            currWin.setAssignedNode(nodeA);
        }
    }

    /**
     * Creates the graph from the input windows
     */
    public void createLogicGraph() {

        assert (m_logicGraph == null); // this shouldn't run more than 1 time
        log.finest("Creating Graph");
        m_logicGraph = createWindowsGraph();


        //createNodesOneInANode();

        if(graphWrapperManager.m_dataStore.useDBSCANLight)
            createNodesIncrementalDBSCANLight();
        else
            createNodesIncrementalDBSCAN();
        System.out.println("average number of potential neighbors: " + (Wifi_Window.s_sumPotNeig/Wifi_Window.s_countPotNeig));
        if(graphWrapperManager.m_dataStore.createClusterAssignFile) {
            printsNodesToFile();
        }
        addNodesAndEdgesToLogicGraph();
        if(graphWrapperManager.m_dataStore.calculateFeaturesForGraphUsingR)
            setFeaturesFromGraphForAllNodes();
        setFeaturesFromInternalKnowledgeForAllNodes();
        handleNodesStats();
    }



    private void handleNodesStats() {
        for (graphWifiNode graphWifiNode : m_logicGraph.vertexSet()) {
            graphWrapperManager.m_dataStore.statistics.considerGraphNode(graphWifiNode);
        }
        graphWrapperManager.m_dataStore.statistics.printGraphNodesStats();
    }

    /**
     * creates\assigns nodes from the all windows. one winsow in a node
     */
    public void createNodesOneInANode() {
        log.log(Level.FINE, "DONE creating Nodes USING one in a node");
        long tStart = System.currentTimeMillis();
        for (int winID=0;winID<m_dataStore.s_winCounter;winID++)
            m_nodeList.add(new graphWifiNode(m_dataStore.getWifiWinByID(winID)));

        long tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;
        double elapsedSeconds = tDelta / 1000.0;
        log.log(Level.FINE, "DONE creating Nodes from distance matrix, number of regionQuery: " + Wifi_Window.s_countRegionQuery + " time: " + elapsedSeconds);
    }

    /**
     * creates\assigns nodes from the all windows
     */
    public void createNodesIncrementalDBSCANLight() {
        log.log(Level.FINE, "DONE creating Nodes USING DBSCANLight");
        long tStart = System.currentTimeMillis();
        Set<String> allDevices = new HashSet<String>(m_dataStore.s_deviceSet);
        allDevices.retainAll(m_dataStore.s_deviceSetTrain);
        for (String currDevice :allDevices ) {
            log.log(Level.FINE, "Device: " + currDevice);
            for (int winID=0;winID<m_dataStore.s_winCounter;winID++)
                createNodeFromSingleWindow(m_dataStore.getWifiWinByID(winID), currDevice);
        }
        long tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;
        double elapsedSeconds = tDelta / 1000.0;
        log.log(Level.FINE, "DONE creating Nodes from distance matrix, number of regionQuery: " + Wifi_Window.s_countRegionQuery+ " time: " + elapsedSeconds);
        Wifi_Window.s_countRegionQuery=0;

        allDevices = new HashSet<String>(m_dataStore.s_deviceSet);
        allDevices.removeAll(m_dataStore.s_deviceSetTrain);
        for (String currDevice : allDevices) {
            log.log(Level.FINE, "Device: " + currDevice);
            for (int winID=0;winID<m_dataStore.s_winCounter;winID++)
                createNodeFromSingleWindow(m_dataStore.getWifiWinByID(winID), currDevice);
        }
        tEnd = System.currentTimeMillis();
        tDelta = tEnd - tStart;
        elapsedSeconds = tDelta / 1000.0;
        log.log(Level.FINE, "DONE creating Nodes from distance matrix, number of regionQuery: " + Wifi_Window.s_countRegionQuery + " time: " + elapsedSeconds);
        log.log(Level.FINE, "DONE creating Nodes from distance matrix, size: " + m_nodeList.size());
    }

    /**
     * creates\assigns nodes from the all windows
     */
    public void createNodesIncrementalDBSCAN() {
        log.log(Level.FINE, "DONE creating Nodes USING DBSCAN");
        long tStart = System.currentTimeMillis();
        Set<String> allDevices = new HashSet<String>(m_dataStore.s_deviceSet);
        allDevices.retainAll(m_dataStore.s_deviceSetTrain);
        for (String currDevice :allDevices ) {
            log.log(Level.FINE, "Device: " + currDevice);
            for (int winID=0;winID<m_dataStore.s_winCounter;winID++){
                if(winID%1000==0)System.out.println(winID);
                createNodeFromSingleWindowDBSCAN(m_dataStore.getWifiWinByID(winID), currDevice);
            }

        }
        long tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;
        double elapsedSeconds = tDelta / 1000.0;
        log.log(Level.FINE, "DONE creating Nodes from distance matrix, number of regionQuery: " + Wifi_Window.s_countRegionQuery + " time: " + elapsedSeconds);
        Wifi_Window.s_countRegionQuery=0;

        allDevices = new HashSet<String>(m_dataStore.s_deviceSet);
        allDevices.removeAll(m_dataStore.s_deviceSetTrain);
        for (String currDevice : allDevices) {
            log.log(Level.FINE, "Device: " + currDevice);
            for (int winID=0;winID<m_dataStore.s_winCounter;winID++) {
                if(winID%1000==0)System.out.println(winID);
                createNodeFromSingleWindowDBSCAN(m_dataStore.getWifiWinByID(winID), currDevice);
            }
        }
        tEnd = System.currentTimeMillis();
        tDelta = tEnd - tStart;
        elapsedSeconds = tDelta / 1000.0;
        log.log(Level.FINE, "DONE creating Nodes from distance matrix, number of regionQuery: " + Wifi_Window.s_countRegionQuery +" time: " + elapsedSeconds);
        log.log(Level.FINE, "DONE creating Nodes from distance matrix, size: " + m_nodeList.size());
    }

    private void createNodeFromSingleWindowDBSCAN(Wifi_Window Wifi_window, String currDevice) {
        if (Wifi_window.getDevice().equals(currDevice)) {
            if(Wifi_window.m_wasVistedDBSCAN)
                return;
            Wifi_window.m_wasVistedDBSCAN =true;
            if (!Wifi_window.mergeWithNeighborsNodeDBSCAN(currDevice)) {
                m_nodeList.add(Wifi_window.getAssignedNode());
            }
        }
    }

    /**
     * creates\assigns node for a single window
     */
    private void createNodeFromSingleWindow(Wifi_Window Wifi_window, String currDevice) {
        if (Wifi_window.getAssignedNode() == null&&Wifi_window.getDevice().equals(currDevice)) {
            if (!Wifi_window.mergeWithNeighborsNode(currDevice)) {
                m_nodeList.add(Wifi_window.getAssignedNode());
            }
        }
    }


    private void createNodesFromDistanceMatrix(SparseIntMatrix2D distMatrix) {
        //log.log(Level.FINE,"creating Nodes from distance matrix");
        int count = 0;
        for (Map.Entry<Integer, Wifi_Window> integerWifi_windowEntry : m_dataStore.s_IndexToWifiWindow.entrySet()) {
            count++;
            System.out.printf("\rCreating nodes from Distance Matrix: %.2f", (double) count / m_dataStore.s_IndexToWifiWindow.size());
            if (integerWifi_windowEntry.getValue().getAssignedNode() == null) {
                graphWifiNode newNode = new graphWifiNode(integerWifi_windowEntry.getValue());
                newNode.mergeWithAllNodesInNeighborhood(distMatrix);
                m_nodeList.add(newNode);
            }

        }
        System.out.println();
        log.log(Level.FINE, "DONE creating Nodes from distance matrix, size: " + m_nodeList.size());

    }

    private void setFeaturesFromInternalKnowledgeForAllNodes() {

        List<String> locationList = graphWrapperManager.m_dataStore.getLocationList();
        for (int i = 0; i < locationList.size(); i++) {
            System.out.println("location: " + i + " is " + locationList.get(i));
        }
        for (guyThesis.wifiTranform.graphWifiNode graphWifiNode : m_logicGraph.vertexSet()) {
            graphWifiNode.setInternalFeatures(locationList);
        }
        setDistanceBasedGraphFeatures();

    }



    private void setDistanceBasedGraphFeatures() {
        for (graphWifiNode firstWifiNode : m_logicGraph.vertexSet()) {
            HashMap<graphWifiNode, Integer> neighborsDistances = getNeighborsUpToDistance(m_logicGraph, firstWifiNode, m_dataStore.maxDistanceForFeatures);
            Double [] sumNodeDistance = new Double[m_dataStore.maxDistanceForFeatures];
            Double [] sumPowerDistance = new Double[m_dataStore.maxDistanceForFeatures];
            Double [] sumCountAPDistance = new Double[m_dataStore.maxDistanceForFeatures];
            Double [] averageNonEmptyWindowsDistance = new Double[m_dataStore.maxDistanceForFeatures];
            Double [] sumCountEmptyNodesDistance = new Double[m_dataStore.maxDistanceForFeatures];
            Double [] sumCountEmptyWindowsDistance = new Double[m_dataStore.maxDistanceForFeatures];
            Double [] sumNonEmptyNodeAtDistance =  new Double[m_dataStore.maxDistanceForFeatures];
            Double [] sumAllWindowsAtDistance =  new Double[m_dataStore.maxDistanceForFeatures];
            //Double [] maxAPPowerAtDistance =  new Double[m_dataStore.maxDistanceForFeatures];





            for (int i = 0; i < m_dataStore.maxDistanceForFeatures; i++) {
                sumNodeDistance[i] = (double) 0;
                sumPowerDistance[i] = (double)0;
                sumCountAPDistance[i] = (double)0;
                averageNonEmptyWindowsDistance[i]=(double)0;
                sumCountEmptyNodesDistance[i]=(double)0;
                sumCountEmptyWindowsDistance[i]=(double)0;
                sumNonEmptyNodeAtDistance[i]=(double)0;
                sumAllWindowsAtDistance[i]=(double)0;
                //maxAPPowerAtDistance[i]= Double.MIN_VALUE;



            }
            for (Map.Entry<graphWifiNode, Integer> nodeDistance : neighborsDistances.entrySet()) {
                graphWifiNode secondWifiNode = nodeDistance.getKey();
                int distanceFromNode2 = nodeDistance.getValue();
                for (int i = 0; i < m_dataStore.maxDistanceForFeatures; i++) {
                    if (distanceFromNode2 <= i) {
                        sumNodeDistance[i] = sumNodeDistance[i] + 1;
                        averageNonEmptyWindowsDistance[i] = averageNonEmptyWindowsDistance[i] + secondWifiNode.getFeature(graphWifiNode.features.numOfWindowsNoEmpty); // sums the number of windows. counts also empty fingerprints
                        sumCountEmptyWindowsDistance[i] = sumCountEmptyWindowsDistance[i]+ secondWifiNode.getFeature(graphWifiNode.features.numOfEmptyWindows);
                        sumAllWindowsAtDistance[i]  = sumAllWindowsAtDistance[i] + secondWifiNode.getFeature(graphWifiNode.features.numOfEmptyWindows) + secondWifiNode.getFeature(graphWifiNode.features.numOfWindowsNoEmpty);
                        if(!secondWifiNode.isEmptyNode()) {
                            //maxAPPowerAtDistance[i] = Math.max(maxAPPowerAtDistance[i],secondWifiNode.getFeature(graphWifiNode.features.maxAPPower));
                            sumPowerDistance[i] = sumPowerDistance[i] + secondWifiNode.getFeature(graphWifiNode.features.SumApPower); // sums the power in each and every wifi row
                            sumCountAPDistance[i] = sumCountAPDistance[i] + secondWifiNode.getFeature(graphWifiNode.features.CountApInAllFingerprints); // sums the number of AP in all if the fingerprints (number of rows)
                        }
                        else
                        {
                            sumCountEmptyNodesDistance[i] = sumCountEmptyNodesDistance[i] + 1; // sums the number of nodes
                        }
                    }
                }
            }
            for (int i = 0; i < m_dataStore.maxDistanceForFeatures; i++) {
                //if(maxAPPowerAtDistance[i]==Double.MIN_VALUE)
                //    maxAPPowerAtDistance[i] = 0.0;
                if(sumCountAPDistance[i]!=0)
                {
                    sumPowerDistance[i] = sumPowerDistance[i] / (sumCountAPDistance[i]);
                }
                if(averageNonEmptyWindowsDistance[i] + sumCountEmptyWindowsDistance[i]!=0){
                    sumCountAPDistance[i] = sumCountAPDistance[i]/ (averageNonEmptyWindowsDistance[i] + sumCountEmptyWindowsDistance[i]);
                }

                if(sumNodeDistance[i]!=0) {
                    sumAllWindowsAtDistance[i] = sumAllWindowsAtDistance[i] / sumNodeDistance[i];
                }

                if(sumNodeDistance[i] - sumCountEmptyNodesDistance[i]!=0){
                    averageNonEmptyWindowsDistance[i] = (averageNonEmptyWindowsDistance[i] / (sumNodeDistance[i] - sumCountEmptyNodesDistance[i]));
                }
                if(sumCountEmptyNodesDistance[i]!=0)
                    sumCountEmptyWindowsDistance[i] = sumCountEmptyWindowsDistance[i] / sumCountEmptyNodesDistance[i];

                sumNonEmptyNodeAtDistance[i] = sumNodeDistance[i] - sumCountEmptyNodesDistance[i];
            }
            firstWifiNode.setFeatureGraphDistance(graphWifiNode.features.sumNodeAtDistance, sumNodeDistance); //****
            firstWifiNode.setFeatureGraphDistance(graphWifiNode.features.sumPowerAtDistance, sumPowerDistance);//****
            firstWifiNode.setFeatureGraphDistance(graphWifiNode.features.sumCountApAtDistance, sumCountAPDistance);//*****
            firstWifiNode.setFeatureGraphDistance(graphWifiNode.features.averageWindowsAtDistance, averageNonEmptyWindowsDistance);//******
            //firstWifiNode.setFeatureGraphDistance(graphWifiNode.features.sumEmptyNodesAtDistance, sumCountEmptyNodesDistance);
           // firstWifiNode.setFeatureGraphDistance(graphWifiNode.features.sumEmptyWindowsAtDistance, sumCountEmptyWindowsDistance);
            //firstWifiNode.setFeatureGraphDistance(graphWifiNode.features.sumNonEmptyNodeAtDistance, sumNonEmptyNodeAtDistance);
            firstWifiNode.setFeatureGraphDistance(graphWifiNode.features.sumAllWindowsAtDistance, sumAllWindowsAtDistance);//******
            //firstWifiNode.setFeatureGraphDistance(graphWifiNode.features.maxAPPower, maxAPPowerAtDistance);




        }
    }

    private HashMap<graphWifiNode, Integer> getNeighborsUpToDistance(UndirectedGraph<graphWifiNode, DefaultEdge> m_logicGraph, graphWifiNode graphWifiNode, int maxDistance) {
        HashMap<graphWifiNode, Integer> ans = new HashMap<graphWifiNode, Integer>();
        int distance = 0;
        ans.put(graphWifiNode, 0);
        while (distance <= maxDistance) {
            distance++;
            HashMap<graphWifiNode, Integer> newNodes = new HashMap<graphWifiNode, Integer>();
            for (Map.Entry<guyThesis.wifiTranform.graphWifiNode, Integer> nodeDistance : ans.entrySet()) {
                HashSet<graphWifiNode> neighborsList = getAdjacentTo(m_logicGraph, nodeDistance.getKey());
                for (guyThesis.wifiTranform.graphWifiNode wifiNode : neighborsList) {
                    if (!ans.containsKey(wifiNode)) {
                        newNodes.put(wifiNode, nodeDistance.getValue() + 1);
                        assert distance == nodeDistance.getValue() + 1;
                    }
                }
            }
            ans.putAll(newNodes);
        }
        return ans;
    }

    private HashSet<graphWifiNode> getAdjacentTo(UndirectedGraph<graphWifiNode, DefaultEdge> m_logicGraph, graphWifiNode currNode) {
        List<graphWifiNode> neighborsList = new ArrayList<graphWifiNode>();
        for (DefaultEdge defaultEdge : m_logicGraph.edgesOf(currNode)) {
            neighborsList.add(m_logicGraph.getEdgeSource(defaultEdge));
            neighborsList.add(m_logicGraph.getEdgeTarget(defaultEdge));
        }
        HashSet<graphWifiNode> ans = new HashSet<graphWifiNode>();
        ans.addAll(neighborsList);
        ans.remove(currNode);
        return ans;
    }

    private void applyFeatureToNodes(double[] feature, graphWifiNode.features featureName) {
        if (feature == null || (feature.length != 0 && feature.length != m_nodeList.size())) {
            log.info("cant read feature: " + featureName.name());
            return;
        }
        for (int i = 0; i < feature.length; i++) {
            m_nodeList.get(i).setFeature(feature[i], featureName);
        }
        //System.out.println("Feature: " + Arrays.toString(feature));
    }


    public void setFeaturesFromGraphForAllNodes() {

        log.log(Level.FINE, "Creating graph in R, Nodes: " + m_nodeList.size());
        assert (m_logicGraph != null);
        rGraphUtil R = new rGraphUtil(m_nodeList.size());
        for (int i = 0; i < m_nodeList.size(); i++) {

            R.setEdgesForNode(i, nodeAsList(i));

        }
        //int[][] graphAsMatrix =logicGraphAsMatrix();
        /*for(int i=0;i<m_nodeList.size();i++){
            int [] vertexEdgesRow= graphAsMatrix[i];  //getNodeVertexRow(m_nodeList.get(i));
            R.addVertexRowToMatrix(vertexEdgesRow);
        }*/

        log.fine("Creating the graph elem in r");
        R.createGraphElem();
        //R.createGraphFromFile(writeLogicGraphAsFile());

        log.log(Level.FINE, "Calculating features");
        applyFeatureToNodes(R.getClusteringCoefficient(), graphWifiNode.features.clusteringCoefficient); //kill
        //applyFeatureToNodes(R.getSubGraphCentrality(), graphWifiNode.features.subGraphCentrality); // not running at all
        //applyFeatureToNodes(R.getBonacichPower(),graphWifiNode.features.BonacichPower); // not running at all - gives an error
        applyFeatureToNodes(R.getEigenvector(), graphWifiNode.features.Eigenvector); // little worse

        applyFeatureToNodes(R.getBetweenness(), graphWifiNode.features.betweenness); // OK, improves netaaim, but little worse on others.
        applyFeatureToNodes(R.getAlphaCentrality(), graphWifiNode.features.AlphaCentrality);//so so - fucked up the gim
        applyFeatureToNodes(R.getCloseness(), graphWifiNode.features.closeness); //so so
        applyFeatureToNodes(R.getPageRank(), graphWifiNode.features.PageRank); //Good. Improves nettaim and others.
        applyFeatureToNodes(R.getHub(), graphWifiNode.features.Hub); // overall improved, worse on some..
        applyFeatureToNodes(R.getAuthority(), graphWifiNode.features.Authority);// overall improved, worse on some..
        applyFeatureToNodes(R.getDegree(), graphWifiNode.features.Degree);

        R.close();
    }

    private List<Integer> nodeAsList(int i) {
        List<Integer> nodeAsList = new ArrayList<Integer>();
        {
            graphWifiNode currNode = m_dataStore.s_NodeIDToNodeHashMap.get(i);
            for (DefaultEdge defaultEdge : m_logicGraph.edgesOf(currNode)) {
                int otherNode = m_logicGraph.getEdgeTarget(defaultEdge).getUID();
                if (otherNode == i)
                    otherNode = m_logicGraph.getEdgeSource(defaultEdge).getUID();
                assert (otherNode != i);
                if (i > otherNode)
                    nodeAsList.add(otherNode);
            }
        }
        Collections.sort(nodeAsList);
        return nodeAsList;
    }


    private UndirectedGraph<graphWifiNode, DefaultEdge> createWindowsGraph() {
        return new SimpleGraph<graphWifiNode, DefaultEdge>(DefaultEdge.class);
    }


    public void displayUIGraph() {
        Viewer viewer = m_GraphUi.display();
        viewer.enableAutoLayout();
    }

    /**
     * create a sparse distance matrix using multi threads
     *
     * @return the sparse distance matrix
     */
    private SparseIntMatrix2D createDistanceMatrix() {
        log.log(Level.FINE, "creating Dist Matrix");
        long countDistanceCalculations = 0;
        SparseIntMatrix2D distMatrix = new SparseIntMatrix2D(m_dataStore.s_IndexToWifiWindow.size(), m_dataStore.s_IndexToWifiWindow.size());
        List<calcDistanceOnWinThreadToMatrix> ThreadSet = new ArrayList<calcDistanceOnWinThreadToMatrix>();
        calcDistanceOnWinThreadToMatrix Thread = calcDistanceOnWinThreadToMatrix.getNewCalcDistanceThread(distMatrix);
        ThreadSet.add(Thread);
        int count = 0;
        // first, for each window, we add all of the other windows with similar APs
        for (Map.Entry<Integer, Wifi_Window> integerWifi_windowEntry : m_dataStore.s_IndexToWifiWindow.entrySet()) {
            count++;
            System.out.printf("\rCreating Matrix: %.2f", (double) count / m_dataStore.s_IndexToWifiWindow.size());
            Wifi_Window sourceWin = integerWifi_windowEntry.getValue();
            Set<Integer> targetWindowsSet = sourceWin.getPotentialNeighborsSet(null);

            // now we filter the higher right part of the matrix, and run the calculation
            for (Integer targetWindow : targetWindowsSet) {
                if (sourceWin.getM_ID() < targetWindow) // remove the lower left diagonal of the distance matrix
                {
                    Thread.addCalculation(sourceWin, Wifi_Window.getWifiWinByIndex(targetWindow));
                    if (Thread.reachedMax()) {
                        //System.out.println("starting Threads: " + Thread.getName());
                        Thread.start();
                        Thread = calcDistanceOnWinThreadToMatrix.getNewCalcDistanceThread(distMatrix);
                        ThreadSet.add(Thread);
                    }
                    countDistanceCalculations++;
                }
            }
        }
        System.out.println();
        log.fine("waiting for Threads");

        for (calcDistanceOnWinThreadToMatrix tmpThread : ThreadSet) {
            try {
                tmpThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.fine("Done creating Dist Matrix, count of distance calculations: " + countDistanceCalculations);
        return distMatrix;

    }


    /***
     * Creates and prints the graph.
     */
    public void createAndPrintUIGraph() {
        createUIGraphFromLogical();
        displayUIGraph();
        log.fine("num of nodes in UI graph: " + m_GraphUi.getNodeSet().size());

    }

    /**
     * A graph is being built this way:
     * the windows are sorted by time.
     * therefore, we traverse them by this sequence.
     * each window is being translated to the node who contains it.
     * for two consequential windows, their nodes are being connected by edge.
     */
    private void addNodesAndEdgesToLogicGraph() {
        log.log(Level.FINE, "Adding edges and vertexes to logic Graph");
        assert (m_logicGraph != null);
        initUIGraphCSS();
        for (Wifi_Window_Container currManager : s_windowsManagerList) {
            Iterator<?> iteratorWins = currManager.getWifiWindowList().iterator();
            graphWifiNode lastNode;
            graphWifiNode currNode = null;
            Wifi_Window lastWin = null;
            while (iteratorWins.hasNext()) {
                lastNode = currNode;
                Wifi_Window currWin = (Wifi_Window) iteratorWins.next();
                currNode = currWin.getAssignedNode();
                assert currNode != null; // all windows should be assigned to nodes
                addVertexToLogicGraph(currNode);
                assert lastWin == null || currWin.getStartTimeStampLong() - lastWin.getStartTimeStampLong() >= 0;
                if (lastWin != null && currWin.getStartTimeStampLong() - lastWin.getStartTimeStampLong() <= graphWrapperManager.m_dataStore.edgeThreshold * 1000)
                    //if ((lastWin == null || !lastNode.isEmptyNode()) && !currNode.isEmptyNode())
                        addEdgeToLogicGraph(lastNode, currNode);
                lastWin = currWin;
            }
        }
    }


    /***
     * copies the logical graph to UI graph
     */
    private void createUIGraphFromLogical() {
        log.log(Level.FINE, "creating UI Graph");
        assert (m_logicGraph != null);
        initUIGraphCSS();
        for (graphWifiNode graphWifiNode : this.m_logicGraph.vertexSet()) {
            for (DefaultEdge defaultEdge : m_logicGraph.edgesOf(graphWifiNode)) {
                addVertexToUIGraph(m_logicGraph.getEdgeSource(defaultEdge));
                addVertexToUIGraph(m_logicGraph.getEdgeTarget(defaultEdge));
                addEdgeToUIGraph(m_logicGraph.getEdgeSource(defaultEdge), m_logicGraph.getEdgeTarget(defaultEdge));
            }
        }
    }

    private void initUIGraphCSS() {
        String styleSheet = "" +
                "node.unsupervised { size: 4px;\n" +
                "fill-color: LavenderBlush;\n" +
                "stroke-mode: plain;\n" +
                "stroke-color: GhostWhite;\n" +
                " }\n" +
                "node.true { size: 4px;\n" +
                "fill-color: Black;\n" +
                " }\n" +
                "node.false { size: 4px;\n" +

                "fill-color: Gray;\n" +
                "stroke-mode: plain;\n" +
                "stroke-color: GhostWhite;\n" +
                " }\n";
        if (!graphWrapperManager.m_dataStore.showLabelOnUIGraph)
            styleSheet += "node {\n" +
                    "    text-mode: hidden;\n" +
                    "}\n";
        styleSheet += "edge {\n" +
                "   shape: line;\n" +
                "   size: 1px;\n" +
                "    shape: line;\n" +
                "    fill-color: #E5E4E2;\n" +
                "}";

        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");// use the new UI
        m_GraphUi = new SingleGraph("WifiWindows");
        m_GraphUi.setStrict(true);
        m_GraphUi.setAutoCreate(false);
        m_GraphUi.addAttribute("ui.quality");
        m_GraphUi.addAttribute("ui.antialias");
        m_GraphUi.addAttribute("ui.stylesheet", styleSheet);
    }

    private void addVertexToLogicGraph(graphWifiNode currNode) {
        this.m_logicGraph.addVertex(currNode);
    }

    private void addVertexToUIGraph(graphWifiNode currNode) {
        this.m_logicGraph.addVertex(currNode);
        String Uid = "" + currNode.getUID();
        if (m_GraphUi.getNode(Uid) == null) {
            Node n = m_GraphUi.addNode("" + currNode.getUID());
            if (currNode.isSupervised()) {
                n.addAttribute("ui.class", currNode.getMajorityIndoorsState().toLowerCase());
                String devName="";
                for (String s : currNode.getDeviceList()) {
                    devName=s;
                }
                n.addAttribute("ui.label", currNode.getNumOfWindow(true)) ;
            } else {
                n.addAttribute("ui.class", "unsupervised");
                n.addAttribute("ui.label", "");
            }
        }
    }

    private void addEdgeToLogicGraph(graphWifiNode sourceNode, graphWifiNode targetNode) {
        if (sourceNode != null && targetNode != null)
            if (!(sourceNode.getUID() == targetNode.getUID()))
                m_logicGraph.addEdge(sourceNode, targetNode);
    }

    private void addEdgeToUIGraph(graphWifiNode sourceNode, graphWifiNode targetNode) {
        if (sourceNode != null && targetNode != null) {
            if (!(sourceNode.getUID() == targetNode.getUID())) {
                if (!m_GraphUi.getNode("" + sourceNode.getUID()).hasEdgeBetween("" + targetNode.getUID())) {
                    m_GraphUi.addEdge("" + edgeUIGraphID, "" + sourceNode.getUID(), "" + targetNode.getUID());
                }
            }
            edgeUIGraphID++;
        }
    }


    private void createLogicalGraphNodesSelfImp() {
        log.log(Level.FINE, "creating Graph Wrapper Elem");
        Iterator<?> iteratorManager = s_windowsManagerList.iterator();
        while (iteratorManager.hasNext()) {
            Wifi_Window_Container currManager = (Wifi_Window_Container) iteratorManager.next();
            Iterator<?> iteratorWins = currManager.getWifiWindowList().iterator();
            Wifi_Window currWin;
            graphWifiNode node = null;
            while (iteratorWins.hasNext()) {
                currWin = (Wifi_Window) iteratorWins.next();
                if (node != null && node.getDistanceFrom(currWin) >= graphWrapperManager.m_dataStore.distanceThreshHold) {
                    node.mergeWith(currWin);
                } else
                    node = addWindowAsNewNode(currWin);
            }
        }
        boolean improvement = true;
        for (int i = 1; improvement; i++) {
            System.out.println("Starting iteration: " + i);
            improvement = false; // 1 time is enough for now...

        }
    }


    /**
     * Deprecated
     *
     * @param graphWifiNode bla
     * @return bla
     */
    private double[] getNodeVertexRow(graphWifiNode graphWifiNode) {
        double[] result = new double[m_nodeList.size()];

        graphWifiNode otherNode;

        for (DefaultEdge defaultEdge : m_logicGraph.edgesOf(graphWifiNode)) {
            if (!m_logicGraph.getEdgeSource(defaultEdge).equals(graphWifiNode)) {
                otherNode = m_logicGraph.getEdgeSource(defaultEdge);
            } else {
                otherNode = m_logicGraph.getEdgeTarget(defaultEdge);
            }
            result[(otherNode).getUID()] = 1;
        }

        return result;
    }

    private void printDistToFile(double[][] disMat) {
        List<Double> distributionList = new ArrayList<Double>();
        for (int i = 0; i < this.m_nodeList.size(); i++) {
            for (int j = i + 1; j < this.m_nodeList.size(); j++) {
                distributionList.add(disMat[i][j]);
            }
        }
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("DistFile.txt", "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Collections.sort(distributionList);
        if (writer != null) {
            for (Double aDistributionList : distributionList) {
                writer.println(aDistributionList);
            }
            writer.println();
            writer.close();
        }

    }


    /**
     * The function performs Hierarchical clustering of the graph nodes.
     * it merges nodes with small distance
     *
     * @return the amount of nodes after merging
     */
    public int HierarchicalClustering() {
        long start = System.nanoTime();
        int numOfNodesCleared;
        double[][] disMat = new double[m_nodeList.size()][m_nodeList.size()];
        System.out.println("calculating Matrix, Size: " + m_nodeList.size());
        List<calcDistanceOnNodesThread> ThreadSet = new ArrayList<calcDistanceOnNodesThread>();
        calcDistanceOnNodesThread Thread = calcDistanceOnNodesThread.getNewCalcDistanceThread(m_nodeList.size());
        ThreadSet.add(Thread);
        // adding all calculations to threads
        for (int i = 0; i < this.m_nodeList.size(); i++) {
            graphWifiNode NodeA = m_nodeList.get(i);
            for (int j = i + 1; j < this.m_nodeList.size(); j++) {
                graphWifiNode NodeB = m_nodeList.get(j);
                Thread.addCalculation(NodeA, NodeB, disMat, i, j);
                if (Thread.reachedMax()) {
                    Thread = calcDistanceOnNodesThread.getNewCalcDistanceThread(m_nodeList.size());
                    ThreadSet.add(Thread);
                }
            }
        }
        System.out.println("Stating Threads: ");

        //calculating using all the threads
        for (calcDistanceOnNodesThread tmpThread : ThreadSet) {
            tmpThread.start();
        }
        for (calcDistanceOnNodesThread tmpThread : ThreadSet) {
            try {
                tmpThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        HashSet<Integer> excludedMerged = new HashSet<Integer>();
        System.out.println("Merging Close Nodes");
        List<graphWifiNode> NewList = new ArrayList<graphWifiNode>();
        for (int i = 0; i < this.m_nodeList.size(); i++) {
            if (!excludedMerged.contains(i)) {
                graphWifiNode NodeA = new graphWifiNode(m_nodeList.get(i));
                for (int j = i + 1; j < this.m_nodeList.size(); j++) {
                    graphWifiNode NodeB = m_nodeList.get(j);
                    if (disMat[i][j] >= graphWrapperManager.m_dataStore.distanceThreshHold && !excludedMerged.contains(j)) { // For spearman test, larger values are better.
                        NodeA.mergeWith(NodeB);
                        excludedMerged.add(j);
                    }
                }
                setWinNodeHash(NodeA);
                NewList.add(NodeA);
            }
        }
        numOfNodesCleared = m_nodeList.size() - NewList.size();
        m_nodeList = NewList;
        long elapsedTime = System.nanoTime() - start;
        System.out.println("Node List after Merge: " + m_nodeList.size() + " Time Taken (minutes): " + elapsedTime / (60.0 * 1000.0 * 1000.0 * 1000.0));
        return numOfNodesCleared;
    }

    /**
     * adds this current window to a new empty node
     *
     * @param currWin the window to add as new node
     */
    public graphWifiNode addWindowAsNewNode(Wifi_Window currWin) {
        graphWifiNode node = new graphWifiNode(currWin);
        m_nodeList.add(node);
        return node;
    }


    public void createNodesUsingMatrix() {
        //createLogicalGraphNodesSelfImp();
        SparseIntMatrix2D distMatrix = createDistanceMatrix();
        System.gc();
        //createLogicalGraphNodesUsingR();
        createNodesFromDistanceMatrix(distMatrix);
        distMatrix = null;
        System.gc();
    }


    private int[][] logicGraphAsMatrix() {
        int result[][] = new int[m_nodeList.size()][m_nodeList.size()];
        for (DefaultEdge defaultEdge : m_logicGraph.edgeSet()) {
            int target = (m_logicGraph.getEdgeTarget(defaultEdge)).getUID();
            int source = (m_logicGraph.getEdgeSource(defaultEdge)).getUID();
            result[target][source] = 1;
            result[source][target] = 1;
        }
        return result;
    }

    /**
     * writes the contiguity matrix to a file.
     *
     * @return
     */
    private String writeLogicGraphAsFile() {
        BufferedWriter out = null;
        int countRows = 0;
        try {
            File graphFile = new File(graphWrapperManager.m_dataStore.contiguityMatrixFileLocation);
            if (!graphFile.exists()) {
                graphFile.createNewFile();
            }
            out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(graphFile, false), "UTF8"
            ));
        } catch (IOException e) {
            e.printStackTrace();
            log.log(Level.FINEST, "Cannot write to GRAPH out file!!!! is it opened??");
            System.exit(1);
        }
        for (int nodeID = 0; nodeID < m_dataStore.s_NodeIDToNodeHashMap.size(); nodeID++) {
            graphWifiNode currNode = m_dataStore.s_NodeIDToNodeHashMap.get(nodeID);
            int[] nodeAsArray = new int[m_dataStore.s_NodeIDToNodeHashMap.size()];
            for (DefaultEdge defaultEdge : m_logicGraph.edgesOf(currNode)) {
                int otherNode = m_logicGraph.getEdgeTarget(defaultEdge).getUID();
                if (otherNode == nodeID)
                    otherNode = m_logicGraph.getEdgeSource(defaultEdge).getUID();
                assert (otherNode != nodeID);
                nodeAsArray[otherNode] = 1;
            }
            try {
                out.write(Arrays.toString(nodeAsArray).replace("[", "").replace("]", "") + '\n');
                countRows++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert countRows == m_dataStore.s_NodeIDToNodeHashMap.size();
        return graphWrapperManager.m_dataStore.contiguityMatrixFileLocation;
    }


    private void renumberNodesInLogicalGraphAfterMergeAndAddToList() {
        m_nodeList = new ArrayList<graphWifiNode>();
        //graphWifiNode.m_nodeToIndex=new HashMap<graphWifiNode, Integer>();
        int nodeCounter = 0;
        for (graphWifiNode graphWifiNode : m_logicGraph.vertexSet()) {
            graphWifiNode.setNewIndex(nodeCounter++);
            m_nodeList.add(graphWifiNode.getUID(), graphWifiNode);
            //guyThesis.wifiTranform.graphWifiNode.m_nodeToIndex.put(graphWifiNode, graphWifiNode.getUID());
        }
        assert (m_nodeList.size() == m_logicGraph.vertexSet().size());
    }


    /**
     * Deprecated.
     */
    private void createUIGraphFromWindows() {
        log.log(Level.FINE, "creating UI Graph");
        initUIGraphCSS();
        Iterator<?> iteratorManager = s_windowsManagerList.iterator();
        while (iteratorManager.hasNext()) {
            Wifi_Window_Container currManager = (Wifi_Window_Container) iteratorManager.next();
            Iterator<?> iteratorWins = currManager.getWifiWindowList().iterator();
            graphWifiNode lastWin;
            graphWifiNode currWin = null;
            while (iteratorWins.hasNext()) {

                lastWin = currWin;
                currWin = ((Wifi_Window) iteratorWins.next()).getAssignedNode();
                addVertexToUIGraph(currWin);
                addEdgeToUIGraph(lastWin, currWin);
            }
        }
    }

    /***
     * writes all the graph nodes to file, with all the features, including the features that are not used for classification
     */
    public void writeGraphInstancesToFile() {
        mainTransform.log.fine("Writing nodes as instances");
        BufferedWriter out = null;
        try {
            File graphFile = new File(graphWrapperManager.m_dataStore.nodesInstancesFilePath);
            if (!graphFile.exists()) {
                graphFile.createNewFile();
            }
            out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(graphFile, false), "UTF8"
            ));
        } catch (IOException e) {
            e.printStackTrace();
            log.log(Level.FINEST, "Cannot write to GRAPH out file!!!! is it opened??");
        }
        String instance = Arrays.toString(graphWifiNode.getColHeaderForInstanceCSV());
        instance = instance.substring(1, instance.length() - 1);
        try {
            out.write(instance + '\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int nodeID = 0; nodeID < graphWrapperManager.m_dataStore.getNumOfNodes(); nodeID++) {
            instance = Arrays.toString(graphWrapperManager.m_dataStore.s_NodeIDToNodeHashMap.get(nodeID).nodeAsInstanceForCsv());
            instance = instance.substring(1, instance.length() - 1);
            try {
                out.write(instance + '\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * creates distance matrix in the right path, for comparing my clustering algorithm to db-scan
     * doesnot work any more. after changing the getneighbors
     */
    public void createDistFile() {
        log.finest("Creating out dist Matrix");
        BufferedWriter out = null;
        try {
            File file = new File(graphWrapperManager.m_dataStore.distMatrixPath);
            if (!file.exists()) {
                file.createNewFile();
            }
            out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file, false), "UTF8"
            ));
        } catch (IOException e) {
            e.printStackTrace();
            log.log(Level.FINEST, "Cannot write to DIST out file!!!! is it opened??");
        }

        int numberOfWin = m_dataStore.s_IndexToWifiWindow.size();
        for (int currWinIndex = 0; currWinIndex < numberOfWin; currWinIndex++) {
            if((currWinIndex %1000 ==0)) System.out.println("" + ((double) currWinIndex) / numberOfWin);
            Wifi_Window currWin = m_dataStore.s_IndexToWifiWindow.get(currWinIndex);
            double[] currOutRow = new double[numberOfWin];

            Set<Integer> potNeigh = currWin.getPotentialNeighborsSet(null);

            for (int secondWinIndex = 0; secondWinIndex < numberOfWin; secondWinIndex++) {

                if (potNeigh.contains(secondWinIndex)) {
                    currOutRow[secondWinIndex] = 1 - currWin.getSimilartyWith(m_dataStore.getWifiWinByID(secondWinIndex));
                }
                else
                {
                    if(currWinIndex==secondWinIndex)
                        currOutRow[secondWinIndex]=0;
                    else
                        currOutRow[secondWinIndex] = 2;
                }
            }
            try {
                DecimalFormat df = new DecimalFormat("#.###");

                String[] currOutRowString = new String[numberOfWin];
                assert currOutRow.length == numberOfWin;
                for (int i = 0; i < currOutRow.length; i++) {
                    currOutRowString[i] = df.format(currOutRow[i]);
                }
                out.write(Arrays.toString(currOutRowString).replaceAll("]","").substring(1) + '\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.finest("Done Out Dist Matrix!!");
    }

    private void printsNodesToFile() {
        log.finest("Creating out cluster assignment file");
        BufferedWriter out = null;

        try {
            String fileName = graphWrapperManager.m_dataStore.clusterAssignFile;
            fileName+=graphWrapperManager.m_dataStore.currFileName;
            if(graphWrapperManager.m_dataStore.useDBSCANLight)
                fileName+="DBSANLight";
            else
                fileName+="DBSCAN";
            fileName+=".csv";
            File file = new File(fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file, false), "UTF8"
            ));
        } catch (IOException e) {
            e.printStackTrace();
            log.log(Level.FINEST, "Cannot write to CLuster out file!!!! is it opened??");
        }

        int numberOfWin = m_dataStore.s_IndexToWifiWindow.size();
        for (int currWinIndex = 0; currWinIndex < numberOfWin; currWinIndex++) {
            try {
                int id = m_dataStore.s_IndexToWifiWindow.get(currWinIndex).getAssignedNode().getUID() +1 ; // cannt use 0 in r
                out.write("" + id + '\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.finest("Done Out CLUSTER Matrix!!");
    }

    public void cleanMemoryAfterGraph() {
        for (int winID=0;winID<m_dataStore.s_winCounter;winID++)
            m_dataStore.getWifiWinByID(winID).cleanMemoryAfterGraph();
    }
/*    private void setDistanceBasedGraphFeaturesbkp() {
        for (guyThesis.wifiTranform.graphWifiNode graphWifiNode : m_logicGraph.vertexSet()) {
            BellmanFordShortestPath tmp = new BellmanFordShortestPath(m_logicGraph, graphWifiNode);
            double number2 = 0;
            double number5 = 0;
            double number15 = 0;
            double number30 = 0;
            number2 += m_logicGraph.degreeOf(graphWifiNode);
            number5 += m_logicGraph.degreeOf(graphWifiNode);
            number15 += m_logicGraph.degreeOf(graphWifiNode);
            number30 += m_logicGraph.degreeOf(graphWifiNode);
            for (graphWifiNode graphWifiNode2 : m_logicGraph.vertexSet()) {
                if (graphWifiNode2 != graphWifiNode && tmp.getCost(graphWifiNode2) <= 2)
                    number2 += m_logicGraph.degreeOf(graphWifiNode);
                if (graphWifiNode2 != graphWifiNode && tmp.getCost(graphWifiNode2) <= 5)
                    number5 += m_logicGraph.degreeOf(graphWifiNode);
                if (graphWifiNode2 != graphWifiNode && tmp.getCost(graphWifiNode2) <= 15)
                    number15 += m_logicGraph.degreeOf(graphWifiNode);
                if (graphWifiNode2 != graphWifiNode && tmp.getCost(graphWifiNode2) <= 30)
                    number30 += m_logicGraph.degreeOf(graphWifiNode);
            }
            graphWifiNode.setFeature(number2, guyThesis.wifiTranform.graphWifiNode.features.sumNodeAtDistance);
            graphWifiNode.setFeature(number5, guyThesis.wifiTranform.graphWifiNode.features.degreeDistance5);
            graphWifiNode.setFeature(number15, guyThesis.wifiTranform.graphWifiNode.features.degreeDistance15);
            graphWifiNode.setFeature(number30, guyThesis.wifiTranform.graphWifiNode.features.degreeDistance30);
        }
    }*/

}
