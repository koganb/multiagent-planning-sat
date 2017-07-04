package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_planner.Ordering;

/**
 * Models the ordering constraints on a partial order plan; implements the Ordering interface.
 * Parameters: steps s1 and s2 that form the ordering s1 -> s2
 *
 * @author Alejandro Torreño
 */
public class POPOrdering implements Ordering {
    private static final long serialVersionUID = 4827398571308475818L;
    private int step1;
    private int step2;

    /**
     * Builds the ordering data structure
     *
     * @param s1 Index of the step s1
     * @param s2 Index of the step s2
     */
    public POPOrdering(int s1, int s2) {
        this.step1 = s1;
        this.step2 = s2;
    }

    /**
     * Retrieves the step s1
     *
     * @return Step s1
     */
    public int getIndex1() {
        return this.step1;
    }

    /**
     * Retrieves the step s2
     *
     * @return Step s2
     */
    public int getIndex2() {
        return this.step2;
    }

    public String toString() {
        String res = new String();
        res += this.step1 + " -> " + this.step2;
        return res;
    }
}
