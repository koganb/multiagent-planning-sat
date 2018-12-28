package il.ac.bgu.cnfClausesModel;

import com.google.common.collect.Lists;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CnfClausesUtils {

    public static Stream<List<FormattableValue<? extends Formattable>>> switchTrueExclusive(
            Variable variable, Map<String, List<Variable>> variableStateMap, Integer stage) {

        return Stream.concat(
                Stream.of(Lists.newArrayList(FormattableValue.of(
                        variable.toBuilder().stage(stage).build(), true))),
                variableStateMap.get(variable.formatFunctionKey()).stream()
                        .filter(v -> !v.formatFunctionKeyWithValue().equals(
                                variable.formatFunctionKeyWithValue()))
                        .map(v -> v.toBuilder().stage(stage).build())
                        .map(v -> Lists.newArrayList(FormattableValue.of(v, false))));
    }

    public static Stream<List<FormattableValue<? extends Formattable>>> addTrue(
            Variable variable, Map<String, List<Variable>> variableStateMap,
            Integer prevStage, Integer newStage) {

        return Stream.concat(
                Stream.of(Lists.newArrayList(FormattableValue.of(
                        variable.toBuilder().stage(newStage).build(), true))),
                variableStateMap.get(variable.formatFunctionKey()).stream()
                        .filter(v -> !v.formatFunctionKeyWithValue().equals(
                                variable.formatFunctionKeyWithValue()))
                        .flatMap(v -> Stream.of(
                                Lists.newArrayList(
                                        FormattableValue.of(v.toBuilder().stage(prevStage).build(), false),
                                        FormattableValue.of(v.toBuilder().stage(newStage).build(), true)
                                ),
                                Lists.newArrayList(
                                        FormattableValue.of(v.toBuilder().stage(prevStage).build(), true),
                                        FormattableValue.of(v.toBuilder().stage(newStage).build(), false)
                                ))));
    }



    public static Stream<List<FormattableValue<? extends Formattable>>> applyPassThrough(
            Variable variable, Map<String, List<Variable>> variableStateMap,
            Integer prevStage, Integer newStage) {

        return variableStateMap.get(variable.formatFunctionKey()).stream()
                .flatMap(v -> Stream.of(
                        Lists.newArrayList(
                                FormattableValue.of(v.toBuilder().stage(prevStage).build(), false),
                                FormattableValue.of(v.toBuilder().stage(newStage).build(), true)
                        ),
                        Lists.newArrayList(
                                FormattableValue.of(v.toBuilder().stage(prevStage).build(), true),
                                FormattableValue.of(v.toBuilder().stage(newStage).build(), false)
                        )
                ));
    }
}
