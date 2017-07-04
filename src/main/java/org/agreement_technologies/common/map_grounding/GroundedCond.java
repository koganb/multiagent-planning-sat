package org.agreement_technologies.common.map_grounding;

/**
 * Grounded condition: (= variable value) or (<> variable value)
 *
 * @author Oscar Sapena
 * @since April 2011
 */
public interface GroundedCond extends java.io.Serializable {
    static final int EQUAL = 1;
    static final int DISTINCT = 2;

    // Returns the condition type (EQUAL or DISTINCT)
    int getCondition();

    // Returns the grounded variable
    GroundedVar getVar();

    // Returns the value (object name, 'undefined' is not allowed)
    String getValue();

    //@Sergio
    boolean equals(GroundedCond e);
}
