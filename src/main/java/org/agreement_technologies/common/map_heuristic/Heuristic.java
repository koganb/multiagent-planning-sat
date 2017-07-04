package org.agreement_technologies.common.map_heuristic;

import java.util.ArrayList;
import java.util.BitSet;

public interface Heuristic {
    static final int INFINITE = (Integer.MAX_VALUE) / 3;
    static final int INFO_LANDMARKS = 1;

    void evaluatePlan(HPlan p, int threadIndex);

    void evaluatePlan(HPlan p, int threadIndex, ArrayList<Integer> achievedLandmarks);

    void evaluatePlanPrivacy(HPlan p, int threadIndex);

    void waitEndEvaluation();

    void startEvaluation(HPlan basePlan);

    Object getInformation(int infoFlag);

    boolean supportsMultiThreading();

    boolean requiresHLandStage();

    int numGlobalLandmarks();

    ArrayList<Integer> checkNewLandmarks(HPlan plan, BitSet achievedLandmarks);
}
