package il.ac.bgu.testUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import il.ac.bgu.cnfCompilation.AgentPOPPrecEffFactory;
import il.ac.bgu.cnfCompilation.retries.RetryPlanUpdater;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.plan.PlanAction;
import il.ac.bgu.variableModel.VariableModelFunction;
import il.ac.bgu.variablesCalculation.FinalVariableStateCalcImpl;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.math3.util.Combinations;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Action.State.FAILED;

@Slf4j
public class ActionDependencyCalculation {

    private Map<ActionKey, Set<Action>> actionDependenciesFull = new HashMap<>();

    private List<FormattableValue<? extends Formattable>> normalExecutionFinalState;
    private FinalVariableStateCalcImpl finalVariableStateCalc;

    private void createActionDependenciesFull(Map<ActionKey, Set<Action>> actionDependencies,
                                              ActionKey actionKey, ActionKey currAction) {

        Set<Action> dependencies = actionDependencies.get(currAction);
        Set<Action> currentDependentActions = actionDependenciesFull.computeIfAbsent(actionKey, k -> new HashSet<>());
        if (CollectionUtils.isNotEmpty(dependencies)) {
            currentDependentActions.addAll(dependencies);
            dependencies.forEach(dependency ->
                    createActionDependenciesFull(actionDependencies, actionKey, new ActionKey(dependency)));
        }
    }

    public ActionDependencyCalculation(Map<Integer, ImmutableList<PlanAction>> plan,
                                       List<FormattableValue<? extends Formattable>> normalExecutionFinalState,
                                       VariableModelFunction failureModelFunction,
                                       RetryPlanUpdater conflictRetriesModel) {
        this.normalExecutionFinalState = normalExecutionFinalState;
        Map<VariableKey, Action> effectsToAction = new HashMap<>();
        Map<ActionKey, Set<Action>>  actionDependencies = new HashMap<>();

        finalVariableStateCalc = new FinalVariableStateCalcImpl(plan, failureModelFunction, conflictRetriesModel);

        plan.entrySet().stream()
                .filter(i -> i.getKey() != -1)
                .flatMap(entry -> entry.getValue().stream().map(step ->
                        ImmutablePair.of(entry.getKey(), step))).
                forEach(pair -> {
                    final PlanAction planAction = pair.right;
                    final Integer stage = pair.left;
                    Action action = Action.of(planAction, stage);
                    ActionKey actionKey = new ActionKey(action);
                    actionDependencies.computeIfAbsent(actionKey, k -> new HashSet<>());

                    planAction.getEffects().stream()
                            //remove action dependency (that we added) from action dependency calculation
                            .filter(eff -> !eff.getFunctionKey().startsWith(AgentPOPPrecEffFactory.READY))
                            .forEach(eff -> {
                                        VariableKey preconditionKey = new VariableKey(eff);
                                        effectsToAction.put(preconditionKey, action);
                                    }
                            );
                     planAction.getPreconditions().stream()
                            //remove action dependency (that we added) from action dependency calculation
                            .filter(prec -> !prec.getFunctionKey().startsWith(AgentPOPPrecEffFactory.READY))
                            .forEach(prec -> {
                                        VariableKey preconditionKey = new VariableKey(prec);
                                Optional.ofNullable(effectsToAction.get(preconditionKey))
                                        .filter(conditionAction -> ! new ActionKey(conditionAction).equals(actionKey))  //eliminate self reference
                                        .ifPresent(conditionAction -> actionDependencies.get(actionKey).add(conditionAction));
                                    }
                            );


                });
        actionDependencies.keySet().forEach(dependency ->
                createActionDependenciesFull(actionDependencies, dependency, dependency));
    }

//    public List<Supplier<Set<Action>>> getIndependentActionsList(List<Integer> actionNumbers) {
//        return actionNumbers.stream()
//                .flatMap(i -> getIndependentActionsList(i).stream())
//                .collect(Collectors.toList());
//    }

    public Stream<Set<Action>> getIndependentActionsList(int actionNumber) {
        List<ActionKey> keys = new ArrayList<>(actionDependenciesFull.keySet());

        return Streams.stream(new Combinations(actionDependenciesFull.size(), actionNumber).iterator()).flatMap(
                combination -> {
                    Set<ActionKey> actionKeys = Arrays.stream(combination).
                            mapToObj(keys::get).
                            collect(Collectors.toSet());
                    Set<ActionKey> dependentActions = actionKeys.stream().
                            flatMap(key -> actionDependenciesFull.get(key).stream()).
                            map(ActionKey::new).
                            collect(Collectors.toSet());

                    return CollectionUtils.intersection(actionKeys, dependentActions).isEmpty() ?
                            Stream.of(actionKeys.stream()
                                    .map(ActionKey::getAction)
                                    .map(action -> action.toBuilder().state(FAILED).build())
                                    .collect(Collectors.toSet())) : Stream.empty();
                });
//                .filter(t -> {
//                    if (finalVariableStateCalc.getFinalVariableState(t).containsAll(normalExecutionFinalState)) {
//                        log.info("Filter out action failure candidate {} as it leads to correct state", t);
//                        return false;
//                    }
//                    return true;
//
//                });
    }

    @EqualsAndHashCode(of = "variableKey")
    @ToString
    private static class VariableKey {
        private String variableKey;

        public VariableKey(Variable variable) {
            this.variableKey = variable.formatFunctionKeyWithValue();
        }
    }

    @EqualsAndHashCode(of = "actionKey")
    @ToString
    private static class ActionKey {
        private String actionKey;
        private Action action;

        public ActionKey(Action action) {
            this.actionKey = action.formatData();
            this.action = action;
        }

        public Action getAction() {
            return action;
        }
    }
}
