package org.agreement_technologies.common.map_parser;

/**
 * @author Oscar
 */
public interface NumericFact {
    // Returns the function name
    String getFunctionName();

    // Returns the function parameters
    String[] getParameters();

    // Returns the value assigned to the function
    double getValue();
}
