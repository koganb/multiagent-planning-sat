package il.ac.bgu.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import il.ac.bgu.cnfClausesModel.CnfClausesFunction;
import il.ac.bgu.cnfCompilation.AgentPOPPrecEffFactory;
import il.ac.bgu.cnfCompilation.CnfCompilation;
import il.ac.bgu.cnfCompilation.retries.RetryPlanUpdater;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.plan.PlanAction;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Action.State.HEALTHY;
import static il.ac.bgu.plan.PlanAction.StepType.NORMAL;

/**
 * Created by borisk on 11/26/2018.
 */

@Slf4j
public class PlanUtils {

    public static List<List<FormattableValue<? extends Formattable>>> createPlanHardConstraints(Map<Integer, ImmutableList<PlanAction>> plan,
                                                                                                RetryPlanUpdater retryPlanUpdater,
                                                                                                CnfClausesFunction healthyCnfClausesCreator,
                                                                                                CnfClausesFunction conflictCnfClausesCreator,
                                                                                                CnfClausesFunction failedCnfClausesCreator) {

        CnfCompilation cnfCompilation = new CnfCompilation(plan, retryPlanUpdater, healthyCnfClausesCreator,
                conflictCnfClausesCreator, failedCnfClausesCreator);

        return StreamEx.<List<FormattableValue<? extends Formattable>>>of()
                .append(cnfCompilation.compileToCnf().stream())
                .append(PlanSolvingUtils.calcInitFacts(plan).stream().map(ImmutableList::of))
                .collect(ImmutableList.toImmutableList());
    }


    public static Map<Integer, ImmutableList<PlanAction>> updatePlanWithAgentDependencies(Map<Integer, ImmutableList<PlanAction>> plan) {
        ImmutableSet<String> agentNames = plan.values().stream().flatMap(Collection::stream)
                .filter(step -> step.getAgentName() != null)
                .map(PlanAction::getAgentName)
                .collect(ImmutableSet.toImmutableSet());

        //add dependency on agent for precondition and effect
        return plan.entrySet().stream().map(entry -> {
            ImmutableList<PlanAction> steps = entry.getValue().stream().map(step -> {
                if (Objects.equals(step.getActionName(), "Initial")) {

                    ImmutableList<Variable> updatedEffects = new ImmutableList.Builder<Variable>()
                            .addAll(step.getEffects())
                            .addAll(agentNames.stream()
                                    .map(AgentPOPPrecEffFactory::createConditionOnAgent).iterator())
                            .build();
                    return new PlanAction(step.getAgentName(), step.getIndex(), step.getActionName(), NORMAL,
                            step.getPreconditions(), updatedEffects);

                }
                else {
                    Variable agentCondition = AgentPOPPrecEffFactory.createConditionOnAgent(step.getAgentName());

                    ImmutableList<Variable> updatedPreconditions = new ImmutableList.Builder<Variable>()
                            .addAll(step.getPreconditions())
                            .add(agentCondition)
                            .build();
                    ImmutableList<Variable> updatedEffects = new ImmutableList.Builder<Variable>()
                            .addAll(step.getEffects())
                            .add(agentCondition)
                            .build();

                    return new PlanAction(step.getAgentName(), step.getIndex(), step.getActionName(), NORMAL,
                            updatedPreconditions, updatedEffects);
                }
            }).collect(ImmutableList.toImmutableList());
            return ImmutablePair.of(entry.getKey(), steps);
        }).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }


    public static List<FormattableValue<Formattable>> encodeHealthyClauses(Map<Integer, ImmutableList<PlanAction>> plan) {

        ImmutableList<FormattableValue<Formattable>> healthyClauses =

                plan.entrySet().stream().
                        filter(i -> i.getKey() != -1).
                        flatMap(entry -> entry.getValue().stream().flatMap(
                                step -> Stream.of(
                                        FormattableValue.<Formattable>of(Action.of(step, entry.getKey(), HEALTHY), true)
                                ))).
                        collect(ImmutableList.toImmutableList());

        log.trace("healthy clauses {}", healthyClauses);
        return healthyClauses;
    }

    public static Map<Integer, ImmutableList<PlanAction>> loadSerializedPlan(String planPath) {
        log.info("Start loading plan {}", planPath);
        Map<Integer, Set<Step>> deserializedPlan = SerializationUtils.deserialize(
                PlanUtils.class.getClassLoader().getResourceAsStream(planPath));
        Map<Integer, ImmutableList<PlanAction>> plan = deserializedPlan.entrySet().stream().
                map(entry -> ImmutablePair.of(
                        entry.getKey(),
                        entry.getValue().stream().map(step -> new PlanAction(
                                step.getAgent(),
                                entry.getKey(),
                                step.getActionName(),
                                NORMAL,
                                step.getPopPrecs().stream().map(Variable::of).collect(ImmutableList.toImmutableList()),
                                step.getPopEffs().stream().map(Variable::of).collect(ImmutableList.toImmutableList())

                        )).collect(ImmutableList.toImmutableList()))).
                collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        return PlanUtils.updatePlanWithAgentDependencies(plan);
    }

}
