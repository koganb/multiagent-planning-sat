package il.ac.bgu.variablesCalculation;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.CnfCompilationUtils;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.variableModel.NoEffectVariableFailureModel;
import il.ac.bgu.variableModel.SuccessVariableModel;
import il.ac.bgu.variableModel.VariableModelFunction;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;


public class FinalNoRetriesVariableStateCalc implements FinalVariableStateCalc {
    private TreeMap<Integer, Set<Step>> plan;
    private VariableModelFunction failureModelFunction;
    private VariableModelFunction conflictModelFunction = new NoEffectVariableFailureModel();
    private VariableModelFunction successModelFunction = new SuccessVariableModel();

    public FinalNoRetriesVariableStateCalc(TreeMap<Integer, Set<Step>> plan, VariableModelFunction failureModelFunction) {
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

                boolean isActionFailed = failedActionsKeys.contains(
                        Action.of(step, stepEntry.getKey()).formatFunctionKey());

                ImmutablePair<Action.State, ImmutableList<FormattableValue<Variable>>> actionResult =
                        ActionUtils.executeAction(
                                step, stepEntry.getKey(), isActionFailed,
                                failureModelFunction, conflictModelFunction,
                                successModelFunction, currentVars, prevStageVars);

                currentVars = actionResult.getRight();

            }
        }
        Integer maxStep = plan.keySet().stream().max(Integer::compareTo)
                .orElseThrow(() -> new RuntimeException("No max step for plan" + plan));

        ImmutableList<FormattableValue<Variable>> finalVariableState = CnfCompilationUtils.calcVariableState(
                currentVars.stream(), maxStep + 1).collect(ImmutableList.toImmutableList());


        return finalVariableState.stream()
                .map(formattableValue -> FormattableValue.<Formattable>of(
                        formattableValue.getFormattable().toBuilder().stage(maxStep + 1).build(),
                        formattableValue.getValue()))
                .collect(ImmutableList.toImmutableList());
    }

}