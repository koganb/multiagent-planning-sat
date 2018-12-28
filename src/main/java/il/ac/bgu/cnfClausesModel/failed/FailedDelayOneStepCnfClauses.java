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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Action.State.FAILED;
import static il.ac.bgu.dataModel.Variable.SpecialState.FREEZED;
import static il.ac.bgu.dataModel.Variable.SpecialState.LOCKED_FOR_UPDATE;

@Slf4j
public class FailedDelayOneStepCnfClauses implements CnfClausesFunction, NamedModel {

    private static final int STAGE_DELAYED_NUM = 1;

    @Override
    public String getName() {
        return "delay one step";
    }


    @Override
    public Stream<List<FormattableValue<? extends Formattable>>> apply(Integer currentStage, Step step, Map<String,
            List<Variable>> variableStateMap) {
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


        Stream<List<FormattableValue<? extends Formattable>>> effectStream =
                Stream.concat(step.getPopPrecs().stream().map(Variable::of)
                                .filter(variable ->
                                        !actionEffKeys.contains(variable.formatFunctionKey())),
                        step.getPopEffs().stream().map(Variable::of))
                        .flatMap(variable ->
                                StreamEx.<List<FormattableValue<? extends Formattable>>>of()
                                        .append(IntStream.rangeClosed(currentStage + 1, currentStage + STAGE_DELAYED_NUM).boxed()
                                                .flatMap(stepAddition ->
                                                        actionEffKeys.contains(variable.formatFunctionKey()) ?
                                                                CnfClausesUtils.switchTrueExclusive(
                                                                        variable.toBuilder().functionValue(LOCKED_FOR_UPDATE.name()).build(),
                                                                        variableStateMap, stepAddition) :
                                                                CnfClausesUtils.addTrue(
                                                                        variable.toBuilder().functionValue(FREEZED.name()).build(),
                                                                        variableStateMap, currentStage, stepAddition)
                                                ))
                                        .append(actionEffKeys.contains(variable.formatFunctionKey()) ?
                                                CnfClausesUtils.switchTrueExclusive(variable, variableStateMap, currentStage + STAGE_DELAYED_NUM + 1) :
                                                CnfClausesUtils.applyPassThrough(variable, variableStateMap, currentStage, currentStage + STAGE_DELAYED_NUM + 1)));


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
