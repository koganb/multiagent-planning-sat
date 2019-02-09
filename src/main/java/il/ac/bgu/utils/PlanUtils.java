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
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_planner.POPPrecEff;
import org.apache.commons.lang3.SerializationUtils;

import java.util.*;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Action.State.HEALTHY;

/**
 * Created by borisk on 11/26/2018.
 */

@Slf4j
public class PlanUtils {

    public static List<List<FormattableValue<? extends Formattable>>> createPlanHardConstraints(Map<Integer, Set<Step>> plan,
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


    public static void updatePlanWithAgentDependencies(Map<Integer, Set<Step>> plan) {

        ImmutableSet<String> agentNames = plan.values().stream().flatMap(Collection::stream)
                .filter(step -> step.getAgent() != null)
                .map(Step::getAgent)
                .collect(ImmutableSet.toImmutableSet());


        //add dependency on agent for precondition and effect
        plan.forEach((index, steps) -> steps.forEach(step -> {
                    if (Objects.equals(step.getActionName(), "Initial")) {
                        agentNames.stream()
                                .map(AgentPOPPrecEffFactory::createConditionOnAgent)
                                .forEach(cond -> step.getPopEffs().add(cond));
                    } else {
                        POPPrecEff agentCondition = AgentPOPPrecEffFactory.createConditionOnAgent(step.getAgent());
                        step.getPopPrecs().add(agentCondition);
                        step.getPopEffs().add(agentCondition);
                    }
                }
        ));

    }


    public static List<FormattableValue<Formattable>> encodeHealthyClauses(Map<Integer, Set<Step>> plan) {

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


    public static Map<Integer, Set<Step>> loadSerializedPlan(String planPath) {
        log.info("Start loading plan {}", planPath);
        return SerializationUtils.deserialize(PlanUtils.class.getClassLoader().getResourceAsStream(planPath));
    }


}
