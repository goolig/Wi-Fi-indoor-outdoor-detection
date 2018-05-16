package guyThesis.wifiTranform.Threads;


import cern.colt.matrix.tint.impl.SparseIntMatrix2D;
import guyThesis.wifiTranform.Wifi.Wifi_Window;
import guyThesis.wifiTranform.graphWrapperManager;

import java.util.LinkedList;

/**
 * Extends thread, and implements calculating distance for 2 nodes
 */
public class calcDistanceOnWinThreadToMatrix extends Thread{
    static private final int s_maxCalculationPerThread=2000000;

    /**
     * creates a new calc distance thread and sets its params
     * @return the new thread
     * @param distMatrix the distance matrix
     */
    public static calcDistanceOnWinThreadToMatrix getNewCalcDistanceThread(SparseIntMatrix2D distMatrix) {
        calcDistanceOnWinThreadToMatrix Thread;
        Thread = new calcDistanceOnWinThreadToMatrix(distMatrix);
        Thread.setPriority(java.lang.Thread.MAX_PRIORITY);
        return Thread;
    }

    LinkedList<singleCalculation> m_calcDistSet;
    final SparseIntMatrix2D m_matrix;

    calcDistanceOnWinThreadToMatrix(SparseIntMatrix2D matrix){
        m_matrix=matrix;
        m_calcDistSet = new LinkedList<singleCalculation>();
    }

    public boolean reachedMax() {
        return m_calcDistSet.size()>=s_maxCalculationPerThread ;
    }

    public void addCalculation(Wifi_Window sourceWin, Wifi_Window targetWindow) {
        m_calcDistSet.add(new singleCalculation(sourceWin,targetWindow));
    }

    private class singleCalculation {
        public Wifi_Window m_sourceWin, m_targetWindow;
        private singleCalculation(Wifi_Window sourceWin, Wifi_Window targetWindow){
            m_sourceWin=sourceWin;
            m_targetWindow=targetWindow;
        }
    }

    public void run() {

        while (!m_calcDistSet.isEmpty()){
            singleCalculation tmpDist = m_calcDistSet.removeFirst();
            assert !tmpDist .m_sourceWin.equals(tmpDist .m_targetWindow);
            double dist = tmpDist.m_sourceWin.getSimilartyWith(tmpDist.m_targetWindow);
            assert m_matrix.get(tmpDist.m_sourceWin.getM_ID(),tmpDist.m_targetWindow.getM_ID())==0; // only 1 calculation per entry...
            if(dist> graphWrapperManager.m_dataStore.distanceThreshHold){
                synchronized(m_matrix){
                    m_matrix.setQuick(tmpDist.m_sourceWin.getM_ID(), tmpDist.m_targetWindow.getM_ID(), 1);
                }
            }

        }
        m_calcDistSet=null;
        //System.out.println("Thread is done: " + this.getName());
    }

}