package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_planner.*;
import org.agreement_technologies.service.tools.CustomArrayList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * Incremental plans for the external search tree; each plan stores a new action
 * w.r.t. its father
 *
 * @author Alex
 */
public class POPIncrementalPlan implements IPlan {
    private String name;
    private POPIncrementalPlan father;
    private Step step;
    private ArrayList<Ordering> orderings;
    private CausalLink[] causalLinks;
    private int g, h, hPriv[], hLan, numSteps;
    private double metric;
    private int[] totalOrder;

    private ExtendedPlanner POP;
    private boolean isSolution;



    public POPIncrementalPlan(POPInternalPlan p, POPIncrementalPlan prev,
                              ExtendedPlanner planner) {
        POP = planner;
        father = prev;
        if (prev != null) {
            step = p.getLatestStep();
            numSteps = father.numSteps + 1;
        }
        // Root
        else {
            step = POP.getInitialStep();
            numSteps = 2;
        }
        isSolution = false;
        orderings = p.getInternalOrderings();
        if (step == null && prev != null) {
            step = POP.getFinalStep();
            numSteps--;
            isSolution = true;
        }
        if (step != null) {
            causalLinks = new CausalLink[step.getPrecs().length];
            p.getInternalCausalLinks(causalLinks);
        }
        totalOrder = null;
        hPriv = null;
    }

    /**
     * Build a plan with the information included in the proposal
     *
     * @param pp            PlanProposal
     * @param basePlan      Base plan
     * @param configuration Planner factory
     */
    public POPIncrementalPlan(ProposalToSend pp, IPlan basePlan,
                              PlannerFactoryImp configuration, ExtendedPlanner planner) {
        POP = planner;
        father = (POPIncrementalPlan) basePlan;
        h = pp.getH();
        hLan = pp.getHLand();
        g = father.g + 1;
        step = pp.getStep(numSteps, configuration);
        numSteps = father.numSteps + (step != null ? 1 : 0);
        orderings = pp.getOrderings(configuration);
        causalLinks = pp.getCausalLinks(configuration, father, step, orderings);
        isSolution = pp.isSolution();
        totalOrder = null;
        metric = -1;
        hPriv = null;
    }

    // Constructor for plan searching
    public POPIncrementalPlan(String planName) {
        name = planName;
        totalOrder = null;
    }

    private static int linearization(POPIncrementalPlan p, int[] toPlan,
                                     int index, boolean[] visited,
                                     HashMap<Integer, ArrayList<Integer>> orderings,
                                     HashMap<Integer, POPIncrementalPlan> incPlans) {
        int s = p.getStep().getIndex();
        visited[s] = true;
        // Analyze causal links s1 -<v,d>-> s2 in p by recursively calling
        // linearization over s1
        // p adds an action and supports all its preconditions through
        // previously existing actions in the plan
        for (CausalLink cl : p.causalLinks) {
            if (!visited[cl.getIndex1()])
                index = linearization(incPlans.get(cl.getIndex1()), toPlan,
                        index, visited, orderings, incPlans);
        }
        ArrayList<Integer> prev = orderings.get(s);
        if (prev != null)
            for (Integer s1 : prev)
                if (!visited[s1])
                    index = linearization(incPlans.get(s1), toPlan, index,
                            visited, orderings, incPlans);
        if (s != 1)
            toPlan[index++] = s;
        return index;
    }

    // Adds all the causal links when expanding a plan
    public void calculateCausalLinks(POPIncrementalPlan[] a) {
        // The base plan does not include any causal links
        POP.getTotalCausalLinks().clear();
        if (!this.isRoot()) {
            POPIncrementalPlan aux = this;
            while (!aux.isRoot()) {
                for (CausalLink c : aux.getCausalLinks())
                    POP.getTotalCausalLinks().add(c);
                aux = aux.father;
            }
        }
        POP.setNumCausalLinks(POP.getTotalCausalLinks().size());
        POP.setModifiedCausalLinks(false);
    }

    // Adds all the causal links when expanding a plan
    public void calculateOrderings(POPIncrementalPlan[] a) {
        // The base plan does not include any causal links
        POP.getTotalOrderings().clear();
        if (!this.isRoot()) {
            POPIncrementalPlan aux = this;
            while (!aux.isRoot()) {
                for (Ordering o : aux.getOrderings())
                    // if(!POPIncrementalPlan.totalOrderings.includes(o))
                    POP.getTotalOrderings().add(o);
                aux = aux.father;
            }
        }
        POP.setNumOrderings(POP.getTotalOrderings().size());
        POP.setModifiedOrderings(false);
    }

