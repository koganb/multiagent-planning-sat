package il.ac.bgu


import com.google.common.collect.Streams
import il.ac.bgu.cnfCompilation.CnfCompilation
import il.ac.bgu.dataModel.Action
import il.ac.bgu.dataModel.Formattable
import il.ac.bgu.dataModel.FormattableValue
import il.ac.bgu.sat.SatSolutionSolver
import il.ac.bgu.sat.SolutionIterator
import il.ac.bgu.variableModel.DelayStageVariableFailureModel
import il.ac.bgu.variablesCalculation.FinalNoRetriesVariableStateCalc
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

    //public static final String PROBLEM_NAME = "elevator30.problem"
    public static final String PROBLEM_NAME = "elevator1.problem"
    //public static final String PROBLEM_NAME = "satellite20.problem"
    //public static final String PROBLEM_NAME = "deports-1.problem"
    //public static final String PROBLEM_NAME = "deports1.problem"
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

        sortedPlan.entrySet().stream()
                .filter({ entry -> entry.key != -1 })
                .forEach({ entry ->
            printf("Step: %s\n", entry.key)
            entry.value.forEach({ step -> printf("\t%-13s: %s\n", step.agent, step) })
        })
    }


    def cleanupSpec() {
        println "TestTime summary: " + testTimeSum

    }

    def "test diagnostics calculation"(Set<Action> failedActions) {
        expect:
        def planningStartMils = System.currentTimeMillis()


        println "Failed actions:" + failedActions
        CnfCompilation cnfCompilation = new CnfCompilation(sortedPlan, new DelayStageVariableFailureModel(1))
        def finalFactsWithFailedActions = new FinalNoRetriesVariableStateCalc(sortedPlan, new DelayStageVariableFailureModel(1)).getFinalVariableState(failedActions)


        Pair<List<List<FormattableValue<Formattable>>>,
                List<FormattableValue<Formattable>>> compilePlanToCnf =
                SatSolver.compilePlanToCnf(cnfCompilation, failedActions)


        def solutionIterator = new SolutionIterator(
                compilePlanToCnf.getLeft(), compilePlanToCnf.getRight(), new SatSolutionSolver())


        if (Streams.stream(solutionIterator).
                filter { solution -> solution.isPresent() }.
                map { solution -> solution.get() }.
                filter { solution ->

                    def solutionFinalState = cnfCompilation.calcFinalFacts(failedActions)

                    println("Solution candidate: " + solution);

                    return (!solution.isEmpty() &&
                            solutionFinalState.containsAll(finalFactsWithFailedActions) &&
                            finalFactsWithFailedActions.containsAll(solutionFinalState) &&
                            failedActions.stream()
                                    .map({ t -> t.toBuilder().state(Action.State.FAILED).build() })
                                    .collect(Collectors.toSet()).containsAll(solution))
                }.findFirst().
                isPresent()) {
            assert true
        } else {
            assert false
        }



        cleanup:
        testTimeSum += (System.currentTimeMillis() - planningStartMils)

        where:
        failedActions << new ActionDependencyCalculation(sortedPlan).getIndependentActionsList(1).stream()
                .map({ actionList ->
            actionList.stream()
                    .map({ action -> action.toBuilder().state(Action.State.FAILED).build() }).collect(Collectors.toSet())
        })
        //.skip(14)
        //        .limit(1)
                .collect(Collectors.toList())

    }
}


