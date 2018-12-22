package il.ac.bgu.cnfClausesModel.failed;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.cnfClausesModel.CnfClausesFunction;
import il.ac.bgu.cnfClausesModel.CnfClausesUtils;
import il.ac.bgu.cnfClausesModel.NamedModel;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Action.State.FAILED;
import static il.ac.bgu.dataModel.Variable.SpecialState.FREEZED;
import static il.ac.bgu.dataModel.Variable.SpecialState.LOCKED_FOR_UPDATE;

@Slf4j
public class FailedNoEffectsCnfClauses implements CnfClausesFunction, NamedModel {

    @Override
    public String getName() {
        return "no effects model";
    }


    @Override
    public Stream<List<FormattableValue<? extends Formattable>>> apply(Integer currentStage, Step step, Map<String, List<Variable>> variableStateMap) {
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


        Stream<List<FormattableValue<? extends Formattable>>> effectStream =
                Stream.concat(step.getPopPrecs().stream(), step.getPopEffs().stream())
                        .map(Variable::of)
                        .flatMap(variable ->
                                CnfClausesUtils.applyPassThrough(variable, variableStateMap, currentStage, currentStage + 1));

        List<List<FormattableValue<? extends Formattable>>> resultClauses =
                effectStream.map(u ->
                        StreamEx.<FormattableValue<? extends Formattable>>of()
                                .append(preconditionList.stream())
                                .append(FormattableValue.<Formattable>of(Action.of(step, currentStage, FAILED), false))
                                .append(u)
                                .collect(ImmutableList.toImmutableList())
                ).collect(ImmutableList.toImmutableList());

        log.debug("failed clauses\n{}", resultClauses.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End failed clause");

        return resultClauses.stream();


    }
}
