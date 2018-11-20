package il.ac.bgu.cnfClausesModel;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.variableModel.VariableModelFunction;
import org.agreement_technologies.common.map_planner.Step;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public interface CnfClausesFunction {
    Stream<ImmutableList<FormattableValue<Formattable>>> apply(Integer currentStage,
                                                               Map<Integer, Set<Step>> plan,
                                                               ImmutableCollection<FormattableValue<Variable>> variablesState);


    VariableModelFunction getVariableModel();
}
