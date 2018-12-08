package il.ac.bgu.cnfClausesModel.healthy;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import il.ac.bgu.cnfClausesModel.CnfClausesFunction;
import il.ac.bgu.cnfClausesModel.NamedModel;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.variableModel.SuccessVariableModel;
import il.ac.bgu.variableModel.VariableModelFunction;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Action.State.HEALTHY;
import static il.ac.bgu.dataModel.Variable.SpecialState.FREEZED;
import static il.ac.bgu.dataModel.Variable.SpecialState.LOCKED_FOR_UPDATE;
import static il.ac.bgu.utils.CnfCompilationUtils.calcVariableState;
import static il.ac.bgu.utils.VariableFunctions.variableKeyFilter;

@SuppressWarnings("UnstableApiUsage")
@Slf4j
public class HealthyCnfClauses implements CnfClausesFunction, NamedModel {

    private SuccessVariableModel successVariableModel = new SuccessVariableModel();

    @Override
    public Stream<ImmutableList<FormattableValue<? extends Formattable>>> apply(Integer currentStage, Step step,
                                                                                ImmutableCollection<FormattableValue<Variable>> variablesState) {
        log.debug("Start add healthy clause");
        ImmutableList<FormattableValue<? extends Formattable>> preconditionList =
                Stream.concat(
                        step.getPopPrecs().stream().
                                map(actionPrec -> FormattableValue.<Formattable>of(Variable.of(actionPrec, currentStage), false)),
                        step.getPopEffs().stream().
                                flatMap(actionEff ->
                                        StreamEx.<FormattableValue<Formattable>>of()
                                                .append(FormattableValue.of(Variable.of(actionEff, FREEZED.name(), currentStage), true))
                                                .append(FormattableValue.of(Variable.of(actionEff, LOCKED_FOR_UPDATE.name(), currentStage), true))
                                )
                ).collect(ImmutableList.toImmutableList());


        Set<String> actionEffKeys = step.getPopEffs().stream()
                .map(eff -> Variable.of(eff).formatFunctionKey())
                .collect(Collectors.toSet());

        //healthy function
        Stream<FormattableValue<Formattable>> effectStream =
                Stream.concat(
                        step.getPopPrecs().stream()
                                .filter(prec ->
                                        !actionEffKeys.contains(Variable.of(prec).formatFunctionKey())),
                        step.getPopEffs().stream()
                ).flatMap(actionEff -> {
                    Predicate<FormattableValue<Variable>> variableKeyPredicate = variableKeyFilter.apply(Variable.of(actionEff));

                    //prec or effect variable
                    return calcVariableState(variablesState.stream(), currentStage + 1)
                            .filter(variableKeyPredicate)
                            .map(formattableValue ->
                                    FormattableValue.of(
                                            formattableValue.getFormattable().toBuilder().stage(currentStage + 1).build(),
                                            formattableValue.getValue()));
                });


        ImmutableList<ImmutableList<FormattableValue<? extends Formattable>>> resultClauses =
                StreamEx.<ImmutableList<FormattableValue<? extends Formattable>>>of()
                        .append(effectStream.map(u ->
                                StreamEx.<FormattableValue<? extends Formattable>>of()
                                        .append(preconditionList.stream())
                                        .append(Stream.of(FormattableValue.of(Action.of(step, currentStage, HEALTHY), false), u))
                                        .collect(ImmutableList.toImmutableList())
                        ))
                        .collect(ImmutableList.toImmutableList());

        log.debug("healthy clauses\n{}", resultClauses.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End add healthy clause");

        return resultClauses.stream();
    }

    @Override
    public VariableModelFunction getVariableModel() {
        return successVariableModel;
    }

    @Override
    public String getName() {
        throw new NotImplementedException("");
    }
}
