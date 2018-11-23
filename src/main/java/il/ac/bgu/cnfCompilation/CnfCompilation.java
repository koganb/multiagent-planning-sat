package il.ac.bgu.cnfCompilation;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.cnfClausesModel.CnfClausesFunction;
import il.ac.bgu.cnfCompilation.retries.RetryPlanUpdater;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.variablesCalculation.FinalVariableStateCalc;
import lombok.extern.slf4j.Slf4j;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_planner.POPPrecEff;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.CnfCompilationUtils.calcVariableState;
import static il.ac.bgu.dataModel.Action.State.*;
import static il.ac.bgu.dataModel.Variable.SpecialState.*;
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
    private Map<Action, Action> actionDependencyMap;


    private CnfClausesFunction healthyCnfClausesCreator;
    private CnfClausesFunction failedCnfClausesCreator;
    private CnfClausesFunction conflictCnfClausesCreator;

    private FinalVariableStateCalc finalVariableStateCalc;


    public CnfCompilation(TreeMap<Integer, Set<Step>> plan,
                          RetryPlanUpdater retryPlanUpdater,
                          CnfClausesFunction healthyCnfClausesCreator,
                          CnfClausesFunction conflictCnfClausesCreator,
                          CnfClausesFunction failedCnfClausesCreator,
                          FinalVariableStateCalc finalVariableStateCalc) {

        this.healthyCnfClausesCreator = healthyCnfClausesCreator;
        this.failedCnfClausesCreator = failedCnfClausesCreator;
        this.conflictCnfClausesCreator = conflictCnfClausesCreator;
        this.finalVariableStateCalc = finalVariableStateCalc;


        //update plans with retries - if configured
        RetryPlanUpdater.RetriesPlanCreatorResult retriesPlanCreatorResult = retryPlanUpdater.updatePlan(plan);
        this.plan = retriesPlanCreatorResult.updatedPlan;
        actionDependencyMap = retriesPlanCreatorResult.actionDependencyMap;


        this.variablesStateBeforeStepExec = calcInitFacts();
        log.debug("Initialized variable state to: {}", variablesStateBeforeStepExec);
    }


    public List<FormattableValue<Variable>> calcInitFacts() {
        return plan.entrySet().stream().filter(i -> i.getKey() == -1).
                flatMap(t -> t.getValue().stream()).
                flatMap(t -> t.getPopEffs().stream()).
                flatMap(eff -> Stream.of(
                        FormattableValue.of(Variable.of(eff, INITIAL_STAGE), true),
                        FormattableValue.of(Variable.of(eff, LOCKED_FOR_UPDATE.name(), INITIAL_STAGE), false),
                        FormattableValue.of(Variable.of(eff, FREEZED.name(), INITIAL_STAGE), false),
                        FormattableValue.of(Variable.of(eff, IN_CONFLICT_RETRY.name(), INITIAL_STAGE), false)
                )).
                collect(Collectors.toList());
    }


    public ImmutableList<FormattableValue<Formattable>> encodeHealthyClauses() {

        ImmutableList<FormattableValue<Formattable>> healthyClauses =

                plan.entrySet().stream().
                        filter(i -> i.getKey() != -1).
                        flatMap(entry -> entry.getValue().stream().flatMap(
                                step -> Stream.of(
                                        FormattableValue.<Formattable>of(Action.of(step, entry.getKey(), HEALTHY), true)
                                ))).
                        collect(ImmutableList.toImmutableList());

        log.trace("healthy clauses {}", healthyClauses);
        return healthyClauses;
    }


    public ImmutableList<FormattableValue<Formattable>> calcFinalFacts(Collection<Action> failedActions) {

        log.debug("Start final values calculation");

        ImmutableList<FormattableValue<Formattable>> finalValues =
                finalVariableStateCalc.getFinalVariableState(failedActions);
        log.debug("Final Values: \n{}", finalValues.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End final values calculation");


        return finalValues;
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
                                    g.getFormattable().getValue().equals(FREEZED.name()) ||
                                    g.getFormattable().getValue().equals(IN_CONFLICT_RETRY.name())) {
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
                this.healthyCnfClausesCreator.apply(stage, step, ImmutableList.copyOf(variablesStateAfterStepExec),
                        actionDependencyMap.get(Action.of(step, stage))));
    }

    Stream<ImmutableList<FormattableValue<Formattable>>> calculateActionFailedClauses(Integer stage) {
        return plan.get(stage).stream().flatMap(step ->
                this.failedCnfClausesCreator.apply(stage, step, ImmutableList.copyOf(variablesStateBeforeStepExec),
                        actionDependencyMap.get(Action.of(step, stage))));
    }

    Stream<ImmutableList<FormattableValue<Formattable>>> calculateConditionsNotMetClauses(Integer stage) {
        return plan.get(stage).stream().flatMap(step ->
                this.conflictCnfClausesCreator.apply(stage, step, ImmutableList.copyOf(variablesStateAfterStepExec),
                        actionDependencyMap.get(Action.of(step, stage))));
    }

    Stream<ImmutableList<FormattableValue<Formattable>>> addActionStatusConstraints(Integer stage, Set<Step> actions) {
        Stream<ImmutableList<FormattableValue<Formattable>>> resultClausesStream = actions.stream().flatMap(action ->
                Stream.of(
                        ImmutableList.of(
                                FormattableValue.of(Action.of(action, stage, HEALTHY), true),
                                FormattableValue.of(Action.of(action, stage, FAILED), true),
                                FormattableValue.of(Action.of(action, stage, CONDITIONS_NOT_MET), true)
                        ),
                        ImmutableList.of(
                                FormattableValue.of(Action.of(action, stage, HEALTHY), true),
                                FormattableValue.of(Action.of(action, stage, FAILED), false),
                                FormattableValue.of(Action.of(action, stage, CONDITIONS_NOT_MET), false)
                        ),
                        ImmutableList.of(
                                FormattableValue.of(Action.of(action, stage, HEALTHY), false),
                                FormattableValue.of(Action.of(action, stage, FAILED), true),
                                FormattableValue.of(Action.of(action, stage, CONDITIONS_NOT_MET), false)
                        ),
                        ImmutableList.of(
                                FormattableValue.of(Action.of(action, stage, HEALTHY), false),
                                FormattableValue.of(Action.of(action, stage, FAILED), false),
                                FormattableValue.of(Action.of(action, stage, CONDITIONS_NOT_MET), true)
                        ),
                        ImmutableList.of(
                                FormattableValue.of(Action.of(action, stage, HEALTHY), false),
                                FormattableValue.of(Action.of(action, stage, FAILED), false),
                                FormattableValue.of(Action.of(action, stage, CONDITIONS_NOT_MET), false)
                        )

                ));

        ImmutableList<ImmutableList<FormattableValue<Formattable>>> resultClauses = resultClausesStream.collect(ImmutableList.toImmutableList());

        return resultClauses.stream();
    }


    public ImmutableList<ImmutableList<FormattableValue<Formattable>>> compileToCnf() {
        Stream<ImmutableList<FormattableValue<Formattable>>> cnfClauses =
                plan.entrySet().stream().
                        filter(i -> i.getKey() != -1).
                        flatMap(entry -> {
                            return Stream.concat(
                                    Stream.concat(
                                            Stream.concat(
                                                    Stream.concat(
                                                            Stream.concat(
                                                                    addActionStatusConstraints(entry.getKey(), entry.getValue()),
                                                                    executeStageAndAddFluents(entry.getKey(), entry.getValue())),
                                                            calculatePassThroughClauses(entry.getKey(), entry.getValue())),
                                                    calculateHealthyClauses(entry.getKey())),
                                            calculateActionFailedClauses(entry.getKey())),
                                    calculateConditionsNotMetClauses(entry.getKey())
                            );
                        });

        return cnfClauses.collect(ImmutableList.toImmutableList());
    }


    Stream<ImmutableList<FormattableValue<Formattable>>> executeStageAndAddFluents(Integer stage, Set<Step> actions) {
        log.debug("Add new fluents to the variable state");

        Set<FormattableValue<Formattable>> newFluents = new HashSet<>();

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

        //find the difference between before and after
        Set<String> beforeKeyWithValue = calcVariableState(variablesStateBeforeStepExec.stream(), stage)
                .map(var -> var.getFormattable().formatFunctionKeyWithValue())
                .collect(toSet());
        calcVariableState(variablesStateAfterStepExec.stream(), stage + 1)
                .filter(var -> !beforeKeyWithValue.contains(
                        var.getFormattable().formatFunctionKeyWithValue()))
                .forEach(var -> {
                    Variable variable = var.getFormattable().toBuilder().stage(stage).build();
                    newFluents.add(FormattableValue.of(variable, false));

                    Variable lockedVariable = variable.toBuilder().functionValue(LOCKED_FOR_UPDATE.name()).build();
                    //this is new variable key
                    if (!beforeKeyWithValue.contains(lockedVariable.formatFunctionKeyWithValue())) {
                        newFluents.add(FormattableValue.of(lockedVariable, false));
                    }
                });


        log.debug("Adding effect to the var state {}\n", newFluents.stream().map(Objects::toString).collect(Collectors.joining("\n")));

        return newFluents.stream().map(ImmutableList::of);
    }


}
