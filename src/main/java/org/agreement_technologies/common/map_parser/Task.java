package org.agreement_technologies.common.map_parser;

/**
 * Ungrounded planning task
 * Stores the problem and domain information of a parsed planning task
 *
 * @author Oscar Sapena
 * @since Mar 2011
 */
public interface Task {
    /**
     * Returns the domain name
     *
     * @return Domain name
     */
    String getDomainName();

    /**
     * Return the problem name
     *
     * @return Problem name
     */
    String getProblemName();

    /**
     * Returns the requirements list
     *
     * @return Array of strings, each string representing a requirement
     * specified in the domain file. Supported requirements are: strips,
     * typing, negative-preconditions and object-fluents
     */
    String[] getRequirements();

    /**
     * Returns the list of types
     *
     * @return Array of strings, each string is a type defined in the domain file
     */
    String[] getTypes();

    /**
     * Returns the base types of a given type
     *
     * @param type Name of the type
     * @return Array of strings which contains the super-types for the given type
     */
    String[] getParentTypes(String type);

    /**
     * Returns the list of objects
     *
     * @return Array of string containing the names of the objects declared in the
     * domain (constants section) and problem (objects section) files
     */
    String[] getObjects();

    /**
     * Returns the type list of a given object
     *
     * @param objName Object name
     * @return Array of string containing the set of types of the given object
     */
    String[] getObjectTypes(String objName);

    /**
     * Returns the list of functions (predicates are also included as they are
     * considered boolean functions)
     *
     * @return Array of functions defined in the domain file
     */
    Function[] getFunctions();

    /**
     * Returns the list of operators
     *
     * @return Array of operators defined in the domain file
     */
    Operator[] getOperators();

    /**
     * Returns the shared data, which defines the information the current agent
     * can share with the other ones
     *
     * @return Array of shared data defined in the problem file
     */
    SharedData[] getSharedData();

    /**
     * Returns the initial state information
     *
     * @return Array of facts
     */
    Fact[] getInit();

    /**
     * Returns the list of belief rules
     *
     * @return Array of belief rules
     */
    Operator[] getBeliefs();

    /**
     * Returns the list of goals
     *
     * @return Array of goals (facts)
     */
    Fact[] getGoals();

    double getSelfInterest();

    double getMetricThreshold();

    Fact[] getPreferences();

    String getPreferenceName(int index);

    Metric getMetric();

    Congestion[] getCongestion();

    NumericFact[] getInitialNumericFacts();
}
