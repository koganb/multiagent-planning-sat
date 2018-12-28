package il.ac.bgu.cnfClausesModel;

import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import org.agreement_technologies.common.map_planner.Step;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface CnfClausesFunction {
    Stream<List<FormattableValue<? extends Formattable>>> apply(Integer currentStage, Step step,
                                                                Map<String, List<Variable>> variableStateMap);
}
