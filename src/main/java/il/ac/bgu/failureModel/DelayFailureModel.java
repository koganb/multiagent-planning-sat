package il.ac.bgu.failureModel;

import com.google.common.collect.ImmutableSet;
import il.ac.bgu.CnfCompilationUtils;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;

import java.util.Collection;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Variable.UNDEFINED;

public class DelayFailureModel implements VariableModelFunction {

    private int numberOfDelayedStages;
    private VariableModelFunction successVariableModel;

    public DelayFailureModel(int numberOfDelayedStages, VariableModelFunction successVariableModel) {
        this.numberOfDelayedStages = numberOfDelayedStages;
        this.successVariableModel = successVariableModel;
    }

    public DelayFailureModel() {
        this.successVariableModel = new SuccessVariableModel();
    }

    @Override
    public Stream<FormattableValue<Variable>> apply(
            Variable variable, Collection<FormattableValue<Variable>> currentVariableSet) {

        if (!variable.getStage().isPresent()) {
            throw new IllegalArgumentException("No variable stage is set for variable: " + variable);
        }

        Integer variableStage = variable.getStage().get();
        return Stream.concat(
                CnfCompilationUtils.updateVariables(ImmutableSet.<FormattableValue<Variable>>builder()
                                .addAll(currentVariableSet)
                                .add(FormattableValue.of(variable, false)).build(),
                        FormattableValue.of(variable.toBuilder()
                                .stage(variableStage + 1)
                                .functionValue(UNDEFINED).build(), true), variableStage + 1),
                successVariableModel.apply(variable.toBuilder()
                        .stage(variableStage + 2).build(), currentVariableSet));
    }
}
