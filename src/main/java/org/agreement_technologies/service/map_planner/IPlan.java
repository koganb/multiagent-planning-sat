package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_heuristic.HPlan;
import org.agreement_technologies.common.map_planner.CausalLink;
import org.agreement_technologies.common.map_planner.Ordering;
import org.agreement_technologies.common.map_planner.Plan;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.tools.CustomArrayList;

import java.util.ArrayList;

public interface IPlan extends HPlan {
    CustomArrayList<CausalLink> getTotalCausalLinks();

    ArrayList<Step> getTotalSteps();

    CustomArrayList<Ordering> getTotalOrderings();

    void setName(int n, Plan father);

    Plan getFather();
}
