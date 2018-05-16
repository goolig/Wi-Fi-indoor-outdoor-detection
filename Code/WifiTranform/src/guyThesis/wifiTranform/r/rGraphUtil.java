package guyThesis.wifiTranform.r;

import org.rosuda.JRI.REXP;

import java.util.List;

/**
 * Created by Guy on 04/03/2015.
 *
 */
public class rGraphUtil extends rInstance {


    private static final String R_CONST_MATRIXNAME = "nodesmatrix";
    private static final String R_CONST_GRAPHNAME = "graph";

    public rGraphUtil(int size){
        loadIgraph();
        m_re.eval(R_CONST_MATRIXNAME + "<- Matrix(0, nrow = "+size+", ncol = "+size+", sparse = TRUE)");
        m_re.waitForR();

    }

    public void setEdgesForNode(int nodeID, List<Integer> nodesList){
        assert nodesList!=null && !nodesList.contains(null) && !nodesList.contains(nodeID);
        nodeID++; // R start counting from 1
        for (Integer otherNode : nodesList) {
            otherNode++; // R starts counting from 1 in this matrix
            m_re.eval(R_CONST_MATRIXNAME + "[" + nodeID + ","+otherNode+"]=1");
        }
//            String otherNodes = nodesList.toString();
//            otherNodes = otherNodes.substring(1, otherNodes.length() - 1);
//            System.out.println(R_CONST_MATRIXNAME + "[" + nodeID + ",c(" + otherNodes + ")]<-1");
//            m_re.eval(R_CONST_MATRIXNAME + "[" + nodeID + ",c(" + otherNodes + ")]<-1");

    }

    private void loadIgraph() {
        m_re.eval("library(igraph)");
        m_re.eval("library(Matrix)");
    }




    public void createGraphElem() {
        m_re.eval(R_CONST_GRAPHNAME+ " <- graph.adjacency(" + R_CONST_MATRIXNAME+ ", mode = \"undirected\")");
        m_re.eval("rm("+R_CONST_MATRIXNAME+")");
    }


    public void close() {
        m_re.eval("rm("+R_CONST_GRAPHNAME+")");
        super.close();
        //m_re.eval("q()");
        //m_re.end();
        //m_re=null;

    }

    public double[] getSubGraphCentrality() {
        System.out.println("Calculating: subgraph.centrality");
        double[] ans = new double[0];
        REXP x =m_re.eval("subgraph.centrality(" + R_CONST_GRAPHNAME+ ")");
        if(x!=null)
            ans=x.asDoubleArray();
        return ans;
    }


    public double[] getBetweenness() {
        System.out.println("Calculating: betweenness");
        double[] ans = new double[0];
        REXP x =m_re.eval("betweenness(" + R_CONST_GRAPHNAME+ ", normalized = FALSE)");
        if(x!=null)
            ans=x.asDoubleArray();
        return ans;
    }
    public double[] getCloseness() {
        System.out.println("Calculating: closeness");
        double[] ans = new double[0];
        REXP x =m_re.eval("closeness(" + R_CONST_GRAPHNAME+ ", normalized = FALSE)");
        if(x!=null)
            ans=x.asDoubleArray();
        return ans;
    }

    public double[] getPageRank() {
        System.out.println("Calculating: page rank");
        double[] ans = new double[0];
        REXP x =m_re.eval("page.rank(" + R_CONST_GRAPHNAME+ ")$vector");
        if(x!=null)
            ans=x.asDoubleArray();
        return ans;
    }
    public double[] getEigenvector() {
        System.out.println("Calculating: evcent");
        double[] ans = new double[0];
        REXP x =m_re.eval("evcent(" + R_CONST_GRAPHNAME+ ",directed = FALSE, scale = TRUE)");
        if(x!=null)
            ans=x.asDoubleArray();
        return ans;
    }
    public double[] getAuthority() {
        System.out.println("Calculating: authority score");
        double[] ans = new double[0];
        REXP x =m_re.eval("authority.score(" + R_CONST_GRAPHNAME+ ",scale = FALSE)$vector");
        if(x!=null)
            ans=x.asDoubleArray();
        return ans;
    }
    public double[] getHub() {
        System.out.println("Calculating: hub score");
        double[] ans = new double[0];
        REXP x =m_re.eval("hub.score(" + R_CONST_GRAPHNAME+ ",scale = FALSE)$vector");
        if(x!=null)
            ans=x.asDoubleArray();
        return ans;
    }
    public double[] getAlphaCentrality() {
        System.out.println("Calculating: alpha centrality");
        double[] ans = new double[0];
        REXP x =m_re.eval("alpha.centrality(" + R_CONST_GRAPHNAME+ ")");
        if(x!=null)
            ans=x.asDoubleArray();
        return ans;
    }
    public double[] getBonacichPower() {
        System.out.println("Calculating: bonpow");
        double[] ans = new double[0];
        REXP x =m_re.eval("bonpow(" + R_CONST_GRAPHNAME+ "rescale=FALSE)");
        if(x!=null)
            ans=x.asDoubleArray();
        return ans;
    }
    public double[] getDegree() {
        System.out.println("Calculating: degree");
        double[] ans = new double[0];
        REXP x =m_re.eval("degree(" + R_CONST_GRAPHNAME+ ", mode=\"all\")"); //, normalized = TRUE
        if(x!=null)
            ans=x.asDoubleArray();
        return ans;
    }

    public double[] getClusteringCoefficient() {
        System.out.println("Calculating: transitivity");

        double[] ans = new double[0];
        REXP x =m_re.eval("transitivity(" + R_CONST_GRAPHNAME+ ",type=\"local\")"); // ,isolates="zero"
        if(x!=null)
            ans=x.asDoubleArray();
        for (int i = 0; i < ans.length; i++) {
            if ( Double.isNaN(ans[i]))
                ans[i] = -1;
        }

        return ans;
    }

    public void createGraphFromFile(String filePath){
        filePath=filePath.replace("\\","\\\\");
        System.out.println(filePath);
        m_re.eval("setwd(dirname(\"" + filePath + "\"))");
        //m_re.eval("print(dirname(\"" + filePath + "\"))");
        //m_re.eval("print(getwd())");
        m_re.eval(R_CONST_MATRIXNAME + " <-as.matrix( read.csv(basename(\"" + filePath+ "\"),header=FALSE,sep=\",\"))");
        m_re.eval("print(typeof("+R_CONST_MATRIXNAME+"))");
        m_re.eval(R_CONST_GRAPHNAME+ " <- graph.adjacency(" + R_CONST_MATRIXNAME+ ", mode = \"undirected\")");
        m_re.eval("rm("+R_CONST_MATRIXNAME+")");
    }

}
