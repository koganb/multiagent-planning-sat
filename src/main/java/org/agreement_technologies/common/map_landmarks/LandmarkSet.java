package org.agreement_technologies.common.map_landmarks;

import java.util.ArrayList;

public interface LandmarkSet extends Comparable<LandmarkSet> {

    ArrayList<LandmarkFluent> getElements();

    String identify();

    void setLGNode(LandmarkNode newNode);

    void addElement(LandmarkFluent fluent);

    LandmarkNode getLTNode();

    void calculateValue();

    boolean match(LandmarkFluent p);

}
