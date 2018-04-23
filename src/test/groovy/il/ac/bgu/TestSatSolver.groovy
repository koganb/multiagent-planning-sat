package il.ac.bgu

import org.agreement_technologies.common.map_planner.Step
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
                Paths.get(this.getClass().getClassLoader().getResource("problems/elevator1.problem").toURI())).stream().
                flatMap({ t -> Arrays.stream(t.split("\t")) }).
                collect(Collectors.toList()).toArray(new String[0])

        sortedPlan = SatSolver.calculateSolution(agentDefs);

    }

    def "test diagnostics calculation"() {
        expect:
        //calculate solution plan
        def diagnosedActions = SatSolver.calculateDiagnostics(sortedPlan,
                actionsToTest.stream().map { pair -> pair.getKey() }.collect(Collectors.toSet()))
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
        assert diagnosedActions.sort() == actualFailedActions.sort()


        where:
        actionsToTest << new ActionDependencyCalculation(sortedPlan).getIndependentActionsList(1).stream().
                limit(100).
                collect(Collectors.toList())


    }
}