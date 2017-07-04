package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_planner.PlannerFactory;

/**
 * Checks whether a single-agent partial-order plan is a solution. If the plan does not have open conditions nor threats, it is a solution.
 *
 * @author Alex
 */
class POPSolutionCheckerCooperative extends POPSolutionChecker {

    public POPSolutionCheckerCooperative() {

    }

    public Boolean isSolution(POPIncrementalPlan candidate, PlannerFactory pf) {
        return candidate.isSolution();
    }
}
