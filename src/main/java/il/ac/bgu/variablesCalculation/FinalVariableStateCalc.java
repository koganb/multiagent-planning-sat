package il.ac.bgu.variablesCalculation;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;

import java.util.Collection;

public interface FinalVariableStateCalc {
    ImmutableList<FormattableValue<Formattable>> getFinalVariableState(Collection<Action> failedActions);
}
