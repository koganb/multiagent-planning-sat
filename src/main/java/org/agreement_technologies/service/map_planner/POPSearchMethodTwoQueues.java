package org.agreement_technologies.service.map_planner;

import java.util.ArrayList;
import java.util.Hashtable;

public class POPSearchMethodTwoQueues implements POPSearchMethod {
    private static final int INITIAL_SIZE = 10000;
    private IPlan[] dtgQueue;
    private IPlan[] prefQueue;
    private Hashtable<String, Integer> dtgPlanPosition;
    private Hashtable<String, Integer> prefPlanPosition;
    private boolean firstQueue;
    private int dtgSize, prefSize;
    private PlanComparator dtgComparator;
    private PlanComparator prefComparator;

    public POPSearchMethodTwoQueues(POPIncrementalPlan initialIncrementalPlan) {
        dtgQueue = new IPlan[INITIAL_SIZE];
        prefQueue = new IPlan[INITIAL_SIZE];
        dtgPlanPosition = new Hashtable<String, Integer>(INITIAL_SIZE);
        prefPlanPosition = new Hashtable<String, Integer>(INITIAL_SIZE);
        firstQueue = true;
        dtgSize = prefSize = 0;
        dtgComparator = new DTGComparator();
        prefComparator = new PrefComparator();
        addToQueue(initialIncrementalPlan);
    }

    @Override
    public IPlan getNextPlan() {
        if (dtgSize == 0 && prefSize == 0) return null;
        if (dtgSize == 0 && firstQueue) firstQueue = false;
        if (prefSize == 0 && !firstQueue) firstQueue = true;
        IPlan min;
        if (firstQueue) {    // From the DTG queue
            min = dtgQueue[1];
        } else {
            min = prefQueue[1];
        }
        dtgSize = removePlan(min.getName(), dtgQueue, dtgSize, dtgPlanPosition, dtgComparator);
        prefSize = removePlan(min.getName(), prefQueue, prefSize, prefPlanPosition, prefComparator);
        firstQueue = !firstQueue;
        return min;
    }

    private int removePlan(String name, IPlan[] queue, int size, Hashtable<String, Integer> planPosition,
                           PlanComparator comp) {
        Integer k = planPosition.get(name);
        if (k == null) return size;
        int parent;
        IPlan plan = queue[k];
        IPlan ult = queue[size--];
        if (comp.compare(ult, plan) < 0) {
            while (k > 1 && comp.compare(ult, queue[k >> 1]) < 0) {
                parent = k >> 1;
                planPosition.put(queue[parent].getName(), k);
                queue[k] = queue[parent];
                k = parent;
            }
            queue[k] = ult;
            planPosition.put(ult.getName(), k);
        } else {
            queue[k] = ult;
            planPosition.put(ult.getName(), k);
            sink(k, queue, size, planPosition, comp);
        }
        return size;
    }

    private void sink(int gap, IPlan[] queue, int size,
                      Hashtable<String, Integer> planPosition, PlanComparator comp) {
        IPlan aux = queue[gap];
        int child = gap << 1;
        boolean ok = false;
        while (child <= size && !ok) {
            if (child != size && comp.compare(queue[child + 1], queue[child]) < 0)
                child++;
            if (comp.compare(queue[child], aux) < 0) {
                planPosition.put(queue[child].getName(), gap);
                queue[gap] = queue[child];
                gap = child;
                child = gap << 1;
            } else ok = true;
        }
        queue[gap] = aux;
        planPosition.put(aux.getName(), gap);
    }

    @Override
    public IPlan getNextPlanResume() {
        return checkNextPlan();
    }

    @Override
    public IPlan checkNextPlan() {
        if (dtgSize == 0 && prefSize == 0) return null;
        if (dtgSize == 0 && firstQueue) firstQueue = false;
        if (prefSize == 0 && !firstQueue) firstQueue = true;
        IPlan min;
        if (firstQueue) {    // From the DTG queue
            min = dtgQueue[1];
        } else {
            min = prefQueue[1];
        }
        return min;
    }

    @Override
    public void addSuccessors(ArrayList<IPlan> successors) {
        for (IPlan s : successors) {
            addToQueue(s);
        }
    }

    @Override
    public void addPlan(IPlan plan) {
        addToQueue(plan);
    }

    @Override
    public boolean isEmpty() {
        return dtgSize > 0 || prefSize > 0;
    }

    @Override
    public int size() {
        return dtgSize + prefSize;
    }

    @Override
    public void addSolution(IPlan solution) {
    }

    @Override
    public IPlan removePlan(String planName) {
        IPlan plan = getPlanByName(planName);
        if (plan != null) {
            dtgSize = removePlan(planName, dtgQueue, dtgSize, dtgPlanPosition, dtgComparator);
            prefSize = removePlan(planName, prefQueue, prefSize, prefPlanPosition, prefComparator);
        }
        return plan;
    }

    @Override
    public IPlan[] getFirstPlans(int n) {
        return null;
    }

    @Override
    public IPlan getPlanByName(String planName) {
        IPlan plan;
        Integer pos = dtgPlanPosition.get(planName);
        if (pos != null) plan = dtgQueue[pos];
        else {
            pos = prefPlanPosition.get(planName);
            if (pos != null) plan = prefQueue[pos];
            else return null;
        }
        return plan;
    }

    @Override
    public int getPublicValue(IPlan p) {
        return p.getG() + 2 * p.getH();
    }

    private void addToQueue(IPlan x) {
        if (x.getName() == null)
            throw new RuntimeException("Cannot add a plan without name");
        dtgSize = addToQueue(x, dtgQueue, dtgSize, dtgPlanPosition, dtgComparator);
        if (x.getFather() != null && x.getFather().getHLan() > x.getHLan()) // Preferred
            prefSize = addToQueue(x, prefQueue, prefSize, prefPlanPosition, prefComparator);
    }

    private int addToQueue(IPlan x, IPlan[] queue, int size, Hashtable<String, Integer> planPosition,
                           PlanComparator comp) {
        int gap = ++size, parent;
        if (size >= queue.length) {
            if (queue == dtgQueue) queue = dtgQueue = growQueue(queue);
            else queue = prefQueue = growQueue(queue);
        }
        while (gap > 1 && comp.compare(x, queue[gap >> 1]) < 0) {
            parent = gap >> 1;
            planPosition.put(queue[parent].getName(), gap);
            queue[gap] = queue[parent];
            gap = parent;
        }
        queue[gap] = x;
        planPosition.put(x.getName(), gap);
        return size;
    }

    private IPlan[] growQueue(IPlan[] queue) {
        IPlan[] newQueue = new IPlan[2 * queue.length];
        System.arraycopy(queue, 0, newQueue, 0, queue.length);
        return newQueue;
    }

    private interface PlanComparator {
        int compare(IPlan p1, IPlan p2);
    }

    private class DTGComparator implements PlanComparator {
        @Override
        public int compare(IPlan p1, IPlan p2) {
            //if(p1.getH() == 0 || p1.getH() == 0)
            //    return p1.getH() - p2.getH();
            int f1 = (p1.getH() << 1) + p1.getG();
            int f2 = (p2.getH() << 1) + p2.getG();
            return f1 - f2;
        }
    }

    private class PrefComparator implements PlanComparator {
        @Override
        public int compare(IPlan p1, IPlan p2) {
            return p1.getHLan() - p2.getHLan();
        }
    }
}
