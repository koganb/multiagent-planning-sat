package org.agreement_technologies.service.map_heuristic;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_grounding.GroundedTask;
import org.agreement_technologies.common.map_heuristic.HPlan;
import org.agreement_technologies.common.map_heuristic.Heuristic;

import java.util.ArrayList;
import java.util.BitSet;

public class BreadthHeuristic implements Heuristic {
    private int numPreferences;

    public BreadthHeuristic(AgentCommunication comm, GroundedTask gTask) {
        this.numPreferences = gTask.getPreferences().size();
    }

    @Override
    public void evaluatePlan(HPlan p, int threadIndex) {
        p.setH(p.numSteps() - 2, 0);
        for (int i = 0; i < numPreferences; i++)
            p.setHPriv(0, i);
    }

    @Override
    public void waitEndEvaluation() {
    }

    @Override
    public void startEvaluation(HPlan basePlan) {
    }

    @Override
    public Object getInformation(int infoFlag) {
        return null;
    }

    @Override
    public boolean supportsMultiThreading() {
        return false;
    }

    @Override
    public void evaluatePlanPrivacy(HPlan p, int threadIndex) {
        for (int i = 0; i < numPreferences; i++)
            p.setHPriv(0, i);
    }

    @Override
    public boolean requiresHLandStage() {
        return false;
    }

    @Override
    public void evaluatePlan(HPlan p, int threadIndex, ArrayList<Integer> achievedLandmarks) {
    }

    @Override
    public int numGlobalLandmarks() {
        return 0;
    }

    @Override
    public ArrayList<Integer> checkNewLandmarks(HPlan plan,
                                                BitSet achievedLandmarks) {
        return null;
    }
}
