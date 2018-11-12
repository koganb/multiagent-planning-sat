package il.ac.bgu.failureModel;

import il.ac.bgu.CnfCompilationUtils;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Variable.LOCKED_FOR_UPDATE;

public class SuccessVariableModel implements VariableModelFunction {
    @Override
    public Stream<FormattableValue<Variable>> apply(
            Variable variable, Integer stage, Collection<FormattableValue<Variable>> currentVariableSet) {

        Variable lockedVariable = variable.toBuilder().functionValue(LOCKED_FOR_UPDATE).build();
        currentVariableSet = CnfCompilationUtils.updateVariables(currentVariableSet, lockedVariable, stage)
                .collect(Collectors.toList());
        return CnfCompilationUtils.updateVariables(currentVariableSet, variable, stage + 1);

    }

}
