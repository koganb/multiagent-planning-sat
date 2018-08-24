package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_communication.Message;
import org.agreement_technologies.common.map_communication.MessageFilter;
import org.agreement_technologies.common.map_communication.PlanningAgentListener;
import org.agreement_technologies.common.map_heuristic.Heuristic;
import org.agreement_technologies.common.map_negotiation.NegotiationFactory;
import org.agreement_technologies.common.map_negotiation.PlanSelection;
import org.agreement_technologies.common.map_planner.*;
import org.agreement_technologies.service.tools.CustomArrayList;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Abstract class: includes the basic planning methods used in POP and
 * POPMultiThread
 *
 * @author Alex
 */
public abstract class Planner implements ExtendedPlanner {
    static final int EQUAL = 1;
    static final int DISTINCT = 2;
    static final int NO_TIMEOUT = -1;
    static final int IDA_SEARCH = 1;
    static final int A_SEARCH = 2;

    protected POPIncrementalPlan basePlan;

    protected PlannerFactoryImp configuration;
    protected Heuristic heuristic;
    protected AgentCommunication comm;
    protected StateMemoization memoization;

    protected POPInternalSearchTree internalSearchTree;
    protected POPSearchMethod searchTree;
    protected SolutionChecker solutionChecker;
    protected PlanSelection planSelection;
    protected ArrayList<POPInternalPlan> successors;
    protected String myAgent;

    protected POPInternalPlan currentInternal;
    protected POPIncrementalPlan initialIncrementalPlan;
    protected POPInternalPlan initialInternalPlan;

    protected int expandedNodes;
    protected PlanningAgentListener agentListener;
    protected OrderingManager matrix;
    protected Hashtable<Integer, Integer> lastValues;
    protected Hashtable<String, String> hashPlanEffects;
    protected Hashtable<String, Boolean> hashEffects;
    // "Static" info
    protected Step initialStep;
    protected Step finalStep;
    protected POPIncrementalPlan[] antecessors;
    protected ArrayList<OpenCondition> openConditions;

    protected CustomArrayList<CausalLink> totalCausalLinks;
    protected int numCausalLinks;
    protected boolean modifiedCausalLinks;

    protected CustomArrayList<Ordering> totalOrderings;
    protected int numOrderings;
    protected boolean modifiedOrderings;
    protected int planningStep;

    protected long planTime = 0, evaluationTime = 0, communicationTime = 0;

    protected ExtendedPlanner parent;
    protected ArrayList<ArrayList<ProposalToSend>> proposalsToSend;
    protected MessageFilterProposals proposalsFilter;
    protected MessageFilterAdjustment adjustmentFilter;
    protected boolean isAnytime;
    private int discarded;
    private Hashtable<Integer, Boolean> hazardousVars;
    private HeuristicAdjustment hAdjustment;
    private NewBasePlanMessage newBasePlanMsg;
    private ArrayList<InternalProposal> allProposals;

    public Planner(AgentCommunication comm) {
        this.comm = comm;
        proposalsToSend = new ArrayList<ArrayList<ProposalToSend>>(comm
                .getOtherAgents().size());
        for (int i = 0; i < comm.getOtherAgents().size(); i++)
            proposalsToSend.add(new ArrayList<ProposalToSend>());
        proposalsFilter = new MessageFilterProposals();
        adjustmentFilter = new MessageFilterAdjustment();
        newBasePlanMsg = new NewBasePlanMessage();
        allProposals = new ArrayList<InternalProposal>();
    }

    @Override
    public POPIncrementalPlan[] getAntecessors() {
        return antecessors;
    }

    public void setAntecessors(POPIncrementalPlan nextPlan) {
        int offset = 1;

        if (nextPlan.isRoot())
            antecessors = new POPIncrementalPlan[1];
        else {
            if (nextPlan.isSolution()) {
                offset--;
                if (nextPlan.getStep().getIndex() == 1)
                    antecessors = new POPIncrementalPlan[nextPlan.getFather()
                            .getStep().getIndex() + 1];
                else
                    antecessors = new POPIncrementalPlan[nextPlan.getStep()
                            .getIndex() + 1];
            } else
                antecessors = new POPIncrementalPlan[nextPlan.getStep()
                        .getIndex()];
        }

        POPIncrementalPlan aux = nextPlan;
        int pos = antecessors.length - 1;
        while (!aux.isRoot()) {
            if (aux.getStep().getIndex() == 1) {
                antecessors[pos] = aux;
                offset++;
                aux = aux.getFather();
            } else {
                antecessors[aux.getStep().getIndex() - offset] = aux;
                pos = aux.getStep().getIndex() - 1;
                aux = aux.getFather();
            }
        }
        antecessors[0] = aux;
    }

    @Override
    public CustomArrayList<CausalLink> getTotalCausalLinks() {
        return totalCausalLinks;
    }

    @Override
    public CustomArrayList<Ordering> getTotalOrderings() {
        return totalOrderings;
    }

    @Override
    public Step getInitialStep() {
        return initialStep;
    }

    public void setInitialStep(Step s) {
        initialStep = s;
    }

    @Override
    public Step getFinalStep() {
        return finalStep;
    }

    public void setFinalStep(Step s) {
        finalStep = s;
    }

    @Override
    public int getNumCausalLinks() {
        return numCausalLinks;
    }

    @Override
    public void setNumCausalLinks(int n) {
        numCausalLinks = n;
    }

    @Override
    public boolean getModifiedCausalLinks() {
        return modifiedCausalLinks;
    }

    @Override
    public void setModifiedCausalLinks(boolean m) {
        modifiedCausalLinks = m;
    }

    @Override
    public boolean getModifiedOrderings() {
        return modifiedOrderings;
    }

    @Override
    public void setModifiedOrderings(boolean m) {
        modifiedOrderings = m;
    }

    @Override
    // A POPInternalPlan adds a causal link to the list
    public void addCausalLink(CausalLink cl) {
        getTotalCausalLinks().add(cl);
        if (!getModifiedCausalLinks())
            setModifiedCausalLinks(true);
    }

    @Override
    // A POPInternalPlan adds a causal link to the list
    public void addOrdering(Ordering cl) {
        totalOrderings.add(cl);
        if (!modifiedOrderings)
            modifiedOrderings = true;
    }

    @Override
    public int getIterations() {
        return this.planningStep;
    }

