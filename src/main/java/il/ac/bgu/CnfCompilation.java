package il.ac.bgu;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.failureModel.NoEffectFailureModel;
import il.ac.bgu.failureModel.VariableModelFunction;
import lombok.extern.slf4j.Slf4j;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_planner.POPPrecEff;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Action.State.*;
import static il.ac.bgu.dataModel.Variable.UNDEFINED;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Created by Boris on 01/07/2017.
 */

@Slf4j
public class CnfCompilation {

    private List<FormattableValue<Variable>> variablesStateBeforeStepExec;
    private List<FormattableValue<Variable>> variablesStateAfterStepExec;


    private TreeMap<Integer, Set<Step>> plan;

    private VariableModelFunction failureModel;


    CnfCompilation(TreeMap<Integer, Set<Step>> plan, VariableModelFunction failureModel) {
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
                        FormattableValue.of(new Variable(eff, 0), true),
                        FormattableValue.of(new Variable(eff, UNDEFINED, 0), false)
                )).
                collect(Collectors.toList());
    }


    public ImmutableList<FormattableValue<Formattable>> encodeHealthyClauses() {

        ImmutableList<FormattableValue<Formattable>> healthyClauses =

                plan.entrySet().stream().
                        filter(i -> i.getKey() != -1).
                        flatMap(entry -> entry.getValue().stream().flatMap(
                                step -> Stream.of(
                                        FormattableValue.<Formattable>of(new Action(step, entry.getKey(), HEALTHY), true)
                                ))).
                        collect(ImmutableList.toImmutableList());

        log.trace("healthy clauses {}", healthyClauses);
        return healthyClauses;
    }


    public ImmutableList<FormattableValue<Formattable>> calcFinalFacts(Set<Action> failedActions) {

        log.debug("Start final values calculation");

        ImmutableList<FormattableValue<Formattable>> finalValues =
                new FinalVariableStateCalc(plan, new NoEffectFailureModel()).getFinalVariableState(failedActions);
        log.debug("Final Values: \n{}", finalValues.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End final values calculation");


        return finalValues;
    }

    Stream<ImmutableList<FormattableValue<Formattable>>> calculatePassThroughClauses(Integer stage, Set<Step> actions) {
        //calculate "pass through" variables

        //get effects keys for stage actions
        Set<String> effectKeys = actions.stream().flatMap(k ->
                k.getPopEffs().stream()).map(CnfEncodingUtils::createEffKey).
                collect(toSet());

        log.debug("Start pass through...");

        Stream<ImmutableList<FormattableValue<Formattable>>> passThroughValuesStream =
                variablesStateBeforeStepExec.stream().
                        filter(value -> !effectKeys.contains(value.getFormattable().formatFunctionKey())).
                        flatMap(g -> Stream.of(
                                ImmutableList.of(
                                        FormattableValue.of((g.getFormattable()).toBuilder().stage(stage).build(), false),
                                        FormattableValue.of((g.getFormattable()).toBuilder().stage(stage + 1).build(), true)),
                                ImmutableList.of(
                                        FormattableValue.of((g.getFormattable()).toBuilder().stage(stage).build(), true),
                                        FormattableValue.of((g.getFormattable()).toBuilder().stage(stage + 1).build(), false))

                        ));
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
                    action.getPopPrecs().stream().
                            map(actionPrec -> FormattableValue.<Formattable>of(new Variable(actionPrec, stage), false)).
                            collect(ImmutableList.toImmutableList());

            //healthy function
            Stream<FormattableValue<Formattable>> effectStream = action.getPopEffs().stream().flatMap(actionEff -> {

                //effect variable
                return variablesStateAfterStepExec.stream()
                        .filter(var -> var.getFormattable().formatFunctionKey().equals(
                                Variable.of(actionEff).formatFunctionKey()))
                        .map(formattableValue ->
                                FormattableValue.of(
                                        formattableValue.getFormattable().toBuilder().stage(stage + 1).build(),
                                        formattableValue.getValue()));
            });


            return effectStream.map(u ->
                    Stream.concat(
                            preconditionList.stream(),
                            Stream.of(FormattableValue.<Formattable>of(new Action(action, stage, HEALTHY), false), u)).
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
                    action.getPopPrecs().stream().
                            map(actionPrec -> FormattableValue.<Formattable>of(new Variable(actionPrec, stage), false)).
                            collect(ImmutableList.toImmutableList());

            Stream<FormattableValue<Formattable>> effectStream = action.getPopEffs().stream().flatMap(actionEff -> {
                Variable variable = new Variable(actionEff, stage);
                List<FormattableValue<Formattable>> failureModelResult =
                        failureModel.apply(variable, variablesStateBeforeStepExec)
                                .filter(var -> var.getFormattable().formatFunctionKey().equals(variable.formatFunctionKey()))
                                .map(var -> FormattableValue.<Formattable>of(var.getFormattable().toBuilder().stage(stage + 1).build(), var.getValue()))
                                .collect(toList());
                return failureModelResult.stream();
            });

            List<FormattableValue<Formattable>> effects = effectStream.collect(toList());


            return effects.stream().map(u ->
                    Stream.concat(
                            preconditionList.stream(),
                            Stream.of(FormattableValue.<Formattable>of(new Action(action, stage, FAILED), false), u)).
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

            // unknown -> not(prec1) v not(prec2) => not(unknown) v not(prec1) v not(prec2)
            Stream<ImmutableList<FormattableValue<Formattable>>> precClauses1 = Stream.of(Stream.concat(
                    Stream.of(FormattableValue.<Formattable>of(new Action(action, stage, UNKNOWN), false)),
                    action.getPopPrecs().stream().map(actionPrec ->
                            FormattableValue.<Formattable>of(Variable.of(actionPrec, stage), false)
                    )
            )
                    .collect(ImmutableList.toImmutableList()));


            // not(prec1) v not(prec2) -> unknown => (prec1 v unknown) ^  (prec2 v unknown)
            Stream<ImmutableList<FormattableValue<Formattable>>> precClauses2 = action.getPopPrecs().stream()
                    .map(actionPrec ->
                            ImmutableList.of(
                                    FormattableValue.of(new Action(action, stage, UNKNOWN), true),
                                    FormattableValue.of(Variable.of(actionPrec, stage), true)
                            ));

            Stream<ImmutableList<FormattableValue<Formattable>>> effectClauses = action.getPopEffs().stream().
                    flatMap(actionEff ->
                            variablesStateAfterStepExec.stream()
                                    .filter(var -> var.getFormattable().formatFunctionKey().equals(
                                            Variable.of(actionEff).formatFunctionKey()))
                                    .flatMap(stateVar -> Stream.of(
                                            ImmutableList.of(
                                                    FormattableValue.of(
                                                            new Action(action, stage, UNKNOWN), false),
                                                    FormattableValue.of(
                                                            stateVar.getFormattable().toBuilder().stage(stage).build(), true),
                                                    FormattableValue.of(
                                                            stateVar.getFormattable().toBuilder().stage(stage + 1).build(), false)),
                                            ImmutableList.of(
                                                    FormattableValue.of(
                                                            new Action(action, stage, UNKNOWN), false),
                                                    FormattableValue.of(
                                                            stateVar.getFormattable().toBuilder().stage(stage).build(), false),
                                                    FormattableValue.of(
                                                            stateVar.getFormattable().toBuilder().stage(stage + 1).build(), true))
                                    )));

            List<ImmutableList<FormattableValue<Formattable>>> result = Stream.concat(
                    Stream.concat(precClauses1, precClauses2), effectClauses).collect(Collectors.toList());
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
                                FormattableValue.of(new Action(action, stage, HEALTHY), true),
                                FormattableValue.of(new Action(action, stage, FAILED), true),
                                FormattableValue.of(new Action(action, stage, UNKNOWN), true)
                        ),
                        ImmutableList.of(
                                FormattableValue.of(new Action(action, stage, HEALTHY), true),
                                FormattableValue.of(new Action(action, stage, FAILED), false),
                                FormattableValue.of(new Action(action, stage, UNKNOWN), false)
                        ),
                        ImmutableList.of(
                                FormattableValue.of(new Action(action, stage, HEALTHY), false),
                                FormattableValue.of(new Action(action, stage, FAILED), true),
                                FormattableValue.of(new Action(action, stage, UNKNOWN), false)
                        ),
                        ImmutableList.of(
                                FormattableValue.of(new Action(action, stage, HEALTHY), false),
                                FormattableValue.of(new Action(action, stage, FAILED), false),
                                FormattableValue.of(new Action(action, stage, UNKNOWN), true)
                        ),
                        ImmutableList.of(
                                FormattableValue.of(new Action(action, stage, HEALTHY), false),
                                FormattableValue.of(new Action(action, stage, FAILED), false),
                                FormattableValue.of(new Action(action, stage, UNKNOWN), false)
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

        //add all preconditions to variable state
        actions.forEach(action -> action.getPopPrecs().forEach(eff -> {
            FormattableValue<Variable> precondition = FormattableValue.of(new Variable(eff, stage), true);

            if (variablesStateBeforeStepExec.stream().noneMatch(var ->
                    var.getFormattable().formatFunctionKeyWithValue().equals(
                            precondition.getFormattable().formatFunctionKeyWithValue()))) {
                log.debug("Adding precondition to the var state {}", precondition);

                variablesStateBeforeStepExec.add(precondition);
                newFluents.add(FormattableValue.of(new Variable(eff, stage), false));
            }
        }));

        //update state according to step effects
        variablesStateAfterStepExec = ImmutableList.copyOf(variablesStateBeforeStepExec);
        for (Step action : actions) {
            for (POPPrecEff eff : action.getPopEffs()) {
                variablesStateAfterStepExec = CnfCompilationUtils.updateVariables(
                        variablesStateAfterStepExec, FormattableValue.of(Variable.of(eff, stage), true), stage)
                        .collect(toList());

            }
        }

        //find the difference between before and after
        Set<String> beforeKeyWithValue = variablesStateBeforeStepExec.stream()
                .map(var -> var.getFormattable().formatFunctionKeyWithValue())
                .collect(toSet());
        variablesStateAfterStepExec.stream()
                .filter(var -> !beforeKeyWithValue.contains(
                        var.getFormattable().formatFunctionKeyWithValue()))
                .forEach(var ->
                        newFluents.add(FormattableValue.of(var.getFormattable(), false)));


        log.debug("Adding effect to the var state {}\n", newFluents.stream().map(Objects::toString).collect(Collectors.joining("\n")));

        return newFluents.stream().map(ImmutableList::of);
    }


}
