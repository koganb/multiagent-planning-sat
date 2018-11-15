package il.ac.bgu.cnfCompilation

import il.ac.bgu.failureModel.DelayStageFailureModel
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
            String serializedPlanFilename, VariableModelFunction failureModel, Integer step, expectedClauses) {
        setup:
        TreeMap<Integer, Set<Step>> plan = SerializationUtils.deserialize(new FileInputStream(
                serializedPlanFilename))
        CnfCompilation cnfCompilation = new CnfCompilation(plan, failureModel)

        expect:
        cnfCompilation.executeStageAndAddFluents(step, plan.get(step))
                .map({ l -> l.stream().map({ v -> v.toString() }).sorted().collect(Collectors.joining(",")) })
                .sorted()
                .collect(Collectors.toList()) == expectedClauses


        where:
        serializedPlanFilename << [
                'deports0.problem.ser',
                'deports0.problem.ser',
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
                new DelayStageFailureModel(1),
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
        step << [0, 0, 0, 0, 0, 10, 20, 30, 40, 50, 60]
        expectedClauses << [
                [
                        "{Stage:00, State:clear~crate1=false}=false",
                        "{Stage:00, State:clear~hoist0=false}=false",
                        "{Stage:00, State:clear~pallet0=true}=false",
                        "{Stage:00, State:on~crate1=hoist0}=false",
                ],
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
                ],
                [
                        "{Stage:00, State:pointing~satellite3=star2}=false",
                        "{Stage:00, State:pointing~satellite4=groundstation1}=false",
                        "{Stage:00, State:power_avail~satellite1=false}=false",
                        "{Stage:00, State:power_on~instrument11=true}=false",
                ],
                [
                        "{Stage:10, State:have_image~planet15~image3=true}=false",
                        "{Stage:10, State:pointing~satellite1=phenomenon14}=false",
                ],
                [
                        "{Stage:20, State:have_image~star16~image4=true}=false",

                ],
                [
                        "{Stage:30, State:have_image~star6~spectrograph6=true}=false",
                ],
                [
                        "{Stage:40, State:have_image~planet22~spectrograph6=true}=false",

                ],
                [
                        "{Stage:50, State:pointing~satellite1=planet22}=false",
                ],
                [
                ],


        ]
    }


}