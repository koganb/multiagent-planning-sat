package org.agreement_technologies.common.map_grounding;

/**
 * Grounded variable
 *
 * @author Oscar Sapena
 * @since April 2011
 */
public interface GroundedVar extends java.io.Serializable {
    // Returns the function name
    String getFuctionName();

    // Returns the function parameters (list of object names)
    String[] getParams();

    // Returns the list of types for a given parameter (0 .. getParams().length - 1)
    String[] getParamTypes(int paramNumber);

    // Returns the function domain types
    String[] getDomainTypes();

    // Returns the initial true value (object name) or null if it has none
    String initialTrueValue();

    // Returns the initial false values for this variable (list of objects)
    String[] initialFalseValues();

    // Minimum time, according to the disRPG, in which the variable can get the
    // given value (objName). Returns -1 if the given value is not reachable
    int getMinTime(String objName);

    // Minimal time, according to the disRPG, in which a given agent can get this
    // variable to have a given value (objName). Returns -1 if the given agent
    // cannot assign the given value to this variable
    int getMinTime(String objName, String agent);

    // Checks whether the given value for this variable can be shared with the given agent
    boolean shareable(String objName, String agent);

    // Checks whether the given variable can be shared with the given agent
    boolean shareable(String agent);

    // List of reachable values for this variable
    String[] getReachableValues();

    boolean isBoolean();
}
