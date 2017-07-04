package org.agreement_technologies.service.map_planner;

import java.io.Serializable;
import java.util.ArrayList;

public class NewBasePlanMessage implements Serializable {
    private static final long serialVersionUID = 8929938148396574560L;
    private String basePlanName;
    private ArrayList<HeuristicChange> hChanges;

    public NewBasePlanMessage() {
        hChanges = new ArrayList<HeuristicChange>();
    }

    public void Clear() {
        hChanges.clear();
    }

    public void setName(String name) {
        basePlanName = name;
    }

    public String getPlanName() {
        return basePlanName;
    }

    public void addAdjustment(String name, int incH) {
        hChanges.add(new HeuristicChange(name, incH));
    }

    public ArrayList<HeuristicChange> getChanges() {
        return hChanges;
    }

    public String toString() {
        return basePlanName + " - " + hChanges.toString();
    }

    public static class HeuristicChange implements Serializable {
        private static final long serialVersionUID = 5546547894487479208L;
        private String planName;
        private int incH;

        public HeuristicChange(String name, int h) {
            planName = name;
            incH = h;
        }

        public String getName() {
            return planName;
        }

        public int getIncH() {
            return incH;
        }

        public String toString() {
            return "(" + planName + "," + incH + ")";
        }
    }

}
