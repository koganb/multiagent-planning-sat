package org.agreement_technologies.common.map_dtg;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_grounding.GroundedTask;
import org.agreement_technologies.common.map_grounding.GroundedVar;

public interface DTGSet {

    void distributeDTGs(AgentCommunication comm, GroundedTask gTask);

    DTG getDTG(GroundedVar v);

    DTG getDTG(String varName);

    void clearCache(int threadIndex);
}
