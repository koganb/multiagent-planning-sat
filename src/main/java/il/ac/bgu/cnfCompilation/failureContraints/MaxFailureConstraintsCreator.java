package il.ac.bgu.cnfCompilation.failureContraints;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import org.agreement_technologies.common.map_planner.Step;
import org.paukov.combinatorics3.Generator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Action.State.FAILED;

/**
 * Created by borisk on 11/27/2018.
 */
public class MaxFailureConstraintsCreator {

    private int maxFailuresNumber;
    private Map<Integer, Set<Step>> plan;

    public MaxFailureConstraintsCreator(int maxFailuresNumber, Map<Integer, Set<Step>> plan) {
        this.maxFailuresNumber = maxFailuresNumber;
        this.plan = plan;
    }

    @SuppressWarnings("UnstableApiUsage")
    public List<List<FormattableValue<Formattable>>> createMaxFailuresClauses() {

        ImmutableList<FormattableValue<Formattable>> allPossibleFailedClauses = plan.entrySet().stream().
                filter(i -> i.getKey() != -1).
                flatMap(entry -> entry.getValue().stream().flatMap(
                        step -> Stream.of(
                                FormattableValue.<Formattable>of(Action.of(step, entry.getKey(), FAILED), false)
                        ))).
                collect(ImmutableList.toImmutableList());

        return Generator.combination(allPossibleFailedClauses)
                .simple(maxFailuresNumber + 1)
                .stream()
                .collect(ImmutableList.toImmutableList());


    }

}
