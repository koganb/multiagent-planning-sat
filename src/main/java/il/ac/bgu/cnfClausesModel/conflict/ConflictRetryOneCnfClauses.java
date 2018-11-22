package il.ac.bgu.cnfClausesModel.conflict;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import il.ac.bgu.cnfClausesModel.CnfClausesFunction;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.variableModel.VariableModelFunction;
import lombok.extern.slf4j.Slf4j;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
public class ConflictRetryOneCnfClauses implements CnfClausesFunction {


    @Override
    public Stream<ImmutableList<FormattableValue<Formattable>>> apply(Integer currentStage,
                                                                      Map<Integer, Set<Step>> plan,
                                                                      ImmutableCollection<FormattableValue<Variable>> variablesState) {

        return null;
    }

    @Override
    public VariableModelFunction getVariableModel() {
        throw new NotImplementedException("TBD");
    }
}
