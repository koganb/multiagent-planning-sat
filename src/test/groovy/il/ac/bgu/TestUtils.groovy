package il.ac.bgu

import com.google.common.collect.ImmutableList
import com.google.common.collect.Streams
import il.ac.bgu.cnfCompilation.CnfCompilation
import il.ac.bgu.dataModel.Action
import il.ac.bgu.dataModel.Formattable
import il.ac.bgu.dataModel.FormattableValue
import il.ac.bgu.failureModel.VariableModelFunction
import il.ac.bgu.sat.SatSolutionSolver
import il.ac.bgu.sat.SolutionIterator
import org.agreement_technologies.common.map_planner.Step
import org.apache.commons.lang3.SerializationUtils
import org.apache.commons.lang3.tuple.Pair

import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

import static il.ac.bgu.dataModel.Action.State.FAILED

class TestUtils {

    static class Problem {
        String problemName
        Collection<Action> ignoreFailedActions = []

        String getProblemName() {
            return problemName
        }

        Collection<Action> getIgnoreFailedActions() {
            return ignoreFailedActions
        }

        Problem(String problemName, Collection<Action> ignoreFailedActions) {
            this.problemName = problemName
            this.ignoreFailedActions = ignoreFailedActions
        }

        Problem(String problemName) {
            this.problemName = problemName
        }

    }

    static Boolean checkSolution(TreeMap<Integer, Set<Step>> plan, VariableModelFunction failureModel, Collection<Action> failedActions) {
        CnfCompilation cnfCompilation = new CnfCompilation(plan, failureModel)
        def finalFactsWithFailedActions = new FinalVariableStateCalc(plan, failureModel).getFinalVariableState(failedActions)


        Pair<ImmutableList<ImmutableList<FormattableValue<Formattable>>>,
                ImmutableList<FormattableValue<Formattable>>> compilePlanToCnf =
                SatSolver.compilePlanToCnf(cnfCompilation, failedActions)


        def solutionIterator = new SolutionIterator(
                compilePlanToCnf.getLeft(), compilePlanToCnf.getRight(), new SatSolutionSolver())


        return Streams.stream(solutionIterator).
                filter { solution -> solution.isPresent() }.
                map { solution -> solution.get() }.
                filter { solution ->

                    def solutionFinalState = cnfCompilation.calcFinalFacts(failedActions)

                    println("Solution candidate: " + solution)

                    return (!solution.isEmpty() &&
                            solutionFinalState.containsAll(finalFactsWithFailedActions) &&
                            finalFactsWithFailedActions.containsAll(solutionFinalState) &&
                            failedActions.stream()
                                    .map({ t -> t.toBuilder().state(FAILED).build() })
                                    .collect(Collectors.toSet()).containsAll(solution))
                }
        .findFirst()
                .isPresent()

    }

    static TreeMap<Integer, Set<Step>> loadPlan(planName) {
        def serPlanFileName = planName + ".ser"
        TreeMap<Integer, Set<Step>> plan
        if (!new File(serPlanFileName).exists()) {
            String[] agentDefs = Files.readAllLines(
                    Paths.get(this.getClass().getClassLoader().getResource("problems/" + planName).toURI())).stream().
                    flatMap({ t -> Arrays.stream(t.split("\t")) }).
                    collect(Collectors.toList()).toArray(new String[0])

            plan = SatSolver.calculateSolution(agentDefs)
            SerializationUtils.serialize(plan, new FileOutputStream(serPlanFileName))
        } else {
            plan = SerializationUtils.deserialize(new FileInputStream(serPlanFileName))
        }
        return plan
    }

    def static printPlan(TreeMap<Integer, Set<Step>> plan) {
        plan.entrySet().stream()
                .filter({ entry -> entry.key != -1 })
                .forEach({ entry ->
            printf("Step: %s\n", entry.key)
            entry.value.forEach({ step -> printf("\t%-13s: %s\n", step.agent, step) })
        })
    }
}


