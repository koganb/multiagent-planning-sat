package il.ac.bgu;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.failureModel.NewFailureModelFunction;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.bgu.CnfCompilation.UNDEFINED;
import static il.ac.bgu.CnfEncodingUtils.createEffId;
import static java.util.stream.Collectors.toSet;

public class FinalVariableStateCalc {


    private TreeMap<Integer, Set<Step>> plan;
    private NewFailureModelFunction failureModelFunction;


    public FinalVariableStateCalc(TreeMap<Integer, Set<Step>> plan, NewFailureModelFunction failureModelFunction) {
        this.plan = plan;
        this.failureModelFunction = failureModelFunction;
    }

    private ImmutableMap<String, ImmutablePair<Variable, Boolean>> getVarsGroupedByFunctionKeyWithValue(
            Integer currentStage, ImmutableSet<ImmutablePair<Variable, Boolean>> variables) {
        Map<String, ImmutableSet<ImmutablePair<Variable, Boolean>>> groupedById =
                variables.stream().
                        filter(v -> v.getKey().getStage() <= currentStage).
                        collect(Collectors.groupingBy(v -> v.getKey().formatFunctionKeyWithValue(), ImmutableSet.toImmutableSet()));

        return groupedById.values().stream().map(vars ->
                vars.stream().max(Comparator.comparing(v -> v.getKey().getStage())).
                        orElseThrow(() -> new RuntimeException("No values for vars " + vars)))
                .collect(ImmutableMap.toImmutableMap(v -> v.getKey().formatFunctionKeyWithValue(), Function.identity()));
    }

    private ImmutableMap<String, ImmutableSet<ImmutablePair<Variable, Boolean>>> getVarsGroupedByFunctionKey(
            ImmutableMap<String, ImmutablePair<Variable, Boolean>> variableState) {
        return ImmutableMap.copyOf(variableState.values().stream()
                .collect(Collectors.toMap(v -> v.getKey().formatFunctionKeyWithValue(), Function.identity(), (p, q) -> p))
                .values().stream()
                .collect(Collectors.groupingBy(v -> v.getKey().formatFunctionKey(),
                        Collectors.mapping(Function.identity(), ImmutableSet.toImmutableSet()))));

    }

    public ImmutableCollection<ImmutablePair<Variable, Boolean>> getFinalVariableState(String... failedSteps) {
        Set<String> failedStepsSet = Arrays.stream(failedSteps).collect(toSet());

        final ImmutableSet.Builder<ImmutablePair<Variable, Boolean>> varsBuilder = ImmutableSet.builder();

        plan.get(-1).iterator().next().getPopEffs()
                .forEach(eff -> {
                    Variable variable = new Variable(eff, -1);
                    varsBuilder.add(ImmutablePair.of(variable, true));
                    varsBuilder.add(ImmutablePair.of(variable.toBuilder().functionValue(UNDEFINED).build(), false));
                });

        plan.entrySet().stream().filter(i -> i.getKey() != -1).forEach(stepEntry -> {
            //keyWithValue to variable

            ImmutableSet<ImmutablePair<Variable, Boolean>> vars = varsBuilder.build();
            ImmutableMap<String, ImmutablePair<Variable, Boolean>> varsGroupedByKeyValue = getVarsGroupedByFunctionKeyWithValue(stepEntry.getKey(), vars);
            ImmutableMap<String, ImmutableSet<ImmutablePair<Variable, Boolean>>> varsGroupedByKey =
                    getVarsGroupedByFunctionKey(varsGroupedByKeyValue);


            stepEntry.getValue().forEach(action -> {

                //step is failed
                if (failedStepsSet.contains(action.getUuid())) {
                    action.getPopEffs().forEach(eff -> {
                        Variable variable = new Variable(eff, stepEntry.getKey());
                        this.failureModelFunction.apply(variable, varsGroupedByKeyValue.values())
                                .forEach(varsBuilder::add);
                    });
                }
                //all preconditions valid
                else if (action.getPopPrecs().stream().
                        map(eff -> createEffId(eff, eff.getValue())).
                        allMatch(key -> Optional.ofNullable(varsGroupedByKeyValue.get(key)).
                                map(Pair::getValue).orElse(false))) {

                    action.getPopEffs().forEach(eff -> {
                        Variable trueVariable = new Variable(eff, stepEntry.getKey());
                        updateVariables(varsGroupedByKey.get(trueVariable.formatFunctionKey()),
                                trueVariable, stepEntry.getKey())
                                .forEach(varsBuilder::add);
                    });
                } else {
                    //preconditions are not valid
                    action.getPopEffs().forEach(eff -> {
                        Variable variable = new Variable(eff, stepEntry.getKey());
                        this.failureModelFunction.apply(variable, varsGroupedByKeyValue.values())
                                .forEach(varsBuilder::add);
                    });

                }
            });


        });

        Integer maxStep = plan.keySet().stream().max(Integer::compareTo)
                .orElseThrow(() -> new RuntimeException("No max step for plan" + plan));

        ImmutableSet<ImmutablePair<Variable, Boolean>> finalVars = varsBuilder.build().stream()
                .collect(ImmutableSet.toImmutableSet());

        ImmutableCollection<ImmutablePair<Variable, Boolean>> variablesState = getVarsGroupedByFunctionKeyWithValue(maxStep, finalVars).values();
        return variablesState.stream()
                .map(pair -> ImmutablePair.of(pair.getKey().toBuilder().stage(maxStep + 1).build(), pair.getRight()))
                .collect(ImmutableSet.toImmutableSet());
    }

    private Stream<ImmutablePair<Variable, Boolean>> updateVariables
            (ImmutableCollection<ImmutablePair<Variable, Boolean>> falseVariable, Variable trueVariable, Integer stage) {
        ImmutableList<ImmutablePair<Variable, Boolean>> updateResult = Stream.concat(
                falseVariable.stream()
                        .filter(v -> !v.getKey().formatFunctionKeyWithValue().equals(trueVariable.formatFunctionKeyWithValue()))
                        .map(v -> ImmutablePair.of(v.getKey().toBuilder().stage(stage).build(), false)),
                Stream.of(ImmutablePair.of(trueVariable.toBuilder().stage(stage).build(), true)))
                .collect(ImmutableList.toImmutableList());

        return updateResult.stream();
    }
}
