package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_grounding.GroundedCond;
import org.agreement_technologies.common.map_grounding.GroundedTask;

import java.util.ArrayList;

/**
 * Plan comparator for the A* search method
 *
 * @author Alex
 */

public class POPComparatorASpeed implements POPComparator {
    private final double selfInterest;
    private final GroundedTask task;
    private final ArrayList<GroundedCond> preferences;
    private double total;

    public POPComparatorASpeed(GroundedTask t) {
        selfInterest = t.getSelfInterestLevel();
        task = t;
        preferences = t.getPreferences();

        total = 0.0;
        for (int i = 0; i < preferences.size(); i++) {
            total += task.getViolatedCost(i);
        }
    }

    public int compare(IPlan p1, IPlan p2) {
        int i;
        double hp1 = 0.0, hp2 = 0.0, res;

        for (i = 0; i < preferences.size(); i++) {
            hp1 += p1.getHpriv(i) * (task.getViolatedCost(i) / total);
            hp2 += p2.getHpriv(i) * (task.getViolatedCost(i) / total);
        }

        if (selfInterest == 0.0)
            return p1.getH() - p2.getH();
        if (selfInterest == 1.0) {
            res = hp1 - hp2;
            return (int) res;
        }

        res = p1.getH() * (1.0 - selfInterest) + hp1 * selfInterest
                - p2.getH() * (1.0 - selfInterest) - hp2 * selfInterest;

        return (int) res;
    }

    public int getPublicValue(IPlan p) {
        return p.getH();
    }
}
