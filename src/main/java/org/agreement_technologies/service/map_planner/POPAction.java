package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_grounding.Action;
import org.agreement_technologies.common.map_planner.Condition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//Acciones para el POP; sustituye a la antigua clase PDDLAction
public class POPAction implements Serializable {
    private String actionName;
    //private Action operator;
    //private ArrayList<String> params;
    private List<POPPrecEff> precs;
    private List<POPPrecEff> effects;
    private Condition[] precConds;
    private Condition[] effConds;
    //private int minTime;
    private boolean[] effectInVariable;

    public POPAction(String actionName, List<POPPrecEff> precs, List<POPPrecEff> effs) {
        int i;
        this.actionName = actionName;
        if (precs != null) {
            this.precs = new ArrayList<POPPrecEff>(precs.size());
            this.precConds = new Condition[precs.size()];
            for (i = 0; i < precs.size(); i++) {
                POPPrecEff p = precs.get(i);
                this.precs.add(p);
                this.precConds[i] = p.getCondition();
            }
        } else {
            this.precs = new ArrayList<POPPrecEff>(0);
            this.precConds = new Condition[0];
        }
        if (effs != null) {
            this.effects = new ArrayList<POPPrecEff>(effs.size());
            this.effConds = new Condition[effs.size()];
            for (i = 0; i < effs.size(); i++) {
                POPPrecEff e = effs.get(i);
                this.effects.add(e);
                this.effConds[i] = e.getCondition();
            }
        } else {
            this.effects = new ArrayList<POPPrecEff>(0);
            this.effConds = new Condition[0];
        }
        if (this.actionName.equals("Initial") || this.actionName.equals("Final"))
            effectInVariable = new boolean[0];
        else {
            effectInVariable = new boolean[this.precs.size()];
            for (i = 0; i < effectInVariable.length; i++)
                effectInVariable[i] = false;
            POPPrecEff p;
            for (i = 0; i < this.precs.size(); i++) {
                p = this.precs.get(i);
                for (POPPrecEff e : this.effects) {
                    if (e.getVarCode() == p.getVarCode()) {
                        effectInVariable[i] = true;
                        break;
                    }
                }
            }
        }
    }

    //Pasamos en el constructor las precondiciones y efectos ya como POPFunctions
    public POPAction(Action act, ArrayList<POPPrecEff> precs, ArrayList<POPPrecEff> effs) {
        int i;
        //this.params = null;
        //this.operator = act;
        if (act != null) {
            this.actionName = act.getOperatorName();
            //if (actionName.equalsIgnoreCase("initial") || actionName.equalsIgnoreCase("final"))
            //	System.out.println("aqui");

            //this.minTime = act.getMinTime();
            //this.params = new ArrayList<String>(act.getParams().length);
            for (i = 0; i < act.getParams().length; i++)
                this.actionName += " " + act.getParams()[i];
            // this.params.add(act.getParams()[i]);
        } else {
            this.actionName = null;
            //this.minTime = -1;
        }
        if (precs != null) {
            this.precs = new ArrayList<POPPrecEff>(precs.size());
            this.precConds = new Condition[precs.size()];
            for (i = 0; i < precs.size(); i++) {
                POPPrecEff p = precs.get(i);
                this.precs.add(p);
                this.precConds[i] = p.getCondition();
            }
        } else {
            this.precs = new ArrayList<POPPrecEff>(0);
            this.precConds = new Condition[0];
        }
        if (effs != null) {
            this.effects = new ArrayList<POPPrecEff>(effs.size());
            this.effConds = new Condition[effs.size()];
            for (i = 0; i < effs.size(); i++) {
                POPPrecEff e = effs.get(i);
                this.effects.add(e);
                this.effConds[i] = e.getCondition();
            }
        } else {
            this.effects = new ArrayList<POPPrecEff>(0);
            this.effConds = new Condition[0];
        }
        if (this.actionName.equals("Initial") || this.actionName.equals("Final"))
            effectInVariable = new boolean[0];
        else {
            effectInVariable = new boolean[this.precs.size()];
            for (i = 0; i < effectInVariable.length; i++)
                effectInVariable[i] = false;
            POPPrecEff p;
            for (i = 0; i < this.precs.size(); i++) {
                p = this.precs.get(i);
                for (POPPrecEff e : this.effects) {
                    if (e.getVarCode() == p.getVarCode()) {
                        effectInVariable[i] = true;
                        break;
                    }
                }
            }
        }
    }

    public boolean hasEffectInVariable(int index) {
        return effectInVariable[index];
    }

    //public Action getOperator()                 {return this.operator;}
    public String getName() {
        return this.actionName;
    }

    //public int getMinTime()                     {return this.minTime;}
    public void setName(String name) {
        this.actionName = name;
    }

    //public ArrayList<String> getParams()        {return this.params;}
    public List<POPPrecEff> getPrecs() {
        return this.precs;
    }

    public List<POPPrecEff> getEffects() {
        return this.effects;
    }

    public String toString() {
        return this.actionName;
    }

    public Condition[] getPrecConditions() {
        return precConds;
    }

    public Condition[] getEffConditions() {
        return effConds;
    }
}
