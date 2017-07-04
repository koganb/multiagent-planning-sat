package org.agreement_technologies.common.map_parser;

/**
 * @author Oscar
 */
public interface NumericExpression {
    static final int NUMBER = 0;
    static final int VARIABLE = 1;
    static final int ADD = 2;
    static final int DEL = 3;
    static final int PROD = 4;
    static final int DIV = 5;
    static final int USAGE = 6;

    public int getType();

    public double getValue();

    public Function getNumericVariable();

    public NumericExpression getLeftExp();

    public NumericExpression getRightExp();

    public CongestionFluent getCongestionFluent();

}
