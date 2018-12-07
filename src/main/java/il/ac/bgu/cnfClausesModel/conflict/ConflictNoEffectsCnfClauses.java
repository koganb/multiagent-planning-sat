package il.ac.bgu.cnfClausesModel.conflict;

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
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Action.State.CONDITIONS_NOT_MET;
import static il.ac.bgu.dataModel.Variable.SpecialState.FREEZED;
import static il.ac.bgu.dataModel.Variable.SpecialState.LOCKED_FOR_UPDATE;
import static il.ac.bgu.utils.CnfCompilationUtils.calcVariableState;

@SuppressWarnings("UnstableApiUsage")
@Slf4j
public class ConflictNoEffectsCnfClauses implements CnfClausesFunction, NamedModel {


    @Override
    public Stream<ImmutableList<FormattableValue<? extends Formattable>>> apply(Integer currentStage,
                                                                                Step step,
                                                                                ImmutableCollection<FormattableValue<Variable>> variablesState) {

        log.debug("Start conditions not met clause");

        // CONDITIONS_NOT_MET -> not(prec1) v not(prec2) => not(CONDITIONS_NOT_MET) v not(prec1) v not(prec2)
        Stream<ImmutableList<FormattableValue<? extends Formattable>>> precClauses1 = Stream.of(


                StreamEx.<FormattableValue<Formattable>>of()
                        .append(Stream.of(FormattableValue.of(Action.of(step, currentStage, CONDITIONS_NOT_MET), false)))
                        .append(step.getPopPrecs().stream().map(actionPrec ->
                                FormattableValue.of(Variable.of(actionPrec, currentStage), false)))
                        .append(step.getPopEffs().stream().map(actionEff ->
                                FormattableValue.of(Variable.of(actionEff, LOCKED_FOR_UPDATE.name(), currentStage), true)))
                        .append(step.getPopEffs().stream().map(actionEff ->
                                FormattableValue.of(Variable.of(actionEff, FREEZED.name(), currentStage), true)))
                        .collect(ImmutableList.toImmutableList()));


        // not(prec1) v not(prec2) -> CONDITIONS_NOT_MET => (prec1 v CONDITIONS_NOT_MET) ^  (prec2 v CONDITIONS_NOT_MET)
        Stream<ImmutableList<FormattableValue<? extends Formattable>>> precClauses2 = step.getPopPrecs().stream()
                .map(actionPrec ->
                        ImmutableList.<FormattableValue<? extends Formattable>>builder()
                                .add(FormattableValue.of(Action.of(step, currentStage, CONDITIONS_NOT_MET), true))
                                .add(FormattableValue.of(Variable.of(actionPrec, currentStage), true))
                                .build());
        // (eff1=LOCKED_FOR_UPDATE) v (eff2=LOCKED_FOR_UPDATE) -> CONDITIONS_NOT_MET => (not eff1=LOCKED_FOR_UPDATE v CONDITIONS_NOT_MET) ^  (not eff2=LOCKED_FOR_UPDATE v CONDITIONS_NOT_MET)
        Stream<ImmutableList<FormattableValue<? extends Formattable>>> precClauses3 = step.getPopEffs().stream()
                .flatMap(actionPrec ->
                        Stream.of(
                                ImmutableList.<FormattableValue<? extends Formattable>>builder()
                                        .add(FormattableValue.of(Action.of(step, currentStage, CONDITIONS_NOT_MET), true))
                                        .add(FormattableValue.of(Variable.of(actionPrec, LOCKED_FOR_UPDATE.name(), currentStage), false))
                                        .build()
                                ,
                                ImmutableList.<FormattableValue<? extends Formattable>>builder()
                                        .add(FormattableValue.of(Action.of(step, currentStage, CONDITIONS_NOT_MET), true))
                                        .add(FormattableValue.of(Variable.of(actionPrec, FREEZED.name(), currentStage), false))
                                        .build()
                        ));


        Set<String> actionEffKeys = step.getPopEffs().stream()
                .map(eff -> Variable.of(eff).formatFunctionKey())
                .collect(Collectors.toSet());


        Stream<ImmutableList<FormattableValue<? extends Formattable>>> effectClauses =
                Stream.concat(
                        step.getPopPrecs().stream()
                                .map(Variable::of)
                                .filter(v -> !actionEffKeys.contains(v.formatFunctionKey())),
                        step.getPopEffs().stream().map(Variable::of)).
                        flatMap(v ->
                                calcVariableState(variablesState.stream(), currentStage + 1)
                                        .filter(var -> var.getFormattable().formatFunctionKey().equals(
                                                v.formatFunctionKey()))
                                        .flatMap(stateVar -> {
                                            if (Objects.equals(stateVar.getFormattable().getValue(), LOCKED_FOR_UPDATE.name())) {
                                                return Stream.of(
                                                        ImmutableList.<FormattableValue<? extends Formattable>>builder()
                                                                .add(FormattableValue.of(
                                                                        Action.of(step, currentStage, CONDITIONS_NOT_MET), false))
                                                                .add(FormattableValue.of(
                                                                        stateVar.getFormattable().toBuilder().stage(currentStage + 1).build(), false))
                                                                .build()

                                                );
                                            } else if (Objects.equals(stateVar.getFormattable().getValue(), FREEZED.name())) {
                                                return Stream.of(
                                                        ImmutableList.<FormattableValue<? extends Formattable>>builder()
                                                                .add(FormattableValue.of(
                                                                        Action.of(step, currentStage, CONDITIONS_NOT_MET), false))
                                                                .add(FormattableValue.of(
                                                                        stateVar.getFormattable().toBuilder().stage(currentStage + 1).build(), false))
                                                                .build());
                                            } else {
                                                return Stream.of(
                                                        ImmutableList.<FormattableValue<? extends Formattable>>builder()
                                                                .add(FormattableValue.of(
                                                                        Action.of(step, currentStage, CONDITIONS_NOT_MET), false))
                                                                .add(FormattableValue.of(
                                                                        stateVar.getFormattable().toBuilder().functionValue(LOCKED_FOR_UPDATE.name()).stage(currentStage).build(), true))
                                                                .add(FormattableValue.of(
                                                                        stateVar.getFormattable().toBuilder().stage(currentStage).build(), true))
                                                                .add(FormattableValue.of(
                                                                        stateVar.getFormattable().toBuilder().stage(currentStage + 1).build(), false))
                                                                .build()
                                                        ,
                                                        ImmutableList.<FormattableValue<? extends Formattable>>builder()
                                                                .add(FormattableValue.of(
                                                                        Action.of(step, currentStage, CONDITIONS_NOT_MET), false))
                                                                .add(FormattableValue.of(
                                                                        stateVar.getFormattable().toBuilder().functionValue(LOCKED_FOR_UPDATE.name()).stage(currentStage).build(), true))
                                                                .add(FormattableValue.of(
                                                                        stateVar.getFormattable().toBuilder().stage(currentStage).build(), false))
                                                                .add(FormattableValue.of(
                                                                        stateVar.getFormattable().toBuilder().stage(currentStage + 1).build(), true))
                                                                .build()

                                                );
                                            }
                                        }));

        List<ImmutableList<FormattableValue<? extends Formattable>>> resultClauses =
                StreamEx.<ImmutableList<FormattableValue<? extends Formattable>>>of()
                        .append(precClauses1)
                        .append(precClauses2)
                        .append(precClauses3)
                        .append(effectClauses)
                        .collect(ImmutableList.toImmutableList());


        log.debug("\n{}", resultClauses.stream().
                map(t -> StringUtils.join(t, ",")).
                collect(Collectors.joining("\n")));
        log.debug("End conditions not met clause");

        return resultClauses.stream();


    }

    @Override
    public VariableModelFunction getVariableModel() {
        throw new NotImplementedException("TBD");
    }

    @Override
    public String getName() {
        return "no effects model";
    }
}
