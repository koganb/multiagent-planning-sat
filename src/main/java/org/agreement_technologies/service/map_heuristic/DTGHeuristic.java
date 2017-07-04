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
import org.agreement_technologies.common.map_planner.PlannerFactory;
import org.agreement_technologies.service.map_dtg.DTGSetImp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.PriorityQueue;

public class DTGHeuristic implements Heuristic {
    private static final int PENALTY = 1000;
    protected GroundedTask groundedTask;                        // Grounded task
    protected AgentCommunication comm;                            // Agent communication
    protected ArrayList<Goal> goals, pgoals;                    // Task goals
    protected HashMap<String, ArrayList<Action>> productors;    // Productor actions
    protected DTGSet dtgs;                                        // DTGs
    protected PlannerFactory pf;

    private HPlan basePlan;                            // Base plan
    private HPlan currentPlan;                        // Plan being evaluated
    private int ready, requestId;
    private int[] totalOrderBase;

    public DTGHeuristic(AgentCommunication comm, GroundedTask gTask, PlannerFactory pf) {
        this.pf = pf;
        this.groundedTask = gTask;
        this.comm = comm;
        dtgs = new DTGSetImp(gTask);
        dtgs.distributeDTGs(comm, gTask);
        this.goals = new ArrayList<Goal>();
        this.pgoals = new ArrayList<Goal>();
        ArrayList<GoalCondition> gc = HeuristicToolkit.computeTaskGoals(comm, gTask);
        for (GoalCondition g : gc) {
            GroundedVar var = null;
            for (GroundedVar v : gTask.getVars())
                if (v.toString().equals(g.varName)) {
                    var = v;
                    break;
                }
            if (var != null) {
                Goal ng = new Goal(gTask.createGroundedCondition(GroundedCond.EQUAL, var, g.value), 0);
                goals.add(ng);
            }
        }
        for (GroundedCond g : gTask.getPreferences())
            pgoals.add(new Goal(g, 0));
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
        requestId = 0;
    }

    private static String selectInitialValueMono(String varName, String endValue, DTG dtg,
                                                 HashMap<String, String> state, HashMap<String, ArrayList<String>> newValues) {
        String bestValue = state.get(varName);
        int bestCost = dtg.pathCost(bestValue, endValue, state, newValues, 0);
        ArrayList<String> valueList = newValues.get(varName);
        if (valueList != null)
            for (int i = 0; i < valueList.size(); i++) {
                String value = valueList.get(i);
                int cost = dtg.pathCost(value, endValue, state, newValues, 0);
                if (cost != -1 && cost < bestCost) {
                    bestCost = cost;
                    bestValue = value;
                }
            }
        return bestValue;
    }

    private static boolean holdsMono(String varName, String value, HashMap<String, String> state,
                                     HashMap<String, ArrayList<String>> newValues) {
        String v = state.get(varName);
        if (v != null && v.equals(value)) return true;
        ArrayList<String> values = newValues.get(varName);
        if (values == null) return false;
        return values.contains(value);
    }

    private static String selectInitialValueMulti(String varName, String endValue, DTG dtg,
                                                  HashMap<String, ArrayList<String>> varValues) {
        ArrayList<String> valueList = varValues.get(varName);
        if (valueList == null) return "?";
        String bestValue = null;
        int bestCost = -1;
        for (String value : valueList)
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
        return bestValue != null ? bestValue : "?";
    }

    /*********************************************************************/
    /*                         M O N O - A G E N T                       */
    private static boolean holds(String varName, String value, HashMap<String, ArrayList<String>> varValues) {
        ArrayList<String> values = varValues.get(varName);
        if (values == null) return false;
        return values.contains(value);
    }

    @Override
    public void startEvaluation(HPlan basePlan) {
        this.basePlan = basePlan;
        this.ready = 0;
        this.totalOrderBase = null;
    }

