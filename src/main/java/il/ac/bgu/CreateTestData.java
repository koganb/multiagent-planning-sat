package il.ac.bgu;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static il.ac.bgu.sat.DiagnosisFindingStopIndicator.FIRST_SOLUTION;
import static java.lang.String.format;

public class CreateTestData {

    public static final String FILE_NO_EFFECTS_FAILURE_MODEL_WITH_RETRIES_PARAMS = "NoEffectsFailureModelWithRetriesParams";
    public static final String FILE_NO_EFFECTS_FAILURE_MODEL_NO_RETRIES_PARAMS = "NoEffectsFailureModelNoRetriesParams";
    public static final String FILE_DELAY_FAILURE_MODEL_NO_RETRIES_PARAMS = "DelayFailureModelNoRetriesParams";
    public static final String FILE_DELAY_FAILURE_MODEL_WITH_RETRIES_PARAMS = "DelayFailureModelWithRetriesParams";
    private static final int FAILED_ACTIONS_SIZE = 20;
    private static final long SAT_TIMEOUT_MILS = 1000 * 60 * 60 * 10L;

    public static void main(String[] args) {
        StreamEx.of(
//                PlanArgs.of("elevator29.problem", IntStream.rangeClosed(4, 4)),
//                PlanArgs.of("elevator28.problem", IntStream.rangeClosed(4, 4)),
//                PlanArgs.of("elevator27.problem", IntStream.rangeClosed(4, 4)),
//                PlanArgs.of("elevator26.problem", IntStream.rangeClosed(4, 4)),
//                PlanArgs.of("elevator25.problem", IntStream.rangeClosed(4, 4)),
               // PlanArgs.of("satellite15.problem", IntStream.rangeClosed(4, 4)),
                PlanArgs.of("zenotravel_pfile20.problem", IntStream.rangeClosed(3, 3))
                //PlanArgs.of("satellite13.problem", IntStream.rangeClosed(4, 4))
//                PlanArgs.of("taxi_p08.problem", IntStream.rangeClosed(1, 4)),
//                PlanArgs.of("taxi_p15.problem", IntStream.rangeClosed(1, 4)),
//                PlanArgs.of("taxi_p16.problem", IntStream.rangeClosed(1, 4)),
//problem                PlanArgs.of("taxi_p17.problem", IntStream.rangeClosed(4, 4))
//                PlanArgs.of("taxi_p18.problem", IntStream.rangeClosed(1, 4)),
//                PlanArgs.of("taxi_p19.problem", IntStream.rangeClosed(1, 4)),
//                PlanArgs.of("taxi_p20.problem", IntStream.rangeClosed(1, 4)),
//                PlanArgs.of("woodworking_p02.problem", IntStream.rangeClosed(1, 4)),
//                PlanArgs.of("woodworking_p03.problem", IntStream.rangeClosed(1, 4)),
//                PlanArgs.of("woodworking_p04.problem", IntStream.rangeClosed(1, 4)),
//                PlanArgs.of("woodworking_p07.problem", IntStream.rangeClosed(1, 4)),
//                PlanArgs.of("zenotravel_pfile10.problem", IntStream.rangeClosed(1, 4)),
//                PlanArgs.of("zenotravel_pfile15.problem", IntStream.rangeClosed(1, 4)),
//                PlanArgs.of("zenotravel_pfile16.problem", IntStream.rangeClosed(1, 4)),
//                PlanArgs.of("zenotravel_pfile17.problem", IntStream.rangeClosed(1, 4)),
//                PlanArgs.of("zenotravel_pfile18.problem", IntStream.rangeClosed(1, 4)),
//                PlanArgs.of("zenotravel_pfile19.problem", IntStream.rangeClosed(1, 4)),
//                PlanArgs.of("zenotravel_pfile20.problem", IntStream.rangeClosed(1, 4))
//                PlanArgs.of("driverlog15.problem", IntStream.rangeClosed(4, 4))
//                PlanArgs.of("elevator30.problem", IntStream.rangeClosed(1, 4)),
//                PlanArgs.of("taxi_p18.problem", IntStream.rangeClosed(1, 4)),
  //              PlanArgs.of("woodworking_p06.problem", IntStream.rangeClosed(4, 4))
//                "driverlog15.problem",
//                "elevator30.problem",
////                "satellite14.problem"
//                "taxi_p18.problem",
//                "woodworking_p06.problem"
//                "woodworking_p06.problem"
        )
                .flatMap(planArgs -> {
                    Map<Integer, ImmutableList<PlanAction>> plan = PlanUtils.loadSerializedPlan(format("plans/%s.ser", planArgs.problemName));
                    return planArgs.failuresCardinality.mapToObj(i -> LoadedPlanParam.of(planArgs.problemName, plan, i));
                })
                .forEach(loadedPlanParam -> {
                    try {
                        createDelayFailureModelNoRetriesParams(loadedPlanParam.problemName, loadedPlanParam.plan, loadedPlanParam.failedActionsCardinality);
                        createDelayFailureModelWithRetriesParams(loadedPlanParam.problemName, loadedPlanParam.plan, loadedPlanParam.failedActionsCardinality);
                        createNoEffectsFailureModelWithRetriesParams(loadedPlanParam.problemName, loadedPlanParam.plan, loadedPlanParam.failedActionsCardinality);
                        createNoEffectsFailureModelNoRetriesParams(loadedPlanParam.problemName, loadedPlanParam.plan, loadedPlanParam.failedActionsCardinality);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

    }

    private static void createNoEffectsFailureModelWithRetriesParams(
            String problemName, Map<Integer, ImmutableList<PlanAction>> plan, int failedActionsCardinality) throws IOException {
        StringBuffer params = createParams(problemName, plan, failedActionsCardinality, new OneRetryPlanUpdater(),
                new NoEffectVariableFailureModel(), new ConflictNoEffectsCnfClauses(), new FailedNoEffectsCnfClauses());
        FileUtils.write(new File(String.format("%s_%s_%s.yml", problemName, failedActionsCardinality,
                FILE_NO_EFFECTS_FAILURE_MODEL_WITH_RETRIES_PARAMS)), params.toString(), Charset.defaultCharset());

    }

    private static void createNoEffectsFailureModelNoRetriesParams(
            String problemName, Map<Integer, ImmutableList<PlanAction>> plan, int failedActionsCardinality) throws IOException {
        StringBuffer params = createParams(problemName, plan, failedActionsCardinality, new NoRetriesPlanUpdater(),
                new NoEffectVariableFailureModel(), new ConflictNoEffectsCnfClauses(), new FailedNoEffectsCnfClauses());
        FileUtils.write(new File(String.format("%s_%s_%s.yml", problemName, failedActionsCardinality,
                FILE_NO_EFFECTS_FAILURE_MODEL_NO_RETRIES_PARAMS)), params.toString(), Charset.defaultCharset());
    }

    private static void createDelayFailureModelNoRetriesParams(
            String problemName, Map<Integer, ImmutableList<PlanAction>> plan, int failedActionsCardinality) throws IOException {
        StringBuffer params = createParams(problemName, plan, failedActionsCardinality, new NoRetriesPlanUpdater(),
                new DelayStageVariableFailureModel(1), new ConflictNoEffectsCnfClauses(), new FailedDelayOneStepCnfClauses());
        FileUtils.write(new File(String.format("%s_%s_%s.yml", problemName, failedActionsCardinality,
                FILE_DELAY_FAILURE_MODEL_NO_RETRIES_PARAMS)), params.toString(), Charset.defaultCharset());
    }

    private static void createDelayFailureModelWithRetriesParams(
            String problemName, Map<Integer, ImmutableList<PlanAction>> plan, int failedActionsCardinality) throws IOException {
        StringBuffer params = createParams(problemName, plan, failedActionsCardinality, new OneRetryPlanUpdater(),
                new DelayStageVariableFailureModel(1), new ConflictNoEffectsCnfClauses(), new FailedDelayOneStepCnfClauses());
        FileUtils.write(new File(String.format("%s_%s_%s.yml", problemName, failedActionsCardinality,
                FILE_DELAY_FAILURE_MODEL_WITH_RETRIES_PARAMS)), params.toString(), Charset.defaultCharset());
    }

    private static StringBuffer createParams(String problemName, Map<Integer, ImmutableList<PlanAction>> plan, int failedActionsCardinality,
                                             RetryPlanUpdater conflictRetriesModel,
                                             VariableModelFunction variableFailureModel,
                                             CnfClausesFunction conflictCnfClausesCreator,
                                             CnfClausesFunction failedCnfClausesCreator) {

        StringBuffer paramsBuffer = new StringBuffer();

        FinalVariableStateCalc finalVariableStateCalc = new FinalVariableStateCalcImpl(plan,
                variableFailureModel, conflictRetriesModel);

        ActionDependencyCalculation actionDependencyCalculation =
                new ActionDependencyCalculation(plan, finalVariableStateCalc.getFinalVariableState(Sets.newHashSet()),
                        variableFailureModel, conflictRetriesModel);

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

        final List< Set<Action>> independentActionsList =
                actionDependencyCalculation.getIndependentActionsList(failedActionsCardinality).collect(Collectors.toList());
        System.out.println(independentActionsList.size());



        StreamEx.of(
                independentActionsList )
                .filter(planFailedActions -> {
                            System.out.println(problemName);
                            System.out.println(planFailedActions);
                            System.out.println("conflict retries:" + conflictRetriesModel.getName());
                            System.out.println("failed clauses creator:" + failedCnfClausesCreator.getClass());


                            List<List<FormattableValue<? extends Formattable>>> cnfPlanClauses =
                                    PlanUtils.createPlanHardConstraints(plan, conflictRetriesModel, new HealthyCnfClauses(),
                                            conflictCnfClausesCreator, failedCnfClausesCreator);

                            List<Either<Throwable, List<? extends Formattable>>> solutions = PlanSolvingUtils.calculateSolutions(
                                    conflictRetriesModel.updatePlan(plan).updatedPlan, cnfPlanClauses, PlanUtils.encodeHealthyClauses(plan), finalVariableStateCalc,
                                    planFailedActions, SAT_TIMEOUT_MILS, FIRST_SOLUTION);
                            return ! solutions.isEmpty() &&
                                    solutions.stream()
                                            .anyMatch(solution -> solution.isLeft() ||  //result got exception
                                                    solution.get().size() == failedActionsCardinality);
                        }
                )
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
    private static class PlanArgs {
        private String problemName;
        private IntStream failuresCardinality;

    }


    @AllArgsConstructor(staticName = "of")
    private static class LoadedPlanParam {
        private String problemName;
        private Map<Integer, ImmutableList<PlanAction>> plan;
        private int failedActionsCardinality;

    }

}
