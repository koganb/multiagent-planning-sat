package org.agreement_technologies.common.map_parser;

/**
 * Ungrounded condition
 *
 * @author Oscar
 * @since Mar 2011
 */
public interface Condition {

    static final int EQUAL = 0;
    static final int DISTINCT = 1;
    static final int MEMBER = 2;
    static final int NOT_MEMBER = 3;
    static final int ASSIGN = 4;
    static final int ADD = 5;
    static final int DEL = 6;

    // Returns the condition type
    int getType();

    // Returns the condition function
    Function getFunction();

    // Returns the condition value
    String getValue();
}