    @Override
    public void evaluatePlan(HPlan p, int threadIndex) {
        if (p.isSolution()) return;
        this.currentPlan = p;
        if (comm.numAgents() == 1)
            evaluateMonoagentPlan();
        else
            evaluateMultiagentPlan();
    }

    @Override
    public void evaluatePlanPrivacy(HPlan p, int threadIndex) {
        if (p.isSolution() || pgoals.isEmpty()) return;
        this.currentPlan = p;
        if (comm.numAgents() == 1)
            evaluateMonoagentPlanPrivacy(null, null);
        else
            evaluateMultiagentPlanPrivacy(null);
    }

    /*********************************************************************/

    private void evaluateMonoagentPlan() {
        int h = 0;
        HashMap<String, String> state;
        HashMap<String, ArrayList<String>> newValues = new HashMap<String, ArrayList<String>>();
        int totalOrder[] = currentPlan.linearization();
        state = currentPlan.computeState(totalOrder, pf);
        PriorityQueue<Goal> openGoals = new PriorityQueue<Goal>();
        for (Goal g : goals) {        // Global goals
            String v = g.varName, end = g.varValue;
            if (!holdsMono(v, end, state, newValues)) {
                String init = selectInitialValueMono(v, end, dtgs.getDTG(v), state, newValues);
                g.distance = pathCostMono(v, init, end, state, newValues);
                if (g.distance >= INFINITE) {
                    h = INFINITE;
                    break;
                }
                openGoals.add(g);
            }
        }
        while (!openGoals.isEmpty() && h < INFINITE) {
            Goal g = openGoals.poll();
            h += solveConditionMono(g, openGoals, state, newValues);
        }
        this.currentPlan.setH(h, 0);
        evaluateMonoagentPlanPrivacy(state, newValues);
    }

    private void evaluateMonoagentPlanPrivacy(HashMap<String, String> state,
                                              HashMap<String, ArrayList<String>> newValues) {
        if (state == null) {
            int totalOrder[] = currentPlan.linearization();
            state = currentPlan.computeState(totalOrder, pf);
            newValues = new HashMap<String, ArrayList<String>>();
        }
        PriorityQueue<Goal> openGoals = new PriorityQueue<Goal>();
        for (int i = 0; i < pgoals.size(); i++) {    // Preferences
            int hp = 0;
            Goal g = pgoals.get(i);
            String v = g.varName, end = g.varValue;
            if (!holdsMono(v, end, state, newValues)) {
                String init = selectInitialValueMono(v, end, dtgs.getDTG(v), state, newValues);
                int dst = pathCostMono(v, init, end, state, newValues);
                if (dst >= INFINITE) hp = INFINITE;
                else openGoals.add(new Goal(v, end, dst));
            }
            while (!openGoals.isEmpty() && hp < INFINITE) {
                g = openGoals.poll();
                hp += solveConditionMono(g, openGoals, state, newValues);
            }
            currentPlan.setHPriv(hp, i);
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
                                   HashMap<String, String> state, HashMap<String, ArrayList<String>> newValues) {
        int h = 0;
        String varName = goal.varName, varValue = goal.varValue;
        if (holdsMono(varName, varValue, state, newValues)) return h;
        DTG dtg = dtgs.getDTG(varName);
        String initValue = selectInitialValueMono(varName, varValue, dtg, state, newValues);
        String[] path = dtg.getPath(initValue, varValue, state, newValues, 0);
        if (path == null) return INFINITE;
        String prevValue = path[0], nextValue;
        for (int i = 1; i < path.length; i++) {
            nextValue = path[i];
            Action a = selectProductorMono(varName, prevValue, nextValue, state, newValues);
            if (a == null) {
                h = INFINITE;
                break;
            }
            h++;
            updateValuesAndGoalsMono(a, openGoals, state, newValues);
            prevValue = nextValue;
        }
        return h;
    }

    private Action selectProductorMono(String varName, String startValue, String endValue,
                                       HashMap<String, String> state, HashMap<String, ArrayList<String>> newValues) {
        ArrayList<Action> productors = this.productors.get(varName + "," + endValue);
        if (productors == null || productors.isEmpty()) return null;
        Action bestAction = null;
        int costBest = INFINITE;
        for (int i = 0; i < productors.size(); i++)
            if (hasPrecondition(productors.get(i), varName, startValue)) {
                int cost = computeCostMono(productors.get(i), state, newValues);
                if (cost < costBest) {
                    costBest = cost;
                    bestAction = productors.get(i);
                }
            }
        return bestAction;
    }

    private boolean hasPrecondition(Action action, String varName, String startValue) {
        for (GroundedCond prec : action.getPrecs())
            if (varName.equals(prec.getVar().toString())) {
                return prec.getValue().equals(startValue);
            }
        return true;    // Variable not in preconditions -> a specific value is not required
    }

    private void updateValuesAndGoalsMono(Action a, PriorityQueue<Goal> openGoals,
                                          HashMap<String, String> state, HashMap<String, ArrayList<String>> newValues) {
        // Add a's preconditions to open goals if they do not hold
        for (GroundedCond p : a.getPrecs()) {
            String precVarName = p.getVar().toString();
            if (!holdsMono(precVarName, p.getValue(), state, newValues)) {
                String precInitValue = selectInitialValueMono(precVarName, p.getValue(),
                        dtgs.getDTG(precVarName), state, newValues);
                Goal newOpenGoal = new Goal(p, pathCostMono(precVarName, precInitValue, p.getValue(),
                        state, newValues));
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
                    if (!values.contains(e.getValue()))
                        values.add(e.getValue());
                }
            }
        }
    }

