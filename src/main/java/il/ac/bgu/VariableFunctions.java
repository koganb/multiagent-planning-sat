package il.ac.bgu;

import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;

import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

public class VariableFunctions {
    public static final BinaryOperator<FormattableValue<Variable>> filterVariableByGreaterStage =
            (v1, v2) ->
                    v1.getFormattable().getStage().orElse(Integer.MAX_VALUE) >
                            v2.getFormattable().getStage().orElse(Integer.MAX_VALUE) ? v1 : v2;

    public static final Function<Variable, Predicate<FormattableValue<Variable>>> variableFilter =
            variable -> var -> var.getFormattable().formatFunctionKeyWithValue()
                    .equals(variable.formatFunctionKeyWithValue());

    public static final Function<Variable, Predicate<FormattableValue<Variable>>> variableKeyFilter =
            variable -> var -> var.getFormattable().formatFunctionKey().equals(variable.formatFunctionKey());

}
