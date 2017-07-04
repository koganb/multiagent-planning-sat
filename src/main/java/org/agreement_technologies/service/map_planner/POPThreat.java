package org.agreement_technologies.service.map_planner;

//Amenaza que provoca un paso del plan sobre un enlace causal

/**
 * @author Alex
 */
public class POPThreat {
    //Parámetros: enlace causal, paso amenazante
    private int threatStep;
    private POPCausalLink causalLink;

    /**
     * @param ts
     * @param cl
     */
    public POPThreat(POPStep ts, POPCausalLink cl) {
        this.threatStep = ts.getIndex();
        this.causalLink = cl;
    }

    /**
     * @return
     */
    public int getThreateningStep() {
        return this.threatStep;
    }

    public POPCausalLink getCausalLink() {
        return this.causalLink;
    }
}
