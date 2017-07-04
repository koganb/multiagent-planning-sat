package org.agreement_technologies.common.map_parser;

/**
 * @author Oscar
 */
public interface CongestionFluent {

    public String getName();

    public int getNumParams();

    public String getParamName(int index);  // 0 <= index < getNumParams()

}
