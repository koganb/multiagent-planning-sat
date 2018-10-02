package il.ac.bgu.cnfCompilation

import il.ac.bgu.failureModel.NoEffectFailureModel
import il.ac.bgu.failureModel.VariableModelFunction
import org.agreement_technologies.common.map_planner.Step
import org.apache.commons.lang3.SerializationUtils
import spock.lang.Specification

import java.util.stream.Collectors

class TestPassThroughClausesCalc extends Specification {


    def "test pass through clauses calculation"(
            String serializedPlanFilename, VariableModelFunction failureModel, expectedClauses) {
        setup:
        TreeMap<Integer, Set<Step>> plan = SerializationUtils.deserialize(new FileInputStream(
                serializedPlanFilename))
        CnfCompilation cnfCompilation = new CnfCompilation(plan, failureModel)

        when:
        cnfCompilation.executeStageAndAddFluents(0, plan.get(0))

        then:
        cnfCompilation.calculatePassThroughClauses(0, plan.get(0))
                .map({ l -> l.stream().map({ v -> v.toString() }).sorted().collect(Collectors.joining(",")) })
                .sorted()
                .collect(Collectors.toList()) == expectedClauses

        where:
        serializedPlanFilename << ['deports0.problem.ser']
        failureModel << [new NoEffectFailureModel()]
        expectedClauses << [
                [
                        "{Stage:00, State:at~truck0=LOCKED_FOR_UPDATE}=false,{Stage:01, State:at~truck0=LOCKED_FOR_UPDATE}=true",
                        "{Stage:00, State:at~truck0=LOCKED_FOR_UPDATE}=true,{Stage:01, State:at~truck0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:at~truck0=distributor1}=false,{Stage:01, State:at~truck0=distributor1}=true",
                        "{Stage:00, State:at~truck0=distributor1}=true,{Stage:01, State:at~truck0=distributor1}=false",
                        "{Stage:00, State:at~truck1=LOCKED_FOR_UPDATE}=false,{Stage:01, State:at~truck1=LOCKED_FOR_UPDATE}=true",
                        "{Stage:00, State:at~truck1=LOCKED_FOR_UPDATE}=true,{Stage:01, State:at~truck1=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:at~truck1=depot0}=false,{Stage:01, State:at~truck1=depot0}=true",
                        "{Stage:00, State:at~truck1=depot0}=true,{Stage:01, State:at~truck1=depot0}=false",
                        "{Stage:00, State:clear~crate0=LOCKED_FOR_UPDATE}=false,{Stage:01, State:clear~crate0=LOCKED_FOR_UPDATE}=true",
                        "{Stage:00, State:clear~crate0=LOCKED_FOR_UPDATE}=true,{Stage:01, State:clear~crate0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:clear~crate0=true}=false,{Stage:01, State:clear~crate0=true}=true",
                        "{Stage:00, State:clear~crate0=true}=true,{Stage:01, State:clear~crate0=true}=false",
                        "{Stage:00, State:clear~hoist1=LOCKED_FOR_UPDATE}=false,{Stage:01, State:clear~hoist1=LOCKED_FOR_UPDATE}=true",
                        "{Stage:00, State:clear~hoist1=LOCKED_FOR_UPDATE}=true,{Stage:01, State:clear~hoist1=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:clear~hoist1=true}=false,{Stage:01, State:clear~hoist1=true}=true",
                        "{Stage:00, State:clear~hoist1=true}=true,{Stage:01, State:clear~hoist1=true}=false",
                        "{Stage:00, State:clear~hoist2=LOCKED_FOR_UPDATE}=false,{Stage:01, State:clear~hoist2=LOCKED_FOR_UPDATE}=true",
                        "{Stage:00, State:clear~hoist2=LOCKED_FOR_UPDATE}=true,{Stage:01, State:clear~hoist2=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:clear~hoist2=true}=false,{Stage:01, State:clear~hoist2=true}=true",
                        "{Stage:00, State:clear~hoist2=true}=true,{Stage:01, State:clear~hoist2=true}=false",
                        "{Stage:00, State:clear~pallet1=LOCKED_FOR_UPDATE}=false,{Stage:01, State:clear~pallet1=LOCKED_FOR_UPDATE}=true",
                        "{Stage:00, State:clear~pallet1=LOCKED_FOR_UPDATE}=true,{Stage:01, State:clear~pallet1=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:clear~pallet1=false}=false,{Stage:01, State:clear~pallet1=false}=true",
                        "{Stage:00, State:clear~pallet1=false}=true,{Stage:01, State:clear~pallet1=false}=false",
                        "{Stage:00, State:clear~pallet2=LOCKED_FOR_UPDATE}=false,{Stage:01, State:clear~pallet2=LOCKED_FOR_UPDATE}=true",
                        "{Stage:00, State:clear~pallet2=LOCKED_FOR_UPDATE}=true,{Stage:01, State:clear~pallet2=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:clear~pallet2=true}=false,{Stage:01, State:clear~pallet2=true}=true",
                        "{Stage:00, State:clear~pallet2=true}=true,{Stage:01, State:clear~pallet2=true}=false",
                        "{Stage:00, State:on~crate0=LOCKED_FOR_UPDATE}=false,{Stage:01, State:on~crate0=LOCKED_FOR_UPDATE}=true",
                        "{Stage:00, State:on~crate0=LOCKED_FOR_UPDATE}=true,{Stage:01, State:on~crate0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:on~crate0=pallet1}=false,{Stage:01, State:on~crate0=pallet1}=true",
                        "{Stage:00, State:on~crate0=pallet1}=true,{Stage:01, State:on~crate0=pallet1}=false",
                        "{Stage:00, State:pos~crate0=LOCKED_FOR_UPDATE}=false,{Stage:01, State:pos~crate0=LOCKED_FOR_UPDATE}=true",
                        "{Stage:00, State:pos~crate0=LOCKED_FOR_UPDATE}=true,{Stage:01, State:pos~crate0=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:pos~crate0=distributor0}=false,{Stage:01, State:pos~crate0=distributor0}=true",
                        "{Stage:00, State:pos~crate0=distributor0}=true,{Stage:01, State:pos~crate0=distributor0}=false",
                        "{Stage:00, State:pos~crate1=LOCKED_FOR_UPDATE}=false,{Stage:01, State:pos~crate1=LOCKED_FOR_UPDATE}=true",
                        "{Stage:00, State:pos~crate1=LOCKED_FOR_UPDATE}=true,{Stage:01, State:pos~crate1=LOCKED_FOR_UPDATE}=false",
                        "{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:pos~crate1=depot0}=true",
                        "{Stage:00, State:pos~crate1=depot0}=true,{Stage:01, State:pos~crate1=depot0}=false",
                ]
        ]
    }


}