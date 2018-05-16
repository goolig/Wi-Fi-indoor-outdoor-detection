package guyThesis.wifiTranform.r;


import guyThesis.wifiTranform.graphWifiNode;
import guyThesis.wifiTranform.graphWrapperManager;
import org.rosuda.JRI.REXP;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Guy on 04/03/2015.
 * createModel graph nodes using r and 10 fold
 */
public class rClassifiersTrainTestLinear extends rInstance {

    private static String R_CONST_Train_DATASET ="TrainDataset";
    private static String R_CONST_All_DATASET ="AllDataset";


    private static String modelName="linearModel";

    private int numInTrain;
    private int numInAllData;

    public rClassifiersTrainTestLinear(){
        loadLib();
        numInTrain=0;
        numInAllData=0;
    }


    private void loadLib()  {
        m_re.eval("library(Matrix)");
    }

    public void addInstanceToTrain(double[] instance) {
        assert instance.length==graphWrapperManager.m_dataStore.maxDistanceForFeatures+1;
        if (numInTrain == 0) {
            createMatrixFromInstance(instance, R_CONST_Train_DATASET);
        } else {
            m_re.assign("tempVector", instance);
            m_re.eval(R_CONST_Train_DATASET + " <- rBind(" + R_CONST_Train_DATASET + ",tempVector)");
            m_re.eval("rm(tempVector)");
        }
        numInTrain++;
    }


    private void createMatrixFromInstance(double[] instance,String matrixName) {
        m_re.assign("tempVector",instance);
        m_re.eval(matrixName+" <- matrix(tempVector,ncol=length(tempVector))");
        m_re.eval("rm(tempVector)");
    }

    private void rMatrixToDataFrame(String r_const_train_dataset) {
        m_re.eval(r_const_train_dataset + "<-data.frame(" + r_const_train_dataset + ")");
    }


    public double[] createModel()  {
        System.out.println("creating model");
        rMatrixToDataFrame(R_CONST_Train_DATASET);

        int targetIndex = graphWrapperManager.m_dataStore.maxDistanceForFeatures+1;
        String formula = "";
        for (int i = 1 ; i<graphWrapperManager.m_dataStore.maxDistanceForFeatures;i++){
            formula = formula + "X" + i + " + ";
        }

        formula = formula + "X" + (graphWrapperManager.m_dataStore.maxDistanceForFeatures);
        //m_re.eval("print(head(" + R_CONST_Train_DATASET + "))");
        System.out.println(modelName + " <- lm(X" + targetIndex + " ~ " + formula + ",data=" + R_CONST_Train_DATASET + ")");
        m_re.eval(modelName +" <- lm(X"+ targetIndex +" ~ " + formula + ",data="+R_CONST_Train_DATASET+")"); // dataa is data.frame
        //m_re.eval("print("+modelName+")");
        m_re.eval("print(anova("+modelName+"))");
        //m_re.eval("print(summary("+modelName + "))");

        REXP tmpAns = m_re.eval("predict(" + modelName + ")");
        assert tmpAns.asDoubleArray()!=null;
        double[] ans = tmpAns.asDoubleArray();

        m_re.eval("rm(" + R_CONST_Train_DATASET + ")");
        return ans;
    }

    public void addInstanceAllData(double[] instance) {
        assert instance.length==graphWrapperManager.m_dataStore.maxDistanceForFeatures; // not includes the target att
        if(numInAllData==0)
            createMatrixFromInstance(instance,R_CONST_All_DATASET);
        else {
            m_re.assign("tempVector", instance);
            m_re.eval(R_CONST_All_DATASET + " <- rBind(" + R_CONST_All_DATASET+ ",tempVector)");
            m_re.eval("rm(tempVector)");
        }
        numInAllData++;
    }

    public double[] classifyAllData() {

        double [] ans;
        System.out.println("predicting");
        rMatrixToDataFrame(R_CONST_All_DATASET);

        REXP tmpAns = m_re.eval("predict(" + modelName + "," + R_CONST_All_DATASET + ")");
        assert tmpAns.asDoubleArray()!=null;
        ans = tmpAns.asDoubleArray();

        m_re.eval("rm("+modelName+")");
        m_re.eval("rm("+R_CONST_All_DATASET+")");
        m_re.eval("rm("+R_CONST_All_DATASET+")");


        return ans;
    }

}
