package org.agreement_technologies.service.map_planner;

import java.util.ArrayList;
import java.util.BitSet;

public class InternalProposal {
    public IPlan plan;
    public BitSet achievedLandmarks;

    public InternalProposal(IPlan p) {
        this.plan = p;
        this.achievedLandmarks = null;
    }

    public InternalProposal(IPlan p, BitSet achievedLandmarks) {
        this.plan = p;
        this.achievedLandmarks = achievedLandmarks;
    }

    public void setAchievedLandmarks(ArrayList<Integer> list, int numlLandmarks) {
        achievedLandmarks = new BitSet(numlLandmarks);
        for (Integer index : list)
            achievedLandmarks.set(index);
    }
}
