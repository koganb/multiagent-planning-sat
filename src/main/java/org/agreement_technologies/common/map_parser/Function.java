package org.agreement_technologies.common.map_parser;

/**
 * Ungrounded function
 *
 * @author Oscar
 * @since Mar 2011
 */
public interface Function extends java.io.Serializable {
    // Retrieves the function name
    String getName();

    // Returns the function parameters
    Parameter[] getParameters();

    // Returns the function domain
    String[] getDomain();

    // Checks if this is a multi-function
    boolean isMultifunction();
}
