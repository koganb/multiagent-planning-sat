package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_planner.CausalLink;
import org.agreement_technologies.common.map_planner.Ordering;
import org.agreement_technologies.common.map_planner.Planner;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.tools.CustomArrayList;

/**
 * @author Alex
 */
public interface ExtendedPlanner extends Planner {
    CustomArrayList<CausalLink> getTotalCausalLinks();

    Step getInitialStep();

    Step getFinalStep();

    CustomArrayList<Ordering> getTotalOrderings();

    POPIncrementalPlan[] getAntecessors();

    boolean getModifiedCausalLinks();

    void setModifiedCausalLinks(boolean m);

    boolean getModifiedOrderings();

    void setModifiedOrderings(boolean m);

    public void solveThreat(POPInternalPlan father, boolean isFinalStep);

    void setNumOrderings(int n);

    void addCausalLink(CausalLink cl);

    void addOrdering(Ordering o);

    int getNumCausalLinks();

    void setNumCausalLinks(int n);
}
