package il.ac.bgu.cnfCompilation;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.cnfClausesModel.CnfClausesFunction;
import il.ac.bgu.cnfCompilation.retries.RetryPlanUpdater;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_planner.POPPrecEff;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Action.State.*;
import static il.ac.bgu.dataModel.Variable.SpecialState.FREEZED;
import static il.ac.bgu.dataModel.Variable.SpecialState.LOCKED_FOR_UPDATE;
import static il.ac.bgu.utils.CnfCompilationUtils.calcVariableState;
import static il.ac.bgu.variableModel.VariableModelFunction.VARIABLE_TYPE.EFFECT;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Created by Boris on 01/07/2017.
 */

@Slf4j
public class CnfCompilation {

    public static final int INITIAL_STAGE = 0;
    private List<FormattableValue<Variable>> variablesStateBeforeStepExec;
    private List<FormattableValue<Variable>> variablesStateAfterStepExec;


    private Map<Integer, Set<Step>> plan;


    private CnfClausesFunction healthyCnfClausesCreator;
    private CnfClausesFunction failedCnfClausesCreator;
    private CnfClausesFunction conflictCnfClausesCreator;


    public CnfCompilation(Map<Integer, Set<Step>> plan,
                          RetryPlanUpdater retryPlanUpdater,
                          CnfClausesFunction healthyCnfClausesCreator,
                          CnfClausesFunction conflictCnfClausesCreator,
                          CnfClausesFunction failedCnfClausesCreator) {

        this.healthyCnfClausesCreator = healthyCnfClausesCreator;
        this.failedCnfClausesCreator = failedCnfClausesCreator;
        this.conflictCnfClausesCreator = conflictCnfClausesCreator;


        //update plans with retries - if configured
        RetryPlanUpdater.RetriesPlanCreatorResult retriesPlanCreatorResult = retryPlanUpdater.updatePlan(plan);

        this.plan = retriesPlanCreatorResult.updatedPlan;

        this.variablesStateBeforeStepExec = calcInitFacts();
        log.debug("Initialized variable state to: {}", variablesStateBeforeStepExec);
    }


