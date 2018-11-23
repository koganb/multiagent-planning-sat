package il.ac.bgu

import il.ac.bgu.cnfClausesModel.conflict.ConflictNoEffectsCnfClauses
import il.ac.bgu.cnfClausesModel.failed.FailedDelayOneStepCnfClauses
import il.ac.bgu.cnfClausesModel.healthy.HealthyCnfClauses
import il.ac.bgu.cnfCompilation.retries.OneRetryPlanUpdater
import il.ac.bgu.dataModel.Action
import il.ac.bgu.variablesCalculation.FinalOneRetryVariableStateCalc
import il.ac.bgu.variablesCalculation.FinalVariableStateCalc
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static TestUtils.Problem
import static il.ac.bgu.dataModel.Action.State.FAILED

@Unroll
class TestDelayFailureModelWithRetries extends Specification {

    @Shared
    def problemArr = [
            new Problem("satellite8.problem", [
                    Action.of("turn_to satellite1 star0 star4", "satellite1", 0, FAILED),
                    Action.of("switch_on instrument7 satellite2", "satellite2", 0, FAILED),
                    Action.of("take_image satellite1 phenomenon14 instrument5 thermograph2", "satellite1", 10, FAILED)

            ]),
    ]


    @Shared
    def planArr = problemArr.collect { TestUtils.loadPlan(it.problemName) }

    @Shared
    def healthyCnfClausesArr = (0..problemArr.size()).collect { new HealthyCnfClauses() }

    @Shared
    def conflictCnfClausesArr = (0..problemArr.size()).collect { new ConflictNoEffectsCnfClauses() }

    @Shared
    def failedCnfClausesArr = (0..problemArr.size()).collect { new FailedDelayOneStepCnfClauses() }


    def "test diagnostics calculation for plan: #problemName, failures: #failedActions "(
            problemName, plan, healthyCnfClausesCreator, conflictCnfClausesCreator, failedCnfClausesCreator, failedActions) {
        setup:
        println "Failed actions:" + failedActions
        TestUtils.printPlan(plan)

        FinalVariableStateCalc finalVariableStateCalc = new FinalOneRetryVariableStateCalc(
                plan, failedCnfClausesCreator.getVariableModel())



        expect:
        assert TestUtils.checkSolution(plan, new OneRetryPlanUpdater(), healthyCnfClausesCreator, conflictCnfClausesCreator,
                failedCnfClausesCreator, finalVariableStateCalc, failedActions)

        where:
        [problemName, plan, healthyCnfClausesCreator, conflictCnfClausesCreator, failedCnfClausesCreator, failedActions] << [
                problemArr,
                planArr,
                healthyCnfClausesArr,
                conflictCnfClausesArr,
                failedCnfClausesArr,
                planArr.collect { p ->
                    new ActionDependencyCalculation(p).getIndependentActionsList(1).collectNested {
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
            res -> [res[0], res[1], res[2], res[3], res[4], res[5][0]]
        }
        .findAll {
            res -> res[5].intersect(res[0].ignoreFailedActions) == []
        }
        .collect {
            res -> [res[0].problemName, res[1], res[2], res[3], res[4], res[5]]
        }
    }

}


