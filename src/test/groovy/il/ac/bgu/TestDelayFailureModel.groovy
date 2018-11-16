package il.ac.bgu

import il.ac.bgu.dataModel.Action
import il.ac.bgu.failureModel.DelayStageFailureModel
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static TestUtils.Problem
import static il.ac.bgu.dataModel.Action.State.FAILED

@Unroll
class TestDelayFailureModel extends Specification {

    @Shared
    def problemArr = [
            //new Problem("deports0.problem"),
            new Problem("deports1.problem", [
                    Action.of("LiftP hoist1 crate0 pallet1 distributor0", "distributor0", 0, FAILED),
                    Action.of("DropP hoist1 crate1 pallet1 distributor0", "distributor0", 6, FAILED),
            ]),
    ]


    @Shared
    def planArr = problemArr.collect { TestUtils.loadPlan(it.problemName) }

    @Shared
    def failedModelArr = (0..problemArr.size()).collect { new DelayStageFailureModel(1) }


    def "test diagnostics calculation for plan: #problemName, failures: #failedActions "(problemName, plan, failureModel, failedActions) {
        setup:
        println "Failed actions:" + failedActions
        TestUtils.printPlan(plan)

        expect:
        assert TestUtils.checkSolution(plan, failureModel, failedActions)

        where:
        [problemName, plan, failureModel, failedActions] << [
                problemArr,
                planArr,
                failedModelArr,
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
            res -> [res[0].problemName, res[1], res[2], res[3]]
        }
    }

}


