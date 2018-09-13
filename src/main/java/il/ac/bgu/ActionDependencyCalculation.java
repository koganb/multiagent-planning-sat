package il.ac.bgu;

import com.google.common.collect.Streams;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.math3.util.Combinations;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ActionDependencyCalculation {

    private Map<Action, Set<Action>> actionDependenciesFull = new HashMap<>();

    public ActionDependencyCalculation(TreeMap<Integer, Set<Step>> plan) {
        Map<FormattableValue, Action> effectsToStepMap = new HashMap<>();
        Map<Action, Set<Action>> actionDependencies = new HashMap<>();

        plan.entrySet().stream().filter(i -> i.getKey() != -1).
                flatMap(entry -> entry.getValue().stream().map(step ->
                        ImmutablePair.of(entry.getKey(), step))).
                forEach(pair -> {
                    Action action = Action.of(pair.right, pair.left);
                    actionDependencies.computeIfAbsent(action, k -> new HashSet<>());

                    pair.getRight().getPopPrecs().forEach(prec -> {
                        FormattableValue<Variable> variableValue = FormattableValue.of(
                                Variable.of(prec), true);
                        Action dependentAction = effectsToStepMap.get(variableValue);

                        if (dependentAction != null) {
                                    //add dependent steps
                            actionDependencies.get(action).add(dependentAction);
                                }

                        effectsToStepMap.put(variableValue, action);
                            }
                    );
                });
        actionDependencies.keySet().forEach(dependency ->
                createActionDependenciesFull(actionDependencies, dependency, dependency));
    }


    private void createActionDependenciesFull(Map<Action, Set<Action>> actionDependencies,
                                              Action actionKey, Action currAction) {
        Set<Action> dependencies = actionDependencies.get(currAction);
        if (dependencies != null) {
            actionDependenciesFull.computeIfAbsent(actionKey, k -> new HashSet<>()).
                    addAll(dependencies);
            dependencies.forEach(dependency ->
                    createActionDependenciesFull(actionDependencies, actionKey, dependency));
        }
    }

    public List<Set<Action>> getIndependentActionsList(int listSize) {
        List<Action> keys = new ArrayList<>(actionDependenciesFull.keySet());

        List<Set<Action>> independentActions = Streams.stream(new Combinations(actionDependenciesFull.size(), listSize).iterator()).flatMap(
                combination -> {
                    Set<Action> actionKeys = Arrays.stream(combination).
                            mapToObj(keys::get).
                            collect(Collectors.toSet());
                    Set<Action> dependentActions = actionKeys.stream().
                            flatMap(key -> actionDependenciesFull.get(key).stream()).
                            collect(Collectors.toSet());

                    return CollectionUtils.intersection(actionKeys, dependentActions).isEmpty() ?
                            Stream.of(actionKeys) : Stream.empty();
                })

                .collect(Collectors.toList());
        return independentActions;
    }
}
