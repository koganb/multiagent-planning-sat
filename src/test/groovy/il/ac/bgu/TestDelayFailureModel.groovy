package il.ac.bgu

import il.ac.bgu.cnfClausesModel.CnfClausesFunction
import il.ac.bgu.cnfClausesModel.conflict.ConflictNoEffectsCnfClauses
import il.ac.bgu.cnfClausesModel.failed.FailedDelayOneStepCnfClauses
import il.ac.bgu.cnfClausesModel.healthy.HealthyCnfClauses
import il.ac.bgu.cnfCompilation.retries.NoRetriesPlanUpdater
import il.ac.bgu.cnfCompilation.retries.RetryPlanUpdater
import il.ac.bgu.dataModel.Formattable
import il.ac.bgu.testUtils.ActionDependencyCalculation
import il.ac.bgu.utils.PlanSolvingUtils
import il.ac.bgu.utils.PlanUtils
import il.ac.bgu.variableModel.DelayStageVariableFailureModel
import il.ac.bgu.variablesCalculation.ActionUtils
import il.ac.bgu.variablesCalculation.FinalNoRetriesVariableStateCalc
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant
import java.util.stream.Collectors

import static TestUtils.Problem
import static il.ac.bgu.dataModel.Action.State.FAILED

@Unroll
class TestDelayFailureModel extends Specification {
    private static final Logger log

    static {
        System.properties.'TEST_NAME' = 'DelayFailureModel_1_failure'
        log = LoggerFactory.getLogger(TestDelayFailureModel.class)
    }


    @Shared
    def maxFailedActionsNumArr = [3]

    public static final int DELAY_STEPS_NUM = 1
    @Shared
    def problemArr = [
            new Problem("elevator28.problem"),
            new Problem("elevator29.problem"),
            new Problem("elevator30.problem"),
            new Problem("satellite14.problem"),
            new Problem("satellite15.problem"),
            new Problem("satellite20.problem"),
            new Problem("deports16.problem"),
            new Problem("deports17.problem"),
            new Problem("deports19.problem"),
    ]


    @Shared
    def planArr = problemArr.collect { TestUtils.loadPlan(it.problemName) }


    @Shared
    def planClausesCreationTime = [:]

    @Shared
    private CnfClausesFunction failedClausesCreator = new FailedDelayOneStepCnfClauses()

    @Shared
    private CnfClausesFunction conflictClausesCreator = new ConflictNoEffectsCnfClauses()

    @Shared
    private RetryPlanUpdater conflictRetriesModel = new NoRetriesPlanUpdater()

    @Shared
    private CnfClausesFunction healthyCnfClausesCreator = new HealthyCnfClauses()


    @Shared
    def cnfPlanClausesArr = [problemArr, planArr].transpose().collect { tuple ->
        Instant start = Instant.now()
        def constraints = TestUtils.createPlanHardConstraints(tuple[1], conflictRetriesModel, healthyCnfClausesCreator,
                conflictClausesCreator, failedClausesCreator)

        planClausesCreationTime[tuple[0].problemName] = Duration.between(start, Instant.now()).toMillis()

        return constraints;
    }


    @Shared
    //final variables state if no errors - to filter out failed actions that lead to 'normal' final state
    def normalFinalStateArr = planArr.collect { plan -> new FinalNoRetriesVariableStateCalc(plan, null).getFinalVariableState([]) }


    def "test diagnostics calculation for plan: #problemName, failures: #failedActions "(
            problemName, plan, cnfPlanClauses, failedActions) {
        setup:

        TestUtils.createStatsLogging(problemName, plan, planClausesCreationTime, failedActions, cnfPlanClauses,
                conflictRetriesModel, conflictClausesCreator, failedClausesCreator, failedActions.size())
        TestUtils.printPlan(plan)

        assert ActionUtils.checkPlanContainsFailedActions(plan, failedActions)


        def finalVariableStateCalc = new FinalNoRetriesVariableStateCalc(plan, new DelayStageVariableFailureModel(DELAY_STEPS_NUM))

        expect:
        List<List<Formattable>> solutions = PlanSolvingUtils.calculateSolutions(plan, cnfPlanClauses, PlanUtils.encodeHealthyClauses(plan), finalVariableStateCalc, failedActions)
                .filter { solution -> !solution.isEmpty() }
                .collect(Collectors.toList())


        log.info(MarkerFactory.getMarker("STATS"), "  solution: ")
        log.info(MarkerFactory.getMarker("STATS"), "    number_of_solutions: {}", solutions.size())

        def foundSolution = solutions.stream().filter { solution ->
            failedActions.stream()
                    .map { t -> t.toBuilder().state(FAILED).build() }
                    .collect(Collectors.toSet()).containsAll(solution)
        }
        .findFirst()
        assert foundSolution.isPresent()

        log.info(MarkerFactory.getMarker("STATS"), "    solution_index: {}", solutions.indexOf(foundSolution.get()))


        where:
        [problemName, plan, cnfPlanClauses, failedActions] << [
                problemArr,
                planArr,
                cnfPlanClausesArr,
                [planArr, normalFinalStateArr].transpose().collect { tuple ->
                    new ActionDependencyCalculation(tuple[0], tuple[1], failedClausesCreator.getVariableModel(), conflictRetriesModel).getIndependentActionsList(
                            maxFailedActionsNumArr)
                }

        ]
                .transpose()
                .collectNested {
            [it]
        }
        .collect { it.combinations() }
                .collectMany { it }
                .collect {
            res -> [res[0], res[1], res[2], res[3][0].get()]
        }
        .findAll {
            res -> res[3].intersect(res[0].ignoreFailedActions).size() == 0
        }
        .collect {
            res -> [res[0].problemName, res[1], res[2].get(), res[3]]
        }
    }

}


