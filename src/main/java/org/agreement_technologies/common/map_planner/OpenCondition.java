package org.agreement_technologies.common.map_planner;


/**
 * Common interface for open conditions
 *
 * @author Alex
 */
public interface OpenCondition {
    public Condition getCondition();

    //public int getMinTime();
    public Step getStep();
}
