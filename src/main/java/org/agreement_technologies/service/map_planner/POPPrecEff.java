package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_planner.Condition;

/**
 * Function-value tuple that defines preconditions and effects in a POP; implements the PreconditionEffect interface.
 * Parameters: reference to the original grounded condition, variable, current value, condition (equal o distinct).
 *
 * @author Alex
 */
public class POPPrecEff {
    static final int EQUAL = 1;
    static final int DISTINCT = 2;
    static final boolean IS_PREC = true;
    static final boolean IS_EFF = false;
    private Condition condition;
    private POPFunction function;
    private String value;
    private int conditionType;
    private String key;
    //private int minTime;
    //private ArrayList<String> agents;
    //private int producers;
    private int index;

    public POPPrecEff(Condition cond, POPFunction var, String val, int co) {
        //, int mt, ArrayList<String> ag, int prod) {
        //this.minTime = var.getVariable().getMinTime(val);
        this.condition = cond;
        this.conditionType = co;
        this.function = var;
        this.value = val;
        //this.minTime = mt;
        //this.agents = ag;
        //this.producers = prod;
        this.key = null;
        this.key = this.toKey();
    }

    public int getVarCode() {
        return condition.getVarCode();
    }

    public int getValueCode() {
        return condition.getValueCode();
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int i) {
        this.index = i;
    }

    public String getValue() {
        return this.value;
    }

    public int getType() {
        return this.conditionType;
    }

    //public ArrayList<String> getAgents()                {return agents;}
    //public void setAgents(ArrayList<String> agents)     {this.agents = agents;}
    public Condition getCondition() {
        return this.condition;
    }

    //public int getMinTime()                             {return this.minTime;}
    //public int getProducers()                           {return this.producers;}
    public void setGroundedCondition(Condition gc) {
        this.condition = gc;
    }

    public String toKey() {
        if (this.key == null) {
            String res;
            if (this.conditionType == EQUAL) res = "=";
            if (this.conditionType == DISTINCT) res = "<>";
            else res = "?";
            key = condition.getVarCode() + res + condition.getValueCode();
            return key;
                    /*new String();
            int n = 0;
            res += this. getFunction().getName() + "(";
            for(String s: this.getFunction().getParams()) {
                if(n == 0) n++;
                else res += ", ";
                res += s;
            }
            if(this.conditionType == EQUAL)       res += ") = ";
            if(this.conditionType == DISTINCT)    res += ") <> ";
            res += this.getValue();

            return res;*/
        } else return this.key;
    }

    /*
    public String[] getAgentsArray() {
        int i;
        String[] ag = new String[this.agents.size()];

        for(i = 0; i < this.agents.size(); i++)
            ag[i] = this.agents.get(i);

        return ag;
    }*/

    public String toString() {
        String res = new String();
        int n = 0;
        res += function.getName() + "(";
        for (String s : function.getParams()) {
            if (n == 0) n++;
            else res += ",";
            res += s;
        }
        if (this.conditionType == EQUAL) res += ")=";
        if (this.conditionType == DISTINCT) res += ")<>";
        res += this.getValue();
        //if(this.groundedCondition == null) res += " -> NO";
        //else res += " -> YES";

        return res;
    }

    public POPFunction getFunction() {
        return function;
    }

    //public void setProducers(int producers)             {this.producers = producers;}
    //public POPFunction getFunction()                    {return this.function;}
    public void setFunction(POPFunction v) {
        this.function = v;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        POPPrecEff that = (POPPrecEff) o;

        if (conditionType != that.conditionType) return false;
        if (index != that.index) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        return key != null ? key.equals(that.key) : that.key == null;

    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + conditionType;
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + index;
        return result;
    }
}
