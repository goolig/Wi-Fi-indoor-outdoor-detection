package guyThesis.wifiTranform.Threads;


import guyThesis.wifiTranform.graphWifiNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Extends thread, and implements calculating distance for 2 nodes
 */
public class calcDistanceOnNodesThread extends Thread
{


    /**
     * creates a new calc distance thread and sets its params
     * @return the new thread
     */
    public static calcDistanceOnNodesThread getNewCalcDistanceThread(int size) {
        calcDistanceOnNodesThread Thread;
        Thread = new calcDistanceOnNodesThread(size);
        Thread.setPriority(java.lang.Thread.MAX_PRIORITY);
        return Thread;
    }

    public boolean reachedMax() {
        //return false; // just for testing
        return m_calcDistSet.size()>= (m_sizeOfTable*m_sizeOfTable/2) / Math.max((Runtime.getRuntime().availableProcessors() - 1),1); // leave 1 CPU out, but have at least 1
    }

    private class calcDist
    {

        graphWifiNode m_nodeA, m_nodeB;
        double[][] m_disMat;
        int m_i,m_j;
        private calcDist(graphWifiNode nodeA, graphWifiNode nodeB, double[][] disMat,int i, int j){
            m_nodeA=nodeA;
            m_nodeB=nodeB;
            m_disMat=disMat;
            m_i=i;
            m_j=j;
        }
        public void run() {
            assert !m_nodeA.equals(m_nodeB);
            double dist = m_nodeA.getDistanceFrom(m_nodeB);
            assert m_disMat[m_i][m_j]==0; // only 1 calculation per entry...
            m_disMat[m_i][m_j] = dist;
        }
    }

    List<calcDist> m_calcDistSet;
    int m_sizeOfTable;
    public calcDistanceOnNodesThread(int sizeOfTable){
        m_sizeOfTable=sizeOfTable;
        m_calcDistSet = new ArrayList<calcDist>();
    }

    public void run() {
        for(calcDist tmpDist:m_calcDistSet){
            tmpDist.run();
        }
    }

    public void addCalculation(graphWifiNode nodeA, graphWifiNode nodeB, double[][] disMat, int i, int j) {
        m_calcDistSet.add(new calcDist(nodeA,nodeB,disMat,i,j));
    }
}