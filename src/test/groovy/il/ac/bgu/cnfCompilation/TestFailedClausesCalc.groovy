package il.ac.bgu.cnfCompilation

import il.ac.bgu.failureModel.NoEffectFailureModel
import il.ac.bgu.failureModel.VariableModelFunction
import org.agreement_technologies.common.map_planner.Step
import org.apache.commons.lang3.SerializationUtils
import spock.lang.Specification

import java.util.stream.Collectors

class TestFailedClausesCalc extends Specification {


    def "test failed clauses calculation"(
            String serializedPlanFilename, VariableModelFunction failureModel, expectedClauses) {
        setup:
        TreeMap<Integer, Set<Step>> plan = SerializationUtils.deserialize(new FileInputStream(
                serializedPlanFilename))
        CnfCompilation cnfCompilation = new CnfCompilation(plan, failureModel)

        when:
        cnfCompilation.executeStageAndAddFluents(0, plan.get(0))

        then:
        cnfCompilation.calculateActionFailedClauses(0, plan.get(0))
                .map({ l -> l.stream().map({ v -> v.toString() }).sorted().collect(Collectors.joining(",")) })
                .sorted()
                .collect(Collectors.toList()) == expectedClauses

        where:
        serializedPlanFilename << ['deports0.problem.ser']
        failureModel << [new NoEffectFailureModel()]
        expectedClauses << [
                [
                        "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=FAILED}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:clear~crate1=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=FAILED}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:clear~crate1=false}=false",
                        "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=FAILED}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:clear~crate1=true}=true",
                        "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=FAILED}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:clear~hoist0=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=FAILED}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:clear~hoist0=false}=false",
                        "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=FAILED}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:clear~hoist0=true}=true",
                        "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=FAILED}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:clear~pallet0=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=FAILED}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:clear~pallet0=false}=true",
                        "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=FAILED}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:clear~pallet0=true}=false",
                        "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=FAILED}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:on~crate1=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=FAILED}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:on~crate1=hoist0}=false",
                        "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=FAILED}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:on~crate1=pallet0}=true",
                ]
        ]
    }


}