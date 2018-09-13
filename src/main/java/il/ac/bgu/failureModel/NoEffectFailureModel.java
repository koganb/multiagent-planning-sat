package il.ac.bgu.failureModel;

import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;

import java.util.Collection;
import java.util.stream.Stream;

public class NoEffectFailureModel implements VariableModelFunction {
    @Override
    public Stream<FormattableValue<Variable>> apply(
            Variable variable, Collection<FormattableValue<Variable>> currentVariableSet) {

        if (currentVariableSet.stream().anyMatch(var -> var.getFormattable().formatFunctionKeyWithValue()
                .equals(variable.formatFunctionKeyWithValue()))) {
            return currentVariableSet.stream();
        }
        return Stream.concat(
                Stream.of(FormattableValue.of(variable, false)),
                currentVariableSet.stream().filter(var -> !var.getFormattable().formatFunctionKeyWithValue()
                        .equals(variable.formatFunctionKeyWithValue()))
        );
    }
}
