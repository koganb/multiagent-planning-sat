package org.agreement_technologies.common.map_grounding;

/**
 * @author Oscar
 */
public interface GroundedCongestionUsage {
    public static final int OR = 0;
    public static final int AND = 1;
    public static final int ACTION = 2;

    public int getType();

    public int getNumTerms();   // if getType() == OR or getType() == AND

    public GroundedCongestionUsage getTerm(int termNumber); // 0 <= termNumber < getNumTerms()

    public String getActionName();  // if getType() == ACTION

    public int getNumActionParameters();

    public boolean actionParameterIsVariable(int paramIndex);   // 0 <= paramIndex < getNumActionParameters()

    public String[] actionParameterTypes(int paramIndex);   // if actionParameterIsVariable(paramIndex)

    public String actionParameterObject(int paramIndex);    // if !actionParameterIsVariable(paramIndex)

}
