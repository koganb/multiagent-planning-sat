package il.ac.bgu;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Boris on 02/07/2017.
 */
public class CnfEncodingUtils {

    private static Integer TOP_WEIGHT = Integer.MAX_VALUE;

    public static Pair<Map<String, Integer>, String> encode(List<List<ImmutablePair<String, Boolean>>> plan,
                                                            List<String> healthClauses) {
        Map<String, Integer> planCodes = new HashMap<>();
        MutableInt currentCode = new MutableInt(0);

        String planCompilation = plan.stream().map(t -> t.stream().map(f -> {
            Integer code = Optional.ofNullable(
                    //in case the code for literal do not exists put it to the map and update the counter
                    planCodes.
                            putIfAbsent(f.getKey(), currentCode.getValue() + 1)).orElseGet(() -> {
                currentCode.setValue(currentCode.getValue() + 1);
                return currentCode.getValue();

            });

            return f.getRight() ? Integer.toString(code) : "-" + Integer.toString(code);

        }).
                //separate the literals with blanks
                        collect(Collectors.joining(" "))).
                map(clause -> String.format("%s %s 0", TOP_WEIGHT, clause)).


                //end zero and new line at the clause end
                        collect(Collectors.joining(System.lineSeparator()));


        String healthClausesCompilation = healthClauses.stream().
                map(healthClause -> String.format("%s %s 0", 1, planCodes.get(healthClause))).
                collect(Collectors.joining(System.lineSeparator()));


        planCompilation = String.format("p wcnf %s %s %s",
                planCodes.size(), plan.size() + healthClauses.size(), TOP_WEIGHT) +
                System.lineSeparator() + planCompilation + System.lineSeparator() + healthClausesCompilation;

        return new ImmutablePair<>(planCodes, planCompilation);

    }
}
