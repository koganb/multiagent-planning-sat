package org.agreement_technologies.common.map_grounding;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Grounded planning task
 *
 * @author Oscar Sapena
 * @since April 2011
 */
public interface GroundedTask extends java.io.Serializable {
    public static final int SAME_OBJECTS_DISABLED = 0;
    public static final int SAME_OBJECTS_REP_PARAMS = 1;
    public static final int SAME_OBJECTS_PREC_EQ_EFF = 2;

    // Gets the domain name
    String getDomainName();

    // Gets the problem name
    String getProblemName();

    // Gets the name of this agent
    String getAgentName();

    /**
     * Returns the list of agents in the MAP task
     *
     * @return Array of string (agent names)
     */
    String[] getAgentNames();

    // Gets the requirement list
    String[] getRequirements();

    // Gets the list of types
    String[] getTypes();

    // Gets the parent types of a given type
    String[] getParentTypes(String type);

    // Gets the object list (including 'undefined')
    String[] getObjects();

    // Gets the list of types for a given object
    String[] getObjectTypes(String objName);

    // Gets the list of variables
    GroundedVar[] getVars();

    GroundedVar getVarByName(String varName);

    // Gets the list of grounded actions
    ArrayList<Action> getActions();

    // Returns the list of grounded belief rules
    GroundedRule[] getBeliefs();

    // Returns the global goals
    ArrayList<GroundedCond> getGlobalGoals();

    /**
     * Creates a new grounded condition
     *
     * @param condition Condition type (EQUAL or DISTINCT)
     * @param var       Grounded variable
     * @param value     Value
     * @return New grounded condition
     */
    GroundedCond createGroundedCondition(int condition, GroundedVar var, String value);

    /**
     * Creates a new grounded effect
     *
     * @param var   Grounded variable
     * @param value Value
     * @return New grounded effect
     */
    GroundedEff createGroundedEffect(GroundedVar var, String value);

    /**
     * Creates a new action
     *
     * @param opName Operator name
     * @param params Action parameters
     * @param prec   Action preconditions
     * @param eff    Action effects
     * @return New action
     */
    Action createAction(String opName, String params[], GroundedCond prec[],
                        GroundedEff eff[]);

    void optimize();    // Optimize structures after grounding

    double getSelfInterestLevel();

    double getMetricThreshold();

    boolean metricRequiresMakespan();

    double evaluateMetric(HashMap<String, String> state, double makespan);

    double evaluateMetricMulti(HashMap<String, ArrayList<String>> state, double makespan);

    ArrayList<GroundedCond> getPreferences();

    int getNumPreferences();    // Returns the number of preferences

    double getViolatedCost(int prefIndex);    // prefIndex in [0,getNumPreferences()-1]

    boolean negationByFailure();

    ArrayList<GroundedCongestion> getCongestion();
}
