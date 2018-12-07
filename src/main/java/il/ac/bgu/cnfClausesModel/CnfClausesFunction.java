package il.ac.bgu.cnfClausesModel;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.variableModel.VariableModelFunction;
import org.agreement_technologies.common.map_planner.Step;

import java.util.stream.Stream;

public interface CnfClausesFunction {
    Stream<ImmutableList<FormattableValue<? extends Formattable>>> apply(Integer currentStage,
                                                                         Step step,
                                                                         ImmutableCollection<FormattableValue<Variable>> variablesState);

    VariableModelFunction getVariableModel();
}
