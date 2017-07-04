package org.agreement_technologies.common.map_parser;

/**
 * @author Oscar
 */
public interface CongestionUsage {
    public static final int OR = 0;
    public static final int AND = 1;
    public static final int ACTION = 2;

    public int getType();

    public int numTerms();  // if type == OR or type == AND

    public CongestionUsage getTerm(int index);  // 0 <= index < numTerms()

    public String getActionName();  // if type == ACTION

    public int numActionParams();

    public String getParamName(int paramNumber);    // 0 <= paramNumber < numActionParams()
}
