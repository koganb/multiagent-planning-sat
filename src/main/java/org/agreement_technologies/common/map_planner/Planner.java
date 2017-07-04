package org.agreement_technologies.common.map_planner;

/**
 * Common interface for a planner
 *
 * @author Alex
 */
public interface Planner {
    /**
     * Computes a solution plan
     */
    Plan computePlan(long start, long timeoutSeconds);

    int getIterations();

}
