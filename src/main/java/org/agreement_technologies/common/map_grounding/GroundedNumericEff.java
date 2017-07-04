package org.agreement_technologies.common.map_grounding;

/**
 * @author Oscar
 */
public interface GroundedNumericEff {
    static final int INCREASE = 0;

    int getType();

    GroundedVar getVariable();

    GroundedNumericExpression getExpression();
}
