package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_planner.CausalLink;
import org.agreement_technologies.common.map_planner.Condition;
import org.agreement_technologies.common.map_planner.Step;

/**
 * Causal link between two steps of a partial-order plan; implements the
 * CausalLink interface.
 *
 * @author Alejandro Torreño
 */
public class POPCausalLink implements CausalLink {
    private static final long serialVersionUID = -328831501262615523L;
    // Parametros: pasos afectados p1 y p2, condicion variable-valor que crea el enlace
    private Step step1;
    private POPPrecEff condition;
    private Step step2;

    public POPCausalLink(POPStep s1, POPPrecEff c, POPStep s2) {
        this.step1 = s1;
        this.condition = c;
        this.step2 = s2;
    }

    @Override
    public int getIndex1() {
        return this.step1.getIndex();
    }

    @Override
    public Condition getCondition() {
        return this.condition.getCondition();
    }

    public void setCondition(POPPrecEff v) {
        this.condition = v;
    }

    @Override
    public int getIndex2() {
        return this.step2.getIndex();
    }

    @Override
    public String toString() {
        String res = "(" + this.getIndex1() + ") -";
        res += this.condition.toString() + "-> (";
        res += this.getIndex2() + ")";

        return res;
    }

    @Override
    public Step getStep1() {
        return this.step1;
    }

    @Override
    public Step getStep2() {
        return this.step2;
    }

    @Override
    public POPFunction getFunction() {
        return condition.getFunction();
    }

    public void setFunction(POPFunction popFunction) {
        condition.setFunction(popFunction);
    }
}
