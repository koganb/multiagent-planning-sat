package il.ac.bgu;

import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

import java.util.Collection;
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

        //add false and true variables
        return StreamEx.of(FormattableValue.of(variable.toBuilder().stage(stage).build(), true))  //true variable
                .append(variables.stream()   //false variables
                        .filter(variableKeyPredicate)
                        .filter(variablePredicate.negate())
                        .collect(toMap(variableKeyWithValue, identity(), filterVariableByGreaterStage)).values().stream()
                        .map(var -> FormattableValue.of(var.getFormattable().toBuilder().stage(stage).build(), false)))
                .append(variables.stream());
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
