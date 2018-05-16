package guyThesis.wifiTranform.DM;

import guyThesis.wifiTranform.dataStoreAndConfig;
import guyThesis.wifiTranform.graphWifiNode;
import guyThesis.wifiTranform.graphWrapperManager;
import guyThesis.wifiTranform.mainTransform;
import guyThesis.wifiTranform.r.rClassifiersTrainTestLinear;
import guyThesis.wifiTranform.r.rClassifiersTrainTestRandomForestGraphNodes;
import guyThesis.wifiTranform.r.rEvaluation;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Created by Guy on 04/03/2015.
 *
 */
public class DMExperiments {

    /**
     * graph holding all the nodes
     */
    private UndirectedGraph<graphWifiNode, DefaultEdge> m_logicGraph;
    protected final Logger log = mainTransform.log;
    /**
     * locations \ devices. depends on the evaluation method
     */
    private List <String> m_indicesList;
    /**
     * r instance with classifier
     */
    rClassifiersTrainTestRandomForestGraphNodes m_nodeClassifier;

    boolean m_useBaseLine;
    /**
     * Constructor
     * @param logicGraph the graph the classify
     * @param useBaseLine if true use the baseline
     */
    public DMExperiments(UndirectedGraph<graphWifiNode, DefaultEdge> logicGraph, boolean useBaseLine) {
        m_useBaseLine=useBaseLine;

        m_logicGraph=logicGraph;
        switch(graphWrapperManager.m_dataStore.evaluationMethod){
            case deviceBased:
                m_indicesList = new ArrayList<String>(graphWrapperManager.m_dataStore.s_deviceSet);
                break;
            case locationBasedFoldCrossValidation:
                m_indicesList = new ArrayList<String>(graphWrapperManager.m_dataStore.s_locationSet);
                m_indicesList.removeAll(graphWrapperManager.m_dataStore.s_locationSetToExclude);
                break;
            default:
                assert(false); // evaluation method not set correctly
                break;
        }
    }

    /**
     * runs the experiment
     */
    public void runTestAndPrintResultsTrainTest() {
        //if(graphWrapperManager.m_dataStore.evaluationMethod == dataStoreAndConfig.evaluationMethodEnum.locationBasedFoldCrossValidation)
            //createNodeClassifierAllData(); //////////////////////// i cant remeber why this is here...
        rClassifiersTrainTestRandomForestGraphNodes classifier;
        HashSet<String> trainSet = new HashSet<String>(m_indicesList);
        HashSet<String> testSet = new HashSet<String>();


        switch(graphWrapperManager.m_dataStore.evaluationMethod){
                case deviceBased:
                    classifier = new rClassifiersTrainTestRandomForestGraphNodes();
                    classifier.resetSeed();
                    trainSet.retainAll(graphWrapperManager.m_dataStore.s_deviceSetTrain);
                    testSet = new HashSet<String>(m_indicesList);
                    testSet.retainAll(graphWrapperManager.m_dataStore.s_deviceSetTest);

                    //calculateGraphFeatures(trainSet, testSet);
                    log.fine("adding nodes, train: " + Arrays.toString(trainSet.toArray()) + " test: " + Arrays.toString(testSet.toArray()));
                    addNodesToTrain(classifier, trainSet , testSet);
                    classifier.setColNameArray(graphWifiNode.getColHeaderForInstanceTrain(m_useBaseLine));
                    classifier.createModel(m_useBaseLine);
                    classifyNodes(classifier, trainSet , testSet);
                    //classifier.close();
                    printConfusionMatrixAndAUC(trainSet , testSet);
                    break;

                case locationBasedFoldCrossValidation:
                    for (String test : m_indicesList) {
                        classifier = new rClassifiersTrainTestRandomForestGraphNodes();
                        log.fine("adding nodes, leaving out: " + test);

                        testSet.add(test);
                        trainSet.remove(test);

                        addNodesToTrain(classifier, trainSet , testSet);
                        classifier.setColNameArray(graphWifiNode.getColHeaderForInstanceTrain(m_useBaseLine));
                        classifier.createModel(m_useBaseLine);
                        classifyNodes(classifier, trainSet , testSet);
                        classifier.close();
                        printConfusionMatrixAndAUC(trainSet , testSet);

                        testSet.clear();
                        trainSet.add(test);
                    }
                    printConfusionMatrixAndAUC(null, null);
                    break;
                default:
                    assert(false); // evaluation method not set correctly
                    break;
            }
    }

