package il.ac.bgu.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.sat.SolutionIterator;
import il.ac.bgu.variablesCalculation.FinalVariableStateCalc;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.collections4.ListUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.slf4j.MarkerFactory.getMarker;

/**
 * Created by borisk on 12/4/2018.
 */
@Slf4j
public class PlanSolvingUtils {

    public static Stream<List<Formattable>> calculateSolutions(
            Map<Integer, Set<Step>> plan,
            List<List<FormattableValue<Formattable>>> hardConstraints,
            List<FormattableValue<Formattable>> softConstraints,
            FinalVariableStateCalc finalVariableStateCalc,
            Collection<Action> failedActions) {


        log.info("final facts calculation");
        Instant start = Instant.now();
        ImmutableList<FormattableValue<Formattable>> finalFacts =
                finalVariableStateCalc.getFinalVariableState(failedActions);
        log.info(getMarker("STATS"), "    final_var_state_calc_mils: {}", Duration.between(start, Instant.now()).toMillis());

        //noinspection GroovyAssignabilityCheck
        List<List<FormattableValue<Formattable>>> hardConstraintsWithFinal =
                StreamEx.<List<FormattableValue<Formattable>>>of()
                        .append(hardConstraints)
                        .append(finalFacts.stream().map(ImmutableList::of))
                        .collect(ImmutableList.toImmutableList());

        if (log.isDebugEnabled()) {
            log.debug("Hard contraints: {} ",
                    hardConstraintsWithFinal.stream()
                            .map(t -> t.stream().map(FormattableValue::toString).collect(Collectors.joining("\n")))
                            .collect(Collectors.joining("\n")));
        }

        SolutionIterator solutionIterator = new SolutionIterator(plan, hardConstraintsWithFinal, softConstraints);

        log.info(getMarker("STATS"), "    sat_solving_mils:");
        return Streams.stream(solutionIterator)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .peek(solution -> {
                    log.info("Solution candidate: {}", solution);

                    ImmutableList<FormattableValue<Formattable>> solutionFinalVariablesState = finalVariableStateCalc.getFinalVariableState(solution);

                    if (ListUtils.intersection(finalFacts, solutionFinalVariablesState).size() !=
                            solutionFinalVariablesState.size()) {
                        throw new RuntimeException("Not equal final states: failedActionsFinalVariablesState: and solutionFinalVariablesState" +
                                ListUtils.subtract(finalFacts, solutionFinalVariablesState));

                    }
                });


    }

}
