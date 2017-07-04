package org.agreement_technologies.common.map_grounding;

/**
 * Grounded effect: (assign variable value)
 *
 * @author Oscar Sapena
 * @since April 2011
 */
public interface GroundedEff extends java.io.Serializable {
    // Returns the grounded variable
    GroundedVar getVar();

    // Returns the value to assign (object name, 'undefined' is not allowed)
    String getValue();

    //@Sergio
    boolean equals(GroundedEff e);

    int hasCode();

}
