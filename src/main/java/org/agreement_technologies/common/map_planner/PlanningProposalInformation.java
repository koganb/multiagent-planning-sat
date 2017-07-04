package org.agreement_technologies.common.map_planner;

/**
 * This class stores a set of extra information about each plan proposal.
 *
 * @author spajares
 */
public interface PlanningProposalInformation {

    int getSolvedOpenGoals();

    void setSolvedOpenGoals(int solvedOpenGoals);

    int getAddedCausalLinks();

    void setAddedCausalLinks(int addedCausalLinks);

    int getAddedSteps();

    void setAddedSteps(int addedSteps);

    int getAddedOrderings();

    void setAddedOrderings(int addedOrderings);

}