    @Override
    public Plan computePlan(long start, long timeoutSeconds) {
        long t1;
        planningStep = 1;
        Plan solution = null;
        ArrayList<IPlan> proposals;
        int solutions = 0;
        MetricChecker metricChecker = new MetricChecker(comm);

        if (this.planningStep == 1)
            discarded = 0;

        long endTime = System.currentTimeMillis() + timeoutSeconds * 1000;
        openConditions = new ArrayList<OpenCondition>();

        for (OpenCondition oc : this.initialInternalPlan.getPreconditions())
            openConditions.add(oc);
        basePlan = (POPIncrementalPlan) searchTree.checkNextPlan();

        if (solutionChecker.isSolution(basePlan, configuration)) {
            if (agentListener != null)
                this.agentListener.trace(1, "Solution found");
            return basePlan;
        }
        if (agentListener != null)
            agentListener.newPlan(searchTree.checkNextPlan(), configuration);
        boolean adjustHLandStage = heuristic.requiresHLandStage();

        while ((solution == null || isAnytime)
                && (timeoutSeconds == -1 || System.currentTimeMillis() < endTime)) {
            if (agentListener != null)
                this.agentListener.trace(0, "Planning step: " + planningStep);

            /*********************** Plan selection stage ***********************/
            t1 = System.currentTimeMillis();
            basePlan = selectNextPlan(adjustHLandStage); //(POPIncrementalPlan) planSelection.selectNextPlan(newBasePlanMsg);
            this.communicationTime = this.communicationTime
                    + (System.currentTimeMillis() - t1);
            /******************************************************************/

            if (basePlan == null)
                break; // No solution
            if (agentListener != null)
                this.agentListener.trace(1,
                        "Plan " + basePlan.getName() + " (h=" + basePlan.getH()
                                + ", hL=" + basePlan.getHLan() + ") selected");
            else if (comm.batonAgent())
                System.out.println("Hdtg = " + basePlan.getH() + ", Hlan = " + basePlan.getHLan());

            this.setAntecessors(basePlan);
            initialInternalPlan.setNumSteps(antecessors.length);
            basePlan.calculateCausalLinks(antecessors);
            basePlan.calculateOrderings(antecessors);

            t1 = System.currentTimeMillis();
            proposals = computeSuccessors(initialInternalPlan);

            this.planTime = this.planTime + (System.currentTimeMillis() - t1);

            solution = sendProposals(proposals, basePlan, adjustHLandStage);
            planningStep++;

            // Check if the solution plan found meets the preference thresholds
            if (solution != null)
                if (!this.solutionChecker
                        .isSolution((POPIncrementalPlan) solution, configuration))
                    solution = null;
            if (agentListener != null && solution != null) {
                if (metricChecker.isBestSolution(solution, configuration
                        .getNegotiationFactory().getNegotiationType())) {
                    // Print the plan only if it improves the average metric
                    printStatistics(solution, start, metricChecker);
                }
                solutions++;
            }
        }
        if (agentListener != null && isAnytime)
            agentListener.trace(0, "Solution plans found: " + solutions);

        //if (comm.batonAgent())
        //	memoization.histogram();

        if (this.comm.getAgentIndex(myAgent) == 0) {
            System.out.println("\nCoDMAP Distributed formatData");
            System.out.println("-------------------------");
            for (int i = 0; i < comm.getAgentList().size(); i++) {
                System.out.println("- Agent " + comm.getAgentList().get(i));
                solution.printPlan(Plan.CoDMAP_DISTRIBUTED, comm.getAgentList().get(i), comm.getAgentList());
            }
            System.out.println("\nCoDMAP Centralized formatData");
            System.out.println("-------------------------");
            solution.printPlan(Plan.CoDMAP_CENTRALIZED, myAgent, comm.getAgentList());
            System.out.println("\nRegular formatData");
            System.out.println("--------------");
            solution.printPlan(Plan.REGULAR, myAgent, comm.getAgentList());
            System.out.println("\n");
        }

        return solution;
    }

    private POPIncrementalPlan selectNextPlan(boolean adjustHLandStage) {
        if (comm.numAgents() == 1)
            return (POPIncrementalPlan) searchTree.getNextPlan();
        POPIncrementalPlan plan;
        if (comm.batonAgent()) {                            // Baton agent
            if (!adjustHLandStage) {
                for (InternalProposal p : allProposals)
                    if (!p.plan.isSolution())
                        searchTree.addPlan(p.plan);
            }
            plan = (POPIncrementalPlan) searchTree.getNextPlan();
            if (plan != null)
                newBasePlanMsg.setName(plan.getName());
            else
                newBasePlanMsg.setName(AgentCommunication.NO_SOLUTION_MESSAGE);
            //System.out.println("Send base plan: " + newBasePlanMsg);
            comm.sendMessage(newBasePlanMsg, true);
        } else {                                            // Non-baton agent
            newBasePlanMsg = (NewBasePlanMessage) comm.receiveMessage(comm.getBatonAgent(), true);
            //System.out.println(comm.getThisAgentName() + " receives base plan: " + newBasePlanMsg);
            storeAndAdjustProposals();
            String planName = newBasePlanMsg.getPlanName();
            if (planName.equals(AgentCommunication.NO_SOLUTION_MESSAGE))
                plan = null;
            else
                plan = (POPIncrementalPlan) searchTree.removePlan(planName);
        }
        newBasePlanMsg.Clear();
        allProposals.clear();
        return plan;
    }

    private void storeAndAdjustProposals() {
        for (NewBasePlanMessage.HeuristicChange change : newBasePlanMsg.getChanges()) {
            String planName = change.getName();
            for (InternalProposal p : allProposals)
                if (p.plan.getName().equals(planName)) {
                    //System.out.println("Adjusted heuristic of " + planName + " with " + change.getIncH());
                    p.plan.setH(p.plan.getH(), p.plan.getHLan() - change.getIncH());
                    break;
                }
        }
        for (InternalProposal p : allProposals)
            if (!p.plan.isSolution())
                searchTree.addPlan(p.plan);
    }

