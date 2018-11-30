package il.ac.bgu

import il.ac.bgu.cnfClausesModel.conflict.ConflictNoEffectsCnfClauses
import il.ac.bgu.cnfClausesModel.failed.FailedNoEffectsCnfClauses
import il.ac.bgu.cnfClausesModel.healthy.HealthyCnfClauses
import il.ac.bgu.cnfCompilation.retries.NoRetriesPlanUpdater
import il.ac.bgu.variablesCalculation.FinalNoRetriesVariableStateCalc
import il.ac.bgu.variablesCalculation.FinalVariableStateCalc
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static TestUtils.Problem
import static il.ac.bgu.dataModel.Action.State.FAILED

@Unroll
class TestNoEffectVariableFailureModel extends Specification {

    @Shared
    def problemArr = [
            new Problem("elevator23.problem"),
//            new Problem("elevator29.problem"),
//            new Problem("satellite20.problem"),
//            new Problem("deports19.problem", [
//                    Action.of("Unload hoist0 crate5 truck0 depot0", "truck0", 19, FAILED),
//                    Action.of("Unload hoist1 crate3 truck0 depot1", "truck0", 13, FAILED),
//            ]),
    ]


    @Shared
    def planArr = problemArr.collect { TestUtils.loadPlan(it.problemName) }

    @Shared
    def healthyCnfClausesArr = (0..problemArr.size()).collect { new HealthyCnfClauses() }

    @Shared
    def conflictCnfClausesArr = (0..problemArr.size()).collect { new ConflictNoEffectsCnfClauses() }

    @Shared
    def failedCnfClausesArr = (0..problemArr.size()).collect { new FailedNoEffectsCnfClauses() }


    def "test diagnostics calculation for plan: #problemName, failures: #failedActions "(
            problemName, plan, healthyCnfClausesCreator, conflictCnfClausesCreator, failedCnfClausesCreator, failedActions) {
        setup:
        println "Failed actions:" + failedActions
        TestUtils.printPlan(plan)

        FinalVariableStateCalc finalVariableStateCalc = new FinalNoRetriesVariableStateCalc(
                plan, failedCnfClausesCreator.getVariableModel())


        expect:
        assert TestUtils.checkSolution(plan, new NoRetriesPlanUpdater(), healthyCnfClausesCreator, conflictCnfClausesCreator,
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


