package il.ac.bgu

import groovy.util.logging.Slf4j
import il.ac.bgu.cnfClausesModel.conflict.ConflictNoEffectsCnfClauses
import il.ac.bgu.cnfClausesModel.failed.FailedNoEffectsCnfClauses
import il.ac.bgu.cnfClausesModel.healthy.HealthyCnfClauses
import il.ac.bgu.cnfCompilation.PlanUtils
import il.ac.bgu.cnfCompilation.retries.NoRetriesPlanUpdater
import il.ac.bgu.dataModel.Action
import il.ac.bgu.variableModel.NoEffectVariableFailureModel
import il.ac.bgu.variablesCalculation.ActionUtils
import il.ac.bgu.variablesCalculation.FinalNoRetriesVariableStateCalc
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static TestUtils.Problem
import static il.ac.bgu.dataModel.Action.State.FAILED

@Unroll
@Slf4j
class TestFullRegressionNoEffectVariableFailureModel extends Specification {

    public static final int FAILED_STEPS_NUM = 1
    @Shared
    def problemArr = [
            new Problem("elevator2.problem"),
            new Problem("elevator23.problem"),
            new Problem("elevator24.problem"),
            new Problem("elevator25.problem"),
            new Problem("elevator26.problem"),
            new Problem("elevator27.problem"),
            new Problem("elevator28.problem"),
            new Problem("elevator29.problem"),
            new Problem("elevator30.problem"),
            new Problem("satellite8.problem"),
            new Problem("satellite9.problem"),
            new Problem("satellite10.problem"),
            new Problem("satellite11.problem"),
            new Problem("satellite12.problem"),
            new Problem("satellite13.problem"),
            new Problem("satellite14.problem"),
            new Problem("satellite15.problem"),
            new Problem("satellite20.problem"),
            new Problem("deports4.problem", [
                    Action.of("DropC hoist0 crate7 crate4 depot0", "depot0", 1, FAILED),
                    Action.of("LiftC hoist0 crate7 crate4 depot0", "depot0", 0, FAILED),
            ]),
            new Problem("deports7.problem"),
            new Problem("deports8.problem"),
            new Problem("deports10.problem"),
            new Problem("deports11.problem", [
                    Action.of("Unload hoist1 crate7 truck0 depot1", "truck0", 29, FAILED),
                    Action.of("Unload hoist0 crate2 truck0 depot0", "truck0", 20, FAILED),
                    Action.of("Unload hoist0 crate1 truck0 depot0", "truck0", 18, FAILED),
                    Action.of("Unload hoist3 crate6 truck0 distributor0", "truck0", 25, FAILED),
            ]),
            new Problem("deports13.problem"),
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
    def cnfPlanClausesArr = [problemArr, planArr].transpose().collect { touple ->
        log.info("Start hard constarints compilation for plan {}", touple[0].problemName)

        def hardConstraints = TestUtils.createPlanHardConstraints(touple[1], new NoRetriesPlanUpdater(), new HealthyCnfClauses(),
                new ConflictNoEffectsCnfClauses(), new FailedNoEffectsCnfClauses(), FAILED_STEPS_NUM)

        log.info("End hard constarints compilation for plan {}", touple[0].problemName)

        return hardConstraints


    }


    def "test diagnostics calculation for plan: #problemName, failures: #failedActions "(
            problemName, plan, cnfPlanClauses, failedActions) {
        setup:
        println "Failed actions:" + failedActions
        TestUtils.printPlan(plan)

        assert ActionUtils.checkPlanContainsFailedActions(plan, failedActions)

        def finalVariableStateCalc = new FinalNoRetriesVariableStateCalc(plan, new NoEffectVariableFailureModel())

        expect:
        assert TestUtils.checkSolution(cnfPlanClauses, PlanUtils.encodeHealthyClauses(plan), finalVariableStateCalc, failedActions)


        where:
        [problemName, plan, cnfPlanClauses, failedActions] << [
                problemArr,
                planArr,
                cnfPlanClausesArr,
                planArr.collect { p ->
                    new ActionDependencyCalculation(p).getIndependentActionsList(FAILED_STEPS_NUM).collectNested {
                        action -> action.toBuilder().state(FAILED).build()
                    }
                }
        ]
                .transpose()
                .collectNested {
            [it]
        }
        .collect { it.combinations() }
                .collectMany { it }
                .collect {
            res -> [res[0], res[1], res[2], res[3][0]]
        }
        .findAll {
            res -> res[3].intersect(res[0].ignoreFailedActions) == []
        }
        .collect {
            res -> [res[0].problemName, res[1], res[2].get(), res[3]]
        }
    }

}


