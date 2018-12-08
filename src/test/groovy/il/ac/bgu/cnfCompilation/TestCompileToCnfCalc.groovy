package il.ac.bgu.cnfCompilation

import com.google.common.collect.ImmutableSet
import il.ac.bgu.dataModel.Action
import il.ac.bgu.sat.SatSolver
import il.ac.bgu.variableModel.DelayStageVariableFailureModel
import il.ac.bgu.variableModel.NoEffectVariableFailureModel
import il.ac.bgu.variableModel.VariableModelFunction
import org.agreement_technologies.common.map_planner.Step
import org.apache.commons.lang3.SerializationUtils
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import java.util.stream.Collectors

@Unroll
@Ignore
class TestCompileToCnfCalc extends Specification {


    def "test compile to CNF #serializedPlanFilename"(String serializedPlanFilename,
                                                      VariableModelFunction failureModel,
                                                      Action failedAction,
                                                      expectedHardConstraints,
                                                      expectedSoftConstraints) {
        setup:
        TreeMap<Integer, Set<Step>> plan = SerializationUtils.deserialize(new FileInputStream(
                serializedPlanFilename))
        CnfCompilation cnfCompilation = new CnfCompilation(plan, failureModel)


        def compilationResult = SatSolver.compilePlanToCnf(cnfCompilation, ImmutableSet.of(failedAction))
        expect:

        def hardConstraints = compilationResult.key
                .stream()
                .map({ l -> l.stream().map({ v -> v.toString() }).sorted().collect(Collectors.joining(",")) })
                .sorted()
                .collect(Collectors.toList())
        hardConstraints == expectedHardConstraints.stream().sorted().collect(Collectors.toList())

        def softConstraints = compilationResult.value
                .stream()
                .map({ v -> v.toString() })
                .sorted()
                .collect(Collectors.toList())
        softConstraints == expectedSoftConstraints

        where:
        serializedPlanFilename << [
                'deports0.problem.ser',
                'deports0.problem.ser',
                'deports0.problem.ser',
                'elevator1.problem.ser',
                'satellite1.problem.ser',
                'elevator2.problem.ser',
                'satellite8.problem.ser',
                'deports3.problem.ser',

        ]

        failureModel << [
                new DelayStageVariableFailureModel(1),
                new DelayStageVariableFailureModel(1),
                new NoEffectVariableFailureModel(),
                new NoEffectVariableFailureModel(),
                new NoEffectVariableFailureModel(),
                new NoEffectVariableFailureModel(),
                new NoEffectVariableFailureModel(),
                new NoEffectVariableFailureModel(),
        ]
        failedAction << [
                Action.of("LiftP hoist0 crate1 pallet0 depot0", "depot0", 0),
                Action.of("Load hoist0 crate1 truck1 depot0", "truck1", 1),
                Action.of("Load hoist0 crate1 truck1 depot0", "truck1", 1),
                Action.of("move-down-slow slow1-0 n8 n4", "slow1-0", 3),
                Action.of("switch_on instrument0 satellite0", "satellite0", 1),
                Action.of("board p1 fast0 n4 n1 n2", "fast0", 2),
                Action.of("turn_to satellite2 planet11 phenomenon9", "satellite2", 11),
                Action.of("Unload hoist0 crate1 truck0 depot0", "truck0", 2),

        ]

        expectedHardConstraints << [
                [],
                [],
                [],
                [],
                [],
                [],
                [],
                [],
        ]


        expectedSoftConstraints << [
                [],
                [],
                [],
                [],
                [],
                [],
                [],
                [],
        ]


    }


}