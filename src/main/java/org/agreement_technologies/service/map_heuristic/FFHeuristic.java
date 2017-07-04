package org.agreement_technologies.service.map_heuristic;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_grounding.*;
import org.agreement_technologies.common.map_heuristic.HPlan;
import org.agreement_technologies.common.map_heuristic.Heuristic;
import org.agreement_technologies.common.map_planner.PlannerFactory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.PriorityQueue;

public class FFHeuristic implements Heuristic {
    private static final int PENALTY = 1000;
    protected GroundedTask groundedTask;                // Grounded task
    protected AgentCommunication comm;                    // Agent communication
    protected ArrayList<GroundedCond> goals, pgoals;    // Task goals
    protected HashMap<String, ArrayList<Action>> productors;
    protected HashMap<String, ArrayList<Action>> requirers;
    protected PlannerFactory pf;

    private int[] totalOrder;

    public FFHeuristic(AgentCommunication comm, GroundedTask gTask, PlannerFactory pf) {
        this.pf = pf;
        this.groundedTask = gTask;
        this.comm = comm;
        this.goals = new ArrayList<GroundedCond>();
        this.pgoals = groundedTask.getPreferences();
        ArrayList<GoalCondition> gc = HeuristicToolkit.computeTaskGoals(comm, gTask);
        for (GoalCondition g : gc) {
            GroundedVar var = null;
            for (GroundedVar v : gTask.getVars())
                if (v.toString().equals(g.varName)) {
                    var = v;
                    break;
                }
            if (var != null)
                goals.add(gTask.createGroundedCondition(GroundedCond.EQUAL, var, g.value));
        }
        productors = new HashMap<String, ArrayList<Action>>();
        requirers = new HashMap<String, ArrayList<Action>>();
        for (Action a : gTask.getActions()) {
            for (GroundedEff e : a.getEffs()) {
                String desc = e.getVar().toString() + "=" + e.getValue();
                ArrayList<Action> list = productors.get(desc);
                if (list == null) {
                    list = new ArrayList<Action>();
                    productors.put(desc, list);
                }
                list.add(a);
            }
            for (GroundedCond c : a.getPrecs()) {
                String desc = c.getVar().toString() + "=" + c.getValue();
                ArrayList<Action> list = requirers.get(desc);
                if (list == null) {
                    list = new ArrayList<Action>();
                    requirers.put(desc, list);
                }
                list.add(a);
            }
        }
    }

    @Override
    public void evaluatePlan(HPlan p, int threadIndex) {
        if (p.isSolution() || comm.numAgents() > 1) {
            p.setH(0, 0);
            for (int i = 0; i < pgoals.size(); i++) p.setHPriv(0, i);
            return;
        }
        totalOrder = p.linearization();
        HashMap<String, ArrayList<String>> varValues = p.computeMultiState(totalOrder, pf);
        RPG rpg = new RPG(varValues, goals, pgoals, requirers);
        p.setH(solveGoals(rpg, varValues, goals), 0);
        ArrayList<GroundedCond> privateGoal = new ArrayList<GroundedCond>(1);
        for (int i = 0; i < pgoals.size(); i++) {
            privateGoal.add(pgoals.get(i));
            p.setHPriv(solveGoals(rpg, varValues, privateGoal), i);
            privateGoal.clear();
        }
    }

    private int solveGoals(RPG rpg, HashMap<String, ArrayList<String>> varValues,
                           ArrayList<GroundedCond> goals) {
        int h = 0;
        PriorityQueue<RPG.VarValue> openConditions = new PriorityQueue<RPG.VarValue>();
        for (GroundedCond g : goals) {
            RPG.VarValue vg = rpg.getVarValue(g);
            if (vg == null) {
                h = PENALTY;
                break;
            }
            if (vg.level > 0)
                openConditions.add(vg);
        }
        Action bestAction;
        int bestCost = 0;
        if (h == 0)
            while (!openConditions.isEmpty()) {
                RPG.VarValue v = openConditions.poll();
                bestAction = null;
                ArrayList<Action> prod = productors.get(v.getId());
                for (Action a : prod)
                    if (rpg.getLevel(a) == v.level - 1) {
                        if (bestAction == null) {
                            bestAction = a;
                            bestCost = rpg.getDifficulty(a);
                        } else {
                            int cost = rpg.getDifficulty(a);
                            if (cost < bestCost) {
                                bestAction = a;
                                bestCost = cost;
                            }
                        }
                    }
                h++;
                for (GroundedCond prec : bestAction.getPrecs()) {
                    RPG.VarValue vp = rpg.getVarValue(prec);
                    if (vp.level > 0 && !openConditions.contains(vp))
                        openConditions.add(vp);
                }
            }
        return h;
    }

    @Override
    public void waitEndEvaluation() {
    }

    @Override
    public void startEvaluation(HPlan basePlan) {
    }

    @Override
    public Object getInformation(int infoFlag) {
        return null;
    }

    @Override
    public boolean supportsMultiThreading() {
        return false;
    }

    @Override
    public void evaluatePlanPrivacy(HPlan p, int threadIndex) {
        if (p.isSolution() || comm.numAgents() > 1 || pgoals.isEmpty()) {
            for (int i = 0; i < pgoals.size(); i++)
                p.setHPriv(0, i);
            return;
        }
        totalOrder = p.linearization();
        HashMap<String, ArrayList<String>> varValues = p.computeMultiState(totalOrder, pf);
        RPG rpg = new RPG(varValues, goals, pgoals, requirers);
        ArrayList<GroundedCond> privateGoal = new ArrayList<GroundedCond>(1);
        for (int i = 0; i < pgoals.size(); i++) {
            privateGoal.add(pgoals.get(i));
            p.setHPriv(solveGoals(rpg, varValues, privateGoal), i);
            privateGoal.clear();
        }
    }

    @Override
    public boolean requiresHLandStage() {
        return false;
    }

    @Override
    public void evaluatePlan(HPlan p, int threadIndex, ArrayList<Integer> achievedLandmarks) {
    }

    @Override
    public int numGlobalLandmarks() {
        return 0;
    }

    @Override
    public ArrayList<Integer> checkNewLandmarks(HPlan plan,
                                                BitSet achievedLandmarks) {
        return null;
    }
}
