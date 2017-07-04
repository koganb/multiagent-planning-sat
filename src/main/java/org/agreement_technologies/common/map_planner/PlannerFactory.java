package org.agreement_technologies.common.map_planner;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_communication.PlanningAgentListener;
import org.agreement_technologies.common.map_grounding.GroundedTask;
import org.agreement_technologies.common.map_grounding.GroundedVar;
import org.agreement_technologies.common.map_heuristic.Heuristic;


/**
 * Common interface for the planner factory
 *
 * @author Alex
 */
public interface PlannerFactory {
    public static final int SEARCH_SPEED = 0,
            SEARCH_BALANCED = 1,
            SEARCH_QUALITY = 2,
            SEARCH_LANDMARKS = 3;

    //Plan createInitialPlan(GroundedTask gTask);
    Planner createPlanner(GroundedTask gTask, Heuristic h, AgentCommunication comm,
                          PlanningAgentListener agentListener, int searchType, int neg, boolean anytime);
    /**
     * Builds a plan using the given information
     * @param gTask    Grounded task
     * @param steps    Array of plan steps
     * @param orderings Array of orderings
     * @param causalLinks Array of causal links
     * @param fictitiousStepsIncluded This parameter is false when the fictitious
     * 		  steps are not included in the given array of steps; in this case,
     * 		  this method generates the fictitious steps according to the
     * 		  information contained in the grounded task
     * @return Plan generated
     */
    //Plan createPlan(GroundedTask gTask, Step[] steps, Ordering[] orderings, 
    //		CausalLink[] causalLinks, boolean fictitiousStepsIncluded);

    /**
     * Creates a new plan ordering
     *
     * @param stepIndex1 Index of the first plan step
     * @param stepIndex2 Index of the second plan step
     * @return New ordering
     */
    Ordering createOrdering(int stepIndex1, int stepIndex2);

    /**
     * Builds a new causal link
     *
     * @param condition Grounded condition
     * @param step1     First plan step
     * @param step2     Second plan step
     * @return New causal link
     */
    CausalLink createCausalLink(Condition condition, Step step1, Step step2);

    /**
     * Creates a new step
     *
     * @param stepIndex Step index in the plan
     * @param agent     Executor agent
     * @param opName    Operator name
     * @param params    Array of action parameters
     * @param prec      Array of preconditions
     * @param eff       Array of effects
     * @return New plan step
     */
    Step createStep(int stepIndex, String agent, String actionName,
                    Condition[] prec, Condition[] eff);

    /**
     * Gets the string key of a variable from its global identifier
     *
     * @param code Global identifier of a variable
     * @return String key of the variable; null if the variable is not in the agent's domain
     */
    String getVarNameFromCode(int code);
    /**
     * Gets the string key of an effect from its global identifier
     * @param code Global identifier of an effect
     * @return String key of the effect; null if the effect is not in the agent's domain
     */
    //String getEffectFromCode(int code);

    /**
     * Gets a value from its global identifier
     *
     * @param code Global identifier of a value
     * @return Value; null if the value is not in the agent's domain
     */
    String getValueFromCode(int code);

    /**
     * Gets the global identifier of a variable from its string key
     *
     * @param var String key of a variable
     * @return Global identifier of the variable; -1 if the variable is not in the agent's domain
     */
    int getCodeFromVarName(String var);

    /**
     * Gets the global identifier of a variable
     *
     * @param var variable
     * @return Global identifier of the variable; -1 if the variable is not in the agent's domain
     */
    int getCodeFromVar(GroundedVar var);
    /**
     * Gets the global identifier of an effect from its string key
     * @param eff String key of an effect
     * @return Global identifier of the effect; -1 if the effect is not in the agent's domain
     */
    //int getCodeFromEffect(String eff);

    /**
     * Gets the global identifier of a value
     *
     * @param val Value
     * @return Global identifier of the value; -1 if the value is not in the agent's domain
     */
    int getCodeFromValue(String val);
}
