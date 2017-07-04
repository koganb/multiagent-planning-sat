package org.agreement_technologies.agents;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_communication.PlanningAgentListener;
import org.agreement_technologies.common.map_grounding.GroundedTask;
import org.agreement_technologies.common.map_landmarks.Landmarks;

public interface AgentListener {
    String getShortName();

    void setAgentListener(PlanningAgentListener paListener);

    void selectPlan(String planName);

    GroundedTask getGroundedTask();

    AgentCommunication getCommunication();

    Landmarks getLandmarks();
}
