package il.ac.bgu;

import com.google.common.base.Charsets;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
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
import il.ac.bgu.sat.DiagnosisFindingStopIndicator;
import il.ac.bgu.sat.SatSolver;
import il.ac.bgu.utils.PlanSolvingUtils;
import il.ac.bgu.utils.PlanUtils;
import il.ac.bgu.variableModel.DelayStageVariableFailureModel;
import il.ac.bgu.variableModel.NoEffectVariableFailureModel;
import il.ac.bgu.variablesCalculation.FinalVariableStateCalc;
import il.ac.bgu.variablesCalculation.FinalVariableStateCalcImpl;
import io.bretty.console.view.ActionView;
import io.bretty.console.view.MenuView;
import io.bretty.console.view.Validator;
import io.bretty.console.view.ViewConfig;
import one.util.streamex.StreamEx;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.core.config.Configurator;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import static il.ac.bgu.ProblemRunner.FailedConflictModel.*;
import static il.ac.bgu.sat.DiagnosisFindingStopIndicator.MINIMAL_CARDINALITY;
import static java.lang.String.format;

public class ProblemRunner {

    @SuppressWarnings("unused")
    private static final Logger log;

    private static boolean jarMode;

    private static Path planPath = Paths.get("plans");

    static {
        Configurator.initialize(null, "conf/log4j.properties");
        log = LoggerFactory.getLogger(YmlToCsvConverter.class);


        //in case:  java -jar build/libs/multiagent-planning-sat-1.0-SNAPSHOT-all.jar
        //in this case the new plans will be serialized into new plans directory
        jarMode = !Files.exists(Paths.get("src"));

        if (jarMode && !Files.exists(planPath)) {
            try {
                Files.createDirectory(planPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private static Map<String, MenuView> domainViewMap = new HashMap<>();
    private static List<MenuView> modelViewList = new ArrayList<>();

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

                    System.out.println("-----------------");
                    String satTimeout = this.prompt("Please set SAT timeout (sec) or press enter for default [300]: ",
                            String.class, s -> {
                                if (StringUtils.isNotEmpty(s)) {
                                    @Nullable Integer timeout = Ints.tryParse(s);
                                    return timeout != null && timeout > 0;
                                }
                                return true;
                            });

                    Long timeout = Optional.ofNullable(Longs.tryParse(satTimeout)).orElse(300L) * 1000;

                    System.out.println("-----------------");
                    String diagStopIndicatorStr = this.prompt(
                            format("Please select the diagnosis finding type or enter for: %s\n", MINIMAL_CARDINALITY.name()) +
                                    Arrays.stream(DiagnosisFindingStopIndicator.values()).map(i ->
                                            format("%3d) %s\n", i.ordinal() + 1, i.name())).collect(Collectors.joining("")), String.class, s -> {
                                if (StringUtils.isNotEmpty(s)) {
                                    @Nullable Integer stopIndicatorInd = Ints.tryParse(s);
                                    return stopIndicatorInd != null && stopIndicatorInd > 0 && stopIndicatorInd <= DiagnosisFindingStopIndicator.values().length;
                                }
                                return true;
                            });

                    DiagnosisFindingStopIndicator stopIndicator = Optional.ofNullable(Longs.tryParse(diagStopIndicatorStr))
                            .map(i -> DiagnosisFindingStopIndicator.values()[i.intValue() - 1]).orElse(MINIMAL_CARDINALITY);


                    new ProblemExecutor(fileName, failedConflictModel, failedActions).execute(timeout, stopIndicator);


                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
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

        List<String> serializedPlans =
                jarMode ?
                        StreamEx.of(
                                //get plans from jar classpath
                                new JarFile(
                                        new File(ProblemRunner.class.getProtectionDomain().getCodeSource().getLocation().getPath())).entries())
                                .map(ZipEntry::getName)
                                .filter(t -> t.startsWith("plans/") && t.endsWith(".ser"))
                                .map(t -> t.replace("plans/", ""))

                                //append plans from local dir
                                .append(Files.list(planPath).map(path -> path.getFileName().toString()))
                                .collect(Collectors.toList())
                        :
                        IOUtils.readLines(ProblemRunner.class.getClassLoader().getResourceAsStream("plans"), Charsets.UTF_8);


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
                            .filter(t -> StringUtils.isNotEmpty(t.trim()))
                            .toArray(String[]::new);

                    String serPlanPath = String.format(jarMode ? "plans/%s.ser" : "src/main/resources/plans/%s.ser", new File(problemFilePath).getName());
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

        ProblemExecutor(String problemName, FailedConflictModel failedConflictModel, List<Action> failedActions) throws IOException, URISyntaxException {
            plan = PlanUtils.loadSerializedPlan("plans/" + problemName);
            this.failedActions = failedActions;
            populateModels(failedConflictModel, plan);

        }

        private void populateModels(FailedConflictModel failedConflictModel, Map<Integer, Set<Step>> plan) {

            switch (failedConflictModel) {
                case FAIL_MODEL_NO_EFFECT_CONFLICT_MODEL_NO_RETRIES:
                    failedClausesCreator = new FailedNoEffectsCnfClauses();
                    conflictRetriesModel = new NoRetriesPlanUpdater();
                    finalVariableStateCalc =
                            new FinalVariableStateCalcImpl(plan, new NoEffectVariableFailureModel());
                    break;
                case FAIL_MODEL_NO_EFFECT_CONFLICT_MODEL_ONE_RETRY:
                    failedClausesCreator = new FailedNoEffectsCnfClauses();
                    conflictRetriesModel = new OneRetryPlanUpdater();
                    finalVariableStateCalc =
                            new FinalVariableStateCalcImpl(
                                    conflictRetriesModel.updatePlan(plan).updatedPlan, new NoEffectVariableFailureModel());
                    break;
                case FAIL_MODEL_DELAY_ONE_STEP_CONFLICT_MODEL_NO_RETRIES:
                    failedClausesCreator = new FailedDelayOneStepCnfClauses();
                    conflictRetriesModel = new NoRetriesPlanUpdater();
                    finalVariableStateCalc =
                            new FinalVariableStateCalcImpl(plan, new DelayStageVariableFailureModel(1));
                    break;
                case FAIL_MODEL_DELAY_ONE_STEP_CONFLICT_MODEL_ONE_RETRY:
                    failedClausesCreator = new FailedDelayOneStepCnfClauses();
                    conflictRetriesModel = new OneRetryPlanUpdater();
                    finalVariableStateCalc =
                            new FinalVariableStateCalcImpl(
                                    conflictRetriesModel.updatePlan(plan).updatedPlan, new DelayStageVariableFailureModel(1));
                    break;
            }
        }


        void execute(Long timeoutMs, DiagnosisFindingStopIndicator stopIndicator) {
            //add agent to preconditions and effects of every action to prevent action collisions in delay failure model
            PlanUtils.updatePlanWithAgentDependencies(plan);

            List<List<FormattableValue<? extends Formattable>>> hardConstraints =
                    PlanSolvingUtils.createPlanHardConstraints(plan, conflictRetriesModel, healthyCnfClausesCreator,
                            conflictClausesCreator, failedClausesCreator);
            List<FormattableValue<Formattable>> softConstraints = PlanUtils.encodeHealthyClauses(plan);
            PlanSolvingUtils.calculateSolutions(plan, hardConstraints, softConstraints, finalVariableStateCalc, failedActions,
                    timeoutMs, stopIndicator)
                    .forEach(solution -> System.out.println("Found solution: " + solution));


        }
    }
}
