package il.ac.bgu.sat;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.CnfEncodingUtils;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;


@Slf4j
public class SolutionIterator implements Iterator<Optional<List<Formattable>>> {

    //immutable
    private final List<List<FormattableValue<Formattable>>> hardConstraints;
    private final List<FormattableValue<Formattable>> softConstraints;
    private final SatSolutionSolverInter satSolutionSolver;

    //mutable
    LinkedHashSet<NodeData> constraintsQueue = new LinkedHashSet<>();

    public SolutionIterator(List<List<FormattableValue<Formattable>>> hardConstraints,
                            List<FormattableValue<Formattable>> softConstraints,
                            SatSolutionSolverInter satSolutionSolver) {
        this.hardConstraints = hardConstraints;
        this.softConstraints = softConstraints;
        this.satSolutionSolver = satSolutionSolver;

        //add node without additional constraints
        constraintsQueue.add(new NodeData(ImmutableList.of()));
    }


    @Override
    public Optional<List<Formattable>> next() {

        assert constraintsQueue.size() > 0;   //guarded by hasNext

        //remove first element from the queue
        NodeData headNode = constraintsQueue.stream().findFirst().get();
        constraintsQueue.remove(headNode);

        Pair<Map<Formattable, Integer>, String> cnfEncoding =
                CnfEncodingUtils.encode(
                        ImmutableList.<List<FormattableValue<Formattable>>>builder().
                                addAll(hardConstraints).
                                addAll(headNode.getAdditionalConstraints()).
                                build(),
                        softConstraints);

        log.debug("Encoding:\n{}\n_____", cnfEncoding.getRight());
        log.debug("Code Map:\n{}\n", cnfEncoding.getLeft());

        //get new solution
        Optional<List<Formattable>> diagnosisCandidates = satSolutionSolver.solveCnf(
                cnfEncoding.getRight(),
                cnfEncoding.getLeft());

        log.info("Solution candidate found: {}", diagnosisCandidates.isPresent());

        //add new contraints to the queue
        diagnosisCandidates.ifPresent(candidatesSet ->
                candidatesSet.forEach(constraint -> constraintsQueue.add(
                        new NodeData(ImmutableList.<List<FormattableValue<Formattable>>>builder().
                                addAll(headNode.getAdditionalConstraints().iterator()).
                                add(ImmutableList.of(FormattableValue.of(constraint, false))).
                                build()
                        ))
                )
        );


        return diagnosisCandidates;


    }


    @Override
    public boolean hasNext() {
        return !constraintsQueue.isEmpty();
    }


    @Getter
    @AllArgsConstructor
    @EqualsAndHashCode
    static class NodeData {
        private List<List<FormattableValue<Formattable>>> additionalConstraints;

    }
}
