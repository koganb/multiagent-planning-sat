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

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
        return currentVars.stream()
                .filter(v -> v.getFormattable().getStage().isPresent())
                .filter(v -> v.getFormattable().getStage().get() <= stage)
                .collect(Collectors.toMap(v -> v.getFormattable().formatFunctionKeyWithValue(),
                        Function.identity(),
                        (v1, v2) -> v1.getFormattable().getStage().get() > v2.getFormattable().getStage().get() ? v1 : v2))
                .values().stream().collect(ImmutableList.toImmutableList());


    }

    public ImmutableList<FormattableValue<Formattable>> getFinalVariableState(Set<Action> failedActions) {

        Set<String> failedActionKeys = failedActions.stream().map(Action::formatData).collect(Collectors.toSet());

        ImmutableList<FormattableValue<Variable>> currentVars = ImmutableList.of();
        for (Map.Entry<Integer, Set<Step>> stepEntry : plan.entrySet()) {
            ImmutableList<FormattableValue<Variable>> prevStageVars = getStageVariableState(currentVars, stepEntry.getKey());

            for (Step step : stepEntry.getValue()) {
                //step is failed
                if (failedActionKeys.contains(Action.of(step, stepEntry.getKey()).formatData())) {
                    for (POPPrecEff eff : step.getPopEffs()) {
                        currentVars = this.failureModelFunction.apply(new Variable(eff, stepEntry.getKey()), currentVars)
                                .collect(ImmutableList.toImmutableList());
                    }
                }
                //all preconditions valid
                else if (CollectionUtils.isEmpty(step.getPopPrecs()) ||
                        step.getPopPrecs().stream()
                                .allMatch(eff -> prevStageVars.stream()
                                        .filter(var -> var.getFormattable().formatFunctionKeyWithValue().equals(
                                                Variable.of(eff).formatFunctionKeyWithValue()))
                                        .findFirst()
                                        .map(FormattableValue::getValue)
                                        .orElse(false))) {  //check key, value, state are equal

                    for (POPPrecEff eff : step.getPopEffs()) {
                        currentVars = successModelFunction.apply(new Variable(eff, stepEntry.getKey()), currentVars)
                                .collect(ImmutableList.toImmutableList());
                    }
                } else {
                    //preconditions are not valid
                    for (POPPrecEff eff : step.getPopEffs()) {
                        currentVars = this.preconditionsNotValidModelFunction.apply(new Variable(eff, stepEntry.getKey()), currentVars)
                                .collect(ImmutableList.toImmutableList());
                    }
                }
            }
        }
        Integer maxStep = plan.keySet().stream().max(Integer::compareTo)
                .orElseThrow(() -> new RuntimeException("No max step for plan" + plan));

        ImmutableList<FormattableValue<Variable>> finalVariableState = getStageVariableState(currentVars, maxStep + 1);

        //add UNDEFINED variables to the final state
        Set<String> variablesWithUndefinedValues = finalVariableState.stream()
                .filter(v -> Variable.UNDEFINED.equals(v.getFormattable().getValue()))
                .map(v -> v.getFormattable().formatFunctionKey())
                .collect(Collectors.toSet());
        finalVariableState = Stream.concat(
                finalVariableState.stream(),
                finalVariableState.stream()
                                .collect(Collectors.groupingBy(t -> t.getFormattable().formatFunctionKey()))
                        .entrySet().stream()
                        .filter(entry -> !variablesWithUndefinedValues.contains(entry.getKey()))
                        .map(entry -> FormattableValue.of(
                                entry.getValue().get(0).getFormattable().toBuilder().functionValue(Variable.UNDEFINED).build(), false)))
                        .collect(ImmutableList.toImmutableList());

        return finalVariableState.stream()
                .map(formattableValue -> FormattableValue.<Formattable>of(
                        formattableValue.getFormattable().toBuilder().stage(maxStep + 1).build(),
                        formattableValue.getValue()))
                .collect(ImmutableList.toImmutableList());
    }
}