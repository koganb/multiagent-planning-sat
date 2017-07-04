package org.agreement_technologies.service.map_planner;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author Alex
 */
public class MessageContentEncodedVarsValues implements Serializable {
    ArrayList<ArrayList<GlobalIndexVarValueInfo>> globalIndexes;
    private int currentGlobalIndexVars;
    private int currentGlobalIndexValues;

    public MessageContentEncodedVarsValues(ArrayList<ArrayList<GlobalIndexVarValueInfo>> ids, int iv, int ival) {
        globalIndexes = ids;
        currentGlobalIndexVars = iv;
        currentGlobalIndexValues = ival;
    }

    public ArrayList<ArrayList<GlobalIndexVarValueInfo>> getGlobalIndexes() {
        return globalIndexes;
    }

    public int getCurrentGlobalIndexVars() {
        return currentGlobalIndexVars;
    }

    public int getCurrentGlobalIndexValues() {
        return currentGlobalIndexValues;
    }
}
