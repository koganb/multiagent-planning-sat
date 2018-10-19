package il.ac.bgu.cnfCompilation

import il.ac.bgu.failureModel.NoEffectFailureModel
import il.ac.bgu.failureModel.VariableModelFunction
import org.agreement_technologies.common.map_planner.Step
import org.apache.commons.lang3.SerializationUtils
import spock.lang.Specification
import spock.lang.Unroll

import java.util.stream.Collectors


@Unroll
class TestConditionsNotMetClausesCalc extends Specification {


    def "test condition not met clauses calculation"(
            String serializedPlanFilename, VariableModelFunction failureModel, Integer step, expectedClauses) {
        setup:
        TreeMap<Integer, Set<Step>> plan = SerializationUtils.deserialize(new FileInputStream(
                serializedPlanFilename))
        CnfCompilation cnfCompilation = new CnfCompilation(plan, failureModel)

        when:
        cnfCompilation.executeStageAndAddFluents(step, plan.get(step))

        then:
        cnfCompilation.calculateConditionsNotMetClauses(step, plan.get(step))
                .map({ l -> l.stream().map({ v -> v.toString() }).sorted().collect(Collectors.joining(",")) })
                .sorted()
                .collect(Collectors.toList()) == expectedClauses

        where:
        serializedPlanFilename << ['deports0.problem.ser',
                                   'elevator1.problem.ser',
                                   'satellite1.problem.ser',
                                   'satellite20.problem.ser',
                                   'satellite20.problem.ser',
                                   'satellite20.problem.ser',
                                   'satellite20.problem.ser',
                                   'satellite20.problem.ser',
                                   'satellite20.problem.ser',
                                   'satellite20.problem.ser',
        ]
        failureModel << [
                new NoEffectFailureModel(),
                new NoEffectFailureModel(),
                new NoEffectFailureModel(),
                new NoEffectFailureModel(),
                new NoEffectFailureModel(),
                new NoEffectFailureModel(),
                new NoEffectFailureModel(),
                new NoEffectFailureModel(),
                new NoEffectFailureModel(),
                new NoEffectFailureModel(),
        ]
        step << [0, 0, 0, 0, 10, 20, 30, 40, 50, 60]
        expectedClauses << [
                [
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

                ],
                [
                        "{Index:00, Agent:fast0,Action:move-up-fast~fast0~n0~n2=CONDITIONS_NOT_MET}=false,{Stage:00, State:lift-at~fast0=LOCKED_FOR_UPDATE}=false,{Stage:01, State:lift-at~fast0=LOCKED_FOR_UPDATE}=true",
                        "{Index:00, Agent:fast0,Action:move-up-fast~fast0~n0~n2=CONDITIONS_NOT_MET}=false,{Stage:00, State:lift-at~fast0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:lift-at~fast0=n0}=false",
                        "{Index:00, Agent:fast0,Action:move-up-fast~fast0~n0~n2=CONDITIONS_NOT_MET}=false,{Stage:00, State:lift-at~fast0=LOCKED_FOR_UPDATE}=true,{Stage:01, State:lift-at~fast0=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:fast0,Action:move-up-fast~fast0~n0~n2=CONDITIONS_NOT_MET}=false,{Stage:00, State:lift-at~fast0=n0}=false,{Stage:01, State:lift-at~fast0=n0}=true",
                        "{Index:00, Agent:fast0,Action:move-up-fast~fast0~n0~n2=CONDITIONS_NOT_MET}=false,{Stage:00, State:lift-at~fast0=n0}=true,{Stage:01, State:lift-at~fast0=n0}=false",
                        "{Index:00, Agent:fast0,Action:move-up-fast~fast0~n0~n2=CONDITIONS_NOT_MET}=false,{Stage:00, State:lift-at~fast0=n2}=false,{Stage:01, State:lift-at~fast0=n2}=true",
                        "{Index:00, Agent:fast0,Action:move-up-fast~fast0~n0~n2=CONDITIONS_NOT_MET}=false,{Stage:00, State:lift-at~fast0=n2}=true,{Stage:01, State:lift-at~fast0=n2}=false",
                        "{Index:00, Agent:fast0,Action:move-up-fast~fast0~n0~n2=CONDITIONS_NOT_MET}=true,{Stage:00, State:lift-at~fast0=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:fast0,Action:move-up-fast~fast0~n0~n2=CONDITIONS_NOT_MET}=true,{Stage:00, State:lift-at~fast0=n0}=true",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=CONDITIONS_NOT_MET}=false,{Stage:00, State:at~p2=LOCKED_FOR_UPDATE}=false,{Stage:01, State:at~p2=LOCKED_FOR_UPDATE}=true",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=CONDITIONS_NOT_MET}=false,{Stage:00, State:at~p2=LOCKED_FOR_UPDATE}=true,{Stage:00, State:at~p2=n2}=false,{Stage:00, State:lift-at~slow0-0=n2}=false,{Stage:00, State:passengers~slow0-0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:passengers~slow0-0=n0}=false",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=CONDITIONS_NOT_MET}=false,{Stage:00, State:at~p2=LOCKED_FOR_UPDATE}=true,{Stage:01, State:at~p2=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=CONDITIONS_NOT_MET}=false,{Stage:00, State:at~p2=n2}=false,{Stage:01, State:at~p2=n2}=true",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=CONDITIONS_NOT_MET}=false,{Stage:00, State:at~p2=n2}=true,{Stage:01, State:at~p2=n2}=false",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=CONDITIONS_NOT_MET}=false,{Stage:00, State:at~p2=slow0-0}=false,{Stage:01, State:at~p2=slow0-0}=true",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=CONDITIONS_NOT_MET}=false,{Stage:00, State:at~p2=slow0-0}=true,{Stage:01, State:at~p2=slow0-0}=false",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=CONDITIONS_NOT_MET}=false,{Stage:00, State:passengers~slow0-0=LOCKED_FOR_UPDATE}=false,{Stage:01, State:passengers~slow0-0=LOCKED_FOR_UPDATE}=true",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=CONDITIONS_NOT_MET}=false,{Stage:00, State:passengers~slow0-0=LOCKED_FOR_UPDATE}=true,{Stage:01, State:passengers~slow0-0=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=CONDITIONS_NOT_MET}=false,{Stage:00, State:passengers~slow0-0=n0}=false,{Stage:01, State:passengers~slow0-0=n0}=true",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=CONDITIONS_NOT_MET}=false,{Stage:00, State:passengers~slow0-0=n0}=true,{Stage:01, State:passengers~slow0-0=n0}=false",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=CONDITIONS_NOT_MET}=false,{Stage:00, State:passengers~slow0-0=n1}=false,{Stage:01, State:passengers~slow0-0=n1}=true",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=CONDITIONS_NOT_MET}=false,{Stage:00, State:passengers~slow0-0=n1}=true,{Stage:01, State:passengers~slow0-0=n1}=false",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=CONDITIONS_NOT_MET}=true,{Stage:00, State:at~p2=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=CONDITIONS_NOT_MET}=true,{Stage:00, State:at~p2=n2}=true",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=CONDITIONS_NOT_MET}=true,{Stage:00, State:lift-at~slow0-0=n2}=true",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=CONDITIONS_NOT_MET}=true,{Stage:00, State:passengers~slow0-0=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:slow0-0,Action:board~p2~slow0-0~n2~n0~n1=CONDITIONS_NOT_MET}=true,{Stage:00, State:passengers~slow0-0=n0}=true",
                        "{Index:00, Agent:slow1-0,Action:move-up-slow~slow1-0~n4~n6=CONDITIONS_NOT_MET}=false,{Stage:00, State:lift-at~slow1-0=LOCKED_FOR_UPDATE}=false,{Stage:01, State:lift-at~slow1-0=LOCKED_FOR_UPDATE}=true",
                        "{Index:00, Agent:slow1-0,Action:move-up-slow~slow1-0~n4~n6=CONDITIONS_NOT_MET}=false,{Stage:00, State:lift-at~slow1-0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:lift-at~slow1-0=n4}=false",
                        "{Index:00, Agent:slow1-0,Action:move-up-slow~slow1-0~n4~n6=CONDITIONS_NOT_MET}=false,{Stage:00, State:lift-at~slow1-0=LOCKED_FOR_UPDATE}=true,{Stage:01, State:lift-at~slow1-0=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:slow1-0,Action:move-up-slow~slow1-0~n4~n6=CONDITIONS_NOT_MET}=false,{Stage:00, State:lift-at~slow1-0=n4}=false,{Stage:01, State:lift-at~slow1-0=n4}=true",
                        "{Index:00, Agent:slow1-0,Action:move-up-slow~slow1-0~n4~n6=CONDITIONS_NOT_MET}=false,{Stage:00, State:lift-at~slow1-0=n4}=true,{Stage:01, State:lift-at~slow1-0=n4}=false",
                        "{Index:00, Agent:slow1-0,Action:move-up-slow~slow1-0~n4~n6=CONDITIONS_NOT_MET}=false,{Stage:00, State:lift-at~slow1-0=n6}=false,{Stage:01, State:lift-at~slow1-0=n6}=true",
                        "{Index:00, Agent:slow1-0,Action:move-up-slow~slow1-0~n4~n6=CONDITIONS_NOT_MET}=false,{Stage:00, State:lift-at~slow1-0=n6}=true,{Stage:01, State:lift-at~slow1-0=n6}=false",
                        "{Index:00, Agent:slow1-0,Action:move-up-slow~slow1-0~n4~n6=CONDITIONS_NOT_MET}=true,{Stage:00, State:lift-at~slow1-0=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:slow1-0,Action:move-up-slow~slow1-0~n4~n6=CONDITIONS_NOT_MET}=true,{Stage:00, State:lift-at~slow1-0=n4}=true",

                ],
                [
                        "{Index:00, Agent:satellite0,Action:turn_to~satellite0~groundstation2~phenomenon6=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite0=LOCKED_FOR_UPDATE}=false,{Stage:01, State:pointing~satellite0=LOCKED_FOR_UPDATE}=true",
                        "{Index:00, Agent:satellite0,Action:turn_to~satellite0~groundstation2~phenomenon6=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite0=LOCKED_FOR_UPDATE}=true,{Stage:00, State:pointing~satellite0=phenomenon6}=false",
                        "{Index:00, Agent:satellite0,Action:turn_to~satellite0~groundstation2~phenomenon6=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite0=LOCKED_FOR_UPDATE}=true,{Stage:01, State:pointing~satellite0=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:satellite0,Action:turn_to~satellite0~groundstation2~phenomenon6=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite0=groundstation2}=false,{Stage:01, State:pointing~satellite0=groundstation2}=true",
                        "{Index:00, Agent:satellite0,Action:turn_to~satellite0~groundstation2~phenomenon6=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite0=groundstation2}=true,{Stage:01, State:pointing~satellite0=groundstation2}=false",
                        "{Index:00, Agent:satellite0,Action:turn_to~satellite0~groundstation2~phenomenon6=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite0=phenomenon6}=false,{Stage:01, State:pointing~satellite0=phenomenon6}=true",
                        "{Index:00, Agent:satellite0,Action:turn_to~satellite0~groundstation2~phenomenon6=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite0=phenomenon6}=true,{Stage:01, State:pointing~satellite0=phenomenon6}=false",
                        "{Index:00, Agent:satellite0,Action:turn_to~satellite0~groundstation2~phenomenon6=CONDITIONS_NOT_MET}=true,{Stage:00, State:pointing~satellite0=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:satellite0,Action:turn_to~satellite0~groundstation2~phenomenon6=CONDITIONS_NOT_MET}=true,{Stage:00, State:pointing~satellite0=phenomenon6}=true",

                ],
                [
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=false,{Stage:00, State:calibrated~instrument11=LOCKED_FOR_UPDATE}=false,{Stage:01, State:calibrated~instrument11=LOCKED_FOR_UPDATE}=true",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=false,{Stage:00, State:calibrated~instrument11=LOCKED_FOR_UPDATE}=true,{Stage:00, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=true,{Stage:00, State:power_avail~satellite1=true}=false,{Stage:00, State:power_on~instrument11=LOCKED_FOR_UPDATE}=true",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=false,{Stage:00, State:calibrated~instrument11=LOCKED_FOR_UPDATE}=true,{Stage:01, State:calibrated~instrument11=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=false,{Stage:00, State:calibrated~instrument11=false}=false,{Stage:01, State:calibrated~instrument11=false}=true",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=false,{Stage:00, State:calibrated~instrument11=false}=true,{Stage:01, State:calibrated~instrument11=false}=false",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=false,{Stage:00, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=false,{Stage:01, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=true",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=false,{Stage:00, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=true,{Stage:01, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=false,{Stage:00, State:power_avail~satellite1=false}=false,{Stage:01, State:power_avail~satellite1=false}=true",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=false,{Stage:00, State:power_avail~satellite1=false}=true,{Stage:01, State:power_avail~satellite1=false}=false",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=false,{Stage:00, State:power_avail~satellite1=true}=false,{Stage:01, State:power_avail~satellite1=true}=true",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=false,{Stage:00, State:power_avail~satellite1=true}=true,{Stage:01, State:power_avail~satellite1=true}=false",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=false,{Stage:00, State:power_on~instrument11=LOCKED_FOR_UPDATE}=false,{Stage:01, State:power_on~instrument11=LOCKED_FOR_UPDATE}=true",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=false,{Stage:00, State:power_on~instrument11=LOCKED_FOR_UPDATE}=true,{Stage:01, State:power_on~instrument11=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=false,{Stage:00, State:power_on~instrument11=false}=false,{Stage:01, State:power_on~instrument11=false}=true",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=false,{Stage:00, State:power_on~instrument11=false}=true,{Stage:01, State:power_on~instrument11=false}=false",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=false,{Stage:00, State:power_on~instrument11=true}=false,{Stage:01, State:power_on~instrument11=true}=true",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=false,{Stage:00, State:power_on~instrument11=true}=true,{Stage:01, State:power_on~instrument11=true}=false",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=true,{Stage:00, State:calibrated~instrument11=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=true,{Stage:00, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=true,{Stage:00, State:power_avail~satellite1=true}=true",
                        "{Index:00, Agent:satellite1,Action:switch_on~instrument11~satellite1=CONDITIONS_NOT_MET}=true,{Stage:00, State:power_on~instrument11=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:satellite3,Action:turn_to~satellite3~star2~star10=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite3=LOCKED_FOR_UPDATE}=false,{Stage:01, State:pointing~satellite3=LOCKED_FOR_UPDATE}=true",
                        "{Index:00, Agent:satellite3,Action:turn_to~satellite3~star2~star10=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite3=LOCKED_FOR_UPDATE}=true,{Stage:00, State:pointing~satellite3=star10}=false",
                        "{Index:00, Agent:satellite3,Action:turn_to~satellite3~star2~star10=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite3=LOCKED_FOR_UPDATE}=true,{Stage:01, State:pointing~satellite3=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:satellite3,Action:turn_to~satellite3~star2~star10=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite3=star10}=false,{Stage:01, State:pointing~satellite3=star10}=true",
                        "{Index:00, Agent:satellite3,Action:turn_to~satellite3~star2~star10=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite3=star10}=true,{Stage:01, State:pointing~satellite3=star10}=false",
                        "{Index:00, Agent:satellite3,Action:turn_to~satellite3~star2~star10=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite3=star2}=false,{Stage:01, State:pointing~satellite3=star2}=true",
                        "{Index:00, Agent:satellite3,Action:turn_to~satellite3~star2~star10=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite3=star2}=true,{Stage:01, State:pointing~satellite3=star2}=false",
                        "{Index:00, Agent:satellite3,Action:turn_to~satellite3~star2~star10=CONDITIONS_NOT_MET}=true,{Stage:00, State:pointing~satellite3=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:satellite3,Action:turn_to~satellite3~star2~star10=CONDITIONS_NOT_MET}=true,{Stage:00, State:pointing~satellite3=star10}=true",
                        "{Index:00, Agent:satellite4,Action:turn_to~satellite4~groundstation1~star16=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite4=LOCKED_FOR_UPDATE}=false,{Stage:01, State:pointing~satellite4=LOCKED_FOR_UPDATE}=true",
                        "{Index:00, Agent:satellite4,Action:turn_to~satellite4~groundstation1~star16=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite4=LOCKED_FOR_UPDATE}=true,{Stage:00, State:pointing~satellite4=star16}=false",
                        "{Index:00, Agent:satellite4,Action:turn_to~satellite4~groundstation1~star16=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite4=LOCKED_FOR_UPDATE}=true,{Stage:01, State:pointing~satellite4=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:satellite4,Action:turn_to~satellite4~groundstation1~star16=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite4=groundstation1}=false,{Stage:01, State:pointing~satellite4=groundstation1}=true",
                        "{Index:00, Agent:satellite4,Action:turn_to~satellite4~groundstation1~star16=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite4=groundstation1}=true,{Stage:01, State:pointing~satellite4=groundstation1}=false",
                        "{Index:00, Agent:satellite4,Action:turn_to~satellite4~groundstation1~star16=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite4=star16}=false,{Stage:01, State:pointing~satellite4=star16}=true",
                        "{Index:00, Agent:satellite4,Action:turn_to~satellite4~groundstation1~star16=CONDITIONS_NOT_MET}=false,{Stage:00, State:pointing~satellite4=star16}=true,{Stage:01, State:pointing~satellite4=star16}=false",
                        "{Index:00, Agent:satellite4,Action:turn_to~satellite4~groundstation1~star16=CONDITIONS_NOT_MET}=true,{Stage:00, State:pointing~satellite4=LOCKED_FOR_UPDATE}=false",
                        "{Index:00, Agent:satellite4,Action:turn_to~satellite4~groundstation1~star16=CONDITIONS_NOT_MET}=true,{Stage:00, State:pointing~satellite4=star16}=true",

                ],
                [
                        "{Index:10, Agent:satellite1,Action:turn_to~satellite1~phenomenon14~phenomenon12=CONDITIONS_NOT_MET}=false,{Stage:10, State:pointing~satellite1=LOCKED_FOR_UPDATE}=false,{Stage:11, State:pointing~satellite1=LOCKED_FOR_UPDATE}=true",
                        "{Index:10, Agent:satellite1,Action:turn_to~satellite1~phenomenon14~phenomenon12=CONDITIONS_NOT_MET}=false,{Stage:10, State:pointing~satellite1=LOCKED_FOR_UPDATE}=true,{Stage:10, State:pointing~satellite1=phenomenon12}=false",
                        "{Index:10, Agent:satellite1,Action:turn_to~satellite1~phenomenon14~phenomenon12=CONDITIONS_NOT_MET}=false,{Stage:10, State:pointing~satellite1=LOCKED_FOR_UPDATE}=true,{Stage:11, State:pointing~satellite1=LOCKED_FOR_UPDATE}=false",
                        "{Index:10, Agent:satellite1,Action:turn_to~satellite1~phenomenon14~phenomenon12=CONDITIONS_NOT_MET}=false,{Stage:10, State:pointing~satellite1=phenomenon12}=false,{Stage:11, State:pointing~satellite1=phenomenon12}=true",
                        "{Index:10, Agent:satellite1,Action:turn_to~satellite1~phenomenon14~phenomenon12=CONDITIONS_NOT_MET}=false,{Stage:10, State:pointing~satellite1=phenomenon12}=true,{Stage:11, State:pointing~satellite1=phenomenon12}=false",
                        "{Index:10, Agent:satellite1,Action:turn_to~satellite1~phenomenon14~phenomenon12=CONDITIONS_NOT_MET}=false,{Stage:10, State:pointing~satellite1=phenomenon14}=false,{Stage:11, State:pointing~satellite1=phenomenon14}=true",
                        "{Index:10, Agent:satellite1,Action:turn_to~satellite1~phenomenon14~phenomenon12=CONDITIONS_NOT_MET}=false,{Stage:10, State:pointing~satellite1=phenomenon14}=true,{Stage:11, State:pointing~satellite1=phenomenon14}=false",
                        "{Index:10, Agent:satellite1,Action:turn_to~satellite1~phenomenon14~phenomenon12=CONDITIONS_NOT_MET}=false,{Stage:10, State:pointing~satellite1=phenomenon19}=false,{Stage:11, State:pointing~satellite1=phenomenon19}=true",
                        "{Index:10, Agent:satellite1,Action:turn_to~satellite1~phenomenon14~phenomenon12=CONDITIONS_NOT_MET}=false,{Stage:10, State:pointing~satellite1=phenomenon19}=true,{Stage:11, State:pointing~satellite1=phenomenon19}=false",
                        "{Index:10, Agent:satellite1,Action:turn_to~satellite1~phenomenon14~phenomenon12=CONDITIONS_NOT_MET}=true,{Stage:10, State:pointing~satellite1=LOCKED_FOR_UPDATE}=false",
                        "{Index:10, Agent:satellite1,Action:turn_to~satellite1~phenomenon14~phenomenon12=CONDITIONS_NOT_MET}=true,{Stage:10, State:pointing~satellite1=phenomenon12}=true",
                        "{Index:10, Agent:satellite3,Action:take_image~satellite3~planet15~instrument21~image3=CONDITIONS_NOT_MET}=false,{Stage:10, State:calibrated~instrument21=true}=false,{Stage:10, State:have_image~planet15~image3=LOCKED_FOR_UPDATE}=true,{Stage:10, State:pointing~satellite3=planet15}=false,{Stage:10, State:power_on~instrument21=true}=false",
                        "{Index:10, Agent:satellite3,Action:take_image~satellite3~planet15~instrument21~image3=CONDITIONS_NOT_MET}=false,{Stage:10, State:have_image~planet15~image3=LOCKED_FOR_UPDATE}=false,{Stage:11, State:have_image~planet15~image3=LOCKED_FOR_UPDATE}=true",
                        "{Index:10, Agent:satellite3,Action:take_image~satellite3~planet15~instrument21~image3=CONDITIONS_NOT_MET}=false,{Stage:10, State:have_image~planet15~image3=LOCKED_FOR_UPDATE}=true,{Stage:11, State:have_image~planet15~image3=LOCKED_FOR_UPDATE}=false",
                        "{Index:10, Agent:satellite3,Action:take_image~satellite3~planet15~instrument21~image3=CONDITIONS_NOT_MET}=false,{Stage:10, State:have_image~planet15~image3=false}=false,{Stage:11, State:have_image~planet15~image3=false}=true",
                        "{Index:10, Agent:satellite3,Action:take_image~satellite3~planet15~instrument21~image3=CONDITIONS_NOT_MET}=false,{Stage:10, State:have_image~planet15~image3=false}=true,{Stage:11, State:have_image~planet15~image3=false}=false",
                        "{Index:10, Agent:satellite3,Action:take_image~satellite3~planet15~instrument21~image3=CONDITIONS_NOT_MET}=false,{Stage:10, State:have_image~planet15~image3=true}=false,{Stage:11, State:have_image~planet15~image3=true}=true",
                        "{Index:10, Agent:satellite3,Action:take_image~satellite3~planet15~instrument21~image3=CONDITIONS_NOT_MET}=false,{Stage:10, State:have_image~planet15~image3=true}=true,{Stage:11, State:have_image~planet15~image3=true}=false",
                        "{Index:10, Agent:satellite3,Action:take_image~satellite3~planet15~instrument21~image3=CONDITIONS_NOT_MET}=true,{Stage:10, State:calibrated~instrument21=true}=true",
                        "{Index:10, Agent:satellite3,Action:take_image~satellite3~planet15~instrument21~image3=CONDITIONS_NOT_MET}=true,{Stage:10, State:have_image~planet15~image3=LOCKED_FOR_UPDATE}=false",
                        "{Index:10, Agent:satellite3,Action:take_image~satellite3~planet15~instrument21~image3=CONDITIONS_NOT_MET}=true,{Stage:10, State:pointing~satellite3=planet15}=true",
                        "{Index:10, Agent:satellite3,Action:take_image~satellite3~planet15~instrument21~image3=CONDITIONS_NOT_MET}=true,{Stage:10, State:power_on~instrument21=true}=true",

                ],
                [
                        "{Index:20, Agent:satellite1,Action:take_image~satellite1~star16~instrument11~image4=CONDITIONS_NOT_MET}=false,{Stage:20, State:calibrated~instrument11=true}=false,{Stage:20, State:have_image~star16~image4=LOCKED_FOR_UPDATE}=true,{Stage:20, State:pointing~satellite1=star16}=false,{Stage:20, State:power_on~instrument11=true}=false",
                        "{Index:20, Agent:satellite1,Action:take_image~satellite1~star16~instrument11~image4=CONDITIONS_NOT_MET}=false,{Stage:20, State:have_image~star16~image4=LOCKED_FOR_UPDATE}=false,{Stage:21, State:have_image~star16~image4=LOCKED_FOR_UPDATE}=true",
                        "{Index:20, Agent:satellite1,Action:take_image~satellite1~star16~instrument11~image4=CONDITIONS_NOT_MET}=false,{Stage:20, State:have_image~star16~image4=LOCKED_FOR_UPDATE}=true,{Stage:21, State:have_image~star16~image4=LOCKED_FOR_UPDATE}=false",
                        "{Index:20, Agent:satellite1,Action:take_image~satellite1~star16~instrument11~image4=CONDITIONS_NOT_MET}=false,{Stage:20, State:have_image~star16~image4=false}=false,{Stage:21, State:have_image~star16~image4=false}=true",
                        "{Index:20, Agent:satellite1,Action:take_image~satellite1~star16~instrument11~image4=CONDITIONS_NOT_MET}=false,{Stage:20, State:have_image~star16~image4=false}=true,{Stage:21, State:have_image~star16~image4=false}=false",
                        "{Index:20, Agent:satellite1,Action:take_image~satellite1~star16~instrument11~image4=CONDITIONS_NOT_MET}=false,{Stage:20, State:have_image~star16~image4=true}=false,{Stage:21, State:have_image~star16~image4=true}=true",
                        "{Index:20, Agent:satellite1,Action:take_image~satellite1~star16~instrument11~image4=CONDITIONS_NOT_MET}=false,{Stage:20, State:have_image~star16~image4=true}=true,{Stage:21, State:have_image~star16~image4=true}=false",
                        "{Index:20, Agent:satellite1,Action:take_image~satellite1~star16~instrument11~image4=CONDITIONS_NOT_MET}=true,{Stage:20, State:calibrated~instrument11=true}=true",
                        "{Index:20, Agent:satellite1,Action:take_image~satellite1~star16~instrument11~image4=CONDITIONS_NOT_MET}=true,{Stage:20, State:have_image~star16~image4=LOCKED_FOR_UPDATE}=false",
                        "{Index:20, Agent:satellite1,Action:take_image~satellite1~star16~instrument11~image4=CONDITIONS_NOT_MET}=true,{Stage:20, State:pointing~satellite1=star16}=true",
                        "{Index:20, Agent:satellite1,Action:take_image~satellite1~star16~instrument11~image4=CONDITIONS_NOT_MET}=true,{Stage:20, State:power_on~instrument11=true}=true",
                ],
                [
                        "{Index:30, Agent:satellite1,Action:take_image~satellite1~star6~instrument10~spectrograph6=CONDITIONS_NOT_MET}=false,{Stage:30, State:calibrated~instrument10=true}=false,{Stage:30, State:have_image~star6~spectrograph6=LOCKED_FOR_UPDATE}=true,{Stage:30, State:pointing~satellite1=star6}=false,{Stage:30, State:power_on~instrument10=true}=false",
                        "{Index:30, Agent:satellite1,Action:take_image~satellite1~star6~instrument10~spectrograph6=CONDITIONS_NOT_MET}=false,{Stage:30, State:have_image~star6~spectrograph6=LOCKED_FOR_UPDATE}=false,{Stage:31, State:have_image~star6~spectrograph6=LOCKED_FOR_UPDATE}=true",
                        "{Index:30, Agent:satellite1,Action:take_image~satellite1~star6~instrument10~spectrograph6=CONDITIONS_NOT_MET}=false,{Stage:30, State:have_image~star6~spectrograph6=LOCKED_FOR_UPDATE}=true,{Stage:31, State:have_image~star6~spectrograph6=LOCKED_FOR_UPDATE}=false",
                        "{Index:30, Agent:satellite1,Action:take_image~satellite1~star6~instrument10~spectrograph6=CONDITIONS_NOT_MET}=false,{Stage:30, State:have_image~star6~spectrograph6=false}=false,{Stage:31, State:have_image~star6~spectrograph6=false}=true",
                        "{Index:30, Agent:satellite1,Action:take_image~satellite1~star6~instrument10~spectrograph6=CONDITIONS_NOT_MET}=false,{Stage:30, State:have_image~star6~spectrograph6=false}=true,{Stage:31, State:have_image~star6~spectrograph6=false}=false",
                        "{Index:30, Agent:satellite1,Action:take_image~satellite1~star6~instrument10~spectrograph6=CONDITIONS_NOT_MET}=false,{Stage:30, State:have_image~star6~spectrograph6=true}=false,{Stage:31, State:have_image~star6~spectrograph6=true}=true",
                        "{Index:30, Agent:satellite1,Action:take_image~satellite1~star6~instrument10~spectrograph6=CONDITIONS_NOT_MET}=false,{Stage:30, State:have_image~star6~spectrograph6=true}=true,{Stage:31, State:have_image~star6~spectrograph6=true}=false",
                        "{Index:30, Agent:satellite1,Action:take_image~satellite1~star6~instrument10~spectrograph6=CONDITIONS_NOT_MET}=true,{Stage:30, State:calibrated~instrument10=true}=true",
                        "{Index:30, Agent:satellite1,Action:take_image~satellite1~star6~instrument10~spectrograph6=CONDITIONS_NOT_MET}=true,{Stage:30, State:have_image~star6~spectrograph6=LOCKED_FOR_UPDATE}=false",
                        "{Index:30, Agent:satellite1,Action:take_image~satellite1~star6~instrument10~spectrograph6=CONDITIONS_NOT_MET}=true,{Stage:30, State:pointing~satellite1=star6}=true",
                        "{Index:30, Agent:satellite1,Action:take_image~satellite1~star6~instrument10~spectrograph6=CONDITIONS_NOT_MET}=true,{Stage:30, State:power_on~instrument10=true}=true",
                ],
                [
                        "{Index:40, Agent:satellite1,Action:take_image~satellite1~planet22~instrument10~spectrograph6=CONDITIONS_NOT_MET}=false,{Stage:40, State:calibrated~instrument10=true}=false,{Stage:40, State:have_image~planet22~spectrograph6=LOCKED_FOR_UPDATE}=true,{Stage:40, State:pointing~satellite1=planet22}=false,{Stage:40, State:power_on~instrument10=true}=false",
                        "{Index:40, Agent:satellite1,Action:take_image~satellite1~planet22~instrument10~spectrograph6=CONDITIONS_NOT_MET}=false,{Stage:40, State:have_image~planet22~spectrograph6=LOCKED_FOR_UPDATE}=false,{Stage:41, State:have_image~planet22~spectrograph6=LOCKED_FOR_UPDATE}=true",
                        "{Index:40, Agent:satellite1,Action:take_image~satellite1~planet22~instrument10~spectrograph6=CONDITIONS_NOT_MET}=false,{Stage:40, State:have_image~planet22~spectrograph6=LOCKED_FOR_UPDATE}=true,{Stage:41, State:have_image~planet22~spectrograph6=LOCKED_FOR_UPDATE}=false",
                        "{Index:40, Agent:satellite1,Action:take_image~satellite1~planet22~instrument10~spectrograph6=CONDITIONS_NOT_MET}=false,{Stage:40, State:have_image~planet22~spectrograph6=false}=false,{Stage:41, State:have_image~planet22~spectrograph6=false}=true",
                        "{Index:40, Agent:satellite1,Action:take_image~satellite1~planet22~instrument10~spectrograph6=CONDITIONS_NOT_MET}=false,{Stage:40, State:have_image~planet22~spectrograph6=false}=true,{Stage:41, State:have_image~planet22~spectrograph6=false}=false",
                        "{Index:40, Agent:satellite1,Action:take_image~satellite1~planet22~instrument10~spectrograph6=CONDITIONS_NOT_MET}=false,{Stage:40, State:have_image~planet22~spectrograph6=true}=false,{Stage:41, State:have_image~planet22~spectrograph6=true}=true",
                        "{Index:40, Agent:satellite1,Action:take_image~satellite1~planet22~instrument10~spectrograph6=CONDITIONS_NOT_MET}=false,{Stage:40, State:have_image~planet22~spectrograph6=true}=true,{Stage:41, State:have_image~planet22~spectrograph6=true}=false",
                        "{Index:40, Agent:satellite1,Action:take_image~satellite1~planet22~instrument10~spectrograph6=CONDITIONS_NOT_MET}=true,{Stage:40, State:calibrated~instrument10=true}=true",
                        "{Index:40, Agent:satellite1,Action:take_image~satellite1~planet22~instrument10~spectrograph6=CONDITIONS_NOT_MET}=true,{Stage:40, State:have_image~planet22~spectrograph6=LOCKED_FOR_UPDATE}=false",
                        "{Index:40, Agent:satellite1,Action:take_image~satellite1~planet22~instrument10~spectrograph6=CONDITIONS_NOT_MET}=true,{Stage:40, State:pointing~satellite1=planet22}=true",
                        "{Index:40, Agent:satellite1,Action:take_image~satellite1~planet22~instrument10~spectrograph6=CONDITIONS_NOT_MET}=true,{Stage:40, State:power_on~instrument10=true}=true",
                ],
                [
                        "{Index:50, Agent:satellite1,Action:turn_to~satellite1~planet22~phenomenon21=CONDITIONS_NOT_MET}=false,{Stage:50, State:pointing~satellite1=LOCKED_FOR_UPDATE}=false,{Stage:51, State:pointing~satellite1=LOCKED_FOR_UPDATE}=true",
                        "{Index:50, Agent:satellite1,Action:turn_to~satellite1~planet22~phenomenon21=CONDITIONS_NOT_MET}=false,{Stage:50, State:pointing~satellite1=LOCKED_FOR_UPDATE}=true,{Stage:50, State:pointing~satellite1=phenomenon21}=false",
                        "{Index:50, Agent:satellite1,Action:turn_to~satellite1~planet22~phenomenon21=CONDITIONS_NOT_MET}=false,{Stage:50, State:pointing~satellite1=LOCKED_FOR_UPDATE}=true,{Stage:51, State:pointing~satellite1=LOCKED_FOR_UPDATE}=false",
                        "{Index:50, Agent:satellite1,Action:turn_to~satellite1~planet22~phenomenon21=CONDITIONS_NOT_MET}=false,{Stage:50, State:pointing~satellite1=phenomenon19}=false,{Stage:51, State:pointing~satellite1=phenomenon19}=true",
                        "{Index:50, Agent:satellite1,Action:turn_to~satellite1~planet22~phenomenon21=CONDITIONS_NOT_MET}=false,{Stage:50, State:pointing~satellite1=phenomenon19}=true,{Stage:51, State:pointing~satellite1=phenomenon19}=false",
                        "{Index:50, Agent:satellite1,Action:turn_to~satellite1~planet22~phenomenon21=CONDITIONS_NOT_MET}=false,{Stage:50, State:pointing~satellite1=phenomenon21}=false,{Stage:51, State:pointing~satellite1=phenomenon21}=true",
                        "{Index:50, Agent:satellite1,Action:turn_to~satellite1~planet22~phenomenon21=CONDITIONS_NOT_MET}=false,{Stage:50, State:pointing~satellite1=phenomenon21}=true,{Stage:51, State:pointing~satellite1=phenomenon21}=false",
                        "{Index:50, Agent:satellite1,Action:turn_to~satellite1~planet22~phenomenon21=CONDITIONS_NOT_MET}=false,{Stage:50, State:pointing~satellite1=planet22}=false,{Stage:51, State:pointing~satellite1=planet22}=true",
                        "{Index:50, Agent:satellite1,Action:turn_to~satellite1~planet22~phenomenon21=CONDITIONS_NOT_MET}=false,{Stage:50, State:pointing~satellite1=planet22}=true,{Stage:51, State:pointing~satellite1=planet22}=false",
                        "{Index:50, Agent:satellite1,Action:turn_to~satellite1~planet22~phenomenon21=CONDITIONS_NOT_MET}=true,{Stage:50, State:pointing~satellite1=LOCKED_FOR_UPDATE}=false",
                        "{Index:50, Agent:satellite1,Action:turn_to~satellite1~planet22~phenomenon21=CONDITIONS_NOT_MET}=true,{Stage:50, State:pointing~satellite1=phenomenon21}=true",

                ],
                [
                        "{Index:60, Agent:satellite1,Action:switch_off~instrument13~satellite1=CONDITIONS_NOT_MET}=false,{Stage:60, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=false,{Stage:61, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=true",
                        "{Index:60, Agent:satellite1,Action:switch_off~instrument13~satellite1=CONDITIONS_NOT_MET}=false,{Stage:60, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=true,{Stage:60, State:power_on~instrument13=LOCKED_FOR_UPDATE}=true,{Stage:60, State:power_on~instrument13=true}=false",
                        "{Index:60, Agent:satellite1,Action:switch_off~instrument13~satellite1=CONDITIONS_NOT_MET}=false,{Stage:60, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=true,{Stage:61, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=false",
                        "{Index:60, Agent:satellite1,Action:switch_off~instrument13~satellite1=CONDITIONS_NOT_MET}=false,{Stage:60, State:power_avail~satellite1=true}=false,{Stage:61, State:power_avail~satellite1=true}=true",
                        "{Index:60, Agent:satellite1,Action:switch_off~instrument13~satellite1=CONDITIONS_NOT_MET}=false,{Stage:60, State:power_avail~satellite1=true}=true,{Stage:61, State:power_avail~satellite1=true}=false",
                        "{Index:60, Agent:satellite1,Action:switch_off~instrument13~satellite1=CONDITIONS_NOT_MET}=false,{Stage:60, State:power_on~instrument13=LOCKED_FOR_UPDATE}=false,{Stage:61, State:power_on~instrument13=LOCKED_FOR_UPDATE}=true",
                        "{Index:60, Agent:satellite1,Action:switch_off~instrument13~satellite1=CONDITIONS_NOT_MET}=false,{Stage:60, State:power_on~instrument13=LOCKED_FOR_UPDATE}=true,{Stage:61, State:power_on~instrument13=LOCKED_FOR_UPDATE}=false",
                        "{Index:60, Agent:satellite1,Action:switch_off~instrument13~satellite1=CONDITIONS_NOT_MET}=false,{Stage:60, State:power_on~instrument13=false}=false,{Stage:61, State:power_on~instrument13=false}=true",
                        "{Index:60, Agent:satellite1,Action:switch_off~instrument13~satellite1=CONDITIONS_NOT_MET}=false,{Stage:60, State:power_on~instrument13=false}=true,{Stage:61, State:power_on~instrument13=false}=false",
                        "{Index:60, Agent:satellite1,Action:switch_off~instrument13~satellite1=CONDITIONS_NOT_MET}=false,{Stage:60, State:power_on~instrument13=true}=false,{Stage:61, State:power_on~instrument13=true}=true",
                        "{Index:60, Agent:satellite1,Action:switch_off~instrument13~satellite1=CONDITIONS_NOT_MET}=false,{Stage:60, State:power_on~instrument13=true}=true,{Stage:61, State:power_on~instrument13=true}=false",
                        "{Index:60, Agent:satellite1,Action:switch_off~instrument13~satellite1=CONDITIONS_NOT_MET}=true,{Stage:60, State:power_avail~satellite1=LOCKED_FOR_UPDATE}=false",
                        "{Index:60, Agent:satellite1,Action:switch_off~instrument13~satellite1=CONDITIONS_NOT_MET}=true,{Stage:60, State:power_on~instrument13=LOCKED_FOR_UPDATE}=false",
                        "{Index:60, Agent:satellite1,Action:switch_off~instrument13~satellite1=CONDITIONS_NOT_MET}=true,{Stage:60, State:power_on~instrument13=true}=true",
                ],
        ]
    }


}