package il.ac.bgu

import il.ac.bgu.cnfClausesModel.CnfClausesFunction
import il.ac.bgu.cnfClausesModel.conflict.ConflictNoEffectsCnfClauses
import il.ac.bgu.cnfClausesModel.failed.FailedDelayOneStepCnfClauses
import il.ac.bgu.cnfClausesModel.healthy.HealthyCnfClauses
import il.ac.bgu.cnfCompilation.retries.NoRetriesPlanUpdater
import il.ac.bgu.cnfCompilation.retries.RetryPlanUpdater
import il.ac.bgu.dataModel.Action
import il.ac.bgu.dataModel.Formattable
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


    public static final int MAX_FAILED_ACTIONS_NUM = 1
    public static final int DELAY_STEPS_NUM = 1
    @Shared
    def problemArr = [
            new Problem("deports0.problem"),
            new Problem("deports1.problem", [
                    Action.of("LiftP hoist1 crate0 pallet1 distributor0", "distributor0", 0, FAILED),
                    Action.of("DropP hoist1 crate1 pallet1 distributor0", "distributor0", 6, FAILED),
            ]),
            new Problem("deports2.problem", [
                    Action.of("LiftC hoist2 crate2 crate1 distributor1", "distributor1", 0, FAILED),
                    Action.of("DropP hoist2 crate0 pallet2 distributor1", "distributor1", 7, FAILED),
                    Action.of("LiftC hoist2 crate2 crate1 distributor1", "distributor0", 7, FAILED),
                    Action.of("DropC hoist1 crate1 crate3 distributor0", "distributor0", 9, FAILED),

            ]),
            new Problem("elevator1.problem", [
                    Action.of("move-up-fast fast0 n0 n2", "fast0", 0, FAILED),
                    Action.of("leave p0 slow1-0 n4 n1 n0", "slow1-0", 4, FAILED),
            ]),
            new Problem("elevator2.problem", [
                    Action.of("leave p2 slow0-0 n1 n1 n0", "slow0-0", 2, FAILED)
            ]),
            new Problem("satellite1.problem", [
                    Action.of("turn_to satellite0 groundstation2 phenomenon6", "satellite0", 0, FAILED)
            ]),
            new Problem("satellite8.problem", [
                    Action.of("turn_to satellite1 star0 star4", "satellite1", 0, FAILED),
                    Action.of("switch_on instrument7 satellite2", "satellite2", 0, FAILED),
                    Action.of("take_image satellite1 phenomenon14 instrument5 thermograph2", "satellite1", 10, FAILED)

            ]),
            new Problem("satellite9.problem", [
                    Action.of("turn_to satellite0 phenomenon7 star0", "satellite0", 0, FAILED),
                    Action.of("turn_to satellite4 planet5 star9", "satellite4", 0, FAILED),
                    Action.of("turn_to satellite3 star9 planet10", "satellite3", 0, FAILED),
            ]),
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


    def "test diagnostics calculation for plan: #problemName, failures: #failedActions "(
            problemName, plan, cnfPlanClauses, failedActions) {
        setup:

        TestUtils.createStatsLogging(problemName, plan, planClausesCreationTime, failedActions, cnfPlanClauses,
                conflictRetriesModel, conflictClausesCreator, failedClausesCreator, MAX_FAILED_ACTIONS_NUM)
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
                planArr.collect { p -> new ActionDependencyCalculation(p).getIndependentActionsList(MAX_FAILED_ACTIONS_NUM) }
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
            res -> res[3].intersect(res[0].ignoreFailedActions) == []
        }
        .collect {
            res -> [res[0].problemName, res[1], res[2].get(), res[3]]
        }
    }

}


