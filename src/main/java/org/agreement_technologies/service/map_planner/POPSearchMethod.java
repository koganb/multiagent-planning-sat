package org.agreement_technologies.service.map_planner;

import java.util.ArrayList;

/**
 * Interface for the search method; manages the storage and extraction of partial order plans
 *
 * @author Alex
 */
public interface POPSearchMethod {
    IPlan getNextPlan();

    IPlan getNextPlanResume();

    IPlan checkNextPlan();

    void addSuccessors(ArrayList<IPlan> successors);

    void addPlan(IPlan plan);

    boolean isEmpty();

    int size();

    //boolean isRepeated(IPlan solution);
    void addSolution(IPlan solution);

    IPlan removePlan(String planName);

    //Returns the n first plans in the queue
    IPlan[] getFirstPlans(int n);

    //Returns the plan specified in planName
    IPlan getPlanByName(String planName);

    int getPublicValue(IPlan p);
}
