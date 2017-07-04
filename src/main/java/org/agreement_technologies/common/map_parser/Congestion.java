package org.agreement_technologies.common.map_parser;

/**
 * @author Oscar
 */
public interface Congestion {

    public String getName();

    public int getNumParams();

    public String[] getParamTypes(int paramNumber); // 0 <= paramNumber < getNumParams()

    public String[] getVariableNames();

    public String[] getVarTypes(int varNumber);    // 0 <= varNumber < getVariableNames().size()

    public CongestionUsage getUsage();

    public int getParamIndex(String paramName);

    public int getNumPenalties();

    public CongestionPenalty getPenalty(int index); // 0 <= index < getNumPenalties()

}
