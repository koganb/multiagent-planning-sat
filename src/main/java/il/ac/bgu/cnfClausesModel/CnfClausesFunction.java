package il.ac.bgu.cnfClausesModel;

import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import il.ac.bgu.plan.PlanAction;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface CnfClausesFunction {
    Stream<List<FormattableValue<? extends Formattable>>> apply(Integer currentStage, PlanAction step,
                                                                Map<String, List<Variable>> variableStateMap);
}
