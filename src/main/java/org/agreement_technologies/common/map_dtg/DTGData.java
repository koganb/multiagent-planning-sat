package org.agreement_technologies.common.map_dtg;

import org.agreement_technologies.common.map_grounding.GroundedCond;
import org.agreement_technologies.common.map_grounding.GroundedEff;
import org.agreement_technologies.common.map_grounding.GroundedTask;
import org.agreement_technologies.common.map_grounding.GroundedVar;

import java.util.ArrayList;

public class DTGData implements java.io.Serializable {
    private static final long serialVersionUID = 6011631219080464901L;
    private String varName;
    private String fromValue, toValue;
    private ArrayList<DTGCondition> commonPrecs;
    private ArrayList<DTGEffect> commonEffs;

    public DTGData(DTGTransition t, String ag) {
        GroundedVar v = t.getVar();
        this.varName = v.shareable(ag) ? v.toString() : "?";
        this.fromValue = v.shareable(t.getStartValue(), ag) ? t.getStartValue() : "?";
        this.toValue = v.shareable(t.getFinalValue(), ag) ? t.getFinalValue() : "?";
        commonPrecs = new ArrayList<DTGCondition>();
        for (GroundedCond prec : t.getCommonPreconditions())
            if (prec.getVar().shareable(prec.getValue(), ag))
                commonPrecs.add(new DTGCondition(prec));
        commonEffs = new ArrayList<DTGEffect>();
        for (GroundedEff eff : t.getCommonEffects())
            if (eff.getVar().shareable(eff.getValue(), ag))
                commonEffs.add(new DTGEffect(eff));
    }

    public static boolean shareable(DTGTransition t, String ag) {
        GroundedVar v = t.getVar();
        if (!v.shareable(ag)) return false;
        if (!v.shareable(t.getStartValue(), ag) && !v.shareable(t.getFinalValue(), ag))
            return false;
        return true;
    }

    public String toString() {
        String s = "";
        for (DTGCondition prec : commonPrecs)
            if (s.equals("")) s = prec.toString();
            else s = s + "," + prec.toString();
        return varName + ": " + fromValue + "->" + toValue + " [" + s + "]";
    }

    public String getVarName() {
        return varName;
    }

    public String getStartValue() {
        return fromValue;
    }

    public String getFinalValue() {
        return toValue;
    }

    public GroundedCond[] getCommonPrecs(GroundedTask task) {
        GroundedVar[] vars = task.getVars();
        GroundedCond[] precs = new GroundedCond[commonPrecs.size()];
        for (int i = 0; i < commonPrecs.size(); i++) {
            DTGCondition c = commonPrecs.get(i);
            GroundedVar v = null;
            for (GroundedVar aux : vars)
                if (c.varName.equals(aux.toString())) {
                    v = aux;
                    break;
                }
            if (v == null)
                throw new RuntimeException("Unknown variable '" + c.varName +
                        "' received during the DTG construction");
            precs[i] = task.createGroundedCondition(c.condition, v, c.value);
        }
        return precs;
    }

    public GroundedEff[] getCommonEffs(GroundedTask task) {
        GroundedVar[] vars = task.getVars();
        GroundedEff[] effs = new GroundedEff[commonEffs.size()];
        for (int i = 0; i < commonEffs.size(); i++) {
            DTGEffect c = commonEffs.get(i);
            GroundedVar v = null;
            for (GroundedVar aux : vars)
                if (c.varName.equals(aux.toString())) {
                    v = aux;
                    break;
                }
            if (v == null)
                throw new RuntimeException("Unknown variable '" + c.varName +
                        "' received during the DTG construction");
            effs[i] = task.createGroundedEffect(v, c.value);
        }
        return effs;
    }

    private static class DTGCondition implements java.io.Serializable {
        private static final long serialVersionUID = -8411329280671918342L;
        private int condition;
        private String varName;
        private String value;

        public DTGCondition(GroundedCond prec) {
            this.condition = prec.getCondition();
            this.varName = prec.getVar().toString();
            this.value = prec.getValue();
        }

        public String toString() {
            return "(" + varName + (condition == GroundedCond.EQUAL ? "=" : "<>") +
                    value + ")";
        }
    }

    private static class DTGEffect implements java.io.Serializable {
        private static final long serialVersionUID = -8967487407479184565L;

        private String varName;
        private String value;

        public DTGEffect(GroundedEff eff) {
            this.varName = eff.getVar().toString();
            this.value = eff.getValue();
        }

        public String toString() {
            return "(" + varName + "=" + value + ")";
        }
    }
}
