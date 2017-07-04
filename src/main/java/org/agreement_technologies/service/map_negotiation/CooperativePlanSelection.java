
package org.agreement_technologies.service.map_negotiation;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_negotiation.PlanSelection;
import org.agreement_technologies.common.map_planner.Plan;
import org.agreement_technologies.service.map_planner.POPIncrementalPlan;
import org.agreement_technologies.service.map_planner.POPSearchMethod;

/**
 * Cooperative plan selection
 * Standard plan selection for cooperative agents without negotiation
 *
 * @author Alex
 */
public class CooperativePlanSelection implements PlanSelection {
    private AgentCommunication comm;
    private POPSearchMethod searchTree;

    public CooperativePlanSelection(AgentCommunication c, POPSearchMethod st) {
        this.comm = c;
        this.searchTree = st;
    }

    public Plan selectNextPlan() {
        //Single agent
        if (comm.numAgents() == 1)
            return (POPIncrementalPlan) searchTree.getNextPlan();
        //Multi-agent
        POPIncrementalPlan plan;
        //Baton agent
        if (comm.batonAgent()) {
            plan = (POPIncrementalPlan) searchTree.getNextPlan();
            if (plan != null)
                //The baton agent sends only the name of the plan
                comm.sendMessage(plan.getName(), true);
            else
                comm.sendMessage(AgentCommunication.NO_SOLUTION_MESSAGE, true);
        }
        //Non-baton agent
        else {
            String planName = (String) comm.receiveMessage(comm.getBatonAgent(), true);
            if (planName.equals(AgentCommunication.NO_SOLUTION_MESSAGE))
                plan = null;
            else
                //The agent selects and extracts a plan that matches the plan name it received
                plan = (POPIncrementalPlan) searchTree.removePlan(planName);
        }
        return plan;
    }

}
