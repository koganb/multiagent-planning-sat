package org.agreement_technologies.common.map_landmarks;

import java.util.ArrayList;

/**
 * Common landmarks class
 *
 * @author Alex
 */
public interface Landmarks {
    int NECESSARY_ORDERINGS = 1;
    int REASONABLE_ORDERINGS = 2;
    int ALL_ORDERINGS = 3;

    ArrayList<LandmarkOrdering> getOrderings(int type, boolean onlyGoals);

    void filterTransitiveOrders();

    void removeCycles();

    ArrayList<LandmarkNode> getNodes();

    int numGlobalNodes();

    int numTotalNodes();
}
