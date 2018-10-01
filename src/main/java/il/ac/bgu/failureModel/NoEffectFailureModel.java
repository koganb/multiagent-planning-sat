package il.ac.bgu.failureModel;

import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.VariableFunctions.*;
import static java.util.function.Function.identity;

public class NoEffectFailureModel implements VariableModelFunction {


    @Override
    public Stream<FormattableValue<Variable>> apply(
            Variable variable, Integer stage, Collection<FormattableValue<Variable>> currentVariableSet) {

        Predicate<FormattableValue<Variable>> variablePredicate = variableFilter.apply(variable);
        Predicate<FormattableValue<Variable>> variableKeyPredicate = variableKeyFilter.apply(variable);

        FormattableValue<Variable> effectedVariable = currentVariableSet.stream()
                .filter(variablePredicate)
                .findFirst()
                .orElse(FormattableValue.of(variable, false));

        //add effected variable if not exists and partition current state by variable key
        Map<Boolean, List<FormattableValue<Variable>>> partitionByVariableKey =
                Stream.concat(Stream.of(effectedVariable),
                        currentVariableSet.stream().filter(variablePredicate.negate()))
                        .collect(Collectors.partitioningBy(variableKeyPredicate));

        //update stage on effected variable
        Stream<FormattableValue<Variable>> effectedVariablesStream = partitionByVariableKey.getOrDefault(Boolean.TRUE, Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(v -> v.getFormattable().formatFunctionKeyWithValue(), identity(), filterVariableByGreaterStage))
                .values().stream()
                .map(v -> FormattableValue.of(v.getFormattable().toBuilder().stage(stage + 1).build(), v.getValue()));

        Stream<FormattableValue<Variable>> notEffectedVariables = partitionByVariableKey.getOrDefault(Boolean.FALSE, Collections.emptyList())
                .stream();

        List<FormattableValue<Variable>> result = Stream.concat(effectedVariablesStream, notEffectedVariables).collect(Collectors.toList());
        return result.stream();

    }
}
