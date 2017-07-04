package org.agreement_technologies.common.map_dtg;

import java.util.ArrayList;
import java.util.HashMap;

public interface DTG {
    static final int INFINITE = (Integer.MAX_VALUE) / 3;

    int pathCost(String initValue, String endValue, HashMap<String, String> state,
                 HashMap<String, ArrayList<String>> newValues, int threadIndex);

    int pathCostMulti(String initValue, String endValue);

    String[] getPathMulti(String initValue, String endValue);

    DTGTransition getTransition(String initValue, String endValue);

    String getVarName();

    String[] getPath(String initValue, String endValue, HashMap<String, String> state,
                     HashMap<String, ArrayList<String>> newValues, int threadIndex);

    DTGTransition[] getTransitionsFrom(String fromValue);

    DTGTransition[] getTransitionsTo(String endValue);

    boolean unknownValue(String value);

    int getDistance(String initValue, String endValue, int threadIndex);

    void clearCache(int Hashtable);
}
