package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_grounding.GroundedTask;
import org.agreement_technologies.common.map_planner.PlannerFactory;

/**
 * Solution checking for problems with private goals
 *
 * @author Alex
 */
public class POPSolutionCheckerPrivateGoals extends POPSolutionChecker {
    private AgentCommunication comm;
    private GroundedTask task;
    private double threshold;

    public POPSolutionCheckerPrivateGoals(AgentCommunication c, GroundedTask t) {
        comm = c;
        task = t;
        threshold = task.getMetricThreshold();
    }

    public Boolean isSolution(POPIncrementalPlan candidate, PlannerFactory pf) {
        //Single agent
        if (comm.numAgents() == 1) {
            if (candidate.isSolution())
                return (task.evaluateMetric(candidate.computeState(
                        candidate.getFather().linearization(), pf), 0) - threshold) <= 0.0f;
            else
                return false;
        }
        //Multi-agent
        //If all the global goals are fulfilled, check if the agents' preferences are met
        if (candidate.isSolution()) {
            int approvals = 0, totalAgents = comm.getAgentList().size();
            //Baton agent: receives other agents' results
            if (comm.batonAgent()) {
                //Receive results from other agents
                for (int i = 0; i < comm.getOtherAgents().size(); i++) {
                    if ((Boolean) comm.receiveMessage(true) == true)
                        approvals++;
                }
                //Calculate own result
                double metric = task.evaluateMetric(
                        candidate.computeState(candidate.getFather().linearization(), pf), 0);
                candidate.setMetric(metric);

                if (metric - threshold <= 0.0f)
                    approvals++;
                //Check if more than the 50% of the agents satisfy their metrics
                Boolean finalRes = (float) approvals / totalAgents > 0.5f;
                //Send final result to the rest of agents
                comm.sendMessage(finalRes, false);

                return finalRes;
            }
            //Participant agent
            else {
                //The participant checks if its metric value reaches the threshold,
                //and communicates the result to the baton agent
                double metric = task.evaluateMetric(
                        candidate.computeState(candidate.getFather().linearization(), pf), 0);
                candidate.setMetric(metric);

                Boolean res = metric - threshold <= 0.0f;
                comm.sendMessage(comm.getBatonAgent(), res, true);

                //The participant waits for the average result computed by the baton agent result and returns it
                res = (Boolean) comm.receiveMessage(false);

                return res;
            }
        } else return false;
    }
}
