package org.agreement_technologies.common.map_parser;

/**
 * A parameter represents a name with a list of types
 *
 * @author Oscar
 * @since Mar 2011
 */
public interface Parameter extends java.io.Serializable {
    // Gets the parameter name
    String getName();

    // Return the parameter type list
    String[] getTypes();
}
