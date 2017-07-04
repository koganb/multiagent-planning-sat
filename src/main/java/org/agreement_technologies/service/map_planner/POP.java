package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_communication.PlanningAgentListener;
import org.agreement_technologies.common.map_heuristic.Heuristic;
import org.agreement_technologies.common.map_planner.CausalLink;
import org.agreement_technologies.common.map_planner.OpenCondition;
import org.agreement_technologies.common.map_planner.Ordering;
import org.agreement_technologies.common.map_planner.PlannerFactory;
import org.agreement_technologies.service.tools.CustomArrayList;

import java.util.ArrayList;

/**
 * Partial-Order Planner main class.
 * Parameters: configuration object, search tree, timeout, solution checking method, auxiliar array, base plan, incremental version of the base plan
 *
 * @author Alex
 */
public class POP extends Planner {
    /**
     * Builds a POP from scratch
     *
     * @param conf     Configuration object
     * @param basePlan Base plan of the planning task
     */
    public POP(PlannerFactoryImp conf, POPStep initial, POPStep last,
               ArrayList<OpenCondition> goals, Heuristic h, AgentCommunication comm,
               PlanningAgentListener agentListener, int searchType, boolean anytime) {
        super(comm);
        this.configuration = conf;

        this.solutionChecker = configuration.getSolutionChecker();
        this.agentListener = agentListener;
        this.heuristic = h;
        this.comm = comm;

        this.myAgent = conf.getAgent();
        this.parent = this;

        setInitialStep(initial);
        setFinalStep(last);

        initialInternalPlan = new POPInternalPlan(null, null, null, null, goals, null, null, false, this);
        this.initialIncrementalPlan = new POPIncrementalPlan(initialInternalPlan, null, this);

        if (agentListener != null)
            agentListener.newPlan(initialIncrementalPlan, configuration);

        //this.planComparator = conf.getPlanComparator();
        this.successors = new ArrayList<POPInternalPlan>();

        initialIncrementalPlan.setName(0, null);
        POPComparator pcmp;
        switch (searchType) {
            case PlannerFactory.SEARCH_SPEED:
                pcmp = new POPComparatorASpeed(
                        this.configuration.getGroundedTask());
                break;
            case PlannerFactory.SEARCH_BALANCED:
                pcmp = new POPComparatorABalanced(
                        this.configuration.getGroundedTask());
                break;
            case PlannerFactory.SEARCH_LANDMARKS:
                pcmp = new POPComparatorALandmarks(
                        this.configuration.getGroundedTask());
                break;
            default:
                pcmp = new POPComparatorAQuality(
                        this.configuration.getGroundedTask());
                break;
        }
        //this.searchTree = new POPSearchMethodA(initialIncrementalPlan, pcmp);
        this.searchTree = new POPSearchMethodTwoQueues(initialIncrementalPlan);
        //Create plan selection method
        planSelection = configuration.getNegotiationFactory().getNegotiationMethod(comm, searchTree);

        totalCausalLinks = new CustomArrayList<CausalLink>(50);
        totalOrderings = new CustomArrayList<Ordering>(50);

        this.matrix = new POPMatrix(20);//new POPOrderingManagerNoMemorization();
        memoization = new StateMemoization(configuration.getNumGlobalVariables());

        this.isAnytime = anytime;
    }

    /**
     * Main loop of the planner: selects and solves plans' flaws and manages the search tree
     *
     * @return Solution plan or valid refinement; null if the complete search tree has been explored without finding further solutions
     */
    public ArrayList<IPlan> POPForwardLoop() {
        ArrayList<IPlan> succ, refinements = new ArrayList<IPlan>();
        this.hashEffects = null;

        //The first plan to be processed is a copy of the planner's initial plan
        POPInternalPlan auxInternal = currentInternal;
        POPStep step;

        //Pre-calculate the applicable actions for the current plan
        ArrayList<POPAction> applicableActions = super.calculateApplicableActions();

        //Search loop; add an applicable action to the plan
        for (POPAction act : applicableActions) {
            currentInternal = auxInternal;
            //matrix.rebuild(currentIncremental);

            if (!this.internalSearchTree.isEmpty())
                this.internalSearchTree.getNextPlan();
            this.internalSearchTree.addPlan(currentInternal);
            //Check if the current action is applicable
            //Create a step associated to the action
            step = new POPStep(act, currentInternal.numSteps(), this.myAgent);
            succ = this.solveAction(step);
            if (succ.size() > 0) {
                for (IPlan s : succ)
                    refinements.add(s);
                succ.clear();
            }
        }

        //Clean causal link and ordering arrays from last usage
        restoreCausalLinks();
        restoreOrderings();

        //Return refinement plans
        return refinements;
    }

    public IPlan sendProposalsMonoagent(ArrayList<IPlan> proposals, IPlan basePlan) {
        IPlan solution = null;
        for (int i = 0; i < proposals.size(); i++) {
            IPlan plan = proposals.get(i);
            if (plan.isSolution() || memoization.search((POPIncrementalPlan) plan) == null) {
                plan.setName(i, basePlan);
                if (plan.isSolution()) plan.setH(0, 0);
                else heuristic.evaluatePlan(plan, 0);
                searchTree.addPlan(plan);
                if (!plan.isSolution())
                    memoization.add((POPIncrementalPlan) plan);
                else
                    solution = plan;
                if (agentListener != null)
                    agentListener.newPlan(plan, configuration);
            }
        }
        return solution;
    }

    @Override
    public void evaluatePlan(IPlan plan, ArrayList<Planner.EvaluationThread> evThreads) {
    }
}