package org.agreement_technologies.common.map_grounding;

/**
 * Reached values in the grounding process
 *
 * @author Oscar Sapena
 * @since May 2011
 */
public interface ReachedValue extends java.io.Serializable {
    // Gets the involved variable
    GroundedVar getVar();

    // Gets the value for this variable
    String getValue();

    // Gets the minimum time for the variable to get this value
    int getMinTime();

    // Checks if this value can be shared to another agent
    boolean shareable(String agName);
}
