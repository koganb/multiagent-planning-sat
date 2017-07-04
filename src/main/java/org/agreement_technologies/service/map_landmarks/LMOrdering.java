package org.agreement_technologies.service.map_landmarks;

import org.agreement_technologies.common.map_landmarks.LandmarkNode;
import org.agreement_technologies.common.map_landmarks.LandmarkOrdering;

/**
 * @author Alex
 */
public class LMOrdering implements LandmarkOrdering {
    private LandmarkNode node1;
    private LandmarkNode node2;
    private int type;

    public LMOrdering(LandmarkNode i1, LandmarkNode i2, int type) {
        node1 = i1;
        node2 = i2;
        this.type = type;
    }

    public String toString() {
        return node1.toString() + " -> " + node2.toString();
    }

    @Override
    public LandmarkNode getNode1() {
        return node1;
    }

    @Override
    public LandmarkNode getNode2() {
        return node2;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public boolean equals(Object x) {
        LandmarkOrdering o = (LandmarkOrdering) x;
        return node1.getIndex() == o.getNode1().getIndex() &&
                node2.getIndex() == o.getNode2().getIndex();
    }
}
