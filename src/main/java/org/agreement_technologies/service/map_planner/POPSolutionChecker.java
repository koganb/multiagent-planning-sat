package org.agreement_technologies.service.map_planner;

/**
 * Abstract class, partial SolutionChecker implementation
 *
 * @author Alex
 */
public abstract class POPSolutionChecker implements SolutionChecker {

    @Override
    public Boolean keepsConstraints(POPInternalPlan incrementalCandidate, POPStep step) {
        if (incrementalCandidate.getThreats() == null)
            return true;
        if (incrementalCandidate.getThreats().size() > 0)
            return false;
        /*if(candidate.getThreats().size() > 0)
            return false;*/
        if (incrementalCandidate.getFather() == null)
            return false;

        int v = step.getAction().getPrecs().size() - (incrementalCandidate.getTotalCausalLinks().size() - incrementalCandidate.getPlanner().getNumCausalLinks());
        if (v > 0) return false;
        
        /*if(incrementalCandidate.getTotalCausalLinks().isEmpty() ||
                incrementalCandidate.getTotalCausalLinks().size() < step.getAction().getPrecs().size())
            return false;
        if (incrementalCandidate.getTotalCausalLinks().get(0).getIndex2() != step.getIndex())
            return false;
        for(int i = 0; i < step.getAction().getPrecs().size(); i++) {
            if(incrementalCandidate.getTotalCausalLinks().get(i).getIndex2() != step.getIndex())
                return false;
        }*/
        return true;
    }
}
