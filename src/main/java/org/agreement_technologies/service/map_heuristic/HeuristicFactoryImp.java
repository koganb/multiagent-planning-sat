package org.agreement_technologies.service.map_heuristic;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_grounding.GroundedTask;
import org.agreement_technologies.common.map_heuristic.Heuristic;
import org.agreement_technologies.common.map_heuristic.HeuristicFactory;
import org.agreement_technologies.common.map_planner.PlannerFactory;

public class HeuristicFactoryImp implements HeuristicFactory {

    @Override
    public Heuristic getHeuristic(int heuristic, AgentCommunication comm, GroundedTask gTask,
                                  PlannerFactory pf) {
        Heuristic h;
        switch (heuristic) {
            case BREATH:
                h = new BreadthHeuristic(comm, gTask);
                break;
            case FF:
                h = new FFHeuristic(comm, gTask, pf);
                break;
            case DTG:
                h = new DTGHeuristic(comm, gTask, pf);
                break;
            case LAND_DTG_NORM:
                h = new LandmarksHeuristic(comm, gTask, false, pf);
                break;
            case LAND_DTG_INC:
                h = new LandmarksHeuristic(comm, gTask, true, pf);
                break;
            default:
                h = null;
        }
        return h;
    }

    @Override
    public Object getHeuristicInfo(int heuristic, int infoFlag) {
        Object res = null;
        if (infoFlag == INFO_USES_LANDMARKS) {
            switch (heuristic) {
                case BREATH:
                    res = "no";
                    break;
                case FF:
                    res = "no";
                    break;
                case DTG:
                    res = "no";
                    break;
                case LAND_DTG_NORM:
                    res = "yes";
                    break;
                case LAND_DTG_INC:
                    res = "yes";
                    break;
            }
        }
        return res;
    }
}
