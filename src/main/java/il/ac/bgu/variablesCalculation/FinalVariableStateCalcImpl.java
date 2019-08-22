package il.ac.bgu.variablesCalculation;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.cnfCompilation.retries.RetryPlanUpdater;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.plan.PlanAction;
import il.ac.bgu.utils.CnfCompilationUtils;
import il.ac.bgu.utils.PlanSolvingUtils;
import il.ac.bgu.variableModel.NoEffectVariableFailureModel;
import il.ac.bgu.variableModel.SuccessVariableModel;
import il.ac.bgu.variableModel.VariableModelFunction;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;
import java.util.stream.Collectors;


public class FinalVariableStateCalcImpl implements FinalVariableStateCalc {
    private Map<Integer, ImmutableList<PlanAction>> plan;
    private VariableModelFunction failureModelFunction;
    private VariableModelFunction conflictModelFunction = new NoEffectVariableFailureModel();
    private VariableModelFunction successModelFunction = new SuccessVariableModel();

    public FinalVariableStateCalcImpl(Map<Integer, ImmutableList<PlanAction>> plan, VariableModelFunction failureModelFunction,
                                      RetryPlanUpdater retryPlanUpdater) {
        this.plan = retryPlanUpdater.updatePlan(plan).updatedPlan;
        this.failureModelFunction = failureModelFunction;
    }

    public ImmutableList<FormattableValue<? extends Formattable>> getFinalVariableState(Collection<? extends Formattable> failedActions) {

        Set<String> failedActionsKeys = failedActions.stream()
                .map(Formattable::formatFunctionKey)
                .collect(Collectors.toSet());

        Map<PlanAction, Action.State> executionStatus = new HashMap<>();

        List<FormattableValue<Variable>> currentVars = PlanSolvingUtils.calcInitFacts(plan);


        for (Map.Entry<Integer, ImmutableList<PlanAction>> stepEntry : plan.entrySet()) {

            if (stepEntry.getKey() != -1) {

                ImmutableList<FormattableValue<Variable>> prevStageVars =
                        CnfCompilationUtils.calcVariableState(currentVars.stream(), stepEntry.getKey())
                                .collect(ImmutableList.toImmutableList());

                for (PlanAction step : stepEntry.getValue()) {

                    //skip retried action if the original action is HEALTHY or FAILED
                    if (step.getStepType() == PlanAction.StepType.RETRIED &&
                            executionStatus.get(new PlanAction(step.getAgentName(),
                                    step.getIndex() - 1, step.getActionName())) != Action.State.CONDITIONS_NOT_MET) {
                        continue;
                    }

                    boolean isActionFailed = failedActionsKeys.contains(
                            Action.of(step, stepEntry.getKey()).formatFunctionKey());

                    ImmutablePair<Action.State, List<FormattableValue<Variable>>> actionResult =
                            ActionUtils.executeAction(
                                    step, stepEntry.getKey(), isActionFailed,
                                    failureModelFunction, conflictModelFunction,
                                    successModelFunction, currentVars, prevStageVars);


                    currentVars = actionResult.getRight();

                    executionStatus.put(step, actionResult.left);


                }
            }
        }
        Integer maxStep = plan.keySet().stream().max(Integer::compareTo)
                .orElseThrow(() -> new RuntimeException("No max step for plan" + plan));

        ImmutableList<FormattableValue<Variable>> finalVariableState = CnfCompilationUtils.calcVariableState(
                currentVars.stream(), maxStep + 1).collect(ImmutableList.toImmutableList());

        final ImmutableList<FormattableValue<? extends Formattable>> finalState = finalVariableState.stream()
                .map(formattableValue -> FormattableValue.<Formattable>of(
                        formattableValue.getFormattable().toBuilder().stage(maxStep + 1).build(),
                        formattableValue.getValue()))
                .collect(ImmutableList.toImmutableList());
        return finalState;
    }

}