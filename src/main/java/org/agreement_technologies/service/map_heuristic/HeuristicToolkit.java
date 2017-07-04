package org.agreement_technologies.service.map_heuristic;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_grounding.GroundedCond;
import org.agreement_technologies.common.map_grounding.GroundedTask;
import org.agreement_technologies.common.map_planner.Condition;
import org.agreement_technologies.common.map_planner.Plan;
import org.agreement_technologies.common.map_planner.Step;

import java.util.ArrayList;
import java.util.HashMap;

public class HeuristicToolkit {

    @SuppressWarnings("unchecked")
    public static ArrayList<GoalCondition> computeTaskGoals(AgentCommunication comm,
                                                            GroundedTask gTask) {
        ArrayList<GoalCondition> ocs = new ArrayList<GoalCondition>();
        for (GroundedCond cond : gTask.getGlobalGoals())
            ocs.add(new GoalCondition(cond.getVar().toString(), cond.getValue()));
        if (comm.numAgents() > 1) {
            if (comm.batonAgent()) {
                for (String ag : comm.getOtherAgents()) {
                    ArrayList<GoalCondition> data = (ArrayList<GoalCondition>)
                            comm.receiveMessage(ag, true);
                    updateConditions(ocs, data);
                }
                comm.sendMessage(ocs, true);
            } else {
                comm.sendMessage(comm.getBatonAgent(), ocs, true);
                ocs = (ArrayList<GoalCondition>) comm.receiveMessage(
                        comm.getBatonAgent(), true);
            }
        }
        return ocs;
    }

    public static HashMap<Integer, Integer> computeState(Plan p, int[] stepsOrder) {
        HashMap<Integer, Integer> varValue = new HashMap<Integer, Integer>();
        ArrayList<Step> stepList = p.getStepsArray();
        for (int step : stepsOrder) {
            Step a = stepList.get(step);
            for (Condition eff : a.getEffs())
                varValue.put(eff.getVarCode(), eff.getValueCode());
        }
        return varValue;
    }

    /*************************************************************/
    /*               P R I V A T E    M E T H O D S              */

    /*************************************************************/

    private static void updateConditions(ArrayList<GoalCondition> ocs,
                                         ArrayList<GoalCondition> data) {
        for (GoalCondition cond : data) {
            boolean found = false;
            for (GoalCondition oc : ocs)
                if (cond.equals(oc)) {
                    found = true;
                    break;
                }
            if (!found) ocs.add(cond);
        }
    }
}
