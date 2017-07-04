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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Multi-thread Partial-Order Planner class. Extends from POP. Parameters:
 * configuration object, search tree, timeout, solution checking method,
 * auxiliar array, base plan, incremental version of the base plan
 *
 * @author Alex
 */
public class POPMultiThread extends Planner {
    private int totalThreads;
    private ArrayList<POPThread> runnables;
    private ArrayList<Thread> threads;
    private boolean multithreadHeuristic;

    /**
     * Builds a POP from scratch
     *
     * @param conf     Configuration object
     * @param basePlan Base plan of the planning task
     */
    public POPMultiThread(PlannerFactoryImp conf, POPStep initial,
                          POPStep last, ArrayList<OpenCondition> goals, Heuristic h,
                          AgentCommunication comm, PlanningAgentListener agentListener,
                          int searchType, boolean anytime) {
        super(comm);
        this.parent = this;

        this.configuration = conf;
        this.solutionChecker = configuration.getSolutionChecker();
        this.agentListener = agentListener;
        this.heuristic = h;
        this.comm = comm;

        this.myAgent = conf.getAgent();

        // The base plan and its incremental version are stored as class members
        // this.initialPlan = basePlan;

        setInitialStep(initial);
        setFinalStep(last);

        initialInternalPlan = new POPInternalPlan(null, null, null, null,
                goals, null, null, false, this);
        this.initialIncrementalPlan = new POPIncrementalPlan(
                initialInternalPlan, null, this);

        if (agentListener != null)
            agentListener.newPlan(initialIncrementalPlan, configuration);

        // this.planComparator = conf.getPlanComparator();
        this.successors = new ArrayList<POPInternalPlan>();

        initialIncrementalPlan.setName(0, null);
        POPComparator pcmp;
        switch (searchType) {
            case PlannerFactory.SEARCH_SPEED:
                pcmp = new POPComparatorASpeed(this.configuration.getGroundedTask());
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
        this.searchTree = new POPSearchMethodTwoQueues(initialIncrementalPlan);
        // Create plan selection method
        planSelection = configuration.getNegotiationFactory()
                .getNegotiationMethod(comm, searchTree);

        totalCausalLinks = new CustomArrayList<CausalLink>(50);
        totalOrderings = new CustomArrayList<Ordering>(50);

        this.matrix = new POPMatrix(20);// new
        // POPOrderingManagerNoMemorization();
        memoization = new StateMemoization(configuration.getNumGlobalVariables());

        this.totalThreads = conf.getTotalThreads();
        runnables = new ArrayList<POPThread>(this.totalThreads);
        threads = new ArrayList<Thread>(this.totalThreads);

        for (int i = 0; i < this.totalThreads; i++) {
            ArrayList<POPAction> threadActions = new ArrayList<POPAction>();

            runnables.add(new POPThread(threadActions, conf, goals, initial,
                    last, this));
        }

        this.multithreadHeuristic = h.supportsMultiThreading();

        this.isAnytime = anytime;
    }

    /**
     * Main loop of the planner: selects and solves plans' flaws and manages the
     * search tree
     *
     * @return Solution plan or valid refinement; null if the complete search
     * tree has been explored without finding further solutions
     */
    @Override
    public ArrayList<IPlan> POPForwardLoop() {
        ArrayList<IPlan> refinements = new ArrayList<IPlan>();
        this.hashEffects = null;

        initialInternalPlan.setNumSteps(-1);

        int i, j;

        /**********************************************************************/
        // Initialize thread variables
        for (POPThread t : this.runnables) {
            t.setCurrentInternalPlan(currentInternal);
            t.initializeSearchTree(initialInternalPlan);
        }

        // ArrayList<POPAction> notApplied = new ArrayList<POPAction>();
        // ArrayList<POPAction> applied = new ArrayList<POPAction>();

        // Calculate applicable actions, distribute them among threads
        int actionsPerThread, remainder, index = 0;
        for (POPThread r : runnables)
            r.clearThreadActions();

        ArrayList<POPAction> applicableActions = super
                .calculateApplicableActions();

        actionsPerThread = applicableActions.size() / totalThreads;
        remainder = applicableActions.size() % totalThreads;

        // Add actions to each of the threads
        for (i = 0; i < totalThreads; i++) {
            for (j = actionsPerThread * index; j < actionsPerThread
                    * (index + 1); j++)
                runnables.get(i).addApplicableAction(applicableActions.get(j));
            index++;
        }
        // Add the remainding actions to the last thread
        for (i = actionsPerThread * index; i < actionsPerThread * index
                + remainder; i++)
            runnables.get(runnables.size() - 1).addApplicableAction(
                    applicableActions.get(i));

        /**********************************************************************/

        // Launch threads to add applicable actions to the plan
        threads.clear();
        for (i = 0; i < this.totalThreads; i++) {
            threads.add(new Thread(this.runnables.get(i)));
            threads.get(i).start();
        }

        // Wait for each thread to conclude its execution
        for (i = 0; i < this.totalThreads; i++) {
            try {
                this.threads.get(i).join();
            } catch (InterruptedException ex) {
                Logger.getLogger(POPMultiThread.class.getName()).log(
                        Level.SEVERE, null, ex);
            }
        }

        // Add the refinement plans obtained by each thread to the main
        // refinement plan list
        for (int t = 0; t < totalThreads; t++)
            refinements.addAll(runnables.get(t).getRefinements());

        // Return refinement plans
        return refinements;
    }

    @Override
    public void setAntecessors(POPIncrementalPlan nextPlan) {
        super.setAntecessors(nextPlan);

        for (POPThread t : this.runnables) {
            t.setAntecessors(antecessors);
        }
    }

    @Override
    public IPlan sendProposalsMonoagent(ArrayList<IPlan> proposals,
                                        IPlan basePlan) {
        IPlan solution = null;
        ArrayList<EvaluationThread> evThreads = new ArrayList<EvaluationThread>();
        int i = 0;
        while (i < proposals.size()) {
            IPlan plan = proposals.get(i);
            if (plan.isSolution()
                    || memoization.search((POPIncrementalPlan) plan) == null) {
                plan.setName(i, basePlan);
                evaluatePlan(plan, evThreads);
                if (!plan.isSolution())
                    memoization.add((POPIncrementalPlan) plan);
                else
                    solution = plan;
                i++;
            } else
                proposals.remove(i);
        }
        for (EvaluationThread ev : evThreads)
            try {
                ev.join();
            } catch (InterruptedException e) {
            }
        for (IPlan plan : proposals) {
            searchTree.addPlan(plan);
            if (agentListener != null)
                agentListener.newPlan(plan, configuration);
        }
        return solution;
    }

    public void evaluatePlan(IPlan plan, ArrayList<EvaluationThread> evThreads) {
        if (plan.isSolution())
            plan.setH(0, 0);
        else if (multithreadHeuristic) {
            EvaluationThread t = new EvaluationThread(plan, evThreads.size());
            t.start();
            evThreads.add(t);
        } else {
            heuristic.evaluatePlan(plan, 0);
        }
    }
}
