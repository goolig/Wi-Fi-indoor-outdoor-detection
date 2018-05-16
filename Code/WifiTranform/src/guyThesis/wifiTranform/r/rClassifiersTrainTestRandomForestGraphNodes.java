package guyThesis.wifiTranform.r;


import guyThesis.wifiTranform.dataStoreAndConfig;
import guyThesis.wifiTranform.graphWifiNode;
import guyThesis.wifiTranform.graphWrapperManager;
import org.rosuda.JRI.REXP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Guy on 04/03/2015.
 * createModel graph nodes using r and 10 fold
 */
public class rClassifiersTrainTestRandomForestGraphNodes extends rInstance {

    protected static String R_CONST_TRAIN_CONTROL ="trControl";
    private static String R_CONST_Train_DATASET ="TrainDataset";
    private static String R_CONST_All_DATASET ="AllDataset";
    private static String R_CONST_COLHEADER="colheaders";

    private static String CONST_0_CODE = "X0";
    private static String CONST_1_CODE = "X1";

    private int numInTrain;
    private int numInAllData;

    /**
     * the data should be processed only once. the flag is used in classify data function
     */
    boolean classDataWasPreProc;


    // private HashMap<Integer,Integer> m_InstanceToLocationList= new HashMap<Integer, Integer>();

    //    private String [] m_packagesNamesForAlgorithms = {"caret","GAMens","logicFS","ipred","klaR","vbmp","ada","mboost","bst","gbm","caTools","glmnet","mda","kernlab","mgcv","gam","stats","MASS","hda","HDclassif","class","rrcov","rrlda","sda","SDDA","LogForest","LogicReg","RWeka","nnet","stepPlr","sparseLDA","earth","pamr","rda","RSNNS","gpls","pls","spls","Boruta","party","obliqueRF","randomForest","rFerns","RRF","evtree","nodeHarvest","obliqueTree","partDSA","rpart","rocc","kohonen","penalizedLDA"};
/*    private String [] m_algoNamesForTest = {
            "bagFDA","treebag","ada","blackboost","bstTree","bstLs",
            "glmboost","fda",*//*"gaussprRadial",*//*"gamLoess","gamSpline","glm",*//*"glmStepAIC",*//*"LMT","multinom",
            "plr","earth","gcvEarth","nnet","kernelpls","pls","simpls",*//*"widekernelpls","pda","pda2", Due to bad accuracy*//**//*"Boruta",*//*
            "rf"*//*,"RRF"*//*,"RRFglobal","ctree","ctree2","J48","JRip","OneR","PART",*//*"svmLinear",*//**//*"svmPoly",*//*
           *//* "svmRadial","svmRadialCost"*//*
    };*/
    private String [] m_algoNamesForTest = {"gbm"};
//    private String [] m_algoNamesForTest = {"bagFDA","treebag","ada","LMT","rf","RRFglobal","ctree","J48","JRip","OneR","PART"};

    private List<String> m_successfulBuiltModelList = new ArrayList<String>();


    public rClassifiersTrainTestRandomForestGraphNodes(){
        classDataWasPreProc=false;
        loadLib();
        numInTrain=0;
        numInAllData=0;
    }


    private void loadLib()  {
        //m_re.eval("detach(package:ggplot2)");
        //m_re.eval("detach(\"ggplot2\")");
        m_re.eval("library(caret)");
        m_re.eval("library(Matrix)");
        //m_re.eval("library(ISLR)");

       // m_re.eval("library(klaR)");
       // m_re.eval("library(MASS)");



    }


    private void createCVElementsByIndex(String indexes) {
        //m_re.eval("print("+indexes+")");
        m_re.eval(R_CONST_TRAIN_CONTROL + "=trainControl(method='cv',index = "+indexes+",summaryFunction=twoClassSummary, classProbs = TRUE)");
        //m_re.eval(R_CONST_TRAIN_CONTROL + "=trainControl(method='cv',summaryFunction=twoClassSummary, classProbs = TRUE)");
    }

    private void createCVElements10Fold() {
        m_re.eval(R_CONST_TRAIN_CONTROL + "=trainControl(method='cv', number=10,summaryFunction=twoClassSummary, classProbs = TRUE)");
    }


    public void addInstanceToTrain(double[] instance, int instanceWeight) {
        //instanceWeight=1;
        if (numInTrain == 0) {
            createMatrixFromInstance(instance, R_CONST_Train_DATASET);
            m_re.eval("tempWeight <- "+instanceWeight);

        } else {
            m_re.eval("tempWeight <- rBind(tempWeight,"+instanceWeight+")");
            m_re.assign("tempVector", instance);
            m_re.eval(R_CONST_Train_DATASET + " <- rBind(" + R_CONST_Train_DATASET + ",tempVector)");
            m_re.eval("rm(tempVector)");
        }
        numInTrain++;
    }


    public void addInstanceAllData(double[] instance) {
        if(numInAllData==0)
            createMatrixFromInstance(instance,R_CONST_All_DATASET);
        else {
            m_re.assign("tempVector", instance);
            m_re.eval(R_CONST_All_DATASET + " <- rBind(" + R_CONST_All_DATASET+ ",tempVector)");
            m_re.eval("rm(tempVector)");
        }
        numInAllData++;

    }

