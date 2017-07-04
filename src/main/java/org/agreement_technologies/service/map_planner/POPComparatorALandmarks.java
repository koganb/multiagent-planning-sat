package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_grounding.GroundedCond;
import org.agreement_technologies.common.map_grounding.GroundedTask;

import java.util.ArrayList;

public class POPComparatorALandmarks implements POPComparator {
    private double selfInterest;
    private GroundedTask task;
    private ArrayList<GroundedCond> preferences;
    private double total;

    public POPComparatorALandmarks(GroundedTask t) {
        selfInterest = t.getSelfInterestLevel();
        task = t;
        preferences = t.getPreferences();
        total = 0.0;
        for (int i = 0; i < preferences.size(); i++) {
            total += task.getViolatedCost(i);
        }
    }

    public int compare(IPlan p1, IPlan p2) {
        int hLand1 = p1.getHLan(),
                hLand2 = p2.getHLan();
        int h1 = 2 * p1.getH() + p1.getG(),
                h2 = 2 * p2.getH() + p2.getG();

        if (hLand1 < hLand2) return -1;
        if (hLand1 > hLand2) return 1;
        if (h1 < h2) return -1;
        if (h1 > h2) return 1;

        return 0;

    	/*
    	int h1, h2, i;
    	if (landInfo) {
    		h1 = p1.getH() + p1.getHLan();
    		h2 = p2.getH() + p2.getHLan();
    	} else {
    		h1 = p1.getH(); 
    	    h2 = p2.getH();
    	}
    	if(selfInterest == 0.0)
            return (h1 << 1) + p1.getG() - (h2 << 1) - p2.getG();
        double hp1 = 0.0, hp2 = 0.0, res;
        for(i = 0; i < preferences.size(); i++) {
            hp1 += p1.getHpriv(i) * (task.getViolatedCost(i) / total);
            hp2 += p2.getHpriv(i) * (task.getViolatedCost(i) / total);
        }
        if(selfInterest == 1.0) {
            res = (2 * hp1) + p1.getG() - (2 * hp2) - p2.getG();
            return (int) res;
        }
        res = (h1 << 1) * (1.0 - selfInterest) + (hp1 * 2) * selfInterest + p1.getG() 
                - (h2 << 1) * (1.0 - selfInterest) - (hp2 * 2) * selfInterest - p2.getG();
        return (int) Math.signum(res);*/
    }
    
    /*
    public int compare(IPlan p1, IPlan p2) {
        int i;
        double hp1 = 0.0, hp2 = 0.0, res;

        for(i = 0; i < preferences.size(); i++) {
            hp1 += p1.getHpriv(i) * (task.getViolatedCost(i) / total);
            hp2 += p2.getHpriv(i) * (task.getViolatedCost(i) / total);
        }
        
        if(selfInterest == 0.0)
            return 2 * p1.getH() + p1.getHLan() + p1.getG() - 
            	   2 * p2.getH() - p2.getHLan() - p2.getG();
        if(selfInterest == 1.0) {
            res = 2 * hp1 + p1.getG() - 2 * hp2 - p2.getG();
            return (int) res;
        }
        
        res = 2 * ((p1.getH() + p1.getHLan()) * invSelfInterest + hp1 * selfInterest) + p1.getG() 
              - 2 * ((p2.getH() + p2.getHLan()) * invSelfInterest - hp2 * selfInterest) - p2.getG();
        
        return (int) res;
    }*/

    public int getPublicValue(IPlan p) {
        return 2 * p.getH() + p.getHLan() + p.getG();
    }
}
