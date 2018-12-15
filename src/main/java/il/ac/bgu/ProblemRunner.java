package il.ac.bgu;

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
import il.ac.bgu.sat.SatSolver;
import il.ac.bgu.utils.PlanSolvingUtils;
import il.ac.bgu.utils.PlanUtils;
import il.ac.bgu.variablesCalculation.FinalVariableStateCalc;
import il.ac.bgu.variablesCalculation.FinalVariableStateCalcImpl;
import io.bretty.console.view.ActionView;
import io.bretty.console.view.MenuView;
import io.bretty.console.view.Validator;
import io.bretty.console.view.ViewConfig;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.core.config.Configurator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static il.ac.bgu.ProblemRunner.FailedConflictModel.*;
import static java.lang.String.format;

public class ProblemRunner {

    private static final Logger log;

    private static Map<String, MenuView> domainViewMap = new HashMap<>();
    private static List<MenuView> modelViewList = new ArrayList<>();

    private static void addPlanActionsMenu(MenuView menuView, String fileName) {

        String planName = fileName.replace(".ser", "");

        menuView.addMenuItem(new ActionView(planName + " problem actions:", planName) {
            @Override
            public void executeCustomAction() {
                Map<Integer, Set<Step>> plan;
                try {
                    plan = PlanUtils.loadSerializedPlan("src/main/resources/plans/" + fileName);
                    List<ImmutablePair<Integer, Step>> planActions = plan.entrySet().stream()
                            .filter(entry -> entry.getKey() != -1)
                            .flatMap(entry ->
                                    entry.getValue().stream().map(step ->
                                            ImmutablePair.of(entry.getKey(), step)))
                            .collect(Collectors.toList());

                    MutableInt index = new MutableInt(-1);
                    planActions.forEach(pair ->
                            this.println(format("%3d) Step: %d   Agent: %s Action:%s",
                                    index.incrementAndGet(), pair.getKey(), pair.getValue().getAgent(), pair.getValue())
                            ));

                    Validator<String> actionChooserValidation = input -> Arrays.stream(input.split(","))
                            .map(String::trim)
                            .allMatch(t -> NumberUtils.isDigits(t) &&
                                    Range.between(0, planActions.size() - 1).contains(Integer.parseInt(t)));

                    String chosenActions = this.prompt("Please choose the failed actions [action index separated by comma]: ",
                            String.class, actionChooserValidation);


                    println("Chosen actions:");
                    println("Problem: " + this.nameInParentMenu);
                    FailedConflictModel failedConflictModel = FailedConflictModel.fromModelId(
                            this.parentView.getParentView().getNameInParentMenu());

                    println("Failed and Conflict models: " + failedConflictModel);
                    List<Action> failedActions = Arrays.stream(chosenActions.split(","))
                            .map(String::trim)
                            .map(Integer::parseInt)
                            .map(planActions::get)
                            .map(planedAction -> Action.of(planedAction.getRight(), planedAction.getLeft()))
                            .collect(Collectors.toList());
                    println("Failed Actions: " + failedActions);

                    pause();

                    new ProblemExecutor(fileName, failedConflictModel, failedActions).execute();


                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    static {
        Configurator.initialize(null, "conf/log4j.properties");
        log = LoggerFactory.getLogger(YmlToCsvConverter.class);

    }

    public static void main(String[] args) throws IOException {
        MenuView mainView = new MenuView("Welcome to SAT problem runner", "",
                new ViewConfig.Builder().setMenuSelectionMessage("Please select fault and conflict model: ").build());

        ViewConfig domainViewConfig = new ViewConfig.Builder().setMenuSelectionMessage("Please select problem domain: ").build();
        modelViewList.add(new MenuView(FAIL_MODEL_NO_EFFECT_CONFLICT_MODEL_NO_RETRIES.modelId,
                FAIL_MODEL_NO_EFFECT_CONFLICT_MODEL_NO_RETRIES.modelId, domainViewConfig));
        modelViewList.add(new MenuView(FAIL_MODEL_DELAY_ONE_STEP_CONFLICT_MODEL_NO_RETRIES.modelId,
                FAIL_MODEL_DELAY_ONE_STEP_CONFLICT_MODEL_NO_RETRIES.modelId, domainViewConfig));
        modelViewList.add(new MenuView(FAIL_MODEL_NO_EFFECT_CONFLICT_MODEL_ONE_RETRY.modelId,
                FAIL_MODEL_NO_EFFECT_CONFLICT_MODEL_ONE_RETRY.modelId, domainViewConfig));
        modelViewList.add(new MenuView(FAIL_MODEL_DELAY_ONE_STEP_CONFLICT_MODEL_ONE_RETRY.modelId,
                FAIL_MODEL_DELAY_ONE_STEP_CONFLICT_MODEL_ONE_RETRY.modelId, domainViewConfig));

        ViewConfig problemViewConfig = new ViewConfig.Builder().setMenuSelectionMessage("Please select problem: ").build();

        List<String> serializedPlans = Files.list(Paths.get("src/main/resources/plans"))
                .map(path -> path.getFileName().toString())
                .filter(fileName -> fileName.endsWith(".ser"))
                .collect(Collectors.toList());

        //create domain menu view
        serializedPlans.stream()
                .map(ProblemRunner::getDomain) //get domain from problem name
                .distinct()
                .forEach(domainName -> domainViewMap.put(domainName,
                        new MenuView(format("%s domain - problems", domainName), domainName, problemViewConfig)));

        modelViewList.forEach(mainView::addMenuItem);
        modelViewList.forEach(modelView -> domainViewMap.values().forEach(modelView::addMenuItem));


        modelViewList.forEach(view -> view.addMenuItem(new ActionView("custom problem", "custom problem") {
            @Override
            public void executeCustomAction() {
                try {
                    String problemFilePath = prompt("Please supply problem file absolute path: ", String.class,
                            input -> new File(input).isFile());

                    String[] agentDefs = Files.readAllLines(Paths.get(problemFilePath)).stream()
                            .flatMap(t -> Arrays.stream(t.split("\t")))
                            .toArray(String[]::new);

                    String serPlanPath = String.format("src/main/resources/plans/%s.ser", new File(problemFilePath).getName());
                    SerializationUtils.serialize(SatSolver.calculateSolution(agentDefs),
                            new FileOutputStream(serPlanPath));

                    pause();

                    String domainName = getDomain(new File(serPlanPath).getName());
                    if (!domainViewMap.containsKey(domainName)) {
                        //add new domain (if not exists)
                        MenuView domainView = new MenuView(format("%s domain - problems", domainName), domainName, problemViewConfig);
                        domainViewMap.put(domainName, domainView);
                        modelViewList.forEach(modelView -> modelView.addMenuItem(domainView));
                    }

                    addPlanActionsMenu(domainViewMap.get(domainName), new File(serPlanPath).getName());

                    goBack();


                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }));


        serializedPlans.stream()
                .sorted(Comparator.comparing(f -> NumberUtils.toInt(f.replaceAll("\\D+", ""), 0)))  //sort by plan number
                .forEach(fileName -> addPlanActionsMenu(domainViewMap.get(getDomain(fileName)), fileName));

        mainView.display();

    }

    @NotNull
    private static String getDomain(String fileName) {
        return fileName.split("\\.")[0].replaceAll("[-\\d]", "");
    }


    enum FailedConflictModel {
        FAIL_MODEL_NO_EFFECT_CONFLICT_MODEL_NO_RETRIES("Fail model: no effect, Conflict model: no retries"),
        FAIL_MODEL_DELAY_ONE_STEP_CONFLICT_MODEL_NO_RETRIES("Fail model: delay one step, Conflict model: no retries"),
        FAIL_MODEL_NO_EFFECT_CONFLICT_MODEL_ONE_RETRY("Fail model: no effect, Conflict model: one retry"),
        FAIL_MODEL_DELAY_ONE_STEP_CONFLICT_MODEL_ONE_RETRY("Fail model: delay one step, Conflict model: one retry");
        private String modelId;

        FailedConflictModel(String modelId) {
            this.modelId = modelId;
        }

        public static FailedConflictModel fromModelId(String modelId) {
            for (FailedConflictModel failedConflictModel : FailedConflictModel.values()) {
                if (failedConflictModel.modelId.equalsIgnoreCase(modelId)) {
                    return failedConflictModel;
                }
            }
            throw new RuntimeException("Not found FailedConflictModel for modelId: " + modelId);
        }

    }

    private static class ProblemExecutor {
        private CnfClausesFunction failedClausesCreator;
        private RetryPlanUpdater conflictRetriesModel;
        private final Map<Integer, Set<Step>> plan;
        private CnfClausesFunction conflictClausesCreator = new ConflictNoEffectsCnfClauses();
        private CnfClausesFunction healthyCnfClausesCreator = new HealthyCnfClauses();
        private FinalVariableStateCalc finalVariableStateCalc;
        private List<Action> failedActions;

        public ProblemExecutor(String problemName, FailedConflictModel failedConflictModel, List<Action> failedActions) throws IOException, URISyntaxException {
            plan = PlanUtils.loadSerializedPlan("src/main/resources/plans/" + problemName);
            this.failedActions = failedActions;
            populateModels(failedConflictModel, plan);

        }

        private void populateModels(FailedConflictModel failedConflictModel, Map<Integer, Set<Step>> plan) {

            switch (failedConflictModel) {
                case FAIL_MODEL_NO_EFFECT_CONFLICT_MODEL_NO_RETRIES:
                    failedClausesCreator = new FailedNoEffectsCnfClauses();
                    conflictRetriesModel = new NoRetriesPlanUpdater();
                    finalVariableStateCalc =
                            new FinalVariableStateCalcImpl(plan, failedClausesCreator.getVariableModel());
                    break;
                case FAIL_MODEL_NO_EFFECT_CONFLICT_MODEL_ONE_RETRY:
                    failedClausesCreator = new FailedNoEffectsCnfClauses();
                    conflictRetriesModel = new OneRetryPlanUpdater();
                    finalVariableStateCalc =
                            new FinalVariableStateCalcImpl(
                                    conflictRetriesModel.updatePlan(plan).updatedPlan, failedClausesCreator.getVariableModel());
                    break;
                case FAIL_MODEL_DELAY_ONE_STEP_CONFLICT_MODEL_NO_RETRIES:
                    failedClausesCreator = new FailedDelayOneStepCnfClauses();
                    conflictRetriesModel = new NoRetriesPlanUpdater();
                    finalVariableStateCalc =
                            new FinalVariableStateCalcImpl(plan, failedClausesCreator.getVariableModel());
                    break;
                case FAIL_MODEL_DELAY_ONE_STEP_CONFLICT_MODEL_ONE_RETRY:
                    failedClausesCreator = new FailedDelayOneStepCnfClauses();
                    conflictRetriesModel = new OneRetryPlanUpdater();
                    finalVariableStateCalc =
                            new FinalVariableStateCalcImpl(
                                    conflictRetriesModel.updatePlan(plan).updatedPlan, failedClausesCreator.getVariableModel());
                    break;
            }
        }


        void execute() {
            //add agent to preconditions and effects of every action to prevent action collisions in delay failure model
            PlanUtils.updatePlanWithAgentDependencies(plan);

            List<List<FormattableValue<? extends Formattable>>> hardConstraints =
                    PlanSolvingUtils.createPlanHardConstraints(plan, conflictRetriesModel, healthyCnfClausesCreator,
                            conflictClausesCreator, failedClausesCreator);
            List<FormattableValue<Formattable>> softConstraints = PlanUtils.encodeHealthyClauses(plan);
            PlanSolvingUtils.calculateSolutions(plan, hardConstraints, softConstraints, finalVariableStateCalc, failedActions)
                    .forEach(solution -> System.out.println("Found solution: " + solution));


        }
    }
}
