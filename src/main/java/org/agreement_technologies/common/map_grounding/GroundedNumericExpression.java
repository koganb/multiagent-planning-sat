package org.agreement_technologies.common.map_grounding;

/**
 * @author Oscar
 */
public interface GroundedNumericExpression {
    static final int NUMBER = 0;
    static final int VARIABLE = 1;
    static final int ADD = 2;
    static final int DEL = 3;
    static final int PROD = 4;
    static final int DIV = 5;
    static final int USAGE = 6;

    int getType();

    double getValue();      // if type == NUMBER

    GroundedCongestionFluent getFluent();   // if type == VARIABLE

    GroundedVar getVariable();  // if type == VARIABLE

    GroundedNumericExpression getLeftOperand(); // if type == ADD, DEL, PROD or DIV

    GroundedNumericExpression getRightOperand(); // if type == ADD, DEL, PROD or DIV

}
