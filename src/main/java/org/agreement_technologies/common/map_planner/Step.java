package org.agreement_technologies.common.map_planner;


import org.agreement_technologies.service.map_planner.POPPrecEff;

import java.util.List;

/**
 * Common interface for plan steps
 *
 * @author Alex
 */
public interface Step {
    String getActionName();

    Condition[] getPrecs();

    Condition[] getEffs();

    int getIndex();

    String getAgent();

    int getTimeStep();

    void setTimeStep(int st);

    List<POPPrecEff> getPopPrecs();

    List<POPPrecEff> getPopEffs();

    public String getUuid();

}