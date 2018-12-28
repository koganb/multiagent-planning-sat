package il.ac.bgu.cnfClausesModel.healthy;

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
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.dataModel.Action.State.HEALTHY;
import static il.ac.bgu.dataModel.Variable.SpecialState.FREEZED;
import static il.ac.bgu.dataModel.Variable.SpecialState.LOCKED_FOR_UPDATE;

@SuppressWarnings("UnstableApiUsage")
@Slf4j
public class HealthyCnfClauses implements CnfClausesFunction, NamedModel {


    @Override
    public Stream<List<FormattableValue<? extends Formattable>>> apply(Integer currentStage, Step step,
                                                                       Map<String, List<Variable>> variableStateMap) {
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
        Stream<List<FormattableValue<? extends Formattable>>> effectStream =
                Stream.concat(
                        step.getPopPrecs().stream()
                                .filter(prec ->
                                        !actionEffKeys.contains(Variable.of(prec).formatFunctionKey())),
                        step.getPopEffs().stream()
                ).flatMap(actionEff ->
                        CnfClausesUtils.switchTrueExclusive(Variable.of(actionEff), variableStateMap, currentStage + 1)
                );


        List<List<FormattableValue<? extends Formattable>>> resultClauses =
                StreamEx.<List<FormattableValue<? extends Formattable>>>of()
                        .append(effectStream.map(u ->
                                StreamEx.<FormattableValue<? extends Formattable>>of()
                                        .append(preconditionList.stream())
                                        .append(FormattableValue.of(Action.of(step, currentStage, HEALTHY), false))
                                        .append(u)
                                        .collect(ImmutableList.toImmutableList())
                        ))
                        .collect(ImmutableList.toImmutableList());

        log.debug("healthy clauses\n{}", resultClauses.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End add healthy clause");

        return resultClauses.stream();
    }

    @Override
    public String getName() {
        throw new NotImplementedException("");
    }


}
