package il.ac.bgu.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import il.ac.bgu.cnfClausesModel.CnfClausesFunction;
import il.ac.bgu.cnfCompilation.CnfCompilation;
import il.ac.bgu.cnfCompilation.retries.RetryPlanUpdater;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.plan.PlanAction;
import il.ac.bgu.sat.DiagnosisFindingStopIndicator;
import il.ac.bgu.sat.SolutionIterator;
import il.ac.bgu.variablesCalculation.FinalVariableStateCalc;
import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.apache.commons.collections4.ListUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Variable.SpecialState.FREEZED;
import static il.ac.bgu.dataModel.Variable.SpecialState.LOCKED_FOR_UPDATE;
import static java.util.stream.Collectors.toList;
import static org.slf4j.MarkerFactory.getMarker;

/**
 * Created by borisk on 12/4/2018.
 */
@Slf4j
public class PlanSolvingUtils {

    public static final int INITIAL_STAGE = 0;


    public static List<Either<Throwable, List<? extends Formattable>>> calculateSolutions(
            Map<Integer, ImmutableList<PlanAction>> plan,
            List<List<FormattableValue<? extends Formattable>>> hardConstraints,
            List<FormattableValue<Formattable>> softConstraints,
            FinalVariableStateCalc finalVariableStateCalc,
            Collection<Action> failedActions,
            Long satTimeoutMils,
            DiagnosisFindingStopIndicator stopIndicator) {


        log.info("final facts calculation");
        Instant start = Instant.now();
        ImmutableList<FormattableValue<? extends Formattable>> finalFacts =
                finalVariableStateCalc.getFinalVariableState(failedActions);
        log.info(getMarker("STATS"), "    final_var_state_calc_mils: {}", Duration.between(start, Instant.now()).toMillis());

        List<List<FormattableValue<? extends Formattable>>> hardConstraintsWithFinal =
                StreamEx.<List<FormattableValue<? extends Formattable>>>of()
                        .append(hardConstraints)
                        .append(finalFacts.stream().map(ImmutableList::of))
                        .collect(ImmutableList.toImmutableList());

        if (log.isDebugEnabled()) {
            log.debug("Hard contraints: {} ",
                    hardConstraintsWithFinal.stream()
                            .map(t -> t.stream().map(FormattableValue::toString).collect(Collectors.joining("\n")))
                            .collect(Collectors.joining("\n")));
        }

        SolutionIterator solutionIterator = new SolutionIterator(plan, hardConstraintsWithFinal, softConstraints,
                satTimeoutMils, stopIndicator);

        log.info(getMarker("STATS"), "    sat_solving_mils:");
        List<Either<Throwable, List<? extends Formattable>>> results =
                Streams.stream(solutionIterator)
                        .filter(t -> t.isLeft() || (t.isRight() && t.get().isPresent()))
                        .map(t -> {
                            Either.RightProjection<Throwable, List<? extends Formattable>> res = t.right().map(Optional::get);
                            return res.toEither();
                        })
                        .collect(Collectors.toList());


        results.stream().filter(Either::isRight).map(Either::get).forEach(solution -> {
             log.info("Solution candidate: {}", solution);

                    List<FormattableValue<? extends Formattable>> solutionFinalVariablesState = finalVariableStateCalc.getFinalVariableState(solution);

                    if (ListUtils.intersection(finalFacts, solutionFinalVariablesState).size() !=
                            solutionFinalVariablesState.size()) {
                        throw new RuntimeException("Not equal final states: failedActionsFinalVariablesState: and solutionFinalVariablesState" +
                                ListUtils.subtract(finalFacts, solutionFinalVariablesState));

                    }
                }
        );

        return results;
    }


    public static List<List<FormattableValue<? extends Formattable>>> createPlanHardConstraints(Map<Integer, ImmutableList<PlanAction>> plan,
                                                                                                RetryPlanUpdater retryPlanUpdater,
                                                                                                CnfClausesFunction healthyCnfClausesCreator,
                                                                                                CnfClausesFunction conflictCnfClausesCreator,
                                                                                                CnfClausesFunction failedCnfClausesCreator) {


        CnfCompilation cnfCompilation = new CnfCompilation(plan, retryPlanUpdater, healthyCnfClausesCreator,
                conflictCnfClausesCreator, failedCnfClausesCreator);

        Stream<List<FormattableValue<? extends Formattable>>> initFacts =
                calcInitFacts(plan).stream().map(ImmutableList::of);

        return StreamEx.<List<FormattableValue<? extends Formattable>>>of()
                .append(cnfCompilation.compileToCnf())
                .append(initFacts)
                .collect(ImmutableList.toImmutableList());


    }

    public static List<FormattableValue<Variable>> calcInitFacts(Map<Integer, ImmutableList<PlanAction>> plan) {

        //true facts added at initial stage
        Map<String, FormattableValue<Variable>> initStageVars = plan.entrySet().stream()
                .filter(i -> i.getKey() == -1)
                .flatMap(t -> t.getValue().stream())
                .flatMap(t -> t.getEffects().stream())
                .map(eff -> FormattableValue.of(Variable.of(eff,INITIAL_STAGE), true))
                .collect(Collectors.toMap(p -> p.getFormattable().formatFunctionKeyWithValue(), Function.identity()));

        //action effects that are not true at initial stage
        Map<String, FormattableValue<Variable>> allStageVars = plan.entrySet().stream()
                .filter(i -> i.getKey() != -1)
                .flatMap(t -> t.getValue().stream())
                .flatMap(t -> t.getEffects().stream())
                .filter(eff -> !initStageVars.keySet().contains(eff.formatFunctionKeyWithValue()))
                .map(eff -> FormattableValue.of(Variable.of(eff, INITIAL_STAGE), false))
                .collect(Collectors.toMap(p -> p.getFormattable().formatFunctionKeyWithValue(), Function.identity(), (a, b) -> a));

        //locked and freezed vars for every variable key
        List<FormattableValue<Variable>> lockedAndFreezedVars = StreamEx.<FormattableValue<Variable>>of()
                .append(initStageVars.values())
                .append(allStageVars.values())
                .collect(Collectors.toMap(p -> p.getFormattable().formatFunctionKey(), Function.identity(), (a, b) -> a))
                .values().stream()
                .flatMap(v ->
                        Stream.of(
                                FormattableValue.of(Variable.of(v.getFormattable(), LOCKED_FOR_UPDATE.name(), INITIAL_STAGE), false),
                                FormattableValue.of(Variable.of(v.getFormattable(), FREEZED.name(), INITIAL_STAGE), false)

                        ))
                .collect(toList());

        List<FormattableValue<Variable>> initVars = StreamEx.<FormattableValue<Variable>>of()
                .append(initStageVars.values())
                .append(allStageVars.values())
                .append(lockedAndFreezedVars)
                .collect(Collectors.toList());

        log.debug("Init vars: {} ", initVars);

        return initVars;


    }
}
