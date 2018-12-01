package il.ac.bgu.cnfClausesModel.failed;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import il.ac.bgu.cnfClausesModel.CnfClausesFunction;
import il.ac.bgu.cnfClausesModel.NamedModel;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.variableModel.VariableModelFunction;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static il.ac.bgu.CnfCompilationUtils.calcVariableState;
import static il.ac.bgu.VariableFunctions.variableKeyFilter;
import static il.ac.bgu.dataModel.Action.State.FAILED;
import static il.ac.bgu.dataModel.Variable.SpecialState.FREEZED;
import static il.ac.bgu.dataModel.Variable.SpecialState.LOCKED_FOR_UPDATE;
import static il.ac.bgu.variableModel.VariableModelFunction.NEXT_STEP_ADDITION;
import static il.ac.bgu.variableModel.VariableModelFunction.VARIABLE_TYPE.EFFECT;
import static il.ac.bgu.variableModel.VariableModelFunction.VARIABLE_TYPE.PRECONDITION;
import static java.util.stream.Collectors.toList;

@Slf4j
public abstract class FailureCnfClauses implements CnfClausesFunction, NamedModel {

    protected VariableModelFunction failureModel;

    @Override
    public Stream<ImmutableList<FormattableValue<Formattable>>> apply(Integer currentStage,
                                                                      Step step,
                                                                      ImmutableCollection<FormattableValue<Variable>> variablesState) {


        log.debug("Start failed clause");

        ImmutableList<FormattableValue<Formattable>> preconditionList =
                Stream.concat(
                        step.getPopPrecs().stream()
                                .map(actionPrec -> FormattableValue.<Formattable>of(
                                        Variable.of(actionPrec, currentStage), false)),
                        step.getPopEffs().stream()
                                .flatMap(actionEff ->
                                        StreamEx.<FormattableValue<Formattable>>of()
                                                .append(FormattableValue.of(
                                                        Variable.of(actionEff, LOCKED_FOR_UPDATE.name(), currentStage), true))
                                                .append(FormattableValue.of(
                                                        Variable.of(actionEff, FREEZED.name(), currentStage), true))

                                )
                ).collect(ImmutableList.toImmutableList());


        Set<String> actionEffKeys = step.getPopEffs().stream()
                .map(eff -> Variable.of(eff).formatFunctionKey())
                .collect(Collectors.toSet());


        Stream<FormattableValue<Formattable>> effectStream =
                Stream.concat(
                        step.getPopPrecs().stream()
                                .map(prec -> ImmutablePair.of(Variable.of(prec), PRECONDITION))
                                .filter(pair -> !actionEffKeys.contains(pair.getLeft().formatFunctionKey()))
                        ,
                        step.getPopEffs().stream()
                                .map(eff -> ImmutablePair.of(Variable.of(eff), EFFECT))
                ).
                        flatMap(precEffPair -> {
                            Variable variable = precEffPair.getLeft();
                            Predicate<FormattableValue<Variable>> variableKeyPredicate = variableKeyFilter.apply(variable);

                            ImmutableList<FormattableValue<Variable>> failureModelVariables =
                                    failureModel.apply(variable, currentStage, variablesState, precEffPair.getRight()).
                                            collect(ImmutableList.toImmutableList());
                            return IntStream.rangeClosed(currentStage + NEXT_STEP_ADDITION, currentStage + failureModel.affectedStepsNumber())
                                    .boxed()
                                    .flatMap(stepAddition ->
                                            calcVariableState(failureModelVariables.stream(), stepAddition)
                                                    .filter(variableKeyPredicate)
                                                    .map(var -> FormattableValue.of(var.getFormattable().toBuilder().stage(stepAddition).build(),
                                                            var.getValue())));
                        });

        List<FormattableValue<Formattable>> effects = effectStream.collect(toList());

        ImmutableList<ImmutableList<FormattableValue<Formattable>>> resultClauses =
                StreamEx.<ImmutableList<FormattableValue<Formattable>>>of()
                        .append(
                                effects.stream().map(u ->
                                        Stream.concat(
                                                preconditionList.stream(),
                                                Stream.of(FormattableValue.<Formattable>of(Action.of(step, currentStage, FAILED), false), u)).
                                                collect(ImmutableList.toImmutableList())
                                ))
                        .collect(ImmutableList.toImmutableList());

        log.debug("failed clauses\n{}", resultClauses.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End failed clause");

        return resultClauses.stream();

    }
}
