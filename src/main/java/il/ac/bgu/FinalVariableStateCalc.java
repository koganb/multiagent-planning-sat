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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Variable.LOCKED_FOR_UPDATE;


public class FinalVariableStateCalc {
    private TreeMap<Integer, Set<Step>> plan;
    private VariableModelFunction failureModelFunction;
    private VariableModelFunction preconditionsNotValidModelFunction = new NoEffectFailureModel();
    private VariableModelFunction successModelFunction = new SuccessVariableModel();

    public FinalVariableStateCalc(TreeMap<Integer, Set<Step>> plan, VariableModelFunction failureModelFunction) {
        this.plan = plan;
        this.failureModelFunction = failureModelFunction;
    }


    private ImmutableList<FormattableValue<Variable>> getStageVariableState(
            ImmutableList<FormattableValue<Variable>> currentVars, Integer stage) {


        Predicate<FormattableValue<Variable>> currentStage = v -> v.getFormattable().getStage().get() <= stage;
        BinaryOperator<FormattableValue<Variable>> filterByGreaterStage = (v1, v2) -> v1.getFormattable().getStage().get() > v2.getFormattable().getStage().get() ? v1 : v2;

        return currentVars.stream()
                .filter(v -> v.getFormattable().getStage().isPresent())
                .filter(currentStage)
                .collect(Collectors.toMap(v -> v.getFormattable().formatFunctionKeyWithValue(),
                        Function.identity(), filterByGreaterStage))
                .values().stream().collect(ImmutableList.toImmutableList());
    }

    public ImmutableList<FormattableValue<Formattable>> getFinalVariableState(Set<Action> failedActions) {


        ImmutableList<FormattableValue<Variable>> currentVars = ImmutableList.of();
        for (Map.Entry<Integer, Set<Step>> stepEntry : plan.entrySet()) {
            ImmutableList<FormattableValue<Variable>> prevStageVars = getStageVariableState(currentVars, stepEntry.getKey());

            for (Step step : stepEntry.getValue()) {

                //step is failed and effects are not locked
                if (failedActions.contains(Action.of(step, stepEntry.getKey(), Action.State.FAILED)) &&
                        checkEffectsValidity(step.getPopEffs(), prevStageVars)) {
                    for (POPPrecEff eff : step.getPopEffs()) {
                        currentVars = this.failureModelFunction.apply(Variable.of(eff), stepEntry.getKey(), currentVars)
                                .collect(ImmutableList.toImmutableList());
                    }
                }

                //all preconditions valid and effects are not locked
                else if (checkPreconditionsValidity(step.getPopPrecs(), prevStageVars) &&
                        checkEffectsValidity(step.getPopEffs(), prevStageVars)) {
                    for (POPPrecEff eff : step.getPopEffs()) {
                        currentVars = successModelFunction.apply(Variable.of(eff), stepEntry.getKey(), currentVars)
                                .collect(ImmutableList.toImmutableList());
                    }
                } else {
                    //preconditions are not valid or effects are locked
                    for (POPPrecEff eff : step.getPopEffs()) {
                        currentVars = this.preconditionsNotValidModelFunction.apply(Variable.of(eff), stepEntry.getKey(), currentVars)
                                .collect(ImmutableList.toImmutableList());
                    }
                }
            }
        }
        Integer maxStep = plan.keySet().stream().max(Integer::compareTo)
                .orElseThrow(() -> new RuntimeException("No max step for plan" + plan));

        ImmutableList<FormattableValue<Variable>> finalVariableState = getStageVariableState(currentVars, maxStep + 1);

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