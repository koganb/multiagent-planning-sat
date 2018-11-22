package il.ac.bgu.cnfCompilation.retries;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Variable;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static il.ac.bgu.dataModel.Action.State.CONDITIONS_NOT_MET;

public class OneRetryPlanUpdater implements RetryPlanUpdater {

    @Override
    public RetriesPlanCreatorResult updatePlan(Map<Integer, Set<Step>> originalPlan) {

        Map<Integer, Set<String>> stepIndexToEffectKeys = createStepIndexToEffectMap(originalPlan);

        Map<Integer, Set<String>> stepIndexToAgentNames = createStepIndexToAgentNameMap(originalPlan);

        Integer maxStep = originalPlan.keySet().stream()
                .reduce(Integer::max).orElseThrow(() -> new RuntimeException("No max step found for plan: " + originalPlan));


        List<Pair<Integer, Step>> actionList = new ArrayList<>();
        Map<Action, Action> actionDependencyMap = new HashMap<>();

        for (Map.Entry<Integer, Set<Step>> entry : originalPlan.entrySet()) {
            if (entry.getKey() >= 0) {  //skip init stage
                for (Step step : entry.getValue()) {

                    //add original step to the list
                    actionList.add(ImmutablePair.of(entry.getKey(), step));

                    //try to add action to the next stage
                    if (entry.getKey() < maxStep) {

                        int futureStep = entry.getKey() + 1;
                        if (step.getPopEffs().stream().map(Variable::of)
                                .map(Variable::formatFunctionKey)

                                //check the effects are not collide
                                .noneMatch(key ->
                                        stepIndexToEffectKeys.getOrDefault(futureStep, ImmutableSet.of())
                                                .contains(key)) &&

                                //check action name is not collide
                                !stepIndexToAgentNames.getOrDefault(futureStep, ImmutableSet.of())
                                        .contains(step.getAgent())

                        ) {
                            //add retry step
                            actionList.add(ImmutablePair.of(futureStep, step));

                            //add to action dependency
                            actionDependencyMap.put(Action.of(step, futureStep), Action.of(step, entry.getKey(), CONDITIONS_NOT_MET));

                            //add added action effects to the map
                            step.getPopEffs().stream().map(Variable::of)
                                    .map(Variable::formatFunctionKey).forEach(effKey -> {
                                Optional.ofNullable(
                                        stepIndexToEffectKeys.putIfAbsent(futureStep, Sets.newHashSet(effKey)))
                                        .ifPresent(set -> set.add(effKey));

                            });

                            //add added action agent name to the map
                            Optional.ofNullable(stepIndexToAgentNames.putIfAbsent(
                                    futureStep, Sets.newHashSet(step.getAgent()))).ifPresent(set -> set.add(step.getAgent()));

                        }
                    }
                }
            }
        }


        @SuppressWarnings("UnstableApiUsage")
        Map<Integer, ImmutableSet<Step>> updatedPlan = actionList.stream().collect(Collectors.groupingBy(
                Pair::getKey,
                Collectors.mapping(Pair::getValue, ImmutableSet.toImmutableSet())));
        return new RetriesPlanCreatorResult(ImmutableMap.copyOf(updatedPlan), ImmutableMap.copyOf(actionDependencyMap));

    }

    private Map<Integer, Set<String>> createStepIndexToAgentNameMap(Map<Integer, Set<Step>> originalPlan) {
        return originalPlan.entrySet().stream()
                .filter(entry -> entry.getKey() >= 0)
                .flatMap(entry -> entry.getValue().stream()
                        .map(step -> ImmutablePair.of(entry.getKey(), step.getAgent())))
                .collect(Collectors.groupingBy(
                        Pair::getKey,
                        Collectors.mapping(Pair::getValue, Collectors.toSet())));
    }

    private Map<Integer, Set<String>> createStepIndexToEffectMap(Map<Integer, Set<Step>> originalPlan) {
        return originalPlan.entrySet().stream()
                .filter(entry -> entry.getKey() >= 0)
                .flatMap(entry -> entry.getValue().stream()
                        .flatMap(step -> step.getPopEffs().stream()
                                .map(eff -> ImmutablePair.of(entry.getKey(), Variable.of(eff).formatFunctionKey()))))
                .collect(Collectors.groupingBy(
                        Pair::getKey,
                        Collectors.mapping(Pair::getValue, Collectors.toSet())));
    }


}