    private void printStatistics(Plan solution, long startTime, MetricChecker mc) {
        if (this.isAnytime) {
            agentListener.trace(0, "");
            agentListener.trace(0, "Solution found: " + solution.getName());
        } else
            agentListener.trace(1, "Solution found: " + solution.getName());
        agentListener
                .trace(0, String.format("Planning (expansion) time: %.3f sec.",
                        this.planTime / 1000.0));
        agentListener.trace(0, String.format("Evaluation time: %.3f sec.",
                this.evaluationTime / 1000.0));
        agentListener.trace(0, String.format("Communication time: %.3f sec.",
                this.communicationTime / 1000.0));
        agentListener.trace(0, String.format("Average branching factor: %.3f",
                (double) (searchTree.size() + planningStep)
                        / (double) planningStep));
        if (this.configuration.getNegotiationFactory().getNegotiationType() != NegotiationFactory.COOPERATIVE) {
            agentListener.trace(0, String.format("Metric value: %.1f",
                    evaluateMetric((POPIncrementalPlan) solution)));
            agentListener.trace(
                    0,
                    String.format("Average metric value: %.1f",
                            mc.getBestMetric()));
        }
        agentListener.trace(0,
                String.format("Discarded plans: %d", this.discarded));
        if (this.isAnytime) {
            agentListener.trace(0,
                    String.format("Plan length: %d", solution.countSteps()));
            agentListener.trace(0, String.format("Makespan: %.1f",
                    ((POPIncrementalPlan) solution).computeMakespan()));
            long endTime = System.currentTimeMillis() - startTime;
            agentListener.trace(0,
                    String.format("Plan found in %.3f sec.", endTime / 1000.0));
            agentListener.trace(0, "");
        }

        calculateTimeSteps(solution);
    }

    public double evaluateMetric(POPIncrementalPlan plan) {
        double makespan;
        if (configuration.getGroundedTask().metricRequiresMakespan())
            makespan = plan.computeMakespan();
        else
            makespan = 0;
        if (plan.isSolution())
            return configuration.getGroundedTask().evaluateMetric(
                    plan.computeState(plan.getFather().linearization(), configuration),
                    makespan);
        return configuration.getGroundedTask().evaluateMetric(
                plan.computeState(plan.linearization(), configuration), makespan);
    }

    // Abstract methods
    public abstract IPlan sendProposalsMonoagent(ArrayList<IPlan> proposals,
                                                 IPlan basePlan);

    public abstract ArrayList<IPlan> POPForwardLoop();

    // public abstract void setAntecessors(POPIncrementalPlan nextPlan);

    public abstract void evaluatePlan(IPlan plan,
                                      ArrayList<Planner.EvaluationThread> evThreads);

    protected ArrayList<IPlan> computeSuccessors(POPInternalPlan basePlan) {
        this.currentInternal = basePlan;

        this.internalSearchTree = new POPInternalSearchTree(
                this.initialInternalPlan);

        ArrayList<IPlan> solutions = addFinalStep();
        ArrayList<IPlan> plans = null;
        // Add successors if:
        // 1 - There are not solutions. In case there are solutions, add
        // successors if:
        // 2 - FMAP is in anytime mode or it is using non-cooperative
        // negotiation strategy
        if (solutions == null
                || (isAnytime || this.configuration.getNegotiationFactory()
                .getNegotiationType() != NegotiationFactory.COOPERATIVE))
            plans = POPForwardLoop();
        if (solutions != null) {
            if (plans != null)
                for (IPlan p : solutions)
                    plans.add(p);
            else
                plans = solutions;
        }
        for (IPlan p : plans) p.setG(p.numSteps());
        return plans;
    }

    protected IPlan sendProposals(ArrayList<IPlan> prop, IPlan basePlan, boolean adjustHLandStage) {
        if (comm.numAgents() == 1)
            return sendProposalsMonoagent(prop, basePlan);
        // Check if there are repeated proposals before evaluating them
        ArrayList<InternalProposal> ownProposals = new ArrayList<InternalProposal>();
        for (IPlan p : prop) {
            if (memoization.search((POPIncrementalPlan) p) == null) {
                if (!p.isSolution())
                    memoization.add((POPIncrementalPlan) p);
                ownProposals.add(new InternalProposal(p));
            } else
                discarded++;
        }
        long t2 = System.currentTimeMillis();
        evaluateProposals(ownProposals, basePlan, adjustHLandStage);
        this.evaluationTime = this.evaluationTime
                + (System.currentTimeMillis() - t2);

        long t3 = System.currentTimeMillis();
        IPlan solution = communicateProposals(ownProposals, adjustHLandStage);
        this.communicationTime = this.communicationTime
                + (System.currentTimeMillis() - t3);

        //evaluatePrivateGoals(allProposals, basePlan);
        return solution;
    }

    protected void evaluatePrivateGoals(ArrayList<IPlan> allProposals,
                                        IPlan basePlan) {
        heuristic.startEvaluation(basePlan);
        for (IPlan p : allProposals)
            heuristic.evaluatePlanPrivacy(p, 0);
        heuristic.waitEndEvaluation();
    }

    protected void evaluateProposals(ArrayList<InternalProposal> proposals, IPlan basePlan,
                                     boolean adjustHLandStage) {
        heuristic.startEvaluation(basePlan);
        if (adjustHLandStage) {
            ArrayList<Integer> achievedLandmarks = new ArrayList<Integer>();
            for (InternalProposal p : proposals) {
                heuristic.evaluatePlan(p.plan, 0, achievedLandmarks);
                p.setAchievedLandmarks(achievedLandmarks, heuristic.numGlobalLandmarks());
                achievedLandmarks.clear();
            }
        } else {
            for (InternalProposal p : proposals)
                heuristic.evaluatePlan(p.plan, 0);
        }
        heuristic.waitEndEvaluation();
    }

    protected IPlan communicateProposals(ArrayList<InternalProposal> ownProposals,
                                         boolean adjustHLandStage) {

        prepareProposalsToSend(ownProposals);
        // Communicate and receive proposals
        IPlan solution;
        if (!adjustHLandStage) {
            int propCount = 0;
            for (String ag : comm.getOtherAgents())
                comm.sendMessage(ag, proposalsToSend.get(propCount++), false);
            solution = receiveProposals(ownProposals);
        } else {
            int propCount = 0, batonAgentIndex = 0;
            for (String ag : comm.getOtherAgents()) {    // Do not send to baton agent
                if (!ag.equals(comm.getBatonAgent()))
                    comm.sendMessage(ag, proposalsToSend.get(propCount), false);
                else batonAgentIndex = propCount;
                propCount++;
            }
            solution = adjustAndReceiveProposals(ownProposals, batonAgentIndex);
        }
        return solution;
    }

