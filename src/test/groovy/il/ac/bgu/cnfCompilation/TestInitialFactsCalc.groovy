package il.ac.bgu.cnfCompilation

import il.ac.bgu.failureModel.NoEffectFailureModel
import il.ac.bgu.failureModel.VariableModelFunction
import org.agreement_technologies.common.map_planner.Step
import org.apache.commons.lang3.SerializationUtils
import spock.lang.Specification
import spock.lang.Unroll

import java.util.stream.Collectors

@Unroll
class TestInitialFactsCalc extends Specification {


    def "test initial facts calculation"(String serializedPlanFilename,
                                         VariableModelFunction failureModel,
                                         expectedClauses) {
        setup:
        TreeMap<Integer, Set<Step>> plan = SerializationUtils.deserialize(new FileInputStream(
                serializedPlanFilename))
        CnfCompilation cnfCompilation = new CnfCompilation(plan, failureModel)

        expect:
        cnfCompilation.calcInitFacts().stream()
                .map({ val -> val.toString() })
                .sorted()
                .collect(Collectors.toList()) == expectedClauses

        where:
        serializedPlanFilename << ['deports0.problem.ser', 'elevator1.problem.ser', 'satellite1.problem.ser']
        failureModel << [new NoEffectFailureModel(), new NoEffectFailureModel(), new NoEffectFailureModel()]
        expectedClauses << [
                [
                        "{Stage:00, State:at~truck0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:at~truck0=distributor1}=true",
                        "{Stage:00, State:at~truck1=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:at~truck1=depot0}=true",
                        "{Stage:00, State:clear~crate0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:clear~crate0=true}=true",
                        "{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:clear~crate1=true}=true",
                        "{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:clear~hoist0=true}=true",
                        "{Stage:00, State:clear~hoist1=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:clear~hoist1=true}=true",
                        "{Stage:00, State:clear~hoist2=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:clear~hoist2=true}=true",
                        "{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:clear~pallet0=false}=true",
                        "{Stage:00, State:clear~pallet1=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:clear~pallet1=false}=true",
                        "{Stage:00, State:clear~pallet2=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:clear~pallet2=true}=true",
                        "{Stage:00, State:on~crate0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:on~crate0=pallet1}=true",
                        "{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:on~crate1=pallet0}=true",
                        "{Stage:00, State:pos~crate0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:pos~crate0=distributor0}=true",
                        "{Stage:00, State:pos~crate1=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:pos~crate1=depot0}=true",
                ],
                [
                        "{Stage:00, State:at~p0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:at~p0=n8}=true",
                        "{Stage:00, State:at~p1=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:at~p1=n3}=true",
                        "{Stage:00, State:at~p2=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:at~p2=n2}=true",
                        "{Stage:00, State:lift-at~fast0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:lift-at~fast0=n0}=true",
                        "{Stage:00, State:lift-at~slow0-0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:lift-at~slow0-0=n2}=true",
                        "{Stage:00, State:lift-at~slow1-0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:lift-at~slow1-0=n4}=true",
                        "{Stage:00, State:passengers~fast0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:passengers~fast0=n0}=true",
                        "{Stage:00, State:passengers~slow0-0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:passengers~slow0-0=n0}=true",
                        "{Stage:00, State:passengers~slow1-0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:passengers~slow1-0=n0}=true",
                ],
                [
                        "{Stage:00, State:calibrated~instrument0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:calibrated~instrument0=false}=true",
                        "{Stage:00, State:pointing~satellite0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:pointing~satellite0=phenomenon6}=true",
                        "{Stage:00, State:power_avail~satellite0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:power_avail~satellite0=true}=true",
                        "{Stage:00, State:power_on~instrument0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:power_on~instrument0=false}=true",

                ]


        ]
    }


}