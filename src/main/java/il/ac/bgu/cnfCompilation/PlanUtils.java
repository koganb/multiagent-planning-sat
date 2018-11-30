package il.ac.bgu.cnfCompilation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import lombok.extern.slf4j.Slf4j;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_planner.POPPrecEff;

import java.util.*;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Action.State.HEALTHY;

/**
 * Created by borisk on 11/26/2018.
 */

@Slf4j
public class PlanUtils {

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


}
