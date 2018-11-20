package il.ac.bgu;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.variableModel.NoEffectVariableFailureModel;
import il.ac.bgu.variableModel.SuccessVariableModel;
import il.ac.bgu.variableModel.VariableModelFunction;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_planner.POPPrecEff;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static il.ac.bgu.dataModel.Variable.SpecialState.FREEZED;
import static il.ac.bgu.dataModel.Variable.SpecialState.LOCKED_FOR_UPDATE;
import static il.ac.bgu.variableModel.VariableModelFunction.VARIABLE_TYPE.EFFECT;
import static il.ac.bgu.variableModel.VariableModelFunction.VARIABLE_TYPE.PRECONDITION;


public class FinalVariableStateCalc {
    private TreeMap<Integer, Set<Step>> plan;
    private VariableModelFunction failureModelFunction;
    private VariableModelFunction preconditionsNotValidModelFunction = new NoEffectVariableFailureModel();
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

                if (stepEntry.getKey() != -1) {

                    //TODO think about variable is from new Group???
                    ImmutableList<FormattableValue<Variable>> finalCurrentVars = currentVars;
                    ImmutableList<FormattableValue<Variable>> addedVariables = step.getPopEffs().stream()
                            .map(eff -> Variable.of(eff, stepEntry.getKey()))
                            .filter(var -> finalCurrentVars.stream()
                                    .noneMatch(v -> v.getFormattable().formatFunctionKeyWithValue().equals(
                                            var.formatFunctionKeyWithValue())))
                            .map(var -> FormattableValue.of(var, false))
                            .collect(ImmutableList.toImmutableList());

                    currentVars = ImmutableList.<FormattableValue<Variable>>builder()
                            .addAll(currentVars)
                            .addAll(addedVariables)
                            .build();
                }

                //step is failed and effects are not locked
                String actionKey = Action.of(step, stepEntry.getKey()).formatFunctionKey();
                if (failedActionsKeys.contains(actionKey) && checkEffectsValidity(step.getPopEffs(), prevStageVars)) {

                    //remove key from failedActionKeys
                    failedActionsKeys.remove(actionKey);

                    for (POPPrecEff prec : step.getPopPrecs()) {
                        currentVars = this.failureModelFunction.apply(Variable.of(prec), stepEntry.getKey(), currentVars, PRECONDITION)
                                .collect(ImmutableList.toImmutableList());
                    }

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


    private boolean checkEffectsValidity(List<POPPrecEff> effects,
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
}