package il.ac.bgu.failureModel;

import il.ac.bgu.CnfCompilationUtils;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;

import java.util.Collection;
import java.util.stream.Stream;

public class SuccessVariableModel implements VariableModelFunction {
    @Override
    public Stream<FormattableValue<Variable>> apply(
            Variable variable, Collection<FormattableValue<Variable>> currentVariableSet) {
        return CnfCompilationUtils.updateVariables(currentVariableSet, FormattableValue.of(variable, true), variable.getStage().get());

    }

}