    public CausalLink[] getCausalLinks() {
        return this.causalLinks;
    }

    public ArrayList<Ordering> getOrderings() {
        return this.orderings;
    }

    public Step getStep() {
        return this.step;
    }

    @Override
    public POPIncrementalPlan getFather() {
        return this.father;
    }

    @Override
    public CustomArrayList<CausalLink> getTotalCausalLinks() {
        return POP.getTotalCausalLinks();
    }

    @Override
    public ArrayList<Step> getTotalSteps() {
        ArrayList<Step> st = new ArrayList<Step>();
        POPIncrementalPlan aux = this;
        int bound;

        // st.add(aux.step);
        while (!aux.isRoot()) {
            if (aux.step != null)
                st.add(aux.step);
            aux = aux.father;
        }

        ArrayList<Step> sti = new ArrayList<Step>(st.size() + 2);
        sti.add(POP.getInitialStep());
        sti.add(POP.getFinalStep());
        if (st.get(0).getIndex() == 1)
            bound = 1;
        else
            bound = 0;
        for (int i = st.size() - 1; i >= bound; i--)
            sti.add(st.get(i));

        return sti;
    }

    @Override
    public double getMetric() {
        if (metric == -1) {
            metric = ((Planner) POP).evaluateMetric(this);
        }

        return metric;
    }

    void setMetric(double metric) {
        this.metric = metric;
    }

    @Override
    public CustomArrayList<Ordering> getTotalOrderings() {
        return POP.getTotalOrderings();
    }

    @Override
    public Step getInitialStep() {
        return POP.getInitialStep();
    }

    @Override
    public Step getFinalStep() {
        return POP.getFinalStep();
    }

    @Override
    public boolean isSolution() {
        return isSolution;
    }

