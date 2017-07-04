package org.agreement_technologies.service.map_negotiation;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_negotiation.NegotiationFactory;
import org.agreement_technologies.common.map_negotiation.PlanSelection;
import org.agreement_technologies.service.map_planner.POPSearchMethod;

/**
 * @author Alex
 */
public class NegotiationFactoryImp implements NegotiationFactory {
    private static final int BORDA_PROPOSALS = 10;
    private int negotiationType;

    public NegotiationFactoryImp(int neg) {
        this.negotiationType = neg;
    }

    @Override
    public PlanSelection getNegotiationMethod(AgentCommunication c, POPSearchMethod st) {
        PlanSelection ps;
        switch (negotiationType) {
            case COOPERATIVE:
                ps = new CooperativePlanSelection(c, st);
                break;
            case BORDA:
                ps = new CustomBordaNegotiation(c, st, BORDA_PROPOSALS);
                break;
            case RUNOFF:
                ps = new RunoffVoting(c, st);
                break;
            default:
                ps = null;
        }
        return ps;
    }

    @Override
    public int getNegotiationType() {
        return this.negotiationType;
    }
}
