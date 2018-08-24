package il.ac.bgu;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.dataModel.Formattable;
import il.ac.bgu.dataModel.FormattableValue;
import il.ac.bgu.dataModel.Variable;
import lombok.extern.slf4j.Slf4j;
import org.agreement_technologies.agents.MAPboot;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_planner.POPAction;
import org.agreement_technologies.service.map_planner.POPStep;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

//    public static void main(String[] args) throws IOException, URISyntaxException, ParseException {
//        Options options = new Options();
//        options.addOption(Option.builder("p").
//                desc("problem name = required").
//                longOpt("problem_name").
//                hasArg(true).
//                numberOfArgs(1).
//                required(true).build());
//        options.addOption(Option.builder("v").
//                desc("verbose").
//                longOpt("verbose").
//                hasArg(false).
//                required(false).build());
//        options.addOption(Option.builder("f").
//                desc("failed actions indexes - optional").
//                longOpt("failed index").
//                hasArg(true).
//                numberOfArgs(Option.UNLIMITED_VALUES).
//                required(false).build());
//        options.addOption("h", "help", false, "show help.");
//
//        help(options);
//
//        CommandLineParser parser = new DefaultParser();
//        CommandLine cmd = parser.parse(options, args);
//
//        if (cmd.hasOption('v')) {
//            org.apache.log4j.Logger logger4j = org.apache.log4j.Logger.getRootLogger();
//            logger4j.setLevel(org.apache.log4j.Level.toLevel("DEBUG"));
//        }
//
//        String problemName = cmd.getOptionValue('p');
//        String[] failedSteps = cmd.hasOption('f') ?
//                cmd.getOptionValues('f') :
//                new String[0];
//
//        //get agent definitions from file
//        String[] agentDefs = Files.readAllLines(
//                Paths.get(ClassLoader.getSystemResource(problemNames.get(problemName)).toURI())).stream().
//                flatMap(t -> Arrays.stream(t.split("\t"))).
//                toArray(String[]::new);
//
//        //calculate solution plan
//        TreeMap<Integer, Set<Step>> sortedPlan = SatSolver.calculateSolution(agentDefs);
//
//        CnfCompilation cnfCompilation = new CnfCompilation(sortedPlan, new NoEffectFailureModel());
//
//
//        Pair<
//                ImmutableSet<ImmutableSet<ImmutablePair<? extends Formattable, Boolean>>>,
//                ImmutableSet<ImmutablePair<? extends Formattable, Boolean>>> compilePlanToCnf =
//                compilePlanToCnf(cnfCompilation, new HashSet<>(Arrays.asList(failedSteps)));
//
//        Pair<Map<Formattable, Integer>, String> cnfEncoding =
//                CnfEncodingUtils.encode(compilePlanToCnf.getLeft(), compilePlanToCnf.getRight());
//
//        Set<? extends Formattable> failedActions = runSatSolver(cnfEncoding.getRight(), cnfEncoding.getLeft());
//
//        //log.info("Failed steps from SAT:\n{}", failedActions.stream().map(t -> StringUtils.join("\t", t, ",")).collect(Collectors.joining("\n")));
//    }


    private static void help(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Main", options);
    }


    public static Pair<ImmutableList<ImmutableList<FormattableValue<Formattable>>>,
            ImmutableList<FormattableValue<Formattable>>> compilePlanToCnf(CnfCompilation cnfCompilation, Set<Action> failedActions) {


        Collection<FormattableValue<Variable>> initFacts = cnfCompilation.calcInitFacts();
        Collection<FormattableValue<Formattable>> finalFacts = cnfCompilation.calcFinalFacts(failedActions);

        ImmutableList<ImmutableList<FormattableValue<Formattable>>> planCnfCompilation = cnfCompilation.compileToCnf();

        Stream<ImmutableList<FormattableValue<Formattable>>> fullPlanCnfCompilationStream =
                Stream.concat(
                        Stream.concat(
                                initFacts.stream().map(t ->
                                        ImmutableList.of(FormattableValue.of(t.getFormattable(), t.getValue()))),
                                planCnfCompilation.stream()
                        ),
                        finalFacts.stream().map(ImmutableList::of)
                );

        ImmutableList<ImmutableList<FormattableValue<Formattable>>> fullPlanCnfCompilation =
                fullPlanCnfCompilationStream.collect(ImmutableList.toImmutableList());

        log.debug("cnf clauses:\n{}", fullPlanCnfCompilation.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));
        ImmutableList<FormattableValue<Formattable>> healthClauses = cnfCompilation.encodeHealthyClauses();
        log.debug("healthy clauses:\n{}", healthClauses.stream().map(t -> StringUtils.join(t, ",")).collect(Collectors.joining("\n")));

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


    public static Set<Formattable> runSatSolver(String cnfPlan, Map<Formattable, Integer> codeMap) {
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

                Map<Formattable, Boolean> variablesResult = codeMap.entrySet().stream().collect(
                        Collectors.toMap(Map.Entry::getKey, t -> codeResultsMap.get(t.getValue()),
                                (p1, p2) -> p1, TreeMap::new));

                log.info("variables result \n{}", variablesResult.entrySet().stream().
                        map(Object::toString).
                        collect(Collectors.joining("\n")));

                return variablesResult.entrySet().stream().
                        filter(entry -> entry.getKey().getValue().matches(format(".*%s.*", Action.State.FAILED.name())) &&
                                entry.getValue()).map(Map.Entry::getKey).collect(Collectors.toSet());

            } else {
                log.warn(" Unsatisfiable !");
                return Sets.newHashSet();
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
