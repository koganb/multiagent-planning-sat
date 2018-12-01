package il.ac.bgu

import com.google.common.collect.ImmutableList
import com.google.common.collect.Streams
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.stream.Collectors
import java.util.stream.Stream

import static org.slf4j.MarkerFactory.getMarker

class TestUtils {
    private static final Logger log = LoggerFactory.getLogger(TestUtils.class);


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

    static Stream<List<Formattable>> calculateSolutions(List<List<FormattableValue<Formattable>>> hardConstraints,
                                                        List<FormattableValue<Formattable>> softConstraints,
                                                        FinalVariableStateCalc finalVariableStateCalc,
                                                        Collection<Action> failedActions) {


        log.info 'final facts calculation'


        Instant start = Instant.now()
        ImmutableList<FormattableValue<Formattable>> finalFacts =
                finalVariableStateCalc.getFinalVariableState(failedActions);
        log.info(getMarker("STATS"), "    final_var_state_calc_mils: {}", Duration.between(start, Instant.now()).toMillis());

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


        log.info(getMarker("STATS"), "    sat_solving_mils:");
        return Streams.stream(solutionIterator).
                filter { solution -> solution.isPresent() }.
                map { solution -> solution.get() }.
                peek { solution ->
                    log.info 'Solution candidate: {}', solution

                    def solutionFinalVariablesState = finalVariableStateCalc.getFinalVariableState(solution)

                    if (finalFacts.intersect(solutionFinalVariablesState).size() !=
                            solutionFinalVariablesState.size()) {
                        throw new RuntimeException('Not equal final states: failedActionsFinalVariablesState: and solutionFinalVariablesState ' +
                                finalFacts - solutionFinalVariablesState)

                    }
                }


    }

    static Map<Integer, Set<Step>> loadPlan(planName) {
        log.info("Start loading plan {}", planName)

        def serPlanFileName = String.format("plans/%s.ser", planName)
        TreeMap<Integer, Set<Step>> plan
        if (TestUtils.class.getClassLoader().getResource(serPlanFileName) == null) {
            String[] agentDefs = Files.readAllLines(
                    Paths.get(TestUtils.class.getClassLoader().getResource("problems/" + planName).toURI())).stream().
                    flatMap({ t -> Arrays.stream(t.split("\t")) }).
                    collect(Collectors.toList()).toArray(new String[0])

            plan = SatSolver.calculateSolution(agentDefs)
            SerializationUtils.serialize(plan, new FileOutputStream(serPlanFileName))
        } else {
            plan = SerializationUtils.deserialize(TestUtils.class.getClassLoader().getResourceAsStream(serPlanFileName))
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


    def static createStatsLogging(problemName, plan, planClausesCreationTime, failedActions,
                                  conflictRetriesModel, conflictClausesCreator, failedClausesCreator, maxFailedActionNumber) {
        log.info(getMarker("STATS"), "- problem: {}", problemName)
        log.info(getMarker("STATS"), "  plan_properties:")
        log.info(getMarker("STATS"), "    failed_actions: \"{}\"", failedActions)
        log.info(getMarker("STATS"), "    number_of_steps: {}", plan.size() - 1) //-1 for initial action
        log.info(getMarker("STATS"), "    number_of_actions: {}",
                plan.values().stream().flatMap { v -> v.stream() }.count() - 1)
        log.info(getMarker("STATS"), "    number_of_agents: {}",
                plan.values().stream()
                        .flatMap { v -> v.stream() }
                        .map { v -> v.getAgent() }
                        .filter { v -> v != null }
                        .distinct()
                        .count())
        log.info(getMarker("STATS"), "  cnf_model_details: ")
        log.info(getMarker("STATS"), "    failure_model: {}", failedClausesCreator.getName())
        log.info(getMarker("STATS"), "    conflict_model: {}", conflictClausesCreator.getName())
        log.info(getMarker("STATS"), "    conflict_retries_model: {}", conflictRetriesModel.getName())
        log.info(getMarker("STATS"), "    max_failures_restriction: {}", maxFailedActionNumber)
        log.info(getMarker("STATS"), "  execution_time:")
        log.info(getMarker("STATS"), "    cnf_compilation_mils: {}", planClausesCreationTime[problemName])

    }
}


