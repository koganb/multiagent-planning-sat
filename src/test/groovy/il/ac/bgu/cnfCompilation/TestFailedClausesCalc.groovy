package il.ac.bgu.cnfCompilation

import il.ac.bgu.failureModel.NoEffectFailureModel
import il.ac.bgu.failureModel.VariableModelFunction
import org.agreement_technologies.common.map_planner.Step
import org.apache.commons.lang3.SerializationUtils
import spock.lang.Specification
import spock.lang.Unroll

import java.util.stream.Collectors


@Unroll
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
        serializedPlanFilename << [
                'deports0.problem.ser',
                'elevator1.problem.ser',
                'satellite1.problem.ser',
                'satellite20.problem.ser'
        ]
        failureModel << [
                new NoEffectFailureModel(),
                new NoEffectFailureModel(),
                new NoEffectFailureModel(),
                new NoEffectFailureModel()
        ]
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
                ],
                [
                        "{Index:00, Agent:fast0,Action:move-up-fast~fast0~n0~n2=FAILED}=false,{Stage:00, State:lift-at~fast0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:lift-at~fast0=n0}=false,{Stage:01, State:lift-at~fast0=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:fast0,Action:move-up-fast~fast0~n0~n2=FAILED}=false,{Stage:00, State:lift-at~fast0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:lift-at~fast0=n0}=false,{Stage:01, State:lift-at~fast0=n0}=true",
                        "{Index:00, Agent:fast0,Action:move-up-fast~fast0~n0~n2=FAILED}=false,{Stage:00, State:lift-at~fast0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:lift-at~fast0=n0}=false,{Stage:01, State:lift-at~fast0=n2}=false",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=FAILED}=false,{Stage:00, State:at~p2=LOCKED_FOR_UPDATE}=true,{Stage:00, State:at~p2=n2}=false,{Stage:00, State:lift-at~slow0-0=n2}=false,{Stage:00, State:passengers~slow0-0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:passengers~slow0-0=n0}=false,{Stage:01, State:at~p2=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=FAILED}=false,{Stage:00, State:at~p2=LOCKED_FOR_UPDATE}=true,{Stage:00, State:at~p2=n2}=false,{Stage:00, State:lift-at~slow0-0=n2}=false,{Stage:00, State:passengers~slow0-0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:passengers~slow0-0=n0}=false,{Stage:01, State:at~p2=n2}=true",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=FAILED}=false,{Stage:00, State:at~p2=LOCKED_FOR_UPDATE}=true,{Stage:00, State:at~p2=n2}=false,{Stage:00, State:lift-at~slow0-0=n2}=false,{Stage:00, State:passengers~slow0-0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:passengers~slow0-0=n0}=false,{Stage:01, State:at~p2=slow0-0}=false",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=FAILED}=false,{Stage:00, State:at~p2=LOCKED_FOR_UPDATE}=true,{Stage:00, State:at~p2=n2}=false,{Stage:00, State:lift-at~slow0-0=n2}=false,{Stage:00, State:passengers~slow0-0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:passengers~slow0-0=n0}=false,{Stage:01, State:passengers~slow0-0=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=FAILED}=false,{Stage:00, State:at~p2=LOCKED_FOR_UPDATE}=true,{Stage:00, State:at~p2=n2}=false,{Stage:00, State:lift-at~slow0-0=n2}=false,{Stage:00, State:passengers~slow0-0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:passengers~slow0-0=n0}=false,{Stage:01, State:passengers~slow0-0=n0}=true",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=FAILED}=false,{Stage:00, State:at~p2=LOCKED_FOR_UPDATE}=true,{Stage:00, State:at~p2=n2}=false,{Stage:00, State:lift-at~slow0-0=n2}=false,{Stage:00, State:passengers~slow0-0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:passengers~slow0-0=n0}=false,{Stage:01, State:passengers~slow0-0=n1}=false",
                        "{Index:00, Agent:slow1-0,Action:move-up-slow~slow1-0~n4~n6=FAILED}=false,{Stage:00, State:lift-at~slow1-0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:lift-at~slow1-0=n4}=false,{Stage:01, State:lift-at~slow1-0=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:slow1-0,Action:move-up-slow~slow1-0~n4~n6=FAILED}=false,{Stage:00, State:lift-at~slow1-0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:lift-at~slow1-0=n4}=false,{Stage:01, State:lift-at~slow1-0=n4}=true",
                        "{Index:00, Agent:slow1-0,Action:move-up-slow~slow1-0~n4~n6=FAILED}=false,{Stage:00, State:lift-at~slow1-0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:lift-at~slow1-0=n4}=false,{Stage:01, State:lift-at~slow1-0=n6}=false",
                ],
                [
                        "{Index:00, Agent:satellite0,Action:turn_to~satellite0~groundstation2~phenomenon6=FAILED}=false,{Stage:00, State:pointing~satellite0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:pointing~satellite0=phenomenon6}=false,{Stage:01, State:pointing~satellite0=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:satellite0,Action:turn_to~satellite0~groundstation2~phenomenon6=FAILED}=false,{Stage:00, State:pointing~satellite0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:pointing~satellite0=phenomenon6}=false,{Stage:01, State:pointing~satellite0=groundstation2}=false",
                        "{Index:00, Agent:satellite0,Action:turn_to~satellite0~groundstation2~phenomenon6=FAILED}=false,{Stage:00, State:pointing~satellite0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:pointing~satellite0=phenomenon6}=false,{Stage:01, State:pointing~satellite0=phenomenon6}=true",
                ],
                [
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=FAILED}=false,{Stage:00, State:calibrated~instrument11=LOCKED_FOR_UPDATE}=true,{Stage:00, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:power_avail~satellite1=true}=false,{Stage:00, State:power_on~instrument11=LOCKED_FOR_UPDATE}=true,{Stage:01, State:calibrated~instrument11=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=FAILED}=false,{Stage:00, State:calibrated~instrument11=LOCKED_FOR_UPDATE}=true,{Stage:00, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:power_avail~satellite1=true}=false,{Stage:00, State:power_on~instrument11=LOCKED_FOR_UPDATE}=true,{Stage:01, State:calibrated~instrument11=false}=true",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=FAILED}=false,{Stage:00, State:calibrated~instrument11=LOCKED_FOR_UPDATE}=true,{Stage:00, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:power_avail~satellite1=true}=false,{Stage:00, State:power_on~instrument11=LOCKED_FOR_UPDATE}=true,{Stage:01, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=FAILED}=false,{Stage:00, State:calibrated~instrument11=LOCKED_FOR_UPDATE}=true,{Stage:00, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:power_avail~satellite1=true}=false,{Stage:00, State:power_on~instrument11=LOCKED_FOR_UPDATE}=true,{Stage:01, State:power_avail~satellite1=false}=false",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=FAILED}=false,{Stage:00, State:calibrated~instrument11=LOCKED_FOR_UPDATE}=true,{Stage:00, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:power_avail~satellite1=true}=false,{Stage:00, State:power_on~instrument11=LOCKED_FOR_UPDATE}=true,{Stage:01, State:power_avail~satellite1=true}=true",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=FAILED}=false,{Stage:00, State:calibrated~instrument11=LOCKED_FOR_UPDATE}=true,{Stage:00, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:power_avail~satellite1=true}=false,{Stage:00, State:power_on~instrument11=LOCKED_FOR_UPDATE}=true,{Stage:01, State:power_on~instrument11=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=FAILED}=false,{Stage:00, State:calibrated~instrument11=LOCKED_FOR_UPDATE}=true,{Stage:00, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:power_avail~satellite1=true}=false,{Stage:00, State:power_on~instrument11=LOCKED_FOR_UPDATE}=true,{Stage:01, State:power_on~instrument11=false}=true",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=FAILED}=false,{Stage:00, State:calibrated~instrument11=LOCKED_FOR_UPDATE}=true,{Stage:00, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:power_avail~satellite1=true}=false,{Stage:00, State:power_on~instrument11=LOCKED_FOR_UPDATE}=true,{Stage:01, State:power_on~instrument11=true}=false",
                        "{Index:00, Agent:satellite3,Action:turn_to~satellite3~star2~star10=FAILED}=false,{Stage:00, State:pointing~satellite3=LOCKED_FOR_UPDATE}=true,{Stage:00, State:pointing~satellite3=star10}=false,{Stage:01, State:pointing~satellite3=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:satellite3,Action:turn_to~satellite3~star2~star10=FAILED}=false,{Stage:00, State:pointing~satellite3=LOCKED_FOR_UPDATE}=true,{Stage:00, State:pointing~satellite3=star10}=false,{Stage:01, State:pointing~satellite3=star10}=true",
                        "{Index:00, Agent:satellite3,Action:turn_to~satellite3~star2~star10=FAILED}=false,{Stage:00, State:pointing~satellite3=LOCKED_FOR_UPDATE}=true,{Stage:00, State:pointing~satellite3=star10}=false,{Stage:01, State:pointing~satellite3=star2}=false",
                        "{Index:00, Agent:satellite4,Action:turn_to~satellite4~groundstation1~star16=FAILED}=false,{Stage:00, State:pointing~satellite4=LOCKED_FOR_UPDATE}=true,{Stage:00, State:pointing~satellite4=star16}=false,{Stage:01, State:pointing~satellite4=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:satellite4,Action:turn_to~satellite4~groundstation1~star16=FAILED}=false,{Stage:00, State:pointing~satellite4=LOCKED_FOR_UPDATE}=true,{Stage:00, State:pointing~satellite4=star16}=false,{Stage:01, State:pointing~satellite4=groundstation1}=false",
                        "{Index:00, Agent:satellite4,Action:turn_to~satellite4~groundstation1~star16=FAILED}=false,{Stage:00, State:pointing~satellite4=LOCKED_FOR_UPDATE}=true,{Stage:00, State:pointing~satellite4=star16}=false,{Stage:01, State:pointing~satellite4=star16}=true",

                ]

        ]
    }


}