    public List<FormattableValue<Variable>> calcInitFacts() {

        //true facts added at initial stage
        Map<String, FormattableValue<Variable>> initStageVars = plan.entrySet().stream()
                .filter(i -> i.getKey() == -1)
                .flatMap(t -> t.getValue().stream())
                .flatMap(t -> t.getPopEffs().stream())
                .map(eff -> FormattableValue.of(Variable.of(eff, INITIAL_STAGE), true))
                .collect(Collectors.toMap(p -> p.getFormattable().formatFunctionKeyWithValue(), Function.identity()));

        //action effects that are not true at initial stage
        Map<String, FormattableValue<Variable>> allStageVars = plan.entrySet().stream()
                .filter(i -> i.getKey() != -1)
                .flatMap(t -> t.getValue().stream())
                .flatMap(t -> t.getPopEffs().stream())
                .filter(eff -> !initStageVars.keySet().contains(Variable.of(eff).formatFunctionKeyWithValue()))
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


    Stream<ImmutableList<FormattableValue<Formattable>>> calculatePassThroughClauses(Integer stage, Set<Step> actions) {
        //calculate "pass through" variables

        //get prec & effects keys for stage actions
        Set<String> precAndEffectKeys = actions.stream()
                .flatMap(k -> Stream.concat(k.getPopPrecs().stream(), k.getPopEffs().stream()))
                .map(precEff -> Variable.of(precEff).formatFunctionKey())
                .collect(toSet());

        log.debug("Start pass through...");

        Stream<ImmutableList<FormattableValue<Formattable>>> passThroughValuesStream =
                calcVariableState(variablesStateBeforeStepExec.stream(), stage).
                        filter(value -> !precAndEffectKeys.contains(value.getFormattable().formatFunctionKey())).
                        flatMap(g -> {
                            if (g.getFormattable().getValue().equals(LOCKED_FOR_UPDATE.name()) ||
                                    g.getFormattable().getValue().equals(FREEZED.name())) {
                                return Stream.of(
                                        //locked_for_update is set to false on next stage
                                        ImmutableList.of(
                                                FormattableValue.of((g.getFormattable()).toBuilder().stage(stage + 1).build(), false))
                                );
                            } else {
                                return Stream.of(
                                        ImmutableList.of(
                                                FormattableValue.of((g.getFormattable()).toBuilder().functionValue(LOCKED_FOR_UPDATE.name()).stage(stage).build(), true),
                                                FormattableValue.of((g.getFormattable()).toBuilder().stage(stage).build(), false),
                                                FormattableValue.of((g.getFormattable()).toBuilder().stage(stage + 1).build(), true)),
                                        ImmutableList.of(
                                                FormattableValue.of((g.getFormattable()).toBuilder().functionValue(LOCKED_FOR_UPDATE.name()).stage(stage).build(), true),
                                                FormattableValue.of((g.getFormattable()).toBuilder().stage(stage).build(), true),
                                                FormattableValue.of((g.getFormattable()).toBuilder().stage(stage + 1).build(), false)

                                        ));
                            }
                        });

        ImmutableList<ImmutableList<FormattableValue<Formattable>>> passThroughValues =
                passThroughValuesStream.collect(ImmutableList.toImmutableList());

        log.debug("\n{}", passThroughValues.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End pass through");
        return passThroughValues.stream();
    }

    Stream<ImmutableList<FormattableValue<Formattable>>> calculateHealthyClauses(Integer stage) {
        return plan.get(stage).stream().flatMap(step ->
                this.healthyCnfClausesCreator.apply(stage, step, ImmutableList.copyOf(variablesStateAfterStepExec)));

    }

    Stream<ImmutableList<FormattableValue<Formattable>>> calculateActionFailedClauses(Integer stage) {
        return plan.get(stage).stream().flatMap(step ->
                this.failedCnfClausesCreator.apply(stage, step, ImmutableList.copyOf(variablesStateBeforeStepExec)));
    }

    Stream<ImmutableList<FormattableValue<Formattable>>> calculateConditionsNotMetClauses(Integer stage) {
        return plan.get(stage).stream().flatMap(step ->
                this.conflictCnfClausesCreator.apply(stage, step, ImmutableList.copyOf(variablesStateAfterStepExec)));
    }

    Stream<ImmutableList<FormattableValue<Formattable>>> addActionStatusConstraints(Integer stage, Set<Step> steps) {

        Stream<ImmutableList<FormattableValue<Formattable>>> resultClausesStream = steps.stream()
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

        ImmutableList<ImmutableList<FormattableValue<Formattable>>> resultClauses = resultClausesStream.collect(ImmutableList.toImmutableList());

        return resultClauses.stream();
    }


    public List<List<FormattableValue<Formattable>>> compileToCnf() {
        return plan.entrySet().stream().
                        filter(i -> i.getKey() != -1).
                        flatMap(entry -> {
                            executeStage(entry.getKey(), entry.getValue());
                            return StreamEx.<List<FormattableValue<Formattable>>>of()
                                    .append(addActionStatusConstraints(entry.getKey(), entry.getValue()))
                                    .append(calculatePassThroughClauses(entry.getKey(), entry.getValue()))
                                    .append(calculateHealthyClauses(entry.getKey()))
                                    .append(calculateActionFailedClauses(entry.getKey()))
                                    .append(calculateConditionsNotMetClauses(entry.getKey()));

                        })
                .collect(ImmutableList.toImmutableList());
    }


    void executeStage(Integer stage, Set<Step> actions) {
        log.debug("Execute stage {} steps {}", stage, actions);

        //copy after variables from the previous stage if exist
        if (variablesStateAfterStepExec != null) {
            variablesStateBeforeStepExec = ImmutableList.copyOf(variablesStateAfterStepExec);
        }

        //update state according to step effects
        variablesStateAfterStepExec = ImmutableList.copyOf(variablesStateBeforeStepExec);
        for (Step action : actions) {
            for (POPPrecEff eff : action.getPopEffs()) {
                variablesStateAfterStepExec = this.healthyCnfClausesCreator.getVariableModel().apply(
                        Variable.of(eff), stage, variablesStateAfterStepExec, EFFECT)
                        .collect(toList());

            }
        }
    }


}
