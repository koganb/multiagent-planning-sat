package il.ac.bgu.cnfCompilation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Variable;
import lombok.AllArgsConstructor;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RetriesPlanCreator {

    private Map<Integer, Set<Step>> originalPlan;
    private Integer maxNumberOfRetries;

    private Map<Integer, Set<String>> stepIndexToEffectKeys;
    private Map<Integer, Set<String>> stepIndexToAgentNames;

    public RetriesPlanCreator(Map<Integer, Set<Step>> originalPlan, Integer maxNumberOfRetries) {
        this.originalPlan = originalPlan;
        this.maxNumberOfRetries = maxNumberOfRetries;

        this.stepIndexToEffectKeys = originalPlan.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .flatMap(step -> step.getPopEffs().stream()
                                .map(eff -> ImmutablePair.of(entry.getKey(), Variable.of(eff).formatFunctionKey()))))
                .collect(Collectors.groupingBy(
                        Pair::getKey,
                        Collectors.mapping(Pair::getValue, Collectors.toSet())));

        this.stepIndexToAgentNames = originalPlan.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(step -> ImmutablePair.of(entry.getKey(), step.getAgent())))
                .collect(Collectors.groupingBy(
                        Pair::getKey,
                        Collectors.mapping(Pair::getValue, Collectors.toSet())));


    }

    public RetriesPlanCreatorResult updatePlanWithRetries() {

        Integer maxStep = originalPlan.keySet().stream()
                .reduce(Integer::max).orElseThrow(() -> new RuntimeException("No max step found for plan: " + originalPlan));


        List<Pair<Integer, Step>>
        for (Map.Entry<Integer, Set<Step>> entry : originalPlan.entrySet()) {
            if (entry.getKey() >= 0) {
                for (Step step : entry.getValue()) {
                    for (int futureStep = entry.getKey(); futureStep <= maxStep; futureStep++) {
                        int finalFutureStep = futureStep;
                        if (step.getPopEffs().stream().map(Variable::of)
                                .map(Variable::formatFunctionKey)
                                .noneMatch(key ->
                                        stepIndexToEffectKeys.getOrDefault(finalFutureStep, ImmutableSet.of())
                                                .contains(key)) &&
                                !stepIndexToAgentNames.getOrDefault(futureStep, ImmutableSet.of())
                                        .contains(step.getAgent())

                                ) {
                            step.getPopEffs().stream().map(Variable::of)
                                    .map(Variable::formatFunctionKey).forEach(effKey -> {
                                Optional.ofNullable(
                                        stepIndexToEffectKeys.putIfAbsent(finalFutureStep, Sets.newHashSet(effKey)))
                                        .ifPresent(set -> set.add(effKey));

                            });

                            Optional.ofNullable(stepIndexToAgentNames.putIfAbsent(
                                    finalFutureStep, Sets.newHashSet(step.getAgent()))).ifPresent(set -> set.add(step.getAgent()));

                        }
                    }
                }


            }
        }


        List<ImmutablePair<Integer, Step>> allPlanSteps = originalPlan.entrySet().stream()
                .filter(entry -> entry.getKey() != -1)
                .flatMap(entry -> entry.getValue().stream()
                        .flatMap(step ->
                                Stream.concat(
                                        Stream.of(ImmutablePair.of(entry.getKey(), step)),
                                        IntStream.rangeClosed(entry.getKey(), maxStep)
                                                .filter(futureStep -> step.getPopEffs().stream()
                                                        .map(Variable::of)
                                                        .map(Variable::formatFunctionKey)
                                                        .noneMatch(key ->
                                                                stepIndexToEffectKeys.getOrDefault(futureStep, ImmutableSet.of())
                                                                        .contains(key)))
                                                .filter(futureStep ->
                                                        stepIndexToAgentNames.getOrDefault(futureStep, ImmutableSet.of())
                                                                .contains(step.getAgent()))
                                                .mapToObj(futureStep -> ImmutablePair.of(futureStep, step))
                                )
                        )
                ).collect(Collectors.toList());

        allPlanSteps.stream().collect(Collectors.groupingBy(
                pair -> pair.,
                ImmutableMap::of,
                Collectors.mapping(Pair::getValue, ImmutableSet.toImmutableSet())));


    }

    @AllArgsConstructor
    public static class RetriesPlanCreatorResult {
        public final ImmutableMap<Integer, ImmutableSet<Step>> updatedPlan;
        public final ImmutableMap<Action, Action> actionDependencyMap;
    }

}
