package il.ac.bgu

import com.google.common.collect.ImmutableList
import il.ac.bgu.cnfClausesModel.CnfClausesFunction
import il.ac.bgu.cnfClausesModel.conflict.ConflictNoEffectsCnfClauses
import il.ac.bgu.cnfClausesModel.failed.FailedNoEffectsCnfClauses
import il.ac.bgu.cnfClausesModel.healthy.HealthyCnfClauses
import il.ac.bgu.cnfCompilation.retries.NoRetriesPlanUpdater
import il.ac.bgu.cnfCompilation.retries.OneRetryPlanUpdater
import il.ac.bgu.cnfCompilation.retries.RetryPlanUpdater
import il.ac.bgu.dataModel.Action
import il.ac.bgu.dataModel.Formattable
import il.ac.bgu.dataModel.FormattableValue
import il.ac.bgu.plan.PlanAction
import il.ac.bgu.sat.DiagnosisFindingStopIndicator
import il.ac.bgu.testUtils.ActionDependencyCalculation
import il.ac.bgu.testUtils.PreparedTestActionReader
import il.ac.bgu.utils.PlanSolvingUtils
import il.ac.bgu.utils.PlanUtils
import il.ac.bgu.variableModel.NoEffectVariableFailureModel
import il.ac.bgu.variablesCalculation.ActionUtils
import il.ac.bgu.variablesCalculation.FinalVariableStateCalcImpl
import io.vavr.control.Either
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
class TestNoEffectsFailureModelWithRetries extends Specification {

    private static final Logger log

    static {
        System.properties.'TEST_NAME' = 'NoEffectsFailureModel_OneRetry'
        log = LoggerFactory.getLogger(TestNoEffectsFailureModelWithRetries.class)
    }

    @Shared
    def maxFailedActionsNumArr = [1, 2, 3, 4, 5]

    @Shared
    def problemArr = [
            "deports11",
            "deports19",
            "elevator23",
            "elevator24",
            "elevator30",
//            "satellite20",
    ]

    public static final long SAT_TIMEOUT = 300L
    public static final DiagnosisFindingStopIndicator SOLUTION_STOP_IND =
            DiagnosisFindingStopIndicator.MINIMAL_CARDINALITY


    @Shared
    def planArr = problemArr.collect { PlanUtils.loadSerializedPlan("plans/${it}.problem.ser") }

    @Shared
    def planClausesCreationTime = [:]

    @Shared
    private CnfClausesFunction failedClausesCreator = new FailedNoEffectsCnfClauses()

    @Shared
    private CnfClausesFunction conflictClausesCreator = new ConflictNoEffectsCnfClauses()

    @Shared
    private RetryPlanUpdater conflictRetriesModel = new OneRetryPlanUpdater()

    @Shared
    private CnfClausesFunction healthyCnfClausesCreator = new HealthyCnfClauses()


    @Shared
    def cnfPlanClausesArr = [problemArr, planArr].transpose().collect { tuple ->
        Instant start = Instant.now()
        def constraints = TestUtils.createPlanHardConstraints(tuple[1], conflictRetriesModel, healthyCnfClausesCreator,
                conflictClausesCreator, failedClausesCreator)

        planClausesCreationTime[tuple[0]] = Duration.between(start, Instant.now()).toMillis()

        return constraints;
    }


    def "test diagnostics calculation for plan: #problemName, failures: #failedActions "(
            problemName, Map<Integer, ImmutableList<PlanAction>> plan,
            List<List<FormattableValue<? extends Formattable>>> cnfPlanClauses, List<Action> failedActions) {
        setup:

        TestUtils.createStatsLogging(problemName, plan, planClausesCreationTime, failedActions, cnfPlanClauses,
                conflictRetriesModel, conflictClausesCreator, failedClausesCreator, failedActions.size())
        TestUtils.printPlan(plan)

        assert ActionUtils.checkPlanContainsFailedActions(plan, failedActions)


        def finalVariableStateCalc = new FinalVariableStateCalcImpl(plan, new NoEffectVariableFailureModel(), conflictRetriesModel)

        expect:
        List<List<? extends Formattable>> solutions = PlanSolvingUtils.calculateSolutions(plan, cnfPlanClauses,
                PlanUtils.encodeHealthyClauses(plan), finalVariableStateCalc, failedActions, SAT_TIMEOUT, SOLUTION_STOP_IND)
                .stream().map { t -> t.flatten() }
                .filter {t -> !t.isEmpty() }.collect(Collectors.toList())


        log.info(MarkerFactory.getMarker("STATS"), "  solution: ")
        log.info(MarkerFactory.getMarker("STATS"), "    number_of_solutions: {}", solutions.size())

        assert solutions.size() > 0

        def foundSolution = solutions.stream()
                .filter { solution ->
            failedActions.stream()
                    .map { t -> t.toBuilder().state(FAILED).build() }
                    .collect(Collectors.toSet()).containsAll(solution)
        }.findFirst()
        assert foundSolution.isPresent()

        log.info(MarkerFactory.getMarker("STATS"), "    solution_index: {}", solutions.indexOf(foundSolution.get()))
        log.info(MarkerFactory.getMarker("STATS"), "    solution_cardinality: {}", foundSolution.get().size())


        where:
        [problemName, plan, cnfPlanClauses, failedActions] <<
                [[problemArr, planArr, cnfPlanClausesArr].transpose(), [1, 2, 3, 4]]
                        .combinations()
                        .collect { [it[0][0], it[0][1], it[0][2], it[1]] }
                        .collect {
                    [it, PreparedTestActionReader.getTestActions(it[0], it[3], "NoEffectsFailureModelWithRetriesParams")]
                }
                .collect {
                    [new Tuple(it[0][0]), new Tuple(it[0][1]), new Tuple(it[0][2]), it[1]].combinations()
                }.collectMany { it }
                        .collect { [it[0], it[1], it[2], it[3].collect { Action.of(it, Action.State.FAILED) }] }

    }

}


