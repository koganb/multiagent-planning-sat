package il.ac.bgu;

import com.google.common.collect.Streams;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.Combinations;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.CnfEncodingUtils.createEffId;
import static il.ac.bgu.CnfEncodingUtils.encodeValue;

public class ActionDependencyCalculation {

    private Map<String, Step> uuidToAction = new HashMap<>();

    private Map<String, Set<String>> actionDependenciesFull = new HashMap<>();

    public ActionDependencyCalculation(TreeMap<Integer, Set<Step>> plan) {
        Map<Pair<String, Boolean>, Step> effectsToStepMap = new HashMap<>();
        Map<String, Set<String>> actionDependencies = new HashMap<>();

        plan.entrySet().stream().filter(i -> i.getKey() != -1).
                flatMap(entry -> entry.getValue().stream()).
                forEach(step -> {
                    String stepUuid = step.getUuid();
                    uuidToAction.put(stepUuid, step);
                    actionDependencies.computeIfAbsent(stepUuid, k -> new HashSet<>());

                    step.getPopPrecs().forEach(prec -> {
                                Step dependentStep = effectsToStepMap.get(ImmutablePair.of(
                                        createEffId(prec), encodeValue(prec, true)));

                                if (dependentStep != null) {
                                    //add dependent steps
                                    actionDependencies.get(stepUuid).add(dependentStep.getUuid());
                                }
                            }
                    );

                    step.getPopEffs().forEach(eff -> {
                        effectsToStepMap.put(ImmutablePair.of(
                                createEffId(eff), encodeValue(eff, true)),
                                step);
                    });

                });
        actionDependencies.keySet().forEach(dependency ->
                createActionDependenciesFull(actionDependencies, dependency, dependency));
    }


    private void createActionDependenciesFull(Map<String, Set<String>> actionDependencies,
                                              String actionKey, String currActionKey) {
        Set<String> dependencies = actionDependencies.get(currActionKey);
        if (dependencies != null) {
            actionDependenciesFull.computeIfAbsent(actionKey, k -> new HashSet<>()).
                    addAll(dependencies);
            dependencies.forEach(dependency ->
                    createActionDependenciesFull(actionDependencies, actionKey, dependency));
        }
    }

    public List<Set<ImmutablePair<String, Step>>> getIndependentActionsList(int listSize) {
        List<String> keys = new ArrayList<>(actionDependenciesFull.keySet());

        return Streams.stream(new Combinations(actionDependenciesFull.size(), listSize).iterator()).flatMap(
                combination -> {
                    Set<String> actionKeys = Arrays.stream(combination).
                            mapToObj(keys::get).
                            collect(Collectors.toSet());
                    Set<String> dependentActions = actionKeys.stream().
                            flatMap(key -> actionDependenciesFull.get(key).stream()).
                            collect(Collectors.toSet());

                    return CollectionUtils.intersection(actionKeys, dependentActions).isEmpty() ?
                            Stream.of(actionKeys) : Stream.empty();
                }
        ).map(keysSet -> keysSet.stream().map(key -> ImmutablePair.of(key, uuidToAction.get(key))).collect(Collectors.toSet())).collect(Collectors.toList());
    }
}
