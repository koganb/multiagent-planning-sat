package il.ac.bgu

import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import il.ac.bgu.cnfClausesModel.CnfClausesFunction
import il.ac.bgu.cnfClausesModel.conflict.ConflictNoEffectsCnfClauses
import il.ac.bgu.cnfClausesModel.failed.FailedDelayOneStepCnfClauses
import il.ac.bgu.cnfClausesModel.healthy.HealthyCnfClauses
import il.ac.bgu.cnfCompilation.retries.NoRetriesPlanUpdater
import il.ac.bgu.cnfCompilation.retries.OneRetryPlanUpdater
import il.ac.bgu.cnfCompilation.retries.RetryPlanUpdater
import il.ac.bgu.dataModel.Action
import il.ac.bgu.dataModel.Formattable
import il.ac.bgu.dataModel.FormattableValue
import il.ac.bgu.plan.PlanAction
import il.ac.bgu.sat.DiagnosisFindingStopIndicator
import il.ac.bgu.testUtils.PreparedTestActionReader
import il.ac.bgu.utils.PlanSolvingUtils
import il.ac.bgu.utils.PlanUtils
import il.ac.bgu.variableModel.DelayStageVariableFailureModel
import il.ac.bgu.variablesCalculation.ActionUtils
import il.ac.bgu.variablesCalculation.FinalVariableStateCalcImpl
import org.apache.commons.collections4.ListUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant
import java.util.stream.Collectors

import static il.ac.bgu.dataModel.Action.State.FAILED

@Unroll
class TestDelayFailureModelNoRetriesRaw extends Specification {

    private static final Logger log

    static {
        System.properties.'TEST_NAME' = 'DelayFailureModelNoRetriesParams'
        log = LoggerFactory.getLogger(TestDelayFailureModelNoRetries.class)
    }


    @Shared
    def problemArr =
            [1, 2, 3, 4, 7, 8, 10, 11, 13, 16, 17, 19].collect {"deports${it}"} +
            [1, 2, 3, 4, 5, 6, 7, 8, 9, 10 , 11, 12, 13, 14,15, 17].collect {"driverlog${it}"} +
            (1..30).collect {"elevator${it}"}+
            ["4-0", "4-1", "4-2", "5-0", "5-1", "5-2", "6-0", "6-1", "6-2", "7-0", "7-1", "7-2", "8-0", "8-1", "8-2", "9-1", "9-2"].collect {"ma-blocksworld${it}"} +
            (5..24).collect {"os-sequencedstrips-p${it}_1"} +
            (4.. 22).collect {"probLOGISTICS-${it}-0"} +
            (1.. 20).collect {"rovers${it}"} +
            [2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 20].collect {"satellite${it}"} +
            (1..28).collect {"woodworking${it}"} +
            (1..20).collect {"${it}".padLeft(2, "0")}.collect{"taxi_p${it}"} +
            ["2-4", "2-5", "2-6", "3-6", "3-7", "3-8", "3-10", "5-10", "5-15", "5-20", "5-25", "5-26"].collect {"ZTRAVEL-${it}"}

    public static final long SAT_TIMEOUT = 300L
    public static final DiagnosisFindingStopIndicator SOLUTION_STOP_IND =
            DiagnosisFindingStopIndicator.MINIMAL_SUBSET


    @Shared
    def planArr = problemArr.collect { PlanUtils.loadSerializedPlan("plans/${it}.problem.ser") }

    @Shared
    def planClausesCreationTime = [:]

    @Shared
    private CnfClausesFunction failedClausesCreator = new FailedDelayOneStepCnfClauses();

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


        def finalVariableStateCalc = new FinalVariableStateCalcImpl(plan, new DelayStageVariableFailureModel(1), conflictRetriesModel)

        expect:

        List<FormattableValue<? extends Formattable>> solutionFinalVariablesState = finalVariableStateCalc.getFinalVariableState(failedActions);

        if (ListUtils.intersection(finalVariableStateCalc.getFinalVariableState(Lists.newArrayList()), solutionFinalVariablesState).size() !=
                solutionFinalVariablesState.size()) {

            List<List<? extends Formattable>> solutions = PlanSolvingUtils.calculateSolutions(plan, cnfPlanClauses,
                    PlanUtils.encodeHealthyClauses(plan), finalVariableStateCalc, failedActions, SAT_TIMEOUT, SOLUTION_STOP_IND)
                    .stream().map { t -> t.flatten() }
                    .filter { t -> !t.isEmpty() }.collect(Collectors.toList())

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
        }

        where:
        [problemName, plan, cnfPlanClauses, failedActions] <<
                [[problemArr, planArr, cnfPlanClausesArr].transpose(), [1]]
                        .combinations()
                        .collect { [it[0][0], it[0][1], it[0][2], it[1]] }
                        .collect {
                    [it, PreparedTestActionReader.getTestActions(it[0], String.format("testCases/raw/%s.problem_%s.yml", it[0], it[3])), "DelayFailureModelNoRetriesParams"]
                }
                .collect {
                    [new Tuple(it[0][0]), new Tuple(it[0][1]), new Tuple(it[0][2]), it[1]].combinations()
                }.collectMany { it }
                        .collect { [it[0], it[1], it[2], it[3].collect { Action.of(it, Action.State.FAILED) }] }


    }

}
