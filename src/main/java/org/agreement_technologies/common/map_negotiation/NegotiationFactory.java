package org.agreement_technologies.common.map_negotiation;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.service.map_planner.POPSearchMethod;

/**
 * @author Alex
 */
public interface NegotiationFactory {
    //Negotiation methods
    public static final int COOPERATIVE = 0;
    public static final int BORDA = 1;
    public static final int RUNOFF = 2;
    public static final int BORDA_PROPOSALS = 10;

    // Methods
    PlanSelection getNegotiationMethod(AgentCommunication c, POPSearchMethod st);

    int getNegotiationType();
}
