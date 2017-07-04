package org.agreement_technologies.common.map_communication;

import org.agreement_technologies.common.map_planner.Plan;
import org.agreement_technologies.common.map_planner.PlannerFactory;

/**
 * @author Oscar Sapena
 *         Receives status changes from a planning agent
 */
public interface PlanningAgentListener {

    void statusChanged(int status);                // Agent status change notification

    void notyfyError(String msg);                // Notifies an error message

    void trace(int indentLevel, String msg);    // Shows a trace message

    void newPlan(Plan plan, PlannerFactory pf);    // Shows a new plan in the tree

    void showPlan(Plan plan, PlannerFactory pf);// Clears the tree and shows a single plan

    void selectPlan(String planName);            // Selects a plan by its name
}
