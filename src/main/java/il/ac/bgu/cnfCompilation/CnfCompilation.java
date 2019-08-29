package il.ac.bgu.cnfCompilation;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.cnfClausesModel.CnfClausesFunction;
import il.ac.bgu.cnfCompilation.retries.RetryPlanUpdater;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.plan.PlanAction;
import il.ac.bgu.utils.PlanSolvingUtils;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Action.State.*;
//import static il.ac.bgu.dataModel.Variable.SpecialState.FREEZED;
import static il.ac.bgu.dataModel.Variable.SpecialState.LOCKED_FOR_UPDATE;
import static java.util.stream.Collectors.toSet;

/**
 * Created by Boris on 01/07/2017.
 */

@Slf4j
public class CnfCompilation {


    private Map<String, List<Variable>> variableStateMap;
    private Map<Integer, ImmutableList<PlanAction>> plan;


    private CnfClausesFunction healthyCnfClausesCreator;
    private CnfClausesFunction failedCnfClausesCreator;
    private CnfClausesFunction conflictCnfClausesCreator;


    public CnfCompilation(Map<Integer, ImmutableList<PlanAction>> plan,
                          RetryPlanUpdater retryPlanUpdater,
                          CnfClausesFunction healthyCnfClausesCreator,
                          CnfClausesFunction conflictCnfClausesCreator,
                          CnfClausesFunction failedCnfClausesCreator) {

        this.healthyCnfClausesCreator = healthyCnfClausesCreator;
        this.failedCnfClausesCreator = failedCnfClausesCreator;
        this.conflictCnfClausesCreator = conflictCnfClausesCreator;


        //update plans with retries - if configured
        this.plan = retryPlanUpdater.updatePlan(plan).updatedPlan;
        variableStateMap = PlanSolvingUtils.calcInitFacts(plan).stream()
                .map(v -> v.getFormattable().toBuilder().stage(null).build())
                .collect(Collectors.groupingBy(Variable::formatFunctionKey,
                        Collectors.toList()));


        log.debug("Initialized variable state to: {}", variableStateMap);
    }


