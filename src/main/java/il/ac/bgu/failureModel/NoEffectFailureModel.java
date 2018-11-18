package il.ac.bgu.failureModel;

import il.ac.bgu.CnfCompilationUtils;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static il.ac.bgu.VariableFunctions.variableFilter;
import static il.ac.bgu.failureModel.VariableModelFunction.VARIABLE_TYPE.EFFECT;
import static il.ac.bgu.failureModel.VariableModelFunction.VARIABLE_TYPE.PRECONDITION;

public class NoEffectFailureModel implements VariableModelFunction {


    @Override
    public Stream<FormattableValue<Variable>> apply(
            Variable variable, Integer currentStage, Collection<FormattableValue<Variable>> currentVariableSet, VARIABLE_TYPE variableType) {

        if (variableType == EFFECT) {

            Predicate<FormattableValue<Variable>> variablePredicate = variableFilter.apply(variable);

            FormattableValue<Variable> effectedVariable = currentVariableSet.stream()
                    .filter(variablePredicate)
                    .findFirst()
                    .orElse(FormattableValue.of(variable, false));

            FormattableValue<Variable> updatedVariable = FormattableValue.of(
                    effectedVariable.getFormattable().toBuilder().stage(currentStage + NEXT_STEP_ADDITION).build(),
                    effectedVariable.getValue());

            return CnfCompilationUtils.calcVariableState(Stream.concat(
                    Stream.of(updatedVariable),
                    currentVariableSet.stream()), currentStage + NEXT_STEP_ADDITION);
        } else if (variableType == PRECONDITION) {
            return currentVariableSet.stream();
        } else {
            throw new RuntimeException("No such variableType: " + variableType);
        }

    }
}
