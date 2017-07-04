package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_negotiation.NegotiationFactory;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.PriorityQueue;

/**
 * Manages the search tree for an A* search.
 * Parameters: plan priority queue, base plan.
 *
 * @author Alex
 */
class POPSearchMethodA implements POPSearchMethod {
    private IPlan[] planQueue;
    private IPlan basePlan;
    //private Memoization memoization;
    private POPComparator planComparator;
    private Hashtable<IPlan, Integer> planPosition;
    private int size;

    private PriorityQueue<IPlan> firstPlansQueue;
    private Hashtable<String, Integer> positions;

    /**
     * Constructor; builds the A* search manager
     *
     * @param initialIncrementalPlan Base plan of the search process
     * @param planComparator         Comparator used to evaluate the partial order plans and extract them in tha appropriate order
     */
    public POPSearchMethodA(IPlan initialIncrementalPlan, POPComparator planComparator) {
        this.planComparator = planComparator;
        this.planQueue = new IPlan[1000];
        this.basePlan = initialIncrementalPlan;
        this.planPosition = new Hashtable<IPlan, Integer>(1000);
        this.size = 0;
        addToQueue(this.basePlan);
        //memoization = new Memoization();

        firstPlansQueue = new PriorityQueue<IPlan>(NegotiationFactory.BORDA_PROPOSALS, this.planComparator);
        positions = new Hashtable<String, Integer>();
    }

    private void addToQueue(IPlan x) {
        if (x.getName() == null)
            throw new RuntimeException("Cannot add a plan without name.");

        if (size == planQueue.length - 1) growQueue();
        int gap = ++size, parent;
        while (gap > 1 && planComparator.compare(x, planQueue[gap >> 1]) < 0) {
            parent = gap >> 1;
            planPosition.put(planQueue[parent], gap);
            planQueue[gap] = planQueue[parent];
            gap = parent;
        }
        planQueue[gap] = x;
        planPosition.put(x, gap);
    }

    private void growQueue() {
        IPlan[] newQueue = new IPlan[2 * planQueue.length];
        System.arraycopy(planQueue, 0, newQueue, 0, planQueue.length);
        planQueue = newQueue;
    }

    /**
     * Retrieves the next partial order plan according to its F value
     *
     * @return Next plan; null if the queue is empty
     */
    public IPlan getNextPlan() {
        if (size == 0) return null;
        IPlan min = planQueue[1];
        planPosition.remove(min);
        planQueue[1] = planQueue[size--];
        sink(1);
        return min;
    }

    private void sink(int gap) {
        IPlan aux = planQueue[gap];
        int child = gap << 1;
        boolean ok = false;
        while (child <= size && !ok) {
            if (child != size && planComparator.compare(planQueue[child + 1], planQueue[child]) < 0)
                child++;
            if (planComparator.compare(planQueue[child], aux) < 0) {
                planPosition.put(planQueue[child], gap);
                planQueue[gap] = planQueue[child];
                gap = child;
                child = gap << 1;
            } else ok = true;
        }
        planQueue[gap] = aux;
        planPosition.put(aux, gap);
    }

    /**
     * Checks the following plan to be processed without extracting it
     *
     * @return
     */
    public IPlan checkNextPlan() {
        if (size == 0) return null;
        return planQueue[1];
    }

    /**
     * Adds to the queue the new plans generated when solving a flaw
     *
     * @param successors Array of successors generated when a flaw of the parent plan is solved
     */
    public void addSuccessors(ArrayList<IPlan> successors) {
        for (IPlan s : successors)
            addToQueue(s);
    }

    /**
     * Returns the number of plans currently stored by the manager
     */
    public int size() {
        return size;
    }

    /**
     * Checks if there are more plans in the search tree
     *
     * @return True if there are still plans in the search tree; false otherwise
     */
    public boolean isEmpty() {
        return size == 0;
    }
    /**
     * Checks if a plan has been previously generated on this execution of the planner
     * @param solution Solution plan
     * @return False, since it is not possible to repeat a solution in an A* search
     */
    /*
    public boolean isRepeated(IPlan solution) {
        return memoization.search((POPIncrementalPlan)solution) != null;
    }*/

    /**
     * It is not necessary to store previous solutions in an A* search, so the method does nothing
     *
     * @param solution Solution plan
     */
    public void addSolution(IPlan solution) {
    }

    /**
     * Gets the next plan to resume the search after finding a solution
     *
     * @return Next plan in the queue
     */
    public IPlan getNextPlanResume() {
        if (size == 0) return null;
        return planQueue[1];
    }

    public void addPlan(IPlan plan) {
        plan.setG(plan.numSteps());
        //memoization.add((POPIncrementalPlan)plan);
        addToQueue(plan);
    }

    public String toString() {
        String str = new String();

        str = "Plans stored: " + size;

        return str;
    }

    @Override
    public IPlan removePlan(String planName) {
        int k = planPosition.get(new POPIncrementalPlan(planName)), parent;
        IPlan plan = planQueue[k];
        IPlan ult = planQueue[size--];
        if (planComparator.compare(ult, plan) < 0) {
            while (k > 1 && planComparator.compare(ult, planQueue[k >> 1]) < 0) {
                parent = k >> 1;
                planPosition.put(planQueue[parent], k);
                planQueue[k] = planQueue[parent];
                k = parent;
            }
            planQueue[k] = ult;
            planPosition.put(ult, k);
        } else {
            planQueue[k] = ult;
            planPosition.put(ult, k);
            sink(k);
        }
        return plan;
    }

    @Override
    //Gets the n best plans in the search tree according to the comparation criteria
    //The plans are not removed from the search tree
    public IPlan[] getFirstPlans(int n) {
        if (size == 0) return null;
        int s = n, pos = 1, gap;
        if (size < n) s = size;

        IPlan[] res = new IPlan[s];
        //Priority queue used to sort the best plans in the heap
        //Add the head of the queue to the auxiliar structure
        firstPlansQueue.add(this.planQueue[pos]);
        positions.put(this.planQueue[pos].getName(), pos);

        //Explore the plan queue
        while (s > 0) {
            //Get the best plan in the auxiliar plan queue
            res[res.length - s] = firstPlansQueue.poll();
            //Retrieve plan position in the original heap
            pos = positions.get(res[res.length - s].getName());
            //Add the child nodes of the best plan in the heap to the auxiliar queue
            gap = pos << 1;
            if (size >= gap) {
                firstPlansQueue.add(this.planQueue[gap]);
                positions.put(this.planQueue[gap].getName(), gap);
                if (size >= (gap + 1)) {
                    firstPlansQueue.add(this.planQueue[gap + 1]);
                    positions.put(this.planQueue[gap + 1].getName(), gap + 1);
                }
            }
            s--;
        }

        firstPlansQueue.clear();
        positions.clear();

        return res;
    }

    @Override
    public IPlan getPlanByName(String planName) {
        for (IPlan p : planQueue)
            if (p != null)
                if (p.getName().equals(planName))
                    return p;
        return null;
    }

    @Override
    public int getPublicValue(IPlan p) {
        return this.planComparator.getPublicValue(p);
    }
}
