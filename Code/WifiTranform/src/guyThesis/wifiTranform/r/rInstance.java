package guyThesis.wifiTranform.r;


import org.rosuda.JRI.Rengine;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Guy on 04/03/2015.
 * Holds an R instance, and performs actions on it.
 */
public class rInstance {

    //protected final Logger log = Logger.getLogger(rInstance.class.getName());


    protected static Rengine m_re=null;
    protected static final String R_CONST_VECTORNAME = "vector";
    protected static final String R_CONST_TMP_Train_VECTOR = "tmpTrainVector";
    protected static final String R_CONST_TMP_Test_VECTOR = "tmpTestVector";


    /**
     * List of variables declared in R.
     */
    protected List<String> m_varTrainList = new ArrayList<String>();
    protected List<String> m_varTestList = new ArrayList<String>();



    public rInstance(){
        if(m_re==null) {
            //commonConstants.setLoggerThings(log);
            // just making sure we have the right version of everything
            if (!Rengine.versionCheck()) {
                System.err.println("** Version mismatch - Java files don't match library version. Better exit now...");
                m_re = null;
            }
            System.out.println("Creating REng");

            // 1) we pass the arguments from the command line
            m_re = new Rengine(new String[]{"--no-save"}, false, new TextConsole());
            System.out.println("Rengine created, waiting for R");
            // the engine creates R is a new thread, so we should wait until it's ready
            if (!m_re.waitForR()) {
                m_re = null;
                System.out.println("Cannot load R");
                return;
            }
            System.out.println("rengine was created");
        }
        else
        {
            System.out.println("R was created previously");
        }
    }

    public void resetSeed(){
        m_re.eval("set.seed(1234)");
    }


    protected void createMatrixUsingC(String s, String rConstMatrixname, int r, int c) {
        m_re.eval(cFunction(s,R_CONST_VECTORNAME));
        m_re.eval(rConstMatrixname +" <- matrix(" +R_CONST_VECTORNAME + ", nrow="+r +", ncol="+c+",byrow=FALSE)");
    }

    protected void rbindFunction(String args, String targetVarName) {
        System.out.println("Rbind");
        String expression = targetVarName + "<-rbind(";
        expression+=args;
        expression+= ")";
        m_re.eval(expression);
    }



    protected void rbindAndDatasetFunction(String args, String targetVarName) {
        String expression = targetVarName + "<-data.frame(rbind(";
        expression+=args;
        expression+= "))";
        m_re.eval(expression);
    }

    protected String cFunction(String args, String targetVarName) {
        String expression = targetVarName + "<-c(";
        expression+=args;
        expression+= ")";
        return expression;
    }

    protected String varTestListToString() {
        String expression="";
        for (int i = 0; i < m_varTestList.size()-1; i++) {
            expression+= m_varTestList.get(i) + ",";
        }
        if(m_varTestList.size()>0)
            expression += m_varTestList.get(m_varTestList.size()-1);
        return expression;
    }

    protected String addNewTestVariable(){
        int count = m_varTestList.size();
        String currentVar = R_CONST_TMP_Test_VECTOR + count;
        m_varTestList.add(count, currentVar);
        return currentVar;
    }
    protected String varTrainListToString() {
        String expression="";
        for (int i = 0; i < m_varTrainList.size()-1; i++) {
            expression+= m_varTrainList.get(i) + ",";
        }
        if(m_varTrainList.size()>0)
            expression += m_varTrainList.get(m_varTrainList.size()-1);
        return expression;
    }

    protected String addNewTrainVariable(){
        int count = m_varTrainList.size();
        String currentVar = R_CONST_TMP_Train_VECTOR + count;
        m_varTrainList.add(count, currentVar);
        return currentVar;
    }


    protected void finalize(){
       // close();
    }

    public void close() {
        //m_re.eval("library(pryr)");
        //m_re.eval("print(mem_used())");
        m_re.eval("rm(list=ls())");
        m_re.eval("gc()");
        //m_re.eval("library(pryr)");
        //m_re.eval("print(mem_used())");
        //m_re.eval("q()");
        //m_re.end();
        //m_re=null;

    }
    public void pause(int amount) {
        m_re.eval("Sys.sleep("+ amount +")");
    }
}
