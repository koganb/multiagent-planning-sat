package org.agreement_technologies.common.map_grounding;

/**
 * @author Oscar
 */
public interface GroundedCongestionFluent {

    public String getName();

    public int getNumParameters();

    public boolean parameterIsVariable(int paramIndex);     // 0 <= paramIndex < getNumParameters()

    public String[] parameterTypes(int paramIndex);         // if parameterIsVariable(paramIndex)

    public String parameterObject(int paramIndex);          // if !parameterIsVariable(paramIndex)

}
