package org.agreement_technologies.common.map_parser;

/**
 * @author Oscar
 */
public interface NumericEffect {
    static final int INCREASE = 0;

    public int getType();

    public Function getNumericVariable();

    public NumericExpression getNumericExpression();
}