    private IPlan adjustAndReceiveProposals(ArrayList<InternalProposal> ownProposals,
                                            int batonAgentIndex) {
        IPlan solution;
        if (!comm.batonAgent()) {
            solution = receiveProposals(ownProposals);
            adjustHeuristic(allProposals);
            hAdjustment.addOwnProposals(proposalsToSend.get(batonAgentIndex));
            comm.sendMessage(comm.getBatonAgent(), hAdjustment, false);
        } else {
            solution = receiveHeuristicAdjustments(ownProposals);
        }
        return solution;
    }

    private IPlan receiveHeuristicAdjustments(ArrayList<InternalProposal> ownProposals) {
        IPlan solution = null;
        hAdjustment = new HeuristicAdjustment(1 + ownProposals.size() * comm.numAgents());
        int index = 0;
        for (String ag : comm.getAgentList()) {
            if (ag.equals(comm.getThisAgentName())) {    // This agent
                for (InternalProposal p : ownProposals) {
                    p.plan.setName(index++, basePlan);

                    //if (p.plan.getName().endsWith("0-1-0-0-30-31-28-81-59-56-60-62-63-59-68-63-70-64-72-70-74-67-75-74-77-64-34-30-36-30-37-30-45-30-48-29")) {
                    //	System.out.println("PLAN " + p.plan.getName());
                    //	System.out.println("* LANDMARKS : " + p.achievedLandmarks.toString());
                    //}

                    allProposals.add(p);
                    if (p.plan.isSolution())
                        solution = p.plan;
                    else
                        memoization.add((POPIncrementalPlan) p.plan);
                    if (agentListener != null) {
                        agentListener.trace(2, "Sending plan " + p.plan.getName() +
                                "[" + p.plan.getH() + "]");
                        agentListener.newPlan(p.plan, configuration);
                    }
                }
            } else {                                    // Other agent
                adjustmentFilter.fromAgent = ag;
                HeuristicAdjustment h = (HeuristicAdjustment) comm
                        .receiveMessage(adjustmentFilter, false);
                for (ProposalToSend prop : h.getProposals()) {
                    POPIncrementalPlan p = new POPIncrementalPlan(prop, basePlan,
                            configuration, this);
                    p.setName(index++, basePlan);
                    String planName = p.getName();

                    //if (planName.endsWith("0-1-0-0-30-31-28-81-59-56-60-62-63-59-68-63-70-64-72-70-74-67-75-74-77-64-34-30-36-30-37-30-45-30-48-29")) {
                    //	System.out.println("PLAN " + planName);
                    //	System.out.println("* LANDMARKS : " + prop.getAchievedLandmarks().toString());
                    //}

                    InternalProposal proposal = new InternalProposal(p, prop.getAchievedLandmarks());
                    ArrayList<Integer> newLand = heuristic.checkNewLandmarks(proposal.plan, proposal.achievedLandmarks);
                    hAdjustment.merge(planName, newLand);
                    allProposals.add(proposal);
                    if (p.isSolution())
                        solution = p;
                    else
                        memoization.add(p);
                    if (agentListener != null) {
                        agentListener.trace(2, "Received plan " + planName + " from " + ag + "[" + p.getH() + "]");
                        agentListener.newPlan(p, configuration);
                    }
                    //System.out.println("PLAN " + proposal.plan.getName());
                    //System.out.println("* LANDMARKS: " + h.newLandmarksList(proposal.plan.getName()));
                }
                ArrayList<String> planNames = h.proposalsWithAdjustments();
                for (String planName : planNames)
                    hAdjustment.merge(planName, h.getNewLandmarks(planName));
            }
        }
        updateHLandValues();
        return solution;
    }

    private void updateHLandValues() {
        for (InternalProposal p : allProposals) {
            IPlan plan = p.plan;
            String planName = plan.getName();
            int incH = hAdjustment.getNumNewLandmarks(planName);
            if (incH > 0) {
                newBasePlanMsg.addAdjustment(planName, incH);
                plan.setH(plan.getH(), plan.getHLan() - incH);
            }
            if (!plan.isSolution())
                searchTree.addPlan(plan);
        }
    }

    private void adjustHeuristic(ArrayList<InternalProposal> proposals) {
        hAdjustment = new HeuristicAdjustment(proposals.size());
        String fromAgent;

        for (InternalProposal prop : proposals) {
            IPlan plan = prop.plan;
            fromAgent = plan.lastAddedStep().getAgent();
            if (fromAgent != null && !comm.getThisAgentName().equals(fromAgent)) {
                //int[] totalOrder = plan.linearization();
                //if (totalOrder[totalOrder.length-1] != plan.lastAddedStep().getIndex()) {
                // Proposal from other agent and the new step is not at the end of the plan
                ArrayList<Integer> newLandmarks = heuristic.checkNewLandmarks(plan,
                        prop.achievedLandmarks);
                hAdjustment.add(plan.getName(), newLandmarks);
                //System.out.println(plan.getName());
                //}
            }
        }
    }

    private IPlan receiveProposals(ArrayList<InternalProposal> ownProposals) {
        IPlan solution = null;
        int index = 0;
        for (String ag : comm.getAgentList()) {
            if (ag.equals(comm.getThisAgentName())) {    // This agent
                for (InternalProposal p : ownProposals) {
                    p.plan.setName(index++, basePlan);
                    allProposals.add(p);
                    if (p.plan.isSolution())
                        solution = p.plan;
                    else
                        memoization.add((POPIncrementalPlan) p.plan);
                    if (agentListener != null) {
                        agentListener.trace(2, "Sending plan " + p.plan.getName() +
                                "[" + p.plan.getH() + "]");
                        agentListener.newPlan(p.plan, configuration);
                    }
                }
            } else {                                    // Other agent
                proposalsFilter.fromAgent = ag;
                @SuppressWarnings("unchecked")
                ArrayList<ProposalToSend> pp = (ArrayList<ProposalToSend>) comm
                        .receiveMessage(proposalsFilter, false);
                for (ProposalToSend prop : pp) {
                    POPIncrementalPlan p = new POPIncrementalPlan(prop, basePlan, configuration, this);
                    p.setName(index++, basePlan);
                    allProposals.add(new InternalProposal(p, prop.getAchievedLandmarks()));
                    if (p.isSolution())
                        solution = p;
                    else
                        memoization.add(p);
                    if (agentListener != null) {
                        agentListener.trace(2, "Received plan " + p.getName() +
                                " from " + ag + "[" + p.getH() + "]");
                        agentListener.newPlan(p, configuration);
                    }
                }
            }
        }
        return solution;
    }

