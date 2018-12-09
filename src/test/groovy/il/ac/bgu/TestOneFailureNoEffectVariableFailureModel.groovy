package il.ac.bgu

import il.ac.bgu.cnfClausesModel.CnfClausesFunction
import il.ac.bgu.cnfClausesModel.conflict.ConflictNoEffectsCnfClauses
import il.ac.bgu.cnfClausesModel.failed.FailedNoEffectsCnfClauses
import il.ac.bgu.cnfClausesModel.healthy.HealthyCnfClauses
import il.ac.bgu.cnfCompilation.retries.NoRetriesPlanUpdater
import il.ac.bgu.cnfCompilation.retries.RetryPlanUpdater
import il.ac.bgu.dataModel.Action
import il.ac.bgu.dataModel.Formattable
import il.ac.bgu.testUtils.ActionDependencyCalculation
import il.ac.bgu.utils.PlanSolvingUtils
import il.ac.bgu.utils.PlanUtils
import il.ac.bgu.variableModel.NoEffectVariableFailureModel
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
class TestOneFailureNoEffectVariableFailureModel extends Specification {

    private static final Logger log

    static {
        System.properties.'TEST_NAME' = 'NoEffectsFailureModel_1_failure'
        log = LoggerFactory.getLogger(TestDelayFailureModel.class)
    }


    public static final int MAX_FAILED_ACTIONS_NUM = 1
    @Shared
    def problemArr = [
//            new Problem("elevator27.problem"),
new Problem("elevator28.problem"),
new Problem("elevator29.problem"),
new Problem("elevator30.problem"),
//            new Problem("satellite13.problem"),
new Problem("satellite14.problem"),
new Problem("satellite15.problem"),
new Problem("satellite20.problem"),
//            new Problem("deports4.problem", [
//                    Action.of("DropC hoist0 crate7 crate4 depot0", "depot0", 1, FAILED),
//                    Action.of("LiftC hoist0 crate7 crate4 depot0", "depot0", 0, FAILED),
//            ]),
//            new Problem("deports10.problem"),
//            new Problem("deports11.problem", [
//                    Action.of("Unload hoist1 crate7 truck0 depot1", "truck0", 29, FAILED),
//                    Action.of("Unload hoist0 crate2 truck0 depot0", "truck0", 20, FAILED),
//                    Action.of("Unload hoist0 crate1 truck0 depot0", "truck0", 18, FAILED),
//                    Action.of("Unload hoist3 crate6 truck0 distributor0", "truck0", 25, FAILED),
//            ]),
new Problem("deports16.problem"),
new Problem("deports17.problem", [
                    Action.of("Load hoist0 crate3 truck0 depot0", "truck0", 10, FAILED),
            ]),
new Problem("deports19.problem", [
                    Action.of("Unload hoist0 crate5 truck0 depot0", "truck0", 19, FAILED),
                    Action.of("Unload hoist1 crate3 truck0 depot1", "truck0", 13, FAILED),
            ]),
    ]



    @Shared
    def planArr = problemArr.collect { TestUtils.loadPlan(it.problemName) }

    @Shared
    def planClausesCreationTime = [:]

    @Shared
    private CnfClausesFunction failedClausesCreator = new FailedNoEffectsCnfClauses()

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
                conflictRetriesModel, conflictClausesCreator, failedClausesCreator, MAX_FAILED_ACTIONS_NUM)
        TestUtils.printPlan(plan)

        assert ActionUtils.checkPlanContainsFailedActions(plan, failedActions)


        def finalVariableStateCalc = new FinalNoRetriesVariableStateCalc(plan, new NoEffectVariableFailureModel())

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
                            MAX_FAILED_ACTIONS_NUM)
                }
        ]
                .transpose()
                .collectNested {
            [it]
        }
        .collect { it.combinations() }
                .collectMany { it }
                .collect {
            res -> [res[0], res[1], res[2].get(), res[3][0].get()]
        }
        .findAll {
            res -> res[3].intersect(res[0].ignoreFailedActions).size() == 0
        }
        .collect {
            res -> [res[0].problemName, res[1], res[2], res[3]]
        }
    }

}


