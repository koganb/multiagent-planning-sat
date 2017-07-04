package org.agreement_technologies.common.map_grounding;


/**
 * Grounded belief rule
 *
 * @author Oscar Sapena
 * @since April 2011
 */
public interface GroundedRule extends java.io.Serializable {
    // Returns the rule name
    String getRuleName();

    // Returns the list of parameters (list of objects)
    String[] getParams();

    // Returns the body of the rule
    GroundedCond[] getBody();

    // Returns the head of the rule
    GroundedEff[] getHead();

}