    private void calculateGraphFeatures(HashSet<String> trainSet, HashSet<String> testSet) {
        rClassifiersTrainTestLinear linearModel;
        Set<graphWifiNode.features> featuresToCalculate = new HashSet<graphWifiNode.features>();
        //featuresToCalculate.add(guyThesis.wifiTranform.graphWifiNode.features.sumNodeAtDistance);
        //featuresToCalculate.add(graphWifiNode.features.sumCountApAtDistance);
        //featuresToCalculate.add(graphWifiNode.features.sumPowerAtDistance);
        featuresToCalculate.add(graphWifiNode.features.averageWindowsAtDistance);


        for (graphWifiNode.features feature : featuresToCalculate) {
            linearModel = new rClassifiersTrainTestLinear();
            HashMap<Integer,graphWifiNode> indexToNodeHash = new HashMap<Integer, graphWifiNode>();
            int counter=0;
            for (graphWifiNode graphWifiNode : m_logicGraph.vertexSet()) {
                if (graphWifiNode.isTrainNode(trainSet, testSet)) {
                    linearModel.addInstanceToTrain(graphWifiNode.getMultiFeatureValueWithClass(feature));
                    indexToNodeHash.put(counter++,graphWifiNode);
                }
            }
            double[] predictionForTrain = linearModel.createModel();
            assert predictionForTrain.length==counter;
            for (int i = 0; i < predictionForTrain.length; i++) {
                indexToNodeHash.get(i).setFeature(predictionForTrain[i],feature );
            }
            indexToNodeHash.clear();
            counter=0;
            for (graphWifiNode graphWifiNode : m_logicGraph.vertexSet()) {
                if (graphWifiNode.isTestNode(trainSet, testSet)) {
                    linearModel.addInstanceAllData(graphWifiNode.getMultiFeatureValue(feature));
                    indexToNodeHash.put(counter++,graphWifiNode);
                }
            }
            double[] predictionForTest = linearModel.classifyAllData();
            assert predictionForTest.length==counter;
            for (int i = 0; i < predictionForTest.length; i++) {
                indexToNodeHash.get(i).setFeature(predictionForTest[i],feature);
            }
            linearModel.close(); // make sure all vars are gone
        }

    }

    /**
     * this function ran first when classifying location based, i cant remember why
     */
    private void createNodeClassifierAllData() {
        //Creating a model for the whole data.
        m_nodeClassifier = new rClassifiersTrainTestRandomForestGraphNodes();
        log.fine("adding nodes, leaving out: none");
        addNodesToTrain(m_nodeClassifier, null,null);
        m_nodeClassifier.setColNameArray(graphWifiNode.getColHeaderForInstanceTrain(m_useBaseLine));
        m_nodeClassifier.createModel(m_useBaseLine);
        classifyNodes(m_nodeClassifier, null, null);
        //m_nodeClassifier.close();

    }

    /**
     * goes into each node, and creates AUC for all of the test windows in each node.
     * @param trainList the list indicating device\location train
     * @param trainList the list indicating device\location test
     */
    private void printConfusionMatrixAndAUC(HashSet<String> trainList, HashSet<String> testList) {
        log.fine("printing confusion Matrix, leaving out: " + testList);
        List<Double> instancesPredictions = new ArrayList<Double>();
        List<Integer> instanceClass = new ArrayList<Integer>();
        int [] confusion = new int[4]; // GT=0 + P=0 , GT=0 + P=1, GT=1 + P=0 , GT=1 + P=1
        for (int nodeID = 0; nodeID < graphWrapperManager.m_dataStore.getNumOfNodes(); nodeID++) {
            int [] currConfusion = graphWrapperManager.m_dataStore.getNodeByID(nodeID).getConfusionMatrixAfterPredictionAndPredAndClassForAUC(trainList, testList, instancesPredictions, instanceClass);
            for(int i=0;i<4;i++)
                confusion[i]+=currConfusion[i];

        }
        assert instanceClass.size()==instancesPredictions.size() && instanceClass.size() == (confusion[0] + confusion[1] + confusion[2] + confusion[3]);
        rEvaluation instance = new rEvaluation();
        System.out.println("AUC for: # instances: " + instancesPredictions.size());
        if(!m_useBaseLine && graphWrapperManager.m_dataStore.printInstancePredToFile)
        {
            printInstacesToFile(instancesPredictions,instanceClass);
        }
        instance.printConfusionMatrixDetails(confusion); // prints accuracy, info gain etc.
        instance.printAUCArea(instancesPredictions, instanceClass);
    }

