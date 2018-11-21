package il.ac.bgu.variableModel;

import il.ac.bgu.CnfCompilationUtils;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import one.util.streamex.StreamEx;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static il.ac.bgu.VariableFunctions.variableFilter;
import static il.ac.bgu.dataModel.Variable.SpecialState.*;

public class SuccessVariableModel implements VariableModelFunction {
    @Override
    public Stream<FormattableValue<Variable>> apply(
            Variable variable, Integer stage, Collection<FormattableValue<Variable>> currentVariableSet, VARIABLE_TYPE variableType) {

        FormattableValue<Variable> lockedVariable = FormattableValue.of(
                variable.toBuilder().functionValue(LOCKED_FOR_UPDATE.name()).stage(stage + 1).build(), false);
        FormattableValue<Variable> freezedVariable = FormattableValue.of(
                variable.toBuilder().functionValue(FREEZED.name()).stage(stage + 1).build(), false);
        FormattableValue<Variable> inConflictRetryVariable = FormattableValue.of(
                variable.toBuilder().functionValue(IN_CONFLICT_RETRY.name()).stage(stage + 1).build(), false);

        Predicate<FormattableValue<Variable>> lockedVariablePredicate =
                variableFilter.apply(lockedVariable.getFormattable());
        Predicate<FormattableValue<Variable>> freezedVariablePredicate =
                variableFilter.apply(freezedVariable.getFormattable());
        Predicate<FormattableValue<Variable>> inConflictRetryVariablePredicate =
                variableFilter.apply(inConflictRetryVariable.getFormattable());

        return StreamEx.<FormattableValue<Variable>>of()
                .append(CnfCompilationUtils.updateVariables(currentVariableSet, variable, stage + 1)
                        .filter(v -> !(lockedVariablePredicate.test(v) &&
                                v.getFormattable().getStage().get().equals(stage + 1)))
                        .filter(v -> !(freezedVariablePredicate.test(v) &&
                                v.getFormattable().getStage().get().equals(stage + 1)))
                        .filter(v -> !(inConflictRetryVariablePredicate.test(v) &&
                                v.getFormattable().getStage().get().equals(stage + 1))))
                //update variable and ensure that locked and freeze variables are updated
                .append(inConflictRetryVariable)
                .append(lockedVariable)
                .append(freezedVariable);

    }

}
