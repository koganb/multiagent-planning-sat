package il.ac.bgu;

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
import il.ac.bgu.testUtils.ActionDependencyCalculation;
import il.ac.bgu.utils.PlanSolvingUtils;
import il.ac.bgu.utils.PlanUtils;
import il.ac.bgu.variableModel.DelayStageVariableFailureModel;
import il.ac.bgu.variableModel.NoEffectVariableFailureModel;
import il.ac.bgu.variableModel.VariableModelFunction;
import il.ac.bgu.variablesCalculation.FinalVariableStateCalc;
import il.ac.bgu.variablesCalculation.FinalVariableStateCalcImpl;
import lombok.AllArgsConstructor;
import one.util.streamex.MoreCollectors;
import one.util.streamex.StreamEx;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static il.ac.bgu.sat.DiagnosisFindingStopIndicator.MINIMAL_CARDINALITY;
import static java.lang.String.format;

public class CreateTestData {

    public static final String FILE_NO_EFFECTS_FAILURE_MODEL_WITH_RETRIES_PARAMS_YML = "NoEffectsFailureModelWithRetriesParams.yml";
    public static final String FILE_NO_EFFECTS_FAILURE_MODEL_NO_RETRIES_PARAMS_YML = "NoEffectsFailureModelNoRetriesParams.yml";
    public static final String FILE_DELAY_FAILURE_MODEL_NO_RETRIES_PARAMS_YML = "DelayFailureModelNoRetriesParams.yml";
    public static final String FILE_DELAY_FAILURE_MODEL_WITH_RETRIES_PARAMS_YML = "DelayFailureModelWithRetriesParams.yml";
    private static final int FAILED_ACTIONS_SIZE = 1;
    private static final long SAT_TIMEOUT_MILS = 10000L;

