package org.agreement_technologies.common.map_planner;

import org.agreement_technologies.service.map_planner.POPFunction;

/**
 * Common interface for causal links
 *
 * @author Alex
 */
public interface CausalLink extends Ordering {
    Condition getCondition();

    Step getStep1();

    Step getStep2();

    POPFunction getFunction();
}
