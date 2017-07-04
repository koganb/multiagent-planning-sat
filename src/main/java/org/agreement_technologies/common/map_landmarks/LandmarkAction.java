package org.agreement_technologies.common.map_landmarks;

import java.util.ArrayList;

public interface LandmarkAction {

    ArrayList<LandmarkFluent> getEffects();

    ArrayList<LandmarkFluent> getPreconditions();

    int getLevel();

    void setLevel(int maxLevel);

    String getName();

}