    @Override
    public int numSteps() {
        return numSteps;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(int n, Plan father) {
        if (isRoot())
            this.name = "\u03A0" + "0";
        else
            this.name = this.father.getName() + "-" + n;
    }

    @Override
    public boolean isRoot() {
        return father == null;
    }

    @Override
    public int getG() {
        return g;
    }

    @Override
    public void setG(int g) {
        this.g = g;
    }

    @Override
    public int getH() {
        return h;
    }

    @Override
    public String toString() {
        String res = this.name + " (H=" + this.h + ",Hl=" + this.hLan + ",Hp=";
        if (hPriv == null || hPriv.length == 0)
            res += "none";
        else {
            res += hPriv[0];
            for (int i = 1; i < hPriv.length; i++)
                res += "," + hPriv[i];
        }
        return res + ")";
    }

    public ArrayList<CausalLink> getCausalLinksArray() {
        POPIncrementalPlan aux = this;
        ArrayList<CausalLink> cl = new ArrayList<CausalLink>();

        while (!aux.isRoot()) {
            if (aux.getCausalLinks() != null)
                for (CausalLink l : aux.getCausalLinks())
                    cl.add(l);
            aux = aux.father;
        }

        return cl;
    }

    public ArrayList<Step> getStepsArray() {
        POPIncrementalPlan aux = this;
        ArrayList<Step> s = new ArrayList<Step>();
        ArrayList<POPIncrementalPlan> p = new ArrayList<POPIncrementalPlan>();
        while (!aux.isRoot()) {
            p.add(aux);
            aux = aux.father;
        }
        s.add(POP.getInitialStep());
        s.add(POP.getFinalStep());
        for (int i = p.size() - 1; i >= 0; i--)
            if (!p.get(i).isSolution)
                s.add(p.get(i).getStep());
        return s;
    }

    public ArrayList<Ordering> getOrderingsArray() {
        POPIncrementalPlan aux = this;
        ArrayList<Ordering> or = new ArrayList<Ordering>();

        while (!aux.isRoot()) {
            if (aux.getOrderings() != null)
                for (Ordering o : aux.getOrderings())
                    or.add(o);
            aux = aux.father;
        }

        return or;
    }

    @Override
    public boolean equals(Object x) {
        return ((POPIncrementalPlan) x).name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public int[] linearization() {
        if (totalOrder != null)
            return totalOrder;
        int index = 0;
        totalOrder = new int[numSteps - 1];
        boolean visited[] = new boolean[numSteps];
        HashMap<Integer, ArrayList<Integer>> orderings = new HashMap<Integer, ArrayList<Integer>>(
                numSteps);
        HashMap<Integer, POPIncrementalPlan> incPlans = new HashMap<Integer, POPIncrementalPlan>(
                numSteps);
        for (POPIncrementalPlan p = this; p != null; p = p.father) {
            // Store orderings in variable "orderings" (adjacency list)
            for (Ordering o : p.orderings) {
                ArrayList<Integer> prev = orderings.get(o.getIndex2());
                if (prev == null) {
                    prev = new ArrayList<Integer>();
                    prev.add(o.getIndex1());
                    orderings.put(o.getIndex2(), prev);
                } else if (!prev.contains(o.getIndex1()))
                    prev.add(o.getIndex1());
            }
            // incPlans stores the incremental plans that add a step to the plan
            incPlans.put(p.step.getIndex(), p);
        }
        // For each incremental plan in incPlans, call linearization (recursive
        // method)
        for (POPIncrementalPlan p = this; p.father != null; p = p.father)
            if (!visited[p.step.getIndex()]) {
                index = linearization(p, totalOrder, index, visited, orderings,
                        incPlans);
            }
        // System.out.println("Linear " + getName() + ": " +
        // java.util.Arrays.toString(toPlan));
        return totalOrder;
        /*
		 * ArrayList<Integer> toPlan = new ArrayList<Integer>(numSteps); boolean
		 * inserted[] = new boolean[numSteps]; toPlan.add(0); toPlan.add(1);
		 * inserted[0] = true; inserted[1] = true; POPIncrementalPlan aux =
		 * this; while (aux.father != null) { for (CausalLink cl:
		 * aux.causalLinks) { if (cl == null || cl.getStep1() == null)
		 * System.out.println(); if (inserted[cl.getIndex1()]) { if
		 * (inserted[cl.getIndex2()]) ensureOrder(toPlan, cl.getIndex1(),
		 * cl.getIndex2()); else { insertAfter(toPlan, cl.getIndex1(),
		 * cl.getIndex2()); inserted[cl.getIndex2()] = true; } } else if
		 * (inserted[cl.getIndex2()]) { insertBefore(toPlan, cl.getIndex1(),
		 * cl.getIndex2()); inserted[cl.getIndex1()] = true; } else {
		 * toPlan.add(1, cl.getIndex1()); inserted[cl.getIndex1()] = true;
		 * toPlan.add(2, cl.getIndex2()); inserted[cl.getIndex2()] = true; } }
		 * aux = aux.father; } aux = this; while (aux.father != null) { for
		 * (Ordering o: aux.orderings) { if (inserted[o.getStep1()]) { if
		 * (inserted[o.getStep2()]) ensureOrder(toPlan, o.getStep1(),
		 * o.getStep2()); else { insertAfter(toPlan, o.getStep1(),
		 * o.getStep2()); inserted[o.getStep2()] = true; } } else if
		 * (inserted[o.getStep2()]) { insertBefore(toPlan, o.getStep1(),
		 * o.getStep2()); inserted[o.getStep1()] = true; } else { toPlan.add(1,
		 * o.getStep1()); inserted[o.getStep1()] = true; toPlan.add(2,
		 * o.getStep2()); inserted[o.getStep2()] = true; } } aux = aux.father; }
		 * int res[] = new int[toPlan.size()]; for (int i = 0; i < res.length;
		 * i++) res[i] = toPlan.get(i); return res;
		 */
    }

    @Override
    public HashMap<String, String> computeState(int[] totalOrder,
                                                PlannerFactory pf) {
        HashMap<String, String> varValue = new HashMap<String, String>();
        ArrayList<Step> stepList = getStepsArray();
        Step a;
        for (int step : totalOrder) {
            a = stepList.get(step);
            for (Condition eff : a.getEffs()) {
                String var = pf.getVarNameFromCode(eff.getVarCode());
                if (var != null) {
                    String value = pf.getValueFromCode(eff.getValueCode());
                    if (value != null)
                        varValue.put(var, value);
                }
            }
        }
        return varValue;
    }

    @Override
    public HashMap<String, ArrayList<String>> computeMultiState(
            int[] totalOrder, PlannerFactory pf) {
        HashMap<String, ArrayList<String>> varValue = new HashMap<String, ArrayList<String>>();
        ArrayList<Step> stepList = getStepsArray();
        String v, value;
        Step a;
        ArrayList<String> list;
        for (int step : totalOrder) {
            a = stepList.get(step);
            for (Condition eff : a.getEffs()) {
                v = pf.getVarNameFromCode(eff.getVarCode());
                if (v != null) {
                    value = pf.getValueFromCode(eff.getValueCode());
                    if (value != null) {
                        if (varValue.containsKey(v)) {
                            list = varValue.get(v);
                            list.set(0, value);
                        } else {
                            list = new ArrayList<String>();
                            list.add(value);
                            varValue.put(v, list);
                        }
                    }
                }
            }
        }
        return varValue;
    }

    @Override
    public Step lastAddedStep() {
        return step;
    }

    // Counts the steps of a solution plan (excluding inital and final step)
    @Override
    public int countSteps() {
        int n = 0;
        POPIncrementalPlan father = this;

        while (!father.isRoot()) {
            if (father.getStep() != null)
                n++;
            father = father.getFather();
        }
        // Do not count the initial step
        return n - 1;
    }

    @Override
    public int getHpriv(int prefIndex) {
        if (hPriv == null)
            return 0;
        return hPriv[prefIndex];
    }

    @Override
    public int getHLan() {
        return hLan;
    }

    @Override
    public void setH(int h, int hLan) {
        this.h = h;
        this.hLan = hLan;
    }

    @Override
    public void setHPriv(int h, int prefIndex) {
        if (hPriv == null)
            hPriv = new int[prefIndex + 1];
        else if (hPriv.length <= prefIndex) {
            int tmp[] = new int[prefIndex + 1];
            System.arraycopy(hPriv, 0, tmp, 0, hPriv.length);
            hPriv = tmp;
        }
        hPriv[prefIndex] = h;
    }

    private int calculateSteps() {
        int steps = 0;
        POPIncrementalPlan father = this;
        while (father != null) {
            steps++;
            father = father.getFather();
        }

        return steps;
    }

    @SuppressWarnings("unchecked")
    public double computeMakespan() {
        if (this.g == 0)
            g = this.calculateSteps();

        ArrayList<Integer> adjacents[] = new ArrayList[g];
        for (int i = 0; i < adjacents.length; i++)
            adjacents[i] = new ArrayList<Integer>();
        ArrayList<Ordering> orderings = getOrderingsArray();
        ArrayList<CausalLink> causalLinks = getCausalLinksArray();
        for (Ordering po : orderings)
            if (!adjacents[po.getIndex1()].contains(po.getIndex2()))
                adjacents[po.getIndex1()].add(po.getIndex2());
        for (CausalLink cl : causalLinks)
            if (!adjacents[cl.getIndex1()].contains(cl.getIndex2()))
                adjacents[cl.getIndex1()].add(cl.getIndex2());
        int distance[] = new int[adjacents.length];
        computeMaxDistance(0, distance, adjacents);
        if (step.getIndex() == 1) { // Final step in the plan
            return distance[1] - 1;
        } else {
            int max = 1;
            for (int i = 2; i < distance.length; i++)
                if (distance[max] < distance[i])
                    max = i;
            return distance[max];
        }
    }

    private void computeMaxDistance(int v, int distance[],
                                    ArrayList<Integer> adjacents[]) {
        for (Integer w : adjacents[v])
            if (distance[w] <= distance[v]) {
                distance[w] = distance[v] + 1;
                computeMaxDistance(w, distance, adjacents);
            }
    }

    @Override
    public Plan getParentPlan() {
        return father;
    }

    @Override
    public int[] computeCodeState(int[] totalOrder, int numVars) {
        int state[] = new int[numVars];
        ArrayList<Step> stepList = getStepsArray();
        Step a;
        for (int step : totalOrder) {
            a = stepList.get(step);
            for (Condition eff : a.getEffs()) {
                state[eff.getVarCode()] = eff.getValueCode();
            }
        }
        return state;
    }

    @Override
    public void printPlan(int output, String myAgent, ArrayList<String> agents) {
        int i, j, makespan = 0;
        //boolean found;
        int[] actions;
        ArrayList<Step> steps = getTotalSteps();

        //Calculate plan linearization
        switch (output) {
            case Plan.CoDMAP_CENTRALIZED:
                actions = linearizePlan(Plan.CoDMAP_CENTRALIZED, agents);
                break;
            case Plan.CoDMAP_DISTRIBUTED:
                actions = linearizePlan(Plan.CoDMAP_DISTRIBUTED, agents);
                break;
            default:
                actions = linearizePlan(Plan.REGULAR, agents);
                break;
        }

        for (i = 0; i < actions.length; i++) {
            if (actions[i] > makespan)
                makespan = actions[i];
        }

        //Print the plan
        //CoDMAP distributed format
        //Print only the sequence of actions of the current agent; print no-ops when necessary

        if (output == Plan.CoDMAP_DISTRIBUTED) {
            for (i = 0; i <= makespan; i++) {
                //found = false;
                for (j = 2; j < actions.length; j++) {
                    if (actions[j] == i && steps.get(j).getAgent().equals(myAgent)) {
                        System.out.println(String.format("%10s - %2d: (%s)", steps.get(j).getUuid(), i, steps.get(j).getActionName()));
                        //found = true;
                    }
                }
                //if(!found)
                //    System.out.println("(no-op)");
            }
        }
        //Regular and CoDMAP Centralized format
        //Print actions of the plan ordered by time step
        else {
            //Only the first agent in the list shall print the plan
            if (myAgent.equals(agents.get(0))) {
                for (i = 0; i <= makespan; i++) {
                    for (j = 2; j < actions.length; j++) {
                        if (actions[j] == i) {
                            if (output == Plan.CoDMAP_CENTRALIZED)
                                System.out.println("(" + steps.get(j).getActionName() + ")");
                            else
                                System.out.println(i + ": (" + steps.get(j).getActionName() + ")");
                        }
                    }
                }
            }
        }
    }

    public int[] linearizePlan(int mode, ArrayList<String> agents) {
        int[] actions = new int[this.getTotalSteps().size()];
        int i, level, assigned;
        boolean assign;
        ArrayList<Integer> pre;
        ArrayList<Step> steps = getTotalSteps();
        //Predecessors hash table: given a step, all its predecessors are stored
        Hashtable<Integer, ArrayList<Integer>> predecessors = new Hashtable<Integer, ArrayList<Integer>>();
        for (i = 0; i < steps.size(); i++) {
            predecessors.put(i, new ArrayList<Integer>());
            //Initialize actions structure (stores the level of each action)
            actions[i] = Plan.UNASSIGNED;
        }
        actions[0] = Plan.INITIAL;
        //Add predecessors by analyzing orderings
        for (Ordering o : this.getTotalOrderings()) {
            predecessors.get(o.getIndex2()).add(o.getIndex1());
        }
        for (CausalLink l : this.getTotalCausalLinks()) {
            predecessors.get(l.getStep2().getIndex()).add(l.getStep1().getIndex());
        }

        switch (mode) {
            //CoDMAP competition - centralized track format (linear plan)
            case Plan.CoDMAP_CENTRALIZED:
                for (level = 0; level <= actions.length - 2; level++) {
                    for (i = 2; i < actions.length; i++) {
                        //If the action does not have an assigned level, check the maximum level of its predecessors
                        if (actions[i] == Plan.UNASSIGNED) {
                            assign = true;
                            pre = predecessors.get(i);
                            for (int p : pre) {
                                //If a predecessor does not have an assigned level, action i cannot be scheduled yet
                                if (actions[p] == Plan.UNASSIGNED) {
                                    assign = false;
                                    break;
                                }
                            }
                            //Schedule action i and break to the next iteration
                            if (assign) {
                                actions[i] = level;
                                break;
                            }
                        }
                    }
                }
                break;
            //CoDMAP competition - distributed track format (a sequence of actions per agent)
            case Plan.CoDMAP_DISTRIBUTED:
                int[] succSequenceSizes = new int[actions.length];
                for (i = 0; i < succSequenceSizes.length; i++)
                    succSequenceSizes[i] = -1;
                Hashtable<Integer, ArrayList<Integer>> successors = new Hashtable<Integer, ArrayList<Integer>>();
                for (i = 0; i < steps.size(); i++)
                    successors.put(i, new ArrayList<Integer>());
                //Add successors by analyzing orderings
                for (Ordering o : this.getTotalOrderings())
                    successors.get(o.getIndex1()).add(o.getIndex2());
                for (CausalLink l : this.getTotalCausalLinks())
                    successors.get(l.getStep1().getIndex()).add(l.getStep2().getIndex());
                //Base case
                for (i = 2; i < succSequenceSizes.length; i++)
                    if (successors.get(i).isEmpty())
                        succSequenceSizes[i] = 0;

                ArrayList<Integer> sequence;
                int j, max;
                boolean exit, completed = false, notCalculated;
                while (!completed) {
                    for (i = 2; i < succSequenceSizes.length; i++) {
                        if (succSequenceSizes[i] == -1) {
                            sequence = successors.get(i);

                            exit = false;
                            for (j = 0; j < sequence.size(); j++) {
                                if (succSequenceSizes[sequence.get(j)] == -1) {
                                    exit = true;
                                    break;
                                }
                            }
                            if (!exit) {
                                max = Integer.MIN_VALUE;
                                for (j = 0; j < sequence.size(); j++) {
                                    if (succSequenceSizes[sequence.get(j)] > max)
                                        max = succSequenceSizes[sequence.get(j)];
                                }
                                succSequenceSizes[i] = max + 1;
                            }
                        }
                    }
                    //Verify if all the successor sequences are calculated
                    notCalculated = false;
                    for (i = 2; i < succSequenceSizes.length; i++)
                        if (succSequenceSizes[i] == -1) {
                            notCalculated = true;
                            break;
                        }
                    if (!notCalculated)
                        completed = true;
                }
                //System.out.println("Max length of the sequences of successors");
                //System.out.println("-----------------------------------------");
                //for(i = 0; i < succSequenceSizes.length; i++)
                //    System.out.println("Action " + i + ": " + succSequenceSizes[i] + " successors");

                int scheduled = 0;
                int bestAction, longestSuccessorSequence;
                level = 0;
                while (scheduled < actions.length - 2) {
                    for (String ag : agents) {
                        bestAction = -1;
                        longestSuccessorSequence = Integer.MIN_VALUE;
                        for (i = 2; i < actions.length; i++) {
                            //If an action introduced by agent ag does not have an assigned level, 
                            //check the maximum level of its predecessors
                            if (actions[i] == Plan.UNASSIGNED && steps.get(i).getAgent().equals(ag)) {
                                assign = true;
                                pre = predecessors.get(i);
                                for (int p : pre) {
                                    //If a predecessor does not have an assigned level,
                                    //or if it is scheduled at the current level,
                                    //action i cannot be scheduled yet
                                    if (actions[p] == Plan.UNASSIGNED || actions[p] == level) {
                                        assign = false;
                                        break;
                                    }
                                }
                                //Schedule action i and break to the next iteration
                                if (assign) {
                                    if (succSequenceSizes[i] > longestSuccessorSequence) {
                                        bestAction = i;
                                        longestSuccessorSequence = succSequenceSizes[i];
                                    }
                                }
                            }
                        }
                        if (bestAction != -1) {
                            actions[bestAction] = level;
                            scheduled++;
                        }
                    }
                    level++;
                }
                break;
            //POP format - actions scheduled as early as possible
            default:
                level = 0;
                assigned = 0;
                while (assigned < actions.length - 2) {
                    for (i = 2; i < actions.length; i++) {
                        //If the action does not have an assigned level, check the maximum level of its predecessors
                        if (actions[i] == Plan.UNASSIGNED) {
                            assign = true;
                            pre = predecessors.get(i);
                            for (int p : pre) {
                                //If a predecessor does not have an assigned level,
                                //or if it is assigned to the current level, action i cannot be scheduled yet
                                if (actions[p] == Plan.UNASSIGNED || actions[p] == level) {
                                    assign = false;
                                    break;
                                }
                            }
                            //Schedule action i and increase assigned variable
                            if (assign) {
                                actions[i] = level;
                                assigned++;
                            }
                        }
                    }
                    //Increase the level after an iteration
                    level++;
                }
                break;
        }

        return actions;
    }

}
