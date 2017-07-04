package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_planner.CausalLink;
import org.agreement_technologies.common.map_planner.Plan;
import org.agreement_technologies.common.map_planner.Step;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Manages the search tree for an IDA* search.
 * Parameters: plan stack, auxiliar queue, base plan, current F limit, next F limit.
 *
 * @Author Alex
 */
public class POPSearchMethodIDA implements POPSearchMethod {
    private ArrayDeque<IPlan> planStack;
    private PriorityQueue<IPlan> successors;
    private IPlan basePlan;
    private ArrayList<Plan> solutions;
    private int fLimit;
    private int nextFLimit;

    /**
     * Constructor; builds the IDA* search manager
     *
     * @param initialIncrementalPlan Base plan of the search
     * @param planComparator         Comparator to arrange plans that descend from the same father
     */
    POPSearchMethodIDA(IPlan initialIncrementalPlan, Comparator<IPlan> planComparator) {
        this.planStack = new ArrayDeque();
        this.successors = new PriorityQueue(100, planComparator);
        this.basePlan = initialIncrementalPlan;
        this.planStack.push(this.basePlan);
        this.fLimit = basePlan.getG() + basePlan.getH();
        this.nextFLimit = Integer.MAX_VALUE;
        this.solutions = new ArrayList<Plan>();
    }

    /**
     * Returns the next plan (in a LIFO fashion); if the stack is empty, it restarts the IDA* search with a new F limit
     *
     * @return Next plan in the stack; null if there are no more plans and no plans have been discarded in this search
     */
    public IPlan getNextPlan() {
        //If there are more plans, we extract the next one from the stack
        if (this.planStack.size() > 0) return this.planStack.pop();
            //If not, we restart the IDA* search with the new F limit
        else {
            //If the next F limit has not been modified, we have expanded the full search tree
            if (this.nextFLimit == Integer.MAX_VALUE)
                return null;
                //If not, we update the F limit and restart the search, returning the base plan
            else {
                this.fLimit = this.nextFLimit;
                this.nextFLimit = Integer.MAX_VALUE;
                //System.out.println("IDA* search reset. New limit: " + this.fLimit);
                return basePlan;
            }
        }
    }

    /**
     * Checks the following plan to be processed without extracting it
     */
    public IPlan checkNextPlan() {
        return this.planStack.peek();
    }

    /**
     * Stores into the search tree the successors generated when solving a flaw of a plan
     *
     * @param suc List of successors
     */
    public void addSuccessors(ArrayList<IPlan> suc) {
        for (IPlan p : suc) this.successors.add(p);
        while (this.successors.size() > 0) {
            //If the successor is within the F limit, we push it
            if ((this.successors.peek().getG() + this.successors.peek().getH()) <= this.fLimit)
                this.planStack.push(this.successors.poll());
                //If not, we store its F value as the new F limit (if it is lower than the current F limit) and we extract the plan
            else if ((this.successors.peek().getG() + this.successors.peek().getH()) < this.nextFLimit) {
                this.nextFLimit = this.successors.peek().getG() + this.successors.peek().getH();
                this.successors.poll();
            } else this.successors.poll();
        }
    }

    /**
     * Returns the number of plans currently stored by the manager
     *
     * @return Number of plans currently stored
     */
    public int size() {
        //If a restart can be applied, we avoid to return a 0 to allow the search to be restarted
        if (this.planStack.isEmpty() && this.nextFLimit != Integer.MAX_VALUE)
            return -1;
        //In any other case, we return the number of plans that remain in the stack
        return this.planStack.size();
    }

    /**
     * Checks if there are more plans in the search tree
     *
     * @return True if there are still plans in the search tree; false otherwise
     */
    public boolean isEmpty() {
        //If a restart can be applied, we avoid to return a 0 to allow the search to be restarted
        if (this.nextFLimit != Integer.MAX_VALUE)
            return false;
            //In any other case, we return the number of plans that remain in the stack
        else return this.planStack.isEmpty();
    }

    /**
     * Checks if a plan has been previously generated on this execution of the planner
     *
     * @param solution Solution plan
     * @return True if the plan is repeated; false if it is not
     */
    public boolean isRepeated(IPlan solution) {
        int i;
        Plan sol = solution;
        boolean res = false, exit;
        CausalLink cl;
        Step s;
        POPOrdering o;

        for (Plan p : solutions) {
            exit = false;
            if (p.getStepsArray().size() != sol.getStepsArray().size()) continue;
            if (p.getCausalLinksArray().size() != sol.getCausalLinksArray().size()) continue;
            for (i = 2; i < p.getStepsArray().size(); i++) {
                s = p.getStepsArray().get(i);
                if (!s.getActionName().equals(sol.getStepsArray().get(i).getActionName())) {
                    exit = true;
                    break;
                }
            }
            if (!exit) {
                for (i = 0; i < p.getCausalLinksArray().size(); i++) {
                    cl = p.getCausalLinksArray().get(i);
                    if (cl.getIndex1() != sol.getCausalLinksArray().get(i).getIndex1()) {
                        exit = true;
                        break;
                    }
                    if (cl.getIndex2() != sol.getCausalLinksArray().get(i).getIndex2()) {
                        exit = true;
                        break;
                    }
                    if (cl.getCondition().getVarCode() != sol.getCausalLinksArray().get(i).getCondition().getVarCode()) {
                        exit = true;
                        break;
                    }
                }
            }
            if (!exit) {
                for (i = 0; i < p.getOrderingsArray().size(); i++) {
                    o = (POPOrdering) p.getOrderingsArray().get(i);
                    /******if(!sol.getMatrix().checkOrdering(o.getStep1(), o.getStep2())) {
                     exit = true;
                     break;
                     }********/
                }
            }
            if (!exit) {
                res = true;
                break;
            }
        }

        return res;
    }

    /**
     * Stores a solution in the serach method
     *
     * @param solution Solution plan
     */
    public void addSolution(IPlan solution) {
        this.solutions.add((Plan) solution);
    }

    /**
     * Gets the next plan to resume the search after finding a solution
     *
     * @return Next plan in the queue
     */
    public IPlan getNextPlanResume() {
        //If there are more plans, we return the next one from the stack
        //We do not extract it, since the search will be restarted with another initial iteration
        if (this.planStack.size() > 0) return this.planStack.peek();
            //If not, we restart the IDA* search with the new F limit
        else {
            //If the next F limit has not been modified, we have expanded the full search tree
            if (this.nextFLimit == Integer.MAX_VALUE)
                return null;
                //If not, we update the F limit and restart the search, returning the base plan
            else {
                this.fLimit = this.nextFLimit;
                this.nextFLimit = Integer.MAX_VALUE;
                //System.out.println("IDA* search reset. New limit: " + this.fLimit);
                IPlan base = basePlan;
                this.planStack.add(base);
                return base;
            }
        }
    }

    public void addPlan(IPlan plan) {
        this.planStack.add(plan);
        //return true;
    }

    @Override
    public IPlan removePlan(String planName) {
        throw new RuntimeException("removePlan: Not implemented yet");
    }

    @Override
    public IPlan[] getFirstPlans(int n) {
        throw new UnsupportedOperationException("getFirstPlans: Not implemented yet.");
    }

    @Override
    public IPlan getPlanByName(String planName) {
        throw new UnsupportedOperationException("getPlanByName: Not supported yet.");
    }

    @Override
    public int getPublicValue(IPlan p) {
        throw new UnsupportedOperationException("getPublicValue: Not supported yet.");
    }
}
