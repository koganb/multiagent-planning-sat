package il.ac.bgu.cnfCompilation;

import com.google.common.collect.ImmutableSet;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_planner.POPPrecEff;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by borisk on 11/26/2018.
 */
public class PlanUtils {

    public static void updatePlanWithAgentDependencies(TreeMap<Integer, Set<Step>> plan) {

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

}
