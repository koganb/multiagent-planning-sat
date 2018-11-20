package il.ac.bgu.cnfClausesModel.conflict;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import il.ac.bgu.cnfClausesModel.CnfClausesFunction;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.CnfCompilationUtils.calcVariableState;
import static il.ac.bgu.dataModel.Action.State.CONDITIONS_NOT_MET;
import static il.ac.bgu.dataModel.Variable.SpecialState.FREEZED;
import static il.ac.bgu.dataModel.Variable.SpecialState.LOCKED_FOR_UPDATE;

@Slf4j
public class ConflictNoEffectsCnfClauses implements CnfClausesFunction {


    @Override
    public Stream<ImmutableList<FormattableValue<Formattable>>> apply(Integer currentStage,
                                                                      Map<Integer, Set<Step>> plan,
                                                                      ImmutableCollection<FormattableValue<Variable>> variablesState) {

        log.debug("Start conditions not met clause");

        Set<Step> actions = plan.get(currentStage);

        Stream<ImmutableList<FormattableValue<Formattable>>> resultClausesStream = actions.stream().flatMap(action -> {

            // CONDITIONS_NOT_MET -> not(prec1) v not(prec2) => not(unknown) v not(prec1) v not(prec2)
            Stream<ImmutableList<FormattableValue<Formattable>>> precClauses1 = Stream.of(
                    StreamEx.<FormattableValue<Formattable>>of()
                            .append(Stream.of(FormattableValue.of(Action.of(action, currentStage, CONDITIONS_NOT_MET), false)))
                            .append(action.getPopPrecs().stream().map(actionPrec ->
                                    FormattableValue.of(Variable.of(actionPrec, currentStage), false)))
                            .append(action.getPopEffs().stream().map(actionEff ->
                                    FormattableValue.of(Variable.of(actionEff, LOCKED_FOR_UPDATE.name(), currentStage), true)))
                            .append(action.getPopEffs().stream().map(actionEff ->
                                    FormattableValue.of(Variable.of(actionEff, FREEZED.name(), currentStage), true)))
                            .collect(ImmutableList.toImmutableList()));


            // not(prec1) v not(prec2) -> CONDITIONS_NOT_MET => (prec1 v CONDITIONS_NOT_MET) ^  (prec2 v CONDITIONS_NOT_MET)
            Stream<ImmutableList<FormattableValue<Formattable>>> precClauses2 = action.getPopPrecs().stream()
                    .map(actionPrec ->
                            ImmutableList.of(
                                    FormattableValue.of(Action.of(action, currentStage, CONDITIONS_NOT_MET), true),
                                    FormattableValue.of(Variable.of(actionPrec, currentStage), true)
                            ));
            // (eff1=LOCKED_FOR_UPDATE) v (eff2=LOCKED_FOR_UPDATE) -> CONDITIONS_NOT_MET => (not eff1=LOCKED_FOR_UPDATE v CONDITIONS_NOT_MET) ^  (not eff2=LOCKED_FOR_UPDATE v CONDITIONS_NOT_MET)
            Stream<ImmutableList<FormattableValue<Formattable>>> precClauses3 = action.getPopEffs().stream()
                    .flatMap(actionPrec ->
                            Stream.of(
                                    ImmutableList.of(
                                            FormattableValue.of(Action.of(action, currentStage, CONDITIONS_NOT_MET), true),
                                            FormattableValue.of(Variable.of(actionPrec, LOCKED_FOR_UPDATE.name(), currentStage), false)
                                    ),
                                    ImmutableList.of(
                                            FormattableValue.of(Action.of(action, currentStage, CONDITIONS_NOT_MET), true),
                                            FormattableValue.of(Variable.of(actionPrec, FREEZED.name(), currentStage), false)
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
                                    calcVariableState(variablesState.stream(), currentStage + 1)
                                            .filter(var -> var.getFormattable().formatFunctionKey().equals(
                                                    v.formatFunctionKey()))
                                            .flatMap(stateVar -> {
                                                if (Objects.equals(stateVar.getFormattable().getValue(), LOCKED_FOR_UPDATE.name())) {
                                                    return Stream.of(
                                                            ImmutableList.of(
                                                                    FormattableValue.of(
                                                                            Action.of(action, currentStage, CONDITIONS_NOT_MET), false),
                                                                    FormattableValue.of(
                                                                            stateVar.getFormattable().toBuilder().stage(currentStage + 1).build(), false)));
                                                } else if (Objects.equals(stateVar.getFormattable().getValue(), FREEZED.name())) {
                                                    return Stream.of(
                                                            ImmutableList.of(
                                                                    FormattableValue.of(
                                                                            Action.of(action, currentStage, CONDITIONS_NOT_MET), false),
                                                                    FormattableValue.of(
                                                                            stateVar.getFormattable().toBuilder().stage(currentStage + 1).build(), false)));
                                                } else {
                                                    return Stream.of(
                                                            ImmutableList.of(
                                                                    FormattableValue.of(
                                                                            Action.of(action, currentStage, CONDITIONS_NOT_MET), false),
                                                                    FormattableValue.of(
                                                                            stateVar.getFormattable().toBuilder().functionValue(LOCKED_FOR_UPDATE.name()).stage(currentStage).build(), true),
                                                                    FormattableValue.of(
                                                                            stateVar.getFormattable().toBuilder().stage(currentStage).build(), true),
                                                                    FormattableValue.of(
                                                                            stateVar.getFormattable().toBuilder().stage(currentStage + 1).build(), false)),
                                                            ImmutableList.of(
                                                                    FormattableValue.of(
                                                                            Action.of(action, currentStage, CONDITIONS_NOT_MET), false),
                                                                    FormattableValue.of(
                                                                            stateVar.getFormattable().toBuilder().functionValue(LOCKED_FOR_UPDATE.name()).stage(currentStage).build(), true),
                                                                    FormattableValue.of(
                                                                            stateVar.getFormattable().toBuilder().stage(currentStage).build(), false),
                                                                    FormattableValue.of(
                                                                            stateVar.getFormattable().toBuilder().stage(currentStage + 1).build(), true))
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

    @Override
    public VariableModelFunction getVariableModel() {
        throw new NotImplementedException("TBD");
    }
}
