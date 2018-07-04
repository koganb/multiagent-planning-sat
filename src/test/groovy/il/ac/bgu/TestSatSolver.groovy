package il.ac.bgu

import com.google.common.collect.ImmutableSet
import com.google.common.collect.Streams
import il.ac.bgu.failureModel.NoEffectFailureModel
import il.ac.bgu.sat.SatSolutionSolver
import il.ac.bgu.sat.SolutionIterator
import org.agreement_technologies.common.map_planner.Step
import org.apache.commons.lang3.SerializationUtils
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.Pair
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

import static il.ac.bgu.CnfEncodingUtils.ActionState.FAILED

@Unroll
class TestSatSolver extends Specification {

    public static final String PROBLEM_NAME = "elevator30.problem"
    //public static final String PROBLEM_NAME = "deports17.problem"
    //public static final String PROBLEM_NAME = "satellite20.problem"
    @Shared
    private TreeMap<Integer, Set<Step>> sortedPlan

    @Shared
    private Long testTimeSum = 0

    def setupSpec() {
        String[] agentDefs = Files.readAllLines(
                Paths.get(this.getClass().getClassLoader().getResource("problems/" + PROBLEM_NAME).toURI())).stream().
                flatMap({ t -> Arrays.stream(t.split("\t")) }).
                collect(Collectors.toList()).toArray(new String[0])

        def planningStartMils = System.currentTimeMillis()


        def serPlanFileName = PROBLEM_NAME + ".ser"
        if (!new File(serPlanFileName).exists()) {
            //if (true) {
            sortedPlan = SatSolver.calculateSolution(agentDefs)
            SerializationUtils.serialize(sortedPlan, new FileOutputStream(serPlanFileName))
            println "Planning time mils: " + (System.currentTimeMillis() - planningStartMils)
        } else {
            println "Loaded serialized plan: " + serPlanFileName
            sortedPlan = SerializationUtils.deserialize(new FileInputStream(serPlanFileName))
        }

    }


    def cleanupSpec() {
        println "TestTime summary: " + testTimeSum

    }

    def "test diagnostics calculation"() {
        expect:
        def planningStartMils = System.currentTimeMillis()


        def actionToUuid = sortedPlan.entrySet().stream().flatMap { entry ->
            entry.value.stream().
                    map { step -> ImmutablePair.of(entry.key, step) }
        }.
                collect(Collectors.toMap(
                        { Pair pair -> CnfEncodingUtils.encodeActionKey(pair.getValue(), pair.getKey(), FAILED) },
                        { Pair pair -> pair.value.getUuid() }))

        ImmutableSet<String> actualFailedActions = actionsToTest.stream().map { pair ->
            Integer actionIndex = sortedPlan.entrySet().stream().
                    filter { entry ->
                        entry.getValue().stream().
                                map { step -> step.getUuid() }.
                                anyMatch { uuid -> uuid == pair.getValue().getUuid() }
                    }.
                    findFirst().get().getKey()

            CnfEncodingUtils.encodeActionState(pair.getValue(), actionIndex, FAILED, true).getLeft()
        }.collect(ImmutableSet.toImmutableSet())

        println "Failed actions:" + actualFailedActions
        CnfCompilation cnfCompilation = new CnfCompilation(sortedPlan, new NoEffectFailureModel())


        actionsToTest.stream().map { pair -> pair.getKey() }.collect(Collectors.toSet())
        def finalFactsWithFailedActions = cnfCompilation.calcFinalFacts(
                actionsToTest.stream().map { pair -> pair.getKey() }.toArray { size -> new String[size] }).
                stream().
                flatMap { t -> t.stream() }.
                sorted().
                collect(ImmutableSet.toImmutableSet())
        def finalFactsWithoutFailedActions = cnfCompilation.calcFinalFacts().
                stream().
                flatMap { t -> t.stream() }.
                sorted().
                collect(ImmutableSet.toImmutableSet())

        if (finalFactsWithFailedActions != finalFactsWithoutFailedActions) {

            //calculate solution plan
            Set<String> failedSteps = actionsToTest.stream().map { pair -> pair.getKey() }.collect(Collectors.toSet())

            Pair<ImmutableSet<ImmutableSet<ImmutablePair<String, Boolean>>>,
                    ImmutableSet<ImmutableSet<ImmutablePair<String, Boolean>>>> compilePlanToCnf =
                    SatSolver.compilePlanToCnf(cnfCompilation, failedSteps)


            def solutionIterator = new SolutionIterator(
                    compilePlanToCnf.getLeft(), compilePlanToCnf.getRight(), new SatSolutionSolver())


            if (Streams.stream(solutionIterator).
                    filter { solution -> solution.isPresent() }.
                    map { solution -> solution.get() }.
                    filter { solution ->
                        def solutionUuids = solution.stream().map { solutionClause -> actionToUuid.get(solutionClause) }.toArray { size -> new String[size] }

                        def solutionFinalState = cnfCompilation.calcFinalFacts(solutionUuids).
                                stream().
                                flatMap { t -> t.stream() }.
                                sorted().
                                collect(ImmutableSet.toImmutableSet())

                        return (solutionFinalState == finalFactsWithFailedActions &&
                                actualFailedActions.containsAll(solution))
                    }.findFirst().
                    isPresent()) {
                assert true
            } else {
                assert false
            }
        }



        cleanup:
        testTimeSum += (System.currentTimeMillis() - planningStartMils)

        where:
        actionsToTest << new ActionDependencyCalculation(sortedPlan).getIndependentActionsList(1).stream().
        //skip(2).
                limit(100).

                collect(Collectors.toList())

    }
}