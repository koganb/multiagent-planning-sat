package org.agreement_technologies.service.map_heuristic;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_communication.Message;
import org.agreement_technologies.common.map_communication.MessageFilter;
import org.agreement_technologies.common.map_dtg.DTG;
import org.agreement_technologies.common.map_dtg.DTGSet;
import org.agreement_technologies.common.map_dtg.DTGTransition;
import org.agreement_technologies.common.map_grounding.*;
import org.agreement_technologies.common.map_heuristic.HPlan;
import org.agreement_technologies.common.map_heuristic.Heuristic;
import org.agreement_technologies.common.map_landmarks.LandmarkFluent;
import org.agreement_technologies.common.map_landmarks.LandmarkNode;
import org.agreement_technologies.common.map_landmarks.LandmarkOrdering;
import org.agreement_technologies.common.map_landmarks.Landmarks;
import org.agreement_technologies.common.map_planner.Condition;
import org.agreement_technologies.common.map_planner.PlannerFactory;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_dtg.DTGSetImp;
import org.agreement_technologies.service.map_landmarks.LandmarksImp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.PriorityQueue;

public class LandmarksHeuristic implements Heuristic {

    private static final int PENALTY = 1000;
    private static final int DTG_PENALTY = 1;
    protected GroundedTask groundedTask;            // Grounded task
    protected AgentCommunication comm;                // Agent communication
    protected ArrayList<Goal> goals, pgoals;            // Task goals
    protected HashMap<String, ArrayList<Action>> productors;    // Producer actions
    protected DTGSet dtgs;                    // DTGs
    private Landmarks landmarks;                // Landmarks
    private ArrayList<LandmarkCheck> landmarkNodes;        // Landmark nodes list
    private ArrayList<LandmarkCheck> rootLandmarkNodes;        // Root landmark nodes list
    private HPlan basePlan;                    // Base plan
    private int[] totalOrderBase;
    private int requestId;
    private int ready;
    private boolean incremental_dtg;
    private boolean requiresHLandStage;
    private PlannerFactory pf;
    private HashMap<String, Boolean> booleanVariable;

    public LandmarksHeuristic(AgentCommunication comm, GroundedTask gTask,
                              boolean incremental_dtg, PlannerFactory pf) {
        this.pf = pf;
        this.groundedTask = gTask;
        this.comm = comm;
        this.incremental_dtg = incremental_dtg;
        dtgs = new DTGSetImp(gTask);
        dtgs.distributeDTGs(comm, gTask);
        this.goals = new ArrayList<Goal>();
        this.pgoals = new ArrayList<Goal>();
        ArrayList<GoalCondition> gc = HeuristicToolkit.computeTaskGoals(comm, gTask);
        for (GoalCondition g : gc) {
            GroundedVar var = null;
            for (GroundedVar v : gTask.getVars()) {
                if (v.toString().equals(g.varName)) {
                    var = v;
                    break;
                }
            }
            if (var != null) {
                Goal ng = new Goal(gTask.createGroundedCondition(GroundedCond.EQUAL, var, g.value), 0);
                goals.add(ng);
            }
        }
        for (GroundedCond g : gTask.getPreferences()) {
            pgoals.add(new Goal(g, 0));
        }
        productors = new HashMap<String, ArrayList<Action>>();
        for (Action a : gTask.getActions()) {
            for (GroundedEff e : a.getEffs()) {
                String desc = e.getVar().toString() + "," + e.getValue();
                ArrayList<Action> list = productors.get(desc);
                if (list == null) {
                    list = new ArrayList<Action>();
                    productors.put(desc, list);
                }
                list.add(a);
            }
        }
        booleanVariable = new HashMap<>(gTask.getVars().length);
        for (GroundedVar v : gTask.getVars()) {
            booleanVariable.put(v.toString(), v.isBoolean());
        }
        initializeLandmarks();
        requestId = 0;
    }

    private static String selectInitialValueMono(String varName, String endValue, DTG dtg,
                                                 HashMap<String, String> state, HashMap<String, ArrayList<String>> newValues,
                                                 int threadIndex) {
        String bestValue = state.get(varName);
        int bestCost = dtg.pathCost(bestValue, endValue, state, newValues, threadIndex);
        ArrayList<String> valueList = newValues.get(varName);
        if (valueList != null) {
            for (int i = 0; i < valueList.size(); i++) {
                String value = valueList.get(i);
                int cost = dtg.pathCost(value, endValue, state, newValues, threadIndex);
                if (cost != -1 && cost < bestCost) {
                    bestCost = cost;
                    bestValue = value;
                }
            }
        }
        return bestValue;
    }

    private static boolean holdsMono(String varName, String value, HashMap<String, String> state,
                                     HashMap<String, ArrayList<String>> newValues) {
        String v = state.get(varName);
        if (v != null && v.equals(value)) {
            return true;
        }
        ArrayList<String> values = newValues.get(varName);
        if (values == null) {
            return false;
        }
        return values.contains(value);
    }

