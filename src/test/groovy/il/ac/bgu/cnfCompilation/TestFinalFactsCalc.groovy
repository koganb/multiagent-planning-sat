package il.ac.bgu.cnfCompilation

import com.google.common.collect.ImmutableSet
import il.ac.bgu.dataModel.Action
import il.ac.bgu.failureModel.NoEffectFailureModel
import il.ac.bgu.failureModel.VariableModelFunction
import org.agreement_technologies.common.map_planner.Step
import org.apache.commons.lang3.SerializationUtils
import spock.lang.Specification

import java.util.stream.Collectors

class TestFinalFactsCalc extends Specification {

    def "test initial facts calculation"(String serializedPlanFilename,
                                         VariableModelFunction failureModel,
                                         Action failedAction,
                                         expectedClauses) {
        setup:
        TreeMap<Integer, Set<Step>> plan = SerializationUtils.deserialize(new FileInputStream(
                serializedPlanFilename))
        CnfCompilation cnfCompilation = new CnfCompilation(plan, failureModel)

        expect:
        cnfCompilation.calcFinalFacts(ImmutableSet.of(failedAction)).stream()
                .map({ val -> val.toString() })
                .sorted()
                .collect(Collectors.toList()) == expectedClauses

        where:
        serializedPlanFilename << ['deports0.problem.ser']
        failureModel << [new NoEffectFailureModel()]
        failedAction << [Action.of("Load hoist0 crate1 truck1 depot0", "truck1", "912531260", 1)]
        expectedClauses << [
                [
                        "{Stage:02, State:at~truck0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:02, State:at~truck0=distributor1}=true",
                        "{Stage:02, State:at~truck1=LOCKED_FOR_UPDATE}=false",
                        "{Stage:02, State:at~truck1=depot0}=true",
                        "{Stage:02, State:clear~crate0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:02, State:clear~crate0=true}=true",
                        "{Stage:02, State:clear~crate1=LOCKED_FOR_UPDATE}=false",
                        "{Stage:02, State:clear~crate1=false}=false",
                        "{Stage:02, State:clear~crate1=true}=true",
                        "{Stage:02, State:clear~hoist0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:02, State:clear~hoist0=false}=false",
                        "{Stage:02, State:clear~hoist0=true}=true",
                        "{Stage:02, State:clear~hoist1=LOCKED_FOR_UPDATE}=false",
                        "{Stage:02, State:clear~hoist1=true}=true",
                        "{Stage:02, State:clear~hoist2=LOCKED_FOR_UPDATE}=false",
                        "{Stage:02, State:clear~hoist2=true}=true",
                        "{Stage:02, State:clear~pallet0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:02, State:clear~pallet0=false}=false",
                        "{Stage:02, State:clear~pallet0=true}=true",
                        "{Stage:02, State:clear~pallet1=LOCKED_FOR_UPDATE}=false",
                        "{Stage:02, State:clear~pallet1=false}=true",
                        "{Stage:02, State:clear~pallet2=LOCKED_FOR_UPDATE}=false",
                        "{Stage:02, State:clear~pallet2=true}=true",
                        "{Stage:02, State:on~crate0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:02, State:on~crate0=pallet1}=true",
                        "{Stage:02, State:on~crate1=LOCKED_FOR_UPDATE}=false",
                        "{Stage:02, State:on~crate1=hoist0}=false",
                        "{Stage:02, State:on~crate1=pallet0}=false",
                        "{Stage:02, State:on~crate1=truck1}=true",
                        "{Stage:02, State:pos~crate0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:02, State:pos~crate0=distributor0}=true",
                        "{Stage:02, State:pos~crate1=LOCKED_FOR_UPDATE}=false",
                        "{Stage:02, State:pos~crate1=depot0}=false",
                        "{Stage:02, State:pos~crate1=truck1}=true",
                ]
        ]
    }


}