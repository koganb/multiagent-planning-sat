package il.ac.bgu;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.agreement_technologies.agents.MAPboot;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_planner.POPAction;
import org.agreement_technologies.service.map_planner.POPStep;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.sat4j.maxsat.SolverFactory;
import org.sat4j.maxsat.WeightedMaxSatDecorator;
import org.sat4j.maxsat.reader.WDimacsReader;
import org.sat4j.reader.Reader;
import org.sat4j.specs.IProblem;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static il.ac.bgu.CnfEncodingUtils.ActionState.*;
import static java.lang.String.format;

/**
 * Created by Boris on 26/05/2017.
 */
@Slf4j
public class SatSolver {

    static private Map<String, String> problemNames = new Reflections("problems", new ResourcesScanner())
            .getResources(Pattern.compile(".*\\.problem")).
                    stream().
                    collect(Collectors.toMap(c -> CharMatcher.invisible().removeFrom(c.replace("problems/", "")), Function.identity()));

    public static void main(String[] args) throws IOException, URISyntaxException, ParseException {
        Options options = new Options();
        options.addOption(Option.builder("p").
                desc("problem name = required").
                longOpt("problem_name").
                hasArg(true).
                numberOfArgs(1).
                required(true).build());
        options.addOption(Option.builder("v").
                desc("verbose").
                longOpt("verbose").
                hasArg(false).
                required(false).build());
        options.addOption(Option.builder("f").
                desc("failed actions indexes - optional").
                longOpt("failed index").
                hasArg(true).
                numberOfArgs(Option.UNLIMITED_VALUES).
                required(false).build());
        options.addOption("h", "help", false, "show help.");

        help(options);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption('v')) {
            org.apache.log4j.Logger logger4j = org.apache.log4j.Logger.getRootLogger();
            logger4j.setLevel(org.apache.log4j.Level.toLevel("DEBUG"));
        }

        String problemName = cmd.getOptionValue('p');
        String[] failedSteps = cmd.hasOption('f') ?
                cmd.getOptionValues('f') :
                new String[0];

        //get agent definitions from file
        String[] agentDefs = Files.readAllLines(
                Paths.get(ClassLoader.getSystemResource(problemNames.get(problemName)).toURI())).stream().
                flatMap(t -> Arrays.stream(t.split("\t"))).
                toArray(String[]::new);

        //calculate solution plan
        TreeMap<Integer, Set<Step>> sortedPlan = SatSolver.calculateSolution(agentDefs);

        CnfCompilation cnfCompilation = new CnfCompilation(sortedPlan);


        Pair<List<List<ImmutablePair<String, Boolean>>>, List<List<ImmutablePair<String, Boolean>>>> compilePlanToCnf =
                compilePlanToCnf(cnfCompilation, new HashSet<>(Arrays.asList(failedSteps)));

        Pair<Map<String, Integer>, String> cnfEncoding =
                CnfEncodingUtils.encode(compilePlanToCnf.getLeft(), compilePlanToCnf.getRight());

        Pair<Set<String>, Set<String>> failedActions = runSatSolver(cnfEncoding.getRight(), cnfEncoding.getLeft(), cnfCompilation.getReverseActions());

