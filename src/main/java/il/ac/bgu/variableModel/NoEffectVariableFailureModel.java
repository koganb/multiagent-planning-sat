package il.ac.bgu.variableModel;

import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;

import java.util.Collection;
import java.util.stream.Stream;

public class NoEffectVariableFailureModel implements VariableModelFunction {


    @Override
    public Stream<FormattableValue<Variable>> apply(
            Variable variable, Integer currentStage, Collection<FormattableValue<Variable>> currentVariableSet, VARIABLE_TYPE variableType) {
        return currentVariableSet.stream();
    }
}
