package il.ac.bgu.sat;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.cnfCompilation.failureContraints.MaxFailureConstraintsCreator;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.utils.CnfEncodingUtils;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.slf4j.MarkerFactory.getMarker;


public class SolutionIterator implements Iterator<Optional<List<Formattable>>> {

    private static final Logger log = LoggerFactory.getLogger(SolutionIterator.class);

    private final static int MAX_SOLUTION_SIZE = 5;

    private List<List<FormattableValue<Formattable>>> hardConstraints;
    private final List<FormattableValue<Formattable>> softConstraints;

    private final SatSolutionSolverInter satSolutionSolver = new SatSolutionSolver();
    private final MaxFailureConstraintsCreator maxFailureConstraintsCreator;


    private List<List<FormattableValue<Formattable>>> solutionConstraints = new ArrayList<>();

    private boolean solutionFound = true;

    private int currentSolutionSize = 1;

    public SolutionIterator(Map<Integer, Set<Step>> plan,
                            List<List<FormattableValue<Formattable>>> hardConstraints,
                            List<FormattableValue<Formattable>> softConstraints) {
        this.hardConstraints = hardConstraints;
        this.softConstraints = softConstraints;
        maxFailureConstraintsCreator = new MaxFailureConstraintsCreator(plan);

    }


    @Override
    public Optional<List<Formattable>> next() {

        List<List<FormattableValue<Formattable>>> hardConstraintsFull =
                ImmutableList.<List<FormattableValue<Formattable>>>builder()
                        .addAll(hardConstraints)
                        .addAll(maxFailureConstraintsCreator.createMaxFailuresClauses(currentSolutionSize))
                        .addAll(solutionConstraints)
                        .build();


        Pair<Map<Formattable, Integer>, String> cnfEncoding =
                CnfEncodingUtils.encode(hardConstraintsFull, softConstraints);

        log.debug("Encoding:\n{}\n_____", cnfEncoding.getRight());
        log.debug("Code Map:\n{}\n", cnfEncoding.getLeft());

        Instant satSolverStartTime = Instant.now();

        Optional<List<Formattable>> diagnosisCandidates = satSolutionSolver.solveCnf(
                cnfEncoding.getRight(),
                cnfEncoding.getLeft());

        Instant satSolverEndTime = Instant.now();
        log.info(getMarker("STATS"), "    - {}", Duration.between(satSolverStartTime, satSolverEndTime).toMillis());
        log.info("Solution candidate found: {}", diagnosisCandidates.isPresent());

        //add solution negation to solution constraints
        diagnosisCandidates
                .map(solution -> solution.stream()
                                .map(constraint -> FormattableValue.of(constraint, false))
                        .collect(ImmutableList.toImmutableList()))
                .ifPresent(solution -> solutionConstraints.add(solution));


        solutionFound = diagnosisCandidates.isPresent();


        if (!solutionFound &&  //solution not found
                solutionConstraints.isEmpty() &&  //no solution found so far
                currentSolutionSize < MAX_SOLUTION_SIZE) {
            currentSolutionSize++;
        }



        return diagnosisCandidates;


    }


    @Override
    public boolean hasNext() {
        return solutionFound || (solutionConstraints.isEmpty() && currentSolutionSize < MAX_SOLUTION_SIZE);
    }

}
