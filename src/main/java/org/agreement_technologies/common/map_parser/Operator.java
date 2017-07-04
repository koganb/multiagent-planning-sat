package org.agreement_technologies.common.map_parser;

/**
 * Operator (ungrounded action)
 *
 * @author Oscar
 * @since Mar 2011
 */
public interface Operator {
    // Returns the operator name
    String getName();

    // Returns the operator parameters
    Parameter[] getParameters();

    // Get the operator precondition (list of conditions)
    Condition[] getPrecondition();

    // Get the operator effect (list of effects)
    Condition[] getEffect();

    // Returns the preference value. Returns -1 if it is not set
    int getPreferenceValue();

    // Get the numeric effects (list of effects) of the operator
    NumericEffect[] getNumericEffects();
}
