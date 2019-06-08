package il.ac.bgu.variablesCalculation;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.plan.PlanAction;
import il.ac.bgu.variableModel.VariableModelFunction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    static ImmutablePair<Action.State, List<FormattableValue<Variable>>> executeAction(
            PlanAction action,
            Integer stage,
            Boolean isFailed,
            VariableModelFunction failureModelFunction,
            VariableModelFunction conflictModelFunction,
            VariableModelFunction successModelFunction,
            final List<FormattableValue<Variable>> variablesState,
            final List<FormattableValue<Variable>> prevStageVariableState) {


        List<FormattableValue<Variable>> newVariablesState = variablesState;

        Action.State actionState;

        if (!checkPreconditionsValidity(action.getPreconditions(), prevStageVariableState) ||
                !checkEffectsValidity(action.getEffects(), prevStageVariableState)) {

            actionState = CONDITIONS_NOT_MET;
            //preconditions are not valid or effects are locked
            for (Variable eff : action.getEffects()) {
                newVariablesState = conflictModelFunction.apply(eff, stage, newVariablesState, EFFECT)
                        .collect(ImmutableList.toImmutableList());
            }

        } else if (isFailed) {
            //step is failed and effects are not locked
            actionState = FAILED;

            for (Variable prec : action.getPreconditions()) {
                newVariablesState = failureModelFunction.apply(prec, stage, newVariablesState, PRECONDITION)
                        .collect(ImmutableList.toImmutableList());
            }

            for (Variable eff : action.getEffects()) {
                newVariablesState = failureModelFunction.apply(eff, stage, newVariablesState, EFFECT)
                        .collect(ImmutableList.toImmutableList());
            }
        } else {
            actionState = Action.State.HEALTHY;
            for (Variable eff : action.getEffects()) {
                newVariablesState = successModelFunction.apply(eff, stage, newVariablesState, EFFECT)
                        .collect(ImmutableList.toImmutableList());
            }

        }

        return ImmutablePair.of(actionState, newVariablesState);
    }


    private static boolean checkPreconditionsValidity(List<Variable> preconditions,
                                                      List<FormattableValue<Variable>> stageVars) {

        //check preconditions are true
        boolean preconditionsValid = preconditions.stream()
                .allMatch(prec -> stageVars.stream()
                        .anyMatch(var -> var.getFormattable().formatFunctionKeyWithValue().equals(
                                prec.formatFunctionKeyWithValue()) && var.getValue()));
        boolean preconditionsNotFreezed = preconditions.stream()
                .noneMatch(prec -> stageVars.stream()
                        .anyMatch(var -> var.getFormattable().formatFunctionKeyWithValue().equals(
                                prec.toBuilder().functionValue(FREEZED.name()).build().formatFunctionKeyWithValue())
                                && var.getValue()));
        return preconditionsValid && preconditionsNotFreezed;

    }


    private static boolean checkEffectsValidity(List<Variable> effects, List<FormattableValue<Variable>> stageVars) {
        //check effects are not locked
        return effects.stream()
                .noneMatch(eff -> stageVars.stream()
                        .anyMatch(var -> var.getFormattable().formatFunctionKeyWithValue().equals(
                                eff.toBuilder()
                                        .functionValue(LOCKED_FOR_UPDATE.name()).build().formatFunctionKeyWithValue()) &&
                                var.getValue()
                        ));
    }


    static public boolean checkPlanContainsFailedActions(Map<Integer, List<PlanAction>> plan, Collection<Action> failedActions) {

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
