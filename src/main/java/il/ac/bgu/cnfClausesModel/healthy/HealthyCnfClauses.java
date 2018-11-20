package il.ac.bgu.cnfClausesModel.healthy;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import il.ac.bgu.cnfClausesModel.CnfClausesFunction;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.variableModel.SuccessVariableModel;
import il.ac.bgu.variableModel.VariableModelFunction;
import lombok.extern.slf4j.Slf4j;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.CnfCompilationUtils.calcVariableState;
import static il.ac.bgu.VariableFunctions.variableKeyFilter;
import static il.ac.bgu.dataModel.Action.State.HEALTHY;
import static il.ac.bgu.dataModel.Variable.SpecialState.FREEZED;
import static il.ac.bgu.dataModel.Variable.SpecialState.LOCKED_FOR_UPDATE;

@Slf4j
public class HealthyCnfClauses implements CnfClausesFunction {

    private SuccessVariableModel successVariableModel = new SuccessVariableModel();

    @Override
    public Stream<ImmutableList<FormattableValue<Formattable>>> apply(Integer currentStage, Map<Integer, Set<Step>> plan,
                                                                      ImmutableCollection<FormattableValue<Variable>> variablesState) {
        log.debug("Start add healthy clause");

        Set<Step> actions = plan.get(currentStage);

        Collection<ImmutableList<FormattableValue<Formattable>>> resultClauses = actions.stream().flatMap(action -> {

            ImmutableList<FormattableValue<Formattable>> preconditionList =
                    Stream.concat(
                            action.getPopPrecs().stream().
                                    map(actionPrec -> FormattableValue.<Formattable>of(Variable.of(actionPrec, currentStage), false)),
                            action.getPopEffs().stream().
                                    flatMap(actionEff ->
                                            Stream.of(
                                                    FormattableValue.<Formattable>of(Variable.of(actionEff, FREEZED.name(), currentStage), true),
                                                    FormattableValue.<Formattable>of(Variable.of(actionEff, LOCKED_FOR_UPDATE.name(), currentStage), true)
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
                        return calcVariableState(variablesState.stream(), currentStage + 1)
                                .filter(variableKeyPredicate)
                                .map(formattableValue ->
                                        FormattableValue.of(
                                                formattableValue.getFormattable().toBuilder().stage(currentStage + 1).build(),
                                                formattableValue.getValue()));
                    });


            return effectStream.map(u ->
                    Stream.concat(
                            preconditionList.stream(),
                            Stream.of(FormattableValue.<Formattable>of(Action.of(action, currentStage, HEALTHY), false), u)).
                            collect(ImmutableList.toImmutableList())
            );
        }).collect(ImmutableList.toImmutableList());

        log.debug("healthy clauses\n{}", resultClauses.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        log.debug("End add healthy clause");

        return resultClauses.stream();
    }

    @Override
    public VariableModelFunction getVariableModel() {
        return successVariableModel;
    }
}
