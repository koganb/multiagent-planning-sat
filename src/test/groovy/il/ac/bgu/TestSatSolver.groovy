package il.ac.bgu

import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors


class TestSatSolver extends Specification {

    @Shared
    private String[] agentDefs

    def setupSpec() {
        agentDefs = Files.readAllLines(
                Paths.get(this.getClass().getClassLoader().getResource("problems/elevator30.problem").toURI())).stream().
                flatMap({ t -> Arrays.stream(t.split("\t")) }).
                collect(Collectors.toList()).toArray(new String[0])
    }

    def "test diagnostics calculation"() {
        expect:
        SatSolver.calculateDiagnostics(agentDefs, ['630604786', '935419721'] as String[]) == [
                "0:h(move-up-slow~slow0-0~n2~n3)",
                "7:h(board~p0~slow2-0~n8~n1~n2)",
        ]

    }
}