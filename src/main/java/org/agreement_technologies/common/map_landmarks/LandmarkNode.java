package org.agreement_technologies.common.map_landmarks;

import java.util.ArrayList;

/**
 * @author Alex
 */
public interface LandmarkNode {
    public static final boolean SINGLE_LITERAL = true;
    public static final boolean DISJUNCTION = false;


    LandmarkFluent[] getFluents();

    boolean isGoal();

    boolean isSingleLiteral();

    LandmarkFluent getLiteral();

    int getIndex();

    void setIndex(int index);

    ArrayList<String> getAgents();

    void setAgents(ArrayList<String> agents);

    String identify();

    void setAntecessors(ArrayList<Integer> antecessors);

    LandmarkSet getDisjunction();

    ArrayList<LandmarkAction> getProducers();

    int setGlobalId(int globalIndex);

    int getGlobalId();
}
