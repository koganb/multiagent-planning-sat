package il.ac.bgu.sat;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.CnfEncodingUtils;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.slf4j.MarkerFactory.getMarker;


public class SolutionIterator implements Iterator<Optional<List<Formattable>>> {

    private static final Logger log = LoggerFactory.getLogger(SolutionIterator.class);

    private List<List<FormattableValue<Formattable>>> hardConstraints;
    private final List<FormattableValue<Formattable>> softConstraints;
    private final SatSolutionSolverInter satSolutionSolver;

    private boolean hasNext = true;

    public SolutionIterator(List<List<FormattableValue<Formattable>>> hardConstraints,
                            List<FormattableValue<Formattable>> softConstraints,
                            SatSolutionSolverInter satSolutionSolver) {
        this.hardConstraints = hardConstraints;
        this.softConstraints = softConstraints;
        this.satSolutionSolver = satSolutionSolver;

    }


    @Override
    public Optional<List<Formattable>> next() {

        Pair<Map<Formattable, Integer>, String> cnfEncoding =
                CnfEncodingUtils.encode(hardConstraints, softConstraints);

        log.debug("Encoding:\n{}\n_____", cnfEncoding.getRight());
        log.debug("Code Map:\n{}\n", cnfEncoding.getLeft());

        Instant satSolverStartTime = Instant.now();

        Optional<List<Formattable>> diagnosisCandidates = satSolutionSolver.solveCnf(
                cnfEncoding.getRight(),
                cnfEncoding.getLeft());

        Instant satSolverEndTime = Instant.now();
        log.info(getMarker("STATS"), "    - {}", Duration.between(satSolverStartTime, satSolverEndTime).toMillis());

        log.info("Solution candidate found: {}", diagnosisCandidates.isPresent());


        hasNext = diagnosisCandidates.isPresent();

        //add solution negation as hard constraints
        hardConstraints = ImmutableList.<List<FormattableValue<Formattable>>>builder()
                .addAll(hardConstraints)
                .add(diagnosisCandidates.map(candidatesSet ->
                        candidatesSet.stream()
                                .map(constraint -> FormattableValue.of(constraint, false))
                                .collect(ImmutableList.toImmutableList())
                ).orElse(ImmutableList.of()))
                .build();

        return diagnosisCandidates;


    }


    @Override
    public boolean hasNext() {
        return hasNext;
    }

}
