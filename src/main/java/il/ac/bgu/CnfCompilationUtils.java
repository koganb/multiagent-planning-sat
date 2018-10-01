package il.ac.bgu;

import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Boris on 01/07/2017.
 */

@Slf4j
public class CnfCompilationUtils {


    public static Stream<FormattableValue<Variable>> updateVariables
            (Collection<FormattableValue<Variable>> variables, Variable trueVariable, Integer stage) {

        Map<Boolean, List<FormattableValue<Variable>>> splitByFunctionKey = variables.stream()
                .collect(Collectors.partitioningBy(var -> var.getFormattable().formatFunctionKey()
                        .equals(trueVariable.formatFunctionKey())));

        return Stream.concat(
                Stream.concat(
                        Stream.of(FormattableValue.of(trueVariable.toBuilder().stage(stage).build(), true)),  //true variable
                        splitByFunctionKey.get(Boolean.TRUE).stream()   //false variables
                                .filter(var -> !var.getFormattable().formatFunctionKeyWithValue()
                                        .equals(trueVariable.formatFunctionKeyWithValue()))
                                .map(var -> FormattableValue.of(var.getFormattable().toBuilder().stage(stage).build(), false))),
                splitByFunctionKey.get(Boolean.FALSE).stream()  //not effected variables
        );

    }


    public static Stream<FormattableValue<Variable>> updateVariableSet(
            Stream<FormattableValue<Variable>> oldVariables, Stream<FormattableValue<Variable>> newVariables) {

        Map<String, FormattableValue<Variable>> variablesMap = oldVariables
                .collect(Collectors.toMap(t -> t.getFormattable().formatFunctionKeyWithValue(), Function.identity()));

        newVariables
                .forEach(var -> variablesMap.put(var.getFormattable().formatFunctionKeyWithValue(), var));

        return variablesMap.values().stream();
    }
}
