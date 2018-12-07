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
import il.ac.bgu.utils.PlanUtils;
import io.bretty.console.view.ActionView;
import io.bretty.console.view.MenuView;
import io.bretty.console.view.Validator;
import io.bretty.console.view.ViewConfig;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.ProblemRunner.FailedConflictModel.*;

public class ProblemRunner {

    public static final String SATELLITE_DOMAIN = "satellite";


    private static final Logger log;
    public static final String DEPORT_DOMAIN = "deport";
    public static final String ELEVATOR_DOMAIN = "elevator";

    private static void addPlanActionsMenu(MenuView menuView, String fileName) {

        String planName = fileName.replace(".ser", "");

        menuView.addMenuItem(new ActionView(planName + " problem actions:", planName) {
            @Override
            public void executeCustomAction() {
                Map<Integer, Set<Step>> plan;
                try {
                    plan = PlanUtils.loadSerializedPlan("plans/" + fileName);
                    List<ImmutablePair<Integer, Step>> planActions = plan.entrySet().stream()
                            .filter(entry -> entry.getKey() != -1)
                            .flatMap(entry ->
                                    entry.getValue().stream().map(step ->
                                            ImmutablePair.of(entry.getKey(), step)))
                            .collect(Collectors.toList());

                    MutableInt index = new MutableInt(-1);
                    planActions.forEach(pair ->
                            this.println(String.format("%3d) Step: %d   Agent: %s Action:%s",
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


    private static Map<String, MenuView> domainView = new HashMap<>();
    private static List<MenuView> modelView = new ArrayList<>();

    static {
        Configurator.initialize(null, "conf/log4j.properties");
        log = LoggerFactory.getLogger(YmlToCsvConverter.class);

    }

    public static void main(String[] args) throws IOException {
        MenuView mainView = new MenuView("Welcome to SAT problem runner", "",
                new ViewConfig.Builder().setMenuSelectionMessage("Please select fault and conflict model: ").build());

        ViewConfig domainViewConfig = new ViewConfig.Builder().setMenuSelectionMessage("Please select problem domain: ").build();
        modelView.add(new MenuView(FAIL_MODEL_NO_EFFECT_CONFLICT_MODEL_NO_RETRIES.modelId,
                FAIL_MODEL_NO_EFFECT_CONFLICT_MODEL_NO_RETRIES.modelId, domainViewConfig));
        modelView.add(new MenuView(FAIL_MODEL_DELAY_ONE_STEP_CONFLICT_MODEL_NO_RETRIES.modelId,
                FAIL_MODEL_DELAY_ONE_STEP_CONFLICT_MODEL_NO_RETRIES.modelId, domainViewConfig));
        modelView.add(new MenuView(FAIL_MODEL_NO_EFFECT_CONFLICT_MODEL_ONE_RETRY.modelId,
                FAIL_MODEL_NO_EFFECT_CONFLICT_MODEL_ONE_RETRY.modelId, domainViewConfig));
        modelView.add(new MenuView(FAIL_MODEL_DELAY_ONE_STEP_CONFLICT_MODEL_ONE_RETRY.modelId,
                FAIL_MODEL_DELAY_ONE_STEP_CONFLICT_MODEL_ONE_RETRY.modelId, domainViewConfig));

        ViewConfig problemViewConfig = new ViewConfig.Builder().setMenuSelectionMessage("Please select problem: ").build();
        domainView.put(SATELLITE_DOMAIN, new MenuView("Satellite domain - problems", SATELLITE_DOMAIN, problemViewConfig));
        domainView.put(DEPORT_DOMAIN, new MenuView("Deport domain - problems", DEPORT_DOMAIN, problemViewConfig));
        domainView.put(ELEVATOR_DOMAIN, new MenuView("Elevator domain - problems", ELEVATOR_DOMAIN, problemViewConfig));

        modelView.forEach(mainView::addMenuItem);
        modelView.forEach(modelView -> domainView.values().forEach(modelView::addMenuItem));


        try (Stream<Path> paths = Files.walk(Paths.get("src/main/resources/plans"))) {
            paths.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted(Comparator.comparing(f -> NumberUtils.toInt(f.replaceAll("\\D+", ""), 0)))  //sort by plan number
                    .forEach(fileName -> {
                        if (fileName.startsWith(DEPORT_DOMAIN)) {
                            addPlanActionsMenu(domainView.get(DEPORT_DOMAIN), fileName);
                        } else if (fileName.startsWith(ELEVATOR_DOMAIN)) {
                            addPlanActionsMenu(domainView.get(ELEVATOR_DOMAIN), fileName);
                        } else if (fileName.startsWith(SATELLITE_DOMAIN)) {
                            addPlanActionsMenu(domainView.get(SATELLITE_DOMAIN), fileName);
                        }
                    });
        }
        mainView.display();

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
        private CnfClausesFunction conflictClausesCreator = new ConflictNoEffectsCnfClauses();
        private CnfClausesFunction healthyCnfClausesCreator = new HealthyCnfClauses();

        public ProblemExecutor(String problemName, FailedConflictModel failedConflictModel, List<Action> failedActions) throws IOException, URISyntaxException {
            populateModels(failedConflictModel);

            Map<Integer, Set<Step>> plan = PlanUtils.loadSerializedPlan("plans/" + problemName);

            plan = conflictRetriesModel.updatePlan(plan).updatedPlan;

        }

        private void populateModels(FailedConflictModel failedConflictModel) {

            switch (failedConflictModel) {
                case FAIL_MODEL_NO_EFFECT_CONFLICT_MODEL_NO_RETRIES:
                    failedClausesCreator = new FailedNoEffectsCnfClauses();
                    conflictRetriesModel = new NoRetriesPlanUpdater();
                    break;
                case FAIL_MODEL_NO_EFFECT_CONFLICT_MODEL_ONE_RETRY:
                    failedClausesCreator = new FailedNoEffectsCnfClauses();
                    conflictRetriesModel = new OneRetryPlanUpdater();
                    break;
                case FAIL_MODEL_DELAY_ONE_STEP_CONFLICT_MODEL_NO_RETRIES:
                    failedClausesCreator = new FailedDelayOneStepCnfClauses();
                    conflictRetriesModel = new NoRetriesPlanUpdater();
                    break;
                case FAIL_MODEL_DELAY_ONE_STEP_CONFLICT_MODEL_ONE_RETRY:
                    failedClausesCreator = new FailedDelayOneStepCnfClauses();
                    conflictRetriesModel = new OneRetryPlanUpdater();
                    break;

            }
        }


        public void execute() {

        }
    }
}
