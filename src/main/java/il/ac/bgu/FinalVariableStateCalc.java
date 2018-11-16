package il.ac.bgu;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.failureModel.NoEffectFailureModel;
import il.ac.bgu.failureModel.SuccessVariableModel;
import il.ac.bgu.failureModel.VariableModelFunction;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_planner.POPPrecEff;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Variable.LOCKED_FOR_UPDATE;
import static il.ac.bgu.failureModel.VariableModelFunction.VARIABLE_TYPE.EFFECT;


public class FinalVariableStateCalc {
    private TreeMap<Integer, Set<Step>> plan;
    private VariableModelFunction failureModelFunction;
    private VariableModelFunction preconditionsNotValidModelFunction = new NoEffectFailureModel();
    private VariableModelFunction successModelFunction = new SuccessVariableModel();

    public FinalVariableStateCalc(TreeMap<Integer, Set<Step>> plan, VariableModelFunction failureModelFunction) {
        this.plan = plan;
        this.failureModelFunction = failureModelFunction;
    }

    public ImmutableList<FormattableValue<Formattable>> getFinalVariableState(Collection<Action> failedActions) {

        Set<String> failedActionsKeys = failedActions.stream()
                .map(Action::formatFunctionKey)
                .collect(Collectors.toSet());

        ImmutableList<FormattableValue<Variable>> currentVars = ImmutableList.of();


        for (Map.Entry<Integer, Set<Step>> stepEntry : plan.entrySet()) {


            ImmutableList<FormattableValue<Variable>> prevStageVars =
                    CnfCompilationUtils.calcVariableState(currentVars.stream(), stepEntry.getKey())
                            .collect(ImmutableList.toImmutableList());

            for (Step step : stepEntry.getValue()) {

                //step is failed and effects are not locked
                String actionKey = Action.of(step, stepEntry.getKey()).formatFunctionKey();
                if (failedActionsKeys.contains(actionKey) && checkEffectsValidity(step.getPopEffs(), prevStageVars)) {

                    //remove key from failedActionKeys
                    failedActionsKeys.remove(actionKey);

                    for (POPPrecEff eff : step.getPopEffs()) {
                        currentVars = this.failureModelFunction.apply(Variable.of(eff), stepEntry.getKey(), currentVars, EFFECT)
                                .collect(ImmutableList.toImmutableList());
                    }
                }

                //all preconditions valid and effects are not locked
                else if (checkPreconditionsValidity(step.getPopPrecs(), prevStageVars) &&
                        checkEffectsValidity(step.getPopEffs(), prevStageVars)) {
                    for (POPPrecEff eff : step.getPopEffs()) {
                        currentVars = successModelFunction.apply(Variable.of(eff), stepEntry.getKey(), currentVars, EFFECT)
                                .collect(ImmutableList.toImmutableList());
                    }
                } else {
                    //preconditions are not valid or effects are locked
                    for (POPPrecEff eff : step.getPopEffs()) {
                        currentVars = this.preconditionsNotValidModelFunction.apply(Variable.of(eff), stepEntry.getKey(), currentVars, EFFECT)
                                .collect(ImmutableList.toImmutableList());
                    }
                }
            }
        }
        Integer maxStep = plan.keySet().stream().max(Integer::compareTo)
                .orElseThrow(() -> new RuntimeException("No max step for plan" + plan));

        ImmutableList<FormattableValue<Variable>> finalVariableState = CnfCompilationUtils.calcVariableState(
                currentVars.stream(), maxStep + 1).collect(ImmutableList.toImmutableList());

        //add LOCKED_FOR_UPDATE variables to the final state
        Set<String> variablesWithUndefinedValues = finalVariableState.stream()
                .filter(v -> LOCKED_FOR_UPDATE.equals(v.getFormattable().getValue()))
                .map(v -> v.getFormattable().formatFunctionKey())
                .collect(Collectors.toSet());
        finalVariableState = Stream.concat(
                finalVariableState.stream(),
                finalVariableState.stream()
                        .collect(Collectors.groupingBy(t -> t.getFormattable().formatFunctionKey()))
                        .entrySet().stream()
                        .filter(entry -> !variablesWithUndefinedValues.contains(entry.getKey()))
                        .map(entry -> FormattableValue.of(
                                entry.getValue().get(0).getFormattable().toBuilder().functionValue(LOCKED_FOR_UPDATE).build(), false)))
                .collect(ImmutableList.toImmutableList());

        if (CollectionUtils.isNotEmpty(failedActionsKeys)) {
            throw new RuntimeException("Failed actions not found: " + failedActionsKeys);
        }

        return finalVariableState.stream()
                .map(formattableValue -> FormattableValue.<Formattable>of(
                        formattableValue.getFormattable().toBuilder().stage(maxStep + 1).build(),
                        formattableValue.getValue()))
                .collect(ImmutableList.toImmutableList());
    }


    private boolean checkPreconditionsValidity(List<POPPrecEff> preconditions,
                                               ImmutableList<FormattableValue<Variable>> stageVars) {

        //check preconditions are true
        return preconditions.stream()
                .allMatch(eff -> stageVars.stream()
                        .filter(var -> var.getFormattable().formatFunctionKeyWithValue().equals(
                                Variable.of(eff).formatFunctionKeyWithValue()))
                        .findFirst()
                        .map(FormattableValue::getValue)
                        .orElse(false));
    }


    private boolean checkEffectsValidity(List<POPPrecEff> effects,
                                         ImmutableList<FormattableValue<Variable>> stageVars) {
        //check effects are not locked
        return effects.stream()
                .noneMatch(eff -> stageVars.stream()
                        .filter(var -> var.getFormattable().formatFunctionKey().equals(
                                Variable.of(eff).formatFunctionKey()))
                        .filter(var -> var.getFormattable().getValue().equals(LOCKED_FOR_UPDATE))
                        .findFirst()
                        .map(FormattableValue::getValue)
                        .orElse(false));
    }
}