    private void printInstacesToFile(List<Double> instancesPredictions, List<Integer> instanceClass) {
        log.finest("Creating out prediction and classification file");
        BufferedWriter out = null;
        try {
            File file = new File(graphWrapperManager.m_dataStore.instancePredFile);
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

        int numberOfInstances = instancesPredictions.size();
        for (int currInstance = 0; currInstance < numberOfInstances; currInstance++) {
            try {
                String Out = instancesPredictions.get(currInstance) + ", " + instanceClass.get(currInstance);
                out.write("" + Out + '\n');
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
        log.finest("Done writing instance prediction and class!!");
    }

    /**
     * classifies the nodes with location equal to test
     * @param classifier the classifier instance
     * @param trainList the location to test or the train device
     * @param testList the location to test or the test device
     */
    private void classifyNodes(rClassifiersTrainTestRandomForestGraphNodes classifier, HashSet<String> trainList, HashSet<String> testList) {
        List <Integer> testNodes = addAllNodesForClassification(classifier, trainList, testList);

        if(trainList!=null)
            mainTransform.log.fine("Classifying train is: " + Arrays.toString(trainList.toArray()));
        else
            mainTransform.log.fine("Classifying train is: null");
        if(testList!=null)
            mainTransform.log.fine("Classifying test is: " + Arrays.toString(testList.toArray()));
        else
            mainTransform.log.fine("Classifying test is: null");

        double [] classification = classifier.classifyAllData(false);
        mainTransform.log.fine("Classification size: " + classification.length + " nodes: " + graphWrapperManager.m_dataStore.getNumOfNodes() + "# test nodes: " + testNodes.size());

        assert classification.length == testNodes.size();
        for (int index=0; index<testNodes.size(); index++) {
            if(graphWrapperManager.m_dataStore.getNodeByID(testNodes.get(index)).isTestNode(trainList, testList))
            {
                graphWrapperManager.m_dataStore.getNodeByID(testNodes.get(index)).setM_classifiedIndoorProb(classification[index]);
            }
        }

        classification = classifier.classifyAllData(true);

        mainTransform.log.fine("Classification size: " + classification.length + " nodes: " + graphWrapperManager.m_dataStore.getNumOfNodes() + "# test nodes: " + testNodes.size());

        assert classification.length == testNodes.size();
        for (int index=0; index<testNodes.size(); index++) {
            if(graphWrapperManager.m_dataStore.getNodeByID(testNodes.get(index)).isTestNode(trainList, testList))
            {
                graphWrapperManager.m_dataStore.getNodeByID(testNodes.get(index)).setM_Classificaiton(classification[index]);
            }
        }
    }

    /**
     * adds all the graph node instances to the classifier
     * all of the graph will be classified
     * @param classifier the instance
     * @param trainList
     * @param testList
     */
    private List<Integer> addAllNodesForClassification(rClassifiersTrainTestRandomForestGraphNodes classifier, HashSet<String> trainList, HashSet<String> testList) {
        mainTransform.log.fine("Adding instances to test");



        List <Integer> ans = new ArrayList<Integer>(1000);
        for (int nodeID = 0; nodeID<graphWrapperManager.m_dataStore.getNumOfNodes();nodeID++){
            if(graphWrapperManager.m_dataStore.s_NodeIDToNodeHashMap.get(nodeID).isTestNode(trainList,testList)) {
                classifier.addInstanceAllData(graphWrapperManager.m_dataStore.s_NodeIDToNodeHashMap.get(nodeID).nodeAsInstanceForTrain(m_useBaseLine));
                ans.add(nodeID);
            }
        }
        return ans;
    }

    /**
     * adds all the graph nodes as instances that are not with location equal to test
     * @param classifier the classifier instance
     * @param trainList the parameter depends: if we are testing by location, it is the test location. if it is by device, it is the train device.
     * @param testList the parameter depends: if we are testing by location, it is the test location. if it is by device, it is the train device.
     */
    private void addNodesToTrain(rClassifiersTrainTestRandomForestGraphNodes classifier, HashSet<String> trainList, HashSet<String> testList) {
        int trainCount=0;
        int unSupervisedCount=0;
        int totalCount=0;
        int testCount=0;
        int testAndTrainCount=0;

        for (graphWifiNode graphWifiNode : m_logicGraph.vertexSet()) {
            boolean unSupervised=true;
            int isTrainAndTest=0;
            totalCount++;
            if (graphWifiNode.isTrainNode(trainList, testList)) {
                classifier.addInstanceToTrain(graphWifiNode.nodeAsInstanceForTrain(m_useBaseLine), graphWifiNode.getInstanceWeight());
                trainCount++;
                unSupervised = false;
                isTrainAndTest++;
            }
            if (graphWifiNode.isTestNode(trainList, testList)) {
                testCount++;
                unSupervised = false;
                isTrainAndTest++;
            }

            if(unSupervised) {
                unSupervisedCount++;
            }
            if(isTrainAndTest==2)
                testAndTrainCount++;
        }

        System.out.println("Train Nodes: " + trainCount);
        System.out.println("Test Nodes: " + testCount);
        System.out.println("Train And Test Nodes At The Same: " + testAndTrainCount);
        System.out.println("Unsupervised Nodes: " + (unSupervisedCount));
        System.out.println("Total Nodes: " + totalCount);
    }

    /**
     * closes the r classifier
     */
    public void close() {
        for (graphWifiNode graphWifiNode : m_logicGraph.vertexSet()) {
            graphWifiNode.resetBetweenRuns(); // will erase the probabilities
        }
            if(m_nodeClassifier!=null)
        {
            m_nodeClassifier.close();
        }
        new rClassifiersTrainTestRandomForestGraphNodes().close();
    }

    public void pause() {
        rEvaluation instance = new rEvaluation();
        instance.pause(100000);
    }
}
