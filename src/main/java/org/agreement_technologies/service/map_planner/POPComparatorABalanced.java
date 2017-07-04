package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_grounding.GroundedCond;
import org.agreement_technologies.common.map_grounding.GroundedTask;

import java.util.ArrayList;

public class POPComparatorABalanced implements POPComparator {
    private final double selfInterest;
    private final GroundedTask task;
    private final ArrayList<GroundedCond> preferences;
    private double total;

    public POPComparatorABalanced(GroundedTask t) {
        selfInterest = t.getSelfInterestLevel();
        task = t;
        preferences = t.getPreferences();

        total = 0.0;
        for (int i = 0; i < preferences.size(); i++) {
            total += task.getViolatedCost(i);
        }
    }

    public int compare(IPlan p1, IPlan p2) {
        int h1 = p1.getH(), h2 = p2.getH(), i;
        if (selfInterest == 0.0)
            return (h1 << 1) + p1.getG() - (h2 << 1) - p2.getG();

        double hp1 = 0.0, hp2 = 0.0, res;
        for (i = 0; i < preferences.size(); i++) {
            hp1 += p1.getHpriv(i) * (task.getViolatedCost(i) / total);
            hp2 += p2.getHpriv(i) * (task.getViolatedCost(i) / total);
        }

        if (selfInterest == 1.0) {
            res = (2 * hp1) + p1.getG() - (2 * hp2) - p2.getG();
            return (int) res;
        }

        res = (h1 << 1) * (1.0 - selfInterest) + (hp1 * 2) * selfInterest + p1.getG()
                - (h2 << 1) * (1.0 - selfInterest) - (hp2 * 2) * selfInterest - p2.getG();
        return (int) res;
    }

    public int getPublicValue(IPlan p) {
        return p.getG() + 2 * p.getH();
    }
}