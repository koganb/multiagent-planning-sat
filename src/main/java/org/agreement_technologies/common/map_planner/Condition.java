package org.agreement_technologies.common.map_planner;

public interface Condition {
    static final int EQUAL = 1;
    static final int DISTINCT = 2;

    // Returns the condition type (EQUAL or DISTINCT)
    int getType();

    // Returns the code of the variable
    int getVarCode();

    // Returns the code of the value
    int getValueCode();

    String toKey();

    String labeled(PlannerFactory pf);
}
