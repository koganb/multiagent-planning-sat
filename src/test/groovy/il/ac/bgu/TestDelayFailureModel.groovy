package il.ac.bgu

import il.ac.bgu.cnfClausesModel.conflict.ConflictNoEffectsCnfClauses
import il.ac.bgu.cnfClausesModel.failed.FailedDelayOneStepCnfClauses
import il.ac.bgu.cnfClausesModel.healthy.HealthyCnfClauses
import il.ac.bgu.dataModel.Action
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
                    Action.of("turn_to satellite1 star3 planet11", "satellite1", 0, FAILED),
                    Action.of("turn_to satellite4 planet5 star9", "satellite4", 0, FAILED),
                    Action.of("turn_to satellite3 star9 planet10", "satellite3", 0, FAILED),
                    Action.of("turn_to satellite0 phenomenon7 star0", "satellite0", 0, FAILED),
                    Action.of("turn_to satellite1 star3 phenomenon12", "satellite1", 14, FAILED),
                    Action.of("switch_off instrument4 satellite1", "satellite1", 13, FAILED),
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

        expect:
        assert TestUtils.checkSolution(plan, healthyCnfClausesCreator, conflictCnfClausesCreator,
                failedCnfClausesCreator, failedActions)

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