    private void prepareProposalsToSend(ArrayList<InternalProposal> proposals) {
        for (int i = 0; i < comm.getOtherAgents().size(); i++)
            proposalsToSend.get(i).clear();
        int propCount;
        for (InternalProposal prop : proposals) {
            propCount = 0;
            for (String ag : comm.getOtherAgents()) {
                ProposalToSend pp = new ProposalToSend(prop, ag, false);
                proposalsToSend.get(propCount).add(pp);
                propCount++;
            }
        }
    }

    protected void calculateTimeSteps(Plan solution) {
        ArrayList<Ordering> ord = solution.getOrderingsArray();
        ArrayList<CausalLink> cl = solution.getCausalLinksArray();
        Hashtable<Integer, Integer> stepsFound = new Hashtable<Integer, Integer>();
        boolean found, moreFound = true;
        int timeStep = 0;

        while (moreFound) {
            moreFound = false;

            for (int i = 2; i < solution.getStepsArray().size(); i++) {
                if (solution.getStepsArray().get(i).getTimeStep() == -1) {
                    found = true;
                    for (Ordering o : ord) {
                        if (o.getIndex2() == i
                                && (stepsFound.get(o.getIndex1()) == null || stepsFound
                                .get(o.getIndex1()) == timeStep)) {
                            found = false;
                            break;
                        }
                    }
                    if (found) {
                        for (CausalLink c : cl) {
                            if (c.getIndex1() != 0 && c.getIndex2() != 1) {
                                if (c.getIndex2() == i
                                        && (stepsFound.get(c.getIndex1()) == null || stepsFound
                                        .get(c.getIndex1()) == timeStep)) {
                                    found = false;
                                    break;
                                }
                            }
                        }
                    }
                    if (found) {
                        moreFound = true;
                        stepsFound.put(i, timeStep);
                        solution.getStepsArray().get(i).setTimeStep(timeStep);
                    }
                }
            }

            timeStep++;
        }
    }

    /**
     * Estimates the actions of the domain that are potentially supportable in
     * the current base plan
     *
     * @return List of potentially supportable actions
     */
    protected ArrayList<POPAction> calculateApplicableActions_old() {
        ArrayList<POPAction> applicableActions = new ArrayList<POPAction>();
        // Analyze all the actions in the agent's domain
        for (POPAction pa : this.configuration.getActions()) {
            if (this.isApplicable(currentInternal, pa)) {
                if (isHazardous(pa))
                    applicableActions.add(pa);
                else if (this.memoization.search(basePlan, pa) == null)
                    applicableActions.add(pa);
            }
        }
        this.agentListener.trace(1, "Agent " + comm.getThisAgentName()
                + " found " + applicableActions.size() + " applicable actions");
        return applicableActions;
    }

    /**
     * Estimates the actions of the domain that are potentially supportable in
     * the current base plan
     *
     * @return List of potentially supportable actions
     */
    protected ArrayList<POPAction> calculateApplicableActions() {
        ArrayList<POPAction> applicableActions = new ArrayList<POPAction>();
        /*long frStateTime = 0, actionTime = 0, totalActionTime = 0;
                if(this.planningStep == 1025 && comm.getThisAgentName().equals("r1"))
                    frStateTime = System.nanoTime();*/
        this.calculateEffectsLastValues(currentInternal);
                /*if(this.planningStep == 1025 && comm.getThisAgentName().equals("r1")) {
                    System.out.println("************* Starting iter. 1025 (agent " + comm.getThisAgentName() + ") *************");
                    frStateTime = System.nanoTime() - frStateTime;
                    System.out.println("Time used to calculate the frontier state (agent " + comm.getThisAgentName() + "): " + frStateTime + " ns.");
                }
                if(this.planningStep == 1025 && comm.getThisAgentName().equals("r1"))
                    totalActionTime = System.nanoTime();*/
        // Analyze all the actions in the agent's domain
        for (POPAction pa : this.configuration.getActions()) {
            if (this.isActionSupportable(currentInternal, pa)) {
                                /*if(this.planningStep == 1025 && comm.getThisAgentName().equals("r1"))
                                    actionTime = System.nanoTime();*/
				/*
				 * if(isHazardous(pa)) applicableActions.add(pa); else
				 */
                if (this.memoization.search(basePlan, pa) == null)
                    applicableActions.add(pa);

                                /*if(this.planningStep == 1025 && comm.getThisAgentName().equals("r1")) {
                                    actionTime = System.nanoTime() - actionTime;
                                    System.out.println("Time used to verify action " + pa.toString() + " (agent " + comm.getThisAgentName() + "): " + actionTime + " ns.");
                                }*/
            }

        }
                /*if(this.planningStep == 1025 && comm.getThisAgentName().equals("r1")) {
                            totalActionTime = System.nanoTime() - totalActionTime;
                            System.out.println("Total time used to estimate applicable actions (agent " + comm.getThisAgentName() + "): " + totalActionTime + " ns.");
                            System.out.println("************* Ending iter. 1025 (agent " + comm.getThisAgentName() + ") *************");
                }*/
        if (agentListener != null)
            agentListener.trace(1, "Agent " + comm.getThisAgentName() + " found " + applicableActions.size() + " applicable actions");
        return applicableActions;
    }

    public Boolean isActionSupportable(POPInternalPlan p, POPAction a) {
        POPPrecEff prec;
        for (int i = 0; i < a.getPrecs().size(); i++) {
            prec = a.getPrecs().get(i);
            if (a.hasEffectInVariable(i)) {
                Integer value = lastValues.get(prec.getVarCode());
                if (value != null && value != prec.getValueCode())
                    return false;
            } else {
                if (this.hashEffects.get(prec.toKey()) == null)
                    return false;
            }
        }
        return true;
    }

    /**
     * Fills the hashEffects structure
     *
     * @param p Base plan
     */
    public void calculateEffectsLastValues(POPInternalPlan p) {
        POPStep ps;
        int[] linearization = this.basePlan.linearization();
        int index;
        this.hashEffects = new Hashtable<String, Boolean>();
        this.lastValues = new Hashtable<Integer, Integer>();
        // Check the steps of the linearized base plan in reverse order
        for (int i = linearization.length - 1; i >= 0; i--) {
            index = linearization[i];
            if (index != 1) {
                ps = (POPStep) p.getStep(index);

                for (POPPrecEff eff : ps.getEffects()) {
                    hashEffects.put(eff.toKey(), true);
                    if (lastValues.get(eff.getVarCode()) == null)
                        lastValues.put(eff.getVarCode(), eff.getValueCode());
                }
            }
        }
    }

