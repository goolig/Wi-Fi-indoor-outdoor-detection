package guyThesis.wifiTranform.r;

import guyThesis.Common.commonConstants;

import java.util.List;

/**
 * Created by Guy on 04/03/2015.
 * creates a confusion matrix from a simple matrix and prints the details
 */
public class rEvaluation extends rInstance {

    private void loadLib()  {
        m_re.eval("library(caret)");
//        m_re.eval("library(ROCR)");
        m_re.eval("library(pROC)");

    }

    public rEvaluation() {
        loadLib();
    }

    public void printConfusionMatrixDetails(int[] matrix){
        assert matrix.length==4;
        m_re.eval("occur <- matrix(c("+matrix[0]+","+matrix[1]+","+matrix[2]+","+matrix[3]+"),ncol=2,byrow=TRUE)");
        m_re.eval("colnames(occur) <- c(\"0\",\"1\")");
        m_re.eval("rownames(occur) <- c(\"0\",\"1\")");
        //m_re.eval("print(occur)");
        m_re.eval("occur <- as.table(occur)");
        m_re.eval("confMatrix <- confusionMatrix(occur)");
        m_re.eval("print(confMatrix)");

        m_re.eval("rm(occur)");
        m_re.eval("rm(confMatrix)");


    }

    public void printAUCArea(List<Double> instancesPredictions, List<Integer> instanceClass) {

        if(instanceClass.contains(0)&&instanceClass.contains(1)) {
            m_re.assign("tmpPred", commonConstants.doubleArrayListToArray(instancesPredictions));
            m_re.assign("tmpClass", commonConstants.intArrayListToArray(instanceClass));

            //auc.tmp <- performance(pred,"auc"); auc <- as.numeric(auc.tmp@y.values)
/*
        m_re.eval("pred <- prediction(tmpPred,tmpClass)");
        m_re.eval("auc.tmp <- performance(pred,\"auc\")"); // auc
        m_re.eval("aucValue1 <- as.numeric(auc.tmp@y.values)");
        m_re.eval("print(\"auc\")");
        m_re.eval("print(aucValue1)");
*/

            m_re.eval("print(auc(tmpClass, tmpPred,direction=\"<\"))");
            m_re.eval("plot(roc(tmpClass, tmpPred))");
            m_re.eval("rm(tmpPred)");
            m_re.eval("rm(tmpClass)");
        }
        else
            m_re.eval("print(\"Not enough classes to calculate auc\")");
    }


}
