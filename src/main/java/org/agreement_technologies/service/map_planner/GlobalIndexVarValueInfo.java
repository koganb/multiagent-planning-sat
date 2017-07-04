package org.agreement_technologies.service.map_planner;

import java.io.Serializable;

/**
 * @author Alex
 */
public class GlobalIndexVarValueInfo implements Serializable {
    private static final long serialVersionUID = -24058733714951171L;
    private String varValue;
    private int globalIndex;

    public GlobalIndexVarValueInfo(int iv, String v) {
        varValue = v;
        globalIndex = iv;
    }

    public String getItem() {
        return varValue;
    }

    public Integer getGlobalIndex() {
        return globalIndex;
    }
}
