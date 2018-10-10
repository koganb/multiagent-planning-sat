package il.ac.bgu.cnfCompilation

import com.google.common.collect.ImmutableSet
import il.ac.bgu.dataModel.Action
import il.ac.bgu.failureModel.NoEffectFailureModel
import il.ac.bgu.failureModel.VariableModelFunction
import org.agreement_technologies.common.map_planner.Step
import org.apache.commons.lang3.SerializationUtils
import spock.lang.Specification
import spock.lang.Unroll

import java.util.stream.Collectors


@Unroll
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
        serializedPlanFilename << ['deports0.problem.ser', 'elevator1.problem.ser', 'satellite1.problem.ser']
        failureModel << [new NoEffectFailureModel(), new NoEffectFailureModel(), new NoEffectFailureModel()]
        failedAction << [
                Action.of("Load hoist0 crate1 truck1 depot0", "truck1", "912531260", 1),
                Action.of("move-down-slow slow1-0 n8 n4", "slow1-0", "1319841323", 1),
                Action.of("switch_on instrument0 satellite0", "satellite0", "1940263925", 1),
        ]
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
                ],
                [
                        "{Stage:10, State:at~p0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:10, State:at~p0=n4}=true",
                        "{Stage:10, State:at~p0=n8}=false",
                        "{Stage:10, State:at~p0=slow1-0}=false",
                        "{Stage:10, State:at~p1=LOCKED_FOR_UPDATE}=false",
                        "{Stage:10, State:at~p1=fast0}=false",
                        "{Stage:10, State:at~p1=n2}=false",
                        "{Stage:10, State:at~p1=n3}=false",
                        "{Stage:10, State:at~p1=n6}=true",
                        "{Stage:10, State:at~p1=slow0-0}=false",
                        "{Stage:10, State:at~p2=LOCKED_FOR_UPDATE}=false",
                        "{Stage:10, State:at~p2=n1}=true",
                        "{Stage:10, State:at~p2=n2}=false",
                        "{Stage:10, State:at~p2=slow0-0}=false",
                        "{Stage:10, State:lift-at~fast0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:10, State:lift-at~fast0=n0}=false",
                        "{Stage:10, State:lift-at~fast0=n2}=false",
                        "{Stage:10, State:lift-at~fast0=n6}=true",
                        "{Stage:10, State:lift-at~slow0-0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:10, State:lift-at~slow0-0=n1}=false",
                        "{Stage:10, State:lift-at~slow0-0=n2}=true",
                        "{Stage:10, State:lift-at~slow0-0=n3}=false",
                        "{Stage:10, State:lift-at~slow1-0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:10, State:lift-at~slow1-0=n4}=true",
                        "{Stage:10, State:lift-at~slow1-0=n6}=false",
                        "{Stage:10, State:lift-at~slow1-0=n8}=false",
                        "{Stage:10, State:passengers~fast0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:10, State:passengers~fast0=n0}=true",
                        "{Stage:10, State:passengers~fast0=n1}=false",
                        "{Stage:10, State:passengers~slow0-0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:10, State:passengers~slow0-0=n0}=true",
                        "{Stage:10, State:passengers~slow0-0=n1}=false",
                        "{Stage:10, State:passengers~slow1-0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:10, State:passengers~slow1-0=n0}=true",
                        "{Stage:10, State:passengers~slow1-0=n1}=false",
                ],
                [
                        "{Stage:02, State:calibrated~instrument0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:02, State:calibrated~instrument0=false}=true",
                        "{Stage:02, State:pointing~satellite0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:02, State:pointing~satellite0=groundstation2}=true",
                        "{Stage:02, State:pointing~satellite0=phenomenon6}=false",
                        "{Stage:02, State:power_avail~satellite0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:02, State:power_avail~satellite0=false}=true",
                        "{Stage:02, State:power_avail~satellite0=true}=false",
                        "{Stage:02, State:power_on~instrument0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:02, State:power_on~instrument0=false}=false",
                        "{Stage:02, State:power_on~instrument0=true}=true",
                ]
        ]
    }


}