package org.agreement_technologies.common.map_heuristic;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_grounding.GroundedTask;
import org.agreement_technologies.common.map_planner.PlannerFactory;

public interface HeuristicFactory {
    // Heuristic functions
    public static final int BREATH = 0;
    public static final int FF = 1;
    public static final int DTG = 2;
    public static final int LAND_DTG_NORM = 3;
    public static final int LAND_DTG_INC = 4;

    // Heuristic information
    public static final int INFO_USES_LANDMARKS = 1;

    // Methods
    Heuristic getHeuristic(int heuristic, AgentCommunication comm, GroundedTask groundedTask,
                           PlannerFactory pf);

    Object getHeuristicInfo(int heuristic, int infoFlag);
}
