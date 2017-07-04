/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.agreement_technologies.service.map_negotiation;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_negotiation.PlanSelection;
import org.agreement_technologies.common.map_planner.Plan;
import org.agreement_technologies.service.map_planner.POPSearchMethod;

/**
 * @author Alex
 */
class RunoffVoting implements PlanSelection {

    public RunoffVoting(AgentCommunication c, POPSearchMethod st) {
    }

    @Override
    public Plan selectNextPlan() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
