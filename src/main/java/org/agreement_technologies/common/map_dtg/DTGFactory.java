package org.agreement_technologies.common.map_dtg;

import org.agreement_technologies.common.map_grounding.GroundedTask;

public interface DTGFactory {
    DTGSet create(GroundedTask task);
}
