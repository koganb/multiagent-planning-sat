package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_grounding.GroundedCond;
import org.agreement_technologies.common.map_grounding.GroundedEff;
import org.agreement_technologies.common.map_planner.Condition;
import org.agreement_technologies.common.map_planner.PlannerFactory;

public class POPCondition implements Condition {
    private int type;
    private int var;
    private int value;

    public POPCondition(GroundedCond c, PlannerFactory pf) {
        type = c.getCondition();
        var = pf.getCodeFromVar(c.getVar());
        value = pf.getCodeFromValue(c.getValue());
    }

    public POPCondition(int type, int var, int value) {
        this.type = type;
        this.var = var;
        this.value = value;
    }

    public POPCondition(GroundedEff e, PlannerFactoryImp pf) {
        this.type = EQUAL;
        this.var = pf.getCodeFromVar(e.getVar());
        this.value = pf.getCodeFromValue(e.getValue());
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public int getVarCode() {
        return var;
    }

    @Override
    public int getValueCode() {
        return value;
    }

    @Override
    public String toKey() {
        if (type == Condition.EQUAL)
            return var + "=" + value;
        else
            return var + "<>" + value;
    }

    public String toString() {
        return toKey();
    }

    @Override
    public String labeled(PlannerFactory pf) {
        if (pf == null) return toKey();
        String varName = pf.getVarNameFromCode(var);
        String valueName = pf.getValueFromCode(value);
        if (varName == null) varName = "" + var;
        if (valueName == null) valueName = "" + value;
        if (type == Condition.EQUAL)
            return varName + "=" + valueName;
        else
            return varName + "<>" + valueName;
    }
}
