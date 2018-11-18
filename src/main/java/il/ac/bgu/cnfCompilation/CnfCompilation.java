package il.ac.bgu.cnfCompilation;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.FinalVariableStateCalc;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.failureModel.SuccessVariableModel;
import il.ac.bgu.failureModel.VariableModelFunction;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_planner.POPPrecEff;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static il.ac.bgu.CnfCompilationUtils.calcVariableState;
import static il.ac.bgu.VariableFunctions.variableKeyFilter;
import static il.ac.bgu.dataModel.Action.State.*;
import static il.ac.bgu.dataModel.Variable.FREEZED;
import static il.ac.bgu.dataModel.Variable.LOCKED_FOR_UPDATE;
import static il.ac.bgu.failureModel.VariableModelFunction.NEXT_STEP_ADDITION;
import static il.ac.bgu.failureModel.VariableModelFunction.VARIABLE_TYPE.EFFECT;
import static il.ac.bgu.failureModel.VariableModelFunction.VARIABLE_TYPE.PRECONDITION;
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


    private TreeMap<Integer, Set<Step>> plan;

    private VariableModelFunction failureModel;
    private VariableModelFunction successModel = new SuccessVariableModel();


    public CnfCompilation(TreeMap<Integer, Set<Step>> plan, VariableModelFunction failureModel) {
        this.plan = plan;
        this.failureModel = failureModel;
        this.variablesStateBeforeStepExec = calcInitFacts();
        log.debug("Initialized variable state to: {}", variablesStateBeforeStepExec);
    }


    public List<FormattableValue<Variable>> calcInitFacts() {
        return plan.entrySet().stream().filter(i -> i.getKey() == -1).
                flatMap(t -> t.getValue().stream()).
                flatMap(t -> t.getPopEffs().stream()).
                flatMap(eff -> Stream.of(
                        FormattableValue.of(Variable.of(eff, INITIAL_STAGE), true),
                        FormattableValue.of(Variable.of(eff, LOCKED_FOR_UPDATE, INITIAL_STAGE), false),
                        FormattableValue.of(Variable.of(eff, FREEZED, INITIAL_STAGE), false)
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
                new FinalVariableStateCalc(plan, failureModel).getFinalVariableState(failedActions);
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
                            if (g.getFormattable().getValue().equals(LOCKED_FOR_UPDATE) ||
                                    g.getFormattable().getValue().equals(FREEZED)) {
                                return Stream.of(
                                        //locked_for_update is set to false on next stage
                                        ImmutableList.of(
                                                FormattableValue.of((g.getFormattable()).toBuilder().stage(stage + 1).build(), false))
                                );
                            } else {
                                return Stream.of(
                                        ImmutableList.of(
                                                FormattableValue.of((g.getFormattable()).toBuilder().functionValue(LOCKED_FOR_UPDATE).stage(stage).build(), true),
                                                FormattableValue.of((g.getFormattable()).toBuilder().stage(stage).build(), false),
                                                FormattableValue.of((g.getFormattable()).toBuilder().stage(stage + 1).build(), true)),
                                        ImmutableList.of(
                                                FormattableValue.of((g.getFormattable()).toBuilder().functionValue(LOCKED_FOR_UPDATE).stage(stage).build(), true),
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


    Stream<ImmutableList<FormattableValue<Formattable>>> calculateHealthyClauses(Integer stage, Set<Step> actions) {
        log.debug("Start add healthy clause");

        Collection<ImmutableList<FormattableValue<Formattable>>> resultClauses = actions.stream().flatMap(action -> {

            ImmutableList<FormattableValue<Formattable>> preconditionList =
                    Stream.concat(
                            action.getPopPrecs().stream().
                                    map(actionPrec -> FormattableValue.<Formattable>of(Variable.of(actionPrec, stage), false)),
                            action.getPopEffs().stream().
                                    flatMap(actionEff ->
                                            Stream.of(
                                                    FormattableValue.<Formattable>of(Variable.of(actionEff, FREEZED, stage), true),
                                                    FormattableValue.<Formattable>of(Variable.of(actionEff, LOCKED_FOR_UPDATE, stage), true)
                                            )
                                    )
                    ).collect(ImmutableList.toImmutableList());


            Set<String> actionEffKeys = action.getPopEffs().stream()
                    .map(eff -> Variable.of(eff).formatFunctionKey())
                    .collect(Collectors.toSet());

            //healthy function
            Stream<FormattableValue<Formattable>> effectStream =
                    Stream.concat(
                            action.getPopPrecs().stream()
                                    .filter(prec ->
                                            !actionEffKeys.contains(Variable.of(prec).formatFunctionKey())),
                            action.getPopEffs().stream()
                    ).flatMap(actionEff -> {
                        Predicate<FormattableValue<Variable>> variableKeyPredicate = variableKeyFilter.apply(Variable.of(actionEff));

                        //prec or effect variable
                        return calcVariableState(variablesStateAfterStepExec.stream(), stage + 1)
                                .filter(variableKeyPredicate)
                                .map(formattableValue ->
                                        FormattableValue.of(
                                                formattableValue.getFormattable().toBuilder().stage(stage + 1).build(),
                                                formattableValue.getValue()));
                    });


            return effectStream.map(u ->
                    Stream.concat(
                            preconditionList.stream(),
                            Stream.of(FormattableValue.<Formattable>of(Action.of(action, stage, HEALTHY), false), u)).
                            collect(ImmutableList.toImmutableList())
            );
        }).collect(ImmutableList.toImmutableList());

        log.debug("healthy clauses\n{}", resultClauses.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End add healthy clause");

        return resultClauses.stream();
    }

    Stream<ImmutableList<FormattableValue<Formattable>>> calculateActionFailedClauses(
            Integer stage, Set<Step> actions) {
        log.debug("Start failed clause");

        Stream<ImmutableList<FormattableValue<Formattable>>> resultClausesStream = actions.stream().flatMap(action -> {

            ImmutableList<FormattableValue<Formattable>> preconditionList =
                    Stream.concat(
                            action.getPopPrecs().stream()
                                    .map(actionPrec -> FormattableValue.<Formattable>of(
                                            Variable.of(actionPrec, stage), false)),
                            action.getPopEffs().stream()
                                    .flatMap(actionEff ->
                                            Stream.of(
                                                    FormattableValue.<Formattable>of(
                                                            Variable.of(actionEff, LOCKED_FOR_UPDATE, stage), true),
                                                    FormattableValue.<Formattable>of(
                                                            Variable.of(actionEff, FREEZED, stage), true)
                                            ))
                    ).collect(ImmutableList.toImmutableList());


            Set<String> actionEffKeys = action.getPopEffs().stream()
                    .map(eff -> Variable.of(eff).formatFunctionKey())
                    .collect(Collectors.toSet());


            Stream<FormattableValue<Formattable>> effectStream =
                    Stream.concat(
                            action.getPopPrecs().stream()
                                    .map(prec -> ImmutablePair.of(Variable.of(prec), PRECONDITION))
                                    .filter(pair -> !actionEffKeys.contains(pair.getLeft().formatFunctionKey()))
                            ,
                            action.getPopEffs().stream()
                                    .map(eff -> ImmutablePair.of(Variable.of(eff), EFFECT))
                    ).
                            flatMap(precEffPair -> {
                                Variable variable = precEffPair.getLeft();
                                Predicate<FormattableValue<Variable>> variableKeyPredicate = variableKeyFilter.apply(variable);

                                ImmutableList<FormattableValue<Variable>> failureModelVariables =
                                        failureModel.apply(variable, stage, variablesStateBeforeStepExec, precEffPair.getRight()).
                                                collect(ImmutableList.toImmutableList());
                                return IntStream.rangeClosed(stage + NEXT_STEP_ADDITION, stage + failureModel.affectedStepsNumber())
                                        .boxed()
                                        .flatMap(stepAddition ->
                                                calcVariableState(failureModelVariables.stream(), stepAddition)
                                                        .filter(variableKeyPredicate)
                                                        .map(var -> FormattableValue.of(var.getFormattable().toBuilder().stage(stepAddition).build(),
                                                                var.getValue())));
                            });

            List<FormattableValue<Formattable>> effects = effectStream.collect(toList());


            return effects.stream().map(u ->
                    Stream.concat(
                            preconditionList.stream(),
                            Stream.of(FormattableValue.<Formattable>of(Action.of(action, stage, FAILED), false), u)).
                            collect(ImmutableList.toImmutableList())
            );
        });

        ImmutableList<ImmutableList<FormattableValue<Formattable>>> resultClauses =
                resultClausesStream.collect(ImmutableList.toImmutableList());

        log.debug("failed clauses\n{}", resultClauses.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End failed clause");

        return resultClauses.stream();
    }


    Stream<ImmutableList<FormattableValue<Formattable>>> calculateConditionsNotMetClauses(Integer stage, Set<Step> actions) {

        log.debug("Start conditions not met clause");

        Stream<ImmutableList<FormattableValue<Formattable>>> resultClausesStream = actions.stream().flatMap(action -> {

            // CONDITIONS_NOT_MET -> not(prec1) v not(prec2) => not(unknown) v not(prec1) v not(prec2)
            Stream<ImmutableList<FormattableValue<Formattable>>> precClauses1 = Stream.of(
                    StreamEx.<FormattableValue<Formattable>>of()
                            .append(Stream.of(FormattableValue.of(Action.of(action, stage, CONDITIONS_NOT_MET), false)))
                            .append(action.getPopPrecs().stream().map(actionPrec ->
                                    FormattableValue.of(Variable.of(actionPrec, stage), false)))
                            .append(action.getPopEffs().stream().map(actionEff ->
                                    FormattableValue.of(Variable.of(actionEff, LOCKED_FOR_UPDATE, stage), true)))
                            .append(action.getPopEffs().stream().map(actionEff ->
                                    FormattableValue.of(Variable.of(actionEff, FREEZED, stage), true)))
                            .collect(ImmutableList.toImmutableList()));


            // not(prec1) v not(prec2) -> CONDITIONS_NOT_MET => (prec1 v CONDITIONS_NOT_MET) ^  (prec2 v CONDITIONS_NOT_MET)
            Stream<ImmutableList<FormattableValue<Formattable>>> precClauses2 = action.getPopPrecs().stream()
                    .map(actionPrec ->
                            ImmutableList.of(
                                    FormattableValue.of(Action.of(action, stage, CONDITIONS_NOT_MET), true),
                                    FormattableValue.of(Variable.of(actionPrec, stage), true)
                            ));
            // (eff1=LOCKED_FOR_UPDATE) v (eff2=LOCKED_FOR_UPDATE) -> CONDITIONS_NOT_MET => (not eff1=LOCKED_FOR_UPDATE v CONDITIONS_NOT_MET) ^  (not eff2=LOCKED_FOR_UPDATE v CONDITIONS_NOT_MET)
            Stream<ImmutableList<FormattableValue<Formattable>>> precClauses3 = action.getPopEffs().stream()
                    .flatMap(actionPrec ->
                            Stream.of(
                                    ImmutableList.of(
                                            FormattableValue.of(Action.of(action, stage, CONDITIONS_NOT_MET), true),
                                            FormattableValue.of(Variable.of(actionPrec, LOCKED_FOR_UPDATE, stage), false)
                                    ),
                                    ImmutableList.of(
                                            FormattableValue.of(Action.of(action, stage, CONDITIONS_NOT_MET), true),
                                            FormattableValue.of(Variable.of(actionPrec, FREEZED, stage), false)
                                    )

                            ));


            Set<String> actionEffKeys = action.getPopEffs().stream()
                    .map(eff -> Variable.of(eff).formatFunctionKey())
                    .collect(Collectors.toSet());


            Stream<ImmutableList<FormattableValue<Formattable>>> effectClauses =
                    Stream.concat(
                            action.getPopPrecs().stream()
                                    .map(Variable::of)
                                    .filter(v -> !actionEffKeys.contains(v.formatFunctionKey())),
                            action.getPopEffs().stream().map(Variable::of)).
                            flatMap(v ->
                                    calcVariableState(variablesStateAfterStepExec.stream(), stage + 1)
                                            .filter(var -> var.getFormattable().formatFunctionKey().equals(
                                                    v.formatFunctionKey()))
                                            .flatMap(stateVar -> {
                                                if (stateVar.getFormattable().getValue().equals(LOCKED_FOR_UPDATE)) {
                                                    return Stream.of(
                                                            ImmutableList.of(
                                                                    FormattableValue.of(
                                                                            Action.of(action, stage, CONDITIONS_NOT_MET), false),
                                                                    FormattableValue.of(
                                                                            stateVar.getFormattable().toBuilder().stage(stage + 1).build(), false)));
                                                } else if (stateVar.getFormattable().getValue().equals(FREEZED)) {
                                                    return Stream.of(
                                                            ImmutableList.of(
                                                                    FormattableValue.of(
                                                                            Action.of(action, stage, CONDITIONS_NOT_MET), false),
                                                                    FormattableValue.of(
                                                                            stateVar.getFormattable().toBuilder().stage(stage + 1).build(), false)));
                                                } else {
                                                    return Stream.of(
                                                            ImmutableList.of(
                                                                    FormattableValue.of(
                                                                            Action.of(action, stage, CONDITIONS_NOT_MET), false),
                                                                    FormattableValue.of(
                                                                            stateVar.getFormattable().toBuilder().functionValue(LOCKED_FOR_UPDATE).stage(stage).build(), true),
                                                                    FormattableValue.of(
                                                                            stateVar.getFormattable().toBuilder().stage(stage).build(), true),
                                                                    FormattableValue.of(
                                                                            stateVar.getFormattable().toBuilder().stage(stage + 1).build(), false)),
                                                            ImmutableList.of(
                                                                    FormattableValue.of(
                                                                            Action.of(action, stage, CONDITIONS_NOT_MET), false),
                                                                    FormattableValue.of(
                                                                            stateVar.getFormattable().toBuilder().functionValue(LOCKED_FOR_UPDATE).stage(stage).build(), true),
                                                                    FormattableValue.of(
                                                                            stateVar.getFormattable().toBuilder().stage(stage).build(), false),
                                                                    FormattableValue.of(
                                                                            stateVar.getFormattable().toBuilder().stage(stage + 1).build(), true))
                                                    );
                                                }
                                            }));

            List<ImmutableList<FormattableValue<Formattable>>> result = Stream.concat(
                    StreamEx.of(precClauses1).append(precClauses2).append(precClauses3), effectClauses).collect(Collectors.toList());
            return result.stream();

        });


        ImmutableList<ImmutableList<FormattableValue<Formattable>>> resultClauses =
                resultClausesStream.collect(ImmutableList.toImmutableList());

        log.debug("\n{}", resultClauses.stream().

                map(t -> StringUtils.join(t, ",")).

                collect(Collectors.joining("\n")));
        log.debug("End conditions not met clause");

        return resultClauses.stream();

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
                                                    calculateHealthyClauses(entry.getKey(), entry.getValue())),
                                            calculateActionFailedClauses(entry.getKey(), entry.getValue())),
                                    calculateConditionsNotMetClauses(entry.getKey(), entry.getValue())
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
                variablesStateAfterStepExec = this.successModel.apply(
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

                    Variable lockedVariable = variable.toBuilder().functionValue(LOCKED_FOR_UPDATE).build();
                    //this is new variable key
                    if (!beforeKeyWithValue.contains(lockedVariable.formatFunctionKeyWithValue())) {
                        newFluents.add(FormattableValue.of(lockedVariable, false));
                    }
                });


        log.debug("Adding effect to the var state {}\n", newFluents.stream().map(Objects::toString).collect(Collectors.joining("\n")));

        return newFluents.stream().map(ImmutableList::of);
    }


}
