package org.agreement_technologies.common.map_grounding;

/**
 * Planning action (grounded operator)
 *
 * @author Oscar Sapena
 * @since April 2011
 */
public interface Action {
    // Returns the operator name
    String getOperatorName();

    // Returns the list of parameters (list of objects)
    String[] getParams();

    // Action preconditions
    GroundedCond[] getPrecs();

    // Action effects
    GroundedEff[] getEffs();

    // Minimum time, according to the disRPG, in which the action can be executed
    int getMinTime();

    void optimize();

    GroundedNumericEff[] getNumEffs();

    boolean isMutex(Action a);
}
