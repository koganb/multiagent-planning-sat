package il.ac.bgu.testUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import il.ac.bgu.cnfCompilation.AgentPOPPrecEffFactory;
import il.ac.bgu.cnfCompilation.retries.RetryPlanUpdater;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.variableModel.VariableModelFunction;
import il.ac.bgu.variablesCalculation.FinalNoRetriesVariableStateCalc;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.math3.util.Combinations;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Action.State.FAILED;

@Slf4j
public class ActionDependencyCalculation {

    private Map<ActionKey, Set<Action>> actionDependenciesFull = new HashMap<>();


    private FinalNoRetriesVariableStateCalc finalVariableStateCalc;
    private ImmutableList<FormattableValue<? extends Formattable>> normalExecutionFinalState;

    public ActionDependencyCalculation(TreeMap<Integer, Set<Step>> plan,
                                       ImmutableList<FormattableValue<? extends Formattable>> normalExecutionFinalState,
                                       VariableModelFunction failureModelFunction,
                                       RetryPlanUpdater conflictRetriesModel) {
        this.normalExecutionFinalState = normalExecutionFinalState;
        Map<VariableKey, Action> preconditionsToAction = new HashMap<>();
        Map<ActionKey, Set<Action>> actionDependencies = new HashMap<>();

        finalVariableStateCalc = new FinalNoRetriesVariableStateCalc(
                conflictRetriesModel.updatePlan(plan).updatedPlan, failureModelFunction);

        plan.entrySet().stream()
                .filter(i -> i.getKey() != -1)
                .flatMap(entry -> entry.getValue().stream().map(step ->
                        ImmutablePair.of(entry.getKey(), step))).
                forEach(pair -> {
                    Action action = Action.of(pair.right, pair.left);
                    ActionKey actionKey = new ActionKey(action);
                    actionDependencies.computeIfAbsent(actionKey, k -> new HashSet<>());

                    pair.getRight().getPopPrecs().stream()
                            //remove action dependency (that we added) from action dependency calculation
                            .filter(prec -> !prec.getFunction().getName().equals(AgentPOPPrecEffFactory.ACTION_NAME))
                            .forEach(prec -> {
                                        VariableKey preconditionKey = new VariableKey(Variable.of(prec));
                                        Action dependentAction = preconditionsToAction.get(preconditionKey);
                                        if (dependentAction != null && !new ActionKey(dependentAction).equals(actionKey)) {
                                            //add dependent steps
                                            actionDependencies.get(actionKey).add(dependentAction);
                                        }
                                        preconditionsToAction.put(preconditionKey, action);
                                    }
                            );
                });
        actionDependencies.keySet().forEach(dependency ->
                createActionDependenciesFull(actionDependencies, dependency, dependency));
    }

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

    public static final int MAX_SIZE = 1;  //no more than MAX_SIZE actions

    public List<Supplier<Set<Action>>> getIndependentActionsList(List<Integer> actionNumbers) {
        return actionNumbers.stream()
                .flatMap(i -> getIndependentActionsList(i).stream())
                .collect(Collectors.toList());
    }

    public List<Supplier<Set<Action>>> getIndependentActionsList(int actionNumber) {
        List<ActionKey> keys = new ArrayList<>(actionDependenciesFull.keySet());

        List<Supplier<Set<Action>>> independentActions = Streams.stream(new Combinations(actionDependenciesFull.size(), actionNumber).iterator()).flatMap(
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
                })
                .limit(MAX_SIZE)
                .filter(t -> {
                    if (finalVariableStateCalc.getFinalVariableState(t).containsAll(normalExecutionFinalState)) {
                        log.info("Filter out action failure candidate {} as it leads to correct state", t);
                        return false;
                    }
                    return true;

                })
                .map(t -> (Supplier<Set<Action>>) () -> t)
                .collect(Collectors.toList());


        return independentActions;
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
