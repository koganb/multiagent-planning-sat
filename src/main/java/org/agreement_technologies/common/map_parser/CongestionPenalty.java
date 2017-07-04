package org.agreement_technologies.common.map_parser;

/**
 * @author Oscar
 */
public interface CongestionPenalty {
    public static final int EQUAL = 0;
    public static final int GREATER = 1;
    public static final int GREATER_EQ = 2;
    public static final int LESS = 3;
    public static final int LESS_EQ = 4;
    public static final int DISTINCT = 5;

    public int getConditionType();

    public CongestionFluent getIncVariable();

    public double getConditionValue();

    public NumericExpression getIncExpression();

}
