package il.ac.bgu

import com.google.common.collect.ImmutableList
import com.google.common.collect.Streams
import il.ac.bgu.dataModel.Action
import il.ac.bgu.dataModel.Formattable
import il.ac.bgu.dataModel.FormattableValue
import il.ac.bgu.failureModel.NoEffectFailureModel
import il.ac.bgu.sat.SatSolutionSolver
import il.ac.bgu.sat.SolutionIterator
import org.agreement_technologies.common.map_planner.Step
import org.apache.commons.lang3.SerializationUtils
import org.apache.commons.lang3.tuple.Pair
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

@Unroll
class TestSatSolver extends Specification {

    //public static final String PROBLEM_NAME = "elevator1.problem"
    //public static final String PROBLEM_NAME = "elevator1.problem"
    public static final String PROBLEM_NAME = "satellite20.problem"
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

    def "test diagnostics calculation"(Set<Action> failedActions) {
        expect:
        def planningStartMils = System.currentTimeMillis()


        println "Failed actions:" + failedActions
        CnfCompilation cnfCompilation = new CnfCompilation(sortedPlan, new NoEffectFailureModel())
        def finalFactsWithFailedActions = new FinalVariableStateCalc(sortedPlan, new NoEffectFailureModel()).getFinalVariableState(failedActions)


        Pair<ImmutableList<ImmutableList<FormattableValue<Formattable>>>,
                ImmutableList<FormattableValue<Formattable>>> compilePlanToCnf =
                SatSolver.compilePlanToCnf(cnfCompilation, failedActions)


        def solutionIterator = new SolutionIterator(
                compilePlanToCnf.getLeft(), compilePlanToCnf.getRight(), new SatSolutionSolver())


        if (Streams.stream(solutionIterator).
                filter { solution -> solution.isPresent() }.
                map { solution -> solution.get() }.
                filter { solution ->

                    def solutionFinalState = cnfCompilation.calcFinalFacts(failedActions);

                    return (solutionFinalState.containsAll(finalFactsWithFailedActions) &&
                            finalFactsWithFailedActions.containsAll(solutionFinalState) &&
                            failedActions.containsAll(solution))
                }.findFirst().
                isPresent()) {
            assert true
        } else {
            assert false
        }



        cleanup:
        testTimeSum += (System.currentTimeMillis() - planningStartMils)

        where:
        failedActions << new ActionDependencyCalculation(sortedPlan).getIndependentActionsList(2).stream().

                collect(Collectors.toList())

    }
}