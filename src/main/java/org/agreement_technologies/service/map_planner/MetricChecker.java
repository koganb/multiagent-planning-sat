package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_negotiation.NegotiationFactory;
import org.agreement_technologies.common.map_planner.Plan;

/**
 * Checks if a plan has the best average metric value
 *
 * @author Alex
 */
public class MetricChecker {
    double bestMetric;
    private AgentCommunication comm;

    public MetricChecker(AgentCommunication c) {
        bestMetric = Double.MAX_VALUE;
        comm = c;
    }

    public boolean isBestSolution(Plan solution, int type) {
        if (type == NegotiationFactory.COOPERATIVE)
            return true;

        double avgMetric = solution.getMetric();
        String agentMetric;
        if (comm.batonAgent()) {
            for (int i = 0; i < comm.getOtherAgents().size(); i++) {
                agentMetric = (String) comm.receiveMessage(true);
                avgMetric += Double.parseDouble(agentMetric);
            }
            avgMetric = avgMetric / comm.getAgentList().size();

            /************* Solución provisional **************/
            avgMetric += ((POPIncrementalPlan) solution).computeMakespan();
            /************* Hablar con Óscar     **************/

            //The baton agent sends the average metric data to the rest of agents
            comm.sendMessage(Double.toString(avgMetric), true);
        }
        //Non-baton agent
        else {
            agentMetric = Double.toString(avgMetric);
            //Send this agent's metric to the baton agent
            comm.sendMessage(comm.getBatonAgent(), agentMetric, true);
            //Receive the average metric data from the baton agent
            agentMetric = (String) comm.receiveMessage(comm.getBatonAgent(), true);
            avgMetric = Double.parseDouble(agentMetric);
        }

        boolean isBest = avgMetric < bestMetric;
        if (isBest)
            bestMetric = avgMetric;

        return isBest;
    }

    public double getBestMetric() {
        return bestMetric;
    }
}