    private void createMatrixFromInstance(double[] instance,String matrixName) {
        m_re.assign("tempVector",instance);
        m_re.eval(matrixName+" <- matrix(tempVector,ncol=length(tempVector))");
        m_re.eval("rm(tempVector)");
    }

    public void setColNameArray(String[] colHeaderForInstance) {
        m_re.rniAssign(R_CONST_COLHEADER, m_re.rniPutStringArray(colHeaderForInstance), 0);
    }

    private void rMatrixToDataFrame(String r_const_train_dataset) {
        m_re.eval(r_const_train_dataset + "<-data.frame(" + r_const_train_dataset + ")");
    }

    private void rSetColNameArray(String name) {
        m_re.eval("names(" + name + ")<-" + R_CONST_COLHEADER);
    }



    public void createModel(boolean useBaseline)  {
        System.out.println("creating model");
        //printHeadAndTailOfData(R_CONST_Train_DATASET);
        rMatrixToDataFrame(R_CONST_Train_DATASET);
        rSetLastColAsFactor(R_CONST_Train_DATASET);
        rSetColNameArray(R_CONST_Train_DATASET);


        switch (graphWrapperManager.m_dataStore.validationMethod)
        {
            case locationBasedCV:
                createReSampleIndices(R_CONST_Train_DATASET);
                createCVElementsByIndex("for_model"); // trains the model using location fold cross validation
            break;
            case tenFoldCV:
                createCVElements10Fold(); // 10 fold cross validation
            break;
            default:
                assert false;
                break;

        }


        removeLocationCol(R_CONST_Train_DATASET);


        String trainDatasetNoClass = R_CONST_Train_DATASET +"[,-ncol("+ R_CONST_Train_DATASET +")]";//R_CONST_Train_DATASET+"[,-ncol("+R_CONST_Train_DATASET+")]";
        String trainDatasetOnlyClass = R_CONST_Train_DATASET +"$classification";//R_CONST_Train_DATASET+"[,ncol("+R_CONST_Train_DATASET+")]";


        for (String modelName : m_algoNamesForTest) {
            if(createModel(modelName, trainDatasetNoClass, trainDatasetOnlyClass,useBaseline)) {
                m_successfulBuiltModelList.add(modelName);
            }
        }

//       for (String model : m_successfulBuiltModelList) {
//           printModel(model);
//        }

        System.out.println(m_successfulBuiltModelList.toString());

        m_re.eval("rm(" + R_CONST_Train_DATASET + ")");
        m_re.eval("rm("+R_CONST_TRAIN_CONTROL+")");


    }




    private void removeLocationCol(String data) {
        m_re.eval(data+"$"+ graphWifiNode.features.location.name()+"<-NULL");
        m_re.eval("colnames(data)");
        //m_re.eval("print("+data+", quote = TRUE, row.names = FALSE)");

    }

    private void printHeadAndTailOfData(String data) {
        m_re.eval("print(head(" + data + "))");
        m_re.eval("print(tail("+data+"))");
    }

    private void rSetLastColAsFactor(String dataset) {
        String expression = dataset +"[,ncol("+ dataset +")]"+ "<-factor(";
        expression+=dataset + "[,ncol("+ dataset +")]";
        expression += ")";
        m_re.eval(expression);

        m_re.eval("levels(" + dataset + "[,ncol(" + dataset +")])[1] <- \""+CONST_0_CODE+"\"");
        m_re.eval("levels(" + dataset + "[,ncol(" + dataset +")])[2] <- \""+CONST_1_CODE+"\"");


    }

    private boolean createModel(String modelName,String trainDatasetNoClass, String trainDatasetOnlyClass, boolean useBaseline) {
        System.out.println("------------------" + modelName.toUpperCase() + "--------------------");
        if(modelName.equals("rf")) {
            //m_re.eval("print("+trainDatasetNoClass+")");
            //ntree = 1000
            //nodesize = the min number of instances in terminal nodes. default is 1
            //nodesize=2/
            if (useBaseline) {
                m_re.eval("newGrid = expand.grid(mtry = c(2))");
                m_re.eval(modelName + " = train(" + trainDatasetNoClass + "," + trainDatasetOnlyClass + ",method=\"" + modelName + "\", trControl=" + R_CONST_TRAIN_CONTROL + ", weights = tempWeight, metric =\"ROC\",tuneGrid = data.frame(mtry = 2),nodesize=4,ntree = 1000)"); //, metric = "Kappa" / "ROC" , ,preProc=c("center", "scale")
            } else {
                m_re.eval("newGrid = expand.grid(mtry = c(1))");
                m_re.eval(modelName + " = train(" + trainDatasetNoClass + "," + trainDatasetOnlyClass + ",method=\"" + modelName + "\", trControl=" + R_CONST_TRAIN_CONTROL + ", weights = tempWeight, metric =\"ROC\",tuneGrid = data.frame(mtry = 3), nodesize=4, ntree = 1000,allowParallel=TRUE)"); //, importance=TRUE, metric = "Kappa" / "ROC" , ,preProc=c("center", "scale")
            }
            //m_re.eval("print("+modelName+"$finalModel$importance)");
            //m_re.eval("print(importance("+modelName+"$finalModel,1))");
            //m_re.eval("print(importance("+modelName+"$finalModel,2))");

        }
        else {
            if (modelName.equals("nb")) {
                m_re.eval(modelName + " = train(" + trainDatasetNoClass + "," + trainDatasetOnlyClass + ",method=\"" + modelName + "\", trControl=" + R_CONST_TRAIN_CONTROL + ", weights = tempWeight, metric =\"ROC\",tuneGrid = data.frame(usekernel = TRUE, fL = 0))"); //, tuneLength = 3
                //m_re.eval("print(warnings())");
            }
            else
            {
                m_re.eval(modelName + " = train(" + trainDatasetNoClass + "," + trainDatasetOnlyClass + ",method=\"" + modelName + "\", trControl=" + R_CONST_TRAIN_CONTROL + ", weights = tempWeight, metric =\"ROC\")"); //, metric = "Kappa" / "ROC" , ,preProc=c("center", "scale")

            }

        }


        REXP exist = m_re.eval("exists(\""+modelName+"\")");
        return exist.asBool().isTRUE();
    }

