package il.ac.bgu

import com.google.common.collect.Sets
import org.agreement_technologies.common.map_planner.Step
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.Pair
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

@Unroll
class TestSatSolver extends Specification {

    @Shared
    private TreeMap<Integer, Set<Step>> sortedPlan

    def setupSpec() {
        String[] agentDefs = Files.readAllLines(
                Paths.get(this.getClass().getClassLoader().getResource("problems/elevator30.problem").toURI())).stream().
                flatMap({ t -> Arrays.stream(t.split("\t")) }).
                collect(Collectors.toList()).toArray(new String[0])

        sortedPlan = SatSolver.calculateSolution(agentDefs);

    }

    def "test diagnostics calculation"() {
        expect:
        //calculate solution plan
        Set<String> failedSteps = actionsToTest.stream().map { pair -> pair.getKey() }.collect(Collectors.toSet())

        Pair<List<List<ImmutablePair<String, Boolean>>>, List<List<ImmutablePair<String, Boolean>>>> compilePlanToCnf =
                SatSolver.compilePlanToCnf(sortedPlan, failedSteps);

        Pair<Map<String, Integer>, String> cnfEncoding =
                CnfEncodingUtils.encode(compilePlanToCnf.getLeft(), compilePlanToCnf.getRight());

        def diagnosedActions = SatSolver.runSatSolver(cnfEncoding.getRight(), cnfEncoding.getLeft());

        def actualFailedActions = actionsToTest.stream().map { pair ->
            Integer actionIndex = sortedPlan.entrySet().stream().
                    filter { entry ->
                        entry.getValue().stream().
                                map { step -> step.getUuid() }.
                                anyMatch { uuid -> uuid == pair.getValue().getUuid() }
                    }.
                    findFirst().get().getKey()

            CnfEncodingUtils.encodeHealthyStep(pair.getValue(), actionIndex)
        }.collect(Collectors.toList())

        if (diagnosedActions.sort() != actualFailedActions.sort()) {
            Set<Set<String>> allDiagnosis = Sets.newHashSet();

            SatSolver.getAllPossibleDiagnosis(compilePlanToCnf.getLeft(), compilePlanToCnf.getRight(), allDiagnosis)

            assert (allDiagnosis.contains(Sets.newHashSet(actualFailedActions)))
        } else {
            assert diagnosedActions.sort() == actualFailedActions.sort()
        }


        where:
        actionsToTest << new ActionDependencyCalculation(sortedPlan).getIndependentActionsList(3).stream().
        //skip(510).
                limit(1000).

                collect(Collectors.toList())

    }
}