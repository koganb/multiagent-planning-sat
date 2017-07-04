package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_planner.Condition;
import org.agreement_technologies.common.map_planner.Step;

import java.util.List;

/**
 * Models the steps of a partial-order plan; implements the Step interface.
 * Parameters: reference to the action associated to the step, agent that introduces the step, index of the step in the plan.
 *
 * @author Alex
 */
public class POPStep implements Step {
    private POPAction action;
    private String agent;
    private int index;

    private int timeStep;

    public POPStep(POPAction act, int i, String ag) {
        this.action = act;
        this.agent = ag;
        this.index = i;
        this.timeStep = -1;
    }

    public POPAction getAction() {
        return this.action;
    }

    //public Action getGroundedAction()  {return this.action.getOperator();}
    public String getAgent() {
        return this.agent;
    }

    public int getIndex() {
        return this.index;
    }

    //Implementation of the Step interface methods
    public String getName() {
        return this.action.getName();
    }
    /*
    public String[] getParameters() {
        int i;
        String[] params = new String[this.getAction().getParams().size()];

        for(i = 0; i < this.getAction().getParams().size(); i++)
            params[i] = this.getAction().getParams().get(i);

        return params;
    }*/

    public POPPrecEff[] getPreconditions() {
        int i;
        POPPrecEff[] precs = new POPPrecEff[this.getAction().getPrecs().size()];

        for (i = 0; i < this.getAction().getPrecs().size(); i++)
            precs[i] = this.getAction().getPrecs().get(i);

        return precs;
    }

    public POPPrecEff[] getEffects() {
        int i;
        POPPrecEff[] effs = new POPPrecEff[this.getAction().getEffects().size()];

        for (i = 0; i < this.getAction().getEffects().size(); i++)
            effs[i] = this.getAction().getEffects().get(i);

        return effs;
    }

    public String toString() {
        String res;

        if (this.index == 0) res = "Initial";
        else if (this.index == 1) res = "Last";
        else res = this.action.toString();

        return res;
    }

    @Override
    public int getTimeStep() {
        return this.timeStep;
    }

    public void setTimeStep(int ts) {
        this.timeStep = ts;
    }

    @Override
    public String getActionName() {
        return action.getName();
    }

    @Override
    public Condition[] getPrecs() {
        return action.getPrecConditions();
    }

    @Override
    public Condition[] getEffs() {
        return action.getEffConditions();
    }


    //borisk add to interface
    @Override
    public List<POPPrecEff> getPopPrecs() {
        return action.getPrecs();
    }

    @Override
    public List<POPPrecEff> getPopEffs() {
        return action.getEffects();
    }
}
