package il.ac.bgu

import il.ac.bgu.cnfClausesModel.conflict.ConflictNoEffectsCnfClauses
import il.ac.bgu.cnfClausesModel.failed.FailedDelayOneStepCnfClauses
import il.ac.bgu.cnfClausesModel.healthy.HealthyCnfClauses
import il.ac.bgu.cnfCompilation.PlanUtils
import il.ac.bgu.cnfCompilation.retries.OneRetryPlanUpdater
import il.ac.bgu.dataModel.Action
import il.ac.bgu.variableModel.DelayStageVariableFailureModel
import il.ac.bgu.variablesCalculation.ActionUtils
import il.ac.bgu.variablesCalculation.FinalOneRetryVariableStateCalc
import il.ac.bgu.variablesCalculation.FinalVariableStateCalc
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static TestUtils.Problem
import static il.ac.bgu.dataModel.Action.State.FAILED

@Unroll
class TestDelayFailureModelWithRetries extends Specification {


    public static final int FAILED_STEPS_NUM = 1
    public static final int DELAY_STEPS_NUM = 1
    @Shared
    def problemArr = [
            new Problem("satellite8.problem", [
                    Action.of("turn_to satellite1 star0 star4", "satellite1", 0, FAILED),
                    Action.of("switch_on instrument7 satellite2", "satellite2", 0, FAILED),
                    Action.of("take_image satellite1 phenomenon14 instrument5 thermograph2", "satellite1", 10, FAILED),
                    Action.of("turn_to satellite1 phenomenon14 phenomenon13", "satellite1", 9, FAILED),

            ]),
    ]


    @Shared
    def planArr = problemArr.collect { TestUtils.loadPlan(it.problemName) }

    @Shared
    def cnfPlanClausesArr = planArr.collect { plan ->
        TestUtils.createPlanHardConstraints(plan, new OneRetryPlanUpdater(), new HealthyCnfClauses(),
                new ConflictNoEffectsCnfClauses(), new FailedDelayOneStepCnfClauses(), FAILED_STEPS_NUM)
    }

    def "test diagnostics calculation for plan: #problemName, failures: #failedActions "(
            problemName, plan, cnfPlanClauses, failedActions) {
        setup:
        println "Failed actions:" + failedActions
        TestUtils.printPlan(plan)

        assert ActionUtils.checkPlanContainsFailedActions(plan, failedActions)


        FinalVariableStateCalc finalVariableStateCalc = new FinalOneRetryVariableStateCalc(
                plan, new DelayStageVariableFailureModel(DELAY_STEPS_NUM))


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