    private void initializeLandmarks() {
        // Compute landmarks
        landmarks = new LandmarksImp(groundedTask, comm);
        landmarks.filterTransitiveOrders();
        // Store landmark nodes
        ArrayList<LandmarkNode> nodes = landmarks.getNodes();
        checkIfRequiresHLandStage(nodes);
        landmarkNodes = new ArrayList<LandmarkCheck>(nodes.size());
        for (LandmarkNode n : nodes) {
            LandmarkCheck l = new LandmarkCheck(n, false, goals);
            landmarkNodes.add(l);
        }
        // Store landmark orderings
        ArrayList<LandmarkOrdering> orderings = landmarks.getOrderings(Landmarks.ALL_ORDERINGS, false);
        for (LandmarkOrdering o : orderings) {
            int n1 = o.getNode1().getIndex(), n2 = o.getNode2().getIndex();
            LandmarkCheck l1 = landmarkNodes.get(n1),
                    l2 = landmarkNodes.get(n2);
            l1.addSuccessor(l2);
            l2.addPredecessor(l1);
        }
        // Check if there is a landmark for each top-level goal
        for (Goal g : goals) {
            boolean found = false;
            for (LandmarkCheck l : landmarkNodes) {
                if (l.matches(g)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                LandmarkCheck l = new LandmarkCheck(g);
                landmarkNodes.add(l);
            }
        }
        // Remove initial-state landmarks
        int i = 0;
        while (i < landmarkNodes.size()) {
            LandmarkCheck l = landmarkNodes.get(i);
            if (l.single && l.predecessors.isEmpty()) {
                GroundedVar v = groundedTask.getVarByName(l.varNames[0]);
                if (InInitialState(l, v)) {
                    landmarkNodes.remove(i);
                    for (LandmarkCheck s : l.successors) {
                        s.removePredecessor(l);
                    }
                } else {
                    i++;
                }
            } else {
                i++;
            }
        }
        // Compute root nodes
        rootLandmarkNodes = new ArrayList<LandmarkCheck>();
        for (i = 0; i < landmarkNodes.size(); i++) {
            LandmarkCheck l = landmarkNodes.get(i);
            l.index = i;
            if (l.predecessors.isEmpty()) {
                l.isRoot = true;
                rootLandmarkNodes.add(l);
            }
        }
        /*
         for (LandmarkCheck l: landmarkNodes) {
         System.out.println(l.toString());
         for (LandmarkCheck l2: l.successors)
         System.out.println("  -> " + l2.toString());
         }*/
    }

    private boolean InInitialState(LandmarkCheck l, GroundedVar v) {
        String lValue = l.varValues[0];
        for (String obj : v.getReachableValues()) {
            int time = v.getMinTime(obj);
            if (time == 0 && obj.equals(lValue)) {
                return true;
            }
        }
        return false;
    }

    private void checkIfRequiresHLandStage(ArrayList<LandmarkNode> nodes) {
        requiresHLandStage = false;
        if (comm.numAgents() == 1) {
            return;
        }
        int numNodes = landmarks.numTotalNodes();
        boolean visibleNode[] = new boolean[numNodes];
        for (LandmarkNode n : nodes) {
            if (n.isSingleLiteral()) {
                visibleNode[n.getGlobalId()] = true;
            }
        }
        for (int i = 0; i < numNodes; i++) {
            if (!visibleNode[i]) {
                requiresHLandStage = true;
                break;
            }
        }
        if (comm.batonAgent()) {
            for (String ag : comm.getOtherAgents()) {
                Boolean req = (Boolean) comm.receiveMessage(ag, false);
                if (req) {
                    requiresHLandStage = true;
                }
            }
            comm.sendMessage(new Boolean(requiresHLandStage), false);
        } else {
            comm.sendMessage(comm.getBatonAgent(), new Boolean(requiresHLandStage), false);
            requiresHLandStage = (Boolean) comm.receiveMessage(comm.getBatonAgent(), false);
        }
        //if (requiresHLandStage) System.out.println("Landmark stage required");
        //else System.out.println("Landmark stage not required");
    }

    @Override
    public void startEvaluation(HPlan basePlan) {
        this.basePlan = basePlan;
        this.ready = 0;
        this.totalOrderBase = null;
    }

    @Override
    public void evaluatePlan(HPlan p, int threadIndex) {
        if (p.isSolution()) {
            return;
        }
        dtgs.clearCache(threadIndex);
        if (comm.numAgents() == 1) {
            evaluateMonoagentPlan(p, threadIndex);
        } else {
            evaluateMultiagentPlan(p, null);
        }
    }

    @Override
    public void evaluatePlan(HPlan p, int threadIndex, ArrayList<Integer> achievedLandmarks) {
        if (p.isSolution()) {
            return;
        }
        dtgs.clearCache(threadIndex);
        evaluateMultiagentPlan(p, achievedLandmarks);
    }

    private void computeState(HPlan currentPlan, HashMap<String, String> state,
                              int[] totalOrder, boolean checked[]) {
        ArrayList<LandmarkCheck> openLandmarkNodes = new ArrayList<LandmarkCheck>(landmarkNodes.size());
        for (LandmarkCheck l : rootLandmarkNodes) {
            openLandmarkNodes.add(l);
        }
        ArrayList<Step> stepList = currentPlan.getStepsArray();
        Step action;
        for (int step : totalOrder) {
            action = stepList.get(step);
            for (Condition eff : action.getEffs()) {
                String varName = pf.getVarNameFromCode(eff.getVarCode());
                String varValue = pf.getValueFromCode(eff.getValueCode());
                if (varName != null && varValue != null) {
                    state.put(varName, varValue);
                }
            }
            checkLandmarks(openLandmarkNodes, state, checked);
        }
    }

    private void checkLandmarks(ArrayList<LandmarkCheck> openLandmarkNodes,
                                HashMap<String, String> state, boolean checked[]) {
        int i = 0;
        while (i < openLandmarkNodes.size()) {
            LandmarkCheck l = openLandmarkNodes.get(i);
            if (l.goOn(state, checked)) {            // The landmark holds in the state and we can progress
                openLandmarkNodes.remove(i);        // Remove node from the open nodes list
                for (LandmarkCheck s : l.successors) {
                    if (!checked[s.index]) {
                        int pos = openLandmarkNodes.indexOf(s);
                        if (pos == -1) {
                            openLandmarkNodes.add(s);    // Non-visited node -> append to open nodes
                        } else if (pos < i) {
                            i = pos;        // Perhaps we can progress now (a predecessor has been reached)
                        }
                    }
                }
            } else {
                i++;
            }
        }
    }

    /**
     * ******************************************************************
     */
    /*                         M O N O - A G E N T                       */
    private void computeMultiState(HPlan currentPlan, HashMap<String, ArrayList<String>> state,
                                   int[] totalOrder, boolean checked[]) {
        ArrayList<LandmarkCheck> openLandmarkNodes = new ArrayList<LandmarkCheck>(landmarkNodes.size());
        for (LandmarkCheck l : rootLandmarkNodes) {
            openLandmarkNodes.add(l);
        }
        ArrayList<Step> stepList = currentPlan.getStepsArray();
        String v, value;
        Step action;
        ArrayList<String> list;
        for (int step : totalOrder) {
            action = stepList.get(step);
            for (Condition eff : action.getEffs()) {
                v = pf.getVarNameFromCode(eff.getVarCode());
                value = pf.getValueFromCode(eff.getValueCode());
                if (v != null && value != null) {
                    if (state.containsKey(v)) {
                        list = state.get(v);
                        list.set(0, value);
                    } else {
                        list = new ArrayList<String>();
                        list.add(value);
                        state.put(v, list);
                    }
                }
            }
            checkLandmarksMulti(openLandmarkNodes, state, checked);
        }
    }

    private void checkLandmarksMulti(ArrayList<LandmarkCheck> openLandmarkNodes,
                                     HashMap<String, ArrayList<String>> state, boolean checked[]) {
        int i = 0;
        while (i < openLandmarkNodes.size()) {
            LandmarkCheck l = openLandmarkNodes.get(i);
            if (l.goOnMulti(state, checked)) {        // The landmark holds in the state and we can progress
                openLandmarkNodes.remove(i);        // Remove node from the open nodes list
                for (LandmarkCheck s : l.successors) {
                    if (!checked[s.index]) {
                        int pos = openLandmarkNodes.indexOf(s);
                        if (pos == -1) {
                            openLandmarkNodes.add(s);    // Non-visited node -> append to open nodes
                        } else if (pos < i) {
                            i = pos;        // Perhaps we can progress now (a predecessor has been reached)
                        }
                    }
                }
            } else {
                i++;
            }
        }
    }

    /**
     * ******************************************************************
     */
    private void evaluateMonoagentPlan(HPlan currentPlan, int threadIndex) {
        HashMap<String, String> state = new HashMap<String, String>();
        int totalOrder[] = currentPlan.linearization();
        boolean checked[] = new boolean[landmarkNodes.size()];
        computeState(currentPlan, state, totalOrder, checked);
        int hl = 0;
        for (int i = 0; i < landmarkNodes.size(); i++) {
            if (!checked[i]) {
                hl++;
            }
        }
        int h = 0;
        if (incremental_dtg) {
            boolean evaluated[] = new boolean[landmarkNodes.size()];
            HashMap<String, ArrayList<String>> newValues = new HashMap<String, ArrayList<String>>();
            ArrayList<LandmarkCheck> unreachedSubgoals = new ArrayList<LandmarkCheck>(landmarkNodes.size());
            for (LandmarkCheck l : rootLandmarkNodes) {
                unreachedSubgoals.add(l);
                evaluated[l.index] = true;
            }
            int i = 0;
            while (i < unreachedSubgoals.size()) {
                LandmarkCheck l = unreachedSubgoals.get(i);
                if (checked[l.index]) {
                    unreachedSubgoals.remove(i);
                    for (LandmarkCheck s : l.successors) {
                        if (!evaluated[s.index]) {
                            unreachedSubgoals.add(s);
                            evaluated[s.index] = true;
                        }
                    }
                } else {
                    i++;
                }
            }
            PriorityQueue<Goal> openGoals = new PriorityQueue<Goal>();
            while (!unreachedSubgoals.isEmpty()) {
                for (i = 0; i < unreachedSubgoals.size(); i++) {
                    LandmarkCheck l = unreachedSubgoals.get(i);
                    if (l.single && !l.isGoal) {
                        String v = l.varNames[0], end = l.varValues[0];
                        if (!holdsMono(v, end, state, newValues)) {
                            String init = selectInitialValueMono(v, end, dtgs.getDTG(v), state,
                                    newValues, threadIndex);
                            int dst = pathCostMono(v, init, end, state, newValues, threadIndex);
                            if (dst >= INFINITE) {
                                h = INFINITE;
                                break;
                            }
                            openGoals.add(new Goal(v, end, dst));
                        }
                    }
                }
                if (h >= INFINITE) {
                    break;
                }
                while (!openGoals.isEmpty() && h < INFINITE) {
                    Goal g = openGoals.poll();
                    h += solveConditionMono(g, openGoals, state, newValues, threadIndex);
                }
                if (h >= INFINITE) {
                    break;
                }
                advanceSubgoalSet(unreachedSubgoals, evaluated);
            }

            for (Goal g : goals) {    // Global goals
                String v = g.varName, end = g.varValue;
                if (!holdsMono(v, end, state, newValues)) {
                    String init = selectInitialValueMono(v, end, dtgs.getDTG(v), state, newValues,
                            threadIndex);
                    int dst = pathCostMono(v, init, end, state, newValues, threadIndex);
                    if (dst >= INFINITE) {
                        h = INFINITE;
                        break;
                    }
                    openGoals.add(new Goal(v, end, dst));
                }
            }
            while (!openGoals.isEmpty() && h < INFINITE) {
                Goal g = openGoals.poll();
                h += solveConditionMono(g, openGoals, state, newValues, threadIndex);
            }

        } else {
            HashMap<String, ArrayList<String>> newValues = new HashMap<String, ArrayList<String>>();
            PriorityQueue<Goal> openGoals = new PriorityQueue<Goal>();
            for (Goal g : goals) {    // Global goals
                String v = g.varName, end = g.varValue;
                if (!holdsMono(v, end, state, newValues)) {
                    String init = selectInitialValueMono(v, end, dtgs.getDTG(v), state, newValues,
                            threadIndex);
                    int dst = pathCostMono(v, init, end, state, newValues, threadIndex);
                    if (dst >= INFINITE) {
                        h = INFINITE;
                        break;
                    }
                    openGoals.add(new Goal(v, end, dst));
                }
            }
            while (!openGoals.isEmpty() && h < INFINITE) {
                Goal g = openGoals.poll();
                h += solveConditionMono(g, openGoals, state, newValues, threadIndex);
            }

        }
        currentPlan.setH(h, hl);
        /*
         for (int i = 0; i < pgoals.size(); i++) {	// Preferences
         int hp = 0;
         openGoals.clear();
         newValues.clear();
         Goal g = pgoals.get(i);
         String v = g.varName, end = g.varValue;
         if (!holdsMono(v, end, state, newValues)) {
         String init = selectInitialValueMono(v, end, dtgs.getDTG(v), state, newValues, threadIndex);
         int dst = pathCostMono(v, init, end, state, newValues, threadIndex);
         if (dst >= INFINITE) hp = INFINITE;
         else openGoals.add(new Goal(v, end, dst));
         }
         while (!openGoals.isEmpty() && hp < INFINITE) {
         g = openGoals.poll();
         hp += solveConditionMono(g, openGoals, state, newValues, threadIndex);
         }
         currentPlan.setHPriv(hp, i);
         }*/
    }

    private void advanceSubgoalSet(ArrayList<LandmarkCheck> unreachedSubgoals,
                                   boolean[] evaluated) {
        ArrayList<LandmarkCheck> newSet = new ArrayList<LandmarkCheck>(unreachedSubgoals.size());
        for (LandmarkCheck l : unreachedSubgoals) {
            for (LandmarkCheck s : l.successors) {
                if (!evaluated[s.index]) {
                    newSet.add(s);
                    evaluated[s.index] = true;
                }
            }
        }
        unreachedSubgoals.clear();
        for (LandmarkCheck l : newSet) {
            unreachedSubgoals.add(l);
        }
    }

    /**
     * Mono-agent heuristic goal evaluation
     *
     * @param goal       Goal to be evaluated
     * @param openGoals  List of open goals
     * @param varValues  State
     * @param queueGoals
     * @return Goal cost
     */
    private int solveConditionMono(Goal goal, PriorityQueue<Goal> openGoals,
                                   HashMap<String, String> state, HashMap<String, ArrayList<String>> newValues,
                                   int threadIndex) {
        int h = 0;
        String varName = goal.varName, varValue = goal.varValue;
        if (holdsMono(varName, varValue, state, newValues)) {
            return h;
        }
        DTG dtg = dtgs.getDTG(varName);
        String initValue = selectInitialValueMono(varName, varValue, dtg, state, newValues,
                threadIndex);
        String[] path = dtg.getPath(initValue, varValue, state, newValues, threadIndex);
        if (path == null) {
            return INFINITE;
        }
        String prevValue = path[0], nextValue;
        for (int i = 1; i < path.length; i++) {
            nextValue = path[i];
            Action a = selectProductorMono(varName, prevValue, nextValue, state, newValues,
                    threadIndex);
            if (a == null) {
                h = INFINITE;
                break;
            }
            h++;
            updateValuesAndGoalsMono(a, openGoals, state, newValues, threadIndex);
            prevValue = nextValue;
        }
        return h;
    }

    private Action selectProductorMono(String varName, String startValue, String endValue,
                                       HashMap<String, String> state, HashMap<String, ArrayList<String>> newValues,
                                       int threadIndex) {
        ArrayList<Action> productors = this.productors.get(varName + "," + endValue);
        if (productors == null || productors.isEmpty()) {
            return null;
        }
        Action bestAction = null;
        int costBest = INFINITE;
        for (int i = 0; i < productors.size(); i++) {
            if (hasPrecondition(productors.get(i), varName, startValue)) {
                int cost = computeCostMono(productors.get(i), state, newValues, threadIndex);
                if (cost < costBest) {
                    costBest = cost;
                    bestAction = productors.get(i);
                }
            }
        }
        return bestAction;
    }

    private boolean hasPrecondition(Action action, String varName, String startValue) {
        for (GroundedCond prec : action.getPrecs()) {
            if (varName.equals(prec.getVar().toString())) {
                return prec.getValue().equals(startValue);
            }
        }
        return true;    // Variable not in preconditions -> a specific value is not required
    }

    private void updateValuesAndGoalsMono(Action a, PriorityQueue<Goal> openGoals,
                                          HashMap<String, String> state, HashMap<String, ArrayList<String>> newValues,
                                          int threadIndex) {
        // Add a's preconditions to open goals if they do not hold
        for (GroundedCond p : a.getPrecs()) {
            String precVarName = p.getVar().toString();
            if (!holdsMono(precVarName, p.getValue(), state, newValues)) {
                String precInitValue = selectInitialValueMono(precVarName, p.getValue(),
                        dtgs.getDTG(precVarName), state, newValues, threadIndex);
                Goal newOpenGoal = new Goal(p, pathCostMono(precVarName, precInitValue, p.getValue(),
                        state, newValues, threadIndex));
                openGoals.add(newOpenGoal);
            }
        }
        // Add a's effects to varValues
        for (GroundedEff e : a.getEffs()) {
            String v = e.getVar().toString();
            if (!state.get(v).equals(e.getValue())) {
                ArrayList<String> values = newValues.get(v);
                if (values == null) {
                    values = new ArrayList<String>();
                    values.add(e.getValue());
                    newValues.put(v, values);
                } else {
                    if (!values.contains(e.getValue())) {
                        values.add(e.getValue());
                    }
                }
            }
        }
    }

    private int pathCostMono(String var, String initValue, String endValue, HashMap<String, String> state,
                             HashMap<String, ArrayList<String>> newValues, int threadIndex) {
        DTG dtg = dtgs.getDTG(var);
        return dtg.pathCost(initValue, endValue, state, null, threadIndex);
    }

    private int computeCostMono(Action a, HashMap<String, String> state,
                                HashMap<String, ArrayList<String>> newValues, int threadIndex) {
        int cost = 0;
        for (GroundedCond prec : a.getPrecs()) {
            String var = prec.getVar().toString();
            DTG dtg = dtgs.getDTG(var);
            String iValue = state.get(var);
            int minPrecCost = dtg.pathCost(iValue, prec.getValue(), state, newValues, threadIndex);
            ArrayList<String> initValues = newValues.get(var);
            if (initValues != null) {
                for (String initValue : initValues) {
                    int precCost = dtg.pathCost(initValue, prec.getValue(), state, newValues, threadIndex);
                    if (precCost < minPrecCost) {
                        minPrecCost = precCost;
                    }
                }
            }
            cost += minPrecCost;
        }
        return cost;
    }

    /**
     * ******************************************************************
     */
    /*                       M U L T I - A G E N T                       */

    /**
     * ******************************************************************
     */
    private void evaluateMultiagentPlan(HPlan currentPlan, ArrayList<Integer> achievedLandmarks) {
        HashMap<String, ArrayList<String>> state = new HashMap<String, ArrayList<String>>();
        int totalOrder[] = currentPlan.linearization();
        boolean checked[] = new boolean[landmarkNodes.size()];
        computeMultiState(currentPlan, state, totalOrder, checked);

        int hl = landmarks.numGlobalNodes();
        for (LandmarkCheck l : landmarkNodes) {
            if (checked[l.index] && l.single) {
                //System.out.println("Achieved: " + l.toString());
                hl--;
                if (achievedLandmarks != null) {
                    achievedLandmarks.add(l.globalIndex);
                }
            }
        }
        /*else if (l.single) {
         System.out.println("Unachieved landmark: " + l.toString());
         }*/
        /*
         if (hl < 0) {
         hl = landmarks.numGlobalNodes();
         for (LandmarkCheck l: landmarkNodes)
         if (checked[l.index] && l.single) {
         System.out.println("Achieved: " + l.toString());
         hl--;
         if (achievedLandmarks != null)
         achievedLandmarks.add(l.globalIndex);
         }
         }*/
        int h = 0;
        PriorityQueue<Goal> openGoals = new PriorityQueue<Goal>();
        for (Goal g : goals) {
            String v = g.varName, end = g.varValue;
            if (!holdsMulti(v, end, state)) {
                String init = selectInitialValueMulti(v, end, dtgs.getDTG(v), state);
                g.distance = pathCostMulti(v, init, end);
                openGoals.add(g);
            }
        }
        while (!openGoals.isEmpty()) {
            Goal g = openGoals.poll();
            if (g.distance >= INFINITE || g.distance < 0) {
                h += DTG_PENALTY;
            } else {
                h += solveConditionMulti(g, state, openGoals, null);
            }
        }
        currentPlan.setH(h, hl);
        //evaluateMultiagentPlanPrivacy(currentPlan, varValues);
    }

    private int pathCostMulti(String var, String initValue, String endValue) {
        DTG dtg = dtgs.getDTG(var);
        return dtg.pathCostMulti(initValue, endValue);
    }

    private int solveConditionMulti(Goal goal, HashMap<String, ArrayList<String>> varValues,
                                    PriorityQueue<Goal> openGoals, TransitionCostRequest tcr) {
        int h;
        String varName = goal.varName, varValue = goal.varValue;
        if (holdsMulti(varName, varValue, varValues)) {
            return 0;
        }
        DTG dtg = dtgs.getDTG(varName);
        String initValue = selectInitialValueMulti(varName, varValue, dtg, varValues);
        if (!dtg.unknownValue(varValue) && !dtg.unknownValue(initValue)) {    // Known initial value
            h = evaluateWithKnownValues(varName, initValue, varValue, dtg, varValues, tcr,
                    openGoals);
        } else {                        // Unknown values
            if (dtg.unknownValue(varValue)) {
                h = evaluateWithUnknownFinalvalue(varName, initValue, varValue, dtg, varValues,
                        tcr, openGoals);
            } else {
                h = evaluateWithUnknownInitialvalue(varName, varValue, dtg, varValues,
                        tcr, openGoals);
            }
        }
        return h;
    }

    private String selectInitialValueMulti(String varName, String endValue, DTG dtg,
                                           HashMap<String, ArrayList<String>> varValues) {
        ArrayList<String> valueList = varValues.get(varName);
        if (valueList == null) {
            return groundedTask.negationByFailure() && booleanVariable.get(varName) ? "false" : "?";
        }
        String bestValue = null;
        int bestCost = -1;
        for (String value : valueList) {
            if (bestValue == null) {
                bestCost = dtg.pathCostMulti(value, endValue);
                bestValue = value;
            } else {
                int cost = dtg.pathCostMulti(value, endValue);
                if (cost != -1 && cost < bestCost) {
                    bestCost = cost;
                    bestValue = value;
                }
            }
        }
        if (bestValue != null) return bestValue;
        else return groundedTask.negationByFailure() && booleanVariable.get(varName) ? "false" : "?";
    }

    private boolean holdsMulti(String varName, String value,
                               HashMap<String, ArrayList<String>> varValues) {
        ArrayList<String> values = varValues.get(varName);
        if (values == null) {
            return false;
        }
        return values.contains(value);
    }

    private int evaluateWithUnknownInitialvalue(String varName, String varValue, DTG dtg,
                                                HashMap<String, ArrayList<String>> varValues, TransitionCostRequest tcr,
                                                PriorityQueue<Goal> openGoals) {
        int h = PENALTY;
        DTGTransition[] transitions = dtg.getTransitionsFrom("?");
        for (DTGTransition t : transitions) {
            if (tcr == null || !tcr.varName.equals(varName) || !tcr.endValue.equals(t.getFinalValue())) {
                //sop("Solving condition " + varName + "=" + varValue + " (init value: ?->" + t.getFinalValue() +  ")");
                int cost = requestTransitionCost(varName, "?", t.getFinalValue(), varValues, dtg, tcr);
                if (cost != PENALTY) {    // Achieved
                    ArrayList<String> values = varValues.get(varName);
                    if (values == null) {
                        values = new ArrayList<String>();
                        values.add(t.getFinalValue());
                        varValues.put(varName, values);
                    } else if (!values.contains(t.getFinalValue())) {
                        values.add(t.getFinalValue());
                    }
                    int restCost = evaluateWithKnownValues(varName, t.getFinalValue(), varValue, dtg,
                            varValues, tcr, openGoals);
                    if (restCost != PENALTY) {
                        h = cost + restCost;
                        break;
                    }
                }
            }
        }
        return h;
    }

    private int evaluateWithKnownValues(String varName, String initValue, String varValue, DTG dtg,
                                        HashMap<String, ArrayList<String>> varValues, TransitionCostRequest tcr,
                                        PriorityQueue<Goal> openGoals) {
        //int cost = dtg.pathCostMulti(initValue, varValue);
        //if (cost >= INFINITE || cost < 0) return DTG_PENALTY;
        int h = 0;
        String[] path = dtg.getPathMulti(initValue, varValue);
        //sop("Solving condition " + varName + "=" + varValue + " (init value: " + initValue +
        //		"). Path: " + java.util.Arrays.toString(path));
        String prevValue = path[0], nextValue, precVarName;
        for (int i = 1; i < path.length; i++) {
            nextValue = path[i];
            Action a = selectProductorMulti(varName, prevValue, nextValue, varValues);
            if (a == null) {
                h += requestTransitionCost(varName, prevValue, nextValue, varValues, dtg, tcr);
                ArrayList<String> values = varValues.get(varName);
                if (values == null) {
                    values = new ArrayList<String>();
                    values.add(nextValue);
                    varValues.put(varName, values);
                } else if (!values.contains(nextValue)) {
                    values.add(nextValue);
                }
            } else {
                h++;
                // Add a's preconditions to open goals if they do not hold
                //sop("Action: " + a.toString());
                for (GroundedCond p : a.getPrecs()) {
                    precVarName = p.getVar().toString();
                    if (!holdsMulti(precVarName, p.getValue(), varValues)) {
                        String precInitValue = selectInitialValueMulti(precVarName, p.getValue(),
                                dtgs.getDTG(precVarName), varValues);
                        Goal newOpenGoal = new Goal(p, pathCostMulti(precVarName, precInitValue, p.getValue()));
                        openGoals.add(newOpenGoal);
                    }
                }
                // Add a's effects to varValues
                for (GroundedEff e : a.getEffs()) {
                    ArrayList<String> values = varValues.get(e.getVar().toString());
                    if (values == null) {
                        values = new ArrayList<String>();
                        values.add(e.getValue());
                        varValues.put(e.getVar().toString(), values);
                    } else if (!values.contains(e.getValue())) {
                        values.add(e.getValue());
                    }
                }
            }
            prevValue = nextValue;
        }
        return h;
    }

    private Action selectProductorMulti(String varName, String startValue, String endValue,
                                        HashMap<String, ArrayList<String>> varValues) {
        ArrayList<Action> productors = this.productors.get(varName + "," + endValue);
        if (productors == null || productors.isEmpty()) {
            return null;
        }
        Action bestAction = null;
        int costBest = PENALTY;
        for (int i = 0; i < productors.size(); i++) {
            if (hasPrecondition(productors.get(i), varName, startValue)) {
                int cost = computeCostMulti(productors.get(i), varValues);
                if (cost < costBest) {
                    costBest = cost;
                    bestAction = productors.get(i);
                }
            }
        }
        return bestAction;
    }

    private int computeCostMulti(Action a, HashMap<String, ArrayList<String>> varValues) {
        int cost = 0;
        for (GroundedCond prec : a.getPrecs()) {
            String var = prec.getVar().toString();
            DTG dtg = dtgs.getDTG(var);
            int minPrecCost = PENALTY;
            ArrayList<String> initValues = varValues.get(var);
            if (initValues != null && !initValues.isEmpty()) {
                for (String initValue : initValues) {
                    int precCost = dtg.pathCostMulti(initValue, prec.getValue());
                    if (precCost < minPrecCost) {
                        minPrecCost = precCost;
                    }
                }
                //if (minPrecCost == PENALTY)
                //	System.out.println(var + "-/->" + prec.getValue());
                cost += minPrecCost;
            } else {
                if (this.groundedTask.negationByFailure() && prec.getVar().isBoolean())
                    cost += dtg.pathCostMulti("false", prec.getValue());
                else
                    cost += dtg.pathCostMulti("?", prec.getValue());
            }
        }
        return cost;
    }

    private int evaluateWithUnknownFinalvalue(String varName, String initValue, String varValue,
                                              DTG dtg, HashMap<String, ArrayList<String>> varValues, TransitionCostRequest tcr,
                                              PriorityQueue<Goal> openGoals) {
        int h = PENALTY;
        DTGTransition[] transitions = dtg.getTransitionsTo("?");
        if (transitions != null) {
            for (DTGTransition t : transitions) {
                openGoals = new PriorityQueue<Goal>();
                int cost = evaluateWithKnownValues(varName, initValue, t.getStartValue(), dtg,
                        varValues, tcr, openGoals);
                if (cost != PENALTY) {
                    int restCost = requestTransitionCost(varName, t.getStartValue(), varValue,
                            varValues, dtg, tcr);
                    if (restCost != PENALTY) {
                        h = cost + restCost;
                        break;
                    }
                }
            }
        }
        return h;
    }

    private void evaluateMultiagentPlanPrivacy(HPlan currentPlan,
                                               HashMap<String, ArrayList<String>> varValues) {
        if (varValues == null) {
            int totalOrder[] = currentPlan.linearization();
            varValues = currentPlan.computeMultiState(totalOrder, pf);
        }
        PriorityQueue<Goal> openGoals = new PriorityQueue<Goal>();
        for (int i = 0; i < pgoals.size(); i++) {    // Preferences
            int hp = 0;
            Goal g = pgoals.get(i);
            String v = g.varName, end = g.varValue;
            if (!holdsMulti(v, end, varValues)) {
                String init = selectInitialValueMulti(v, end, dtgs.getDTG(v), varValues);
                g.distance = pathCostMulti(v, init, end);
                openGoals.add(g);
            }
            while (!openGoals.isEmpty()) {
                hp += solveConditionMulti(openGoals.poll(), varValues, openGoals, null);
            }
            currentPlan.setHPriv(hp, i);
        }
    }

    private int requestTransitionCost(String varName, String prevValue, String nextValue,
                                      HashMap<String, ArrayList<String>> varValues, DTG dtg, TransitionCostRequest prevTcr) {
        int h = PENALTY;
        DTGTransition t = dtg.getTransition(prevValue, nextValue);
        ArrayList<String> askedAgents = new ArrayList<String>();
        for (String ag : t.getAgents()) // Send requests
        {
            if (!ag.equals(comm.getThisAgentName())) {
                if (detectLoop(ag, prevTcr)) {
                    continue;
                }
                TransitionCostRequest tcr = new TransitionCostRequest(varName, prevValue, nextValue,
                        comm.getThisAgentName(), prevTcr, requestId);
                tcr.setState(varValues, groundedTask, ag);
                //sop("Requesting transition cost of " + varName + "(" + prevValue + "->" + nextValue + ") to " + ag);
                comm.sendMessage(ag, tcr, false);
                askedAgents.add(ag);
            }
        }
        // Wait responses
        MessageFilter filter = new DTGMessageFilter(askedAgents, requestId++);
        while (!askedAgents.isEmpty()) {
            Serializable msg = comm.receiveMessage(filter, false);
            if (msg instanceof ReplyTransitionCost) {        // Response received
                int index = askedAgents.indexOf(comm.getSenderAgent());
                askedAgents.remove(index);
                int cost = ((ReplyTransitionCost) msg).cost;
                //sop("Response received from " + comm.getSenderAgent() + ": " + cost);
                if (cost < h) {
                    h = cost;
                }
            } else if (msg instanceof String) { // End stage message
                assert (((String) msg).equals(AgentCommunication.END_STAGE_MESSAGE));
                ready++;
                //sop("End stage message received (" + ready + " agents ready)");
            } else {                            // Transition cost request received
                TransitionCostRequest tcr = (TransitionCostRequest) msg;
                evaluateRequest(tcr, comm.getSenderAgent());
            }
        }
        //transitionCost.put(new Transition(varName, prevValue, nextValue), h);
        return h;
    }

    private boolean detectLoop(String ag, TransitionCostRequest t) {
        if (t == null) {
            return false;
        }
        int index = t.agents.indexOf(ag);
        if (index == -1) {
            return false;
        }
        if (groundedTask.negationByFailure()) return true;
        int count = 1;
        for (int i = index + 1; i < t.agents.size(); i++) {
            if (t.agents.get(i).equals(ag)) {
                count++;
                if (count > 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private void evaluateRequest(TransitionCostRequest t, String fromAgent) {
        //if (t.varName.equals("message-data msg1-1 node1") && fromAgent.equals("base"))
        //    System.out.println("AQUI");
        //sop("Request from " + fromAgent + ": " + t.toString());
        if (totalOrderBase == null) {
            totalOrderBase = basePlan.linearization();
        }
        HashMap<String, ArrayList<String>> varValues = basePlan.computeMultiState(totalOrderBase, pf);
        t.updateState(varValues);
        PriorityQueue<Goal> openGoals = new PriorityQueue<Goal>();
        openGoals.add(t.getGoal());
        int h = 0;
        while (!openGoals.isEmpty()) {
            h += solveConditionMulti(openGoals.poll(), varValues, openGoals, t);
        }
        //System.out.println("Sending reply (" + h + ") to agent " + fromAgent + " - " + t.varName);
        comm.sendMessage(fromAgent, new ReplyTransitionCost(h, t.requestId), false);
    }

    @Override
    public void evaluatePlanPrivacy(HPlan p, int threadIndex) {
        if (p.isSolution() || pgoals.isEmpty()) {
            return;
        }
        dtgs.clearCache(threadIndex);
        int hp;
        HashMap<String, String> state = new HashMap<String, String>();
        HashMap<String, ArrayList<String>> newValues = new HashMap<String, ArrayList<String>>();
        int totalOrder[] = p.linearization();
        boolean checked[] = new boolean[landmarkNodes.size()];
        computeState(p, state, totalOrder, checked);
        PriorityQueue<Goal> openGoals = new PriorityQueue<Goal>();
        for (int i = 0; i < pgoals.size(); i++) {    // Preferences
            hp = 0;
            openGoals.clear();
            newValues.clear();
            Goal g = pgoals.get(i);
            String v = g.varName, end = g.varValue;
            if (!holdsMono(v, end, state, newValues)) {
                String init = selectInitialValueMono(v, end, dtgs.getDTG(v), state, newValues, threadIndex);
                int dst = pathCostMono(v, init, end, state, newValues, threadIndex);
                if (dst >= INFINITE) {
                    hp = INFINITE;
                } else {
                    openGoals.add(new Goal(v, end, dst));
                }
            }
            while (!openGoals.isEmpty() && hp < INFINITE) {
                g = openGoals.poll();
                hp += solveConditionMono(g, openGoals, state, newValues, threadIndex);
            }
            p.setHPriv(hp, i);
        }
    }

    @Override
    public void waitEndEvaluation() {
        WaitMessageFilter filter = new WaitMessageFilter();
        if (comm.batonAgent()) {
            ready++;
            //sop("Baton agent waiting end stage messages", null);
            while (ready < comm.numAgents()) {
                Serializable msg = comm.receiveMessage(filter, false);
                if (msg instanceof String) {    // End of evaluation stage received
                    assert (((String) msg).equals(AgentCommunication.END_STAGE_MESSAGE));
                    ready++;
                    //sop("End stage message received (" + ready + " agents ready)", null);
                } else {                        // Evaluation request
                    String fromAgent = comm.getSenderAgent();
                    TransitionCostRequest tcr = (TransitionCostRequest) msg;
                    evaluateRequest(tcr, fromAgent);
                }
            }
            //sop("Sending end stage messages to all agents", null);
            comm.sendMessage(AgentCommunication.END_STAGE_MESSAGE, false);
        } else {
            boolean endStage = false;
            //sop("Sending end stage message to baton agent", null);
            comm.sendMessage(comm.getBatonAgent(), AgentCommunication.END_STAGE_MESSAGE, false);
            while (!endStage) {
                Serializable msg = comm.receiveMessage(filter, false);
                if (msg instanceof String) {    // End of evaluation stage received
                    //sop("End stage message received from baton agent", null);
                    assert (((String) msg).equals(AgentCommunication.END_STAGE_MESSAGE));
                    endStage = true;
                } else {                        // Evaluation request
                    String fromAgent = comm.getSenderAgent();
                    TransitionCostRequest tcr = (TransitionCostRequest) msg;
                    evaluateRequest(tcr, fromAgent);
                }
            }
        }
    }

    @Override
    public boolean supportsMultiThreading() {
        return comm.numAgents() == 1;
    }

    @Override
    public Object getInformation(int infoFlag) {
        if (infoFlag == Heuristic.INFO_LANDMARKS) {
            return landmarks;
        }
        return null;
    }

    /**
     * *******************************************************************
     */
    /*                              G O A L                               */
    @Override
    public boolean requiresHLandStage() {
        return (comm.numAgents() > 1)
                && requiresHLandStage;
    }

    /**
     * *******************************************************************
     */
    /*         T R A N S I T I O N    C O S T    R E Q U E S T            */
    @Override
    public int numGlobalLandmarks() {
        return landmarks.numGlobalNodes();
    }

    /**
     * *******************************************************************
     */
    /*            R E P L Y    T R A N S I T I O N    C O S T             */
    @Override
    public ArrayList<Integer> checkNewLandmarks(HPlan plan, BitSet achievedLandmarks) {
        ArrayList<Integer> newLandmarks = new ArrayList<Integer>();
        HashMap<String, String> state = new HashMap<String, String>();
        int[] totalOrder = plan.linearization();
        boolean checked[] = new boolean[landmarkNodes.size()];
        ArrayList<LandmarkCheck> openLandmarkNodes = new ArrayList<LandmarkCheck>(landmarkNodes.size());
        for (LandmarkCheck l : rootLandmarkNodes) {
            openLandmarkNodes.add(l);
        }
        ArrayList<Step> stepList = plan.getStepsArray();
        Step action;
        for (int step : totalOrder) {
            action = stepList.get(step);
            for (Condition eff : action.getEffs()) {
                String varName = pf.getVarNameFromCode(eff.getVarCode());
                String valueName = pf.getValueFromCode(eff.getValueCode());
                if (varName != null && valueName != null) {
                    state.put(varName, valueName);
                }
            }
            checkLandmarks(openLandmarkNodes, state, checked);
        }
        for (int i = 0; i < checked.length; i++) {
            if (checked[i]) {
                LandmarkCheck l = landmarkNodes.get(i);
                if (l.single && !achievedLandmarks.get(l.globalIndex)) {
                    newLandmarks.add(l.globalIndex);
                }
            }
        }
        return newLandmarks;
    }

    /**
     * *******************************************************************
     */
    /*                 L A N D M A R K    C H E C K                       */

    /**
     * *******************************************************************
     */
    private static class Goal implements Comparable<Goal> {

        String varName, varValue;
        int distance;

        public Goal(GroundedCond goal, int distance) {
            this(goal.getVar().toString(), goal.getValue(), distance);
        }

        public Goal(String varName, String varValue, int distance) {
            this.varName = varName;
            this.varValue = varValue;
            this.distance = distance;
        }

        @Override
        public int compareTo(Goal g) {
            return g.distance - distance;
        }

        @Override
        public String toString() {
            return varName + "=" + varValue + "(" + distance + ")";
        }

        @Override
        public int hashCode() {
            return (varName + "=" + varValue).hashCode();
        }

        @Override
        public boolean equals(Object x) {
            Goal g = (Goal) x;
            return varName.equals(g.varName) && varValue.equals(g.varValue);
        }
    }

    /**
     * *******************************************************************
     */
    /*               D T G    M E S S A G E    F I L T E R                */

    /**
     * *******************************************************************
     */
    public static class TransitionCostRequest implements java.io.Serializable {

        private static final long serialVersionUID = 5485301296724177527L;
        public ArrayList<String> agents;
        public String varName, startValue, endValue;
        public ArrayList<ArrayList<String>> varValuesList;
        public int requestId;

        public TransitionCostRequest(String varName, String prevValue, String nextValue,
                                     String agentName, TransitionCostRequest prevTcr, int requestId) {
            this.varName = varName;
            this.startValue = prevValue;
            this.endValue = nextValue;
            this.agents = new ArrayList<String>();
            if (prevTcr != null) {
                for (String ag : prevTcr.agents) {
                    this.agents.add(ag);
                }
            }
            this.agents.add(agentName);
            this.varValuesList = new ArrayList<ArrayList<String>>();
            this.requestId = requestId;
        }

        public Goal getGoal() {
            return new Goal(varName, endValue, -1);
        }

        public void updateState(HashMap<String, ArrayList<String>> varValues) {
            for (ArrayList<String> list : varValuesList) {
                ArrayList<String> values = varValues.get(list.get(0));
                if (values == null) {
                    values = new ArrayList<String>();
                    for (int i = 1; i < list.size(); i++) {
                        values.add(list.get(i));
                    }
                    varValues.put(list.get(0), values);
                } else {
                    for (int i = 1; i < list.size(); i++) {
                        if (!values.contains(list.get(i))) {
                            values.add(list.get(i));
                        }
                    }
                }
            }
        }

        public void setState(HashMap<String, ArrayList<String>> varValues,
                             GroundedTask groundedTask, String toAgent) {
            for (String v : varValues.keySet()) {
                GroundedVar gv = groundedTask.getVarByName(v);
                if (gv.shareable(toAgent)) {
                    ArrayList<String> list = varValues.get(v),
                            newList = new ArrayList<String>(list.size() + 1);
                    newList.add(v);
                    for (String value : list) {
                        if (gv.shareable(value, toAgent)) {
                            newList.add(value);
                        }
                    }
                    this.varValuesList.add(newList);
                }
            }
        }

        public String toString() {
            return varName + "(" + startValue + "->" + endValue + ")";
        }
    }

    /**
     * *******************************************************************
     */
    public static class ReplyTransitionCost implements Serializable {

        private static final long serialVersionUID = 8450612556336972847L;
        int cost;
        int requestId;

        public ReplyTransitionCost(int cost, int requestId) {
            this.cost = cost;
            this.requestId = requestId;
        }
    }

    /**
     * *******************************************************************
     */
    private static class LandmarkCheck {

        int index, globalIndex;
        boolean isRoot, single, isGoal;
        String varNames[], varValues[];
        ArrayList<LandmarkCheck> successors, predecessors;

        private LandmarkCheck(LandmarkNode n, boolean isRoot, ArrayList<Goal> goals) {
            this.globalIndex = n.getGlobalId();
            this.isRoot = isRoot;
            LandmarkFluent[] lfs = n.getFluents();
            this.varNames = new String[lfs.length];
            this.varValues = new String[lfs.length];
            for (int i = 0; i < lfs.length; i++) {
                varNames[i] = lfs[i].getVarName();
                varValues[i] = lfs[i].getValue();
            }
            single = n.isSingleLiteral();
            isGoal = false;
            if (single) {
                for (Goal g : goals) {
                    if (varNames[0].equals(g.varName) && varValues[0].equals(g.varValue)) {
                        isGoal = true;
                        break;
                    }
                }
            }
            successors = new ArrayList<LandmarkCheck>();
            predecessors = new ArrayList<LandmarkCheck>();
        }

        public LandmarkCheck(Goal g) {
            isRoot = false;
            single = true;
            isGoal = true;
            varNames = new String[1];
            varValues = new String[1];
            varNames[0] = g.varName;
            varValues[0] = g.varValue;
            successors = new ArrayList<LandmarkCheck>();
            predecessors = new ArrayList<LandmarkCheck>();
        }

        public boolean matches(Goal g) {
            if (!single || !isGoal) {
                return false;
            }
            return g.varName.equals(varNames[0]) && g.varValue.equals(varValues[0]);
        }

        public void removePredecessor(LandmarkCheck l) {
            for (int i = 0; i < predecessors.size(); i++) {
                if (predecessors.get(i) == l) {
                    predecessors.remove(i);
                    break;
                }
            }
        }

        public void addPredecessor(LandmarkCheck l) {
            predecessors.add(l);
        }

        public void addSuccessor(LandmarkCheck l) {
            successors.add(l);
        }

        public boolean goOnMulti(HashMap<String, ArrayList<String>> state, boolean[] checked) {
            if (checked[index]) {
                return false;
            }
            for (LandmarkCheck p : predecessors) {
                if (!checked[p.index]) {
                    return false;
                }
            }
            checked[index] = !single || holdsMulti(state);
            return checked[index];
        }

        private boolean holdsMulti(HashMap<String, ArrayList<String>> state) {
            for (int i = 0; i < varNames.length; i++) {
                ArrayList<String> values = state.get(varNames[i]);
                if (values != null && values.contains(varValues[i])) {
                    return true;
                }
            }
            return false;
        }

        public boolean goOn(HashMap<String, String> state, boolean checked[]) {
            if (checked[index]) {
                return false;
            }
            for (LandmarkCheck p : predecessors) {
                if (!checked[p.index]) {
                    return false;
                }
            }
            checked[index] = !single || holds(state);
            return checked[index];
        }

        private boolean holds(HashMap<String, String> state) {
            for (int i = 0; i < varNames.length; i++) {
                String stateValue = state.get(varNames[i]);
                if (stateValue != null && stateValue.equals(varValues[i])) {
                    return true;
                }
            }
            return false;
        }

        public String toString() {
            String s = varNames[0] + "=" + varValues[0];
            for (int i = 1; i < varNames.length; i++) {
                s += " v " + varNames[i] + "=" + varValues[i];
            }
            return s;
        }
    }

    /**
     * *******************************************************************
     */
    public static class DTGMessageFilter implements MessageFilter {

        private int requestId;
        private ArrayList<String> askedAgents;

        public DTGMessageFilter(ArrayList<String> askedAgents, int requestId) {
            this.requestId = requestId;
            this.askedAgents = askedAgents;
        }

        @Override
        public boolean validMessage(Message m) {
            if (m.content() instanceof ReplyTransitionCost) {
                return ((ReplyTransitionCost) m.content()).requestId == requestId
                        && askedAgents.contains(m.sender());
            }
            if (m.content() instanceof String
                    && ((String) m.content()).equals(AgentCommunication.END_STAGE_MESSAGE)) {
                return true;
            }
            return m.content() instanceof TransitionCostRequest;
        }
    }

    public static class WaitMessageFilter implements MessageFilter {
        @Override
        public boolean validMessage(Message m) {
            return (m.content() instanceof String) ||
                    (m.content() instanceof TransitionCostRequest);
        }
    }
}
