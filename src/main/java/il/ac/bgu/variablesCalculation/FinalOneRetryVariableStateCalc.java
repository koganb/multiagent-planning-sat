package il.ac.bgu.variablesCalculation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.utils.CnfCompilationUtils;
import il.ac.bgu.variableModel.NoEffectVariableFailureModel;
import il.ac.bgu.variableModel.SuccessVariableModel;
import il.ac.bgu.variableModel.VariableModelFunction;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


public class FinalOneRetryVariableStateCalc implements FinalVariableStateCalc {
    private Map<Integer, Set<Step>> plan;
    private VariableModelFunction failureModelFunction;
    private VariableModelFunction conflictModelFunction = new NoEffectVariableFailureModel();
    private VariableModelFunction successModelFunction = new SuccessVariableModel();

    public FinalOneRetryVariableStateCalc(Map<Integer, Set<Step>> plan, VariableModelFunction failureModelFunction) {
        this.plan = plan;
        this.failureModelFunction = failureModelFunction;
    }

    public ImmutableList<FormattableValue<? extends Formattable>> getFinalVariableState(Collection<? extends Formattable> failedActions) {

        Set<String> failedActionsKeys = failedActions.stream()
                .map(Formattable::formatFunctionKey)
                .collect(Collectors.toSet());

        ImmutableList<FormattableValue<Variable>> currentVars = ImmutableList.of();
        ImmutableList<Step> currentConflictActions = ImmutableList.of();


        for (Map.Entry<Integer, Set<Step>> stepEntry : plan.entrySet()) {


            ImmutableList<FormattableValue<Variable>> prevStageVars =
                    CnfCompilationUtils.calcVariableState(currentVars.stream(), stepEntry.getKey())
                            .collect(ImmutableList.toImmutableList());

            ImmutableList<Step> prevStageConflictActions = currentConflictActions;
            ImmutableList.Builder<Step> currentConflictActionsBuilder = ImmutableList.builder();

            for (Step step : stepEntry.getValue()) {

                boolean isActionFailed = failedActionsKeys.contains(
                        Action.of(step, stepEntry.getKey()).formatFunctionKey());

                ImmutablePair<Action.State, ImmutableList<FormattableValue<Variable>>> actionResult =
                        ActionUtils.executeAction(
                                step, stepEntry.getKey(), isActionFailed,
                                failureModelFunction, conflictModelFunction,
                                successModelFunction, currentVars, prevStageVars);

                currentVars = actionResult.getRight();

                if (actionResult.getLeft() == Action.State.CONDITIONS_NOT_MET) {
                    currentConflictActionsBuilder.add(step);
                }

            }
            currentConflictActions = currentConflictActionsBuilder.build();

            //run conflict actions from the previous stage
            ImmutableList<Step> filteredConflictActions = filterConflictValidAction(stepEntry.getValue(), prevStageConflictActions);
            for (Step conflictAction : filteredConflictActions) {
                ImmutablePair<Action.State, ImmutableList<FormattableValue<Variable>>> actionResult =
                        ActionUtils.executeAction(
                                conflictAction, stepEntry.getKey(), false,
                                failureModelFunction, conflictModelFunction,
                                successModelFunction, currentVars, prevStageVars);

                currentVars = actionResult.getRight();

            }
        }

        //calculate variable state for the max plan state
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


    private ImmutableList<Step> filterConflictValidAction(Set<Step> currentActions,
                                                          ImmutableList<Step> prevStageConflictActions) {


        ImmutableSet<String> currentActionsEffectKeys = currentActions.stream()
                .flatMap(step -> step.getPopEffs().stream())
                .map(eff -> Variable.of(eff).formatFunctionKey())
                .collect(ImmutableSet.toImmutableSet());

        ImmutableSet<String> currentAgents = currentActions.stream()
                .map(action -> Optional.ofNullable(action.getAgent()).orElse(""))
                .collect(ImmutableSet.toImmutableSet());

        return prevStageConflictActions.stream()
                .filter(
                        //ensure that agents are different from agents of current actions
                        action -> !currentAgents.contains(action.getAgent()))
                .filter(
                        //effects of the conflict action do not clash with current stage planed actions
                        //in this case conflicted action will not be retried
                        action -> action.getPopEffs().stream()
                                .map(eff -> Variable.of(eff).formatFunctionKey())
                                .noneMatch(currentActionsEffectKeys::contains))
                .collect(ImmutableList.toImmutableList());

    }

}