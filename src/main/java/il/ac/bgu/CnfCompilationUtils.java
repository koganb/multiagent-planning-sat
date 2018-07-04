package il.ac.bgu;

import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.stream.Stream;

/**
 * Created by Boris on 01/07/2017.
 */

@Slf4j
public class CnfCompilationUtils {


    public static ImmutableSet<ImmutablePair<String, Boolean>> updateVariableState
            (ImmutableSet<ImmutablePair<String, Boolean>> variableState, String effId) {
        //update previous values
        ImmutableSet<ImmutablePair<String, Boolean>> updatedVariableState = Stream.concat(
                //update previous values
                variableState.stream().
                        filter(pair -> !pair.getKey().equals(effId)).
                        map(pair -> ImmutablePair.of(pair.getKey(), false)),
                Stream.of(ImmutablePair.of(effId, true))).
                collect(ImmutableSet.toImmutableSet());
        return updatedVariableState;


    }
}
