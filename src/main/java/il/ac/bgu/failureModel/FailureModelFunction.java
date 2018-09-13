package il.ac.bgu.failureModel;

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Set;
import java.util.stream.Stream;

@FunctionalInterface
public interface FailureModelFunction {
    Stream<ImmutablePair<String, Boolean>> apply(Integer step, String variableKey,
                                                 Set<ImmutablePair<String, Boolean>> variableSetBeforeAction,
                                                 Set<ImmutablePair<String, Boolean>> variableSetAfterAction);
}