package il.ac.bgu.variablesCalculation;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;

import java.util.Collection;

public interface FinalVariableStateCalc {
    ImmutableList<FormattableValue<? extends Formattable>> getFinalVariableState(Collection<? extends Formattable> failedActions);
}
