package guyThesis.wifiTranform.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Guy on 19/03/2015.
 *
 */
public class majorityEvaluator {
    HashMap<String, Integer> m_lstForFindMax;
    public majorityEvaluator() {
        m_lstForFindMax= new HashMap<String, Integer>();

    }

    public void addNewObs(String value){
        if (!m_lstForFindMax.containsKey(value)) {
            m_lstForFindMax.put(value, 1);
        } else {
            Integer tmp = m_lstForFindMax.get(value);
            tmp++;
            m_lstForFindMax.put(value, tmp);
        }
    }

    public String getCommonValue(){
        String ans=null;
        Integer MaxInt = Integer.MIN_VALUE;
        for (Map.Entry<String, Integer> x : m_lstForFindMax.entrySet()) {
            if (x.getValue() > MaxInt) {
                MaxInt = x.getValue();
                ans = x.getKey();
            }
        }
        return ans;
    }
}
