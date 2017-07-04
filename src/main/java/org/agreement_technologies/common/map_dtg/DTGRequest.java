package org.agreement_technologies.common.map_dtg;

import java.io.Serializable;

public interface DTGRequest extends Serializable {
    String toAgent();

    String fromAgent();

    String varName();

    String reachedValue();

    int reachedValueCost();

    String initialValue();
}
