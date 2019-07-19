package il.ac.bgu;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import il.ac.bgu.cnfClausesModel.CnfClausesFunction;
import il.ac.bgu.cnfClausesModel.conflict.ConflictNoEffectsCnfClauses;
import il.ac.bgu.cnfClausesModel.failed.FailedDelayOneStepCnfClauses;
import il.ac.bgu.cnfClausesModel.failed.FailedNoEffectsCnfClauses;
import il.ac.bgu.cnfClausesModel.healthy.HealthyCnfClauses;
import il.ac.bgu.cnfCompilation.retries.NoRetriesPlanUpdater;
import il.ac.bgu.cnfCompilation.retries.OneRetryPlanUpdater;
import il.ac.bgu.cnfCompilation.retries.RetryPlanUpdater;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.plan.PlanAction;
import il.ac.bgu.testUtils.ActionDependencyCalculation;
import il.ac.bgu.utils.PlanSolvingUtils;
import il.ac.bgu.utils.PlanUtils;
import il.ac.bgu.variableModel.DelayStageVariableFailureModel;
import il.ac.bgu.variableModel.NoEffectVariableFailureModel;
import il.ac.bgu.variableModel.VariableModelFunction;
import il.ac.bgu.variablesCalculation.FinalVariableStateCalc;
import il.ac.bgu.variablesCalculation.FinalVariableStateCalcImpl;
import io.vavr.control.Either;
import lombok.AllArgsConstructor;
import one.util.streamex.MoreCollectors;
import one.util.streamex.StreamEx;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.math3.util.Combinations;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static il.ac.bgu.sat.DiagnosisFindingStopIndicator.FIRST_SOLUTION;
import static java.lang.String.format;

public class CreateRawTestData {

    private static final int FAILED_ACTIONS_SIZE = 25;

    public static void main(String[] args) {
        StreamEx.of(
                Args.of("deports%s.problem",
                        Arrays.asList(1, 2, 3, 4, 7, 8, 10, 11, 13, 16, 17, 19), Arrays.asList(1, 2, 3)),
                Args.of("driverlog%s.problem",
                        Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 17), Arrays.asList(1, 2, 3)),
                Args.of("elevator%s.problem",
                        IntStream.rangeClosed(1, 30).boxed().collect(Collectors.toList()), Arrays.asList(1, 2, 3)),
                Args.of("ma-blocksworld%s.problem",
                        Arrays.asList("4-0", "4-1", "4-2", "5-0", "5-1", "5-2", "6-0", "6-1", "6-2", "7-0", "7-1", "7-2", "8-0", "8-1", "8-2", "9-1", "9-2"), Arrays.asList(1, 2, 3)),
                Args.of("os-sequencedstrips-p%s_1.problem",
                        IntStream.rangeClosed(5, 24).boxed().collect(Collectors.toList()), Arrays.asList(1, 2, 3)),
                Args.of("probLOGISTICS-%s-0.problem",
                        IntStream.rangeClosed(4, 22).boxed().collect(Collectors.toList()), Arrays.asList(1, 2, 3)),
                Args.of("rovers%s.problem",
                        IntStream.rangeClosed(1, 20).boxed().collect(Collectors.toList()), Arrays.asList(1, 2, 3)),
                Args.of("satellite%s.problem",
                        Arrays.asList(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 20), Arrays.asList(1, 2, 3)),
                Args.of("woodworking%s.problem",
                        IntStream.rangeClosed(1, 28).boxed().collect(Collectors.toList()), Arrays.asList(1, 2, 3)),
                Args.of("taxi_p%s.problem",
                        IntStream.rangeClosed(1, 20).mapToObj(s -> String.format("%02d", s)).collect(Collectors.toList()), Arrays.asList(1, 2, 3)),
                Args.of("ZTRAVEL-%s.problem",
                        Arrays.asList("2-4", "2-5", "2-6", "3-6", "3-7", "3-8", "3-10", "5-10", "5-15", "5-20", "5-25", "5-26"), Arrays.asList(1, 2, 3))

        )
                .flatMap(arg -> arg.problemIndexes.stream().map(i -> PlanArgs.of(format(arg.problemPattern, i), arg.failuresCardinality)))
                .flatMap(planArgs -> {
                    Map<Integer, ImmutableList<PlanAction>> plan = PlanUtils.loadSerializedPlan(format("plans/%s.ser", planArgs.problemName));
                    return planArgs.failuresCardinality.stream().map(i -> LoadedPlanParam.of(planArgs.problemName, plan, i));
                })
                .forEach(loadedPlanParam -> {
                    try {
                        StringBuffer params = createParams(loadedPlanParam.problemName, loadedPlanParam.plan, loadedPlanParam.failedActionsCardinality);

                        FileUtils.write(new File(String.format("%s_%s.yml", loadedPlanParam.problemName, loadedPlanParam.failedActionsCardinality
                                )), params.toString(), Charset.defaultCharset());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

    }



    private static StringBuffer createParams(String problemName, Map<Integer, ImmutableList<PlanAction>> plan, int failedActionsCardinality) {

        StringBuffer paramsBuffer = new StringBuffer();

        paramsBuffer.append(format(
                "action_cardinality: %s\n", failedActionsCardinality));
        paramsBuffer.append(format(
                "problem_name: %s\n", problemName));
        paramsBuffer.append(format(
                "plan_steps_number: %s\n", plan.entrySet().size()));
        paramsBuffer.append(format(
                "plan_actions_number: %s\n", plan.values().stream().mapToLong(Collection::size).sum()));
        paramsBuffer.append(format(
                "plan_agent_number: %s\n", plan.values().stream()
                        .flatMap(Collection::stream).map(PlanAction::getAgentName).distinct().count()));
        paramsBuffer.append(
                "test_actions:\n");


        createActionStream(plan, failedActionsCardinality)
                .collect(MoreCollectors.head(FAILED_ACTIONS_SIZE))
                .forEach(actionSet -> {
                    paramsBuffer.append("- action_set:\n");
                    actionSet.forEach(
                            action -> {
                                paramsBuffer.append(format(
                                        "  - agent: %s\n", action.getAgentName()));
                                paramsBuffer.append(format(
                                        "    stage: %s\n", action.getStage()));
                                paramsBuffer.append(format(
                                        "    action_name: %s\n", action.getActionName()));
                            }
                    );
                });
        return paramsBuffer;
    }


    @AllArgsConstructor(staticName = "of")
    private static class Args {
        private String problemPattern;
        private List<?> problemIndexes;
        private List<Integer> failuresCardinality;
    }

    @AllArgsConstructor(staticName = "of")
    private static class PlanArgs {
        private String problemName;
        private List<Integer> failuresCardinality;

    }



    @AllArgsConstructor(staticName = "of")
    private static class LoadedPlanParam {
        private String problemName;
        private Map<Integer, ImmutableList<PlanAction>> plan;
        private int failedActionsCardinality;

    }


    private static Stream<Set<Action>> createActionStream(Map<Integer, ImmutableList<PlanAction>> plan, Integer actionNumber) {
        final List<Action> actions = plan.entrySet().stream()
                .filter(i -> i.getKey() != -1)
                .flatMap(entry -> entry.getValue().stream()
                        .map(planAction -> Action.of(planAction, entry.getKey())))
                .collect(Collectors.toList());

        Collections.shuffle(actions);

        return Streams.stream(new Combinations(actions.size(), actionNumber).iterator()).map(
                combination -> Arrays.stream(combination).mapToObj(actions::get).collect(Collectors.toSet()));
    }


}
