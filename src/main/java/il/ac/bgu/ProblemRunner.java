package il.ac.bgu;

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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProblemRunner {

    private static final int DEPORT_DOMAIN_CODE = 0;
    private static final int ELEVATOR_DOMAIN_CODE = 1;
    private static final int SATELLITE_DOMAIN_CODE = 2;

    private static final Logger log;
    private static Map<String, MenuView> domainView = new HashMap<>();
    private static List<MenuView> modelView = new ArrayList<>();

    static {
        Configurator.initialize(null, "conf/log4j.properties");
        log = LoggerFactory.getLogger(YmlToCsvConverter.class);

    }

    private static void addPlanActionsMenu(MenuView menuView, String fileName) {

        String planName = fileName.replace(".problem.ser", "");

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
                            this.println(String.format("%d) Step: %d   Agent: %s Action:%s",
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
                    println("Failed and Conflict models: " + this.parentView.getParentView().getNameInParentMenu());
                    List<Action> failedActions = Arrays.stream(chosenActions.split(","))
                            .map(String::trim)
                            .map(Integer::parseInt)
                            .map(planActions::get)
                            .map(planedAction -> Action.of(planedAction.getRight(), planedAction.getLeft()))
                            .collect(Collectors.toList());
                    println("Failed Actions: " + failedActions);

                    pause();


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
        modelView.add(new MenuView("Fail model: no effect, Conflict model: no retries",
                "Fail model: no effect, Conflict model: no retries", domainViewConfig));
        modelView.add(new MenuView("Fail model: delay one step, Conflict model: no retries",
                "Fail model: delay one step, Conflict model: no retries", domainViewConfig));
        modelView.add(new MenuView("Fail model: no effect, Conflict model: one retry",
                "Fail model: no effect, Conflict model: one retry", domainViewConfig));
        modelView.add(new MenuView("Fail model: delay one step, Conflict model: one retry",
                "Fail model: delay one step, Conflict model: one retry", domainViewConfig));

        ViewConfig problemViewConfig = new ViewConfig.Builder().setMenuSelectionMessage("Please select problem: ").build();
        domainView.put("satellite", new MenuView("Satellite domain - problems", "satellite", problemViewConfig));
        domainView.put("deport", new MenuView("Deport domain - problems", "deport", problemViewConfig));
        domainView.put("elevator", new MenuView("Elevator domain - problems", "elevator", problemViewConfig));

        modelView.forEach(mainView::addMenuItem);
        modelView.forEach(modelView -> domainView.values().forEach(modelView::addMenuItem));


        try (Stream<Path> paths = Files.walk(Paths.get("src/main/resources/plans"))) {
            paths.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted(Comparator.comparing(f -> NumberUtils.toInt(f.replaceAll("\\D+", ""), 0)))  //sort by plan number
                    .forEach(fileName -> {
                        if (fileName.startsWith("deports")) {
                            addPlanActionsMenu(domainView.get("deport"), fileName);
                        } else if (fileName.startsWith("elevator")) {
                            addPlanActionsMenu(domainView.get("elevator"), fileName);
                        } else if (fileName.startsWith("satellite")) {
                            addPlanActionsMenu(domainView.get("satellite"), fileName);
                        }
                    });
        }


        mainView.display();


        Map<Integer, List<Path>> domainIndexToPlan;

        try (Stream<Path> paths = Files.walk(Paths.get("src/main/resources/plans"))) {
            domainIndexToPlan = paths.filter(Files::isRegularFile)
                    .flatMap(file -> {
                        if (file.startsWith("deports"))
                            return Stream.of(ImmutablePair.of(DEPORT_DOMAIN_CODE, file));
                        if (file.startsWith("elevator"))
                            return Stream.of(ImmutablePair.of(ELEVATOR_DOMAIN_CODE, file));
                        if (file.startsWith("satellite"))
                            return Stream.of(ImmutablePair.of(SATELLITE_DOMAIN_CODE, file));
                        return Stream.of(); //default
                    })
                    .collect(Collectors.groupingBy(ImmutablePair::getLeft,
                            Collectors.mapping(Pair::getValue, Collectors.toList())));
        }


        System.out.println("Welcome to SAT problem runner");


    }
}
