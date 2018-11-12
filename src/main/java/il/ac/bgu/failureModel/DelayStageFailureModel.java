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

        ImmutableList<FormattableValue<Variable>> currentVars =
                ImmutableList.<FormattableValue<Variable>>builder()
                        .addAll(variableSet)
                        .add(FormattableValue.of(variable, false)) //add variable to the state in case it doesn't exist
                        .build();
        int targetStage = currentStage + numberOfStagesToDelay + NEXT_STEP_ADDITION;
        for (int stage = currentStage + NEXT_STEP_ADDITION; stage < targetStage; stage++) {
            currentVars = CnfCompilationUtils.updateVariables(currentVars,
                    variable.toBuilder()
                            .functionValue(LOCKED_FOR_UPDATE)
                            .build(), stage)
                    .collect(ImmutableList.toImmutableList());
        }
        currentVars = CnfCompilationUtils.updateVariables(currentVars, variable, targetStage)
                .collect(ImmutableList.toImmutableList());

        return currentVars.stream();
    }

    @Override
    public Integer affectedStepsNumber() {
        return NEXT_STEP_ADDITION + numberOfStagesToDelay;
    }
}
