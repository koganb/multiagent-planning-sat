package il.ac.bgu

import com.google.common.collect.ImmutableSet
import il.ac.bgu.dataModel.Action
import il.ac.bgu.failureModel.NoEffectFailureModel
import org.agreement_technologies.common.map_planner.Step
import org.apache.commons.lang3.SerializationUtils
import spock.lang.Shared
import spock.lang.Specification

import java.util.stream.Collectors

class TestCnfCompilation extends Specification {

    @Shared
    TreeMap<Integer, Set<Step>> sortedPlan

    @Shared
    CnfCompilation cnfCompilation

    def setupSpec() {
        sortedPlan = SerializationUtils.deserialize(new FileInputStream("deports0.problem.ser"))
    }

    def setup() {
        cnfCompilation = new CnfCompilation(sortedPlan, new NoEffectFailureModel())

    }


    def "test initial facts calculation"() {
        expect:
        cnfCompilation.calcInitFacts().stream()
                .map({ val -> val.toString() })
                .sorted()
                .collect(Collectors.toList()) == [
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
        ]
    }


    def "test final facts calculation"() {
        expect:
        //TODO check the possibility to convert predicates to the boolean fluents

        cnfCompilation.calcFinalFacts(ImmutableSet.of(
                Action.of("Load hoist0 crate1 truck1 depot0", "truck1", "912531260", 1))).stream()
                .map({ val -> val.toString() })
                .sorted()
                .collect(Collectors.toList()) == [
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
    }


    def "test that variables that exist in the effects are added to variable state"() {
        expect:
        cnfCompilation.executeStageAndAddFluents(0, sortedPlan.get(0))
                .map({ l -> l.stream().map({ v -> v.toString() }).sorted().collect(Collectors.joining(",")) })
                .sorted()
                .collect(Collectors.toList()) == [
                "{Stage:00, State:clear~crate1=false}=false",
                "{Stage:00, State:clear~hoist0=false}=false",
                "{Stage:00, State:clear~pallet0=true}=false",
                "{Stage:00, State:on~crate1=hoist0}=false",
        ]
    }

    def "test pass through clauses calculation"() {
        when:
        cnfCompilation.executeStageAndAddFluents(0, sortedPlan.get(0))
        then:
        cnfCompilation.calculatePassThroughClauses(0, sortedPlan.get(0))
                .map({ l -> l.stream().map({ v -> v.toString() }).sorted().collect(Collectors.joining(",")) })
                .sorted()
                .collect(Collectors.toList()) == [
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
    }


    def "test healthy clauses calculation"() {
        when:
        cnfCompilation.executeStageAndAddFluents(0, sortedPlan.get(0))
        then:
        cnfCompilation.calculateHealthyClauses(0, sortedPlan.get(0))
                .map({ l -> l.stream().map({ v -> v.toString() }).sorted().collect(Collectors.joining(",")) })
                .sorted()
                .collect(Collectors.toList()) == [
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=HEALTHY}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:clear~crate1=LOCKED_FOR_UPDATE}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=HEALTHY}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:clear~crate1=false}=true",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=HEALTHY}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:clear~crate1=true}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=HEALTHY}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:clear~hoist0=LOCKED_FOR_UPDATE}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=HEALTHY}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:clear~hoist0=false}=true",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=HEALTHY}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:clear~hoist0=true}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=HEALTHY}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:clear~pallet0=LOCKED_FOR_UPDATE}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=HEALTHY}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:clear~pallet0=false}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=HEALTHY}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:clear~pallet0=true}=true",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=HEALTHY}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:on~crate1=LOCKED_FOR_UPDATE}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=HEALTHY}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:on~crate1=hoist0}=true",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=HEALTHY}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false,{Stage:01, State:on~crate1=pallet0}=false",
        ]
    }

    def "test failed clauses calculation"() {
        when:
        cnfCompilation.executeStageAndAddFluents(0, sortedPlan.get(0))
        then:
        cnfCompilation.calculateActionFailedClauses(0, sortedPlan.get(0))
                .map({ l -> l.stream().map({ v -> v.toString() }).sorted().collect(Collectors.joining(",")) })
                .sorted()
                .collect(Collectors.toList()) == [
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

    }

    def "test condition not met clauses calculation"() {
        when:
        cnfCompilation.executeStageAndAddFluents(0, sortedPlan.get(0))
        then:
        cnfCompilation.calculateConditionsNotMetClauses(0, sortedPlan.get(0))
                .map({ l -> l.stream().map({ v -> v.toString() }).sorted().collect(Collectors.joining(",")) })
                .sorted()
                .collect(Collectors.toList()) == [
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=false,{Stage:01, State:clear~crate1=LOCKED_FOR_UPDATE}=true",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~crate1=true}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:clear~hoist0=true}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:on~crate1=pallet0}=false,{Stage:00, State:pos~crate1=depot0}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=true,{Stage:01, State:clear~crate1=LOCKED_FOR_UPDATE}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:clear~crate1=false}=false,{Stage:01, State:clear~crate1=false}=true",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:clear~crate1=false}=true,{Stage:01, State:clear~crate1=false}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:clear~crate1=true}=false,{Stage:01, State:clear~crate1=true}=true",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:clear~crate1=true}=true,{Stage:01, State:clear~crate1=true}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=false,{Stage:01, State:clear~hoist0=LOCKED_FOR_UPDATE}=true",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=true,{Stage:01, State:clear~hoist0=LOCKED_FOR_UPDATE}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:clear~hoist0=false}=false,{Stage:01, State:clear~hoist0=false}=true",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:clear~hoist0=false}=true,{Stage:01, State:clear~hoist0=false}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:clear~hoist0=true}=false,{Stage:01, State:clear~hoist0=true}=true",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:clear~hoist0=true}=true,{Stage:01, State:clear~hoist0=true}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=false,{Stage:01, State:clear~pallet0=LOCKED_FOR_UPDATE}=true",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=true,{Stage:01, State:clear~pallet0=LOCKED_FOR_UPDATE}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:clear~pallet0=false}=false,{Stage:01, State:clear~pallet0=false}=true",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:clear~pallet0=false}=true,{Stage:01, State:clear~pallet0=false}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:clear~pallet0=true}=false,{Stage:01, State:clear~pallet0=true}=true",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:clear~pallet0=true}=true,{Stage:01, State:clear~pallet0=true}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=false,{Stage:01, State:on~crate1=LOCKED_FOR_UPDATE}=true",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=true,{Stage:01, State:on~crate1=LOCKED_FOR_UPDATE}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:on~crate1=hoist0}=false,{Stage:01, State:on~crate1=hoist0}=true",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:on~crate1=hoist0}=true,{Stage:01, State:on~crate1=hoist0}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:on~crate1=pallet0}=false,{Stage:01, State:on~crate1=pallet0}=true",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=false,{Stage:00, State:on~crate1=pallet0}=true,{Stage:01, State:on~crate1=pallet0}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=true,{Stage:00, State:clear~crate1=LOCKED_FOR_UPDATE}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=true,{Stage:00, State:clear~crate1=true}=true",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=true,{Stage:00, State:clear~hoist0=LOCKED_FOR_UPDATE}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=true,{Stage:00, State:clear~hoist0=true}=true",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=true,{Stage:00, State:clear~pallet0=LOCKED_FOR_UPDATE}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=true,{Stage:00, State:on~crate1=LOCKED_FOR_UPDATE}=false",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=true,{Stage:00, State:on~crate1=pallet0}=true",
                "{Index:00, Agent:depot0,Action:LiftP~hoist0~crate1~pallet0~depot0=CONDITIONS_NOT_MET}=true,{Stage:00, State:pos~crate1=depot0}=true",

        ]
    }


}