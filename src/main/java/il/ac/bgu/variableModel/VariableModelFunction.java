package il.ac.bgu.variableModel;

import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;

import java.util.Collection;
import java.util.stream.Stream;

@FunctionalInterface
public interface VariableModelFunction {
    Stream<FormattableValue<Variable>> apply(
            Variable variable, Integer currentStage, Collection<FormattableValue<Variable>> currentVariableSet, VARIABLE_TYPE variableType);

    Integer NEXT_STEP_ADDITION = 1;

    enum VARIABLE_TYPE {PRECONDITION, EFFECT}

    default Integer affectedStepsNumber() {
        return NEXT_STEP_ADDITION;
    }
}