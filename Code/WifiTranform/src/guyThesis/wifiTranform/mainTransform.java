package guyThesis.wifiTranform;

import guyThesis.Common.commonConstants;
import guyThesis.wifiTranform.DM.DMExperiments;

import java.io.*;
import java.util.logging.Logger;

/**
 * Created by shtar on 20/03/14.
 * Main function for running wifi mapping experiment.
 */
public class mainTransform {
    public static final Logger log = Logger.getLogger("mainLog");
    //public graphWrapperManager s_mgr;

    public static void main(String[] args) throws IOException {
        //double [] distanceThreshold = {1,0.9,0.8,0.7,0.6,0.5,0.4,0.3,0.2,0.1};
        //for (double currThreshold : distanceThreshold) {
            runSingleExperiment(new dataStoreAndConfig());
        //}
    }

    private static void runSingleExperiment(dataStoreAndConfig dataStore) {
        graphWrapperManager s_mgr;
        commonConstants.setLoggerThings(log);
        log.fine("Starting experiment");
        s_mgr = new graphWrapperManager(dataStore);
        String constants="usePowerRatio = "+ graphWrapperManager.m_dataStore.usePowerRatio;
        //constants += ", windowsSizeLimitSeconds = " + graphWrapperManager.m_dataStore.windowsSizeLimitSeconds;
        constants += ", windowsSizeLimitScans = " + graphWrapperManager.m_dataStore.windowsSizeLimitScans;
        constants += ", distanceFunction = " + graphWrapperManager.m_dataStore.distanceFunction;
        constants += ", distanceThreshHold = " + graphWrapperManager.m_dataStore.distanceThreshHold;
        constants += ", showLabelOnUIGraph = " + graphWrapperManager.m_dataStore.showLabelOnUIGraph;
        constants += ", contiguityMatrixFileLocation = " + graphWrapperManager.m_dataStore.contiguityMatrixFileLocation;
        constants += ", showUIGraph = " + graphWrapperManager.m_dataStore.showUIGraph;
        log.fine("Parameters: "+ constants);

final File inFolder = new File(commonConstants.inRawDataFolderPath);
        s_mgr.createWindowsForInput(inFolder);
        if(graphWrapperManager.m_dataStore.writeOutFile) {
            s_mgr.CreateWriteCloseOut();
        }
        if(graphWrapperManager.m_dataStore.createDistFile) {
            s_mgr.createDistFile();
        }

        s_mgr.createLogicGraph();

        if (graphWrapperManager.m_dataStore.showUIGraph)
        s_mgr.createAndPrintUIGraph();
        if (graphWrapperManager.m_dataStore.writeGraphAsCSV)
        s_mgr.writeGraphInstancesToFile();

        s_mgr.cleanMemoryAfterGraph();

        DMExperiments experiment;
        log.fine("starting experiment: baseline");
        experiment = new DMExperiments(s_mgr.getM_logicGraph(),true);
        experiment.runTestAndPrintResultsTrainTest();
        experiment.close();

        log.fine("starting experiment: actual");
        experiment = new DMExperiments(s_mgr.getM_logicGraph(),false);
        experiment.runTestAndPrintResultsTrainTest();
        //experiment.pause();
        experiment.close();

        }
        }
