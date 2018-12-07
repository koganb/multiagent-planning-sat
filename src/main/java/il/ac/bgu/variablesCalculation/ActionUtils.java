package il.ac.bgu.variablesCalculation;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.variableModel.VariableModelFunction;
import lombok.extern.slf4j.Slf4j;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_planner.POPPrecEff;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static il.ac.bgu.dataModel.Action.State.CONDITIONS_NOT_MET;
import static il.ac.bgu.dataModel.Action.State.FAILED;
import static il.ac.bgu.dataModel.Variable.SpecialState.FREEZED;
import static il.ac.bgu.dataModel.Variable.SpecialState.LOCKED_FOR_UPDATE;
import static il.ac.bgu.variableModel.VariableModelFunction.VARIABLE_TYPE.EFFECT;
import static il.ac.bgu.variableModel.VariableModelFunction.VARIABLE_TYPE.PRECONDITION;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;


@Slf4j
public class ActionUtils {

    static ImmutablePair<Action.State, ImmutableList<FormattableValue<Variable>>> executeAction(
            Step action,
            Integer stage,
            Boolean isFailed,
            VariableModelFunction failureModelFunction,
            VariableModelFunction conflictModelFunction,
            VariableModelFunction successModelFunction,
            final ImmutableList<FormattableValue<Variable>> variablesState,
            final ImmutableList<FormattableValue<Variable>> prevStageVariableState) {


        ImmutableList<FormattableValue<Variable>> addedVariables = ImmutableList.of();
        if (stage != -1) {

            //TODO think about variable is from new Group???
            addedVariables = action.getPopEffs().stream()
                    .map(eff -> Variable.of(eff, stage))
                    .filter(var -> variablesState.stream()
                            .noneMatch(v -> v.getFormattable().formatFunctionKeyWithValue().equals(
                                    var.formatFunctionKeyWithValue())))
                    .map(var -> FormattableValue.of(var, false))
                    .collect(ImmutableList.toImmutableList());

        }

        ImmutableList<FormattableValue<Variable>> newVariablesState = ImmutableList.<FormattableValue<Variable>>builder()
                .addAll(variablesState)
                .addAll(addedVariables)
                .build();

        Action.State actionState = null;

        if (!checkPreconditionsValidity(action.getPopPrecs(), prevStageVariableState) ||
                !checkEffectsValidity(action.getPopEffs(), prevStageVariableState)) {

            actionState = CONDITIONS_NOT_MET;
            //preconditions are not valid or effects are locked
            for (POPPrecEff eff : action.getPopEffs()) {
                newVariablesState = conflictModelFunction.apply(Variable.of(eff), stage, newVariablesState, EFFECT)
                        .collect(ImmutableList.toImmutableList());
            }

        } else if (isFailed) {
            //step is failed and effects are not locked
            actionState = FAILED;

            for (POPPrecEff prec : action.getPopPrecs()) {
                newVariablesState = failureModelFunction.apply(Variable.of(prec), stage, newVariablesState, PRECONDITION)
                        .collect(ImmutableList.toImmutableList());
            }

            for (POPPrecEff eff : action.getPopEffs()) {
                newVariablesState = failureModelFunction.apply(Variable.of(eff), stage, newVariablesState, EFFECT)
                        .collect(ImmutableList.toImmutableList());
            }
        } else {
            actionState = Action.State.HEALTHY;
            for (POPPrecEff eff : action.getPopEffs()) {
                newVariablesState = successModelFunction.apply(Variable.of(eff), stage, newVariablesState, EFFECT)
                        .collect(ImmutableList.toImmutableList());
            }

        }

        return ImmutablePair.of(actionState, newVariablesState);
    }


    private static boolean checkPreconditionsValidity(List<POPPrecEff> preconditions,
                                                      ImmutableList<FormattableValue<Variable>> stageVars) {

        //check preconditions are true
        return
                preconditions.stream()
                        .allMatch(prec -> stageVars.stream()
                                .anyMatch(var -> var.getFormattable().formatFunctionKeyWithValue().equals(
                                        Variable.of(prec).formatFunctionKeyWithValue()) && var.getValue()))
                        &&
                        preconditions.stream()
                                .noneMatch(prec -> stageVars.stream()
                                        .anyMatch(var -> var.getFormattable().formatFunctionKeyWithValue().equals(
                                                Variable.of(prec).toBuilder().functionValue(FREEZED.name()).build().formatFunctionKeyWithValue())
                                                && var.getValue()));

    }


    private static boolean checkEffectsValidity(List<POPPrecEff> effects,
                                                ImmutableList<FormattableValue<Variable>> stageVars) {
        //check effects are not locked
        return effects.stream()
                .noneMatch(eff -> stageVars.stream()
                        .anyMatch(var -> var.getFormattable().formatFunctionKeyWithValue().equals(
                                Variable.of(eff).toBuilder()
                                        .functionValue(LOCKED_FOR_UPDATE.name()).build().formatFunctionKeyWithValue()) &&
                                var.getValue()
                        ));
    }


    static public boolean checkPlanContainsFailedActions(Map<Integer, Set<Step>> plan, Collection<Action> failedActions) {

        Collection<Action> allPlanActions = plan.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(action -> Action.of(action, entry.getKey(), FAILED)))
                .collect(Collectors.toList());

        Collection<Action> subtractResult = CollectionUtils.removeAll(failedActions, allPlanActions);

        if (isNotEmpty(subtractResult)) {
            log.warn("The following actions were not found in plan: {}", subtractResult);
        }

        return isEmpty(subtractResult);

    }


}