    public boolean isHazardous(POPAction a) {
        for (POPPrecEff eff : a.getEffects())
            if (this.hazardousVars.get(eff.getVarCode()) != null)
                return true;
        return false;
    }

    public Boolean isApplicable(POPInternalPlan p, POPAction a) {
        POPStep ps;
        int i;
        Boolean found;
        if (this.hashEffects == null) {
            // hashEffects stores the effects of the steps of the base plan
            this.hashEffects = new Hashtable<String, Boolean>();
            this.hazardousVars = new Hashtable<Integer, Boolean>();
            // Abakyze the steps of the base plan
            for (i = 0; i < p.numSteps(); i++) {// Step s: p.getStepsArray()) {
                ps = (POPStep) p.getStep(i);
                found = false;
                if (ps.getIndex() != 1) {
                    for (POPPrecEff eff : ps.getEffects()) {
                        // Fill hashEffects structure
                        this.hashEffects.put(eff.toKey(), Boolean.TRUE);

                        if (ps.getIndex() != 0) {
                            found = false;
                            for (POPPrecEff pre : ps.getPreconditions()) {
                                if (pre.getVarCode() == eff.getVarCode()) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found)
                                this.hazardousVars.put(eff.getVarCode(), Boolean.TRUE);
                        }
                    }
                }
            }
        }

        for (POPPrecEff prec : a.getPrecs())
            if (this.hashEffects.get(prec.toKey()) == null)
                return false;

        return true;
    }

    /**
     * Restore the causal links array when a new POPInternalPlan is expanded
     */
    public void restoreCausalLinks() {
        getTotalCausalLinks().trimToSize(getNumCausalLinks());
        setModifiedCausalLinks(false);
    }

    /**
     * Restore the causal links array when a new POPInternalPlan is expanded
     */
    public void restoreOrderings() {
        totalOrderings.trimToSize(numOrderings);
        modifiedOrderings = false;
    }

    /**
     * Secondary loop, integrates a new action in the plan and solves all its
     * flaws
     *
     * @param step
     * @return
     */
    protected ArrayList<IPlan> solveAction(POPStep step) {
        ArrayList<IPlan> refinements = new ArrayList<IPlan>();
        successors.clear();
        POPIncrementalPlan refinement;

        while (!this.internalSearchTree.isEmpty()) {
            this.expandedNodes++;
            // Clean causal link and ordering arrays from previous usage
            restoreCausalLinks();
            restoreOrderings();
            currentInternal = (POPInternalPlan) this.internalSearchTree
                    .getNextPlan();
            // Store causal links and orderings of the next plan in the array
            currentInternal.addCausalLinks();
            currentInternal.addOrderings();

            matrix.rebuild(currentInternal);

            // If the plan supports completely the action, store it as a
            // solution
            if (this.solutionChecker.keepsConstraints(currentInternal, step)) {
                refinement = new POPIncrementalPlan(
                        (POPInternalPlan) currentInternal,
                        antecessors[antecessors.length - 1], parent);
                // if(!this.searchTree.isRepeated(refinement))
                refinements.add(refinement);
            } else {
                // If the plan has threats, solve the first of them
                if (currentInternal.getThreats().size() > 0) {
                    solveThreat(currentInternal, step.getIndex() == 1);
                }
                // If the plan is threat-free, the next open condition of the
                // new action is solved
                else {
                    if (currentInternal.getPreconditions().size() > 0) {
                        this.solveOpenCondition(currentInternal, step);
                    }
                }
            }
        }

        return refinements;
    }

