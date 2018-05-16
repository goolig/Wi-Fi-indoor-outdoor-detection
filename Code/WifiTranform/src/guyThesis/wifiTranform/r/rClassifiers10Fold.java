package guyThesis.wifiTranform.r;


/**
 * Created by Guy on 04/03/2015.
 * createModel graph nodes using r and 10 fold
 */
public class rClassifiers10Fold extends rInstance {

    protected static String R_CONST_TRAINCONTROL="trControl";


    public rClassifiers10Fold(){
        loadLib();
        createNFoldElements(10);
    }

    protected void createNFoldElements(int numOfFolds){
        m_re.eval(R_CONST_TRAINCONTROL + "=trainControl(method='cv',number="+numOfFolds+")");
    }

    private static String R_CONST_DATASET="Dataset";
    private static String R_CONST_COLHEADER="colheaders";

    private void loadLib()  {



            m_re.eval("library(caret)");
        m_re.eval("library(klaR)");
    }

    public void addInstance(double[] instance) {
        String currentVar = addNewTrainVariable();
        m_re.rniAssign(currentVar, m_re.rniPutDoubleArray(instance), 0);
    }

    public void setColNameArray(String[] colHeaderForInstance) {
        m_re.rniAssign(R_CONST_COLHEADER, m_re.rniPutStringArray(colHeaderForInstance), 0);
       // m_re.eval("print("+R_CONST_COLHEADER+")");


    }


    public void classify()  {
        System.out.println("classifying");
        rbindAndDatasetFunction(varTrainListToString(), R_CONST_DATASET);
        rSetLastColAsFactor(R_CONST_DATASET);
        rSetColNameArray();
        //createMatrixUsingC(varTrainListToString(), R_CONST_MATRIXNAME);

        String datasetNoClass = R_CONST_DATASET+"[,-ncol("+R_CONST_DATASET+")]";//R_CONST_DATASET+"[,-ncol("+R_CONST_DATASET+")]";
        String datasetWithClass =R_CONST_DATASET+"$classification";//R_CONST_DATASET+"[,ncol("+R_CONST_DATASET+")]";
/*
        m_re.eval("naivebayse = train("+datasetNoClass+","+datasetWithClass+",method=\"nb\", trControl=" + R_CONST_TRAIN_CONTROL+")");
        m_re.eval("print(naivebayse)");
        m_re.eval("print(naivebayse$finalModel)");
      //  m_re.eval("predictions <- predict(model$finalModel,"+ datasetNoClass+")");
      //  m_re.eval("ans <- table(predict(model$finalModel,"+datasetNoClass+")$class,"+datasetWithClass+")");
     //   m_re.eval("print(ans)");

        m_re.eval("ctreeFit = train("+datasetNoClass+","+datasetWithClass+",method=\"ctree\", trControl=" + R_CONST_TRAIN_CONTROL+")");
        m_re.eval("print(ctreeFit)");
        m_re.eval("print(ctreeFit$finalModel)");
        //   m_re.eval("plot(ctreeFit$finalModel)");




        m_re.eval("C45Fit = train("+datasetNoClass+","+datasetWithClass+",method=\"J48\", trControl=" + R_CONST_TRAIN_CONTROL+")");
        m_re.eval("print(C45Fit)");
        m_re.eval("print(C45Fit$finalModel)");


        m_re.eval("knnFit = train("+datasetNoClass+","+datasetWithClass+",method=\"knn\", trControl=" + R_CONST_TRAIN_CONTROL+")");
        m_re.eval("print(knnFit)");
        m_re.eval("print(knnFit$finalModel)");

        m_re.eval("rulesFit = train("+datasetNoClass+","+datasetWithClass+",method=\"PART\", trControl=" + R_CONST_TRAIN_CONTROL+")");
        m_re.eval("print(rulesFit)");
        m_re.eval("print(rulesFit$finalModel)");


        m_re.eval("svmFit  = train("+datasetNoClass+","+datasetWithClass+",method=\"svmLinear\", trControl=" + R_CONST_TRAIN_CONTROL+")");
        m_re.eval("print(svmFit)");
        m_re.eval("print(svmFit$finalModel)");


        m_re.eval("nnetFit = train("+datasetNoClass+","+datasetWithClass+",method=\"nnet\", trControl=" + R_CONST_TRAIN_CONTROL+")");
        m_re.eval("print(nnetFit)");
        m_re.eval("print(nnetFit$finalModel)");


          //m_re.eval("install.packages(\"randomForest\")");
*/
        /*
        m_re.eval("randomForestFit = train("+datasetNoClass+","+datasetWithClass+",method=\"rf\", trControl=" + R_CONST_TRAIN_CONTROL+")");
        m_re.eval("print(randomForestFit)");
        m_re.eval("print(randomForestFit$finalModel)");
*/
        m_re.eval("C45Fit = train("+datasetNoClass+","+datasetWithClass+",method=\"J48\", trControl=" + R_CONST_TRAINCONTROL+")");
        m_re.eval("print(C45Fit)");
        m_re.eval("print(C45Fit$finalModel)");


/*
        m_re.eval("resamps <- resamples(list(\n" +
                "  ctree=ctreeFit,\n" +
                "  C45=C45Fit,\n" +
                "  SVM=svmFit,\n" +
                "  KNN=knnFit,\n" +
                "  rules=rulesFit,\n" +
                "  NeuralNet=nnetFit,\n" +
                "  randomForest=randomForestFit))");
        m_re.eval("print(resamps)");
        m_re.eval("print(summary(resamps))");

        m_re.eval("difs <- diff(resamps)");
        m_re.eval("print(difs)");
        m_re.eval("print(summary(difs))");
*/

/*
        System.out.println("Press Enter to continue");
        try{System.in.read();}
        catch(Exception e){}*/
        m_re.end();

    }

    private void rSetColNameArray() {
      //  m_re.eval("print(names("+R_CONST_DATASET+"))");
        m_re.eval("names("+R_CONST_DATASET+")<-"+R_CONST_COLHEADER);
      //  m_re.eval("print(names("+R_CONST_DATASET + "))");




    }

    private void rSetLastColAsFactor(String dataset) {
        String expression = dataset +"[,ncol("+ dataset +")]"+ "<-factor(";
        expression+=dataset + "[,ncol("+ dataset +")]";
        expression+= ")";
        m_re.eval(expression);
    }


}
