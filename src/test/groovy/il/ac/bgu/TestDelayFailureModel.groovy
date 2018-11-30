package il.ac.bgu

import il.ac.bgu.cnfClausesModel.conflict.ConflictNoEffectsCnfClauses
import il.ac.bgu.cnfClausesModel.failed.FailedDelayOneStepCnfClauses
import il.ac.bgu.cnfClausesModel.healthy.HealthyCnfClauses
import il.ac.bgu.cnfCompilation.PlanUtils
import il.ac.bgu.cnfCompilation.retries.NoRetriesPlanUpdater
import il.ac.bgu.dataModel.Action
import il.ac.bgu.variableModel.DelayStageVariableFailureModel
import il.ac.bgu.variablesCalculation.ActionUtils
import il.ac.bgu.variablesCalculation.FinalNoRetriesVariableStateCalc
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static TestUtils.Problem
import static il.ac.bgu.dataModel.Action.State.FAILED

@Unroll
class TestDelayFailureModel extends Specification {

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
    def cnfPlanClausesArr = planArr.collect { plan ->
        TestUtils.createPlanHardConstraints(plan, new NoRetriesPlanUpdater(), new HealthyCnfClauses(),
                new ConflictNoEffectsCnfClauses(), new FailedDelayOneStepCnfClauses())
    }



    def "test diagnostics calculation for plan: #problemName, failures: #failedActions "(
            problemName, plan, cnfPlanClauses, failedActions) {
        setup:
        println "Failed actions:" + failedActions
        TestUtils.printPlan(plan)

        assert ActionUtils.checkPlanContainsFailedActions(plan, failedActions)


        def finalVariableStateCalc = new FinalNoRetriesVariableStateCalc(plan, new DelayStageVariableFailureModel(1))

        expect:
        assert TestUtils.checkSolution(cnfPlanClauses, PlanUtils.encodeHealthyClauses(plan), finalVariableStateCalc, failedActions)


        where:
        [problemName, plan, cnfPlanClauses, failedActions] << [
                problemArr,
                planArr,
                cnfPlanClausesArr,
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


