package il.ac.bgu.failureModel;

import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;

import java.util.Collection;
import java.util.stream.Stream;

@FunctionalInterface
public interface NewVariableModelFunction {
    Stream<FormattableValue<Variable>> apply(
            Variable variable, Collection<FormattableValue<Variable>> currentVariableSet);
}