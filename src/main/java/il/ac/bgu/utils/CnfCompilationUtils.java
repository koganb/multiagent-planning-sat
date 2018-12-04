package il.ac.bgu.utils;

import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.VariableFunctions.*;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * Created by Boris on 01/07/2017.
 */

@Slf4j
public class CnfCompilationUtils {


    public static Stream<FormattableValue<Variable>> updateVariables
            (Collection<FormattableValue<Variable>> variables, Variable variable, Integer stage) {

        Predicate<FormattableValue<Variable>> variableKeyPredicate = variableKeyFilter.apply(variable);
        Predicate<FormattableValue<Variable>> variablePredicate = variableFilter.apply(variable);

        //split variables for current and future
        Map<Boolean, List<FormattableValue<Variable>>> partitionByStage = variables.stream()
                .filter(v -> v.getFormattable().getStage().isPresent())
                .collect(Collectors.partitioningBy(v -> v.getFormattable().getStage().get() <= stage));

        Collection<FormattableValue<Variable>> currentVariables =
                partitionByStage.getOrDefault(true, Collections.emptyList());
        Collection<FormattableValue<Variable>> futureVariables =
                partitionByStage.getOrDefault(false, Collections.emptyList());


        return StreamEx.<FormattableValue<Variable>>of()
                //append true variable
                .append(FormattableValue.of(variable.toBuilder().stage(stage).build(), true))
                //append false variables
                .append(currentVariables.stream()
                        .filter(variableKeyPredicate)
                        .filter(variablePredicate.negate())
                        .collect(toMap(variableKeyWithValue, identity(), filterVariableByGreaterStage)).values().stream()
                        .map(var -> FormattableValue.of(var.getFormattable().toBuilder().stage(stage).build(), false)))
                //append rest variables of the current stage
                .append(currentVariables.stream()
                        .filter(v -> variableKeyPredicate.negate().test(v) ||
                                !v.getFormattable().getStage().get().equals(stage)))
                //append future stage variables
                .append(futureVariables);
    }


    public static Stream<FormattableValue<Variable>> calcVariableState(Stream<FormattableValue<Variable>> variables, Integer currentStage) {
        BinaryOperator<FormattableValue<Variable>> filterByGreaterStage =
                (v1, v2) -> v1.getFormattable().getStage().get() >
                        v2.getFormattable().getStage().get() ? v1 : v2;

        Collection<FormattableValue<Variable>> values = variables
                .filter(v -> v.getFormattable().getStage().isPresent())
                .filter(v -> v.getFormattable().getStage().get() <= currentStage)
                .collect(Collectors.toMap(
                        v -> v.getFormattable().formatFunctionKeyWithValue(),
                        Function.identity(),
                        filterByGreaterStage
                )).values();
        return values.stream();

    }

}
