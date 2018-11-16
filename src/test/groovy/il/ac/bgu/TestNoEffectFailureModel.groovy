package il.ac.bgu

import il.ac.bgu.dataModel.Action
import il.ac.bgu.failureModel.NoEffectFailureModel
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static TestUtils.Problem
import static il.ac.bgu.dataModel.Action.State.FAILED

@Unroll
class TestNoEffectFailureModel extends Specification {

    @Shared
    def problemArr = [
            new Problem("elevator29.problem"),
            new Problem("satellite20.problem"),
            new Problem("deports19.problem", [
                    Action.of("Unload hoist0 crate5 truck0 depot0", "truck0", 19, FAILED),
                    Action.of("Unload hoist1 crate3 truck0 depot1", "truck0", 13, FAILED),
            ]),
    ]


    @Shared
    def planArr = problemArr.collect { TestUtils.loadPlan(it.problemName) }

    @Shared
    def failedModelArr = (0..problemArr.size()).collect { new NoEffectFailureModel() }


    def "test diagnostics calculation for plan: #problemName, failures: #failedActions "(problemName, plan, failureModel, failedActions) {
        expect:
        println "Failed actions:" + failedActions

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


