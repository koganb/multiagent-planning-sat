package il.ac.bgu.sat

import com.google.common.collect.ImmutableSet
import org.apache.commons.lang3.tuple.ImmutablePair
import spock.lang.Specification


class TestSolutionIterator extends Specification {


    def "check solution finder"() {
        setup:
        def satSolutionSolver = Stub(SatSolutionSolverInter)
        satSolutionSolver.solveCnf(*_) >>
                Optional.of(ImmutableSet.of("1", "2")) >>
                Optional.of(ImmutableSet.of("3")) >>
                Optional.empty() >>
                Optional.of(ImmutableSet.of("4")) >>
                Optional.empty()

        def satSolutionFinder = new SolutionIterator(ImmutableSet.of(), ImmutableSet.of(), satSolutionSolver)

        expect:
        satSolutionFinder.nextSolution() == Optional.of(ImmutableSet.of("1", "2"))
        satSolutionFinder.nextSolution() == Optional.of(ImmutableSet.of("3"))
        satSolutionFinder.nextSolution() == Optional.of(ImmutableSet.of("4"))
        satSolutionFinder.nextSolution() == Optional.empty()
        satSolutionFinder.nextSolution() == Optional.empty()

        and:
        assert satSolutionFinder.constraintsPointer.isRoot()
        satSolutionFinder.constraintsPointer.data().additionalConstraints == ImmutableSet.of()
        satSolutionFinder.constraintsPointer.find(new SolutionIterator.NodeData(
                ImmutableSet.of(
                        ImmutableSet.of(ImmutablePair.of("1", true)),
                        ImmutableSet.of(ImmutablePair.of("3", true))))).
                parent().data() == new SolutionIterator.NodeData(
                ImmutableSet.of(ImmutableSet.of(ImmutablePair.of("1", true))))

        satSolutionFinder.constraintsPointer.find(new SolutionIterator.NodeData(
                ImmutableSet.of(
                        ImmutableSet.of(ImmutablePair.of("2", true)),
                        ImmutableSet.of(ImmutablePair.of("4", true))))).
                parent().data() == new SolutionIterator.NodeData(
                ImmutableSet.of(ImmutableSet.of(ImmutablePair.of("2", true))))

    }


}