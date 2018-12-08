package il.ac.bgu.sat;

import lombok.extern.slf4j.Slf4j;
import org.agreement_technologies.agents.MAPboot;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_planner.POPAction;
import org.agreement_technologies.service.map_planner.POPStep;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Created by Boris on 26/05/2017.
 */
@Slf4j
public class SatSolver {

    public static TreeMap<Integer, Set<Step>> calculateSolution(String[] agentDefs) {
        Set<Map<Integer, Set<Step>>> solutionPlans = MAPboot.runCommandLine(agentDefs);

        Map<Integer, Set<Set<Step>>> solutionsBySteps = solutionPlans.stream().
                flatMap(i -> i.entrySet().stream()).
                collect(
                        Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(
                                Map.Entry::getValue,
                                Collectors.toSet())));

        return solutionsBySteps.entrySet().stream().map(
                i -> new ImmutablePair<>(i.getKey(), mergeStageStepsOfDifferentPlans(i.getValue()))).

                collect(Collectors.groupingBy(Pair::getLeft, TreeMap::new,

                        Collector.of(
                                HashSet::new,
                                (col, pair) -> col.addAll(pair.getRight()),
                                (r1, r2) -> {
                                    r1.addAll(r2);
                                    return r1;
                                }
                        )));

    }


    private static Set<Step> mergeStageStepsOfDifferentPlans(Set<Set<Step>> steps) {
        return steps.stream().
                //filter out steps with precondition or effect functions null
                        flatMap(Collection::stream).filter(t ->
                        t.getPopPrecs().stream().allMatch(y -> y.getFunction() != null) &&
                                t.getPopEffs().stream().allMatch(y -> y.getFunction() != null)).
                //create map to filter duplicate steps
                        collect(
                        Collectors.groupingBy(Step::getActionName, Collectors.toList())).
                //in case of initial step take all steps (because of privacy) else take first one;
                        entrySet().stream().flatMap(t ->
                        t.getKey().equals("Initial") ?
                                //merge effects of initial step
                                Stream.of(new POPStep(new POPAction(
                                        "Initial",
                                        new ArrayList<>(),
                                        t.getValue().stream().flatMap(k -> k.getPopEffs().stream()).distinct().collect(Collectors.toList())), 0, null))
                                : Stream.of(t.getValue().get(0))).
                        collect(Collectors.toSet());

    }

}
