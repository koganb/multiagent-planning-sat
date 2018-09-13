package il.ac.bgu;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.failureModel.SuccessVariableModel;
import il.ac.bgu.failureModel.VariableModelFunction;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_planner.POPPrecEff;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Action.State.FAILED;


public class FinalVariableStateCalc {
    private TreeMap<Integer, Set<Step>> plan;
    private VariableModelFunction failureModelFunction;
    private VariableModelFunction successModelFunction = new SuccessVariableModel();

    public FinalVariableStateCalc(TreeMap<Integer, Set<Step>> plan, VariableModelFunction failureModelFunction) {
        this.plan = plan;
        this.failureModelFunction = failureModelFunction;
    }

    public ImmutableList<FormattableValue<Formattable>> getFinalVariableState(Set<Action> failedActions) {

        ImmutableList<FormattableValue<Variable>> currentVars = ImmutableList.of();
        for (Map.Entry<Integer, Set<Step>> stepEntry : plan.entrySet()) {
            ImmutableList<FormattableValue<Variable>> prevStageVars = ImmutableList.copyOf(currentVars);

            for (Step step : stepEntry.getValue()) {
                //step is failed
                if (failedActions.contains(Action.of(step, stepEntry.getKey(), FAILED))) {
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
                        currentVars = this.failureModelFunction.apply(new Variable(eff, stepEntry.getKey()), currentVars)
                                .collect(ImmutableList.toImmutableList());
                    }
                }
            }
        }
        Integer maxStep = plan.keySet().stream().max(Integer::compareTo)
                .orElseThrow(() -> new RuntimeException("No max step for plan" + plan));

        //add UNDEFINED variables to the final state
        currentVars =
                Stream.concat(
                        currentVars.stream(),
                        currentVars.stream()
                                .collect(Collectors.groupingBy(t -> t.getFormattable().formatFunctionKey()))
                                .values().stream()
                                .map(list -> FormattableValue.of(
                                        list.get(0).getFormattable().toBuilder().functionValue(Variable.UNDEFINED).build(), false)))
                        .collect(ImmutableList.toImmutableList());

        return currentVars.stream()
                .map(formattableValue -> FormattableValue.<Formattable>of(
                        formattableValue.getFormattable().toBuilder().stage(maxStep + 1).build(),
                        formattableValue.getValue()))
                .collect(ImmutableList.toImmutableList());
    }
}