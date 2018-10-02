package il.ac.bgu.cnfCompilation

import il.ac.bgu.failureModel.NoEffectFailureModel
import il.ac.bgu.failureModel.VariableModelFunction
import org.agreement_technologies.common.map_planner.Step
import org.apache.commons.lang3.SerializationUtils
import spock.lang.Specification

import java.util.stream.Collectors

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
        serializedPlanFilename << ['deports0.problem.ser']
        failureModel << [new NoEffectFailureModel()]
        expectedClauses << [
                [
                        "{Stage:00, State:clear~crate1=false}=false",
                        "{Stage:00, State:clear~hoist0=false}=false",
                        "{Stage:00, State:clear~pallet0=true}=false",
                        "{Stage:00, State:on~crate1=hoist0}=false",
                ]


        ]
    }


}