    /*********************************************************************/
	/*                       M U L T I - A G E N T                       */
    private int pathCostMono(String var, String initValue, String endValue, HashMap<String, String> state,
                             HashMap<String, ArrayList<String>> newValues) {
        DTG dtg = dtgs.getDTG(var);
        return dtg.pathCost(initValue, endValue, state, newValues, 0);
    }

    private int computeCostMono(Action a, HashMap<String, String> state,
                                HashMap<String, ArrayList<String>> newValues) {
        int cost = 0;
        for (GroundedCond prec : a.getPrecs()) {
            String var = prec.getVar().toString();
            DTG dtg = dtgs.getDTG(var);
            String iValue = state.get(var);
            int minPrecCost = dtg.pathCost(iValue, prec.getValue(), state, newValues, 0);
            ArrayList<String> initValues = newValues.get(var);
            if (initValues != null)
                for (String initValue : initValues) {
                    int precCost = dtg.pathCost(initValue, prec.getValue(), state, newValues, 0);
                    if (precCost < minPrecCost) minPrecCost = precCost;
                }
            cost += minPrecCost;
        }
        return cost;
    }

    /*********************************************************************/

    private void evaluateMultiagentPlan() {
        int totalOrder[] = currentPlan.linearization();
        HashMap<String, ArrayList<String>> varValues = currentPlan.computeMultiState(totalOrder, pf);
        PriorityQueue<Goal> openGoals = new PriorityQueue<Goal>();
        int h = 0;
        for (Goal g : goals) {
            String v = g.varName, end = g.varValue;
            if (!holds(v, end, varValues)) {
                String init = selectInitialValueMulti(v, end, dtgs.getDTG(v), varValues);
                g.distance = pathCostMulti(v, init, end);
                openGoals.add(g);
            }
        }
        while (!openGoals.isEmpty())
            h += solveConditionMulti(openGoals.poll(), varValues, openGoals, null);
        currentPlan.setH(h, 0);
        evaluateMultiagentPlanPrivacy(varValues);
    }

