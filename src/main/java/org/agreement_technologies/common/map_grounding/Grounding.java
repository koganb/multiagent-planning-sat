package org.agreement_technologies.common.map_grounding;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_parser.Task;

/**
 * Planning task grounding
 *
 * @author Oscar Sapena
 * @since April 2011
 */
public interface Grounding {

    // Computes the list of static functions
    void computeStaticFunctions(Task task, AgentCommunication comm);

    // Grounds a planning task. Also receives the name of the agent
    GroundedTask ground(Task task, AgentCommunication comm, boolean negationByFailure);
}