        //log.info("Failed steps from SAT:\n{}", failedActions.stream().map(t -> StringUtils.join("\t", t, ",")).collect(Collectors.joining("\n")));
    }


    private static void help(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Main", options);
    }


    public static Pair<List<List<ImmutablePair<String, Boolean>>>,
            List<List<ImmutablePair<String, Boolean>>>> compilePlanToCnf(CnfCompilation cnfCompilation, Set<String> failedSteps) {


        List<List<ImmutablePair<String, Boolean>>> initFacts = cnfCompilation.calcInitFacts();
        List<List<ImmutablePair<String, Boolean>>> finalFacts = cnfCompilation.calcFinalFacts(failedSteps.toArray(new String[0]));

        List<List<ImmutablePair<String, Boolean>>> planCnfCompilation = cnfCompilation.compileToCnf();

        List<List<ImmutablePair<String, Boolean>>> fullPlanCnfCompilation =
                Stream.concat(Stream.concat(initFacts.stream(), planCnfCompilation.stream()),
                        finalFacts.stream()).
                        collect(Collectors.toList());

        log.info("cnf clauses:\n{}", fullPlanCnfCompilation.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        List<List<ImmutablePair<String, Boolean>>> healthClauses = cnfCompilation.encodeHealthyClauses();
        log.info("healthy clauses:\n{}", healthClauses.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));

        return ImmutablePair.of(fullPlanCnfCompilation, healthClauses);


        /*
        log.info("code map:\n{}", cnfEncoding.getKey());

        Map<String, Integer> variableToCodeMap = cnfEncoding.getKey();

        //inverted code map
        Map<Integer, String> codeToVariableMap =
                variableToCodeMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        log.info("cnf compilation output:\n{}",
                Arrays.stream(cnfEncoding.getValue().split("\\R")).
                        skip(1).
                        map(line ->
                                Arrays.stream(line.split(" ")).
                                        skip(1).
                                        filter(token -> !Objects.equals(token, "0")).
                                        map(Integer::parseInt).
                                        flatMap(token -> Stream.of(token.toString(), ImmutablePair.of(codeToVariableMap.get(Math.abs(token)), token > 0).toString())).
                                        collect(Collectors.partitioningBy(token -> isNumeric(token) || token.startsWith("-")))).
                        map(partResultMap ->
                                partResultMap.get(Boolean.TRUE).stream().collect(Collectors.joining(","))
                                        +
                                        "  "
                                        +
                                        partResultMap.get(Boolean.FALSE).stream().collect(Collectors.joining(","))
                        ).
                        map(Objects::toString).
                        collect(Collectors.joining("\n")));

        //log.trace("cnf compilation output:\n{}", cnfEncoding.getValue());
        */
        //return cnfEncoding;

    }

    public static void getAllPossibleDiagnosis(List<List<ImmutablePair<String, Boolean>>> hardConstraints,
                                               List<List<ImmutablePair<String, Boolean>>> softConstraints,
                                               Set<Set<String>> diagnosisSet, Map<String, String> reversableActions) {


        Pair<Map<String, Integer>, String> cnfEncoding =
                CnfEncodingUtils.encode(hardConstraints, softConstraints);

        Pair<Set<String>, Set<String>> possibleDiagnosis = SatSolver.runSatSolver(cnfEncoding.getRight(), cnfEncoding.getLeft(), reversableActions);

        log.info("Possible diagnosis {}", possibleDiagnosis);

        Set<String> mustDiagnosis = possibleDiagnosis.getLeft();
        Set<String> optionalDiagnosis = possibleDiagnosis.getRight();

        if (!mustDiagnosis.isEmpty()) {

            Set<Set<String>> optionalFailuresCombinations = optionalDiagnosis.size() > 0 ?
                    IntStream.range(0, (int) Math.pow(2, optionalDiagnosis.size())).
                            mapToObj(i -> String.format("%0" + optionalDiagnosis.size() + "d",
                                    Integer.parseInt(Integer.toBinaryString(i))).split("")).
                            map(i ->
                                    Streams.zip(
                                            Arrays.stream(i).map(j -> j.equals("1")),
                                            optionalDiagnosis.stream(),
                                            ImmutablePair::of
                                    ).
                                            filter(pair -> pair.left).
                                            map(pair -> pair.right).
                                            collect(Collectors.toSet())
                            ).
                            collect(Collectors.toSet()) :
                    Stream.of(mustDiagnosis).collect(Collectors.toSet());

            Set<Set<String>> diagnosisCandidates = optionalFailuresCombinations.stream().
                    map(optionalFailure -> Sets.union(Sets.newHashSet(optionalFailure), mustDiagnosis)).
                    collect(Collectors.toSet());

            if (!diagnosisSet.contains(mustDiagnosis)) {
                diagnosisSet.addAll(diagnosisCandidates);
                mustDiagnosis.forEach(diag -> {
                            List<List<ImmutablePair<String, Boolean>>> hardConstraintsCopy = new ArrayList<>(hardConstraints);

                            log.warn("Add hard constraint {}", diag);
                            log.warn("hardConstraints: {}", hardConstraints);

                            hardConstraintsCopy.add(Lists.newArrayList(ImmutablePair.of(diag, false)));
                            getAllPossibleDiagnosis(hardConstraintsCopy, softConstraints, diagnosisSet, reversableActions);
                        }
                );
            }
        }

    }

    public static Pair<Set<String>, Set<String>> runSatSolver(String cnfPlan, Map<String, Integer> codeMap,
                                                              Map<String, String> reversibleActions) {
//        ISolver solver = org.sat4j.maxsat.SolverFactory.newMiniMaxSAT();
//        solver.setTimeout(3600); // 1 hour timeout
        WeightedMaxSatDecorator solver = new WeightedMaxSatDecorator(SolverFactory.newDefault());
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

                Map<String, Boolean> variablesResult = codeMap.entrySet().stream().collect(
                        Collectors.toMap(Map.Entry::getKey, t -> codeResultsMap.get(t.getValue()),
                                (p1, p2) -> p1, TreeMap::new));

                log.info("variables result \n{}", variablesResult.entrySet().stream().
                        map(Object::toString).
                        collect(Collectors.joining("\n")));

                Map<String, List<String>> actionsByAgent = variablesResult.entrySet().stream().filter(entry -> entry.getKey().matches(".+Agent.+")).
                        filter(Map.Entry::getValue).
                        map(Map.Entry::getKey).
                        collect(Collectors.groupingBy(actionVar -> actionVar.split(",")[1]));

                Set<String> possibleFailures = new HashSet<>();
                Set<String> actionFailures = new HashSet<>();
                for (List<String> actionList : actionsByAgent.values()) {
                    String actionCandidate = null;

                    for (String action : actionList) {
                        if (reversibleActions.get(action) != null) {
                            actionCandidate = action;
                        } else if (action.matches(format(".*%s.*", HEALTHY.name())) &&
                                actionCandidate != null &&
                                action.equals(reversibleActions.get(actionCandidate))) {
                            possibleFailures.add(actionCandidate.replace(HEALTHY.name(), FAILED.name()));
                            actionCandidate = null;
                        } else if (!action.matches(format(".*%s.*", UNKNOWN.name()))) {
                            actionCandidate = null;

                            if (action.matches(format(".*%s.*", FAILED.name()))) {
                                actionFailures.add(action);
                            }
                        }
                    }

                }

                return ImmutablePair.of(actionFailures, possibleFailures);

            } else {
                log.warn(" Unsatisfiable !");
                return ImmutablePair.of(Sets.newHashSet(), Sets.newHashSet());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static TreeMap<Integer, Set<Step>> calculateSolution(String[] agentDefs) {
        Set<Map<Integer, Set<Step>>> solutionPlans = MAPboot.runCommandLine(agentDefs);

        Map<Integer, Set<Set<Step>>> solutionsBySteps = solutionPlans.stream().
                flatMap(i -> i.entrySet().stream()).
                collect(
                        Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(
                                Map.Entry::getValue,
                                Collectors.toSet())));

        return solutionsBySteps.entrySet().stream().map(
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
