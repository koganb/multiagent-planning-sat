package il.ac.bgu.sat;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.sat4j.maxsat.SolverFactory;
import org.sat4j.maxsat.WeightedMaxSatDecorator;
import org.sat4j.maxsat.reader.WDimacsReader;
import org.sat4j.reader.Reader;
import org.sat4j.specs.IProblem;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Slf4j
public class SatSolutionSolver implements SatSolutionSolverInter {
    WeightedMaxSatDecorator solver = new WeightedMaxSatDecorator(SolverFactory.newDefault());

    @Override
    public Optional<List<? extends Formattable>> solveCnf(String cnfPlan, Map<Formattable, Integer> codeMap) {
        //solver.setVerbose(true);
        Reader reader = new WDimacsReader(solver);

        try {
            IProblem problem = reader.parseInstance(IOUtils.toInputStream(cnfPlan, "UTF-8"));
            if (problem.isSatisfiable()) {
                log.info(" Satisfiable !");

                StringWriter out = new StringWriter();
                PrintWriter writer = new PrintWriter(out);
                reader.decode(problem.model(), writer);

                Map<Integer, Boolean> codeResultsMap = Arrays.stream(out.toString().split("\\s")).
                        filter(t -> ObjectUtils.notEqual(t, "0")).
                        collect(Collectors.toMap(t -> Math.abs(Integer.parseInt(t)),
                                t -> Integer.parseInt(t) > 0));

                Map<? extends Formattable, Boolean> variablesResult = codeMap.entrySet().stream().collect(
                        Collectors.toMap(Map.Entry::getKey, t -> codeResultsMap.get(t.getValue()),
                                (p1, p2) -> p1));

                log.debug("variables result \n{}", variablesResult.entrySet().stream().
                        map(Object::toString).
                        collect(Collectors.joining("\n")));

                ImmutableList<Formattable> failedActions = variablesResult.entrySet().stream().
                        filter(entry -> entry.getKey().getValue().matches(Action.State.FAILED.name()) &&
                                entry.getValue()).map(Map.Entry::getKey).collect(ImmutableList.toImmutableList());
                return failedActions.isEmpty() ? Optional.empty() : Optional.of(failedActions);

            } else {
                log.warn(" Unsatisfiable !");
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