    Stream<ImmutableList<FormattableValue<? extends Formattable>>> calculatePassThroughClauses(Integer stage, List<PlanAction> actions) {
        //calculate "pass through" variables

        //get prec & effects keys for stage actions
//        Set<String> precAndEffectKeys = actions.stream()
//                .flatMap(k -> Stream.concat(k.getPreconditions().stream(), k.getEffects().stream()))
//                .map(Variable::formatFunctionKey)
//                .collect(toSet());


        Set<String> effectKeys = actions.stream()
                .flatMap(k ->  k.getEffects().stream())
                .map(Variable::formatFunctionKey)
                .collect(toSet());


        log.debug("Start pass through...");

        Stream<ImmutableList<FormattableValue<? extends Formattable>>> passThroughValuesStream =
                this.variableStateMap.entrySet().stream()
                        .filter(entry -> !effectKeys.contains(entry.getKey()))
                        .flatMap(entry -> entry.getValue().stream())
                        .flatMap(g -> {
                            if (
                                    g.getValue().equals(LOCKED_FOR_UPDATE.name())
//                                    ||
//                                    g.getValue().equals(FREEZED.name())
                            ) {
                                return Stream.of(
                                        //locked_for_update is set to false on next stage
                                        ImmutableList.of(FormattableValue.of(g.toBuilder().stage(stage + 1).build(), false))
                                );
                            } else {
                                return Stream.of(
                                        ImmutableList.of(
                                                FormattableValue.of(g.toBuilder().functionValue(LOCKED_FOR_UPDATE.name()).stage(stage).build(), true),
                                                FormattableValue.of(g.toBuilder().stage(stage).build(), false),
                                                FormattableValue.of(g.toBuilder().stage(stage + 1).build(), true)),
                                        ImmutableList.of(
                                                FormattableValue.of(g.toBuilder().functionValue(LOCKED_FOR_UPDATE.name()).stage(stage).build(), true),
                                                FormattableValue.of(g.toBuilder().stage(stage).build(), true),
                                                FormattableValue.of(g.toBuilder().stage(stage + 1).build(), false)

                                        ));
                            }
                        });

        ImmutableList<ImmutableList<FormattableValue<? extends Formattable>>> passThroughValues =
                passThroughValuesStream.collect(ImmutableList.toImmutableList());

        log.debug("\n{}", passThroughValues.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End pass through");
        return passThroughValues.stream();
    }

    Stream<List<FormattableValue<? extends Formattable>>> calculateHealthyClauses(Integer stage) {
        return plan.get(stage).stream().flatMap(step ->
                this.healthyCnfClausesCreator.apply(stage, step, variableStateMap));

    }

    Stream<List<FormattableValue<? extends Formattable>>> calculateActionFailedClauses(Integer stage) {
        return plan.get(stage).stream().flatMap(step ->
                this.failedCnfClausesCreator.apply(stage, step, this.variableStateMap));
    }

    Stream<List<FormattableValue<? extends Formattable>>> calculateConditionsNotMetClauses(Integer stage) {
        return plan.get(stage).stream().flatMap(step ->
                this.conflictCnfClausesCreator.apply(stage, step, this.variableStateMap));
    }

    private Stream<ImmutableList<FormattableValue<? extends Formattable>>> addActionStatusConstraints(Integer stage, List<PlanAction> steps) {

        Stream<ImmutableList<FormattableValue<? extends Formattable>>> resultClausesStream = steps.stream()
                .flatMap(step ->
                        Stream.of(
                                ImmutableList.of(
                                        FormattableValue.of(Action.of(step, stage, HEALTHY), true),
                                        FormattableValue.of(Action.of(step, stage, FAILED), true),
                                        FormattableValue.of(Action.of(step, stage, CONDITIONS_NOT_MET), true)
                                ),
                                ImmutableList.of(
                                        FormattableValue.of(Action.of(step, stage, HEALTHY), true),
                                        FormattableValue.of(Action.of(step, stage, FAILED), false),
                                        FormattableValue.of(Action.of(step, stage, CONDITIONS_NOT_MET), false)
                                ),
                                ImmutableList.of(
                                        FormattableValue.of(Action.of(step, stage, HEALTHY), false),
                                        FormattableValue.of(Action.of(step, stage, FAILED), true),
                                        FormattableValue.of(Action.of(step, stage, CONDITIONS_NOT_MET), false)
                                ),
                                ImmutableList.of(
                                        FormattableValue.of(Action.of(step, stage, HEALTHY), false),
                                        FormattableValue.of(Action.of(step, stage, FAILED), false),
                                        FormattableValue.of(Action.of(step, stage, CONDITIONS_NOT_MET), true)
                                ),
                                ImmutableList.of(
                                        FormattableValue.of(Action.of(step, stage, HEALTHY), false),
                                        FormattableValue.of(Action.of(step, stage, FAILED), false),
                                        FormattableValue.of(Action.of(step, stage, CONDITIONS_NOT_MET), false)
                                )

                        ));

        ImmutableList<ImmutableList<FormattableValue<? extends Formattable>>> resultClauses = resultClausesStream.collect(ImmutableList.toImmutableList());

        return resultClauses.stream();
    }


    public List<List<FormattableValue<? extends Formattable>>> compileToCnf() {
        return plan.entrySet().stream().
                        filter(i -> i.getKey() != -1).
                flatMap(this::apply)
                .collect(ImmutableList.toImmutableList());
    }


    private Stream<? extends List<FormattableValue<? extends Formattable>>> apply(Map.Entry<Integer, ImmutableList<PlanAction>> entry) {
        return StreamEx.<List<FormattableValue<? extends Formattable>>>of()
                .append(addActionStatusConstraints(entry.getKey(), entry.getValue()))
                .append(calculatePassThroughClauses(entry.getKey(), entry.getValue()))
                .append(calculateHealthyClauses(entry.getKey()))
                .append(calculateActionFailedClauses(entry.getKey()))
                .append(calculateConditionsNotMetClauses(entry.getKey()));
    }
}
