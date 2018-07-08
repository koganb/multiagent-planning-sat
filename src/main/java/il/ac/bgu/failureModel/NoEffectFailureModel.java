package il.ac.bgu.failureModel;

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static il.ac.bgu.CnfEncodingUtils.createEffInstance;
import static il.ac.bgu.CnfEncodingUtils.encodeValue;

public class NoEffectFailureModel implements FailureModelFunction {
    @Override
    public Stream<ImmutablePair<String, Boolean>> apply(Integer stage,
                                                        String variableKey,
                                                        Set<ImmutablePair<String, Boolean>> variableSetBeforeAction,
                                                        Set<ImmutablePair<String, Boolean>> variableSetAfterAction) {
        return Optional.ofNullable(variableSetBeforeAction).
                //add multiple value variable fluents
                        map(stateVars ->
                        stateVars.stream().
                                map(effPair -> new ImmutablePair<>(
                                        createEffInstance(effPair.getKey(), stage + 1),
                                        encodeValue(effPair.getValue(), true)))).
                        orElse(Stream.empty());

    }
}
