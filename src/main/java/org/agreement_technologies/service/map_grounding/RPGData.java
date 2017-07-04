package org.agreement_technologies.service.map_grounding;

import org.agreement_technologies.common.map_grounding.GroundedTask;
import org.agreement_technologies.common.map_grounding.ReachedValue;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Class for RPG data communication
 *
 * @author Oscar
 * @since May 2011
 */
public class RPGData implements Serializable {
    private static final long serialVersionUID = -1957180289257722567L;
    String varName;
    String params[];
    String paramTypes[][];
    String value;
    String valueTypes[];
    int minTime[];

    /**
     * Initializes the data from the grounded task value
     *
     * @param v            Value to communicate
     * @param groundedTask Grounded task
     */
    public RPGData(ReachedValue v, GroundedTask gTask, ArrayList<String> agents) {
        varName = v.getVar().getFuctionName();
        params = v.getVar().getParams();
        paramTypes = new String[params.length][];
        for (int i = 0; i < params.length; i++)
            paramTypes[i] = v.getVar().getParamTypes(i);
        value = v.getValue();
        valueTypes = gTask.getObjectTypes(value);
        minTime = new int[agents.size()];
        for (int i = 0; i < minTime.length; i++)
            minTime[i] = v.getVar().getMinTime(value, agents.get(i));
    }

    /**
     * Returns a description of this data
     */
    public String toString() {
        String res = "(= (" + varName;
        for (String p : params) res += " " + p;
        res += ") " + value + ")[" + (minTime[0] == -1 ? "-" : minTime[0]);
        for (int i = 1; i < minTime.length; i++)
            res += "," + (minTime[i] == -1 ? "-" : minTime[i]);
        return res + "]";
    }
}