    private void evaluateMultiagentPlanPrivacy(
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
            if (!holds(v, end, varValues)) {
                String init = selectInitialValueMulti(v, end, dtgs.getDTG(v), varValues);
                g.distance = pathCostMulti(v, init, end);
                openGoals.add(g);
            }
            while (!openGoals.isEmpty())
                hp += solveConditionMulti(openGoals.poll(), varValues, openGoals, null);
            currentPlan.setHPriv(hp, i);
        }
    }

    private int pathCostMulti(String var, String initValue, String endValue) {
        DTG dtg = dtgs.getDTG(var);
        return dtg.pathCostMulti(initValue, endValue);
    }

    /**
     * Multi-agent heuristic goal evaluation
     *
     * @param goal Goal to be evaluated
     * @return Goal cost
     */
    private int solveConditionMulti(Goal goal, HashMap<String, ArrayList<String>> varValues,
                                    PriorityQueue<Goal> openGoals, TransitionCostRequest tcr) {
        int h;
        String varName = goal.varName, varValue = goal.varValue;
        if (holds(varName, varValue, varValues)) return 0;
        DTG dtg = dtgs.getDTG(varName);
        String initValue = selectInitialValueMulti(varName, varValue, dtg, varValues);
        if (!dtg.unknownValue(varValue) && !dtg.unknownValue(initValue)) {    // Known initial value
            h = evaluateWithKnownValues(varName, initValue, varValue, dtg, varValues, tcr,
                    openGoals);
        } else {                        // Unknown values
            if (dtg.unknownValue(varValue))
                h = evaluateWithUnknownFinalvalue(varName, initValue, varValue, dtg, varValues,
                        tcr, openGoals);
            else
                h = evaluateWithUnknownInitialvalue(varName, varValue, dtg, varValues,
                        tcr, openGoals);
        }
        return h;
    }

    private int evaluateWithUnknownInitialvalue(String varName, String varValue, DTG dtg,
                                                HashMap<String, ArrayList<String>> varValues, TransitionCostRequest tcr,
                                                PriorityQueue<Goal> openGoals) {
        int h = PENALTY;
        DTGTransition[] transitions = dtg.getTransitionsFrom("?");
        for (DTGTransition t : transitions)
            if (tcr == null || !tcr.varName.equals(varName) || !tcr.endValue.equals(t.getFinalValue())) {
                //sop("Solving condition " + varName + "=" + varValue + " (init value: ?->" + t.getFinalValue() +  ")");
                int cost = requestTransitionCost(varName, "?", t.getFinalValue(), varValues, dtg, tcr);
                if (cost != PENALTY) {    // Achieved
                    ArrayList<String> values = varValues.get(varName);
                    if (values == null) {
                        values = new ArrayList<String>();
                        values.add(t.getFinalValue());
                        varValues.put(varName, values);
                    } else if (!values.contains(t.getFinalValue()))
                        values.add(t.getFinalValue());
                    int restCost = evaluateWithKnownValues(varName, t.getFinalValue(), varValue, dtg,
                            varValues, tcr, openGoals);
                    if (restCost != PENALTY) {
                        h = cost + restCost;
                        break;
                    }
                }
            }
        return h;
    }

    private int evaluateWithUnknownFinalvalue(String varName, String initValue, String varValue,
                                              DTG dtg, HashMap<String, ArrayList<String>> varValues, TransitionCostRequest tcr,
                                              PriorityQueue<Goal> openGoals) {
        int h = PENALTY;
        DTGTransition[] transitions = dtg.getTransitionsTo("?");
        if (transitions != null) {
            for (DTGTransition t : transitions) {
                openGoals = new PriorityQueue<DTGHeuristic.Goal>();
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

    private int evaluateWithKnownValues(String varName, String initValue, String varValue, DTG dtg,
                                        HashMap<String, ArrayList<String>> varValues, TransitionCostRequest tcr,
                                        PriorityQueue<Goal> openGoals) {
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
                //if (comm.getThisAgentName().equals("distributor1")) System.out.println("h=" + h);
                ArrayList<String> values = varValues.get(varName);
                if (values == null) {
                    values = new ArrayList<String>();
                    values.add(nextValue);
                    varValues.put(varName, values);
                } else if (!values.contains(nextValue))
                    values.add(nextValue);
            } else {
                h++;
                // Add a's preconditions to open goals if they do not hold
                //sop("Action: " + a.toString());
                for (GroundedCond p : a.getPrecs()) {
                    precVarName = p.getVar().toString();
                    if (!holds(precVarName, p.getValue(), varValues)) {
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
                    } else if (!values.contains(e.getValue()))
                        values.add(e.getValue());
                }
            }
            prevValue = nextValue;
        }
        return h;
    }

    private Action selectProductorMulti(String varName, String startValue, String endValue,
                                        HashMap<String, ArrayList<String>> varValues) {
        ArrayList<Action> productors = this.productors.get(varName + "," + endValue);
        if (productors == null || productors.isEmpty()) return null;
        Action bestAction = null;
        int costBest = PENALTY;
        for (int i = 0; i < productors.size(); i++)
            if (hasPrecondition(productors.get(i), varName, startValue)) {
                int cost = computeCostMulti(productors.get(i), varValues);
                if (cost < costBest) {
                    costBest = cost;
                    bestAction = productors.get(i);
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
                    if (precCost < minPrecCost) minPrecCost = precCost;
                }
                //if (minPrecCost == PENALTY)
                //	System.out.println(var + "-/->" + prec.getValue());
                cost += minPrecCost;
            } else {
                cost += dtg.pathCostMulti("?", prec.getValue());
            }
        }
        return cost;
    }

    private int requestTransitionCost(String varName, String prevValue, String nextValue,
                                      HashMap<String, ArrayList<String>> varValues, DTG dtg, TransitionCostRequest prevTcr) {
        DTGTransition t = dtg.getTransition(prevValue, nextValue);
        ArrayList<String> askedAgents = new ArrayList<String>();
        int h = PENALTY;
        for (String ag : t.getAgents())    // Send requests
            if (!ag.equals(comm.getThisAgentName())) {
                if (detectLoop(ag, prevTcr))
                    continue;
                TransitionCostRequest tcr = new TransitionCostRequest(varName, prevValue, nextValue,
                        comm.getThisAgentName(), prevTcr, requestId);
                tcr.setState(varValues, groundedTask, ag);
                //sop("Requesting transition cost of " + varName + "(" + prevValue + "->" + nextValue + ") to " + ag);
                comm.sendMessage(ag, tcr, false);
                askedAgents.add(ag);
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
                if (cost < h) h = cost;
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
        if (t == null) return false;
        int index = t.agents.indexOf(ag);
        if (index == -1) return false;
        int count = 1;
        for (int i = index + 1; i < t.agents.size(); i++) {
            if (t.agents.get(i).equals(ag)) {
                count++;
                if (count > 2) return true;
            }
        }
        return false;
    }

    private void evaluateRequest(TransitionCostRequest t, String fromAgent) {
        //sop("Request from " + fromAgent + ": " + t.toString());
        if (totalOrderBase == null)
            totalOrderBase = basePlan.linearization();
        HashMap<String, ArrayList<String>> varValues = basePlan.computeMultiState(totalOrderBase, pf);
        t.updateState(varValues);
        PriorityQueue<Goal> openGoals = new PriorityQueue<Goal>();
        openGoals.add(t.getGoal());
        int h = 0;
        while (!openGoals.isEmpty()) {
            h += solveConditionMulti(openGoals.poll(), varValues, openGoals, t);
        }
        //sop("Sending reply (" + h + ") to agent " + fromAgent);
        comm.sendMessage(fromAgent, new ReplyTransitionCost(h, t.requestId), false);
    }

    @Override
    public void waitEndEvaluation() {
        if (comm.batonAgent()) {
            ready++;
            //sop("Baton agent waiting end stage messages", null);
            while (ready < comm.numAgents()) {
                Serializable msg = comm.receiveMessage(false);
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
                Serializable msg = comm.receiveMessage(false);
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
        return false;
    }

    /**********************************************************************/
	/*                              G O A L                               */

    /**********************************************************************/

	/*
	private static class Fluent implements LandmarkFluent {
		String var, value;

		public Fluent(String varName, String varValue) {
			var = varName;
			value = varValue;
		}
		@Override
		public String toString() {
			return var + "=" + value;
		}
		@Override
		public int hashCode() {
			return (var + "=" + value).hashCode();
		}
		@Override
		public boolean equals(Object x) {
			LandmarkFluent f = (LandmarkFluent) x;
			return var.equals(f.getVarName()) && value.equals(f.getValue());
		}
		@Override
		public GroundedVar getVar() {
			throw new RuntimeException("Not implemented");
		}
		@Override
		public String getValue() {
			return value;
		}
		@Override
		public String getVarName() {
			return var;
		}
	}*/
    @Override
    public Object getInformation(int infoFlag) {
        return null;
    }

    /**********************************************************************/
	/*         T R A N S I T I O N    C O S T    R E Q U E S T            */
    @Override
    public boolean requiresHLandStage() {
        return false;
    }

    /**********************************************************************/
	/*            R E P L Y    T R A N S I T I O N    C O S T             */
    @Override
    public void evaluatePlan(HPlan p, int threadIndex, ArrayList<Integer> achievedLandmarks) {
    }

    /**********************************************************************/
	/*               D T G    M E S S A G E    F I L T E R                */
    @Override
    public int numGlobalLandmarks() {
        return 0;
    }

    /**********************************************************************/
	/*                              G O A L                               */
    @Override
    public ArrayList<Integer> checkNewLandmarks(HPlan plan,
                                                BitSet achievedLandmarks) {
        return null;
    }

    /**********************************************************************/

    private static class Goal implements Comparable<Goal> {
        String varName, varValue;
        int distance;

        public Goal(GroundedCond goal, int distance) {
            this(goal.getVar().toString(), goal.getValue(), distance);
        }

        /*public Fluent toFluent() {
            return new Fluent(varName, varValue);
        }*/
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

    /**********************************************************************/

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
            if (prevTcr != null)
                for (String ag : prevTcr.agents)
                    this.agents.add(ag);
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
                    for (int i = 1; i < list.size(); i++)
                        values.add(list.get(i));
                    varValues.put(list.get(0), values);
                } else {
                    for (int i = 1; i < list.size(); i++) {
                        if (!values.contains(list.get(i)))
                            values.add(list.get(i));
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
                    for (String value : list)
                        if (gv.shareable(value, toAgent))
                            newList.add(value);
                    this.varValuesList.add(newList);
                }
            }
        }

        public String toString() {
            return varName + "(" + startValue + "->" + endValue + ")";
        }
    }

    /**********************************************************************/

    public static class ReplyTransitionCost implements Serializable {
        private static final long serialVersionUID = 8450612556336972847L;
        int cost;
        int requestId;

        public ReplyTransitionCost(int cost, int requestId) {
            this.cost = cost;
            this.requestId = requestId;
        }
    }

    /**********************************************************************/

    public static class DTGMessageFilter implements MessageFilter {
        private int requestId;
        private ArrayList<String> askedAgents;

        public DTGMessageFilter(ArrayList<String> askedAgents, int requestId) {
            this.requestId = requestId;
            this.askedAgents = askedAgents;
        }

        @Override
        public boolean validMessage(Message m) {
            if (m.content() instanceof ReplyTransitionCost)
                return ((ReplyTransitionCost) m.content()).requestId == requestId &&
                        askedAgents.contains(m.sender());
            if (m.content() instanceof String &&
                    ((String) m.content()).equals(AgentCommunication.END_STAGE_MESSAGE))
                return true;
            return m.content() instanceof TransitionCostRequest;
        }
    }

	/*
	public void sop(String s, TransitionCostRequest t) {
		System.out.println("[" +  comm.getThisAgentName() + "] " + s + 
				(t == null ? "" : " (" + t.toString() + ")"));
	}*/
}
