package il.ac.bgu.cnfCompilation

import il.ac.bgu.failureModel.NoEffectFailureModel
import il.ac.bgu.failureModel.VariableModelFunction
import org.agreement_technologies.common.map_planner.Step
import org.apache.commons.lang3.SerializationUtils
import spock.lang.Specification
import spock.lang.Unroll

import java.util.stream.Collectors

@Unroll
class TestAddFluentsCalc extends Specification {


    def "test that variables that exist in the effects are added to variable state"(
            String serializedPlanFilename, VariableModelFunction failureModel, expectedClauses) {
        setup:
        TreeMap<Integer, Set<Step>> plan = SerializationUtils.deserialize(new FileInputStream(
                serializedPlanFilename))
        CnfCompilation cnfCompilation = new CnfCompilation(plan, failureModel)

        expect:
        cnfCompilation.executeStageAndAddFluents(0, plan.get(0))
                .map({ l -> l.stream().map({ v -> v.toString() }).sorted().collect(Collectors.joining(",")) })
                .sorted()
                .collect(Collectors.toList()) == expectedClauses


        where:
        serializedPlanFilename << ['deports0.problem.ser', 'elevator1.problem.ser', 'satellite1.problem.ser']
        failureModel << [new NoEffectFailureModel(), new NoEffectFailureModel(), new NoEffectFailureModel()]
        expectedClauses << [
                [
                        "{Stage:00, State:clear~crate1=false}=false",
                        "{Stage:00, State:clear~hoist0=false}=false",
                        "{Stage:00, State:clear~pallet0=true}=false",
                        "{Stage:00, State:on~crate1=hoist0}=false",
                ],
                [
                        "{Stage:00, State:at~p2=slow0-0}=false",
                        "{Stage:00, State:lift-at~fast0=n2}=false",
                        "{Stage:00, State:lift-at~slow1-0=n6}=false",
                        "{Stage:00, State:passengers~slow0-0=n1}=false",
                ],
                [
                        "{Stage:00, State:pointing~satellite0=groundstation2}=false",
                ]


        ]
    }


}