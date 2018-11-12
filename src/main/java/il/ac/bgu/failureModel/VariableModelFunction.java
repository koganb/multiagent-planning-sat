package il.ac.bgu.failureModel;

import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;

import java.util.Collection;
import java.util.stream.Stream;

@FunctionalInterface
public interface VariableModelFunction {
    Integer NEXT_STEP_ADDITION = 1;

    Stream<FormattableValue<Variable>> apply(
            Variable variable, Integer currentStage, Collection<FormattableValue<Variable>> currentVariableSet);

    default Integer affectedStepsNumber() {
        return NEXT_STEP_ADDITION;
    }
}