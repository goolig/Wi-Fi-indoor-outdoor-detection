package GuyThesis.Mining;



import guyThesis.Common.commonConstants;
import weka.core.Instances;
import weka.core.Range;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToString;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Guy on 12/01/2015.
 */
public class Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args){
        commonConstants.setLoggerThings(log);
        LoadCsvToArff();
    }

    private static void LoadCsvToArff() {
        // load CSV
        try {
            CSVLoader loader = new CSVLoader();
            loader.setSource(new File(commonConstants.OutTransformedFilePath));
            Instances data = loader.getDataSet();
            data.setRelationName("TranformedWifiScan");
            NominalToString filter1 = new NominalToString(new Range("1,2,3,4"));
            filter1.setInputFormat(data);
           //filter1.setAttributeIndexes(""+1);

            data = Filter.useFilter(data, filter1);

            // save ARFF
            ArffSaver saver = new ArffSaver();
            saver.setInstances(data);

                saver.setFile(new File(commonConstants.ArffFilePath));

        //    saver.setDestination(new File(commonConstants.ArffFilePath)); //not longer needed in weka >3.5X
            saver.writeBatch();
        } catch (Exception e) {
            e.printStackTrace();
            log.log(Level.FINEST,"Cannot Write ARFF file");
        }
    }


}
