package org.agreement_technologies.common.map_landmarks;

import java.util.ArrayList;

public interface LandmarkGraph {

    ArrayList<LandmarkOrdering> getReasonableOrderingList();

    ArrayList<LandmarkOrdering> getNeccessaryOrderingList();

    ArrayList<LandmarkNode> getNodes();

    int numGlobalNodes();

    int numTotalNodes();
}
