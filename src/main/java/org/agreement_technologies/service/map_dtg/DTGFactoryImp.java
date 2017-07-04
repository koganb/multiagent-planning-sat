package org.agreement_technologies.service.map_dtg;

import org.agreement_technologies.common.map_dtg.DTGFactory;
import org.agreement_technologies.common.map_dtg.DTGSet;
import org.agreement_technologies.common.map_grounding.GroundedTask;

public class DTGFactoryImp implements DTGFactory {

    @Override
    public DTGSet create(GroundedTask task) {
        return new DTGSetImp(task);
    }

}