    public static void main(String[] args) {
        FileUtils.deleteQuietly(new File(FILE_DELAY_FAILURE_MODEL_NO_RETRIES_PARAMS_YML));
        FileUtils.deleteQuietly(new File(FILE_DELAY_FAILURE_MODEL_WITH_RETRIES_PARAMS_YML));
        FileUtils.deleteQuietly(new File(FILE_NO_EFFECTS_FAILURE_MODEL_NO_RETRIES_PARAMS_YML));
        FileUtils.deleteQuietly(new File(FILE_NO_EFFECTS_FAILURE_MODEL_WITH_RETRIES_PARAMS_YML));

        StreamEx.of("zenotravel_pfile20.problem")
                .flatMap(problemName -> {
                    Map<Integer, Set<Step>> plan = PlanUtils.loadSerializedPlan(format("plans/%s.ser", problemName));
                    List<FormattableValue<? extends Formattable>> noFailuresFinalVariableState =
                            new FinalVariableStateCalcImpl(plan, null).getFinalVariableState(Sets.newHashSet());
                    return IntStream.rangeClosed(1, 4).mapToObj(i -> PlanParam.of(problemName, plan, i, noFailuresFinalVariableState));
                })
                .forEach(planParam -> {
                    try {
                        createDelayFailureModelNoRetriesParams(planParam.problemName, planParam.plan, planParam.failedActionsCardinality,
                                planParam.noFailuresFinalVariableState);
                        createDelayFailureModelWithRetriesParams(planParam.problemName, planParam.plan, planParam.failedActionsCardinality,
                                planParam.noFailuresFinalVariableState);
                        createNoEffectsFailureModelWithRetriesParams(planParam.problemName, planParam.plan, planParam.failedActionsCardinality,
                                planParam.noFailuresFinalVariableState);
                        createNoEffectsFailureModelNoRetriesParams(planParam.problemName, planParam.plan, planParam.failedActionsCardinality,
                                planParam.noFailuresFinalVariableState);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

    }

    private static void createNoEffectsFailureModelWithRetriesParams(
            String problemName, Map<Integer, Set<Step>> plan, int failedActionsCardinality, List<FormattableValue<? extends Formattable>> noFailuresFinalVariableState) throws IOException {
        StringBuffer params = createParams(problemName, plan, failedActionsCardinality, new OneRetryPlanUpdater(),
                new NoEffectVariableFailureModel(), noFailuresFinalVariableState, new ConflictNoEffectsCnfClauses(), new FailedNoEffectsCnfClauses());
        FileUtils.write(new File(FILE_NO_EFFECTS_FAILURE_MODEL_WITH_RETRIES_PARAMS_YML), params.toString(), Charset.defaultCharset(), true);

    }

    private static void createNoEffectsFailureModelNoRetriesParams(
            String problemName, Map<Integer, Set<Step>> plan, int failedActionsCardinality, List<FormattableValue<? extends Formattable>> noFailuresFinalVariableState) throws IOException {
        StringBuffer params = createParams(problemName, plan, failedActionsCardinality, new NoRetriesPlanUpdater(),
                new NoEffectVariableFailureModel(), noFailuresFinalVariableState, new ConflictNoEffectsCnfClauses(), new FailedNoEffectsCnfClauses());
        FileUtils.write(new File(FILE_NO_EFFECTS_FAILURE_MODEL_NO_RETRIES_PARAMS_YML), params.toString(), Charset.defaultCharset(), true);
    }

    private static void createDelayFailureModelNoRetriesParams(
            String problemName, Map<Integer, Set<Step>> plan, int failedActionsCardinality, List<FormattableValue<? extends Formattable>> noFailuresFinalVariableState) throws IOException {
        StringBuffer params = createParams(problemName, plan, failedActionsCardinality, new NoRetriesPlanUpdater(),
                new DelayStageVariableFailureModel(1), noFailuresFinalVariableState, new ConflictNoEffectsCnfClauses(), new FailedDelayOneStepCnfClauses());
        FileUtils.write(new File(FILE_DELAY_FAILURE_MODEL_NO_RETRIES_PARAMS_YML), params.toString(), Charset.defaultCharset(), true);
    }

    private static void createDelayFailureModelWithRetriesParams(
            String problemName, Map<Integer, Set<Step>> plan, int failedActionsCardinality, List<FormattableValue<? extends Formattable>> noFailuresFinalVariableState) throws IOException {
        StringBuffer params = createParams(problemName, plan, failedActionsCardinality, new OneRetryPlanUpdater(),
                new DelayStageVariableFailureModel(1), noFailuresFinalVariableState, new ConflictNoEffectsCnfClauses(), new FailedDelayOneStepCnfClauses());
        FileUtils.write(new File(FILE_DELAY_FAILURE_MODEL_WITH_RETRIES_PARAMS_YML), params.toString(), Charset.defaultCharset(), true);
    }

    private static StringBuffer createParams(String problemName, Map<Integer, Set<Step>> plan, int failedActionsCardinality,
                                             RetryPlanUpdater conflictRetriesModel,
                                             VariableModelFunction variableFailureModel,
                                             List<FormattableValue<? extends Formattable>> noFailuresFinalVariableState,
                                             CnfClausesFunction conflictCnfClausesCreator,
                                             CnfClausesFunction failedCnfClausesCreator) {

        StringBuffer paramsBuffer = new StringBuffer();

        ActionDependencyCalculation actionDependencyCalculation =
                new ActionDependencyCalculation(plan, noFailuresFinalVariableState, variableFailureModel, conflictRetriesModel);

        paramsBuffer.append(format(
                "- action_cardinality: %s\n", failedActionsCardinality));
        paramsBuffer.append(format(
                "  problem_name: %s\n", problemName));
        paramsBuffer.append(
                "  test_actions:\n");

        StreamEx.of(
                actionDependencyCalculation.getIndependentActionsList(failedActionsCardinality)
                        .map(failActionCandidates -> FailedActionsParam.of(plan, failActionCandidates)))
                .filter(planFailedActions -> {
                            FinalVariableStateCalc finalVariableStateCalc = new FinalVariableStateCalcImpl(planFailedActions.plan,
                                    variableFailureModel);
                            List<List<FormattableValue<? extends Formattable>>> cnfPlanClauses =
                                    PlanUtils.createPlanHardConstraints(planFailedActions.plan, conflictRetriesModel, new HealthyCnfClauses(),
                                            conflictCnfClausesCreator, failedCnfClausesCreator);

                            return PlanSolvingUtils.calculateSolutions(
                                    planFailedActions.plan, cnfPlanClauses, PlanUtils.encodeHealthyClauses(planFailedActions.plan), finalVariableStateCalc,
                                    planFailedActions.failActionsCandidates, SAT_TIMEOUT_MILS, MINIMAL_CARDINALITY)
                                    .filter(solution -> !solution.isEmpty())
                                    .anyMatch(solution -> solution.size() == failedActionsCardinality);
                        }
                )
                .collect(MoreCollectors.head(FAILED_ACTIONS_SIZE))
                .forEach(actionSet -> {
                    paramsBuffer.append("  - action_set:\n");
                    actionSet.failActionsCandidates.forEach(
                            action -> {
                                paramsBuffer.append(format(
                                        "    - agent: %s\n", action.getAgentName()));
                                paramsBuffer.append(format(
                                        "      stage: %s\n", action.getStage()));
                                paramsBuffer.append(format(
                                        "      action_name: %s\n", action.getActionName()));
                            }
                    );
                });
        return paramsBuffer;
    }

    @AllArgsConstructor(staticName = "of")
    private static class FailedActionsParam {
        private Map<Integer, Set<Step>> plan;
        private Set<Action> failActionsCandidates;
    }

    @AllArgsConstructor(staticName = "of")
    private static class PlanParam {
        private String problemName;
        private Map<Integer, Set<Step>> plan;
        private int failedActionsCardinality;
        private List<FormattableValue<? extends Formattable>> noFailuresFinalVariableState;
    }

}
