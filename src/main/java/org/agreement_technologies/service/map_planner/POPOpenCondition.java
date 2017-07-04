package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_planner.Condition;
import org.agreement_technologies.common.map_planner.OpenCondition;

/**
 * Open condition definition.
 * Parameters: reference to the condition, associated step, associated plan, flag that indicates if the precondition is a goal.
 *
 * @author Alex
 */
public class POPOpenCondition implements OpenCondition {
    static final int EQUAL = 1;
    static final int DISTINCT = 2;

    private POPPrecEff precEff;
    private POPStep step;
    private Boolean isGoal;
    //private POPPlan plan;
    //private String key;

    /**
     * Constructor
     *
     * @param cond Reference to the original precondition
     * @param s    Associated step
     * @param g    Flag that indicates if the open condition is a goal
     * @param p    Reference to the plan in which de open condition is included
     */
    public POPOpenCondition(POPPrecEff cond, POPStep s, Boolean g/*, POPPlan p*/) {
        this.step = s;
        this.isGoal = g;
        this.precEff = cond;
        //this.plan = p;
        //this.key = this.getCondition().toKey();
    }

    public Condition getCondition() {
        return precEff.getCondition();
    }

    public POPPrecEff getPrecEff() {
        return precEff;
    }

    public POPStep getStep() {
        return step;
    }

    public void setStep(POPStep step) {
        this.step = step;
    }
    //public int getMinTime()                     {return precEff.getMinTime();}

    /**
     * Checks if the open condition is a goal
     */
    public Boolean isGoal() {
        return this.isGoal;
    }

    public void setGoal() {
        this.isGoal = true;
    }

    public void setNotGoal() {
        this.isGoal = false;
    }
    //public POPPlan getPlan()            {return plan;}
    //public void setPlan(POPPlan plan)   {this.plan = plan;}
    //public String toKey()               {return key;}

    /**
     * Defines a key to identify the open condition in hash tables
     * <p>
     * private void setKey() {
     * this.key = this.condition.getFunction().getName() + "(";
     * for(String s: this.condition.getFunction().getParams())
     * this.key += s + ",";
     * <p>
     * if(this.condition.getType() == EQUAL)
     * this.key += ")=";
     * if(this.condition.getType() == DISTINCT)
     * this.key += ")!=";
     * this.key += this.condition.getValue();
     * }
     */

    public String toString() {
        return this.precEff.toString();
    }

}
