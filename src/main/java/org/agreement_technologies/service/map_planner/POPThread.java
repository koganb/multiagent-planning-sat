package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_planner.CausalLink;
import org.agreement_technologies.common.map_planner.OpenCondition;
import org.agreement_technologies.common.map_planner.Ordering;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.tools.CustomArrayList;

import java.util.ArrayList;

public class POPThread extends Planner implements Runnable {
    private ArrayList<POPAction> threadActions;
    private ArrayList<IPlan> threadRefinements;

    public POPThread(ArrayList<POPAction> actions, PlannerFactoryImp conf, ArrayList<OpenCondition> goals,
                     Step initial, Step last, POPMultiThread planner) {
        super(planner.comm);
        this.parent = planner;
        this.solutionChecker = ((POPMultiThread) parent).getSolutionChecker();
        threadActions = actions;

        this.myAgent = conf.getAgent();

        initialStep = initial;
        finalStep = last;

        initialInternalPlan = new POPInternalPlan(null, null, null, null, goals, null, null, false, this);
        this.initialIncrementalPlan = new POPIncrementalPlan(initialInternalPlan, null, this);

        this.successors = new ArrayList<POPInternalPlan>();

        initialIncrementalPlan.setName(0, null);
        initializeArrays();

        this.matrix = new POPMatrix(20);
    }

    public void run() {
        threadRefinements = POPForwardLoop();
    }

    public void setCurrentInternalPlan(POPInternalPlan p) {
        currentInternal = p;
    }

    @Override
    public ArrayList<IPlan> POPForwardLoop() {
        ArrayList<IPlan> succ, refinements = new ArrayList<IPlan>();
        this.hashEffects = null;
        POPInternalPlan auxInternal;
        POPStep step;
        currentInternal = new POPInternalPlan(currentInternal, this);
        auxInternal = currentInternal;
        initialInternalPlan.setNumSteps(-1);

        //Synchronize class information
        initialInternalPlan.setNumSteps(antecessors.length);
        calculateCausalLinks(((POPMultiThread) parent).getBasePlan(), antecessors);
        calculateOrderings(((POPMultiThread) parent).getBasePlan(), antecessors);

        //Search loop; add an applicable action to the plan
        for (POPAction act : threadActions) {
            currentInternal = auxInternal;

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

    public void initializeSearchTree(POPInternalPlan p) {
        this.internalSearchTree = new POPInternalSearchTree(p);
    }

    public void setAntecessors(POPIncrementalPlan[] ant) {
        antecessors = new POPIncrementalPlan[ant.length];
        for (int i = 0; i < ant.length; i++)
            antecessors[i] = ant[i];
    }

    public ArrayList<IPlan> getRefinements() {
        return this.threadRefinements;
    }

    public void initializeArrays() {
        totalCausalLinks = new CustomArrayList<CausalLink>(50);
        totalOrderings = new CustomArrayList<Ordering>(50);
    }

    //Adds all the causal links when expanding a plan
    public void calculateCausalLinks(POPIncrementalPlan base, POPIncrementalPlan[] antecessors) {
        //The base plan does not include any causal links
        this.getTotalCausalLinks().clear();
        if (!base.isRoot()) {
            POPIncrementalPlan aux = base;
            while (!aux.isRoot()) {
                for (CausalLink c : aux.getCausalLinks())
                    this.getTotalCausalLinks().add(c);
                aux = aux.getFather();
            }
        }
        this.setNumCausalLinks(this.getTotalCausalLinks().size());
        this.setModifiedCausalLinks(false);
    }

    //Adds all the causal links when expanding a plan
    public void calculateOrderings(POPIncrementalPlan base, POPIncrementalPlan[] antecessors) {
        //The base plan does not include any causal links
        this.getTotalOrderings().clear();
        if (!base.isRoot()) {
            POPIncrementalPlan aux = base;
            while (!aux.isRoot()) {
                for (Ordering o : aux.getOrderings())
                    //if(!POPIncrementalPlan.totalOrderings.includes(o))
                    this.getTotalOrderings().add(o);
                aux = aux.getFather();
            }
        }
        this.setNumOrderings(this.getTotalOrderings().size());
        this.setModifiedOrderings(false);
    }

    void clearThreadActions() {
        this.threadActions.clear();
    }

    void addApplicableAction(POPAction pa) {
        this.threadActions.add(pa);
    }

    @Override
    public IPlan sendProposalsMonoagent(ArrayList<IPlan> proposals, IPlan basePlan) {
        return null;
    }

    @Override
    public void evaluatePlan(IPlan plan, ArrayList<EvaluationThread> evThreads) {
    }

    @Override
    public void setAntecessors(POPIncrementalPlan nextPlan) {
    }
}