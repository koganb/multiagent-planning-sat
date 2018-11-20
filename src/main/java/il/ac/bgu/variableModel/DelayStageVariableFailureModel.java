package il.ac.bgu.variableModel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import il.ac.bgu.CnfCompilationUtils;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;

import java.util.Collection;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Variable.FREEZED;
import static il.ac.bgu.dataModel.Variable.LOCKED_FOR_UPDATE;
import static il.ac.bgu.variableModel.VariableModelFunction.VARIABLE_TYPE.EFFECT;
import static il.ac.bgu.variableModel.VariableModelFunction.VARIABLE_TYPE.PRECONDITION;

public class DelayStageVariableFailureModel implements VariableModelFunction {
    private Integer numberOfStagesToDelay;

    public DelayStageVariableFailureModel(Integer numberOfStagesToDelay) {
        this.numberOfStagesToDelay = numberOfStagesToDelay;
    }

    @Override
    public Stream<FormattableValue<Variable>> apply(Variable variable, Integer currentStage,
                                                    Collection<FormattableValue<Variable>> currentVars, VARIABLE_TYPE variableType) {

        int targetStage = currentStage + numberOfStagesToDelay + NEXT_STEP_ADDITION;

        if (variableType == EFFECT) {

            for (int stage = currentStage + NEXT_STEP_ADDITION; stage < targetStage; stage++) {
                currentVars = CnfCompilationUtils.updateVariables(currentVars,
                        variable.toBuilder()
                                .functionValue(LOCKED_FOR_UPDATE)
                                .build(), stage)
                        .collect(ImmutableList.toImmutableList());
            }
            currentVars = CnfCompilationUtils.updateVariables(currentVars, variable, targetStage)
                    .collect(ImmutableList.toImmutableList());

        } else if (variableType == PRECONDITION) {
            Builder<FormattableValue<Variable>> currentStateBuilder =
                    ImmutableList.<FormattableValue<Variable>>builder().addAll(currentVars);
            for (int stage = currentStage + NEXT_STEP_ADDITION; stage < targetStage; stage++) {

                //add freeze variable to the next stage
                currentStateBuilder.add(FormattableValue.of(
                        variable.toBuilder().functionValue(FREEZED).stage(stage).build(), true));
            }
            currentStateBuilder.add(FormattableValue.of(
                    variable.toBuilder().functionValue(FREEZED).stage(targetStage).build(), false));

            currentVars = currentStateBuilder.build();
        } else {
            throw new RuntimeException("Unsupported variableType " + variableType);
        }

        return currentVars.stream();
    }

    @Override
    public Integer affectedStepsNumber() {
        return NEXT_STEP_ADDITION + numberOfStagesToDelay;
    }
}
