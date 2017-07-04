package org.agreement_technologies.common.map_planner;

import java.util.ArrayList;

/**
 * Common interface for partial order plans
 *
 * @author Alex
 */
public interface Plan {
    static final int REGULAR = 0;
    static final int CoDMAP_CENTRALIZED = 1;
    static final int CoDMAP_DISTRIBUTED = 2;

    static final int UNASSIGNED = -2;
    static final int INITIAL = -1;

    ArrayList<CausalLink> getCausalLinksArray();

    ArrayList<Step> getStepsArray();

    ArrayList<Ordering> getOrderingsArray();

    Step getInitialStep();

    Step getFinalStep();

    boolean isSolution();

    int numSteps();

    int countSteps();

    String getName();

    boolean isRoot();

    int getG();

    void setG(int g);

    int getH();

    double getMetric();

    int getHpriv(int prefIndex);

    int getHLan();

    void setH(int h, int hLan);

    void setHPriv(int h, int prefIndex);

    Plan getParentPlan();

    void printPlan(int output, String myagent, ArrayList<String> agents);

    int[] linearizePlan(int mode, ArrayList<String> agents);

}
