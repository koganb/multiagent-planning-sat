package il.ac.bgu.failureModel;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static il.ac.bgu.CnfEncodingUtils.createEffInstance;
import static il.ac.bgu.dataModel.Variable.UNDEFINED;

public class DelayStageFailureModel implements FailureModelFunction {
    private Integer numberOfStagesToDelay;

    public DelayStageFailureModel(Integer numberOfStagesToDelay) {
        this.numberOfStagesToDelay = numberOfStagesToDelay;
    }

    @Override
    public Stream<ImmutablePair<String, Boolean>> apply(Integer stage,
                                                        String variableKey,
                                                        Set<ImmutablePair<String, Boolean>> variableSetBeforeAction,
                                                        Set<ImmutablePair<String, Boolean>> variableSetAfterAction) {
        ImmutableSet<ImmutablePair<String, Boolean>> result = Optional.ofNullable(variableSetAfterAction).
                //add multiple value variable fluents
                        map(stateVars ->
                        stateVars.stream().
                                filter(effPair -> !effPair.getKey().contains(UNDEFINED)).
                                flatMap(effPair -> Stream.of(
                                        IntStream.range(1, numberOfStagesToDelay + 1).
                                                mapToObj(futureStage ->
                                                        ImmutablePair.of(
                                                                createEffInstance(effPair.getKey(), stage + futureStage), false)),
                                        IntStream.range(1, numberOfStagesToDelay + 1).
                                                mapToObj(futureStage ->
                                                        ImmutablePair.of(
                                                                createEffInstance(variableKey, stage + futureStage, UNDEFINED), true)
                                                ),
                                        Stream.of(
                                                ImmutablePair.of(
                                                        createEffInstance(effPair.getKey(), stage + numberOfStagesToDelay + 1), effPair.getRight()
                                                )),
                                        Stream.of(
                                                ImmutablePair.of(
                                                        createEffInstance(variableKey, stage + numberOfStagesToDelay + 1, UNDEFINED), false
                                                ))

                                        ).reduce(Stream::concat).orElse(Stream.empty())
                                )).
                        orElse(Stream.empty()).collect(ImmutableSet.toImmutableSet());

        return result.stream();
    }
}
