package org.agreement_technologies.common.map_grounding;

import java.util.ArrayList;

/**
 * @author Oscar
 */
public interface GroundedCongestion {

    public String getName();

    public String getFullName();

    public int getNumParameters();

    public ArrayList<String> getParameters();

    public GroundedCongestionUsage getUsage();

    public ArrayList<GroundedCongestionPenalty> getPenalty();
}
