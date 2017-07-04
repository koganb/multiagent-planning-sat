package org.agreement_technologies.common.map_landmarks;

import org.agreement_technologies.common.map_grounding.GroundedVar;

import java.util.ArrayList;

public interface LandmarkFluent {
    static final int PENALTY = 2;

    GroundedVar getVar();

    String getVarName();

    String getValue();

    ArrayList<String> getAgents();

    int getIndex();

    ArrayList<LandmarkAction> getTotalProducers();

    ArrayList<LandmarkAction> getProducers();

    int getLevel();

    boolean isGoal();
}
