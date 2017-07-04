package org.agreement_technologies.service.map_heuristic;

public class GoalCondition implements java.io.Serializable {
    private static final long serialVersionUID = -5772521153532509020L;

    public String varName;
    public String value;

    public GoalCondition(String varName, String value) {
        this.varName = varName;
        this.value = value;
    }

    @Override
    public boolean equals(Object x) {
        return varName.equals(((GoalCondition) x).varName) &&
                value.equals(((GoalCondition) x).value);
    }

    @Override
    public String toString() {
        return varName + "=" + value;
    }
}
