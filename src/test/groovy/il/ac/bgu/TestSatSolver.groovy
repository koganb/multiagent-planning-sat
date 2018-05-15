package il.ac.bgu

import com.google.common.collect.Sets
import com.google.common.collect.Streams
import org.agreement_technologies.common.map_planner.Step
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.Pair
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors
import java.util.stream.IntStream

import static il.ac.bgu.CnfEncodingUtils.ActionState.FAILED

@Unroll
class TestSatSolver extends Specification {

    @Shared
    private TreeMap<Integer, Set<Step>> sortedPlan

    @Shared
    private Long testTimeSum = 0;

    def setupSpec() {
        String[] agentDefs = Files.readAllLines(
                Paths.get(this.getClass().getClassLoader().getResource("problems/satellite20.problem").toURI())).stream().
                flatMap({ t -> Arrays.stream(t.split("\t")) }).
                collect(Collectors.toList()).toArray(new String[0])

        def planningStartMils = System.currentTimeMillis()


        try {
            sortedPlan = SatSolver.calculateSolution(agentDefs)
            FileOutputStream fileOut =
                    new FileOutputStream("plan_satellite20.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(sortedPlan);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
        println "Planning time mils: " + (System.currentTimeMillis() - planningStartMils)

    }


    def cleanupSpec() {
        println "TestTime summary: " + testTimeSum

    }

    def "test diagnostics calculation"() {
        expect:
        def planningStartMils = System.currentTimeMillis()

        CnfCompilation cnfCompilation = new CnfCompilation(sortedPlan)

        //calculate solution plan
        Set<String> failedSteps = actionsToTest.stream().map { pair -> pair.getKey() }.collect(Collectors.toSet())

        Pair<List<List<ImmutablePair<String, Boolean>>>, List<List<ImmutablePair<String, Boolean>>>> compilePlanToCnf =
                SatSolver.compilePlanToCnf(cnfCompilation, failedSteps)

        Pair<Map<String, Integer>, String> cnfEncoding =
                CnfEncodingUtils.encode(compilePlanToCnf.getLeft(), compilePlanToCnf.getRight())

        def diagnosedActions = SatSolver.runSatSolver(cnfEncoding.getRight(), cnfEncoding.getLeft(), cnfCompilation.getReverseActions())

        def actualFailedActions = actionsToTest.stream().map { pair ->
            Integer actionIndex = sortedPlan.entrySet().stream().
                    filter { entry ->
                        entry.getValue().stream().
                                map { step -> step.getUuid() }.
                                anyMatch { uuid -> uuid == pair.getValue().getUuid() }
                    }.
                    findFirst().get().getKey()

            CnfEncodingUtils.encodeActionState(pair.getValue(), actionIndex, FAILED, true).getLeft()
        }.collect(Collectors.toSet())

        def mustFailures = diagnosedActions.left
        def optionalFailures = diagnosedActions.right
        def optionalFailuresSize = optionalFailures.size()
        def optionalFailuresCombinations = optionalFailures.size() > 0 ? IntStream.range(0, (int) Math.pow(2, optionalFailuresSize)).
                mapToObj { i ->
                    String.format("%0" + optionalFailuresSize + "d",
                            Integer.parseInt(Integer.toBinaryString(i))).split("")
                }.
                map { i ->
                    Streams.zip(
                            Arrays.stream(i).map { j -> j.equals("1") },
                            optionalFailures.stream(),
                            { arg1, arg2 -> ImmutablePair.of(arg1, arg2) }
                    ).filter { pair -> pair.left }.map { pair -> pair.right }.collect(Collectors.toList())
                }.
                collect(Collectors.toList()) : mustFailures



        def diagnosisCandidates = optionalFailuresCombinations.stream().
                map { optionalFailure -> Sets.union(Sets.newHashSet(optionalFailure), mustFailures) }.
                collect(Collectors.toSet())

        if (!diagnosisCandidates.contains(Sets.newHashSet(actualFailedActions))) {
            Set<Set<String>> allDiagnosis = Sets.newHashSet()

            SatSolver.getAllPossibleDiagnosis(compilePlanToCnf.getLeft(), compilePlanToCnf.getRight(), allDiagnosis, cnfCompilation.getReverseActions())

            assert (allDiagnosis.contains(Sets.newHashSet(actualFailedActions)))
        } else {
            assert true
        }


        cleanup:
        testTimeSum += (System.currentTimeMillis() - planningStartMils)

        where:
        actionsToTest << new ActionDependencyCalculation(sortedPlan).getIndependentActionsList(1).stream().
//               skip(407).
//                limit(1).

                collect(Collectors.toList())

    }
}