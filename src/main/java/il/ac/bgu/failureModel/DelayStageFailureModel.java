package il.ac.bgu.failureModel;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.CnfCompilationUtils;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;

import java.util.Collection;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Variable.LOCKED_FOR_UPDATE;

public class DelayStageFailureModel implements VariableModelFunction {
    private Integer numberOfStagesToDelay;

    public DelayStageFailureModel(Integer numberOfStagesToDelay) {
        this.numberOfStagesToDelay = numberOfStagesToDelay;
    }

    @Override
    public Stream<FormattableValue<Variable>> apply(Variable variable, Integer currentStage,
                                                    Collection<FormattableValue<Variable>> variableSet) {

        ImmutableList<FormattableValue<Variable>> currentVars = ImmutableList.copyOf(variableSet);
        int targetStage = currentStage + numberOfStagesToDelay + 1;
        for (int stage = currentStage + 1; stage < targetStage; stage++) {
            currentVars = CnfCompilationUtils.updateVariables(currentVars,
                    variable.toBuilder()
                            .functionValue(LOCKED_FOR_UPDATE)
                            .build(), stage)
                    .collect(ImmutableList.toImmutableList());
        }
        return CnfCompilationUtils.updateVariables(currentVars, variable, targetStage);
    }
}