    private void printModel(String modelName) {
        System.out.println("------------------" + modelName.toUpperCase() + "--------------------");
        m_re.eval("print("+modelName+")");
        m_re.eval("print("+modelName+"$finalModel)");

    }


    public double[] classifyAllData(boolean useClass) {

        double [] ans=null;
        System.out.println("predicting");
        if(!classDataWasPreProc) {
            rMatrixToDataFrame(R_CONST_All_DATASET);
            rSetLastColAsFactor(R_CONST_All_DATASET);

            rSetColNameArray(R_CONST_All_DATASET);
            removeLocationCol(R_CONST_All_DATASET);
            classDataWasPreProc=true;
        }
        String DatasetNoClass = R_CONST_All_DATASET +"[,-ncol("+ R_CONST_All_DATASET +")]";

        for (String modelName : m_algoNamesForTest) {
            ans = predictForProbability(modelName, DatasetNoClass,useClass);
            break;
            //TODO: something with better models. select the best model
        }
       // m_re.eval("rm(tmpPred)");
        //m_re.eval("rm("+R_CONST_All_DATASET+")");
        for (String modelName : m_algoNamesForTest) {
            //m_re.eval("rm("+modelName+")");
        }
        //m_re.eval("rm("+R_CONST_COLHEADER+")");
        m_re.eval("rm(tempWeight)");

        return ans;
    }

    private double[] predict(String modelName, String datasetNoClass) {
        m_re.eval("tmpPred <- predict(" + modelName + "$finalModel," + datasetNoClass + " , type = \"class\")"); // class / prob
        REXP predictions = m_re.eval("as.numeric(levels(tmpPred))[tmpPred]");
        assert predictions.asDoubleArray()!=null;
        return predictions.asDoubleArray();

    }

    private double[] predictForProbability(String modelName, String datasetNoClass,boolean useClass) {
        REXP predictions;
        if(useClass) {
            m_re.eval("tmpPred <- predict(" + modelName + "," + datasetNoClass + " , type = \"raw\")"); // class / prob
            //m_re.eval("print(tmpPred[,0])");
            m_re.eval("levels(tmpPred)[1] <- \"0\""); // convert X0 to 0
            m_re.eval("levels(tmpPred)[2] <- \"1\""); // convert X1 to 1
            predictions = m_re.eval("as.numeric(levels(tmpPred))[tmpPred]");
        }
        else
        {
            //m_re.eval("tmpPred <- predict(" + modelName + "$finalModel," + datasetNoClass + " , type = \"prob\")"); // class / prob
            m_re.eval("tmpPred <- predict(" + modelName + "," + datasetNoClass + " , type = \"prob\")"); // class / prob
            //m_re.eval("print(tmpPred[,1])");
            //m_re.eval("tmpPred$X0PROB <- tmpPred$X0 / (tmpPred$X0 + tmpPred$X1)");
            //m_re.eval("print(tmpPred)");

            predictions = m_re.eval("tmpPred$X0"); // only the first col. the first one has numbers, maybe we need the second... maybe by name

        }
        assert predictions.asDoubleArray() != null;
        return predictions.asDoubleArray();

    }

    private void createReSampleIndices(String r_const_train_dataset) {
        m_re.eval(r_const_train_dataset + "$" + graphWifiNode.features.location.name() + " <- as.factor(" + r_const_train_dataset + "$" + graphWifiNode.features.location.name() + ")");
        m_re.eval("groups <- levels("+r_const_train_dataset+"$"+graphWifiNode.features.location.name()+")");
        m_re.eval("for_model <- vector(mode = \"list\", length = length(groups))");
        m_re.eval("names(for_model) <- groups");
        m_re.eval("for(i in seq(along = groups)) \n for_model[[i]] <- which("+r_const_train_dataset + "$" + graphWifiNode.features.location.name()+" != groups[i])");

        m_re.eval("rm(groups)");

    }
}
