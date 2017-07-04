package org.agreement_technologies.service.map_dtg;

import org.agreement_technologies.common.map_dtg.DTGRequest;


public class DTGRequestImp implements DTGRequest {
    private static final long serialVersionUID = 4766115568690770517L;
    private String fromAgent, toAgent;
    private String varName;
    private String initialValue;
    private String reachedValue;
    private int reachedValueCost;

    public DTGRequestImp(String fromAgent, String toAgent, String varName,
                         String initialValue, String reachedValue, int reachedValueCost) {
        this.fromAgent = fromAgent;
        this.toAgent = toAgent;
        this.varName = varName;
        this.reachedValue = reachedValue;
        this.reachedValueCost = reachedValueCost;
        this.initialValue = initialValue;
    }

    @Override
    public String toString() {
        return fromAgent + ":" + varName + " -> " +
                toAgent + ":" + reachedValue + ":" + reachedValueCost;
    }

    @Override
    public String toAgent() {
        return toAgent;
    }

    @Override
    public String fromAgent() {
        return fromAgent;
    }

    @Override
    public String varName() {
        return varName;
    }

    @Override
    public String reachedValue() {
        return reachedValue;
    }

    @Override
    public int reachedValueCost() {
        return reachedValueCost;
    }

    @Override
    public String initialValue() {
        return initialValue;
    }
}
