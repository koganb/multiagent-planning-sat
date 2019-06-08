package il.ac.bgu.sat;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.cnfCompilation.failureContraints.MaxFailureConstraintsCreator;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.plan.PlanAction;
import il.ac.bgu.utils.CnfEncodingUtils;
import io.vavr.control.Either;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.slf4j.MarkerFactory.getMarker;


public class SolutionIterator implements Iterator<Either<Throwable, Optional<List<? extends Formattable>>>> {

    private static final Logger log = LoggerFactory.getLogger(SolutionIterator.class);

    private final static int MAX_SOLUTION_SIZE = 6;

    private List<List<FormattableValue<? extends Formattable>>> hardConstraints;
    private final List<FormattableValue<Formattable>> softConstraints;

    private final SatSolutionSolverInter satSolutionSolver;
    private DiagnosisFindingStopIndicator stopIndicator;
    private final MaxFailureConstraintsCreator maxFailureConstraintsCreator;


    private List<List<FormattableValue<? extends Formattable>>> solutionConstraints = new ArrayList<>();

    private boolean solutionFoundInCurIteration = false;
    private boolean solutionFound = false;

    private int currentSolutionSize = 1;
    private boolean exceptionRaised = false;


    public SolutionIterator(Map<Integer, ImmutableList<PlanAction>> plan,
                            List<List<FormattableValue<? extends Formattable>>> hardConstraints,
                            List<FormattableValue<Formattable>> softConstraints,
                            long satTimeoutMils, DiagnosisFindingStopIndicator stopIndicator) {
        this.hardConstraints = hardConstraints;
        this.softConstraints = softConstraints;
        this.maxFailureConstraintsCreator = new MaxFailureConstraintsCreator(plan);
        this.satSolutionSolver = new SatSolutionSolver(satTimeoutMils);
        this.stopIndicator = stopIndicator;
    }


    @Override
    public Either<Throwable, Optional<List<? extends Formattable>>> next() {

        try {
            List<List<FormattableValue<? extends Formattable>>> hardConstraintsFull =
                    ImmutableList.<List<FormattableValue<? extends Formattable>>>builder()
                            .addAll(hardConstraints)
                            .addAll(maxFailureConstraintsCreator.createMaxFailuresClauses(currentSolutionSize))
                            .addAll(solutionConstraints)
                            .build();


            Pair<Map<Formattable, Integer>, String> cnfEncoding =
                    CnfEncodingUtils.encode(hardConstraintsFull, softConstraints);

            log.debug("Encoding:\n{}\n_____", cnfEncoding.getRight());
            log.debug("Code Map:\n{}\n", cnfEncoding.getLeft());

            Instant satSolverStartTime = Instant.now();

            Optional<List<? extends Formattable>> diagnosisCandidates = satSolutionSolver.solveCnf(
                    cnfEncoding.getRight(),
                    cnfEncoding.getLeft());

            Instant satSolverEndTime = Instant.now();
            log.info(getMarker("STATS"), "      - ");
            log.info(getMarker("STATS"), "        mils: {}", Duration.between(satSolverStartTime, satSolverEndTime).toMillis());
            log.info(getMarker("STATS"), "        is_found: {}", diagnosisCandidates.isPresent());
            log.info(getMarker("STATS"), "        solution_size: {}", currentSolutionSize);
            log.info("Solution candidate found: {}", diagnosisCandidates.isPresent());

            //add solution negation to solution constraints
            Optional<ImmutableList<FormattableValue<? extends Formattable>>> constraintOptional = diagnosisCandidates
                    .map(solution -> solution.stream()
                            .map(constraint -> FormattableValue.of(constraint, false))
                            .collect(ImmutableList.toImmutableList()));

            constraintOptional.ifPresent(solution -> solutionConstraints.add(solution));

            solutionFoundInCurIteration = diagnosisCandidates.isPresent();

            if (solutionFoundInCurIteration) {
                solutionFound = true;
            } else {
                currentSolutionSize++;
            }
            return Either.right(diagnosisCandidates);
        } catch (Throwable e) {
            log.info(getMarker("STATS"), "    exception: {}", e.getClass().getCanonicalName());
            exceptionRaised = true;
            return Either.left(e);
        }
    }


    @Override
    public boolean hasNext() {
        if (exceptionRaised) {
            return false;
        }
        switch (stopIndicator) {
            case FIRST_SOLUTION:
                //continue to search for solution till it is found
                return !solutionFound && currentSolutionSize < MAX_SOLUTION_SIZE;
            case MINIMAL_CARDINALITY:
                //continue to search for solution if it is found in current iteration or not found so far
                return (solutionFoundInCurIteration || !solutionFound) && currentSolutionSize < MAX_SOLUTION_SIZE;
            case MINIMAL_SUBSET:
            default:
                //continue to search till constraints are valid
                return currentSolutionSize < MAX_SOLUTION_SIZE;
        }

    }

}
