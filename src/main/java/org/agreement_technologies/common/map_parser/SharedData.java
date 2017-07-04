package org.agreement_technologies.common.map_parser;

/**
 * Ungrounded shared data
 *
 * @author Oscar Sapena
 * @since Mar 2011
 */
public interface SharedData {
    // Returns the function
    Function getFunction();

    // Get the list of agents that can observe this function
    String[] getAgents();
}
