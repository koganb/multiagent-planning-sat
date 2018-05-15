package il.ac.bgu;

import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_planner.POPPrecEff;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Created by Boris on 02/07/2017.
 */
public class CnfEncodingUtils {

    public static final String STATE_FORMAT = "Index:%02d, State:%s";
    private static Integer HARD_CONSTRAINTS_WEIGHT = Integer.MAX_VALUE;
    private static Integer SOFT_CONSTRAINTS_WEIGHT = 1;

    public static Boolean encodeValue(POPPrecEff precEff, Boolean encodeValue) {
        return Optional.ofNullable(BooleanUtils.toBooleanObject(precEff.getValue())).
                map(v -> encodeValue(v, encodeValue)).
                orElse(encodeValue);

    }

    public static Boolean encodeValue(Boolean currentValue, Boolean encodeValue) {
        return currentValue == encodeValue;

    }

    public static String createEffKey(POPPrecEff precEff) {
        return precEff.getFunction().toKey().replace(" ", "~");
    }

    public static String createEffId(POPPrecEff precEff) {
        String effKey = createEffKey(precEff);
        return Optional.ofNullable(BooleanUtils.toBooleanObject(precEff.getValue())).
                map(val -> format("%s", effKey)).  //boolean effect
                orElse(format("%s=%s", effKey, precEff.getValue()));
    }

    public static String createEffId(POPPrecEff precEff, Integer stage) {
        return format(STATE_FORMAT, stage, createEffId(precEff));
    }

    public static String createEffId(String effKey, Integer stage) {
        return format(STATE_FORMAT, stage, effKey);
    }

    public static String encodeAction(Step step) {
        return step.getActionName().replace(" ", "~");
    }

    public static String encodeActionKey(Step step, Integer stage, ActionState actionState) {
        return format("Index:%02d,Agent:%s,Action:%s=%s", stage, step.getAgent(), encodeAction(step), actionState.name());

    }

    public static ImmutablePair<String, Boolean> encodeActionState(Step step, Integer stage, ActionState actionState,
                                                                   Boolean focusActionStateValue) {
        String actionFormat = format("Index:%02d,Agent:%s,Action:%s=%s", stage, step.getAgent(), encodeAction(step), actionState.name());
        return ImmutablePair.of(encodeActionKey(step, stage, actionState), focusActionStateValue);

    }

    public enum ActionState {HEALTHY, FAILED, UNKNOWN}


    public static Pair<Map<String, Integer>, String> encode(List<List<ImmutablePair<String, Boolean>>> hardConstraints,
                                                            List<List<ImmutablePair<String, Boolean>>> softConstraints) {
        Map<String, Integer> planCodes = new HashMap<>();
        MutableInt currentCode = new MutableInt(0);

        String cnfCompilation = Stream.of(ImmutablePair.of(HARD_CONSTRAINTS_WEIGHT, hardConstraints),
                ImmutablePair.of(SOFT_CONSTRAINTS_WEIGHT, softConstraints)).
                flatMap(pair -> pair.getRight().stream().map(t -> ImmutablePair.of(pair.getLeft(), t))).
                map(pair -> {
                    String cnfClauseString = pair.getRight().stream().map(f -> {
                        Integer code = Optional.ofNullable(
                                //in case the code for literal do not exists put it to the map and update the counter
                                planCodes.
                                        putIfAbsent(f.getKey(), currentCode.getValue() + 1)).orElseGet(() -> {
                            currentCode.setValue(currentCode.getValue() + 1);
                            return currentCode.getValue();

                        });

                        return f.getRight() ? Integer.toString(code) : "-" + Integer.toString(code);

                    }).collect(Collectors.joining(" "));
                    return ImmutablePair.of(pair.getLeft(), cnfClauseString);
                }).
                map(pair -> String.format("%s %s 0",
                        pair.left,  //clause weight
                        pair.right  //clause
                )).

                //end zero and new line at the clause end
                        collect(Collectors.joining(System.lineSeparator()));

        String planCompilation = String.format("p wcnf %s %s %s",
                planCodes.size(), hardConstraints.size() + softConstraints.size(), HARD_CONSTRAINTS_WEIGHT) +
                System.lineSeparator() + cnfCompilation;

        return new ImmutablePair<>(planCodes, planCompilation);

    }
}
