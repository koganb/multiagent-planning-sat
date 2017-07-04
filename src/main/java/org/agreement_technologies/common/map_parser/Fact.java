package org.agreement_technologies.common.map_parser;

/**
 * Ungrounded fact
 *
 * @author Oscar Sapena
 * @since Mar 2011
 */
public interface Fact extends java.io.Serializable {
    // Returns the function name
    String getFunctionName();

    // Returns the function parameters
    String[] getParameters();

    // Returns the list of values assigned to the function
    String[] getValues();

    // Checks whether the assignment is negated
    boolean negated();

}