    /**
     * Selects and solves an open condition of the plan
     *
     * @param p      Base plan
     * @param father Incremental definition of the base plan
     */
    public void solveOpenCondition(POPInternalPlan father, POPStep newStep) {
        POPInternalPlan successor;
        POPAction act;
        POPStep step;
        POPOpenCondition prec;
        ArrayList<POPInternalPlan> suc = new ArrayList<POPInternalPlan>();
        int i;
        boolean isFinalStep = newStep.getIndex() == 1;

        // If the new step is not yet stored into the plan, we add it by solving
        // one of its preconditions through a causal link
        if (father.numSteps() <= newStep.getIndex()) {
            // Select the first precondition of the new step
            prec = new POPOpenCondition(newStep.getPreconditions()[0], newStep,
                    true);
            // Solve the selected open condition with the existent steps of the
            // plan
            for (i = 0; i < father.numSteps(); i++) {// Step s:
                // father.getStepsArray())
                // {
                step = (POPStep) father.getStep(i);
                act = step.getAction();
                if (step.getIndex() != 1) {
                    for (POPPrecEff eff : act.getEffects()) {
                        if (eff.getVarCode() == prec.getCondition().getVarCode()) {
                            if ((prec.getCondition().getType() == EQUAL
                                    && eff.getValueCode() == prec.getCondition().getValueCode()
                                    && eff.getType() == EQUAL) ||
                                    (prec.getCondition().getType() == DISTINCT &&
                                            eff.getValueCode() != prec.getCondition().getValueCode())) {
                                successor = new POPInternalPlan(father, newStep,
                                        new POPCausalLink(step, prec.getPrecEff(), newStep),
                                        new POPOrdering(step.getIndex(), prec.getStep().getIndex()),
                                        father.getPreconditions(), null, prec,
                                        isFinalStep, this);

                                this.detectThreatsNewStep(newStep,
                                        step.getIndex(), father, successor);
                                this.detectThreatsLink(
                                        successor.getCausalLink(), father,
                                        successor);

                                // father.setSuccessors();
                                suc.add(successor);
                            }
                        }
                    }
                }
            }
        } else {
            // Retrieve the next open condition to be solved and erase it from
            // the plan
            prec = (POPOpenCondition) father.getPreconditions().get(
                    father.getPreconditions().size() - 1);
            father.getPreconditions().remove(
                    father.getPreconditions().size() - 1);

            // Search among the plan's steps
            for (i = 0; i < father.numSteps(); i++) {
                step = (POPStep) father.getStep(i);
                act = step.getAction();

                if (step.getIndex() != prec.getStep().getIndex()) { // Si el
                    // paso no
                    // es el
                    // mismo
                    // asociado
                    // a la
                    // precondicion
                    // (un paso
                    // no puede
                    // enlazarse
                    // a sí
                    // mismo)
                    if (step.getIndex() != 1) { // Nos saltamos el paso final,
                        // que no puede resolver ninguna
                        // precondicion
                        for (POPPrecEff eff : act.getEffects()) { // Recorremos
                            // los
                            // efectos
                            // del paso
                            if (eff.getVarCode() == prec.getCondition().getVarCode()) { // Si la variable del
                                // efecto coincide con
                                // la de la
                                // precondicion
                                // Si la precondicion es de tipo = y el valor
                                // de la precondicion coincide con el del
                                // efecto, o bien
                                // si la precondicion es de tipo != y el valor
                                // de la precondicion es distinto al del efecto
                                if ((prec.getCondition().getType() == EQUAL
                                        && eff.getValueCode() == prec.getCondition().getValueCode()
                                        && eff.getType() == EQUAL)
                                        || (prec.getCondition().getType() == DISTINCT &&
                                        eff.getValueCode() != prec.getCondition().getValueCode())) {
                                    if (!matrix.checkOrdering(prec.getStep()
                                            .getIndex(), step.getIndex())) {
                                        POPStep st = (POPStep) father
                                                .getStep(prec.getStep()
                                                        .getIndex());
                                        // Generamos el nuevo plan incremental,
                                        // guardando la lista de precondiciones
                                        // abiertas y el plan padre
                                        successor = new POPInternalPlan(father,
                                                null,
                                                new POPCausalLink(step, prec
                                                        .getPrecEff(), st),
                                                new POPOrdering(
                                                        step.getIndex(), prec
                                                        .getStep()
                                                        .getIndex()),
                                                father.getPreconditions(),
                                                null, prec, isFinalStep, this);

                                        if (!matrix.checkOrdering(step
                                                .getIndex(), prec.getStep()
                                                .getIndex()))
                                            successor.setOrdering(step
                                                    .getIndex(), prec.getStep()
                                                    .getIndex());

                                        this.detectThreatsLink(
                                                successor.getCausalLink(),
                                                father, successor); // Buscamos
                                        // amenazas
                                        // que
                                        // puedan
                                        // provocar
                                        // los pasos
                                        // existentes
                                        // al nuevo
                                        // causal
                                        // link

                                        // father.setSuccessors();
                                        suc.add(successor);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            father.cleanPlan();
        }

        // Store the successors in the search tree
        this.internalSearchTree.addSuccessors(suc);
    }

    /**
     * Solves the last threat of the plan by promoting or demoting the
     * threatening step
     *
     * @param p      Base plan
     * @param father Incremental definition of the base plan
     */
    @Override
    public void solveThreat(POPInternalPlan father, boolean isFinalStep) {
        POPInternalPlan successor1 = null, successor2 = null;

        // Extract the last threat of the list to solve it (LIFO criteria)
        POPThreat threat = father.getThreats().remove(
                father.getThreats().size() - 1);

        // Locate the indexes of the involved steps
        int index1 = threat.getCausalLink().getIndex1(); // Store the index of
        // step Pi
        int index2 = threat.getCausalLink().getIndex2(); // Store the index of
        // step Pj
        int indexThreat = threat.getThreateningStep(); // Store the index of the
        // threatening step P

        // Check if the threat exists already; if not, we erase it, exit the
        // method and restart the search process with the current plan
        // The threat may have been removed by the deletion of a previous threat
        if (matrix.checkOrdering(indexThreat, index1)
                || matrix.checkOrdering(index2, indexThreat)) {
            internalSearchTree.addPlan(father);
        }

        // Promotion process (performed only if Pj is NOT the final step)
        if (index2 != 1) {
            // Try to promote P (add ordering Pj -> P). To do so, check if there
            // is not an ordering (direct or transitive) P -> Pj
            // If the ordering P -> Pj is not found, an ordering Pj -> P is
            // added
            if (!matrix.checkOrdering(indexThreat, index2)
                    && !matrix.checkOrdering(index2, indexThreat)) {
                successor1 = new POPInternalPlan(father, null, null, null,
                        father.getPreconditions(), father.getThreats(), null,
                        isFinalStep, father.getPlanner());
                successor1.setOrdering(index2, indexThreat);
                internalSearchTree.addPlan(successor1);
            }
        }

        // Demotion process (performed only if Pi is NOT the initial step)
        if (index1 != 0) {
            // Try to demote P (add ordering P -> Pi).
            if (!matrix.checkOrdering(index1, indexThreat)
                    && !matrix.checkOrdering(indexThreat, index1)) {
                successor2 = new POPInternalPlan(father, null, null, null,
                        father.getPreconditions(), father.getThreats(), null,
                        isFinalStep, father.getPlanner());
                successor2.setOrdering(indexThreat, index1);
                internalSearchTree.addPlan(successor2);
            }
        }
        // Store the successors in the search tree
        if (successor1 != null || successor2 != null)
            father.cleanPlan();
    }

    /**
     * Detects all the threats caused by the inclusion of a new causal link si
     * -p-> sj, where si may be an existing or a new step
     *
     * @param link      New causal link
     * @param successor Successor plan that stores the new causal link
     */
    public void detectThreatsLink(POPCausalLink link, POPInternalPlan father,
                                  POPInternalPlan successor) {
        int i, j, index1, index2;

        Boolean order;
        POPStep step;
        ArrayList<POPThreat> threats = new ArrayList<POPThreat>();
        int type = link.getCondition().getType();

        // Buscamos los indices de los dos pasos del causal link
        index1 = link.getIndex1();
        index2 = link.getIndex2();

        // Recorremos los pasos del plan (obviamos los pasos inicial y final,
        // dado que no pueden provocar amenazas)
        for (i = 2; i < father.numSteps(); i++) {
            step = (POPStep) father.getStep(i);
            if (step == null) {
                father.setNumSteps(-1);
                father.numSteps();
                step = (POPStep) father.getStep(i);
            }
            // Comprobamos que el paso actual no es uno de los dos que forman el
            // causal link
            if (index1 != i && index2 != i) {
                for (j = 0; j < step.getAction().getEffects().size(); j++) {
                    // Si la condición del causal link es de tipo =
                    if (type == EQUAL) {
                        // Recorremos los efectos buscando uno con la misma
                        // variable y distinto valor
                        if (step.getAction().getEffects().get(j).getVarCode() == link.getCondition().getVarCode()
                                && step.getAction().getEffects().get(j).getValueCode() != link.getCondition().getValueCode()) {
                            // Si P es el paso inicial, hay un orden P > Pi
                            if (i == 0)
                                order = true;
                                // Buscamos un ordering P > Pi
                            else
                                order = matrix.checkOrdering(i, index1);
                            // Si no, buscamos un ordering Pj > P
                            if (!order)
                                order = matrix.checkOrdering(index2, i);
                            // Si no hemos encontrado los orderings, añadimos
                            // la nueva amenaza (dado que P puede colocarse
                            // entre Pi y Pj)
                            if (!order)
                                threats.add(new POPThreat(step, link));
                        }
                    }
                    // Si la condicion del causal link es de tipo !=
                    if (type == DISTINCT) {
                        // Recorremos los efectos buscando uno con la misma
                        // variable y valor
                        if (step.getAction().getEffects().get(j).getVarCode() ==
                                link.getCondition().getVarCode() &&
                                step.getAction().getEffects().get(j).getValueCode() ==
                                        link.getCondition().getValueCode()) {
                            if (i == 0)
                                order = true;
                            else
                                order = matrix.checkOrdering(i, index1);
                            if (!order)
                                order = matrix.checkOrdering(index2, i);
                            if (!order)
                                threats.add(new POPThreat(step, link));
                        }
                    }
                }
            }
        }
        successor.addThreats(threats);
    }

    /**
     * Detects all the threats caused by the inclusion of a new step
     *
     * @param step      New step introduced
     * @param index     Index of the step s2 of the ordering ns -> s2, where ns is the
     *                  new step introduced
     * @param successor Successor plan that stores the new step
     */
    public void detectThreatsNewStep(POPStep step, int index,
                                     POPInternalPlan father, POPInternalPlan successor) {
        int i, j, index1, index2, indexStep;
        Boolean order;
        POPCausalLink link;
        Condition precondition;
        ArrayList<POPThreat> threats = new ArrayList<POPThreat>();

        indexStep = step.getIndex();

        for (i = 0; i < step.getAction().getEffects().size(); i++) { // Recorremos
            // los
            // efectos
            // de la
            // acción
            // asociada
            // al
            // paso
            for (j = father.getTotalCausalLinks().size() - 1; j >= 0; j--) { // Recorremos
                // los
                // enlaces
                // causales
                link = (POPCausalLink) father.getTotalCausalLinks().get(j); // Guardamos
                // link,
                // step1,
                // step2
                // y
                // literal
                // en
                // variables
                // para
                // mayor
                // simplicidad
                precondition = link.getCondition();
                // Obtenemos el índice del paso inicial del causal link
                index1 = link.getIndex1();
                index2 = link.getIndex2();

                // Si el paso nuevo no es ninguno de los dos pasos del enlace
                // causal, comprobamos las amenazas
                if (indexStep != index1 && indexStep != index2) {
                    // Si la condición del causal link es de tipo =
                    if (link.getCondition().getType() == EQUAL) {
                        // Si el efecto del paso y la condición del causal link
                        // coinciden y tienen distinto valor
                        if (step.getAction().getEffects().get(i).getVarCode() ==
                                precondition.getVarCode() &&
                                step.getAction().getEffects().get(i).getValueCode()
                                        != precondition.getValueCode()) {
                            // Buscamos en la fila de la matriz del paso p2 del
                            // ordering pn -> p2
                            // Si intentamos comprobar el ordering pn -> p2,
                            // devolvemos true directamente
                            order = matrix.checkOrdering(index2, index);
                            // Si no hemos encontrado ningún ordering P > Pi o
                            // Pj > P, creamos una nueva amenaza
                            if (!order)
                                threats.add(new POPThreat(step, link));
                        }
                    }
                    // Si la condición del causal link es de tipo !=
                    else if (link.getCondition().getType() == DISTINCT) {
                        // Si el efecto del paso y la condición del causal link
                        // coinciden y tienen el mismo valor
                        if (step.getAction().getEffects().get(i).getVarCode() == precondition.getVarCode()
                                && step.getAction().getEffects().get(i).getValueCode() == precondition.getValueCode()) {
                            if (index1 == index)
                                order = true;
                            else
                                order = matrix.checkOrdering(index, index1);
                            if (!order)
                                threats.add(new POPThreat(step, link));
                        }
                    }
                }
            }
        }
        // Añadimos las amenazas encontradas al plan sucesor
        successor.addThreats(threats);
    }

    public SolutionChecker getSolutionChecker() {
        return this.solutionChecker;
    }

    public POPIncrementalPlan getBasePlan() {
        return basePlan;
    }

    public int getNumOrderings() {
        return numOrderings;
    }

    @Override
    public void setNumOrderings(int n) {
        numOrderings = n;
    }

    private ArrayList<IPlan> addFinalStep() {
        this.hashEffects = null;

        // The first plan to be processed is a copy of the planner's initial
        // plan
        POPInternalPlan auxInternal = currentInternal;
        initialInternalPlan.setNumSteps(-1);

        if (this.basePlan.getH() == 0) {
            // Check if the final step is applicable
            POPStep st = (POPStep) currentInternal.getFinalStep();
            if (this.isApplicable(currentInternal, st.getAction())) {
                ArrayList<IPlan> succ = this.solveAction(st);

                currentInternal = auxInternal;
                restoreCausalLinks();
                restoreOrderings();
                currentInternal.restorePlan(openConditions);

                // Return successors (solution plans), if any
                if (succ.size() > 0) {
                    return succ;
                }
            }
        }
        // Return null if there is no solution
        return null;
    }

    public static class MessageFilterProposals implements MessageFilter {
        protected String fromAgent;

        public boolean validMessage(Message m) {
            return m.sender().equals(fromAgent)
                    && (m.content() instanceof ArrayList<?>);
        }
    }

    public static class MessageFilterAdjustment implements MessageFilter {
        protected String fromAgent;

        public boolean validMessage(Message m) {
            return m.sender().equals(fromAgent)
                    && (m.content() instanceof HeuristicAdjustment);
        }
    }

    public class EvaluationThread extends Thread {
        IPlan plan;
        int threadIndex;

        EvaluationThread(IPlan plan, int threadIndex) {
            this.plan = plan;
            this.threadIndex = threadIndex;
        }

        public void run() {
            heuristic.evaluatePlan(plan, threadIndex);
        }
    }
}