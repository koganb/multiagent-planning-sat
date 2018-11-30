package il.ac.bgu

import com.google.common.collect.ImmutableList
import com.google.common.collect.Streams
import groovy.util.logging.Slf4j
import il.ac.bgu.cnfClausesModel.CnfClausesFunction
import il.ac.bgu.cnfCompilation.CnfCompilation
import il.ac.bgu.cnfCompilation.PlanUtils
import il.ac.bgu.cnfCompilation.retries.RetryPlanUpdater
import il.ac.bgu.dataModel.Action
import il.ac.bgu.dataModel.Formattable
import il.ac.bgu.dataModel.FormattableValue
import il.ac.bgu.sat.SatSolutionSolver
import il.ac.bgu.sat.SolutionIterator
import il.ac.bgu.variablesCalculation.FinalVariableStateCalc
import one.util.streamex.StreamEx
import org.agreement_technologies.common.map_planner.Step
import org.apache.commons.lang3.SerializationUtils

import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

import static il.ac.bgu.dataModel.Action.State.FAILED

@Slf4j
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

    static Boolean checkSolution(List<List<FormattableValue<Formattable>>> hardConstraints,
                                 List<FormattableValue<Formattable>> softConstraints,
                                 FinalVariableStateCalc finalVariableStateCalc,
                                 Collection<Action> failedActions) {


        log.info 'final facts calculation'
        ImmutableList<FormattableValue<Formattable>> finalFacts =
                finalVariableStateCalc.getFinalVariableState(failedActions);

        //noinspection GroovyAssignabilityCheck
        List<List<FormattableValue<Formattable>>> hardConstraintsWithFinal =
                StreamEx.<List<FormattableValue<Formattable>>> of()
                        .append(hardConstraints)
                        .append(finalFacts.stream().map { f -> ImmutableList.<FormattableValue<Formattable>> of(f) })
                        .collect(ImmutableList.toImmutableList())

        if (log.isDebugEnabled()) {
            log.debug("Hard contraints: {} ", hardConstraintsWithFinal.stream().map {
                t ->
                    t.stream().map {
                        k -> k.toString()
                    }.collect(Collectors.joining("\n"))
            }.collect(Collectors.joining("\n")))
        }


        def solutionIterator = new SolutionIterator(hardConstraintsWithFinal, softConstraints, new SatSolutionSolver())


        log.info 'start SAT solving'
        return Streams.stream(solutionIterator).
                filter { solution -> solution.isPresent() }.
                map { solution -> solution.get() }.
                filter { solution ->
                    log.info 'Solution candidate: {}', solution

                    def solutionFinalVariablesState = finalVariableStateCalc.getFinalVariableState(solution)

                    if (finalFacts.intersect(solutionFinalVariablesState).size() !=
                            solutionFinalVariablesState.size()) {
                        throw new RuntimeException('Not equal final states: failedActionsFinalVariablesState: and solutionFinalVariablesState ' +
                                finalFacts - solutionFinalVariablesState)

                    }

                    log.info("Solution candidate: {}", solution)

                    return (!solution.isEmpty() &&
                            failedActions.stream()
                                    .map({ t -> t.toBuilder().state(FAILED).build() })
                                    .collect(Collectors.toSet()).containsAll(solution))
                }
        .findFirst()
                .isPresent()

    }

    static Map<Integer, Set<Step>> loadPlan(planName) {
        log.info("Start loading plan {}", planName)

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

        //add agent to preconditions and effects of every action to prevent action collisions in delay failure model
        PlanUtils.updatePlanWithAgentDependencies(plan)


        return plan
    }

    def static printPlan(Map<Integer, Set<Step>> plan) {
        plan.entrySet().stream()
                .filter({ entry -> entry.key != -1 })
                .forEach({ entry ->
            printf("Step: %s\n", entry.key)
            entry.value.forEach({ step -> printf("\t%-13s: %s\n", step.agent, step) })
        })
    }

    //costraints results container
    static class Tuple1<T> {
        private T object;

        private Tuple1(T object) {
            this.object = object
        }

        static Tuple1<T> of(T object) {
            new Tuple1<T>(object);
        }

        T get() {
            return object
        }
    }


    def static createPlanHardConstraints(Map<Integer, Set<Step>> plan,
                                         RetryPlanUpdater retryPlanUpdater,
                                         CnfClausesFunction healthyCnfClausesCreator,
                                         CnfClausesFunction conflictCnfClausesCreator,
                                         CnfClausesFunction failedCnfClausesCreator,
                                         int maxFailures) {


        CnfCompilation cnfCompilation = new CnfCompilation(plan, retryPlanUpdater, healthyCnfClausesCreator,
                conflictCnfClausesCreator, failedCnfClausesCreator, maxFailures);

        final def hardContraints = Tuple1.of(StreamEx.<List<FormattableValue<Formattable>>> of()
                .append(cnfCompilation.compileToCnf())
                .append(cnfCompilation.calcInitFacts().collect { f -> ImmutableList.of(f) })
                .collect(ImmutableList.toImmutableList()))

        return hardContraints

    }
}


