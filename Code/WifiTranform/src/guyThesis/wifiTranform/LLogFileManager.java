package guyThesis.wifiTranform;

import guyThesis.Common.commonConstants;
import guyThesis.wifiTranform.Wifi.Wifi_Row;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by Guy on 21/04/2015.
 * holds records from the log file
 *
 */
public class LLogFileManager {
    private final List<logLine> m_logList;
    private final List<logLine> m_mistakeList;

    public LLogFileManager(String logFilePath) {
        assert new File(logFilePath).exists();
        m_logList = readCSVtoWifiLog(logFilePath);
        m_mistakeList = new ArrayList<logLine>(100);
        for (logLine logLine : m_logList) {
            if(logLine.isMistake())
                m_mistakeList.add(logLine);
        }
        mainTransform.log.log(Level.FINEST, "mistakes in log file:" + m_mistakeList.size());
    }


    public List<logLine> readCSVtoWifiLog(String filePath) {
        BufferedReader br = null;
        String line ;
        String cvsSplitBy = commonConstants.columnSeparator;
        List<logLine> logList = new ArrayList<logLine>();
        try {

            br = new BufferedReader(new FileReader(filePath));
            //skip first line
            br.readLine();
            while ((line = br.readLine()) != null) {

                // use columnSeparator as separator
                String[] CSVRow = line.split(cvsSplitBy,-1);
                if (CSVRow.length > 1) {
                    logLine currRow = new logLine(CSVRow);
                    logList.add(currRow);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            mainTransform.log.log(Level.FINEST, "Cannot read file!!!! is it opened??");
            System.exit(1);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        mainTransform.log.log(Level.FINEST, "Lines in log file: " + logList.size());
        return logList;

    }

    public void setMistakeIfNeededForRow(Wifi_Row wifi_row) {
        for (logLine logLine : m_mistakeList) {
            if(logLine.isInRange(wifi_row.getTSDate().getTime()))
            {
                wifi_row.setMistake();
                //System.out.println("mistake: " + wifi_row.getTSDate().toString() + " because: " + new Date(logLine.m_timeStamp).toString() + " for: " + logLine.m_value +" mins");
                break;
            }
        }
    }

    private class logLine{

        long m_timeStamp;
        commonConstants.logEvents m_eventType;
        String m_value;
        String m_comment;

        public logLine(String[] line){
            assert line!=null&&line.length==4;
            try{
                m_timeStamp = Long.parseLong(line[0]);
                m_eventType = commonConstants.logEvents.valueOf(line[1]);
                m_value=line[2];
                m_comment=line[3];
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        private int getLengthInSeconds (){
            assert isMistake();
            return Integer.parseInt(m_value) * 60;  // the mistake is in minutes
        }

        public long mistakeStartTS(){
            assert isMistake();
            return m_timeStamp - getLengthInSeconds() * 1000;
        }

        public boolean isMistake() {
            return m_eventType.equals(commonConstants.logEvents.mistake);
        }

        public boolean isInRange(long time) {
            return time >= mistakeStartTS() && time <=m_timeStamp;
        }
    }

}
