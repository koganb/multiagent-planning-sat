package il.ac.bgu;

import lombok.extern.slf4j.Slf4j;
import org.agreement_technologies.agents.MAPboot;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_planner.POPAction;
import org.agreement_technologies.service.map_planner.POPStep;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.reader.DimacsReader;
import org.sat4j.reader.Reader;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Boris on 26/05/2017.
 */
@Slf4j
public class SatSolver {
    public static void main(String[] args) throws IOException {

        //get agent definitions from file
        String[] agentDefs = Files.readAllLines(Paths.get("C:\\MyProjects\\FMAP\\elevator1")).stream().
                flatMap(t -> Arrays.stream(t.split("\t"))).
                toArray(String[]::new);

        //calculate solution plan
        TreeMap<Integer, Set<Step>> sortedPlan = calculateSolution(agentDefs);

        Pair<Map<String, Integer>, String> cnfEncoding = compilePlanToCnf(sortedPlan);


        runSatSolver(cnfEncoding.getRight(), cnfEncoding.getLeft());
    }


    private static Pair<Map<String, Integer>, String> compilePlanToCnf(TreeMap<Integer, Set<Step>> sortedPlan) {

        CnfCompilation cnfCompilation = new CnfCompilation(sortedPlan);
        List<List<ImmutablePair<String, Boolean>>> planCnfCompilation = cnfCompilation.compileToCnf();

        List<List<ImmutablePair<String, Boolean>>> initFacts = cnfCompilation.calcInitFacts();
        List<List<ImmutablePair<String, Boolean>>> finalFacts = cnfCompilation.calcFinalFacts();

        List<List<ImmutablePair<String, Boolean>>> fullPlanCnfCompilation = Stream.concat(
                Stream.concat(initFacts.stream(), planCnfCompilation.stream()),
                finalFacts.stream()).
                collect(Collectors.toList());


        Pair<Map<String, Integer>, String> cnfEncoding = CnfEncodingUtils.encode(fullPlanCnfCompilation);

        return cnfEncoding;
    }

    private static void runSatSolver(String cnfPlan, Map<String, Integer> codeMap) {
        ISolver solver = SolverFactory.newDefault();
        solver.setTimeout(3600); // 1 hour timeout
        Reader reader = new DimacsReader(solver);

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

                Map<String, Boolean> variablesResult = codeMap.entrySet().stream().collect(
                        Collectors.toMap(Map.Entry::getKey, t -> codeResultsMap.get(t.getValue()),
                                (p1, p2) -> p1, TreeMap::new));

                log.info("variables result {}", variablesResult);
                TreeMap<String, Boolean> actionResultsMap = variablesResult.entrySet().stream().
                        filter(t -> t.getKey().contains("h(")).
                        collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (p1, p2) -> p1, TreeMap::new));

                log.info("Action variables {}", actionResultsMap);


            } else {
                log.warn(" Unsatisfiable !");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static TreeMap<Integer, Set<Step>> calculateSolution(String[] agentDefs) {
        Set<Map<Integer, Set<Step>>> solutionPlans = MAPboot.runCommandLine(agentDefs);

        Map<Integer, Set<Set<Step>>> solutionsBySteps = solutionPlans.stream().
                flatMap(i -> i.entrySet().stream()).
                collect(
                        Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(
                                Map.Entry::getValue,
                                Collectors.toSet())));

        TreeMap<Integer, Set<Step>> sortedPlan = solutionsBySteps.entrySet().stream().map(
                i -> new ImmutablePair<>(i.getKey(), mergeStageStepsOfDifferentPlans(i.getValue()))).

                collect(Collectors.groupingBy(Pair::getLeft, TreeMap::new,

                        Collector.of(
                                HashSet::new,
                                (col, pair) -> col.addAll(pair.getRight()),
                                (r1, r2) -> {
                                    r1.addAll(r2);
                                    return r1;
                                }
                        )));

        return sortedPlan;

    }


    private static Set<Step> mergeStageStepsOfDifferentPlans(Set<Set<Step>> steps) {
        return steps.stream().
                //filter out steps with precondition or effect functions null
                        flatMap(Collection::stream).filter(t ->
                        t.getPopPrecs().stream().allMatch(y -> y.getFunction() != null) &&
                                t.getPopEffs().stream().allMatch(y -> y.getFunction() != null)).
                //create map to filter duplicate steps
                        collect(
                        Collectors.groupingBy(Step::getActionName, Collectors.toList())).
                //in case of initial step take all steps (because of privacy) else take first one;
                        entrySet().stream().flatMap(t ->
                        t.getKey().equals("Initial") ?
                                //merge effects of initial step
                                Stream.of(new POPStep(new POPAction(
                                        "Initial",
                                        new ArrayList<>(),
                                        t.getValue().stream().flatMap(k -> k.getPopEffs().stream()).distinct().collect(Collectors.toList())), 0, null))
                                : Stream.of(t.getValue().get(0))).
                        collect(Collectors.toSet());

    }

}
