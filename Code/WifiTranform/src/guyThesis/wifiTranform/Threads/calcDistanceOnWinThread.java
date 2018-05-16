package guyThesis.wifiTranform.Threads;


import guyThesis.wifiTranform.Utils.Pair;
import guyThesis.wifiTranform.Wifi.Wifi_Window;
import guyThesis.wifiTranform.graphWrapperManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Extends thread, and implements calculating distance for 2 nodes
 */
public class calcDistanceOnWinThread extends Thread{
    private static final int s_numOfAvailableCPU = 8;
    private static final int S_MIN_NUMBER_OF_CALCULATION_PER_THREAD = 0;

    /**
     * creates a new calc distance thread and sets its params
     * @return the new thread
     */
    public static calcDistanceOnWinThread getNewCalcDistanceThread(int maxSize) {
        calcDistanceOnWinThread Thread;
        Thread = new calcDistanceOnWinThread(maxSize);
        Thread.setPriority(java.lang.Thread.MAX_PRIORITY);
        return Thread;
    }

    ArrayList<singleCalculation> m_calcDistSet;
    List<Pair<Integer, Double>> m_matrix;

    calcDistanceOnWinThread(int maxSize){
        m_calcDistSet=new ArrayList<singleCalculation>(maxSize);

    }

    public void addCalculation(Wifi_Window sourceWin, Wifi_Window targetWindow) {
        m_calcDistSet.add(new singleCalculation(sourceWin, targetWindow));
    }

    public boolean reachedMax(int size) {
        return m_calcDistSet.size() > size / s_numOfAvailableCPU && m_calcDistSet.size() > S_MIN_NUMBER_OF_CALCULATION_PER_THREAD;
    }

    private class singleCalculation {
        public Wifi_Window m_sourceWin, m_targetWindow;
        private singleCalculation(Wifi_Window sourceWin, Wifi_Window targetWindow){
            m_sourceWin=sourceWin;
            m_targetWindow=targetWindow;
        }
    }

    public void run() {
        m_matrix=new ArrayList<Pair<Integer, Double>>(m_calcDistSet.size());
        for (singleCalculation calc : m_calcDistSet) {
            //assert !calc.m_sourceWin.equals(calc.m_targetWindow);
            double dist = calc.m_sourceWin.getSimilartyWith(calc.m_targetWindow);
            if (dist > graphWrapperManager.m_dataStore.distanceThreshHold) {
                m_matrix.add(new Pair<Integer, Double>(calc.m_targetWindow.getM_ID(), dist));
            }
        }
    }

    public List<Pair<Integer, Double>> getM_matrix() {
        return m_matrix;
    }

    //System.out.println("Thread is done: " + this.getName());
}

