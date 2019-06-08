package il.ac.bgu.testUtils;

import il.ac.bgu.plan.PlanAction;
import il.ac.bgu.utils.PlanUtils;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.yaml.snakeyaml.Yaml;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PreparedTestActionReader {

    @Getter
    @AllArgsConstructor(staticName = "of")
    @EqualsAndHashCode
    @ToString
    private static class ActionMetadata {
        private String agent;
        private Integer stage;
        private String actionName;
    }


    public static List<List<PlanAction>> getTestActions(String problemName, Integer failuresNumber, String failureModel) {

        String directoryName = problemName.replaceAll("\\d+$", "");
        String preparedTestFilePath = String.format("testCases/%s/%s/%s.problem_%s_%s.yml",
                directoryName, problemName, problemName, failuresNumber, failureModel);

        System.out.println("Loading: " + preparedTestFilePath);
        Object document = new Yaml()
                .load(PreparedTestActionReader.class.getClassLoader()
                .getResourceAsStream(preparedTestFilePath));

        final Map<ActionMetadata, PlanAction> actionMetadataMap = PlanUtils.loadSerializedPlan(
                String.format("plans/%s.problem.ser", problemName)).values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(t -> ActionMetadata.of(t.getAgentName(), t.getIndex(), t.getActionName()), Function.identity()));

        final List<?> testActionsMetaData = ((List<?>) ((Map<?, ?>) document).get("test_actions")).stream().map(t -> ((Map<?, ?>) t).get("action_set")).collect(Collectors.toList());
        return testActionsMetaData.stream()
                .map(l -> ((List<?>) l).stream()
                        .map(t -> ((Map<?, ?>) t))
                        .map(t ->
                                ActionMetadata.of(t.get("agent").toString(), (Integer) t.get("stage"), (String) t.get("action_name")))
                        .map(actionMetadataMap::get)
                        .collect(Collectors.toList())).collect(Collectors.toList());
    }


    public static void main(String[] args) {
        getTestActions("deports13",3, "NoEffectsFailureModelNoRetriesParams").forEach(
                t -> System.out.println(t)
        );
    }
}
