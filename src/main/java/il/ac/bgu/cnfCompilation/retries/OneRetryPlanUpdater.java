package il.ac.bgu.cnfCompilation.retries;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.plan.PlanAction;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static il.ac.bgu.dataModel.Action.State.CONDITIONS_NOT_MET;

public class OneRetryPlanUpdater implements RetryPlanUpdater {

    @Override
    public RetriesPlanCreatorResult updatePlan(Map<Integer, ImmutableList<PlanAction>> originalPlan) {

        Map<Integer, Set<String>> stepIndexToPreconditionKeys = createStepIndexToEffectMap(originalPlan, PlanAction::getPreconditions);
        Map<Integer, Set<String>> stepIndexToEffectKeys = createStepIndexToEffectMap(originalPlan, PlanAction::getEffects);

        Map<Integer, Set<String>> stepIndexToAgentNames = createStepIndexToAgentNameMap(originalPlan);

        Integer maxStep = originalPlan.keySet().stream()
                .reduce(Integer::max).orElseThrow(() -> new RuntimeException("No max step found for plan: " + originalPlan));


        List<Pair<Integer, PlanAction>> actionList = new ArrayList<>();
        Map<Action, Action> actionDependencyMap = new HashMap<>();

        for (Map.Entry<Integer, ImmutableList<PlanAction>> entry : originalPlan.entrySet()) {
            if (entry.getKey() >= 0) {  //skip init stage
                for (PlanAction step : entry.getValue()) {

                    //add original step to the list
                    actionList.add(ImmutablePair.of(entry.getKey(), step));

                    //try to add action to the next stage
                    if (entry.getKey() < maxStep) {

                        int futureStep = entry.getKey() + 1;
                        if (step.getEffects().stream()
                                .map(Variable::formatFunctionKey)

                                //check the effects are not collide
                                .noneMatch(key ->
                                        stepIndexToPreconditionKeys.getOrDefault(futureStep, ImmutableSet.of())
                                                .contains(key)
                                                ||
                                                stepIndexToEffectKeys.getOrDefault(futureStep, ImmutableSet.of())
                                                        .contains(key)
                                ) &&
                                step.getPreconditions().stream()
                                        .map(Variable::formatFunctionKey)

                                        //check the preconditions are not collide with effects of other actions
                                        .noneMatch(key ->
                                                stepIndexToEffectKeys.getOrDefault(futureStep, ImmutableSet.of())
                                                        .contains(key)
                                        ) &&

                                //check agent name is not collide
                                !stepIndexToAgentNames.getOrDefault(futureStep, ImmutableSet.of())
                                        .contains(step.getAgentName())

                        ) {
                            PlanAction retriedStep = new PlanAction(step.getAgentName(), futureStep, step.getActionName(), PlanAction.StepType.RETRIED,
                                    step.getPreconditions(), step.getEffects());

                            //add retry step
                            actionList.add(ImmutablePair.of(futureStep, retriedStep));

                            //add to action dependency
                            actionDependencyMap.put(Action.of(retriedStep, futureStep), Action.of(retriedStep, entry.getKey(), CONDITIONS_NOT_MET));

                            //add added action effects to the map
                            retriedStep.getEffects().stream()
                                    .map(Variable::formatFunctionKey).forEach(effKey -> {
                                Optional.ofNullable(
                                        stepIndexToEffectKeys.putIfAbsent(futureStep, Sets.newHashSet(effKey)))
                                        .ifPresent(set -> set.add(effKey));

                            });

                            //add added action agent name to the map
                            Optional.ofNullable(stepIndexToAgentNames.putIfAbsent(
                                    futureStep, Sets.newHashSet(retriedStep.getAgentName()))).ifPresent(set -> set.add(step.getAgentName()));

                        }
                    }
                }
            }
        }

        originalPlan.entrySet().stream()
                .filter(entry -> entry.getKey() == -1)
                .flatMap(entry -> entry.getValue().stream()
                        .map(step -> ImmutablePair.of(entry.getKey(), step)))
                .forEach(actionList::add);
        Map<Integer, ImmutableList<PlanAction>> updatedPlan =
                actionList.stream().collect(Collectors.groupingBy(
                        Pair::getKey,
                        Collectors.mapping(Pair::getValue, ImmutableList.toImmutableList())));
        return new RetriesPlanCreatorResult(updatedPlan, actionDependencyMap);

    }

    private Map<Integer, Set<String>> createStepIndexToAgentNameMap(Map<Integer, ImmutableList<PlanAction>> originalPlan) {
        return originalPlan.entrySet().stream()
                .filter(entry -> entry.getKey() >= 0)
                .flatMap(entry -> entry.getValue().stream()
                        .map(step -> ImmutablePair.of(entry.getKey(), step.getAgentName())))
                .collect(Collectors.groupingBy(
                        Pair::getKey,
                        Collectors.mapping(Pair::getValue, Collectors.toSet())));
    }

    private Map<Integer, Set<String>> createStepIndexToEffectMap(Map<Integer, ImmutableList<PlanAction>> originalPlan,
                                                                 Function<PlanAction, List<Variable>> conditionEffectsFunction) {
        return originalPlan.entrySet().stream()
                .filter(entry -> entry.getKey() >= 0)
                .flatMap(entry -> entry.getValue().stream()
                        .flatMap(step -> conditionEffectsFunction.apply(step).stream()
                                .map(eff -> ImmutablePair.of(entry.getKey(), eff.formatFunctionKey()))))
                .collect(Collectors.groupingBy(
                        Pair::getKey,
                        Collectors.mapping(Pair::getValue, Collectors.toSet())));
    }

    @Override
    public String getName() {
        return "one retry";
    }
}
