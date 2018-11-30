package il.ac.bgu.sat;

import il.ac.bgu.dataModel.Formattable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SatSolutionSolverInter {
    Optional<List<Formattable>> solveCnf(String cnfPlan, Map<Formattable, Integer> codeMap);
}
