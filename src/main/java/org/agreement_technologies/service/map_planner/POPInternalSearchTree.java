package org.agreement_technologies.service.map_planner;

import java.util.ArrayList;
import java.util.Stack;

/**
 * Manages the search tree for a Depth search.
 * Parameters: plan stack, base plan.
 *
 * @author Alex
 */
class POPInternalSearchTree {
    private Stack<POPInternalPlan> planStack;
    private POPInternalPlan basePlan;

    /**
     * Constructor; builds the depth search manager
     *
     * @param initialIncrementalPlan Base plan of the search process
     * @param planComparator         Comparator used to evaluate the partial order plans and extract them in tha appropriate order
     */
    public POPInternalSearchTree(POPInternalPlan initialIncrementalPlan) {
        this.planStack = new Stack<POPInternalPlan>();
        this.basePlan = initialIncrementalPlan;
        this.planStack.push(this.basePlan);
    }

    /**
     * Retrieves the next partial order plan according to its F value
     *
     * @return Next plan; null if the queue is empty
     */
    public POPInternalPlan getNextPlan() {
        return this.planStack.pop();
    }

    /**
     * Adds to the queue the new plans generated when solving a flaw
     *
     * @param successors Array of successors generated when a flaw of the parent plan is solved
     */
    public void addSuccessors(ArrayList<POPInternalPlan> successors) {
        for (POPInternalPlan s : successors)
            this.planStack.push(s);
    }

    public boolean isEmpty() {
        return this.planStack.isEmpty();
    }

    public void addPlan(POPInternalPlan plan) {
        this.planStack.push(plan);
    }

    public String toString() {
        String str = new String();

        str = "Plans stored: " + this.planStack.size();

        return str;
    }
}
