package il.ac.bgu.sat;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.dataModel.Formattable;

import java.util.Map;
import java.util.Optional;

public interface SatSolutionSolverInter {
    Optional<ImmutableList<Formattable>> solveCnf(String cnfPlan, Map<Formattable, Integer> codeMap);
}
